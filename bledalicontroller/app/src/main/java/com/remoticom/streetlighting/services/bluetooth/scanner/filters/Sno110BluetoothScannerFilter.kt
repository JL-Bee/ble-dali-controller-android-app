package com.remoticom.streetlighting.services.bluetooth.scanner.filters

import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import com.remoticom.streetlighting.services.bluetooth.scanner.BLUETOOTH_SIGNIFY_SCAN_FILTER_SERVICE_DATA_FIRST_BYTE
import com.remoticom.streetlighting.services.bluetooth.scanner.BLUETOOTH_SIGNIFY_SCAN_FILTER_SERVICE_DATA_SECOND_BYTE
import com.remoticom.streetlighting.services.bluetooth.scanner.BLUETOOTH_SIGNIFY_SCAN_FILTER_SERVICE_UUID

class Sno110BluetoothScannerFilter : BluetoothScannerFilter {
  override fun matches(result: ScanResult?): Boolean {
    val serviceUuid = ParcelUuid.fromString(BLUETOOTH_SIGNIFY_SCAN_FILTER_SERVICE_UUID)

    val parsedData = result?.scanRecord?.serviceData?.get(serviceUuid)

    return matchesPartialData(
      byteArrayOf(
        BLUETOOTH_SIGNIFY_SCAN_FILTER_SERVICE_DATA_FIRST_BYTE,
        BLUETOOTH_SIGNIFY_SCAN_FILTER_SERVICE_DATA_SECOND_BYTE
      ),
      null,
      parsedData)
  }

  private fun matchesPartialData(
    data: ByteArray,
    dataMask: ByteArray?,
    parsedData: ByteArray?
  ): Boolean {
    if (parsedData == null || parsedData.size < data.size) {
      return false
    }
    if (dataMask == null) {
      for (i in data.indices) {
        if (parsedData[i] != data[i]) {
          return false
        }
      }
      return true
    }
    for (i in data.indices) {
      if (dataMask[i].toInt() and parsedData[i].toInt() != dataMask[i].toInt() and data[i].toInt()) {
        return false
      }
    }
    return true
  }
}
