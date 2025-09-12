package com.remoticom.streetlighting.ui.nodes.utilities

import android.content.Context
import com.remoticom.streetlighting.R
import com.remoticom.streetlighting.data.Node

fun Node.TimeStatus.toText(context: Context?) : String? {
  if (null == context) return null

  return when (this) {
    Node.TimeStatus.Fixed -> context.getString(R.string.node_info_time_status_fixed)
    Node.TimeStatus.NotFixed -> context.getString(R.string.node_info_time_status_not_fixed)
  }
}
