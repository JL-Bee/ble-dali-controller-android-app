package com.remoticom.streetlighting.ui.nodes.info

import android.util.Log
import com.remoticom.streetlighting.R

public fun NodeInfoListFragment.wattToString(value: Any?, scale: Any? = 1) : String? {
  if (value !is Number) return null

  if (scale !is Number) return null

  return "%.2fW".format(value.toLong() * scale.toFloat())
}

fun NodeInfoListFragment.voltageToString(value: Any?, scale: Any? = 1) : String? {
  if (value !is Number) return null

  if (scale !is Number) return null

  return "%.2fV".format(value.toLong() * scale.toFloat())
}

fun currentToString(value: Any?, scale: Any? = 1) : String? {
  if (value !is Number) return null

  if (scale !is Number) return null

  return "%.3fA".format(value.toLong() * scale.toFloat())
}

public fun NodeInfoListFragment.lumenToString(value: Any?) : String? {
  return value?.let { "${it}lm" }
}

public fun NodeInfoListFragment.kelvinToString(value: Any?) : String? {
  return value?.let { "${it}K" }
}

public fun NodeInfoListFragment.wattPerHourToString(value: Any?, scale: Any? = 1) : String? {
  if (value !is Number) return null

  if (scale !is Number) return null

  return "%.2fW/h".format(value.toLong() * scale.toFloat())
}

public fun NodeInfoListFragment.wattHourToString(value: Any?, scale: Any? = 1) : String? {
  if (value !is Number) return null

  if (scale !is Number) return null

  return "%.2fWh".format(value.toLong() * scale.toFloat())
}

public fun NodeInfoListFragment.voltAmpereToString(value: Any?, scale: Any? = 1) : String? {
  if (value !is Number) return null

  if (scale !is Number) return null

  return "%.2fVA".format(value.toLong() * scale.toFloat())
}

public fun NodeInfoListFragment.voltAmpereHourToString(value: Any?, scale: Any? = 1) : String? {
  if (value !is Number) return null

  if (scale !is Number) return null

  return "%.2fVAh".format(value.toLong() * scale.toFloat())
}

public fun NodeInfoListFragment.integerToString(value: Any?, scale: Any? = 1) : String? {
  Log.d("UTILS", "integerToString(${value}, ${scale})")

  if (value !is Number) return null

  if (scale !is Number) return null

  return value.let { (value.toLong() * scale.toLong()).toString() }
}

fun NodeInfoListFragment.hoursToString(value: Any?, scale: Any? = 1) : String? {
  if (value !is Number) return null

  if (scale !is Number) return null

  return "${value.toLong() * scale.toLong()}h"
}

public fun NodeInfoListFragment.secondsToString(value: Any?) : String? {
  return value?.let { "${it}s" }
}

public fun NodeInfoListFragment.frequencyToString(value: Any?) : String? {
  return value?.let { "${it}Hz" }
}

public fun NodeInfoListFragment.floatToString(value: Any?, scale: Any? = 1) : String? {
  if (value !is Number) return null

  if (scale !is Number) return null

  return "%.2f".format(value.toLong() * scale.toFloat())
}

public fun NodeInfoListFragment.booleanToString(value: Any?, scale: Any? = 1) : String? {
  if (value !is Boolean) return null

  return if (value) this.context?.getString(R.string.node_info_dali_memory_value_boolean_true) else this.context?.getString(R.string.node_info_dali_memory_value_boolean_false)
}

public fun NodeInfoListFragment.temperatureToString(value: Any?): String? {
  Log.d("UTILS", "temperatureToString(${value})")

  if (value !is Number) return null

  return value.let { "${it.toInt() - 60} ℃" }
}

public fun NodeInfoListFragment.percentageToString(value: Any?) : String? {
  return value?.let { "${it}%" }
}
