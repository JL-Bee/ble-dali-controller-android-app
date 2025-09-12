package com.remoticom.streetlighting.data

import android.os.ParcelUuid
import com.remoticom.streetlighting.services.bluetooth.data.Device
import com.remoticom.streetlighting.services.bluetooth.data.DeviceType
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DeviceCharacteristics
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DiagnosticsStatus
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.SNO110_STATE_FLAG_FIX_POSITION
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.SNO110_STATE_FLAG_FIX_TIME
import com.remoticom.streetlighting.services.web.PeripheralStatus
import com.remoticom.streetlighting.services.web.data.Peripheral

// Only properties in constructor can be modified with copy(..)
data class Node(
  val id: String,
  val device: Device,
  val deviceType: DeviceType,
  var rssi: Int?,
  val localName: String? = "Node${device.address.replace(":", "")}",
  // (first) advertised service UUID is displayed as unique product id:
  val serviceUuids: List<ParcelUuid>?,
  val info: Peripheral?,
  val characteristics: DeviceCharacteristics?,
  val connectionStatus: NodeConnectionStatus = NodeConnectionStatus.DISCONNECTED,
  val peripheralStatus: PeripheralStatus?
) {

  enum class GpsStatus {
    Fixed,
    NotFixed
  }

  enum class TimeStatus {
    Fixed,
    NotFixed
  }

  val gpsStatus: GpsStatus? = diagnosticsStatusToGpsStatus(deviceType, characteristics?.diagnostics?.status)
  val timeStatus: TimeStatus? = diagnosticsStatusToTimeStatus(deviceType, characteristics?.diagnostics?.status)

  val name: String?
    get() {
      if (!info?.assetName.isNullOrEmpty()) return info?.assetName

      return localName
    }

  private fun diagnosticsStatusToGpsStatus(deviceType: DeviceType, status: DiagnosticsStatus?) : GpsStatus? {
    if (null == status) return null

    return when (deviceType) {
      DeviceType.Zsc010 -> if (status >= 64)
          GpsStatus.NotFixed
        else
          GpsStatus.Fixed
      DeviceType.Bdc -> GpsStatus.NotFixed
      DeviceType.Sno110 ->
        if (status and SNO110_STATE_FLAG_FIX_POSITION == 0x00)
          GpsStatus.NotFixed
        else
          GpsStatus.Fixed
    }
  }

  private fun diagnosticsStatusToTimeStatus(deviceType: DeviceType, status: DiagnosticsStatus?) : TimeStatus? {
    if (null == status) return null

    return when (deviceType) {
      DeviceType.Zsc010, DeviceType.Bdc ->
        if (status >= 64)
          TimeStatus.NotFixed
        else
          TimeStatus.Fixed
      DeviceType.Sno110 ->
        if (status and SNO110_STATE_FLAG_FIX_TIME == 0x00)
          TimeStatus.NotFixed
        else
          TimeStatus.Fixed
    }
  }

  override fun toString(): String = "$id ($localName)"
}
