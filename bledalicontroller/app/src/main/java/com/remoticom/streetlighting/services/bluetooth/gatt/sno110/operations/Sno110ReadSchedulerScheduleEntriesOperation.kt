package com.remoticom.streetlighting.services.bluetooth.gatt.sno110.operations

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattCallback
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattConnection
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattErrorCode
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattOperation
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.*
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.characteristics.ScheduleEntry
import java.util.*

open class Sno110ReadSchedulerScheduleEntriesOperation() : GattOperation<List<ScheduleEntry>>() {
  override fun performAsync(
    connection: GattConnection,
    callback: GattCallback<List<ScheduleEntry>>
  ) {
    super.performAsync(connection, callback)

    val bluetoothCharacteristic = connection.getCharacteristic(
      UUID.fromString(SNO110_BLUETOOTH_SERVICE_SCHEDULER),
      UUID.fromString(SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_CONTROL_POINT)
    )

    if (null == bluetoothCharacteristic) {
      completeWithError(GattErrorCode.PreconditionFailed)
      return
    }

    if (!bluetoothCharacteristic.serializeSchedulerControlPointOpcode(SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_CONTROL_POINT_OPCODE_READ_ENTRIES)) {
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

    if (status != BluetoothGatt.GATT_SUCCESS) {
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
      val entries = characteristic.deserializeSchedulerScheduleEntries()

      completeWithData(entries)
    }
  }
}
