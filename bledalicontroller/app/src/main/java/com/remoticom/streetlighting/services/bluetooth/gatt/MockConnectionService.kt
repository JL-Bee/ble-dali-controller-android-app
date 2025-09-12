package com.remoticom.streetlighting.services.bluetooth.gatt

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.remoticom.streetlighting.services.bluetooth.data.Device
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.*
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattConnectionStatus
import com.remoticom.streetlighting.services.web.TokenProvider
import com.remoticom.streetlighting.services.web.data.Peripheral
import kotlinx.coroutines.delay

class MockConnectionService private constructor() :
  ConnectionService {

  private val _state = MutableLiveData(ConnectionService.State())
  override val state: LiveData<ConnectionService.State> = _state

  override suspend fun connect(
    device: Device,
    tokenProvider: TokenProvider,
    peripheral: Peripheral?
  ): Boolean {
    throw NotImplementedError("Mock")
  }

  private var serviceState = ConnectionService.State()
    set(value) {
      Log.d(TAG, "Service state updated with $value")
      field = value
      _state.postValue(value)
    }

  suspend fun connectWithPassword(device: Device, password: Int?): Boolean {
    serviceState = ConnectionService.State(
      connectionStatus = GattConnectionStatus.Connecting,
      device = device
    )

    delay(100)

    serviceState = ConnectionService.State(
      connectionStatus = GattConnectionStatus.Connected,
      device = device
    )

    return true
  }

  override suspend fun readCharacteristics(
    health: Int?,
    state: Int?
  ): DeviceCharacteristics {
    delay(100)

    val characteristics =
      DeviceCharacteristics(
        general = GeneralCharacteristics(
          GeneralMode.DIM
        ),
        dim = DimCharacteristics(
          2
        ),
        dali = DaliCharacteristics(
          clo = false,
          powerLevel = 3
        ),
        diagnostics = DiagnosticsCharacteristics(
          status = 0
        )
      )

    serviceState = serviceState.copy(
      characteristics = characteristics
    )

    return characteristics
  }

  override suspend fun writeCharacteristics(characteristics: DeviceCharacteristics): Boolean {
    delay(200)
    return true
  }

  override suspend fun readDaliBanks() {
    TODO("Not yet implemented")
  }

  override suspend fun disconnect() {
    delay(100)
    serviceState = ConnectionService.State()
  }

  override fun isOperationInProgress(): Boolean {
    return false
  }

  companion object {
    private const val TAG = "MockConnectionService"

    @Volatile
    private var instance: MockConnectionService? = null

    fun getInstance() =
      instance
        ?: synchronized(this) {
          instance
            ?: MockConnectionService().also {
              instance = it
            }
        }
  }
}
