package com.remoticom.streetlighting.services.bluetooth.data.characteristics

data class GpsCharacteristics(val position: GpsPosition?) {
}

data class GpsPosition(val latitude: Float = Float.NaN, val longitude: Float = Float.NaN) {
}
