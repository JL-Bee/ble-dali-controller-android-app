package com.remoticom.streetlighting.ui.nodes.settings.data

import kotlinx.serialization.Serializable

@Serializable
data class PowerLevel(val name : String? = null, val powerLevelPreset: Int? = null) {

  override fun toString(): String {
    return name ?: ""
  }
}
