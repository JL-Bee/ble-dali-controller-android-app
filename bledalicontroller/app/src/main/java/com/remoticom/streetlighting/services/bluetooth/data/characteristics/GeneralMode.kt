package com.remoticom.streetlighting.services.bluetooth.data.characteristics

enum class GeneralMode(val value: Int) {
  DIM(0),
  ASTRO(1),
  LUX(2),
  NOMINAL(3),
  STEP_DIMMING(4),
  UNKNOWN(Int.MAX_VALUE);

  companion object {
    private val values = values();
    fun getByValue(value: Int) = values.firstOrNull { it.value == value }
  }
}
