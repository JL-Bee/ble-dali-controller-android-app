package com.remoticom.streetlighting.services.bluetooth.gatt.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile

class ConnectGattOperation : GattOperation<Boolean>() {
  override fun performAsync(
    connection: GattConnection,
    callback: GattCallback<Boolean>
  ) {
    super.performAsync(connection, callback)
    try {
      connection.connectGatt()
    } catch (securityException: SecurityException) {
      completeWithError(GattErrorCode.MissingPermission)
    }
  }

  override fun onConnectionStateChange(
    gatt: BluetoothGatt?,
    status: Int,
    newState: Int
  ) {
    super.onConnectionStateChange(gatt, status, newState)

    if (BluetoothGatt.GATT_SUCCESS == status) {
      when (newState) {
        BluetoothProfile.STATE_CONNECTED ->
          completeWithData(true)
        BluetoothProfile.STATE_DISCONNECTING, BluetoothProfile.STATE_DISCONNECTED ->
          completeWithData(false)
      }
    } else {
      completeWithError(GattErrorCode.GattError, status)
    }
  }

  override fun shouldFailOnDisconnect(): Boolean = false
}

