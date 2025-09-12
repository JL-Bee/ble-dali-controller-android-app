package com.remoticom.streetlighting.services.bluetooth.data.characteristics

data class DimStep(
  val hour: Int = 0,
  val minute: Int = 0,
  val level: Int = 0
)
