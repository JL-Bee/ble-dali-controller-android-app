package com.remoticom.streetlighting.services.bluetooth.scanner.filters

import android.bluetooth.le.ScanResult

interface BluetoothScannerFilter {
  fun matches(result: ScanResult?) : Boolean
}
