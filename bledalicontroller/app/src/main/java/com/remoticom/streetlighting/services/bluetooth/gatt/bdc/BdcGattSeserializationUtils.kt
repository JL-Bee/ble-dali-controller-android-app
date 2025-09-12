package com.remoticom.streetlighting.services.bluetooth.gatt.bdc

import android.bluetooth.BluetoothGattCharacteristic
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.*
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.TimeZone
import java.util.*

fun BluetoothGattCharacteristic.serializeGeneralMode(value: GeneralMode?): Boolean {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_GENERAL_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_GENERAL_MODE)

  if (value == null || value == GeneralMode.UNKNOWN) return false

  setValue(value.value, BluetoothGattCharacteristic.FORMAT_UINT8, 0)

  return true
}

fun BluetoothGattCharacteristic.deserializeGeneralMode(): GeneralMode {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_GENERAL_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_GENERAL_MODE)

  return GeneralMode.getByValue(
    this.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
  ) ?: GeneralMode.UNKNOWN
}

fun BluetoothGattCharacteristic.deserializeGeneralName(): String {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_GENERAL_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_GENERAL_NAME)

  return this.getStringValue(0)
}

fun BluetoothGattCharacteristic.serializeDimPreset(value: DimPreset?): Boolean {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DIM_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DIM_PRESET)

  if (null == value) return false

  setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0)

  return true
}

fun BluetoothGattCharacteristic.deserializeDimPreset(): DimPreset {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DIM_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DIM_PRESET)

  return this.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
}

fun BluetoothGattCharacteristic.serializeDimSteps(value: List<DimStep>?) : Boolean {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DIM_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS)

  if (null == value || BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_COUNT != value.count()) return false

  val bytes = ByteArray(BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_COUNT * BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_BYTES_PER_STEP)

  // Test error 9:
  //val bytes = ByteArray(25)

  for (i in 0 until BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_COUNT) {
    bytes[i * BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_BYTES_PER_STEP + BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_INDEX_HOUR] = value[i].hour.toByte()
    bytes[i * BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_BYTES_PER_STEP + BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_INDEX_MINUTE] = value[i].minute.toByte()
    bytes[i * BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_BYTES_PER_STEP + BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_INDEX_R] = 0
    bytes[i * BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_BYTES_PER_STEP + BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_INDEX_G] = 0
    bytes[i * BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_BYTES_PER_STEP + BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_INDEX_B] = 0
    bytes[i * BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_BYTES_PER_STEP + BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_INDEX_W] = value[i].level.toByte()
  }

  setValue(bytes)

  return true
}

fun BluetoothGattCharacteristic.deserializeDimSteps() : List<DimStep> {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DIM_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS)

  val dimSteps = mutableListOf<DimStep>()

  if (value.count() < BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_COUNT * BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_BYTES_PER_STEP) return dimSteps

  for (i in 0 until BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_COUNT) {
    dimSteps.add(
      DimStep(
        value[i * BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_BYTES_PER_STEP + BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_INDEX_HOUR].toInt(),
        value[i * BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_BYTES_PER_STEP + BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_INDEX_MINUTE].toInt(),
        value[i * BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_BYTES_PER_STEP + BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS_INDEX_W].toInt()
      )
    )
  }

  return dimSteps
}

fun BluetoothGattCharacteristic.serializeDimNominalLightLevel(value: DimNominalLightLevel?): Boolean {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DIM_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DIM_NOMINAL_LIGHT_LEVEL)

  if (null == value) return false

  // R, G, B, W
  val bytes = ubyteArrayOf(0x00u, 0x00u, 0x00u, value.toUByte()).toByteArray()

  setValue(bytes)

  return true
}

fun BluetoothGattCharacteristic.deserializeDimNominalLightLevel() : DimNominalLightLevel {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DIM_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DIM_NOMINAL_LIGHT_LEVEL)

  // R, G, B, W
  val level = value[BDC_BLUETOOTH_CHARACTERISTIC_DIM_NOMINAL_LIGHT_LEVEL_INDEX_W].toInt()

  return level
}

fun BluetoothGattCharacteristic.serializeDaliClo(value: DaliClo?): Boolean {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DALI_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DALI_CLO)

  if (null == value) return false

  setValue(if (value) 1 else 0, BluetoothGattCharacteristic.FORMAT_UINT8, 0)

  return true
}

fun BluetoothGattCharacteristic.deserializeDaliClo(): DaliClo {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DALI_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DALI_CLO)

  return this.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) != 0
}

fun BluetoothGattCharacteristic.serializeDaliPowerLevel(value: DaliPowerLevel?): Boolean {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DALI_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DALI_POWER_LEVEL)

  if (null == value || value !in 0 until BDC_BLUETOOTH_CHARACTERISTIC_DALI_AVAILABLE_POWER_LEVELS_COUNT) return false

  setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0)

  return true
}

fun BluetoothGattCharacteristic.deserializeDaliPowerLevel(): DaliPowerLevel {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DALI_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DALI_POWER_LEVEL)

  return this.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
}

fun BluetoothGattCharacteristic.deserializeDaliAvailablePowerLevels(): List<DaliPowerLevel> {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DALI_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DALI_AVAILABLE_POWER_LEVELS)

  val availablePowerLevels = mutableListOf<DaliPowerLevel>()

  if (value.count() < BDC_BLUETOOTH_CHARACTERISTIC_DALI_AVAILABLE_POWER_LEVELS_COUNT * BDC_BLUETOOTH_CHARACTERISTIC_DALI_AVAILABLE_POWER_LEVELS_BYTES_PER_LEVEL) return availablePowerLevels

  for (i in 0 until BDC_BLUETOOTH_CHARACTERISTIC_DALI_AVAILABLE_POWER_LEVELS_COUNT) {
    availablePowerLevels.add(this.getIntValue(
      BDC_BLUETOOTH_CHARACTERISTIC_DALI_AVAILABLE_POWER_LEVELS_LEVEL_FORMAT, i * BDC_BLUETOOTH_CHARACTERISTIC_DALI_AVAILABLE_POWER_LEVELS_BYTES_PER_LEVEL
    ))
  }

  return availablePowerLevels
}

fun BluetoothGattCharacteristic.deserializeDaliFixtureName(): String {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DALI_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DALI_FIXTURE_NAME)

  return this.getStringValue(0)
}

fun BluetoothGattCharacteristic.serializeDaliFadeTime(value: DaliFadeTime?) : Boolean {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DALI_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DALI_FADE_TIME)

  if (value == null || value < 0 || value > 15) return false

  setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0)

  return true
}

fun BluetoothGattCharacteristic.deserializeDaliFadeTime() : DaliFadeTime {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DALI_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DALI_FADE_TIME)

  return this.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
}

// TODO: Bank, address, size, data
fun BluetoothGattCharacteristic.deserializeDaliMemoryResponse() : ByteArray {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DALI_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_RESPONSE)

  return this.value
}

// TODO: Bank, address, size
fun BluetoothGattCharacteristic.serializeDaliMemoryRequest(value: ByteArray?) : Boolean {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DALI_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_REQUEST)

  if (null == value) return false

  setValue(value)

  return true
}

fun BluetoothGattCharacteristic.deserializeDaliMemoryRequest() : ByteArray {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DALI_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_REQUEST)

  return value
}

fun BluetoothGattCharacteristic.serializeTimeTimeZone(value: TimeZone?): Boolean {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_TIME_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_TIME_TIMEZONE)

  if (null == value || value.utcOffset !in -12..14) return false

  setValue(
    value.utcOffset,
    BluetoothGattCharacteristic.FORMAT_SINT8,
    BDC_BLUETOOTH_CHARACTERISTIC_TIME_TIMEZONE_INDEX_OFFSET
  )

  setValue(
    if (value.daylightSavingTimeEnabled) BDC_BLUETOOTH_CHARACTERISTIC_TIME_TIMEZONE_DST_ENABLED else BDC_BLUETOOTH_CHARACTERISTIC_TIME_TIMEZONE_DST_DISABLED,
    BluetoothGattCharacteristic.FORMAT_UINT8,
    BDC_BLUETOOTH_CHARACTERISTIC_TIME_TIMEZONE_INDEX_DST
  )

  return true
}

fun BluetoothGattCharacteristic.deserializeTimeTimeZone(): TimeZone {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_TIME_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_TIME_TIMEZONE)

  val offset = this.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, BDC_BLUETOOTH_CHARACTERISTIC_TIME_TIMEZONE_INDEX_OFFSET)
  val daylightSavingEnabled = (this.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, BDC_BLUETOOTH_CHARACTERISTIC_TIME_TIMEZONE_INDEX_DST) != BDC_BLUETOOTH_CHARACTERISTIC_TIME_TIMEZONE_DST_DISABLED)

  return TimeZone(offset, daylightSavingEnabled)
}

fun BluetoothGattCharacteristic.deserializeTimeUnixTimestamp(): UnixTimestamp {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_TIME_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_TIME_UNIX_TIMESTAMP)

  return this.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0)
}

fun BluetoothGattCharacteristic.serializeTimeMidnightOffset(value: MidnightOffset?): Boolean {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_TIME_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_TIME_MIDNIGHT_OFFSET)

  if (null == value) return false

  setValue(value, BluetoothGattCharacteristic.FORMAT_SINT8, 0)

  return true
}

fun BluetoothGattCharacteristic.deserializeTimeMidnightOffset(): MidnightOffset {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_TIME_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_TIME_MIDNIGHT_OFFSET)

  // TODO: Signed or unsigned?
  return this.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0)
}

fun BluetoothGattCharacteristic.deserializeGpsPosition() : GpsPosition {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_GPS_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_GPS_POSITION)

  val latitudeBits = this.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32, 0)
  val longitudeBits = this.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32, BDC_BLUETOOTH_CHARACTERISTIC_GPS_POSITION_BYTES_PER_COMPONENT)

  val latitudeBitsReversed = Integer.reverseBytes(latitudeBits)
  val longitudeBitsReversed = Integer.reverseBytes(longitudeBits)

  val latitude = java.lang.Float.intBitsToFloat(latitudeBitsReversed)
  val longitude = java.lang.Float.intBitsToFloat(longitudeBitsReversed)

  return GpsPosition(latitude, longitude)
}

fun BluetoothGattCharacteristic.deserializeDiagnosticsStatus(): DiagnosticsStatus {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DIAGNOSTICS_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DIAGNOSTICS_STATUS)

  return this.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
}

fun BluetoothGattCharacteristic.deserializeDiagnosticsVersion(): DiagnosticsVersion {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_DIAGNOSTICS_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_DIAGNOSTICS_VERSION)

  if (value.count() < BDC_BLUETOOTH_CHARACTERISTIC_DIAGNOSTICS_VERSION_COUNT * BDC_BLUETOOTH_CHARACTERISTIC_DIAGNOSTICS_VERSION_BYTES_PER_VERSION) return DiagnosticsVersion.emptyDiagnosticsVersion()

  val firmwareVersion = deserializeVersion(0)
  val libraryVersion = deserializeVersion(
    BDC_BLUETOOTH_CHARACTERISTIC_DIAGNOSTICS_VERSION_BYTES_PER_VERSION
  )

  return DiagnosticsVersion(firmwareVersion, libraryVersion)
}

private fun BluetoothGattCharacteristic.deserializeVersion(offset: Int) : Version {
  return Version(
    this.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 0),
    this.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1),
    this.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 2)
  )
}

fun BluetoothGattCharacteristic.serializeSecurityPassword(value: Int?): Boolean {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_SECURITY_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_SECURITY_PASSWORD)

  if (null == value) return false

  setValue(value, BluetoothGattCharacteristic.FORMAT_UINT32, 0)

  return true
}

fun BluetoothGattCharacteristic.deserializeSecurityPassword(): Int {
  assert(this.serviceMask == UUID.fromString(BDC_BLUETOOTH_SERVICE_SECURITY_MASK))
  assert(this.applicationId == BDC_BLUETOOTH_CHARACTERISTIC_SECURITY_PASSWORD)

  return this.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0)
}
