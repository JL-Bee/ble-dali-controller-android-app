package com.remoticom.streetlighting.ui.nodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.remoticom.streetlighting.data.NodeRepository

class NodeListViewModelFactory(private val repository: NodeRepository) :
  ViewModelProvider.Factory {

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return NodeListViewModel(repository) as T
  }
}
