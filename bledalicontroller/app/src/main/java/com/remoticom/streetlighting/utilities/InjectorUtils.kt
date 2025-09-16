package com.remoticom.streetlighting.utilities

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.remoticom.streetlighting.data.NodeRepository
// import com.remoticom.streetlighting.services.bluetooth.gatt.MockConnectionService
// import com.remoticom.streetlighting.services.bluetooth.scanner.MockScannerService
import com.remoticom.streetlighting.services.bluetooth.scanner.BluetoothScannerService
import com.remoticom.streetlighting.ui.nodes.NodeListViewModelFactory
import com.remoticom.streetlighting.ui.nodes.info.NodeInfoListViewModelFactory
import com.remoticom.streetlighting.ui.nodes.settings.NodeSettingsViewModelFactory
import com.remoticom.streetlighting.ui.nodes.settings.write.NodeWriteConfirmationViewModelFactory
import com.remoticom.streetlighting.CoroutineScopeProvider
import com.remoticom.streetlighting.services.authentication.AuthenticationService
import com.remoticom.streetlighting.services.bluetooth.gatt.ConnectionService
import com.remoticom.streetlighting.services.bluetooth.gatt.GattConnectionService
import com.remoticom.streetlighting.ui.login.LoginViewModelFactory

object InjectorUtils {

  var currentNodeSettingsViewModelFactory : NodeSettingsViewModelFactory? = null
  fun getNodeRepository(context: Context, scopeProvider: CoroutineScopeProvider): NodeRepository {
    val scope = scopeProvider.provideScope()
    val scannerService = BluetoothScannerService.getInstance(context = context)

    val connectionService: ConnectionService = GattConnectionService.getInstance(
      scope,
      context,
      scannerService)
//    val scannerService = MockScannerService.getInstance()
//    val connectionService = MockConnectionService.getInstance()

    val nodeRepository = NodeRepository.getInstance(scannerService, connectionService)

    nodeRepository.attachToLifecycle(ProcessLifecycleOwner.get().lifecycle)

    return nodeRepository
  }

  fun provideNodeListViewModelFactory(
    context: Context,
    scopeProvider: CoroutineScopeProvider
  ): NodeListViewModelFactory {
    val repository = getNodeRepository(context, scopeProvider)
    return NodeListViewModelFactory(repository)
  }

  fun provideNodeInfoListViewModelFactory(
    context: Context,
    scopeProvider: CoroutineScopeProvider,
    nodeId: String
  ): NodeInfoListViewModelFactory {
    val repository = getNodeRepository(context, scopeProvider)
    return NodeInfoListViewModelFactory(repository, nodeId)
  }

  fun provideNodeSettingsViewModelFactory(
    context: Context,
    scopeProvider: CoroutineScopeProvider,
    nodeId: String
  ): NodeSettingsViewModelFactory {
    currentNodeSettingsViewModelFactory?.let {
      return it
    }

    val repository = getNodeRepository(context, scopeProvider)
    currentNodeSettingsViewModelFactory = NodeSettingsViewModelFactory(repository, nodeId)

    return currentNodeSettingsViewModelFactory!!
  }

  // TODO (REFACTOR): Refactor to use proper DI
  fun resetNodeSettingsViewModelFactory() {
    currentNodeSettingsViewModelFactory = null
  }

  fun provideNodeWriteConfirmationViewModelFactory(
    context: Context,
    scopeProvider: CoroutineScopeProvider,
    nodeId: String,
    success: Boolean
  ): NodeWriteConfirmationViewModelFactory {
    val repository = getNodeRepository(context, scopeProvider)
    return NodeWriteConfirmationViewModelFactory(repository, nodeId, success)
  }

  fun provideLoginViewModelFactory(
  ) : LoginViewModelFactory {
    val authenticationService = AuthenticationService.getInstance()
    return LoginViewModelFactory(authenticationService)
  }
}
