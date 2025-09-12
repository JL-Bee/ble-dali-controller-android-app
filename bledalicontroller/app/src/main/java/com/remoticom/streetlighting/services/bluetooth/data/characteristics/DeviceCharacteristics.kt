package com.remoticom.streetlighting.services.bluetooth.data.characteristics

data class DeviceCharacteristics(
  val general: GeneralCharacteristics? = null,
  val dim: DimCharacteristics? = null,
  val dali: DaliCharacteristics? = null,
  val time: TimeCharacteristics? = null,
  val gps: GpsCharacteristics? = null,
  val diagnostics: DiagnosticsCharacteristics? = null,
  val daliBanks: Map<Int, Map<Int, Any?>>? = null
) {
}
