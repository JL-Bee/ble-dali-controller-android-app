package com.remoticom.streetlighting.services.bluetooth.gatt

import androidx.lifecycle.LiveData
import com.remoticom.streetlighting.services.bluetooth.data.Device
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DeviceCharacteristics
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattConnectionStatus
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattError
import com.remoticom.streetlighting.services.web.TokenProvider
import com.remoticom.streetlighting.services.web.data.Peripheral

interface ConnectionService {
  val state: LiveData<State>

  data class State(
    val connectionStatus: GattConnectionStatus = GattConnectionStatus.Disconnected,
    val device: Device? = null,
    val characteristics: DeviceCharacteristics? = null,
    val lastGattError: GattError<*>? = null
  )

  suspend fun connect(device: Device, tokenProvider: TokenProvider, peripheral: Peripheral?): Boolean

  suspend fun readCharacteristics(health: Int?, state: Int?): DeviceCharacteristics

  suspend fun writeCharacteristics(characteristics: DeviceCharacteristics): Boolean

  suspend fun readDaliBanks()

  suspend fun disconnect()

  fun isOperationInProgress() : Boolean
}
