package com.remoticom.streetlighting.ui.nodes.info

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.remoticom.streetlighting.data.Node
import com.remoticom.streetlighting.data.NodeConnectionStatus
import com.remoticom.streetlighting.data.NodeRepository
import com.remoticom.streetlighting.services.bluetooth.gatt.bdc.BDC_BLUETOOTH_CHARACTERISTIC_DIAGNOSTICS_STATUS_D4I_ERROR
import com.remoticom.streetlighting.services.web.data.Peripheral
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO (REFACTOR): Similar to NodeSettingsViewModel? Compose viewModel with some 'super' viewModel?
class NodeInfoListViewModel(
  private val nodeRepository: NodeRepository,
  private val nodeId: String
) : ViewModel() {
  data class ViewState(
    val connectionStatus: NodeConnectionStatus = NodeConnectionStatus.DISCONNECTED,
    val currentNode: Node? = null
  )

  var readDaliBanksJob: Job? = null

  val state = nodeRepository.state.map { newState ->
    if (null == newState) return@map ViewState()

    val currentNode = newState.scanResults[nodeId]
    val connectionStatus = currentNode?.connectionStatus ?: NodeConnectionStatus.DISCONNECTED

    ViewState(
      connectionStatus = connectionStatus,
      currentNode = currentNode
    )
  }

  fun startReadDaliBanksIfNeeded() {
    readDaliBanksJob?.cancel()
    readDaliBanksJob = viewModelScope.launch {
      delay(READ_DALI_BANKS_START_DELAY_MS)

      if (!waitForPendingOperations()) {
        Log.i(TAG, "Operation still in progress. Skipping read of DALI banks.")
        return@launch
      }

      val node = state.value?.currentNode
      val characteristics = node?.characteristics
      if (node == null || characteristics == null) {
        Log.i(TAG, "Node characteristics not available. Skipping read of DALI banks.")
        return@launch
      }

      val diagnosticsStatus = characteristics.diagnostics?.status
      if (diagnosticsStatus == null) {
        Log.i(TAG, "Diagnostics status not available. Skipping read of DALI banks.")
        return@launch
      }

      if (diagnosticsStatus and BDC_BLUETOOTH_CHARACTERISTIC_DIAGNOSTICS_STATUS_D4I_ERROR == BDC_BLUETOOTH_CHARACTERISTIC_DIAGNOSTICS_STATUS_D4I_ERROR) {
        Log.i(TAG, "D4I not available (or diagnostics status not loaded). Skipping read of DALI banks.")
        return@launch
      }

      if (nodeRepository.isOperationInProgress()) {
        Log.i(TAG, "Operation resumed before reading DALI banks. Skipping read request.")
        return@launch
      }

      nodeRepository.readDaliBanks(node)
    }
  }

  private suspend fun waitForPendingOperations(): Boolean {
    var attempts = 0
    while (nodeRepository.isOperationInProgress() && attempts < READ_DALI_BANKS_MAX_ATTEMPTS) {
      delay(READ_DALI_BANKS_OPERATION_CHECK_INTERVAL_MS)
      attempts += 1
    }
    return !nodeRepository.isOperationInProgress()
  }

  fun stopReadDaliBanks() {
    readDaliBanksJob?.cancel()
  }

  suspend fun updatePeripheralInfo(uuid: String, peripheral: Peripheral) : NodeRepository.UpdatePeripheralResult {
    val result = nodeRepository.updatePeripheralInfo(uuid, peripheral)

    when (result) {
      is NodeRepository.UpdatePeripheralResult.Success -> Log.d(TAG, "Peripheral updated")
      is NodeRepository.UpdatePeripheralResult.Failure -> Log.e(TAG, "Error updating peripheral: ${result.error}")
    }

    return result
  }

  suspend fun unclaimPeripheral(uuid: String, peripheral: Peripheral) : NodeRepository.UpdatePeripheralResult {
    Log.d(TAG, "Unclaiming node/peripheral: $uuid")

    val result = nodeRepository.unclaimPeripheral(uuid, peripheral)

    when (result) {
      is NodeRepository.UpdatePeripheralResult.Success ->
        Log.d(TAG, "Peripheral updated")
      is NodeRepository.UpdatePeripheralResult.Failure ->
        Log.e(TAG, "Error updating peripheral: ${result.error}")
    }

    return result
  }

  suspend fun disconnectNode() {
    nodeRepository.disconnectNode(nodeId)
  }

  companion object {
    const val TAG = "NodeInfoListVM"
    private const val READ_DALI_BANKS_START_DELAY_MS = 200L
    private const val READ_DALI_BANKS_OPERATION_CHECK_INTERVAL_MS = 100L
    private const val READ_DALI_BANKS_MAX_ATTEMPTS = 10
  }
}
