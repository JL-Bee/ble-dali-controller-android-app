package com.remoticom.streetlighting.ui.nodes.settings.utilities

import com.remoticom.streetlighting.services.bluetooth.data.DeviceType
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DimStep
import com.remoticom.streetlighting.ui.nodes.settings.data.DimmingSchemeStep
import java.util.*
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

fun percentageToDimLevel(deviceType: DeviceType, percentage: Int) : Int {
  if (0 == percentage) return 0

  return if (deviceType == DeviceType.Zsc010)
    ((99 / 3) * (log10(percentage.toDouble()) + 1) + 1).roundToInt()
  else
    percentage
}

fun dimLevelToPercentage(deviceType: DeviceType, dimLevel: Int, stepSize: Float) : Int {
  if (0 == dimLevel) return 0

  val percentage = if (deviceType == DeviceType.Zsc010)
      10.0.pow((dimLevel.toDouble() - 1) / (99 / 3) - 1)
    else dimLevel.toDouble()

  val adjustedToStepSize = (percentage / stepSize).roundToInt() * stepSize

  return adjustedToStepSize.roundToInt()
}
