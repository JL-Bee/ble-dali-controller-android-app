package com.remoticom.streetlighting.ui.nodes.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.remoticom.streetlighting.data.Node
import com.remoticom.streetlighting.data.NodeConnectionStatus
import com.remoticom.streetlighting.data.NodeRepository
import com.remoticom.streetlighting.services.bluetooth.data.DeviceType
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.*
import com.remoticom.streetlighting.services.bluetooth.gatt.bdc.BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_COUNT
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_OUTPUT_RANGE_CONFIG_MIN_PERCENTAGE
import com.remoticom.streetlighting.services.bluetooth.gatt.zsc010.ZSC010_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_COUNT
import com.remoticom.streetlighting.services.configuration.ConfigurationService
import com.remoticom.streetlighting.services.web.data.Peripheral
import com.remoticom.streetlighting.ui.nodes.settings.utilities.dimLevelToPercentage
import kotlinx.coroutines.delay

const val DIM_LEVEL_STEP_SIZE = 5.0f

// TODO (REFACTOR): Similar to NodeInfoListViewModel? Compose viewModel with some 'super' viewModel?
class NodeSettingsViewModel(
  private val nodeRepository: NodeRepository,
  private val nodeId: String,
  private val configurationService: ConfigurationService = ConfigurationService()
) : ViewModel() {

  data class ViewState(
    val connectionStatus: NodeConnectionStatus = NodeConnectionStatus.DISCONNECTED,
    val currentNode: Node? = null,
    val editedNode: EditableNode? = null,
    val currentDimStep: Int? = null,
    val lastConfigurationName: String? = null,
    val isWriting: Boolean = false
  )

  private var editedNode : EditableNode? = null
  private var isWritingInProgress: Boolean = false

  private val _state = MediatorLiveData<ViewState>()
  val state: LiveData<ViewState> = _state

  private val _busy = MediatorLiveData<Boolean>().apply { value = false }
  val busy: LiveData<Boolean> = _busy

  init {
      _state.addSource(nodeRepository.state) {
        _state.value = mergeDataSources(nodeRepository.state, configurationService.state)!!
      }
      _state.addSource(configurationService.state) {
        _state.value = mergeDataSources(nodeRepository.state, configurationService.state)!!
      }

      _busy.addSource(_state) { _busy.value = isBusy() }
      _busy.addSource(nodeRepository.state) { _busy.value = isBusy() }
      _busy.value = isBusy()
  }

  private fun mergeDataSources(
    nodeState: LiveData<NodeRepository.State>,
    configurationState: LiveData<ConfigurationService.State>,
  ): ViewState? {
    if (null == nodeState.value || null == configurationState.value) return ViewState()

    val ( _, scanResults ) = nodeState.value!!
    val ( loadedConfiguration, status, lastConfigurationName ) = configurationState.value!!

    val currentNode = scanResults[nodeId]
    val connectionStatus = currentNode?.connectionStatus ?: NodeConnectionStatus.DISCONNECTED

    if (null == editedNode) {
      editedNode = EditableNode.fromNode(currentNode)
    }

    // Overwrites editedNode with selected configuration
    // changes from node repository will keep this configuration active
    if (null != loadedConfiguration) {
      editedNode = loadedConfiguration
    }

    if (null != editedNode){
      if (null == editedNode?.generalMode && null != currentNode?.characteristics?.general?.mode) {
        editedNode?.generalMode = currentNode.characteristics.general.mode
      }

      if (null == editedNode?.dimPreset && null != currentNode?.characteristics?.dim?.preset) {
        editedNode?.dimPreset = currentNode.characteristics.dim.preset
      }

      if (null == editedNode?.dimSteps && null != currentNode?.characteristics?.dim?.steps && null != currentNode.characteristics.dim.preset) {
        when (currentNode.deviceType) {
          DeviceType.Zsc010, DeviceType.Bdc -> editedNode?.dimSteps = currentNode.characteristics.dim.steps.take(EditableNode.presetToNumberOfDimSteps(currentNode.characteristics.dim.preset)).map {
            MutableDimStep(it.hour, it.minute, dimLevelToPercentage(currentNode.deviceType, it.level, stepSize = DIM_LEVEL_STEP_SIZE).toFloat())
          }
          DeviceType.Sno110 -> editedNode?.dimSteps = currentNode.characteristics.dim.steps.map {
            MutableDimStep(it.hour, it.minute, dimLevelToPercentage(currentNode.deviceType, it.level, stepSize = DIM_LEVEL_STEP_SIZE).toFloat())
          }
        }
      }

      if (null == editedNode?.dimLevel && null != currentNode) {
        when (currentNode.deviceType) {
          DeviceType.Zsc010 -> {
            if (null != currentNode.characteristics?.dim?.steps && currentNode.characteristics.dim.steps.isNotEmpty()) {
              editedNode?.dimLevel = dimLevelToPercentage(
                currentNode.deviceType,
                currentNode.characteristics.dim.steps[0].level,
                stepSize = DIM_LEVEL_STEP_SIZE
              ).toFloat()
            }
          }
          DeviceType.Sno110, DeviceType.Bdc -> {
            if (null != currentNode.characteristics?.dim?.level) {
              editedNode?.dimLevel = dimLevelToPercentage(
                currentNode.deviceType,
                currentNode.characteristics.dim.level,
                stepSize = DIM_LEVEL_STEP_SIZE
              ).toFloat()
            }
          }
        }
      }

      if (null == editedNode?.dimPlanningEnabled && null != currentNode?.characteristics?.dim?.preset) {
        editedNode?.dimPlanningEnabled = when (currentNode.deviceType) {
          DeviceType.Bdc -> null
          DeviceType.Zsc010, DeviceType.Sno110 -> currentNode.characteristics.dim.preset > 0
        }
      }

      if (null == editedNode?.daliClo && null != currentNode?.characteristics?.dali?.clo) {
        editedNode?.daliClo = currentNode.characteristics.dali.clo
      }

      if (null == editedNode?.daliPowerLevel && null != currentNode?.characteristics?.dali?.powerLevel) {
        editedNode?.daliPowerLevel = currentNode.characteristics.dali.powerLevel
      }

      if (null == editedNode?.daliFadeTime && null != currentNode?.characteristics?.dali?.fadeTime) {
        editedNode?.daliFadeTime = currentNode.characteristics.dali.fadeTime.toFloat()
      }

      if (null == editedNode?.timeTimeZoneUtcOffset && null != currentNode?.characteristics?.time?.timezone?.utcOffset) {
        editedNode?.timeTimeZoneUtcOffset = currentNode.characteristics.time.timezone.utcOffset
      }

      if (null == editedNode?.timeTimeZoneDayLightSavingTimeEnabled && null != currentNode?.characteristics?.time?.timezone?.daylightSavingTimeEnabled) {
        editedNode?.timeTimeZoneDayLightSavingTimeEnabled = currentNode.characteristics.time.timezone.daylightSavingTimeEnabled
      }

      if (null == editedNode?.timeMidnightOffset && null != currentNode?.characteristics?.time?.midnightOffset) {
        editedNode?.timeMidnightOffset = currentNode.characteristics.time.midnightOffset.toFloat()
      }
    }

    return ViewState(
      connectionStatus = connectionStatus,
      currentNode = currentNode,
      editedNode = editedNode,
      currentDimStep = selectedDimStep,
      lastConfigurationName = lastConfigurationName,
      isWriting = isWritingInProgress
    )
  }


  suspend fun connectCurrentNode() {
    nodeRepository.connectNode(nodeId)
  }

  suspend fun disconnectCurrentNode() {
    nodeRepository.disconnectNode(nodeId)
  }

  suspend fun updateCurrentNode(characteristics: DeviceCharacteristics): Boolean {
    if (isWritingInProgress || nodeRepository.isOperationInProgress()) {
      return false
    }

    val node = state.value?.currentNode ?: return false

    updateWritingState(true)

    return try {
      nodeRepository.writeCharacteristics(node, characteristics)
    } finally {
      updateWritingState(false)
    }
  }

  private fun updateWritingState(isWriting: Boolean) {
    isWritingInProgress = isWriting

    _state.value?.let { currentState ->
      _state.postValue(currentState.copy(isWriting = isWriting))
    }

    _busy.postValue(isBusy())
  }

  fun isBusy(): Boolean = isWritingInProgress || nodeRepository.isOperationInProgress()

  suspend fun claimPeripheral(uuid: String, peripheral: Peripheral) : NodeRepository.UpdatePeripheralResult {
    Log.d(TAG, "Claiming node/peripheral: $uuid")

    val result = nodeRepository.claimPeripheral(uuid, peripheral)

    when (result) {
      is NodeRepository.UpdatePeripheralResult.Success -> Log.d(
        TAG, "Peripheral updated")
      is NodeRepository.UpdatePeripheralResult.Failure -> Log.e(
        TAG, "Error updating peripheral: ${result.error}")
    }

    return result
  }

  fun addDimStep() {
    val dimSteps = editedNode?.dimSteps

    state.value?.currentNode?.deviceType?.let { deviceType ->
      when (deviceType) {
        DeviceType.Zsc010 -> {
          dimSteps?.let { dimSteps ->

            if (dimSteps.count() < ZSC010_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_COUNT) {
              val newSteps = dimSteps.toMutableList()
              val lastStep = dimSteps.last()

              newSteps.add(MutableDimStep(lastStep.hour, lastStep.minute, lastStep.level))

              editedNode?.dimSteps = newSteps

              // TODO (REFACTOR): Trigger change in state
            }
          }
        }
        DeviceType.Bdc -> {
          dimSteps?.let { dimSteps ->

            if (dimSteps.count() < BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_COUNT) {
              val newSteps = dimSteps.toMutableList()
              val lastStep = dimSteps.last()

              newSteps.add(MutableDimStep(lastStep.hour, lastStep.minute, lastStep.level))

              editedNode?.dimSteps = newSteps

              // TODO (REFACTOR): Trigger change in state
            }
          }
        }
        DeviceType.Sno110 -> {
          dimSteps?.let { dimSteps ->

            // TODO (REQUIREMENTS): SNO110 - Make dynamic based on Sno110ReadSchedulerMaxNumberOfDimStepsOperation?
            if (dimSteps.count() < 5) {
              val newSteps = dimSteps.toMutableList()
              val lastStep = dimSteps.last()

              newSteps.add(MutableDimStep(lastStep.hour, lastStep.minute, lastStep.level))

              editedNode?.dimSteps = newSteps

              // TODO (REFACTOR): Trigger change in state
            }
          }
        }

      }
    }
  }

  fun deleteDimStep () {
    val dimSteps = editedNode?.dimSteps

    dimSteps?.let { dimSteps ->
      if (dimSteps.count() > 1) {
        val newSteps = dimSteps.toMutableList()

        newSteps.removeAt(newSteps.size - 1)

        editedNode?.dimSteps = newSteps

        // TODO (REFACTOR): Trigger change in state
      }
    }
  }

  var selectedDimStep : Int? = null

  fun selectDimStep(id: Int) {
    selectedDimStep = id
  }

  // Used by node settings fragment to avoid slider being out range
  fun safeDefaultDimLevelForDeviceType(deviceType: DeviceType) : Float {
    return when (deviceType) {
      DeviceType.Sno110 -> SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_OUTPUT_RANGE_CONFIG_MIN_PERCENTAGE.toFloat()
      DeviceType.Zsc010 -> 0.0f
      DeviceType.Bdc -> 0.0f
    }
  }

  fun saveConfiguration(context: Context?, name: String) {
    Log.d(TAG, "Save config in vm")

    context?.let {
      editedNode?.let {
        // TODO: Or without local editedNode ?
        //  E.g. moved editedNode to configurationService?
        configurationService.saveConfiguration(context, it, name)
      }
    }
  }

  fun loadConfiguration(context: Context?, name: String) {
    context?.let {
      configurationService.loadConfiguration(context, name)
    }
  }

  fun listConfigurations(context: Context?) : List<String> {
    return context?.let {
      configurationService.listConfigurations(context)
    } ?: emptyList()
  }

  fun clearConfigurations(context: Context?) {
    context?.let {
      configurationService.clearConfigurations(context)
    }
  }

  companion object {
    private const val TAG = "NodeSettingsVM"
  }
}
