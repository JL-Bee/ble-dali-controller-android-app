package com.remoticom.streetlighting.ui.nodes.dimplan

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.remoticom.streetlighting.CoroutineScopeProvider
import com.remoticom.streetlighting.databinding.FragmentNodeDimPlanListBinding
import com.remoticom.streetlighting.services.bluetooth.data.DeviceType
import com.remoticom.streetlighting.services.bluetooth.gatt.zsc010.ZSC010_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_COUNT
import com.remoticom.streetlighting.ui.nodes.settings.MutableDimStep
import com.remoticom.streetlighting.ui.nodes.settings.NodeSettingsViewModel
import com.remoticom.streetlighting.utilities.InjectorUtils
import kotlinx.coroutines.CoroutineScope

/**
 * A fragment representing a list of Items.
 */
class NodeDimPlanListFragment : Fragment() {

  private var listener = object : OnListFragmentInteractionListener {
    override fun onListFragmentInteraction(item: NodeDimPlanListItem?) {
      if (null == item) return;

      findNavController().navigate(
        NodeDimPlanListFragmentDirections.actionNavNodeDimPlanToNavNodeDimPlanSettings(
          args.nodeId,
          item.id.toInt(10)
        )
      )

    }
  }

  private lateinit var mainScope: CoroutineScope

  private lateinit var binding: FragmentNodeDimPlanListBinding

  private val args: NodeDimPlanListFragmentArgs by navArgs()

  private val viewModel: NodeSettingsViewModel by viewModels {
    InjectorUtils.provideNodeSettingsViewModelFactory(
      requireContext(),
      this.activity as CoroutineScopeProvider,
      this.activity as FragmentActivity,
      args.nodeId
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    binding = FragmentNodeDimPlanListBinding.inflate(inflater, container, false).apply {
      viewmodel = viewModel
      lifecycleOwner = viewLifecycleOwner
    }

    binding.addDimStepClickListener = View.OnClickListener {
      viewModel.addDimStep()

      // TODO (REFACTOR): View should react to state (requires mediator)
      updateListWithDimSteps(viewModel.state.value?.editedNode?.dimSteps)
      updateButtonsWithDimSteps(viewModel.state.value?.editedNode?.dimSteps)
    }

    binding.deleteDimStepClickListener = View.OnClickListener {
      viewModel.deleteDimStep()

      // TODO (REFACTOR): View should react to state (requires mediator)
      updateListWithDimSteps(viewModel.state.value?.editedNode?.dimSteps)
      updateButtonsWithDimSteps(viewModel.state.value?.editedNode?.dimSteps)
    }

    viewModel.state.observe(viewLifecycleOwner) {
      updateListWithDimSteps(it.editedNode?.dimSteps)
      updateButtonsWithDimSteps(it.editedNode?.dimSteps)
    }

    binding.list.apply {
      adapter = NodeDimPlanListRecyclerViewAdapter(emptyList(), listener)
      addItemDecoration(
        DividerItemDecoration(
          context,
          DividerItemDecoration.VERTICAL
        )
      )
    }

    return binding.root
  }

  private fun updateListWithDimSteps(dimSteps: List<MutableDimStep>?) {
    dimSteps?.let {
      binding.list.adapter = NodeDimPlanListRecyclerViewAdapter(dimSteps.mapIndexed { index, dimStep -> NodeDimPlanListItem(index.toString(), dimStep) }, listener)
    }
  }

  private fun updateButtonsWithDimSteps(dimSteps: List<MutableDimStep>?) {
    when (DeviceType.valueOf(args.deviceType)) {
      DeviceType.Zsc010, DeviceType.Bdc -> {
        dimSteps?.let {
          binding.nodeDimPlanButtonAddStep.isEnabled = (dimSteps.size < ZSC010_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_COUNT)
          binding.nodeDimPlanButtonDeleteStep.isEnabled = (dimSteps.size > 1)
        }
      }
      DeviceType.Sno110 -> {
        dimSteps?.let {
          // TODO (SNO110): SNO110 - Make dynamic based on Sno110ReadSchedulerMaxNumberOfDimStepsOperation?
          binding.nodeDimPlanButtonAddStep.isEnabled = (dimSteps.size < 5)
          binding.nodeDimPlanButtonDeleteStep.isEnabled = (dimSteps.size > 1)
        }
      }
    }
  }

  interface OnListFragmentInteractionListener {
    // TODO (REFACTOR): Update argument type and name
    fun onListFragmentInteraction(item: NodeDimPlanListItem?)
  }

  companion object {
    private const val TAG = "NodeDimPlanListFragment"
  }
}
