package com.remoticom.streetlighting.services.bluetooth.gatt.zsc010.operations

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattCallback
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattConnection
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattErrorCode
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattOperation

open class Zsc010WriteCharacteristicGattOperation<T>(
  private val serviceMask: String,
  private val characteristic: Long,
  private val value: T?,
  private val serialize: (BluetoothGattCharacteristic, T?) -> Boolean,
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

    if (!serialize(bluetoothCharacteristic, value)) {
      completeWithError(GattErrorCode.SerializationFailed)
      return
    }

    val bytes = bluetoothCharacteristic.value ?: run {
      completeWithError(GattErrorCode.SerializationFailed)
      return
    }

    if (!connection.writeValueChunked(bluetoothCharacteristic, bytes)) {
      completeWithError(GattErrorCode.GattMethodFailed)
      return
    }
  }

  override fun onCharacteristicWrite(
    gatt: BluetoothGatt,
    characteristic: BluetoothGattCharacteristic,
    status: Int
  ) {
    super.onCharacteristicWrite(gatt, characteristic, status)

    if (status == BluetoothGatt.GATT_SUCCESS) {
      val confirmedValue = deserialize(characteristic)
      if (confirmedValue == value) {
        completeWithData(confirmedValue)
      } else {
        completeWithError(GattErrorCode.WriteCharacteristicValueMismatch)
      }
    } else {
      completeWithError(GattErrorCode.GattError, status)
    }
  }
}
