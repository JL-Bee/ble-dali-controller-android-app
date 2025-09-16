package com.remoticom.streetlighting.services.bluetooth.gatt.connection

import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import android.Manifest
import android.os.Build
import com.remoticom.streetlighting.services.bluetooth.gatt.bdc.bdcServiceMatchingMask
import com.remoticom.streetlighting.services.bluetooth.gatt.bdc.getBdcCharacteristic
import com.remoticom.streetlighting.services.bluetooth.gatt.zsc010.getZsc010Characteristic
import com.remoticom.streetlighting.services.bluetooth.gatt.zsc010.zsc010ServiceMatchingMask
import java.lang.reflect.Method
import java.util.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GattConnection(
  private val context: Context,
  private val device: BluetoothDevice,
  private val callback: BluetoothGattCallback?,
  private val macAddressServiceMask: String,
) : BluetoothGattCallback() {

  private var currentOperation: BluetoothGattCallback? = null
  private val operationMutex = Mutex()

  private var gatt: BluetoothGatt? = null
  private var macAddress: Long? = null

  var currentMtu: Int = 23
    private set

  private var pendingChunks: ArrayDeque<ByteArray>? = null
  private var chunkedCharacteristic: BluetoothGattCharacteristic? = null
  private var chunkedOriginalValue: ByteArray? = null

  private val TAG
    get() = (currentOperation ?: this).javaClass.simpleName

  suspend fun <T> perform(operation: GattOperation<T>, block: ((GattOperation.Result<T>) -> Unit)? = null): GattOperation.Result<T> =
    operationMutex.withLock {
      assert(currentOperation == null)
      currentOperation = operation
      Log.d(TAG, "STARTED")
      try {
        val result = operation.perform(this)

        block?.let {
          it(result)
        }

        Log.d(TAG, "FINISHED")

        result
      } catch (exception: Exception) {
        Log.e(TAG, "FAILED", exception)
        throw exception
      } finally {
        currentOperation = null
      }
    }

  fun connectGatt(autoConnect: Boolean = false) {
    Log.v(TAG, "connectGatt initiated")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val missingPermission = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
      ) != PackageManager.PERMISSION_GRANTED

      if (missingPermission) {
        Log.e(TAG, "Required Bluetooth permissions not granted")
        throw SecurityException("Missing Bluetooth permissions")
      }
    }

    try {
      gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        device.connectGatt(context, autoConnect, this, BluetoothDevice.TRANSPORT_LE)
      } else {
        device.connectGatt(context, autoConnect, this)
      }
      macAddress = null
    } catch (securityException: SecurityException) {
      Log.e(TAG, "SecurityException during connectGatt", securityException)
      throw securityException
    }
  }

  fun disconnect() = gatt?.let {
    Log.v(TAG, "disconnect initiated")
    it.disconnect()
    true
  } ?: false

  fun close() = gatt?.let {
    Log.v(TAG, "close initiated")
    it.close()
    true
  } ?: false

  fun resetOperation() {
    currentOperation = null
  }

  fun discoverServices() = gatt?.let {
    Log.v(TAG, "discoverServices initiated")
    it.discoverServices()
    true
  } ?: false

  // Can be used if needed by RequestMtuOperation
  fun requestMtu(mtu: Int) = gatt?.let {
    Log.v(TAG, "requestMtu initiated")
    it.requestMtu(mtu)
    true
  } ?: false

  fun refresh() = gatt?.let {
    try {
      Log.d(TAG, "Clearing GATT internal cache and force a refresh of the services from the remote device.")

      val refresh: Method? = it.javaClass.getMethod("refresh")
      refresh?.invoke(it)
    } catch (e: Exception) {
      Log.e(TAG, "Error calling Gatt refresh ${e.message}")
    }
  }

  fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic?, enable: Boolean) = gatt?.let {
    it.setCharacteristicNotification(characteristic, enable)
  } ?: false

  // TODO (REFACTOR/SNO110): This is ZSC010 specific
  private fun zsc010ServiceMatchingMask(serviceMask: String) = gatt?.let {
    it.zsc010ServiceMatchingMask(serviceMask)
  }

  // TODO (REFACTOR/SNO110): This is ZSC010 specific
  private fun bdcServiceMatchingMask(serviceMask: String) = gatt?.let {
    it.bdcServiceMatchingMask(serviceMask)
  }


  // TODO (REFACTOR/SNO110): This is ZSC010 specific
  fun getZsc010Characteristic(
    serviceMask: String,
    characteristic: Long
  ): BluetoothGattCharacteristic? {
    if (null == macAddress) return null

    val service = zsc010ServiceMatchingMask(serviceMask) ?: return null

    return service.getZsc010Characteristic(
      serviceMask,
      characteristic,
      macAddress!!
    )
  }

  // TODO: Refactor with above?!
  fun getBdcCharacteristic(
    serviceMask: String,
    characteristic: Long
  ): BluetoothGattCharacteristic? {
    if (null == macAddress) return null

    val service = bdcServiceMatchingMask(serviceMask) ?: return null

    return service.getBdcCharacteristic(
      serviceMask,
      characteristic,
      macAddress!!
    )
  }


  fun getCharacteristic(
    serviceUuid: UUID,
    characteristicUuid: UUID
  ) : BluetoothGattCharacteristic? {
    val services = gatt?.services

    val service = services?.find {
      it.uuid == serviceUuid
    } ?: return null

    val characteristic = service.getCharacteristic(characteristicUuid)

    return characteristic
  }

  fun readCharacteristic(characteristic: BluetoothGattCharacteristic) =
    gatt?.let {
      Log.v(TAG, "readCharacteristic initiated")
      it.readCharacteristic(characteristic)
      true
    } ?: false

  fun beginReliableWrite(): Boolean {
    return gatt?.beginReliableWrite() ?: false
  }

  fun abortReliableWrite() {
    gatt?.abortReliableWrite()
  }

  fun executeReliableWrite() {
    gatt?.executeReliableWrite()
  }

  fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) =
    gatt?.let {
      Log.v(TAG, "writeCharacteristic initiated")
      it.writeCharacteristic(characteristic)
      true
    } ?: false

  fun writeValueChunked(
    characteristic: BluetoothGattCharacteristic,
    data: ByteArray
  ): Boolean = gatt?.let {
    val maxPayload = currentMtu - 3
    Log.d(TAG, "currentMtu=$currentMtu, maxPayload=$maxPayload")

    if (currentMtu < 69) {
      Log.w(TAG, "MTU below 69, using small frame mode")
    }

    val chunks = ArrayDeque<ByteArray>()
    var offset = 0
    while (offset < data.size) {
      val end = minOf(offset + maxPayload, data.size)
      chunks.addLast(data.copyOfRange(offset, end))
      offset = end
    }

    if (chunks.isEmpty()) {
      Log.e(TAG, "No data to write")
      return@let false
    }

    pendingChunks = chunks
    chunkedCharacteristic = characteristic
    chunkedOriginalValue = data

    characteristic.value = pendingChunks!!.removeFirst()
    it.writeCharacteristic(characteristic)
  } ?: false

  fun writeDescriptor(descriptor: BluetoothGattDescriptor) =
    gatt?.let {
      Log.v(TAG, "writeDescriptor initiated")
      it.writeDescriptor(descriptor)
    } ?: false

  fun isOperationInProgress() : Boolean {
    return (currentOperation != null)
  }

  override fun onConnectionStateChange(
    gatt: BluetoothGatt?,
    status: Int,
    newState: Int
  ) {
    Log.v(TAG, "onConnectionStateChange called")
    super.onConnectionStateChange(gatt, status, newState)

    if (BluetoothGatt.GATT_SUCCESS == status) {
      when (newState) {
         BluetoothProfile.STATE_DISCONNECTED -> {
           this.gatt?.close()
           this.gatt = null
           this.macAddress = null
        }
      }
    } else {
      Log.e(TAG,"status=${status.toGattStatusDescription()}")
    }

    callback?.onConnectionStateChange(gatt, status, newState)

    // Must be called last, because code after suspended operation
    // must see changes by made by callback
    currentOperation?.onConnectionStateChange(gatt, status, newState)
  }

  override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
    Log.v(TAG, "onServicesDiscovered called")
    super.onServicesDiscovered(gatt, status)

    if (BluetoothGatt.GATT_SUCCESS == status) {
      // TODO: Refactor findMACAddress also applies for BDC
      macAddress = gatt.findMACAddress(macAddressServiceMask)
    } else {
      Log.e(TAG,"status=${status.toGattStatusDescription()}")
    }

    callback?.onServicesDiscovered(gatt, status)

    // Must be called last, because code after suspended operation
    // must see changes by made by callback
    currentOperation?.onServicesDiscovered(gatt, status)
  }

  override fun onCharacteristicRead(
    gatt: BluetoothGatt,
    characteristic: BluetoothGattCharacteristic,
    status: Int
  ) {
    Log.v(TAG, "onCharacteristicRead called")
    super.onCharacteristicRead(gatt, characteristic, status)

    if (BluetoothGatt.GATT_SUCCESS != status) {
      Log.e(TAG,"status=${status.toGattStatusDescription()}")
    }

    callback?.onCharacteristicRead(gatt, characteristic, status)

    // Must be called last, because code after suspended operation
    // must see changes by made by callback
    currentOperation?.onCharacteristicRead(gatt, characteristic, status)
  }

  override fun onCharacteristicWrite(
    gatt: BluetoothGatt,
    characteristic: BluetoothGattCharacteristic,
    status: Int
  ) {
    Log.v(TAG, "onCharacteristicWrite called")
    super.onCharacteristicWrite(gatt, characteristic, status)

    if (BluetoothGatt.GATT_SUCCESS != status) {
      Log.e(TAG,"status=${status.toGattStatusDescription()}")
    }

    if (chunkedCharacteristic != null && characteristic === chunkedCharacteristic) {
      val queue = pendingChunks
      if (status == BluetoothGatt.GATT_SUCCESS && queue != null && queue.isNotEmpty()) {
        characteristic.value = queue.removeFirst()
        gatt.writeCharacteristic(characteristic)
        return
      } else {
        if (status == BluetoothGatt.GATT_SUCCESS) {
          chunkedOriginalValue?.let { characteristic.value = it }
        }
        pendingChunks = null
        chunkedCharacteristic = null
        chunkedOriginalValue = null
      }
    }

    callback?.onCharacteristicWrite(gatt, characteristic, status)

    // Must be called last, because code after suspended operation
    // must see changes by made by callback
    currentOperation?.onCharacteristicWrite(gatt, characteristic, status)
  }

  override fun onCharacteristicChanged(
    gatt: BluetoothGatt,
    characteristic: BluetoothGattCharacteristic
  ) {
    Log.v(TAG, "onCharacteristicChanged called")
    super.onCharacteristicChanged(gatt, characteristic)

    callback?.onCharacteristicChanged(gatt, characteristic)

    // Must be called last, because code after suspended operation
    // must see changes by made by callback
    currentOperation?.onCharacteristicChanged(gatt, characteristic)
  }

  override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
    Log.v(TAG, "onMtuChanged called")
    super.onMtuChanged(gatt, mtu, status)

    if (BluetoothGatt.GATT_SUCCESS != status) {
      Log.e(TAG,"status=${status.toGattStatusDescription()}")
    }

    currentMtu = mtu
    if (currentMtu < 69) {
      Log.w(TAG, "MTU $currentMtu below recommended 69")
    }

    callback?.onMtuChanged(gatt, mtu, status)

    // Must be called last, because code after suspended operation
    // must see changes by made by callback
    currentOperation?.onMtuChanged(gatt, mtu, status)
  }

  override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
    Log.v(TAG, "onReliableWriteCompleted called")
    super.onReliableWriteCompleted(gatt, status)

    if (BluetoothGatt.GATT_SUCCESS != status) {
      Log.e(TAG,"status=${status.toGattStatusDescription()}")
    }

    callback?.onReliableWriteCompleted(gatt, status)

    // Must be called last, because code after suspended operation
    // must see changes by made by callback
    currentOperation?.onReliableWriteCompleted(gatt, status)
  }

  override fun onDescriptorRead(
    gatt: BluetoothGatt,
    descriptor: BluetoothGattDescriptor,
    status: Int
  ) {
    Log.v(TAG, "onDescriptorRead called")
    super.onDescriptorRead(gatt, descriptor, status)

    if (BluetoothGatt.GATT_SUCCESS != status) {
      Log.e(TAG,"status=${status.toGattStatusDescription()}")
    }

    callback?.onDescriptorRead(gatt, descriptor, status)

    // Must be called last, because code after suspended operation
    // must see changes by made by callback
    currentOperation?.onDescriptorRead(gatt, descriptor, status)
  }

  override fun onDescriptorWrite(
    gatt: BluetoothGatt,
    descriptor: BluetoothGattDescriptor,
    status: Int
  ) {
    Log.v(TAG, "onDescriptorWrite called")
    super.onDescriptorWrite(gatt, descriptor, status)

    if (BluetoothGatt.GATT_SUCCESS != status) {
      Log.e(TAG,"status=${status.toGattStatusDescription()}")
    }

    callback?.onDescriptorWrite(gatt, descriptor, status)

    // Must be called last, because code after suspended operation
    // must see changes by made by callback
    currentOperation?.onDescriptorWrite(gatt, descriptor, status)
  }

  override fun onReadRemoteRssi(
    gatt: BluetoothGatt,
    rssi: Int,
    status: Int
  ) {
    Log.v(TAG, "onReadRemoteRssi called")
    super.onReadRemoteRssi(gatt, rssi, status)

    if (BluetoothGatt.GATT_SUCCESS != status) {
      Log.e(TAG,"status=${status.toGattStatusDescription()}")
    }

    callback?.onReadRemoteRssi(gatt, rssi, status)

    // Must be called last, because code after suspended operation
    // must see changes by made by callback
    currentOperation?.onReadRemoteRssi(gatt, rssi, status)
  }
}
