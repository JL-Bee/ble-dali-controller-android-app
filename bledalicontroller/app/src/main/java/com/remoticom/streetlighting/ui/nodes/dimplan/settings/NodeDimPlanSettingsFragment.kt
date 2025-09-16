package com.remoticom.streetlighting.ui.nodes.dimplan.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.navArgs
import com.remoticom.streetlighting.CoroutineScopeProvider
import com.remoticom.streetlighting.databinding.FragmentNodeDimPlanSettingsBinding
import com.remoticom.streetlighting.databinding.FragmentNodeSettingsBinding
import com.remoticom.streetlighting.ui.nodes.dimplan.NodeDimPlanListFragmentArgs
import com.remoticom.streetlighting.ui.nodes.settings.NodeSettingsViewModel
import com.remoticom.streetlighting.utilities.InjectorUtils
import kotlin.math.min
import kotlin.math.round

/**
 * A simple [Fragment] subclass.
 * Use the [NodeDimPlanSettingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NodeDimPlanSettingsFragment : Fragment() {

  private val args: NodeDimPlanSettingsFragmentArgs by navArgs()

  private val viewModel: NodeSettingsViewModel by viewModels {
    InjectorUtils.provideNodeSettingsViewModelFactory(
      requireContext(),
      this.activity as CoroutineScopeProvider,
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
    // Inflate the layout for this fragment

    viewModel.selectDimStep(args.dimStepId)

    val binding = FragmentNodeDimPlanSettingsBinding.inflate(inflater, container, false).apply {
      viewmodel = viewModel
      lifecycleOwner = viewLifecycleOwner
    }

    binding.nodeDimPlanSettingsSliderLightLevel.addOnChangeListener { _, value, _ ->
      binding.nodeDimPlanSettingsTextviewLevel.text = String.format("%.0f", value)

      viewModel.state.value?.let {
        it.editedNode?.dimSteps?.get(viewModel.selectedDimStep!!)?.let { dimStep ->
          dimStep.level = value
        }
      }
    }

    binding.nodeDimPlanSettingsTimePicker.setIs24HourView(true)
    binding.nodeDimPlanSettingsTimePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
      viewModel.state.value?.let {
        it.editedNode?.dimSteps?.get(viewModel.selectedDimStep!!)?.let { dimStep ->
          dimStep.hour = hourOfDay
          dimStep.minute = minute
        }
      }
    }

    viewModel.state.observe(viewLifecycleOwner) {
      updateLightLevelSliderWithEditedNode(binding, it)
    }

    return binding.root
  }

  private fun updateLightLevelSliderWithEditedNode(
    binding: FragmentNodeDimPlanSettingsBinding,
    state: NodeSettingsViewModel.ViewState
  ) {
    val stepSize = binding.nodeDimPlanSettingsSliderLightLevel.stepSize

    viewModel.selectedDimStep?.let {
      val dimStep = state.editedNode?.dimSteps?.get(it)

      binding.nodeDimPlanSettingsSliderLightLevel.value = dimStep?.level ?: 0.0f
    }


  }

  companion object {
  }
}
