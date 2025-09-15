package com.remoticom.streetlighting.data

import android.util.Log
import androidx.lifecycle.*
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DeviceCharacteristics
import com.remoticom.streetlighting.services.bluetooth.gatt.ConnectionService
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattConnectionStatus
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattError
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattErrorCode
import com.remoticom.streetlighting.services.bluetooth.scanner.ScannerService
import com.remoticom.streetlighting.services.web.*
import com.remoticom.streetlighting.services.web.data.OwnershipStatus
import com.remoticom.streetlighting.services.web.data.Peripheral
import kotlinx.coroutines.runBlocking

class NodeRepository private constructor (
  private val scannerService: ScannerService,
  private val connectionService: ConnectionService
) : LifecycleObserver {
  data class State(
    val isScanning: Boolean = false,
    val scanResults: Map<String, Node> = mapOf(),
    val scanErrorCode: Int? = null,
    val connectionStatus: NodeConnectionStatus = NodeConnectionStatus.DISCONNECTED,
    val connectedNode: Node?,
    val lastConnectionError: NodeConnectionError? = null,
    val hasTimedOutWithoutResults: Boolean = false
  )

  private val webService = WebService(OkHttpHttpClientFactory(/*AuthenticationProvider("basic", "auth")*/))
  private val peripheralsDataSource = PeripheralsDataSource(webService)

  private val scannerState = scannerService.state
  private val connectionState = connectionService.state
  private val webState = peripheralsDataSource.state

  private val _state = MediatorLiveData<State>()
  val state: LiveData<State> = _state

  init {
    _state.addSource(scannerService.state) {
      _state.value = mergeDataSources(scannerState, connectionState, webState)!!
    }
    _state.addSource(connectionService.state) {
      _state.value = mergeDataSources(scannerState, connectionState, webState)!!
    }
    _state.addSource(peripheralsDataSource.state) {
      _state.value = mergeDataSources(scannerState, connectionState, webState)!!
    }
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  private fun onPause() {
    _state.value?.connectedNode?.let {
      runBlocking {
        Log.d(TAG, "Application on pause. Disconnecting if needed...")
        disconnectNode(it)
      }
    }
  }

  private fun mergeDataSources(
    scannerState: LiveData<ScannerService.State>,
    connectionState: LiveData<ConnectionService.State>,
    webState: LiveData<PeripheralsDataSource.State>
  ): State? {
    if (null == scannerState.value || null == connectionState.value || null == webState.value) return null

    val (isScanning, results, errorCode, hasTimedOutWithoutResults) = scannerState.value!!
    val (connectionStatus, connectedDevice, characteristics, lastGattError) = connectionState.value!!
    val (peripherals) = webState.value!!

    val nodeConnectionStatus = connectionStatus.toNodeConnectionStatus()

    val connectedNode = connectedDevice?.let {
      results[it.uuid]?.let { result ->
        val device = result.device
        Node(
          id = device.uuid,
          localName = device.name,
          serviceUuids = device.serviceUuids,
          deviceType = result.device.type,
          rssi = result.rssi,
          device = device,
          info = peripherals[device.uuid]?.peripheral,
          characteristics = characteristics,
          connectionStatus = nodeConnectionStatus,
          peripheralStatus = peripherals[device.uuid]?.status
        )
      }
    }

    val scanResults = results.mapValues {
      val result = it.value
      val device = result.device
      if (connectedNode?.id == device.uuid)
        connectedNode
      else {
        peripheralsDataSource.loadPeripheralIfNeeded(device.uuid)

        Node(
          id = device.uuid,
          localName = device.name,
          serviceUuids = device.serviceUuids,
          deviceType = device.type,
          rssi = result.rssi,
          device = device,
          info = peripherals[device.uuid]?.peripheral,
          characteristics = characteristics,
          connectionStatus = NodeConnectionStatus.DISCONNECTED,
          peripheralStatus = peripherals[device.uuid]?.status
        )
      }
    }

    return State(
      isScanning = isScanning,
      scanResults = scanResults,
      scanErrorCode = errorCode,
      connectionStatus = nodeConnectionStatus,
      connectedNode = connectedNode,
      lastConnectionError = lastGattError?.toNodeConnectionError(),
      hasTimedOutWithoutResults = hasTimedOutWithoutResults
    )
  }

  private fun lookupNodeById(nodeId: String): Node? {
    return state.value?.scanResults?.get(nodeId)
   }

  suspend fun connectNode(nodeId: String) {
    lookupNodeById(nodeId)?.let {
      connectNode(it)
    }
  }

  @ExperimentalUnsignedTypes
  suspend fun connectNode(node: Node) {
    if (isConnecting()) {
      // Avoid assertion error when connecting to node while still connecting to other
      Log.i(TAG, "Current node in connecting status. Not connecting to other node.")

      return
    }

    if (isOperationInProgress()) {
      // There could still be a read or write operation being in progress;
      // Only proceed when not in progress to avoid assertion error
      Log.i(TAG, "Operation in progress. Not connecting to other node.")

      return
    }

    with(connectionService) {
      if (connect(
          node.device,
          webService,
          node.info
        )) {
        readCharacteristics(node.device.health, node.device.state)
      }
    }
  }

  suspend fun disconnectNode(nodeId: String) {
    lookupNodeById(nodeId)?.let {
      disconnectNode(it)
    }
  }

  suspend fun disconnectNode(node: Node) {
    if (node.connectionStatus != NodeConnectionStatus.CONNECTED) return

    if (isOperationInProgress()) {
      // When (most likely) read of characteristics is in progress;
      // Only proceed when not in progress to avoid assertion error
      Log.i(TAG, "Operation in progress. Not disconnecting.")

      return
    }

    connectionService.disconnect()
  }

  suspend fun writeCharacteristics(node: Node, newCharacteristics: DeviceCharacteristics): Boolean {
    if (node.connectionStatus != NodeConnectionStatus.CONNECTED) return false

    if (node.characteristics == newCharacteristics) return true

    return connectionService.writeCharacteristics(newCharacteristics)
  }

  suspend fun readDaliBanks(node: Node) {
    if (node.connectionStatus != NodeConnectionStatus.CONNECTED) return

    if (isOperationInProgress()) {
      // Only proceed when not in progress to avoid assertion error
      Log.i(TAG, "Operation in progress. Not starting read of DALI banks.")

      return
    }

    connectionService.readDaliBanks()
  }

  fun clearPeripheralsCache() {
    peripheralsDataSource.clearPeripherals()
  }

  suspend fun startScan() {
    if (_state.value?.connectedNode == null) {
      connectionService.disconnect()
    }
    scannerService.startScan()
  }

  fun stopScan(isTimeout: Boolean) {
    scannerService.stopScan(isTimeout)
  }

  suspend fun updatePeripheralInfo(uuid: String, peripheral: Peripheral) : UpdatePeripheralResult {
    return when (val result = peripheralsDataSource.updatePeripheral(uuid, peripheral)) {
      is PeripheralResult.Success -> UpdatePeripheralResult.Success
      is PeripheralResult.Failure -> UpdatePeripheralResult.Failure(result.exception.message, (result.reason == FailureReason.Unauthorized))
    }
  }

  suspend fun claimPeripheral(uuid: String, peripheral: Peripheral) : UpdatePeripheralResult {
    return updatePeripheralInfo(uuid, peripheral.copy(ownershipStatus = OwnershipStatus.Claimed, pin = null, password = null))
  }

  suspend fun unclaimPeripheral(uuid: String, peripheral: Peripheral) : UpdatePeripheralResult {
    return updatePeripheralInfo(uuid, peripheral.copy(ownershipStatus = OwnershipStatus.Unclaimed, pin = null, password = null))
  }

  fun isOperationInProgress() : Boolean {
    return this.connectionService.isOperationInProgress()
  }

  fun isConnecting() : Boolean  {
    return _state.value?.connectionStatus == NodeConnectionStatus.CONNECTING
  }

  sealed class UpdatePeripheralResult {
    object Success : UpdatePeripheralResult()
    data class Failure(
      val error: String?,
      val shouldLogout: Boolean) : UpdatePeripheralResult()
  }

  companion object {
    private const val TAG = "NodeRepository"
    @Volatile
    private var instance: NodeRepository? = null

    fun getInstance(
      scannerService: ScannerService,
      connectionService: ConnectionService
    ) =
      instance ?: synchronized(this) {
        instance ?: NodeRepository(
          scannerService,
          connectionService
        ).also {
          instance = it
        }
      }
  }
}

fun GattConnectionStatus.toNodeConnectionStatus() = when (this) {
  GattConnectionStatus.Disconnected -> NodeConnectionStatus.DISCONNECTED
  GattConnectionStatus.Connected -> NodeConnectionStatus.CONNECTED
  GattConnectionStatus.Connecting -> NodeConnectionStatus.CONNECTING
  GattConnectionStatus.Disconnecting -> NodeConnectionStatus.DISCONNECTING
}

fun GattError<*>.toNodeConnectionError() =  NodeConnectionError(
  code = code.toNodeConnectionErrorCode(),
  reason = status,
  operationIdentifier = operationIdentifier
)

fun GattErrorCode.toNodeConnectionErrorCode() = when(this) {
  GattErrorCode.GattError -> NodeConnectionErrorCode.GattError
  GattErrorCode.GattMethodFailed -> NodeConnectionErrorCode.GattMethodFailed
  GattErrorCode.PreconditionFailed -> NodeConnectionErrorCode.PreconditionFailed
  GattErrorCode.SerializationFailed -> NodeConnectionErrorCode.SerializationFailed
  GattErrorCode.WriteCharacteristicValueMismatch -> NodeConnectionErrorCode.WriteCharacteristicValueMismatch
}
