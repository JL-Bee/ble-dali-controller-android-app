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
import kotlinx.coroutines.*

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
    readDaliBanksJob = viewModelScope.launch {
      state.value?.currentNode?.let { node ->

        node.characteristics?.diagnostics?.status?.let { status ->
          if (status and BDC_BLUETOOTH_CHARACTERISTIC_DIAGNOSTICS_STATUS_D4I_ERROR != BDC_BLUETOOTH_CHARACTERISTIC_DIAGNOSTICS_STATUS_D4I_ERROR) {
            nodeRepository.readDaliBanks(node)
          } else {
            Log.i(TAG, "D4I not available (or diagnostics status not loaded). Skipping read of DALI banks.")
          }
        }
      }
    }
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
  }
}
