package com.remoticom.streetlighting.services.bluetooth.data.characteristics

typealias DiagnosticsStatus = Int

data class DiagnosticsCharacteristics(
  val status: DiagnosticsStatus? = null,
  val version: DiagnosticsVersion? = null
) {

}
