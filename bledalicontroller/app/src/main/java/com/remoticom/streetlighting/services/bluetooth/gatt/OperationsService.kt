package com.remoticom.streetlighting.services.bluetooth.gatt

import com.remoticom.streetlighting.services.bluetooth.data.Device
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.*
import com.remoticom.streetlighting.services.web.TokenProvider
import com.remoticom.streetlighting.services.web.data.Peripheral

interface OperationsService {

  suspend fun connect(device: Device, tokenProvider: TokenProvider, peripheral: Peripheral?): Boolean

  suspend fun disconnect()

  suspend fun readGeneralCharacteristics(): GeneralCharacteristics

  suspend fun readDimCharacteristics() : DimCharacteristics

  suspend fun readDaliCharacteristics() : DaliCharacteristics

  suspend fun readTimeCharacteristics() : TimeCharacteristics

  suspend fun readGpsCharacteristics() : GpsCharacteristics

  // Health and state come from advertisements on SNO110 (ZSC010 does not use it)
  suspend fun readDiagnosticsCharacteristics(health: Int?, state: Int?) : DiagnosticsCharacteristics

  suspend fun readDaliBank(bank: Int) : Map<Int, Any?>

  suspend fun writeGeneralCharacteristics(generalCharacteristics: GeneralCharacteristics?) : Boolean

  suspend fun writeDimCharacteristics(dimCharacteristics: DimCharacteristics?) : Boolean

  suspend fun writeDaliCharacteristics(daliCharacteristics: DaliCharacteristics?) : Boolean

  suspend fun writeTimeCharacteristics(timeCharacteristics: TimeCharacteristics?) : Boolean
}
