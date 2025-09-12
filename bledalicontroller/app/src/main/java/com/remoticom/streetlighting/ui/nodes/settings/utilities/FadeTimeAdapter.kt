package com.remoticom.streetlighting.ui.nodes.settings.utilities

import android.widget.TextView
import androidx.databinding.BindingAdapter
import java.text.DecimalFormat
import kotlin.math.roundToInt

@BindingAdapter( "app:textWithFadeTimeScaling")
fun textWithFadeTimeScaling(textView: TextView, fadeTime: Float?) {
  textView.text = DecimalFormat("#.##").format(fadeTimeValueToSeconds(fadeTime))
}

fun fadeTimeValueToSeconds(fadeTime: Float?) : Float? {
  if (null == fadeTime) return 0f

  return when(fadeTime.roundToInt()) {
    0 -> 0f
    1 -> 0.5f
    2 -> 1f
    3 -> 1.5f
    4 -> 2f
    5 -> 3f
    6 -> 4f
    7 -> 6f
    8 -> 8f
    9 -> 10f
    10 -> 15f
    11 -> 20f
    12 -> 30f
    13 -> 45f
    14 -> 60f
    15 -> 90f
    else -> null
  }
}
