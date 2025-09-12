package com.remoticom.streetlighting.services.bluetooth.scanner

import android.os.ParcelUuid
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.remoticom.streetlighting.services.bluetooth.data.Device
import com.remoticom.streetlighting.services.bluetooth.data.DeviceScanInfo
import com.remoticom.streetlighting.services.bluetooth.data.DeviceType
import com.remoticom.streetlighting.services.bluetooth.utilities.toDeviceUUID
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class MockScannerService private constructor() : ScannerService {
  data class DeviceImpl(
    override val uuid: String,
    override val address: String,
    override val type: DeviceType,
    override val name: String?,
    override val serviceUuids: List<ParcelUuid>?,
    override val health: Int?,
    override val state: Int?
  ) : Device

  private val _state = MutableLiveData(ScannerService.State())
  override val state: LiveData<ScannerService.State> = _state

  private var serviceState = ScannerService.State()
    set(value) {
      field = value
      _state.postValue(value)
    }

  private fun createMockRDeviceScanInfo(uuid: String, address: String, rssi: Int = 1) = DeviceScanInfo(
    DeviceImpl(
      uuid,
      address,
      DeviceType.Zsc010,
      "Test $address",
      null,
      null,
      null
    ),
    rssi
  )

  var scanJob: Job? = null

  override fun startScan() {
    scanJob = GlobalScope.launch {
      val results: MutableMap<String, DeviceScanInfo> = mutableMapOf()

      serviceState = serviceState.copy(
        isScanning = true,
        results = results.toMap()
      )

      for (i in 0..60) {
        delay(200)
        val address = "00:00:00:00:00:%02x".format(i)
        address.toDeviceUUID()?.let { uuid ->
          results[uuid] = createMockRDeviceScanInfo(uuid, address)
        }
        serviceState = serviceState.copy(
          results = results.toMap()
        )
      }
    }
  }

  override fun stopScan(isTimeout: Boolean) {
    scanJob?.cancel()
    serviceState = serviceState.copy(
      isScanning = false
    )
  }

  companion object {
    private const val TAG = "MockScannerService"

    @Volatile
    private var instance: MockScannerService? = null

    fun getInstance() =
      instance
        ?: synchronized(this) {
          instance
            ?: MockScannerService().also {
              instance = it
            }
        }
  }

}
