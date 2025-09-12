package com.remoticom.streetlighting.services.configuration

import com.remoticom.streetlighting.services.bluetooth.data.characteristics.GeneralMode
import com.remoticom.streetlighting.ui.nodes.settings.EditableNode
import com.remoticom.streetlighting.ui.nodes.settings.MutableDimStep
import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
  val generalMode: GeneralMode,
  val dimSteps : List<ConfigurationDimStep>,
  val dimLevel : Float,
  val daliFadeTime: Float,
  val timeMidnightOffset: Float,
  // NOTE: Make sure to encodeDefaults
  // see: https://stackoverflow.com/questions/69008915/how-to-serialize-kotlin-data-class-with-default-values-into-json-using-kotlinx-s
  val _configurationVersion: Int = 1,
) {
  fun toEditableNode() : EditableNode {
    return EditableNode(
      generalMode = generalMode,
      dimSteps = dimSteps.map {
        MutableDimStep(
          hour = it.hour,
          minute = it.minute,
          level = it.level
        )
      },
      dimLevel = dimLevel,
      daliFadeTime = daliFadeTime,
      timeMidnightOffset = timeMidnightOffset,
    )
  }

  companion object {
    fun fromEditableNode(editableNode: EditableNode) : Configuration {
      return Configuration(
        generalMode = editableNode.generalMode ?: GeneralMode.NOMINAL,
        dimSteps = editableNode.dimSteps?.map {
          ConfigurationDimStep(hour = it.hour, minute = it.minute, level = it.level)
        } ?: emptyList(),
        dimLevel = editableNode.dimLevel ?: 0.0f,
        daliFadeTime = editableNode.daliFadeTime ?: 0.0f,
        timeMidnightOffset = editableNode.timeMidnightOffset ?: 0.0f,
      )
    }
  }
}

@Serializable
data class ConfigurationDimStep(
  val hour: Int,
  val minute: Int,
  val level: Float,
)
