package com.remoticom.streetlighting.services.bluetooth.gatt.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import android.util.Log

class DiscoverServicesOperation() : GattOperation<Boolean>() {
  override fun performAsync(
    connection: GattConnection,
    callback: GattCallback<Boolean>
  ) {
    super.performAsync(connection, callback)

    if (!connection.discoverServices()) {
      completeWithData(false)
    }
  }

  override fun onConnectionStateChange(
    gatt: BluetoothGatt?,
    status: Int,
    newState: Int
  ) {
    super.onConnectionStateChange(gatt, status, newState)

    if (status != BluetoothGatt.GATT_SUCCESS) return

    when (newState) {
      BluetoothProfile.STATE_DISCONNECTING, BluetoothProfile.STATE_DISCONNECTED ->
        completeWithData(false)
    }
  }

  override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
    super.onServicesDiscovered(gatt, status)

    if (status == BluetoothGatt.GATT_SUCCESS) {
      Log.d(TAG, "Discovered services")
      gatt.services.forEach { service ->
        Log.d(TAG, "Service: uuid=${service.uuid}")
        service.characteristics.forEach { characteristic ->
          Log.d(TAG, "Characteristic: uuid=${characteristic.uuid}")
        }
      }

      completeWithData(true)
    } else {
      completeWithError(GattErrorCode.GattError, status)
    }
  }

  companion object {
    private const val TAG = "DiscoverServicesOp"
  }

  override fun shouldFailOnDisconnect(): Boolean = false
}

