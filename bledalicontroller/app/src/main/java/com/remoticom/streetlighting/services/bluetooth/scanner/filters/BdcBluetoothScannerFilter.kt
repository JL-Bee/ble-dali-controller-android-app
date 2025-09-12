package com.remoticom.streetlighting.services.bluetooth.scanner.filters

import android.bluetooth.le.ScanResult
import com.remoticom.streetlighting.services.bluetooth.utilities.and
import java.util.*

private const val BDC_BLUETOOTH_SCAN_FILTER_SERVICE_UUID = "A55D1C55-0088-0000-0000-000000000000"
private const val BDC_BLUETOOTH_SCAN_FILTER_SERVICE_MASK = "FFFFFFFF-FFFF-0000-0000-000000000000"

class BdcBluetoothScannerFilter : BluetoothScannerFilter {

  override fun matches(result: ScanResult?): Boolean {
    if (null == result) return false

    val advertisedServiceUUID = result.scanRecord?.serviceUuids?.first()

    val bdcServiceUUIDMask = UUID.fromString(BDC_BLUETOOTH_SCAN_FILTER_SERVICE_MASK)

    val expectedServiceUUID = UUID.fromString(BDC_BLUETOOTH_SCAN_FILTER_SERVICE_UUID)

    return null != advertisedServiceUUID && ((advertisedServiceUUID.uuid and bdcServiceUUIDMask) == (expectedServiceUUID and bdcServiceUUIDMask))
  }
}
