package com.remoticom.streetlighting.ui.nodes.settings.write

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.remoticom.streetlighting.CoroutineScopeProvider
import com.remoticom.streetlighting.R
import com.remoticom.streetlighting.databinding.FragmentNodeWriteConfirmationBinding

import com.remoticom.streetlighting.utilities.InjectorUtils

class NodeWriteConfirmationFragment : Fragment() {

  private val args: NodeWriteConfirmationFragmentArgs by navArgs()

  private val viewModel: NodeWriteConfirmationViewModel by viewModels {
    InjectorUtils.provideNodeWriteConfirmationViewModelFactory(
      requireContext(),
      this.activity as CoroutineScopeProvider,
      this.activity as FragmentActivity,
      args.nodeId,
      args.success
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val binding = FragmentNodeWriteConfirmationBinding.inflate(inflater, container, false).apply {
      viewmodel = viewModel
      lifecycleOwner = viewLifecycleOwner
      nextClickListener = View.OnClickListener {

        // TODO (REQUIREMENTS?): Disconnect current node?

        if (isAdded) {
          findNavController().popBackStack(R.id.nav_home, false)
        }
      }
    }

    return binding.root
  }
}
