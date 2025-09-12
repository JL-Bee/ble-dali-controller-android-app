package com.remoticom.streetlighting.ui.nodes.settings.data

import kotlinx.serialization.Serializable

@Serializable
data class Mode(val name : String? = null, val mode: Int? = null) {

  override fun toString(): String {
    return name ?: ""
  }
}
