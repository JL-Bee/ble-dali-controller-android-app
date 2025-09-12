package com.remoticom.streetlighting.services.bluetooth.utilities

import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DaliPowerLevel

fun List<DaliPowerLevel>.filterToValidPowerLevels() : List<DaliPowerLevel> {
  return if (this.indexOf(0) < 0) this else this.subList(0, this.indexOf(0))
}
