package com.remoticom.streetlighting.services.bluetooth.scanner

import android.os.ParcelUuid
import androidx.lifecycle.LiveData
import com.remoticom.streetlighting.services.bluetooth.data.DeviceScanInfo

interface ScannerService {
  val state: LiveData<State>

  data class State(
    val isScanning: Boolean = false,
    val results: Map<String, DeviceScanInfo> = mapOf(),
    val errorCode: Int? = null,
    val hasTimedOutWithoutResults: Boolean = false
  )

  fun startScan()
  fun stopScan(isTimeout: Boolean = false)
}
