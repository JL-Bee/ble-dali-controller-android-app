package com.remoticom.streetlighting.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.remoticom.streetlighting.services.bluetooth.data.Device
import com.remoticom.streetlighting.services.bluetooth.data.DeviceScanInfo
import com.remoticom.streetlighting.services.bluetooth.data.DeviceType
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DeviceCharacteristics
import com.remoticom.streetlighting.services.bluetooth.gatt.ConnectionService
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattConnectionStatus
import com.remoticom.streetlighting.services.bluetooth.scanner.ScannerService
import com.remoticom.streetlighting.services.web.TokenProvider
import com.remoticom.streetlighting.services.web.data.Peripheral
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NodeRepositoryTest {

  @get:Rule
  val instantTaskExecutorRule = InstantTaskExecutorRule()

  @Test
  fun `connected without characteristics is treated as connecting`() {
    val device = TestDevice(uuid = "test-uuid", address = "00:11:22:33:44:55")
    val scannerState = MutableLiveData(ScannerService.State())
    val connectionState = MutableLiveData(ConnectionService.State())

    val repository = createRepository(scannerState, connectionState)

    scannerState.value = ScannerService.State(
      results = mapOf(device.uuid to DeviceScanInfo(device, rssi = -42))
    )

    connectionState.value = ConnectionService.State(
      connectionStatus = GattConnectionStatus.Connected,
      device = device,
      characteristics = null,
      lastGattError = null
    )

    val state = repository.state.value

    requireNotNull(state)
    assertEquals(NodeConnectionStatus.CONNECTING, state.connectionStatus)
    assertEquals(NodeConnectionStatus.CONNECTING, state.connectedNode?.connectionStatus)
  }

  private fun createRepository(
    scannerLiveData: MutableLiveData<ScannerService.State>,
    connectionLiveData: MutableLiveData<ConnectionService.State>
  ): NodeRepository {
    val constructor = NodeRepository::class.java.getDeclaredConstructor(
      ScannerService::class.java,
      ConnectionService::class.java
    )
    constructor.isAccessible = true

    return constructor.newInstance(
      FakeScannerService(scannerLiveData),
      FakeConnectionService(connectionLiveData)
    )
  }

  private class FakeScannerService(
    private val liveData: MutableLiveData<ScannerService.State>
  ) : ScannerService {
    override val state = liveData

    override fun startScan() = Unit

    override fun stopScan(isTimeout: Boolean) = Unit
  }

  private class FakeConnectionService(
    private val liveData: MutableLiveData<ConnectionService.State>
  ) : ConnectionService {
    override val state = liveData

    override suspend fun connect(
      device: Device,
      tokenProvider: TokenProvider,
      peripheral: Peripheral?
    ): Boolean = false

    override suspend fun readCharacteristics(health: Int?, state: Int?) =
      throw UnsupportedOperationException("Not required for test")

    override suspend fun writeCharacteristics(characteristics: DeviceCharacteristics) =
      throw UnsupportedOperationException("Not required for test")

    override suspend fun readDaliBanks() =
      throw UnsupportedOperationException("Not required for test")

    override suspend fun disconnect() =
      throw UnsupportedOperationException("Not required for test")

    override fun isOperationInProgress(): Boolean = false
  }

  private data class TestDevice(
    override val uuid: String,
    override val address: String,
    override val type: DeviceType = DeviceType.Zsc010,
    override val name: String? = null,
    override val serviceUuids: List<android.os.ParcelUuid>? = null,
    override val health: Int? = null,
    override val state: Int? = null
  ) : Device
}
