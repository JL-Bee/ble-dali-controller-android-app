package com.remoticom.streetlighting.ui.nodes.settings

import com.remoticom.streetlighting.data.Node
import com.remoticom.streetlighting.services.bluetooth.data.DeviceType
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DaliPowerLevel
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DimPreset
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.GeneralMode
import com.remoticom.streetlighting.ui.nodes.settings.utilities.dimLevelToPercentage

class MutableDimStep(
  var hour: Int,
  var minute: Int,
  var level: Float)

class EditableNode(var generalMode : GeneralMode? = null,
                   var dimPreset : DimPreset? = null,
                   var dimSteps : List<MutableDimStep>? = null,
                   var dimLevel : Float? = null,       // Convenience prop
                   var dimPlanningEnabled : Boolean? = null,
                   var daliClo : Boolean? = null,
                   var daliPowerLevel: DaliPowerLevel? = null,
                   var daliFadeTime: Float? = null,
                   var timeTimeZoneUtcOffset : Int? = null,
                   var timeTimeZoneDayLightSavingTimeEnabled : Boolean? = null,
                   var timeMidnightOffset : Float? = null,
) {
  companion object {
    fun presetToNumberOfDimSteps(preset: DimPreset?) : Int {
      if (null == preset || preset == 0) {
        return 1
      }

      return preset
    }

    fun fromNode(node : Node?) : EditableNode? {

      if (null == node) return null

      val numberOfDimSteps = presetToNumberOfDimSteps(node.characteristics?.dim?.preset)

      return EditableNode(
        generalMode = node.characteristics?.general?.mode,
        dimPreset = node.characteristics?.dim?.preset,
        dimSteps = when (node.deviceType) {
          DeviceType.Zsc010, DeviceType.Bdc -> node.characteristics?.dim?.steps?.take(numberOfDimSteps)?.map { MutableDimStep(it.hour, it.minute, dimLevelToPercentage(node.deviceType, it.level, stepSize = DIM_LEVEL_STEP_SIZE).toFloat()) }
          DeviceType.Sno110 -> node.characteristics?.dim?.steps?.map { MutableDimStep(it.hour, it.minute, dimLevelToPercentage(node.deviceType, it.level, stepSize = DIM_LEVEL_STEP_SIZE).toFloat()) }
        },
        dimLevel = when (node.deviceType) {
          // Getting current value from first dim step
          DeviceType.Zsc010 -> node.characteristics?.dim?.steps?.getOrNull(0)?.level?.let { dimLevelToPercentage(node.deviceType, it, stepSize = DIM_LEVEL_STEP_SIZE).toFloat() }
          DeviceType.Sno110, DeviceType.Bdc -> node.characteristics?.dim?.level?.let { dimLevelToPercentage(node.deviceType, it, stepSize = DIM_LEVEL_STEP_SIZE).toFloat() }
        },
        dimPlanningEnabled = when (node.deviceType) {
          DeviceType.Bdc -> null // NOTE: Not using dimPlanningEnabled (convenience prop) for BDC (Uses generalMode)
          DeviceType.Zsc010, DeviceType.Sno110 -> node.characteristics?.dim?.preset?.let { node.characteristics.dim.preset > 0 }
        },
        daliClo = node.characteristics?.dali?.clo,
        daliPowerLevel = node.characteristics?.dali?.powerLevel,
        daliFadeTime = node.characteristics?.dali?.fadeTime?.toFloat(),
        timeTimeZoneUtcOffset = node.characteristics?.time?.timezone?.utcOffset,
        timeTimeZoneDayLightSavingTimeEnabled = node.characteristics?.time?.timezone?.daylightSavingTimeEnabled,
        timeMidnightOffset = node.characteristics?.time?.midnightOffset?.toFloat(),
      )
    }
  }
}
