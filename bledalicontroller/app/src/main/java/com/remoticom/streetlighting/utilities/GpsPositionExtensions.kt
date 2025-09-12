package com.remoticom.streetlighting.utilities

import com.remoticom.streetlighting.services.bluetooth.data.characteristics.GpsPosition

fun GpsPosition.toDisplayString() : String? {
  if (this.latitude.isNaN() || this.longitude.isNaN()) return null

  return "${this.latitude} / ${this.longitude}"
}
