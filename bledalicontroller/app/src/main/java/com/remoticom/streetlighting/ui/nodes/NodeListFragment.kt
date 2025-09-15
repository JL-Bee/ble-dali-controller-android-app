package com.remoticom.streetlighting.ui.nodes

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.remoticom.streetlighting.CoroutineScopeProvider
import com.remoticom.streetlighting.R
import com.remoticom.streetlighting.data.Node
import com.remoticom.streetlighting.data.NodeConnectionStatus
import com.remoticom.streetlighting.data.NodeRepository
import com.remoticom.streetlighting.databinding.FragmentNodeListBinding
import com.remoticom.streetlighting.services.web.data.OwnershipStatus
import com.remoticom.streetlighting.ui.nodes.utilities.showErrorAlert
import com.remoticom.streetlighting.utilities.InjectorUtils
import com.remoticom.streetlighting.utilities.showShouldLogoutAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class NodeListFragment : Fragment() {
  private lateinit var mainScope: CoroutineScope

  private lateinit var binding: FragmentNodeListBinding

  private val viewModel: NodeListViewModel by viewModels {
    InjectorUtils.provideNodeListViewModelFactory(
      requireContext(),
      this.activity as CoroutineScopeProvider,
      this.activity as FragmentActivity
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    mainScope = (this.activity as CoroutineScopeProvider).provideScope()

    binding =
      FragmentNodeListBinding.inflate(inflater, container, false).apply {
        viewmodel = viewModel
        lifecycleOwner = viewLifecycleOwner
      }

    // Reduces flickering list when updating
    binding.nodeListRecyclerview.itemAnimator = null

    context ?: return binding.root

    viewModel.state.observe(viewLifecycleOwner) { state: NodeListViewModel.ViewState ->
      binding.nodeListNoNodesFoundMessageBar.visibility =
        if (state.isScanningTimedOutWithoutResults) View.VISIBLE else View.GONE

      binding.nodeListProgressbar.visibility =
        if (state.isScanning) View.VISIBLE else View.INVISIBLE

      val fabIcon =
        if (state.isScanning) android.R.drawable.ic_media_pause else android.R.drawable.ic_popup_sync
      binding.nodeListFab.setImageResource(fabIcon)
    }

    val adapter = NodeListAdapter(object : OnListFragmentInteractionListener {
      override fun onNodeSelected(item: Node?) {
        viewModel.state.value?.let { state: NodeListViewModel.ViewState->
          if (state.isScanning) {
            viewModel.stopScan()
          } else {
            item?.let {
              Log.d(TAG, "Selected node: ${item.id}")

              selectNode(item)
            }
          }
        }

      }

      override fun onNodeButtonClicked(item: Node?) {
        when (item?.info?.ownershipStatus) {
          OwnershipStatus.Unclaimed -> {
            Log.i(TAG, "Claiming node...")

            claimNode(item)
          }
          OwnershipStatus.Claimed -> {
            when (item.connectionStatus) {
              NodeConnectionStatus.DISCONNECTED -> connectToNode(item)
              NodeConnectionStatus.CONNECTING -> {}
              NodeConnectionStatus.CONNECTED -> disconnectNode(item)
              NodeConnectionStatus.DISCONNECTING -> {}
            }
          }
          else -> {
            Log.w(TAG, "Invalid ownership status")
          }
        }
      }
    })

    // Dividers
    val dividerItemDecoration =
      DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
    binding.nodeListRecyclerview.addItemDecoration(dividerItemDecoration)

    // Adapter + subscription to viewmodel
    binding.nodeListRecyclerview.adapter = adapter
    subscribeAdapter(adapter)

    // Floating action button
    binding.nodeListFab.setOnClickListener {
      viewModel.state.value?.let { state: NodeListViewModel.ViewState ->
        if (state.isScanning) {
          viewModel.stopScan()
        } else {
          viewModel.startScan()
        }
      }
    }

    // Title
    (activity as? AppCompatActivity)?.supportActionBar?.title =
      resources.getString(R.string.node_list_title)

    return binding.root
  }

  override fun onResume() {
    super.onResume()

    // Inform user when app is missing scan permission and do not show scan button
    binding.nodeListInvalidPermissions.visibility = if (!hasScanPermission()) View.VISIBLE else View.GONE
    binding.nodeListFab.visibility = if (hasScanPermission()) View.VISIBLE else View.GONE

    if (viewModel.state.value?.connectedNode == null && hasScanPermission()) {
      viewModel.startScan()
    }
  }

  override fun onPause() {
    super.onPause()

    viewModel.state.value?.let { state: NodeListViewModel.ViewState ->
      if (state.isScanning) {
        viewModel.stopScan()
        runBlocking { viewModel.scanningJob?.join() }
      }
    }
  }

  private fun subscribeAdapter(adapter: NodeListAdapter) {
    viewModel.state.observe(viewLifecycleOwner) { state: NodeListViewModel.ViewState ->
      Log.d(TAG, "Updating list of nodes")

      adapter.submitList(state.nodes)
    }
  }

  private fun selectNode(item: Node) {
    if (item != viewModel.state.value?.connectedNode) return

    // TODO (REFACTOR): Implement proper DI (intention is reuse of NodeSettingsViewModel between Settings and Dim plan screens)

    // Prevent old cached view model from being used:
    InjectorUtils.resetNodeSettingsViewModelFactory()

    val action =
        NodeListFragmentDirections.actionNavHomeToNavNodesettings(item.id, item.deviceType.toString(), item.device.address)

    view?.findNavController()?.navigate(action)
  }

  private fun disconnectNode(node: Node?) {
    node?.let {
      mainScope.launch {
        viewModel.disconnectNode(node)
      }
    }
  }

  private fun connectToNode(node: Node?) {
    node?.let {
      mainScope.launch {
        viewModel.state.value?.let { state: NodeListViewModel.ViewState ->
          if (state.isScanning) {
            viewModel.stopScan()
            viewModel.scanningJob?.join()
          }
        }

        viewModel.connectNode(node)
      }
    }
  }

  private fun claimNode(node: Node?) {
    node?.info?.let {
      mainScope.launch {
        val result = viewModel.claimPeripheral(node.id, it)

        when (result) {
          is NodeRepository.UpdatePeripheralResult.Success -> {
            Log.d(TAG, "Node claimed successfully")

            // Visible in UI (button turns into 'connect')
          }
          is NodeRepository.UpdatePeripheralResult.Failure -> {
            if (result.shouldLogout) {
              Log.e(TAG, "Error claiming node: user should logout and login before trying again")

              showShouldLogoutAlert(R.string.node_list_claim_node_error_title, result.error)

            } else {
              showErrorAlert(result.error)

              Log.e(TAG, "Error claiming node: ${result.error}")
            }
          }
        }
      }
    }
  }

  private fun hasPermissions(context: Context, vararg permissions: String): Boolean = permissions.all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
  }

  private fun hasScanPermission() : Boolean {
    context?.let {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return hasPermissions(
          it,
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.BLUETOOTH,
          Manifest.permission.BLUETOOTH_ADMIN,
        )
      }

      return hasPermissions(
        it,
        Manifest.permission.BLUETOOTH_SCAN,
      )
    }

    return false
  }

  // TODO (REFACTOR): Listener flow might be refactored further
  interface OnListFragmentInteractionListener {
    fun onNodeSelected(item: Node?)

    fun onNodeButtonClicked(item: Node?)
  }

  companion object {
    const val TAG = "NodeListFragment"
  }
}
