package com.remoticom.streetlighting.services.bluetooth.gatt.connection

import android.bluetooth.BluetoothGatt
import com.remoticom.streetlighting.services.bluetooth.utilities.and
import java.util.*

fun BluetoothGatt.findMACAddress(mask: String): Long? {
  this.services?.forEach {
    if (it.uuid and UUID.fromString(mask) == UUID.fromString(
        mask
      )
    ) {
      return (it.uuid.leastSignificantBits and 0x0000FFFFFFFFFFFFL)
    }
  }

  return null
}
