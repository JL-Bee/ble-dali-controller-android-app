package com.remoticom.streetlighting.services.bluetooth.data.characteristics

data class Version(val major: Int, val minor: Int, val patch: Int, val build: Int? = null) {
  companion object {
    fun emptyVersion() : Version {
      return Version(0, 0, 0)
    }
  }
}

data class DiagnosticsVersion(val firmwareVersion: Version, val libraryVersion: Version) {
  companion object {
    fun emptyDiagnosticsVersion() : DiagnosticsVersion {
      return DiagnosticsVersion(Version(0,0,0), Version(0,0,0))
    }
  }
}
