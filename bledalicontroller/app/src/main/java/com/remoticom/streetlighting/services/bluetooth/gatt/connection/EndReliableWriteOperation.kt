package com.remoticom.streetlighting.services.bluetooth.gatt.connection

import android.bluetooth.BluetoothGatt

class EndReliableWriteOperation(private val commitWrites: Boolean) : GattOperation<Boolean>() {
  override fun performAsync(
    connection: GattConnection,
    callback: GattCallback<Boolean>
  ) {
    super.performAsync(connection, callback)

    if (commitWrites) {
      connection.executeReliableWrite()
    } else {
      connection.abortReliableWrite()
    }
  }

  override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
    super.onReliableWriteCompleted(gatt, status)

    if (status == BluetoothGatt.GATT_SUCCESS) {
      completeWithData(true)
    } else {
      completeWithError(GattErrorCode.GattError, status)
    }
  }
}
