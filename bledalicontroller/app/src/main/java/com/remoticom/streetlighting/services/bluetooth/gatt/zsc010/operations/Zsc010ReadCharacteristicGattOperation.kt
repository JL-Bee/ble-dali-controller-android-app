package com.remoticom.streetlighting.services.bluetooth.gatt.zsc010.operations

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattCallback
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattConnection
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattErrorCode
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattOperation

open class Zsc010ReadCharacteristicGattOperation<T>(
  private val serviceMask: String,
  private val characteristic: Long,
  private val deserialize: (BluetoothGattCharacteristic) -> T
) : GattOperation<T>() {
  override fun performAsync(
    connection: GattConnection,
    callback: GattCallback<T>
  ) {
    super.performAsync(connection, callback)

    val bluetoothCharacteristic = connection.getZsc010Characteristic(
      serviceMask,
      characteristic
    )

    if (null == bluetoothCharacteristic) {
      completeWithError(GattErrorCode.PreconditionFailed)
      return
    }

    if (!connection.readCharacteristic(bluetoothCharacteristic)) {
      completeWithError(GattErrorCode.GattMethodFailed)
      return
    }
  }

  override fun onCharacteristicRead(
    gatt: BluetoothGatt,
    characteristic: BluetoothGattCharacteristic,
    status: Int
  ) {
    super.onCharacteristicRead(gatt, characteristic, status)

    if (status == BluetoothGatt.GATT_SUCCESS) {
      completeWithData(deserialize(characteristic))
    } else {
      completeWithError(GattErrorCode.GattError, status)
    }
  }
}
