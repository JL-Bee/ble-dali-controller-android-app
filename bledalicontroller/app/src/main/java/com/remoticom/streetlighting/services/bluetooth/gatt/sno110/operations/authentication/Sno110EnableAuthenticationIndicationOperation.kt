package com.remoticom.streetlighting.services.bluetooth.gatt.sno110.operations.authentication

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.*
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.SNO110_BLUETOOTH_CHARACTERISTIC_SECURITY_AUTHENTICATE
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.SNO110_BLUETOOTH_SERVICE_SECURITY
import java.util.*

open class Sno110EnableAuthenticationIndicationOperation(
) : GattOperation<Boolean>() {
  override fun performAsync(
    connection: GattConnection,
    callback: GattCallback<Boolean>
  ) {
    super.performAsync(connection, callback)

    val authenticateCharacteristic = connection.getCharacteristic(
      UUID.fromString(SNO110_BLUETOOTH_SERVICE_SECURITY),
      UUID.fromString(SNO110_BLUETOOTH_CHARACTERISTIC_SECURITY_AUTHENTICATE)
    )

    if (!connection.setCharacteristicNotification(authenticateCharacteristic, true)) {
      completeWithError(GattErrorCode.GattMethodFailed)
      return
    }

    // Should be 00002902-0000-1000-8000-00805f9b34fb
    // (standard UUID for "Client Characteristic Configuration")
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
