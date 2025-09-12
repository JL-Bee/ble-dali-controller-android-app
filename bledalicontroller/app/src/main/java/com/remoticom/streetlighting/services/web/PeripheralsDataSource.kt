package com.remoticom.streetlighting.services.web

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.remoticom.streetlighting.services.authentication.AuthenticationService
import com.remoticom.streetlighting.services.web.data.Peripheral
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.lang.Exception

enum class FailureReason {
  BadRequest,
  Unauthorized, // Token expired or invalid
  Forbidden,    // Unauthorized user or not allowed
  PeripheralNotFound,
  ServerError,
  Other,
}

sealed class PeripheralResult {
  data class Success(val peripheral: Peripheral) : PeripheralResult()
  data class Failure(
    val exception: Exception,
    val reason: FailureReason
    ) : PeripheralResult()
}

enum class PeripheralStatus {
  None,
  Loading,
  Loaded,
  Error,
  Forbidden,
}

data class PeripheralState(
  val status: PeripheralStatus = PeripheralStatus.None,
  val peripheral: Peripheral? = null,
//  val requestCount: Int = 0,
  val lastError: Error? = null
)

class PeripheralsDataSource(private val webService: WebService) {

  private val scope = MainScope()

  data class State(
    val peripherals: Map<String, PeripheralState> = mapOf()
    //val isLoading: Boolean = false,
  )

  private val _state = MutableLiveData(State())
  val state: LiveData<State> = _state

  private fun updatePeripheralState(uuid: String, state: PeripheralState) {
    Log.d(TAG, "Updating peripheral ($uuid) state to: ${state.toString().replace(Regex("password=[0-9]*,[' ']pin=null"), "password=****, pin=****")}")

    val peripherals = _state.value?.peripherals?.toMutableMap() ?: mutableMapOf()

    peripherals[uuid] = state

    _state.value = _state.value?.copy(peripherals = peripherals)
  }

  private fun currentPeripheral(uuid: String) : PeripheralState {
    return _state.value?.peripherals?.get(uuid) ?: PeripheralState()
  }

//  private fun requestCountsForPeripheral(uuid: String) : Int? {
//    return _state.value?.peripherals?.get(uuid)?.requestCount
//  }

  fun clearPeripherals() {
    _state.value = _state.value?.copy(peripherals = mapOf())
  }

  fun loadPeripheralIfNeeded(uuid: String) {
    // Log.d(TAG, "Loading peripheral if needed: $uuid")

    val status = currentPeripheral(uuid).status

    if (status != PeripheralStatus.None) return

    Log.d(TAG, "Loading peripheral: $uuid")

    updatePeripheralState(uuid, currentPeripheral(uuid).copy(
      status = PeripheralStatus.Loading
//      requestCount = currentPeripheral(uuid).requestCount + 1
    ))

    scope.launch {
      when (val result = webService.requestPeripheral(uuid)) {
        is PeripheralResult.Success -> {
          Log.d(TAG, "Peripheral ($uuid) loaded.")

          updatePeripheralState(uuid, currentPeripheral(uuid).copy(
            status = PeripheralStatus.Loaded,
            peripheral = result.peripheral))
        }
        is PeripheralResult.Failure -> {
          Log.d(TAG, "Error loading peripheral ($uuid).")

          if (result.reason == FailureReason.Unauthorized) {
            AuthenticationService.getInstance().logout()
          }

          updatePeripheralState(uuid, currentPeripheral(uuid).copy(
            status = if (result.reason == FailureReason.Forbidden) PeripheralStatus.Forbidden else PeripheralStatus.Error
          ))

//          val retries = requestCountsForPeripheral(uuid)
//
//          retries?.let {
//            if (it < 3) {
//              Log.d(TAG, "Error loading peripheral ($uuid). Available for retry.")
//
//              updatePeripheral(uuid, currentPeripheral(uuid).copy(
//                status = PeripheralStatus.None
//              ))
//            } else {
//              Log.d(TAG, "Error loading peripheral ($uuid). Max retries reached.")
//
//              updatePeripheral(uuid, currentPeripheral(uuid).copy(
//                status = PeripheralStatus.Error
//              ))
//            }
//          }
        }
      }
    }
  }

  suspend fun updatePeripheral(uuid: String, peripheral: Peripheral) : PeripheralResult {
    updatePeripheralState(uuid, currentPeripheral(uuid).copy(status = PeripheralStatus.Loading, peripheral = currentPeripheral(uuid).copy().peripheral))

    val result = webService.updatePeripheral(uuid, peripheral)

    when (result) {
      is PeripheralResult.Success -> {
        // Update local peripheral
        updatePeripheralState(uuid, currentPeripheral(uuid).copy(status = PeripheralStatus.Loaded, peripheral = result.peripheral))

      }
      is PeripheralResult.Failure -> {
        Log.e(TAG, "Error updating peripheral: ${result.reason} (${result.exception.message})")

        // If updating fails we don't modify the data source local peripheral
        updatePeripheralState(uuid, currentPeripheral(uuid).copy(status = PeripheralStatus.Error))
      }
    }

    return result
  }

  companion object {
    const val TAG = "PeripheralsDataSource"
  }
}
