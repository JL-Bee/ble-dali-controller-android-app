package com.remoticom.streetlighting.ui.nodes

import android.util.Log
import androidx.lifecycle.*
import com.remoticom.streetlighting.data.Node
import com.remoticom.streetlighting.data.NodeConnectionStatus
import com.remoticom.streetlighting.data.NodeRepository
import com.remoticom.streetlighting.services.web.data.Peripheral
import com.remoticom.streetlighting.ui.nodes.info.NodeInfoListViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NodeListViewModel(private val nodeRepository: NodeRepository) :
  ViewModel() {

  private var isTimeout: Boolean = false

  data class ViewState(
    val isScanning: Boolean = false,
    val scanErrorCode: Int? = null,
    val nodes: List<Node> = listOf(),
    val connectionStatus: NodeConnectionStatus = NodeConnectionStatus.DISCONNECTED,
    val connectedNode: Node? = null,
    val isScanningTimedOutWithoutResults: Boolean = false
  )

  val state = nodeRepository.state.map { newState ->
    if (newState == null) return@map ViewState()

    val (
      isScanning,
      scanResults,
      scanErrorCode,
      connectionStatus,
      connectedNode,
      _,
      hasTimedOutWithoutResults
    ) = newState

    ViewState(
      isScanning = isScanning,
      scanErrorCode = scanErrorCode,
      nodes = scanResults.values.sortedByDescending { node ->
        node.rssi
      },
      connectionStatus = connectionStatus,
      connectedNode = connectedNode,
      isScanningTimedOutWithoutResults = hasTimedOutWithoutResults
    )
  }

  var scanningJob : Job? = null

  suspend fun connectNode(node: Node) {
    nodeRepository.connectNode(node)
  }

  suspend fun disconnectNode(node: Node) {
    nodeRepository.disconnectNode(node)
  }

  fun startScan() {
    // Do not start scan when operation is in progress
    // E.g. disconnect operation here will fail because connect operation is active
    if (nodeRepository.isOperationInProgress()) {
      Log.i(TAG, "Operation in progress. Not starting scan.")

      return
    }

    isTimeout = false

    // Make it easier to recover from issues, like peripherals that were not loaded
    nodeRepository.clearPeripheralsCache()

    scanningJob = viewModelScope.launch {

      try {
        nodeRepository.startScan()

        delay(30_000)

        isTimeout = true
      } finally {
        // Make sure scan is stopped upon job cancellation
        // See https://kotlinlang.org/docs/reference/coroutines/cancellation-and-timeouts.html
        nodeRepository.stopScan(isTimeout)
      }
    }
  }

  fun stopScan() {
    scanningJob?.let {
      if (it.isActive) it.cancel()
    }
  }

  suspend fun claimPeripheral(uuid: String, peripheral: Peripheral) : NodeRepository.UpdatePeripheralResult {
    Log.d(TAG, "Claiming node/peripheral: $uuid")

    val result = nodeRepository.claimPeripheral(uuid, peripheral)

    when (result) {
      is NodeRepository.UpdatePeripheralResult.Success -> Log.d(
        NodeInfoListViewModel.TAG, "Peripheral updated")
      is NodeRepository.UpdatePeripheralResult.Failure -> Log.e(
        NodeInfoListViewModel.TAG, "Error updating peripheral: ${result.error}")
    }

    return result
  }

  companion object {
    private const val TAG = "NodeListVM"
  }
}
