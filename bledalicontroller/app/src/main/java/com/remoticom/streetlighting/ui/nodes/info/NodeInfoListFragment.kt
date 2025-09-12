package com.remoticom.streetlighting.ui.nodes.info

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.remoticom.streetlighting.CoroutineScopeProvider
import com.remoticom.streetlighting.R
import com.remoticom.streetlighting.data.Node
import com.remoticom.streetlighting.data.NodeRepository
import com.remoticom.streetlighting.databinding.AlertEditviewBinding
import com.remoticom.streetlighting.databinding.FragmentNodeInfoListBinding
import com.remoticom.streetlighting.services.bluetooth.data.DeviceType
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DiagnosticsStatus
import com.remoticom.streetlighting.services.bluetooth.gatt.bdc.*
import com.remoticom.streetlighting.services.web.data.Peripheral
import com.remoticom.streetlighting.ui.nodes.utilities.toText
import com.remoticom.streetlighting.utilities.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class NodeInfoListFragment : Fragment() {

  private var listener = object : OnListFragmentInteractionListener {
    override fun onListFragmentInteraction(item: NodeInfoListItem?) {
      if (null == item || !item.editable) return

      if (item.type == NodeInfoListItemType.Owner) {
        showUnClaimAlert()
      } else {
        showEditAlert(item)
      }
    }

    private fun showUnClaimAlert() {
      viewModel.state.value?.currentNode?.let { node ->
        node.info?.let { peripheral ->
          context?.let {
            MaterialAlertDialogBuilder(it)
              .setTitle(R.string.node_info_unclaim_node_confirmation_title)
              .setMessage(R.string.node_info_unclaim_node_confirmation_message)
              .setPositiveButton(R.string.node_info_unclaim_node_confirmation_title_positive_button_text) { dialogInterface: DialogInterface?, _ ->
                dialogInterface?.cancel()

                unclaimPeripheral(node.id, peripheral)
              }
              .setNegativeButton(R.string.node_info_unclaim_node_confirmation_title_negative_button_text) { dialogInterface: DialogInterface?, _ ->
                  dialogInterface?.cancel()
              }
              .show()
          }
        }
      }
    }

    private fun showEditAlert(item: NodeInfoListItem) {
      val binding =
        AlertEditviewBinding.inflate(LayoutInflater.from(context)).apply {
          value = item.value
        }

      context?.let {
        MaterialAlertDialogBuilder(it)
          .setTitle(item.label)
          .setView(binding.root)
          .setPositiveButton(
            R.string.node_info_edit_dialog_positive_button_text
          ) { _, _ ->
            Log.d(TAG, "Saving: ${binding.value}")

            when (item.type) {
              NodeInfoListItemType.AssetName -> saveAssetName(binding.value)
              else -> {}
            }
          }
          .setNeutralButton(
            R.string.node_info_edit_dialog_neutral_button_text
          ) { _, _ ->
            Log.d(TAG, "Cancelling")
          }.show()
      }
    }
  }

  private val args: NodeInfoListFragmentArgs by navArgs()

  private val viewModel: NodeInfoListViewModel by viewModels {
    InjectorUtils.provideNodeInfoListViewModelFactory(
      requireContext(),
      this.activity as CoroutineScopeProvider,
      this.activity as FragmentActivity,
      args.nodeId
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setHasOptionsMenu(true)
  }

  private fun createNodeInfoListItem(
    resId: Int,
    value: String?,
    type: NodeInfoListItemType? = null,
    editable: Boolean = false,
    status: NodeInfoListItemStatus = NodeInfoListItemStatus.Regular
  ): NodeInfoListItem = NodeInfoListItem(
      getString(resId),
      value ?: getString(R.string.node_info_null_value),
      type,
      editable,
      status
    )

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val binding =
      FragmentNodeInfoListBinding.inflate(inflater, container, false).apply {
        viewmodel = viewModel
        lifecycleOwner = viewLifecycleOwner
      }

    viewModel.state.observe(viewLifecycleOwner) { newState ->
      val (_, currentNode) = newState

      val softwareVersion = when (currentNode?.deviceType) {
        DeviceType.Zsc010, DeviceType.Bdc -> currentNode?.characteristics?.diagnostics?.version?.firmwareVersion?.toText(context)
        DeviceType.Sno110 -> currentNode?.characteristics?.diagnostics?.version?.libraryVersion?.toText(context)
        else -> null
      }

      val infoListItems: MutableList<NodeInfoListItem> = ArrayList()

      infoListItems.add(
        createNodeInfoListItem(
          R.string.node_info_owner_label,
          currentNode?.info?.owner,
          type = NodeInfoListItemType.Owner,
          editable = true
        )
      )
      infoListItems.add(
        createNodeInfoListItem(
          R.string.node_info_asset_name_label,
          currentNode?.info?.assetName,
          type = NodeInfoListItemType.AssetName,
          editable = true
        )
      )
      infoListItems.add(
        createNodeInfoListItem(
          R.string.node_info_software_version_label,
          softwareVersion
        )
      )
      infoListItems.add(
        createNodeInfoListItem(
          R.string.node_info_time_label,
          if (currentNode?.timeStatus == Node.TimeStatus.NotFixed) currentNode?.timeStatus?.toText(context) else currentNode?.characteristics?.time?.toDisplayString(deviceType = currentNode.deviceType, context = context),
          null,
          false,
          if (currentNode?.timeStatus == Node.TimeStatus.NotFixed) NodeInfoListItemStatus.Error else NodeInfoListItemStatus.Success
        )
      )
      infoListItems.add(
        (createNodeInfoListItem(
          R.string.node_info_diagnostics_label,
          diagnosticsStatusToString(currentNode?.deviceType, currentNode?.characteristics?.diagnostics?.status)
        ))
      )
      if (currentNode?.deviceType == DeviceType.Bdc) {
        val daliMemoryBankItems = memoryBanksInfoItems(currentNode.characteristics?.daliBanks)

        Log.d(TAG, "Adding ${daliMemoryBankItems.count()} items")

        infoListItems.addAll(daliMemoryBankItems)
      }
      binding.list.adapter =
        NodeInfoListRecyclerViewAdapter(infoListItems, listener)
    }

    binding.list.apply {
      adapter = NodeInfoListRecyclerViewAdapter(ArrayList(), listener)
      addItemDecoration(
        DividerItemDecoration(
          context,
          DividerItemDecoration.VERTICAL
        )
      )
    }

    return binding.root
  }

  private fun memoryBanksInfoItems(daliBanks: Map<Int, Map<Int, Any?>>?) : List<NodeInfoListItem> {
    val bank1Items = memoryBank1InfoItems(daliBanks?.get(BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_1))

    val bank202Items = memoryBank202InfoItems(
      daliBanks?.get(BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_202)
    )

    val bank203Items = memoryBank203InfoItems(
      daliBanks?.get(BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_203)
    )

    val bank204Items = memoryBank204InfoItems(
      daliBanks?.get(BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_204)
    )

    val bank205Items = memoryBank205InfoItems(
      daliBanks?.get(BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_205)
    )

    val bank206Items = memoryBank206InfoItems(
      daliBanks?.get(BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_206)
    )

    val bank207Items = memoryBank207InfoItems(
      daliBanks?.get(BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_207)
    )

    return bank1Items +
      bank202Items +
      bank203Items +
      bank204Items +
      bank205Items +
      bank206Items +
      bank207Items
  }

  private fun memoryBank1InfoItems(bankData: Map<Int, Any?>?) : List<NodeInfoListItem> {
    return listOf(
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_1_0x03,
        integerToString(bankData?.get(0x03))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_1_0x09,
        integerToString(bankData?.get(0x09))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_1_0x11,
        integerToString(bankData?.get(0x11))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_1_0x13,
        integerToString(bankData?.get(0x13))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_1_0x14,
        integerToString(bankData?.get(0x14))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_1_0x15,
        wattToString(bankData?.get(0x15))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_1_0x17,
        wattToString(bankData?.get(0x17))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_1_0x19,
        voltageToString(bankData?.get(0x19))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_1_0x1B,
        voltageToString(bankData?.get(0x1B))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_1_0x1D,
        lumenToString(bankData?.get(0x1D))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_1_0x20,
        integerToString(bankData?.get(0x20))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_1_0x21,
        kelvinToString(bankData?.get(0x21))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_1_0x23,
        integerToString(bankData?.get(0x23))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_1_0x24,
        bankData?.get(0x24)?.toString()
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_1_0x3C,
        bankData?.get(0x3C)?.toString()
      ),
    )
  }

  private fun memoryBank202InfoItems(bankData: Map<Int, Any?>?) : List<NodeInfoListItem> {
    return listOf(
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_202_0x03,
        integerToString(bankData?.get(0x03))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_202_0x05,
        wattHourToString(bankData?.get(0x05), factorToScale(bankData?.get(0x04)))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_202_0x0C,
        wattToString(bankData?.get(0x0C), factorToScale(bankData?.get(0x0B)))
      ),
    )
  }

  private fun memoryBank203InfoItems(bankData: Map<Int, Any?>?) : List<NodeInfoListItem> {
    return listOf(
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_203_0x03,
        integerToString(bankData?.get(0x03))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_203_0x05,
        voltAmpereHourToString(bankData?.get(0x05), factorToScale(bankData?.get(0x04)))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_203_0x0C,
        voltAmpereToString(bankData?.get(0x0C), factorToScale(bankData?.get(0x0B)))
      ),
    )
  }

  private fun memoryBank204InfoItems(bankData: Map<Int, Any?>?) : List<NodeInfoListItem> {
    return listOf(
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_204_0x03,
        integerToString(bankData?.get(0x03))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_204_0x05,
        wattHourToString(bankData?.get(0x05), factorToScale(bankData?.get(0x04)))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_204_0x0C,
        wattToString(bankData?.get(0x0C), factorToScale(bankData?.get(0x0B)))
      ),
    )
  }

  private fun memoryBank205InfoItems(bankData: Map<Int, Any?>?) : List<NodeInfoListItem> {
    return listOf(
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x03,
        integerToString(bankData?.get(0x03))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x04,
        secondsToString(bankData?.get(0x04))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x08,
        integerToString(bankData?.get(0x08))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x0B,
        voltageToString(bankData?.get(0x0B), 0.1)
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x0D,
        frequencyToString(bankData?.get(0x0D))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x0E,
        floatToString(bankData?.get(0x0E), 0.01)
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x0F,
        booleanToString(bankData?.get(0x0F))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x10,
        integerToString(bankData?.get(0x10))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x11,
        booleanToString(bankData?.get(0x11))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x12,
        integerToString(bankData?.get(0x12))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x13,
        booleanToString(bankData?.get(0x13))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x14,
        integerToString(bankData?.get(0x14))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x15,
        booleanToString(bankData?.get(0x15))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x16,
        integerToString(bankData?.get(0x16))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x17,
        booleanToString(bankData?.get(0x17))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x18,
        integerToString(bankData?.get(0x18))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x19,
        booleanToString(bankData?.get(0x19))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x1A,
        integerToString(bankData?.get(0x1A))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x1B,
        temperatureToString(bankData?.get(0x1B))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_205_0x1C,
        percentageToString(bankData?.get(0x1C))
      ),
    )
  }

  private fun memoryBank206InfoItems(bankData: Map<Int, Any?>?) : List<NodeInfoListItem> {
    return listOf(
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x03,
        integerToString(bankData?.get(0x03))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x04,
        integerToString(bankData?.get(0x04))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x07,
        integerToString(bankData?.get(0x07))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x0A,
        secondsToString(bankData?.get(0x0A))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x0E,
        secondsToString(bankData?.get(0x0E))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x12,
        voltageToString(bankData?.get(0x12), 0.1)
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x14,
        currentToString(bankData?.get(0x14), 0.001)
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x16,
        booleanToString(bankData?.get(0x16))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x17,
        integerToString(bankData?.get(0x17))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x18,
        booleanToString(bankData?.get(0x18))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x19,
        integerToString(bankData?.get(0x19))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x1A,
        booleanToString(bankData?.get(0x1A))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x1B,
        integerToString(bankData?.get(0x1B))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x1C,
        booleanToString(bankData?.get(0x1C))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x1D,
        integerToString(bankData?.get(0x1D))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x1E,
        booleanToString(bankData?.get(0x1E))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x1F,
        integerToString(bankData?.get(0x1F))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_206_0x20,
        temperatureToString(bankData?.get(0x20))
      ),
    )
  }

  private fun memoryBank207InfoItems(bankData: Map<Int, Any?>?) : List<NodeInfoListItem> {
    return listOf(
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_207_0x03,
        integerToString(bankData?.get(0x03))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_207_0x04,
        hoursToString(bankData?.get(0x04), 1000)
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_207_0x05,
        integerToString(bankData?.get(0x05))
      ),
      createNodeInfoListItem(
        R.string.node_info_dali_memory_bank_207_0x06,
        integerToString(bankData?.get(0x06), 100)
      ),
    )
  }

  private fun factorToScale(factor: Any?) : Float? {
    if (factor !is Number) return null

    return when (factor) {
      -6L -> 0.000001f
      -5L -> 0.00001f
      -4L -> 0.0001f
      -3L -> 0.001f
      -2L -> 0.01f
      -1L -> 0.1f
      0L -> 1f
      1L -> 10f
      2L -> 100f
      3L -> 1000f
      4L -> 10000f
      5L -> 100000f
      6L -> 1000000f
      else -> null
    }
  }


  override fun onResume() {
    super.onResume()

    // TODO: Prevent start reading when already in progress?!
    viewModel.startReadDaliBanksIfNeeded()
  }

  override fun onPause() {
     viewModel.stopReadDaliBanks()

    super.onPause()
  }

  private fun diagnosticsStatusToString(deviceType: DeviceType?, diagnosticsStatus: DiagnosticsStatus?) : String? {
    if (null == diagnosticsStatus) return null

    if (null == deviceType) return null

    return "${this.context?.errorDescriptionForDiagnosticsStatus(deviceType, diagnosticsStatus)} (${diagnosticsStatus})"
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
    }
    return super.onOptionsItemSelected(item)
  }

  private fun reportProblemWithCurrentNode() {
    viewModel.state.value?.currentNode?.let {
      context?.sendReportProblemWithNodeMail(it)
    }
  }

  private fun saveAssetName(assetName: String?) {
    viewModel.state.value?.currentNode?.let { node ->
      node.info?.let { peripheral ->
        lifecycleScope.launch {
          val newInfo =
            peripheral.copy(assetName = assetName, pin = null, password = null)

          val result = viewModel.updatePeripheralInfo(node.id, newInfo)

          when (result) {
            is NodeRepository.UpdatePeripheralResult.Success -> showSavedAlert()
            is NodeRepository.UpdatePeripheralResult.Failure -> {
              if (result.shouldLogout) {
                showShouldLogoutAlert(
                  R.string.node_info_save_asset_name_error_title,
                  result.error
                )
              } else {
                showErrorAlert(result.error)
              }
            }
          }
        }
      }
    }
  }

  private fun showSavedAlert() {
    context?.let {
      MaterialAlertDialogBuilder(it)
        .setTitle(R.string.node_info_save_asset_name_success_title)
        .setMessage(R.string.node_info_save_asset_name_success_message)
        .setNeutralButton(R.string.node_info_save_asset_name_neutral_button_text) {
            dialogInterface: DialogInterface?, _ -> dialogInterface?.cancel()
        }
        .show()
    }
  }

  private fun unclaimPeripheral(uuid: String, peripheral: Peripheral) {
    lifecycleScope.launch {
      val result = viewModel.unclaimPeripheral(uuid, peripheral)

      when (result) {
        is NodeRepository.UpdatePeripheralResult.Success -> {
          // Seems safest to disconnect directly when unclaim is successful
          viewModel.disconnectNode()

          showUnclaimSuccessAlert()
        }
        is NodeRepository.UpdatePeripheralResult.Failure -> {
          if (result.shouldLogout) {
            showShouldLogoutAlert(R.string.node_info_unclaim_node_error_title, result.error)
          } else {
            showUnclaimErrorAlert(result.error)
          }
        }
      }
    }
  }

  private fun showErrorAlert(error: String?) {
    context?.let {
      MaterialAlertDialogBuilder(it)
        .setTitle(R.string.node_info_save_asset_name_error_title)
        .setMessage(getString(R.string.node_info_save_asset_name_error_message_format, error))
        .setNeutralButton(R.string.node_info_save_asset_name_neutral_button_text) {
            dialogInterface: DialogInterface?, _ -> dialogInterface?.cancel()
        }
        .show()
    }
  }

  private fun showUnclaimSuccessAlert() {
    context?.let {
      MaterialAlertDialogBuilder(it)
        .setTitle(R.string.node_info_unclaim_node_success_title)
        .setMessage(R.string.node_info_unclaim_node_success_message)
        .setNeutralButton(R.string.node_info_unclaim_node_success_neutral_button_text) {
            dialogInterface: DialogInterface?, _ ->
            dialogInterface?.cancel()

          if (isAdded) {
            findNavController().popBackStack(R.id.nav_home, false)
          }
        }
        .show()
    }
  }

  private fun showUnclaimErrorAlert(error: String?) {
    context?.let {
      MaterialAlertDialogBuilder(it)
        .setTitle(R.string.node_info_unclaim_node_error_title)
        .setMessage(getString(R.string.node_info_unclaim_node_error_message_format, error))
        .setNeutralButton(R.string.node_info_unclaim_node_error_neutral_button_text) {
            dialogInterface: DialogInterface?, _ -> dialogInterface?.cancel()
        }
        .show()
    }
  }

  /**
   * This interface must be implemented by activities that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other fragments contained in that
   * activity.
   *
   *
   * See the Android Training lesson
   * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
   * for more information.
   */
  interface OnListFragmentInteractionListener {
    // TODO (REFACTOR): Update argument type and name (or inline above?)
    fun onListFragmentInteraction(item: NodeInfoListItem?)
  }

  companion object {
    const val TAG = "NodeInfoListFragment"
  }
}
