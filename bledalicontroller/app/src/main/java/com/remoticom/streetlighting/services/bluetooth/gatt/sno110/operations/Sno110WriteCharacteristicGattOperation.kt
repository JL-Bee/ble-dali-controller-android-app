package com.remoticom.streetlighting.services.bluetooth.gatt.sno110.operations

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattCallback
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattConnection
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattErrorCode
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattOperation
import java.util.*

open class Sno110WriteCharacteristicGattOperation<T>(
  private val serviceUuidString: String,
  private val characteristicUuidString: String,
  private val value: T?,
  private val serialize: (BluetoothGattCharacteristic, T?) -> Boolean //,
  //private val deserialize: (BluetoothGattCharacteristic) -> T
) : GattOperation<T>() {
  override fun performAsync(
    connection: GattConnection,
    callback: GattCallback<T>
  ) {
    super.performAsync(connection, callback)

    val bluetoothCharacteristic = connection.getCharacteristic(
      UUID.fromString(serviceUuidString),
      UUID.fromString(characteristicUuidString)
    )

    if (null == bluetoothCharacteristic) {
      completeWithError(GattErrorCode.PreconditionFailed)
      return
    }

    if (!serialize(bluetoothCharacteristic, value)) {
      completeWithError(GattErrorCode.SerializationFailed)
      return
    }

    if (!connection.writeCharacteristic(bluetoothCharacteristic)) {
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
      if (null != value) {
        completeWithData(value)
      } else {
        completeWithError(GattErrorCode.WriteCharacteristicValueMismatch)
      }
    } else {
      completeWithError(GattErrorCode.GattError, status)
    }
  }
}
