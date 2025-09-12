package com.remoticom.streetlighting.services.bluetooth.data

import android.os.ParcelUuid

enum class DeviceType {
  Zsc010,
  Bdc,
  Sno110
}

interface Device {
  val uuid: String
  val address: String
  val type: DeviceType
  val name: String?
  val serviceUuids: List<ParcelUuid>?
  val health: Int?
  val state: Int?
}
