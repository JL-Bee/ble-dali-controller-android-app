package com.remoticom.streetlighting.ui.nodes.utilities

import android.content.Context
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DiagnosticsVersion
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.Version

fun Version.toText(context: Context?) : String? {
  if (null == context) return null

  build?.let {
    return "${major}.${minor}.${patch}.${it}"
  } ?: return "${major}.${minor}.${patch}"
}
