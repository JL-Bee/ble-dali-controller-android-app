package com.remoticom.streetlighting.services.bluetooth.data.characteristics

typealias DimPreset = Int
typealias DimNominalLightLevel = Int

data class DimCharacteristics(
  val preset: DimPreset? = null,
  val steps: List<DimStep>? = null,
  val level: Int? = null
)
