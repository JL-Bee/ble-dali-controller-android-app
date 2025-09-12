package com.remoticom.streetlighting.services.bluetooth.utilities

import android.bluetooth.le.ScanResult
import java.nio.ByteBuffer
import java.util.*

// Figure out where the mac address / uuid is located in manufacturer specific data in new firmware
// Log.d(TAG, "manufacturer specific data: ${result?.scanRecord?.manufacturerSpecificData?.get(48)?.contentToString()}")

fun ScanResult.toDeviceUUID(): String? = device?.address?.toDeviceUUID()

fun String.toDeviceUUID(): String? {
  val addressHex = replace(":", "")

  if (addressHex.length != 12) return null

  // Convert address to bytes
  val bytes = ByteArray(6)
  for (i in 0 until bytes.count()) {
    bytes[i] = addressHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
  }

  // Convert to long
  val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)

  // Long is longer than 6 bytes (e.g. 8): pad with zeros
  for (i in 0 until Long.SIZE_BYTES - bytes.size) buffer.put(0x00)
  buffer.put(bytes)
  buffer.flip()

  // UUID toString results in lower case string, but implementation might change
  return UUID(0x0L, buffer.long).toString().lowercase(Locale.getDefault())
}
