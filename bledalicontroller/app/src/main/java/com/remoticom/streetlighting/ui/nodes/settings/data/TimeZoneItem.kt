package com.remoticom.streetlighting.ui.nodes.settings.data

import kotlinx.serialization.Serializable

@Serializable
data class TimeZoneItem(val name : String? = null, val offset : Int? = null) {

  override fun toString(): String {
    return name ?: ""
  }
}
