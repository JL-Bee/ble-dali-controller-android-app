package com.remoticom.streetlighting.services.bluetooth.gatt.sno110.operations.authentication

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattCallback
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattConnection
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattErrorCode
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattOperation
import java.util.*

open class Sno110WriteChallengeOperation<T>(
  private val service: String,
  private val characteristic: String,
  private val value: T?,
  private val serialize: (BluetoothGattCharacteristic, T?) -> Boolean,
  private val deserialize: (BluetoothGattCharacteristic) -> T,
  private val deserializeNotification: (BluetoothGattCharacteristic) -> T
) : GattOperation<T>() {
  override fun performAsync(
    connection: GattConnection,
    callback: GattCallback<T>
  ) {
    super.performAsync(connection, callback)

    val bluetoothCharacteristic = connection.getCharacteristic(
      UUID.fromString(service),
      UUID.fromString(characteristic)
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
      if (confirmedValue != value) {
        completeWithError(GattErrorCode.WriteCharacteristicValueMismatch)
      }
    } else {
      completeWithError(GattErrorCode.GattError, status)
    }
  }

  override fun onCharacteristicChanged(
    gatt: BluetoothGatt?,
    characteristic: BluetoothGattCharacteristic?
  ) {
    super.onCharacteristicChanged(gatt, characteristic)

    if (null == characteristic) {
      completeWithError(GattErrorCode.PreconditionFailed)
    } else {
      val indication = deserializeNotification(characteristic)

      completeWithData(indication)
    }
  }
}
