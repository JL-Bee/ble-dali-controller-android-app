package com.remoticom.streetlighting.ui.nodes.settings.write

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.remoticom.streetlighting.data.NodeRepository

class NodeWriteConfirmationViewModelFactory(
  private val repository: NodeRepository,
  private val nodeId: String,
  private val success: Boolean
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return NodeWriteConfirmationViewModel(repository, nodeId, success) as T
  }
}
