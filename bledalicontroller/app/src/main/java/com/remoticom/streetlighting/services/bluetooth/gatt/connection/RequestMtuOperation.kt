package com.remoticom.streetlighting.services.bluetooth.gatt.connection

import android.bluetooth.BluetoothGatt
import android.util.Log

// NOTE: Currently not used / nodes probably have non-negotiable / fixed MTU
class RequestMtuOperation(val mtu: Int) : GattOperation<Boolean>() {
  override fun performAsync(
    connection: GattConnection,
    callback: GattCallback<Boolean>
  ) {
    super.performAsync(connection, callback)

    if (!connection.requestMtu(mtu)) {
      completeWithData(false)
    }
  }

  override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
    super.onMtuChanged(gatt, mtu, status)

    if (status == BluetoothGatt.GATT_SUCCESS) {
      Log.d(TAG, "MTU changed: $mtu")

      completeWithData(true)
    } else {
      completeWithError(GattErrorCode.GattError, status)
    }
  }

  companion object {
    private const val TAG = "RequestMtuOp"
  }
}

