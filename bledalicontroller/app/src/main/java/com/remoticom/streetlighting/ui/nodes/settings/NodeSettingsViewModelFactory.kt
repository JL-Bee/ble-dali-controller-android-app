package com.remoticom.streetlighting.ui.nodes.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.remoticom.streetlighting.data.NodeRepository

class NodeSettingsViewModelFactory(
  private val repository: NodeRepository,
  private val nodeId: String
) : ViewModelProvider.Factory {
  private var currentNodeSettingsViewModel : NodeSettingsViewModel? = null

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    currentNodeSettingsViewModel?.let {
      return it as T
    }

    currentNodeSettingsViewModel = NodeSettingsViewModel(repository, nodeId)

    return currentNodeSettingsViewModel as T
  }
}
