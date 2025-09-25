package com.remoticom.streetlighting.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.remoticom.streetlighting.data.NodeConnectionStatus
import com.remoticom.streetlighting.services.bluetooth.data.Device
import com.remoticom.streetlighting.services.bluetooth.data.DeviceScanInfo
import com.remoticom.streetlighting.services.bluetooth.data.DeviceType
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DeviceCharacteristics
import com.remoticom.streetlighting.services.bluetooth.gatt.ConnectionService
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattConnectionStatus
import com.remoticom.streetlighting.services.bluetooth.scanner.ScannerService
import com.remoticom.streetlighting.services.web.TokenProvider
import com.remoticom.streetlighting.services.web.data.Peripheral
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

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

  @Test
  fun `disconnect waits for pending write to finish`() = runBlocking {
    val device = TestDevice(uuid = "test-uuid", address = "00:11:22:33:44:55")
    val scannerState = MutableLiveData(ScannerService.State())
    val connectionState = MutableLiveData(ConnectionService.State())

    val connectionService = QueueAwareConnectionService(connectionState)

    val repository = createRepository(scannerState, connectionState, connectionService)

    scannerState.value = ScannerService.State(
      results = mapOf(device.uuid to DeviceScanInfo(device, rssi = -55))
    )

    connectionState.value = ConnectionService.State(
      connectionStatus = GattConnectionStatus.Connected,
      device = device,
      characteristics = DeviceCharacteristics(),
      lastGattError = null
    )

    val initialState = repository.state.getOrAwaitValue()
    val node = requireNotNull(initialState.connectedNode)

    val writeJob = async {
      repository.writeCharacteristics(node, DeviceCharacteristics())
    }

    val writeCompletion = connectionService.awaitWriteRequest()

    repository.disconnectNode(node)

    assertEquals(0, connectionService.disconnectCount)

    val disconnectingState = repository.state.getOrAwaitValue {
      it.connectionStatus == NodeConnectionStatus.DISCONNECTING
    }

    assertEquals(NodeConnectionStatus.DISCONNECTING, disconnectingState.connectionStatus)

    writeCompletion.complete(Unit)
    writeJob.await()

    withTimeout(1_000) {
      connectionService.awaitDisconnect()
    }

    assertEquals(1, connectionService.disconnectCount)

    connectionState.value = connectionState.value?.copy(
      connectionStatus = GattConnectionStatus.Disconnected,
      device = null,
      characteristics = null
    )

    val disconnectedState = repository.state.getOrAwaitValue {
      it.connectionStatus == NodeConnectionStatus.DISCONNECTED
    }

    assertEquals(NodeConnectionStatus.DISCONNECTED, disconnectedState.connectionStatus)
  }

  private fun createRepository(
    scannerLiveData: MutableLiveData<ScannerService.State>,
    connectionLiveData: MutableLiveData<ConnectionService.State>,
    connectionService: ConnectionService = FakeConnectionService(connectionLiveData)
  ): NodeRepository {
    val constructor = NodeRepository::class.java.getDeclaredConstructor(
      ScannerService::class.java,
      ConnectionService::class.java
    )
    constructor.isAccessible = true

    return constructor.newInstance(
      FakeScannerService(scannerLiveData),
      connectionService
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

  private class QueueAwareConnectionService(
    private val liveData: MutableLiveData<ConnectionService.State>
  ) : ConnectionService {
    override val state: LiveData<ConnectionService.State> = liveData

    @Volatile
    private var operationInProgress: Boolean = false

    private val writeRequests = Channel<CompletableDeferred<Unit>>(Channel.UNLIMITED)
    private val disconnectRequests = Channel<Unit>(Channel.UNLIMITED)

    var disconnectCount: Int = 0
      private set

    override suspend fun connect(
      device: Device,
      tokenProvider: TokenProvider,
      peripheral: Peripheral?
    ): Boolean = false

    override suspend fun readCharacteristics(health: Int?, state: Int?) =
      throw UnsupportedOperationException("Not required for test")

    override suspend fun writeCharacteristics(characteristics: DeviceCharacteristics): Boolean {
      val completion = CompletableDeferred<Unit>()
      operationInProgress = true
      writeRequests.send(completion)
      completion.await()
      operationInProgress = false
      return true
    }

    override suspend fun readDaliBanks() =
      throw UnsupportedOperationException("Not required for test")

    override suspend fun disconnect() {
      disconnectCount += 1
      disconnectRequests.send(Unit)
    }

    override fun isOperationInProgress(): Boolean = operationInProgress

    suspend fun awaitWriteRequest(): CompletableDeferred<Unit> = writeRequests.receive()

    suspend fun awaitDisconnect() = disconnectRequests.receive()
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

private fun <T> LiveData<T>.getOrAwaitValue(
  timeout: Long = 2_000,
  predicate: (T) -> Boolean = { true }
): T {
  var data: T? = null
  val latch = CountDownLatch(1)
  val observer = object : Observer<T> {
    override fun onChanged(o: T) {
      data = o
      if (predicate(o)) {
        latch.countDown()
        this@getOrAwaitValue.removeObserver(this)
      }
    }
  }

  observeForever(observer)

  if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
    removeObserver(observer)
    throw TimeoutException("LiveData value was never set.")
  }

  @Suppress("UNCHECKED_CAST")
  return data as T
}
