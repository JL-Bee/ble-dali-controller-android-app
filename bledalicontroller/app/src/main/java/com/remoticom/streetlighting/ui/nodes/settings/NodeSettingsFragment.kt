package com.remoticom.streetlighting.ui.nodes.settings

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.remoticom.streetlighting.CoroutineScopeProvider

import kotlinx.serialization.*
import kotlinx.serialization.json.*

import com.remoticom.streetlighting.R
import com.remoticom.streetlighting.data.Node
import com.remoticom.streetlighting.data.NodeConnectionStatus
import com.remoticom.streetlighting.data.NodeRepository
import com.remoticom.streetlighting.databinding.AlertEditviewBinding
import com.remoticom.streetlighting.ui.nodes.settings.data.DimmingScheme
import com.remoticom.streetlighting.databinding.FragmentNodeSettingsBinding
import com.remoticom.streetlighting.services.bluetooth.data.DeviceType
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.*
import com.remoticom.streetlighting.services.bluetooth.gatt.bdc.BDC_BLUETOOTH_CHARACTERISTIC_DIM_PRESET_PLAN_DISABLED
import com.remoticom.streetlighting.services.bluetooth.gatt.bdc.BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_COUNT
import com.remoticom.streetlighting.services.bluetooth.gatt.zsc010.ZSC010_BLUETOOTH_CHARACTERISTIC_DIM_PRESET_PLAN_DISABLED
import com.remoticom.streetlighting.services.bluetooth.gatt.zsc010.ZSC010_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_COUNT
import com.remoticom.streetlighting.ui.nodes.settings.data.Mode
import com.remoticom.streetlighting.ui.nodes.settings.data.PowerLevel
import com.remoticom.streetlighting.ui.nodes.settings.data.TimeZoneItem
import com.remoticom.streetlighting.services.bluetooth.utilities.filterToValidPowerLevels
import com.remoticom.streetlighting.services.web.data.OwnershipStatus
import com.remoticom.streetlighting.ui.nodes.NodeListFragment
import com.remoticom.streetlighting.ui.nodes.info.NodeInfoListFragment
import com.remoticom.streetlighting.ui.nodes.settings.utilities.fadeTimeValueToSeconds
import com.remoticom.streetlighting.ui.nodes.settings.utilities.percentageToDimLevel
import com.remoticom.streetlighting.ui.nodes.utilities.showErrorAlert
import com.remoticom.streetlighting.utilities.InjectorUtils
import com.remoticom.streetlighting.utilities.sendReportProblemWithNodeMail
import com.remoticom.streetlighting.utilities.showShouldLogoutAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import java.text.DecimalFormat
import kotlin.math.roundToInt

class NodeSettingsFragment : Fragment() {

  companion object {
    private const val TAG = "NodeSettingsFragment"
  }

  private lateinit var mainScope: CoroutineScope

  private val args: NodeSettingsFragmentArgs by navArgs()

  private val viewModel: NodeSettingsViewModel by viewModels {
    InjectorUtils.provideNodeSettingsViewModelFactory(
      requireContext(),
      this.activity as CoroutineScopeProvider,
      args.nodeId
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    mainScope = (this.activity as CoroutineScopeProvider).provideScope()

    val binding =
      FragmentNodeSettingsBinding.inflate(inflater, container, false).apply {
        viewmodel = viewModel
        lifecycleOwner = viewLifecycleOwner
      }

    // Probably easier/better to navigate in fragment than viewmodel
    binding.aboutNodeClickListener = View.OnClickListener {
      findNavController().navigate(
        NodeSettingsFragmentDirections.actionNavNodesettingsToNavNodeinfo(
          args.nodeId
        )
      )
    }

    binding.dimPlanSettingsClickListener = View.OnClickListener {
      findNavController().navigate(
        NodeSettingsFragmentDirections.actionNavNodeSettingsToNavNodeDimPlan(
          args.nodeId,
          args.deviceType
        )
      )
    }

    binding.loadConfigurationClickListener = View.OnClickListener {
      showLoadConfigurationDialog()
    }

    binding.saveConfigurationClickListener = View.OnClickListener {
      showSaveConfigurationDialog()
    }

    binding.toggleConnectionClickListener = View.OnClickListener {
      viewModel.state.value?.currentNode?.let { node ->
        when (node.info?.ownershipStatus) {
          OwnershipStatus.Unclaimed -> {
            Log.i(NodeListFragment.TAG, "Claiming node...")

            claimNode(node)
          }
          OwnershipStatus.Claimed -> {
            when (node.connectionStatus) {
              NodeConnectionStatus.DISCONNECTED -> connect()
              NodeConnectionStatus.CONNECTING -> {}
              NodeConnectionStatus.CONNECTED -> disconnect()
              NodeConnectionStatus.DISCONNECTING -> {}
            }
          }
          else -> {
            Log.w(NodeListFragment.TAG, "Invalid ownership status")
          }
        }
      }
    }

    binding.writeNodeClickListener = View.OnClickListener {
      val preset = if (viewModel.state.value?.editedNode?.generalMode == GeneralMode.STEP_DIMMING) viewModel.state.value?.editedNode?.dimSteps?.count() else BDC_BLUETOOTH_CHARACTERISTIC_DIM_PRESET_PLAN_DISABLED

      val lightLevel = when (DeviceType.valueOf(args.deviceType)) {
        DeviceType.Zsc010 -> binding.nodeSettingsSliderLightLevel.value.roundToInt()
        // Same as for ZSC010, but ZSC010 does this calc in another place (dim steps):
        DeviceType.Bdc -> percentageToDimLevel(DeviceType.valueOf(args.deviceType), binding.nodeSettingsSliderLightLevel.value.roundToInt())
        DeviceType.Sno110 -> binding.nodeSettingsSliderLightLevel.value.roundToInt()
      }

      val dimSteps = when (DeviceType.valueOf(args.deviceType)) {
        DeviceType.Zsc010 ->
          if (preset == ZSC010_BLUETOOTH_CHARACTERISTIC_DIM_PRESET_PLAN_DISABLED)
            zsc010DimStepsForSingleLevel(lightLevel)
          else
            dimStepsForPlan(viewModel.state.value?.editedNode?.dimSteps)

        DeviceType.Bdc ->
          dimStepsForPlan(viewModel.state.value?.editedNode?.dimSteps)

        DeviceType.Sno110 ->
          dimStepsForPlan(viewModel.state.value?.editedNode?.dimSteps)
      }

      val dimCharacteristics = DimCharacteristics(preset, dimSteps, lightLevel)

      // General
      val mode = (binding.nodeSettingsSpinnerSwitching.selectedItem as Mode?)?.mode
      val generalMode = if (null != mode) GeneralMode.getByValue(mode) else null
      val generalCharacteristics = GeneralCharacteristics(generalMode)

      // Dali
      val clo = binding.nodeSettingsSwitchClo.isChecked
      val powerLevel = (binding.nodeSettingsSpinnerPower.selectedItem as PowerLevel?)?.powerLevelPreset
      val fadeTime = binding.nodeSettingsSliderFadeTime.value.roundToInt()
      val daliCharacteristics = DaliCharacteristics(clo, powerLevel, null, null, fadeTime)

      // Time
      val midnightOffset = binding.nodeSettingsSliderTimeMidnightShift.value.roundToInt()
      val timeCharacteristics = TimeCharacteristics(null, null, midnightOffset)

      val characteristics =
        DeviceCharacteristics(
          generalCharacteristics,
          dimCharacteristics,
          daliCharacteristics,
          timeCharacteristics
        )

      mainScope.launch {
        try {
          val success = viewModel.updateCurrentNode(characteristics)

          if (success) Log.d(TAG, "Node successfully written")
            else Log.w(TAG, "Error writing to node")

          findNavController().navigate(
            NodeSettingsFragmentDirections.actionNavNodeSettingsToNavNodeWriteConfirmation(
              args.nodeId,
              success
            )
          )

        } finally {
        }
      }
    }

    context?.let { context ->

      // TODO (REFACTOR): Load JSON earlier? onCreate?

//      val powerLevels = loadPowerLevels()
      val powerLevelAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, mutableListOf<PowerLevel>())

      val schemes = loadSchemes()
      val schemeAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, schemes)

      val deviceType = DeviceType.valueOf(args.deviceType)

      val modes = when (deviceType) {
        DeviceType.Zsc010 -> loadZsc010Modes()
        DeviceType.Bdc -> loadBdcModes()
        DeviceType.Sno110 -> loadSno110Modes()
      }

      val modeAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, modes)

      val timeZones = loadTimeZones()
      val timeZoneAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, timeZones)

      binding.nodeSettingsSwitchClo.setOnCheckedChangeListener { buttonView, isChecked ->
        viewModel.state.value?.let {
          // Update of property (like CLO) on editedNode does not trigger a state update
          // And it's not possible to combine a two-way data binding with an checkedChangeListener,
          // so we update the editedNode CLO and the spinner here
          // (to display list with -5% when CLO is enabled)
          it.editedNode?.daliClo = isChecked

          updatePowerLevelSpinnerWithState(binding.nodeSettingsSpinnerPower, powerLevelAdapter, it, true)
        }
      }

      binding.nodeSettingsSliderFadeTime.addOnChangeListener { _, value, _ ->
        viewModel.state.value?.let {
          it.editedNode?.daliFadeTime = value

          updateFadeTimeValueTextWithEditedNode(binding, it)
        }
      }

      binding.nodeSettingsSliderLightLevel.addOnChangeListener { _, value, _ ->
        viewModel.state.value?.let {
          if (null != it.editedNode?.dimLevel) {
            it.editedNode.dimLevel = value
          }

          updateLightLevelValueTextWithEditedNode(binding, it)
        }
      }

      binding.nodeSettingsSliderTimeMidnightShift.addOnChangeListener { _, value, _ ->
        viewModel.state.value?.let {
          if (null != it.editedNode?.timeMidnightOffset) {
            it.editedNode.timeMidnightOffset = value
          }

          updateTimeMidnightOffsetValueTextWithEditedNode(binding, it)
        }
      }

      // TODO: BDC?!
//      binding.nodeSettingsSwitchDimPlan.setOnCheckedChangeListener { buttonView, isChecked ->
//        viewModel.state.value?.let {
//          it.editedNode?.dimPlanningEnabled = isChecked
//
//          it.editedNode?.dimPreset = when (DeviceType.valueOf(args.deviceType)) {
//            DeviceType.Zsc010 -> if (isChecked) it.editedNode?.dimSteps?.count() else ZSC010_BLUETOOTH_CHARACTERISTIC_DIM_PRESET_PLAN_DISABLED
//            DeviceType.Bdc -> if (isChecked) it.editedNode?.dimSteps?.count() else BDC_BLUETOOTH_CHARACTERISTIC_DIM_PRESET_PLAN_DISABLED
//            DeviceType.Sno110 -> if (isChecked) it.editedNode?.dimSteps?.count() else SNO110_PROXY_DIM_PRESET_PLAN_DISABLED
//          }
//
//
//          updateFadeTimeVisibilityWithEditedNode(binding, it)
//          updateDimPlanSettingsButtonWithEditedNode(binding, it)
//        }
//      }

      viewModel.state.observe(viewLifecycleOwner) {
        updateSwitchStatusWithEditedNode(binding, it)
//        updateDimPlanSwitchWithEditedNode(binding, it)

        updateDimPlanSettingsButtonWithEditedNode(binding, it)

        updateFadeTimeValueTextWithEditedNode(binding, it)
        updateFadeTimeSliderWithEditedNode(binding, it)

        updateLightLevelValueTextWithEditedNode(binding, it)
        updateLightLevelSliderWithEditedNode(binding, it)

        updateTimeMidnightOffsetValueTextWithEditedNode(binding, it)
        updateTimeMidnightOffsetSliderWithEditedNode(binding, it)

        // TODO (FUTURE): Refactor
        updateModeSpinnerWithState(binding.nodeSettingsSpinnerSwitching, modeAdapter, it)
        updatePowerLevelSpinnerWithState(binding.nodeSettingsSpinnerPower, powerLevelAdapter, it)
        updateSchemeSpinnerWithState(binding.nodeSettingsSpinnerScheme, schemeAdapter, it)
        // updateTimeZoneSpinnerWithState(binding.nodeSettingsSpinnerTimezoneOffset, timeZoneAdapter, it)
      }

      // DALI > PowerLevel (0-6)
      binding.nodeSettingsSpinnerPower.apply {

        adapter = powerLevelAdapter
        setSelection(0, false)

        this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
          override fun onNothingSelected(parent: AdapterView<*>?) {
            Log.d(TAG, "Nothing selected")
          }

          override fun onItemSelected(
            parent: AdapterView<*>,
            view: View?,
            position: Int,
            id: Long
          ) {
            if (position < 0) return

            val adapter = parent.adapter as? ArrayAdapter<PowerLevel>

            val powerLevel = adapter?.getItem(position)

            viewModel.state.value?.editedNode?.daliPowerLevel = powerLevel?.powerLevelPreset

            Log.d(TAG, "Power level selected: powerLevel=${powerLevel?.powerLevelPreset}")
          }
        }
      }

      // DIM > DimPlanning (0-14)
      binding.nodeSettingsSpinnerScheme.apply {

        adapter = schemeAdapter
        setSelection(0, false)

        this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
          override fun onNothingSelected(parent: AdapterView<*>?) {
            Log.d(TAG, "Nothing selected")
          }

          override fun onItemSelected(
            parent: AdapterView<*>,
            view: View?,
            position: Int,
            id: Long
          ) {
            if (position < 0) return

            val adapter = parent.adapter as? ArrayAdapter<DimmingScheme>

            val scheme = adapter?.getItem(position)

            // TODO (SNO110): check dimSteps?
            viewModel.state.value?.editedNode?.dimPreset = scheme?.preset

            Log.d(TAG, "Dim preset selected: planning=${scheme?.preset}")
          }
        }
      }

      // GENERAL > Mode (0=dim,1=astro,2=lux)
      binding.nodeSettingsSpinnerSwitching.apply {

        adapter = modeAdapter
        setSelection(0, false)

        this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
          override fun onNothingSelected(parent: AdapterView<*>?) {
            Log.d(TAG, "Nothing selected")
          }

          override fun onItemSelected(
            parent: AdapterView<*>,
            view: View?,
            position: Int,
            id: Long
          ) {
            if (position < 0) return

            val adapter = parent.adapter as ArrayAdapter<Mode>

            val mode = adapter.getItem(position)

            viewModel.state.value?.editedNode?.generalMode = if (null != mode?.mode) { GeneralMode.getByValue(mode.mode) } else { null }

            // TODO: Should be refactored when need to support ZSC/SNO (use ZSC constants, etc.)
            viewModel.state.value?.editedNode?.dimPreset = if (viewModel.state.value?.editedNode?.generalMode == GeneralMode.STEP_DIMMING) viewModel.state.value?.editedNode?.dimSteps?.count() else BDC_BLUETOOTH_CHARACTERISTIC_DIM_PRESET_PLAN_DISABLED

            viewModel.state.value?.let {
              updateLightLevelSliderWithEditedNode(binding, it)
              updateDimPlanSettingsButtonWithEditedNode(binding, it)
            }

            Log.d(TAG, "Mode selected: mode=${mode?.mode}")
          }
        }
      }

//      binding.nodeSettingsSpinnerTimezoneOffset.apply {
//
//        adapter = timeZoneAdapter
//        setSelection(0, false)
//
//        this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//
//          override fun onNothingSelected(parent: AdapterView<*>?) {
//            Log.d(TAG, "Nothing selected")
//          }
//
//          override fun onItemSelected(
//            parent: AdapterView<*>,
//            view: View?,
//            position: Int,
//            id: Long
//          ) {
//            if (position < 0) return
//
//            if (null == viewModel.state.value?.editedNode?.timeTimeZoneUtcOffset) return
//
//            val adapter = parent.adapter as ArrayAdapter<TimeZoneItem>
//
//            val item = adapter.getItem(position)
//
//            viewModel.state.value?.editedNode?.timeTimeZoneUtcOffset = item?.offset
//
//            Log.d(TAG, "Time zone selected: offset=${item?.offset}")
//          }
//        }
//      }
    }

    return binding.root
  }

  private fun zsc010DimStepsForSingleLevel(lightLevel : Int) : List<DimStep>? {
    val deviceType = DeviceType.valueOf(args.deviceType)

    return List(ZSC010_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_COUNT) {
      DimStep(it, 0, percentageToDimLevel(deviceType, lightLevel))
    }
  }

  private fun bdcDimStepsForSingleLevel(lightLevel : Int) : List<DimStep>? {
    val deviceType = DeviceType.valueOf(args.deviceType)

    return List(BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_COUNT) {
      DimStep(it, 0, percentageToDimLevel(deviceType, lightLevel))
    }
  }

  private fun dimStepsForPlan(mutableDimSteps : List<MutableDimStep>?) : List<DimStep>? {
    if (null == mutableDimSteps) return null

    val deviceType = DeviceType.valueOf(args.deviceType)

    val dimSteps = mutableDimSteps.map {
      DimStep(it.hour, it.minute, percentageToDimLevel(deviceType, it.level.toInt()))
    }.toMutableList()

    // Filling to 5 steps (preset knows number of original steps)
    if (deviceType == DeviceType.Zsc010 || deviceType == DeviceType.Bdc) {
      val additionalSteps =
        ZSC010_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_COUNT - dimSteps.count()

      val lastDimStep = dimSteps.last()

      for (i in 0 until additionalSteps) {
        dimSteps.add(
          DimStep(
            hour = lastDimStep.hour,
            minute = lastDimStep.minute,
            level = lastDimStep.level
          )
        )
      }
    }

    return dimSteps
  }

  private fun updateSwitchStatusWithEditedNode(
    binding: FragmentNodeSettingsBinding,
    it: NodeSettingsViewModel.ViewState
  ) {
    binding.nodeSettingsSwitchClo.isChecked = (it.editedNode?.daliClo == true)
  }

  private fun updateFadeTimeValueTextWithEditedNode(
    binding: FragmentNodeSettingsBinding,
    it: NodeSettingsViewModel.ViewState
  ) {
    binding.nodeSettingsTextviewFadeTime.text = DecimalFormat("#.##").format(fadeTimeValueToSeconds(it.editedNode?.daliFadeTime))
  }



  private fun updateFadeTimeSliderWithEditedNode(
    binding: FragmentNodeSettingsBinding,
    it: NodeSettingsViewModel.ViewState
  ) {
    binding.nodeSettingsSliderFadeTime.value = it.editedNode?.daliFadeTime ?: 0.0f
  }

  private fun updateLightLevelValueTextWithEditedNode(
    binding: FragmentNodeSettingsBinding,
    it: NodeSettingsViewModel.ViewState
  ) {
    binding.nodeSettingsTextviewLightLevel.text = "${it.editedNode?.dimLevel?.roundToInt() ?: 0}"
  }

  private fun updateLightLevelSliderWithEditedNode(
    binding: FragmentNodeSettingsBinding,
    it: NodeSettingsViewModel.ViewState
  ) {
    it.currentNode?.deviceType?.let { deviceType ->
      binding.nodeSettingsSliderLightLevel.value = it.editedNode?.dimLevel ?: viewModel.safeDefaultDimLevelForDeviceType(deviceType)
      binding.nodeSettingsSliderLightLevel.isEnabled = (it.editedNode?.generalMode == GeneralMode.NOMINAL && it.currentNode.connectionStatus == NodeConnectionStatus.CONNECTED)
    }
  }

  private fun updateTimeMidnightOffsetValueTextWithEditedNode(
    binding: FragmentNodeSettingsBinding,
    it: NodeSettingsViewModel.ViewState
  ) {
    binding.nodeSettingsTextviewTimeMidnightShift.text = "${it.editedNode?.timeMidnightOffset?.roundToInt() ?: 0}"
  }

  private fun updateTimeMidnightOffsetSliderWithEditedNode(
    binding: FragmentNodeSettingsBinding,
    it: NodeSettingsViewModel.ViewState
  ) {
    binding.nodeSettingsSliderTimeMidnightShift.value = it.editedNode?.timeMidnightOffset ?: 0.0f
  }


  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.node, menu)

    super.onCreateOptionsMenu(menu, inflater)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_node_report_problem -> {
        reportProblemWithCurrentNode()
        return true
      }
      // TODO: Leave menu option available to users (without confirmation?)
      R.id.action_clear_configurations -> {
        viewModel.clearConfigurations(context)
        return true
      }
    }
    return super.onOptionsItemSelected(item)
  }

  private fun connect() {
    mainScope.launch {
      viewModel.connectCurrentNode()
    }
  }

  private fun disconnect() {
    if (viewModel.isBusy()) {
      Toast.makeText(requireContext(), R.string.node_settings_disconnect_busy_message, Toast.LENGTH_SHORT).show()
      return
    }

    mainScope.launch {
      viewModel.disconnectCurrentNode()
    }
  }

  private inline fun <reified T> loadList(filename: String, serializer: KSerializer<List<T>>) : List<T> {
    val text = context?.assets?.open(filename)?.bufferedReader().use {
      it?.readText()
    } ?: return emptyList()

    val json = Json

    return json.decodeFromString(serializer, text)
  }

  private fun loadSchemes() : List<DimmingScheme> {
    return loadList("schemes.json", ListSerializer(DimmingScheme.serializer()))
  }

  private fun loadPowerLevels() : List<PowerLevel> {
    return loadList("power_levels.json", ListSerializer(PowerLevel.serializer()))
  }

  private fun loadZsc010Modes() : List<Mode> {
    return loadList("modes_zsc010.json", ListSerializer(Mode.serializer()))
  }

  private fun loadBdcModes() : List<Mode> {
    return loadList("modes_bdc.json", ListSerializer(Mode.serializer()))
  }

  private fun loadSno110Modes() : List<Mode> {
    return loadList("modes_sno110.json", ListSerializer(Mode.serializer()))
  }

  private fun loadTimeZones() : List<TimeZoneItem> {
    return loadList("timezones.json", ListSerializer(TimeZoneItem.serializer()))
  }

  private fun updateModeSpinnerWithState(spinner: Spinner, adapter: ArrayAdapter<Mode>, state : NodeSettingsViewModel.ViewState) {
    val (_, _, editedNode) = state

    for (position in 0 until adapter.count) {
      val item = adapter.getItem(position)

      if (editedNode?.generalMode?.value == item?.mode) {
        spinner.setSelection(position)
        break
      }
    }
  }

  private fun updatePowerLevelSpinnerWithState(spinner: Spinner, adapter: ArrayAdapter<PowerLevel>, state : NodeSettingsViewModel.ViewState, forceRefresh: Boolean = false) {
    val (_, currentNode, editedNode) = state

    val availablePowerLevels =
      currentNode?.characteristics?.dali?.availablePowerLevels ?: return

    val validPowerLevels = availablePowerLevels.filterToValidPowerLevels()

    // Update available levels
    if (validPowerLevels.count() != adapter.count || forceRefresh) {
      adapter.clear()

      val powerLevels = validPowerLevels.mapIndexed { index, it ->
        if (state.editedNode?.daliClo == true) {
          PowerLevel("${(it * 0.95).roundToInt()}W", index)
        } else {
          PowerLevel("${it}W", index)
        }
      }

      adapter.addAll(powerLevels)
    }

    // Set selected item
    for (position in 0 until adapter.count) {
      val item = adapter.getItem(position)

      if (editedNode?.daliPowerLevel == item?.powerLevelPreset) {
        spinner.setSelection(position)
        break
      }
    }
  }

  private fun updateSchemeSpinnerWithState(spinner: Spinner, adapter: ArrayAdapter<DimmingScheme>, state : NodeSettingsViewModel.ViewState) {
    val (_, _, editedNode) = state

    for (position in 0 until adapter.count) {
      val item = adapter.getItem(position)

      if (editedNode?.dimPreset == item?.preset) {
        spinner.setSelection(position)
        break
      }
    }
  }

//  private fun updateTimeZoneSpinnerWithState(spinner : Spinner, adapter: ArrayAdapter<TimeZoneItem>, state : NodeSettingsViewModel.ViewState) {
//    val (_, _, editedNode) = state
//
//    for (position in 0 until adapter.count) {
//      val item = adapter.getItem(position)
//
//      if (editedNode?.timeTimeZoneUtcOffset == item?.offset) {
//        spinner.setSelection(position)
//        break
//      }
//    }
//  }

  private fun updateDimPlanSettingsButtonWithEditedNode(
    binding: FragmentNodeSettingsBinding,
    it: NodeSettingsViewModel.ViewState
  ) {
    binding.nodeSettingsButtonDimPlanSettings.isEnabled = (it.editedNode?.generalMode == GeneralMode.STEP_DIMMING) && (it.connectionStatus == NodeConnectionStatus.CONNECTED)
  }


  private fun reportProblemWithCurrentNode() {
    viewModel.state.value?.currentNode?.let {
      context?.sendReportProblemWithNodeMail(it)
    }
  }

  private fun claimNode(node: Node?) {
    node?.info?.let {
      mainScope.launch {
        when (val result = viewModel.claimPeripheral(node.id, it)) {
          is NodeRepository.UpdatePeripheralResult.Success -> {
            Log.d(NodeListFragment.TAG, "Node claimed successfully")

            // Visible in UI (button turns into 'connect')
          }
          is NodeRepository.UpdatePeripheralResult.Failure -> {
            Log.d(NodeListFragment.TAG, "Failed to claim node: ${result.error}")
            if (result.shouldLogout) {
              Log.e(NodeListFragment.TAG, "Error claiming node: user should logout and login before trying again")

              showShouldLogoutAlert(R.string.node_list_claim_node_error_title, result.error)

            } else {
              showErrorAlert(result.error)

              Log.e(NodeListFragment.TAG, "Error claiming node: ${result.error}")
            }
          }
        }
      }
    }
  }


  private fun showLoadConfigurationDialog() {
    Log.d(TAG, "Load config!")

    val configurations = viewModel.listConfigurations(context).toTypedArray()

    context?.let { ctx ->
      MaterialAlertDialogBuilder(ctx)
        .setTitle(R.string.node_settings_configuration_load_dialog_title)
        .setNeutralButton(R.string.node_settings_configuration_load_dialog_neutral_button_text) { d, _ -> d.cancel() }
        .setItems(configurations) { d, i ->
          Log.d(TAG, "Selected: $i")

          d.cancel()

          viewModel.loadConfiguration(context, configurations[i])
        }
        .show()
    }
  }

  private fun showSaveConfigurationDialog() {
    context?.let {
      val alertBinding =
        AlertEditviewBinding.inflate(layoutInflater).apply {
          value = viewModel.state.value?.lastConfigurationName ?: ""
        }

      MaterialAlertDialogBuilder(it)
        .setTitle(R.string.node_settings_configuration_save_dialog_title)
        .setView(alertBinding.root)
        .setPositiveButton(
          R.string.node_settings_configuration_save_dialog_positive_button_text
        ) { d, _ ->
          alertBinding.value?.let { name ->
            Log.d(NodeInfoListFragment.TAG, "Saving: $name")

            d.cancel()

            if (name.trim().length >= 1) {
              viewModel.saveConfiguration(context, name.trim())
            }
          }
        }
        .setNeutralButton(
          R.string.node_settings_configuration_save_dialog_neutral_button_text
        ) { d, _ ->
          d.cancel()
        }.show()
    }
  }
}
