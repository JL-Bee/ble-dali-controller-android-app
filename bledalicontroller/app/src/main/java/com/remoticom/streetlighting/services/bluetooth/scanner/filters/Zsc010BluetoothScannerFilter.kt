package com.remoticom.streetlighting.services.bluetooth.scanner.filters

import android.bluetooth.le.ScanResult
import com.remoticom.streetlighting.services.bluetooth.utilities.and
import java.util.*

const val ZSC010_BLUETOOTH_SCAN_FILTER_SERVICE_UUID = "A55D1C55-004B-0000-0000-000000000000"
const val ZSC010_BLUETOOTH_SCAN_FILTER_SERVICE_MASK = "FFFFFFFF-FFFF-0000-0000-000000000000"

class Zsc010BluetoothScannerFilter : BluetoothScannerFilter {
  override fun matches(result: ScanResult?): Boolean {
    if (null == result) return false

    val advertisedServiceUUID = result.scanRecord?.serviceUuids?.first()
    val zsc010ServiceUUIDMask = UUID.fromString(ZSC010_BLUETOOTH_SCAN_FILTER_SERVICE_MASK)

    val expectedServiceUUID = UUID.fromString(ZSC010_BLUETOOTH_SCAN_FILTER_SERVICE_UUID)

    return null != advertisedServiceUUID && ((advertisedServiceUUID.uuid and zsc010ServiceUUIDMask) == (expectedServiceUUID and zsc010ServiceUUIDMask))
  }
}
