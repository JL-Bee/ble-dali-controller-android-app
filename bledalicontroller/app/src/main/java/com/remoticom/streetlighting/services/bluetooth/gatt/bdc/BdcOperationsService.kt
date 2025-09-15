package com.remoticom.streetlighting.services.bluetooth.gatt.bdc

import android.util.Log
import com.remoticom.streetlighting.services.bluetooth.data.*
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.*
import com.remoticom.streetlighting.services.bluetooth.gatt.ConnectionProvider
import com.remoticom.streetlighting.services.bluetooth.gatt.GattOperationsService
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.*
import com.remoticom.streetlighting.services.bluetooth.gatt.bdc.operations.*
import com.remoticom.streetlighting.services.web.TokenProvider
import com.remoticom.streetlighting.services.web.data.Peripheral
import kotlinx.coroutines.delay

class BdcOperationsService constructor(
  connectionProvider: ConnectionProvider
) : GattOperationsService(connectionProvider) {

  private val daliMemoryOperationsService: BdcDaliMemoryOperationsService = BdcDaliMemoryOperationsService(connectionProvider)

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

    // Request MTU
    if (!connectionProvider.performOperation(RequestMtuOperation(517), false)) {
      connectionProvider.tearDown()
      return
    }

    // Discover services
    if (!connectionProvider.performOperation(DiscoverServicesOperation(), false)) {
      connectionProvider.tearDown()
      return
    }

    delay(500)

    // Write pin
    if (connectionProvider.performOperation(BdcWriteSecurityPasswordOperation(devicePassword)) != devicePassword) {
      connectionProvider.tearDown()
      return
    }

    delay(500)
  }

  override suspend fun readGeneralCharacteristics() : GeneralCharacteristics {
    val mode = connectionProvider.performOperation(BdcReadGeneralModeOperation())
    val name = connectionProvider.performOperation(BdcReadGeneralNameOperation())

    return GeneralCharacteristics(
      mode,
      name,
    )
  }

  override suspend fun readDimCharacteristics() : DimCharacteristics {
    // ZSC010 - Using preset to know whether dim plan is being used or single value
    val dimPreset = connectionProvider.performOperation(BdcReadDimPresetOperation())

    // Firmware returns it in reverse order (compared to order it was written in).
    // (Cannot reverse in deserializer because of generic write/read value matching check)
    // Reversing it (back) ourselves here for now:
    val dimSteps = connectionProvider.performOperation(BdcReadDimStepsOperation())?.reversed()

    val level = connectionProvider.performOperation(BdcReadDimNominalLightLevelOperation())

    return DimCharacteristics(
      dimPreset,
      dimSteps,
      level
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


//    val fixtureName = connectionProvider.performOperation(BdcReadDaliFixtureNameOperation())
    val fixtureName = null

    val fadeTime = connectionProvider.performOperation(BdcReadDaliFadeTimeOperation())

    return DaliCharacteristics(clo, powerLevel, availablePowerLevels, fixtureName, fadeTime)
  }

  override suspend fun readTimeCharacteristics() : TimeCharacteristics {
    // TODO: Any need to read this for BDC?
    val unixTimestamp = connectionProvider.performOperation(BdcReadTimeUnixTimestampOperation())

    val midnightOffset = connectionProvider.performOperation(BdcReadTimeMidnightOffsetOperation())

    return TimeCharacteristics(null, unixTimestamp, midnightOffset)
  }

  override suspend fun readGpsCharacteristics() : GpsCharacteristics {
    val position = connectionProvider.performOperation(BdcReadGpsPositionOperation())

    return GpsCharacteristics(position)
  }

  override suspend fun readDiagnosticsCharacteristics(
    health: Int?,
    state: Int?
  ): DiagnosticsCharacteristics {
    val status = connectionProvider.performOperation(BdcReadDiagnosticsStatusOperation())
    val version = connectionProvider.performOperation(BdcReadDiagnosticsVersionOperation())

    return DiagnosticsCharacteristics(status, version)
  }

  override suspend fun readDaliBank(bank: Int) : Map<Int, Any?> {
    Log.d(TAG, "Reading DALI memory bank: ${bank}")

    return when (bank) {
      BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_1 -> {
        val a = readDaliBank1()

        return a
      }
      BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_202 -> readDaliBank202()
      BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_203 -> readDaliBank203()
      BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_204 -> readDaliBank204()
      BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_205 -> readDaliBank205()
      BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_206 -> readDaliBank206()
      BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_207 -> readDaliBank207()
      else -> emptyMap()
    }
  }

  private suspend fun readDaliBank1() : Map<Int, Any?> {
    val bank = BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_1.toByte();

    return mapOf(
      0x03 to daliMemoryOperationsService.readDaliMemoryUInt48(
        bank,
        0x03.toByte(),
        0xFFFFFFFFFFFFu
      ),
      0x09 to daliMemoryOperationsService.readDaliMemoryUInt64(
        bank,
        0x09.toByte(),
        0xFFFFFFFFFFFFFFFFu
      ),
      0x11 to daliMemoryOperationsService.readDaliMemoryUInt16(
        bank,
        0x11.toByte(),
        null
      ),
      0x13 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x13.toByte(),
        100u
      ),
      0x14 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x14.toByte(),
      54u
      ),
      0x15 to daliMemoryOperationsService.readDaliMemoryUInt16(
        bank,
        0x15.toByte(),
        0xFFFFu
      ),
      0x17 to daliMemoryOperationsService.readDaliMemoryUInt16(
        bank,
        0x17.toByte(),
        0xFFFFu
      ),
      0x19 to daliMemoryOperationsService.readDaliMemoryUInt16(
        bank,
        0x19.toByte(),
        0xFFFFu
      ),
      0x1B to daliMemoryOperationsService.readDaliMemoryUInt16(
        bank,
        0x1B.toByte(),
        0xFFFFu
      ),
      0x1D to daliMemoryOperationsService.readDaliMemoryUInt24(
        bank,
        0x1D.toByte(),
        0xFFFFFFu
      ),
      0x20 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x20.toByte(),
        101u
      ),
      0x21 to daliMemoryOperationsService.readDaliMemoryUInt16(
        bank,
        0x21.toByte(),
        17001u
      ),
      0x23 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x23.toByte(),
        0xFFu
      ),
      0x24 to daliMemoryOperationsService.readDaliMemoryString(
        bank,
        0x24.toByte(),
        24
      ),
      0x3C to daliMemoryOperationsService.readDaliMemoryString(
        bank,
        0x3C.toByte(),
        60
      ),
    )
  }

  private suspend fun readDaliBank202() : Map<Int, Any?> {
    val bank = BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_202.toByte();

    return mapOf(
      0x03 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x03.toByte(),
        null,
      ),
      0x04 to daliMemoryOperationsService.readDaliMemoryInt8(
        bank,
        0x04.toByte()
      ),
      0x05 to daliMemoryOperationsService.readDaliMemoryUInt48(
        bank,
        0x05.toByte(),
        0xFFFFFFFFFFFEu
      ),
      0x0B to daliMemoryOperationsService.readDaliMemoryInt8(
        bank,
        0x0B.toByte()
      ),
      0x0C to daliMemoryOperationsService.readDaliMemoryUInt32(
        bank,
        0x0C.toByte(),
        0xFFFFFFFEu
      ),
    )
  }

  private suspend fun readDaliBank203() : Map<Int, Any?> {
    val bank = BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_203.toByte();

    return mapOf(
      0x03 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x03.toByte(),
        null
      ),
      0x04 to daliMemoryOperationsService.readDaliMemoryInt8(
        bank,
        0x04.toByte()
      ),
      0x05 to daliMemoryOperationsService.readDaliMemoryUInt48(
        bank,
        0x05.toByte(),
        0xFFFFFFFFFFFEu
      ),
      0x0B to daliMemoryOperationsService.readDaliMemoryInt8(
        bank,
        0x0B.toByte()
      ),
      0x0C to daliMemoryOperationsService.readDaliMemoryUInt32(
        bank,
        0x0C.toByte(),
        0xFFFFFFFEu
      ),
    )
  }

  private suspend fun readDaliBank204() : Map<Int, Any?> {
    val bank = BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_204.toByte();

    return mapOf(
      0x03 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x03.toByte(),
        null
      ),
      0x04 to daliMemoryOperationsService.readDaliMemoryInt8(
        bank,
        0x04.toByte()
      ),
      0x05 to daliMemoryOperationsService.readDaliMemoryUInt48(
        bank,
        0x05.toByte(),
        0xFFFFFFFFFFFEu
      ),
      0x0B to daliMemoryOperationsService.readDaliMemoryInt8(
        bank,
        0x0B.toByte()
      ),
      0x0C to daliMemoryOperationsService.readDaliMemoryUInt32(
        bank,
        0x0C.toByte(),
        0xFFFFFFFEu
      ),
    )
  }

  private suspend fun readDaliBank205() : Map<Int, Any?> {
    val bank = BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_205.toByte();

    return mapOf(
      0x03 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x03.toByte(),
        null
      ),
      0x04 to daliMemoryOperationsService.readDaliMemoryUInt32(
        bank,
        0x04.toByte(),
        0xFFFFFFFEu
      ),
      0x08 to daliMemoryOperationsService.readDaliMemoryUInt24(
        bank,
        0x08.toByte(),
        0xFFFFFEu
      ),
      0x0B to daliMemoryOperationsService.readDaliMemoryUInt16(
        bank,
        0x0B.toByte(),
        0xFFFEu
      ),
      0x0D to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x0D.toByte(),
        0xFEu
      ),
      0x0E to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x0E.toByte(),
        101u
      ),
      0x0F to daliMemoryOperationsService.readDaliMemoryBool(
        bank,
        0x0F.toByte(),
      ),
      0x10 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x10.toByte(),
        0xFEu
      ),
      0x11 to daliMemoryOperationsService.readDaliMemoryBool(
        bank,
        0x11.toByte()
      ),
      0x12 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x12.toByte(),
        0xFEu
      ),
      0x13 to daliMemoryOperationsService.readDaliMemoryBool(
        bank,
        0x13.toByte()
      ),
      0x14 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x14.toByte(),
        0xFEu
      ),
      0x15 to daliMemoryOperationsService.readDaliMemoryBool(
        bank,
        0x15.toByte()
      ),
      0x16 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x16.toByte(),
        0xFEu
      ),
      0x17 to daliMemoryOperationsService.readDaliMemoryBool(
        bank,
        0x17.toByte()
      ),
      0x18 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x18.toByte(),
        0xFEu
      ),
      0x19 to daliMemoryOperationsService.readDaliMemoryBool(
        bank,
        0x19.toByte()
      ),
      0x1A to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x1A.toByte(),
        0xFEu
      ),
      0x1B to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x1B.toByte(),
        0xFEu
      ),
      0x1C to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x1C.toByte(),
        101u
      ),
    )
  }

  private suspend fun readDaliBank206() : Map<Int, Any?> {
    val bank = BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_206.toByte();

    return mapOf(
      0x03 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x03.toByte(),
        null
      ),
      0x04 to daliMemoryOperationsService.readDaliMemoryUInt24(
        bank,
        0x04.toByte(),
        0xFFFFFEu
      ),
      0x07 to daliMemoryOperationsService.readDaliMemoryUInt24(
        bank,
        0x07.toByte(),
        0xFFFFFEu
      ),
      0x0A to daliMemoryOperationsService.readDaliMemoryUInt32(
        bank,
        0x0A.toByte(),
        0xFFFFFFFEu
      ),
      0x0E to daliMemoryOperationsService.readDaliMemoryUInt32(
        bank,
        0x0E.toByte(),
        0xFFFFFFFEu
      ),
      0x12 to daliMemoryOperationsService.readDaliMemoryUInt16(
        bank,
        0x12.toByte(),
        0xFFFEu
      ),
      0x14 to daliMemoryOperationsService.readDaliMemoryUInt16(
        bank,
        0x14.toByte(),
        0xFFFEu
      ),
      0x16 to daliMemoryOperationsService.readDaliMemoryBool(
        bank,
        0x16.toByte()
      ),
      0x17 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x17.toByte(),
        0xFEu
      ),
      0x18 to daliMemoryOperationsService.readDaliMemoryBool(
        bank,
        0x18.toByte()
      ),
      0x19 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x19.toByte(),
        0xFEu
      ),
      0x1A to daliMemoryOperationsService.readDaliMemoryBool(
        bank,
        0x1A.toByte()
      ),
      0x1B to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x1B.toByte(),
        0xFEu
      ),
      0x1C to daliMemoryOperationsService.readDaliMemoryBool(
        bank,
        0x1C.toByte()
      ),
      0x1D to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x1D.toByte(),
        0xFEu
      ),
      0x1E to daliMemoryOperationsService.readDaliMemoryBool(
        bank,
        0x1E.toByte()
      ),
      0x1F to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x1F.toByte(),
        0xFEu
      ),
      0x20 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x20.toByte(),
        0xFEu
      ),
    )
  }

  private suspend fun readDaliBank207() : Map<Int, Any?> {
    val bank = BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_207.toByte();

    return mapOf(
      0x03 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x03.toByte(),
        null
      ),
      0x04 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x04.toByte(),
        0xFEu
      ),
      0x05 to daliMemoryOperationsService.readDaliMemoryUInt8(
        bank,
        0x05.toByte(),
        0xFEu
      ),
      0x06 to daliMemoryOperationsService.readDaliMemoryUInt16(
        bank,
        0x06.toByte(),
        0xFFFEu
      ),
    )
  }

  override suspend fun writeGeneralCharacteristics(generalCharacteristics: GeneralCharacteristics?): Boolean {
    return connectionProvider.performWriteTransaction(
      listOf(
        BdcWriteGeneralModeOperation(generalCharacteristics?.mode),
        // BdcWriteGeneralNameOperation(generalCharacteristics?.name)
      )
    )
  }

  override suspend fun writeDimCharacteristics(dimCharacteristics: DimCharacteristics?) : Boolean {
    // Need to separate preset from steps to avoid GATT error 0x09
    // ..which is probably indication of exceeding MTU (40 bytes?!)
    val successPreset = connectionProvider.performWriteTransaction(listOf(
      BdcWriteDimPresetOperation(dimCharacteristics?.preset),
    ))

    val successSteps = connectionProvider.performWriteTransaction(listOf(
      BdcWriteDimStepsOperation(dimCharacteristics?.steps),
    ))

    val successLevel = connectionProvider.performWriteTransaction(listOf(
      BdcWriteDimNominalLightLevelOperation(dimCharacteristics?.level)
    ))

    return successPreset && successSteps && successLevel
  }

  override suspend fun writeDaliCharacteristics(daliCharacteristics: DaliCharacteristics?) : Boolean {
    return connectionProvider.performWriteTransaction(listOf(
      BdcWriteDaliFadeTimeOperation(daliCharacteristics?.fadeTime)
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
      BdcWriteTimeMidnightOffsetOperation(timeCharacteristics?.midnightOffset)
    ))
  }

  companion object {
    private const val TAG = "BdcOperationsService"
  }
}


