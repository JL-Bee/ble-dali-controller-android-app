package com.remoticom.streetlighting.ui.nodes.utilities

import android.content.Context
import com.remoticom.streetlighting.R
import com.remoticom.streetlighting.data.Node

fun Node.GpsStatus.toText(context: Context?) : String? {
  if (null == context) return null

  return when (this) {
    Node.GpsStatus.Fixed -> context.getString(R.string.node_info_gps_status_fixed)
    Node.GpsStatus.NotFixed -> context.getString(R.string.node_info_gps_status_not_fixed)
  }
}
