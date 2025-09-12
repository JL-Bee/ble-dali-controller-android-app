package com.remoticom.streetlighting.services.bluetooth.gatt.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile

class DisconnectGattOperation() : GattOperation<Boolean>() {
  override fun performAsync(
    connection: GattConnection,
    callback: GattCallback<Boolean>
  ) {
    super.performAsync(connection, callback)

    if (!connection.disconnect()) {
      completeWithData(false)
    }
  }

  override fun onConnectionStateChange(
    gatt: BluetoothGatt,
    status: Int,
    newState: Int
  ) {
    super.onConnectionStateChange(gatt, status, newState)

    if (status == BluetoothGatt.GATT_SUCCESS) {
      when (newState) {
        BluetoothProfile.STATE_DISCONNECTED ->
          completeWithData(true)
      }
    } else {
      completeWithError(GattErrorCode.GattError, status)
    }

  }
}
