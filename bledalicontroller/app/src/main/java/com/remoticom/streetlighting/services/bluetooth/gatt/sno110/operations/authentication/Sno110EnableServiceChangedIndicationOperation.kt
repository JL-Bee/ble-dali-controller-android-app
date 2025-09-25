package com.remoticom.streetlighting.services.bluetooth.gatt.sno110.operations.authentication

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.*
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.SNO110_BLUETOOTH_CHARACTERISTIC_GATT_SERVICE_CHANGED
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.SNO110_BLUETOOTH_SERVICE_GATT
import java.util.*

open class Sno110EnableServiceChangedIndicationOperation(
) : GattOperation<Boolean>() {
  override fun performAsync(
    connection: GattConnection,
    callback: GattCallback<Boolean>
  ) {
    super.performAsync(connection, callback)

    val authenticateCharacteristic = connection.getCharacteristic(
      UUID.fromString(SNO110_BLUETOOTH_SERVICE_GATT),
      UUID.fromString(SNO110_BLUETOOTH_CHARACTERISTIC_GATT_SERVICE_CHANGED)
    )

    if (!connection.setCharacteristicNotification(authenticateCharacteristic, true)) {
      completeWithError(GattErrorCode.GattMethodFailed)
      return
    }

    val descriptor = authenticateCharacteristic?.descriptors?.first()

    if (null == descriptor) {
      completeWithError(GattErrorCode.GattError)
      return
    }

    descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE

    connection.writeDescriptor(descriptor)
  }

  override fun onDescriptorWrite(
    gatt: BluetoothGatt?,
    descriptor: BluetoothGattDescriptor?,
    status: Int
  ) {
    super.onDescriptorWrite(gatt, descriptor, status)

    if (status == GattStatus.GATT_SUCCESS) {
      completeWithData(true)
    } else {
      completeWithError(GattErrorCode.GattMethodFailed, status)
    }
  }
}
