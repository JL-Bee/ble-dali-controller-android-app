package com.remoticom.streetlighting.ui.nodes.settings.write

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.remoticom.streetlighting.data.NodeConnectionError
import com.remoticom.streetlighting.data.NodeRepository

class NodeWriteConfirmationViewModel(
  private val nodeRepository: NodeRepository,
  private val nodeId: String,
  private val success: Boolean
) : ViewModel() {

  data class ViewState(
    val lastConnectionError: NodeConnectionError? = null,
    val status: Boolean? = null
  )

  val state = nodeRepository.state.map { newState ->
    if (null == newState) return@map ViewState()

    ViewState(
      lastConnectionError = newState.lastConnectionError,
      status = success
    )
  }
}
