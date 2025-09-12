package com.remoticom.streetlighting.ui.nodes.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.remoticom.streetlighting.data.NodeRepository

class NodeInfoListViewModelFactory(
  private val repository: NodeRepository,
  private val nodeId: String
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return NodeInfoListViewModel(repository, nodeId) as T
  }

}
