package com.remoticom.streetlighting.services.bluetooth.gatt.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.concurrent.atomic.AtomicBoolean

typealias GattCallback<T> = (GattOperation.Result<T>) -> Unit

typealias GattData<T> = GattOperation.Result.Data<T>
typealias GattError<T> = GattOperation.Result.Error<T>
typealias GattFatalError<T> = GattOperation.Result.FatalError<T>

enum class GattErrorCode {
  GattError,
  GattMethodFailed,
  PreconditionFailed,
  SerializationFailed,
  WriteCharacteristicValueMismatch,
  MissingPermission
}

abstract class GattOperation<T> : BluetoothGattCallback() {
  lateinit private var callback: GattCallback<T>
  private val hasCompleted = AtomicBoolean(false)

  sealed class Result<out T> {
    data class Data<out T>(val value: T) : Result<T>() {
      operator fun not() = value

      override fun toString(): String {
        return value.toString()
      }
    }
    data class Error<out T>(val code: GattErrorCode, val status: Int? = null, val operationIdentifier: String? = null) : Result<T>() {
      operator fun not() = code

      override fun toString(): String {
        return when(code) {
          GattErrorCode.GattError -> "$code (${status?.toGattStatusDescription()})"
          else -> code.toString()
        }
      }
    }
    // TODO (REFACTOR): Fatal error seems not to be used
    data class FatalError<out T>(val exception: Throwable) : Result<T>() {
      operator fun not() = exception

      override fun toString(): String {
        return exception.toString()
      }
    }
  }

  protected open fun performAsync(
    connection: GattConnection,
    callback: GattCallback<T>
  ) {
    hasCompleted.set(false)
    this.callback = callback
  }

  protected fun completeWithData(value: T) = complete(Result.Data(value))

  protected fun completeWithError(code: GattErrorCode, status: Int? = null) =
    complete(Result.Error<T>(code, status, this::class.simpleName))

  protected fun completeWithFatalError(exception: Throwable) =
    complete(Result.FatalError<T>(exception))

  private fun complete(result: Result<T>) {
    if (hasCompleted.compareAndSet(false, true)) {
      callback(result)
    } else {
      Log.w(TAG, "Ignoring completion for ${this::class.simpleName} because the operation has already finished")
    }
  }

  protected open fun shouldFailOnDisconnect(): Boolean = true

  internal fun handleConnectionStateChange(status: Int, newState: Int) {
    if (status != BluetoothGatt.GATT_SUCCESS) {
      Log.w(TAG, "Completing ${this::class.simpleName} due to GATT error ${status.toGattStatusDescription()}")
      completeWithError(GattErrorCode.GattError, status)
      return
    }

    if (newState == BluetoothProfile.STATE_DISCONNECTED && shouldFailOnDisconnect()) {
      Log.w(TAG, "Completing ${this::class.simpleName} due to unexpected disconnection")
      completeWithError(GattErrorCode.GattError, BluetoothGatt.GATT_FAILURE)
    }
  }

  suspend fun perform(connection: GattConnection): Result<T> =
    suspendCancellableCoroutine { continuation ->
      performAsync(connection) { result ->
        when(result) {
          is Result.FatalError ->
            continuation.resumeWithException(!result)
          else -> // succeeded or non fatal, here is a value
            continuation.resume(result)
        }
      }
      continuation.invokeOnCancellation {
        Log.d(TAG, "Operation cancelled. Resetting current operation...")

        connection.resetOperation()
      }
    }

  companion object {
    private const val TAG = "GattOperation"
  }
}
