package com.remoticom.streetlighting.services.bluetooth.gatt.zsc010

import android.util.Log
import com.remoticom.streetlighting.services.bluetooth.data.*
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.*
import com.remoticom.streetlighting.services.bluetooth.gatt.ConnectionProvider
import com.remoticom.streetlighting.services.bluetooth.gatt.GattOperationsService
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.*
import com.remoticom.streetlighting.services.bluetooth.gatt.zsc010.operations.*
import com.remoticom.streetlighting.services.web.TokenProvider
import com.remoticom.streetlighting.services.web.data.Peripheral

class Zsc010OperationsService constructor(
  connectionProvider: ConnectionProvider
) : GattOperationsService(connectionProvider) {

  override suspend fun connect(
    device: Device,
    tokenProvider: TokenProvider,
    peripheral: Peripheral?
  ) {
    peripheral ?: return

    if (null == peripheral.password) {
      Log.e(TAG, "Password not yet available")

      return
    }

    val devicePassword = peripheral.password.toUIntOrNull()?.toInt()

    if (null == devicePassword) {
      Log.e(TAG, "Password has invalid format")

      return
    }

    // Connect
    if (!connectionProvider.performOperation(ConnectGattOperation(), false)) {
      return
    }

    // Discover services
    if (!connectionProvider.performOperation(DiscoverServicesOperation(), false)) {
      connectionProvider.tearDown()
      return
    }

    // Write pin
    if (connectionProvider.performOperation(Zsc010WriteSecurityPasswordOperation(devicePassword)) != devicePassword) {
      connectionProvider.tearDown()
      return
    }
  }

  override suspend fun readGeneralCharacteristics() : GeneralCharacteristics {
    val mode = connectionProvider.performOperation(Zsc010ReadGeneralModeOperation())

    return GeneralCharacteristics(mode)
  }

  override suspend fun readDimCharacteristics() : DimCharacteristics {
    // ZSC010 - Using preset to know whether dim plan is being used or single value
    val dimPreset = connectionProvider.performOperation(Zsc010ReadDimPresetOperation())

    // Firmware returns it in reverse order (compared to order it was written in).
    // (Cannot reverse in deserializer because of generic write/read value matching check)
    // Reversing it (back) ourselves here for now:
    val dimSteps = connectionProvider.performOperation(Zsc010ReadDimStepsOperation())?.reversed()

    return DimCharacteristics(
      dimPreset,
      dimSteps
    )
  }

  override suspend fun readDaliCharacteristics() : DaliCharacteristics {
    // ZSC010 - 'CLO Enabled' not supported
    // val clo = performOperation(ReadDaliCloOperation())
    val clo = null

    // ZSC010 - 'Power level' not supported
    // val powerLevel = performOperation(ReadDaliPowerLevelOperation())
    val powerLevel = null
    // val availablePowerLevels = performOperation(ReadDaliAvailablePowerLevelsOperation())
    val availablePowerLevels = null


    val fixtureName = connectionProvider.performOperation(Zsc010ReadDaliFixtureNameOperation())

    val fadeTime = connectionProvider.performOperation(Zsc010ReadDaliFadeTimeOperation())

    return DaliCharacteristics(clo, powerLevel, availablePowerLevels, fixtureName, fadeTime)
  }

  override suspend fun readTimeCharacteristics() : TimeCharacteristics {
    val timezone = connectionProvider.performOperation(Zsc010ReadTimeTimeZoneOperation())
    val unixTimestamp = connectionProvider.performOperation(Zsc010ReadTimeUnixTimestampOperation())

    return TimeCharacteristics(timezone, unixTimestamp)
  }

  override suspend fun readGpsCharacteristics() : GpsCharacteristics {
    val position = connectionProvider.performOperation(Zsc010ReadGpsPositionOperation())

    return GpsCharacteristics(position)
  }

  override suspend fun readDiagnosticsCharacteristics(
    health: Int?,
    state: Int?
  ): DiagnosticsCharacteristics {
    val status = connectionProvider.performOperation(Zsc010ReadDiagnosticsStatusOperation())
    val version = connectionProvider.performOperation(Zsc010ReadDiagnosticsVersionOperation())

    return DiagnosticsCharacteristics(status, version)
  }

  override suspend fun readDaliBank(bank: Int): Map<Int, Any> {
    return emptyMap()
  }

  override suspend fun writeGeneralCharacteristics(generalCharacteristics: GeneralCharacteristics?): Boolean {
    return connectionProvider.performWriteTransaction(
      listOf(
        Zsc010WriteGeneralModeOperation(generalCharacteristics?.mode)
      )
    )
  }

  override suspend fun writeDimCharacteristics(dimCharacteristics: DimCharacteristics?) : Boolean {
    return connectionProvider.performWriteTransaction(listOf(
      Zsc010WriteDimPresetOperation(dimCharacteristics?.preset),
      Zsc010WriteDimStepsOperation(dimCharacteristics?.steps)
    ))
  }

  override suspend fun writeDaliCharacteristics(daliCharacteristics: DaliCharacteristics?) : Boolean {
    return connectionProvider.performWriteTransaction(listOf(
      Zsc010WriteDaliFadeTimeOperation(daliCharacteristics?.fadeTime)
    ))

    // Power levels not used by ZSC010 and SNO110
    //    if (success) {
    //      serviceState = serviceState.copy(
    //        characteristics = serviceState.safeCharacteristics.copy(
    //          // Keep earlier read available power levels
    //          dali = daliCharacteristics?.copy(availablePowerLevels = serviceState.characteristics?.dali?.availablePowerLevels)
    //        )
    //      )
    //    }
  }

  override suspend fun writeTimeCharacteristics(timeCharacteristics: TimeCharacteristics?) : Boolean {
    return connectionProvider.performWriteTransaction(listOf(
      Zsc010WriteTimeTimeZoneOperation(timeCharacteristics?.timezone)
    ))
  }

  companion object {
    private const val TAG = "Zsc010OperationsService"
  }
}


