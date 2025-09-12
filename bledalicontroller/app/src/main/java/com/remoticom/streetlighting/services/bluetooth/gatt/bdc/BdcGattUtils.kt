package com.remoticom.streetlighting.services.bluetooth.gatt.bdc

import android.bluetooth.*
import com.remoticom.streetlighting.services.bluetooth.utilities.and
import java.util.*

fun BluetoothGattService.getBdcCharacteristic(
  service: String,
  characteristic: Long,
  macAddress: Long
): BluetoothGattCharacteristic? {
  return this.getCharacteristic(
    createCharacteristicUUID(
      UUID.fromString(service),
      characteristic,
      macAddress
    )
  )
}

fun BluetoothGatt.bdcServiceMatchingMask(serviceMask: String): BluetoothGattService? {
  val serviceToMatchMaskUUID = UUID.fromString(serviceMask)
  val serviceMaskUUID = UUID.fromString(BDC_BLUETOOTH_SERVICE_MASK)

  return this.services.find {
    (it.uuid and serviceMaskUUID) == serviceToMatchMaskUUID
  }
}

val BluetoothGattCharacteristic.serviceMask: UUID
  get() = this.service.uuid and UUID.fromString(BDC_BLUETOOTH_SERVICE_MASK)

val BluetoothGattCharacteristic.applicationId: Long
  get() = this.uuid.leastSignificantBits shr 48


fun createCharacteristicUUID(
  serviceMask: UUID,
  characteristic: Long,
  macAddress: Long
): UUID {
  val mostSignificant = serviceMask.mostSignificantBits
  val leastSignificant = (characteristic shl 48) or macAddress

  return UUID(mostSignificant, leastSignificant)
}
