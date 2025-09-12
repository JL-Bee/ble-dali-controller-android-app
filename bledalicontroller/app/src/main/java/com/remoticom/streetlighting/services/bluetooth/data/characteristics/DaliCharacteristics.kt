package com.remoticom.streetlighting.services.bluetooth.data.characteristics

typealias DaliPowerLevel = Int
typealias DaliClo = Boolean
typealias DaliFadeTime = Int

data class DaliCharacteristics(
  val clo: DaliClo? = null,
  val powerLevel: DaliPowerLevel? = null,
  val availablePowerLevels: List<DaliPowerLevel>? = null,
  val fixtureName: String? = null,
  val fadeTime: DaliFadeTime? = null
  )
