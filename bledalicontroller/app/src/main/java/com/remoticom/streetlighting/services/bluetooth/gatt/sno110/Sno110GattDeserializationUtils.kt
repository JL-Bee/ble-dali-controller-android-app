package com.remoticom.streetlighting.services.bluetooth.gatt.sno110

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Range
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DimStep
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.GpsPosition
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.UnixTimestamp
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.Version
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.characteristics.ScheduleEntry
import io.ktor.utils.io.bits.*
import java.io.ByteArrayInputStream
import java.lang.NumberFormatException
import java.util.*

fun BluetoothGattCharacteristic.deserializeChallenge(): ByteArray {
  assert(this.service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SECURITY))

  assert(
    this.uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_SECURITY_AUTHENTICATE
    )
  )

  return this.value
}

fun BluetoothGattCharacteristic.deserializeChallengeResponse(): ByteArray {
  assert(this.service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SECURITY))

  assert(
    this.uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_SECURITY_AUTHENTICATE
    )
  )

  return this.value
}

fun BluetoothGattCharacteristic.deserializeDiagnosticsVersion(): Version {
  assert(this.service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_DEVICE_INFO))

  // Deserializer is reused for both fw and sw version
  assert(this.uuid == UUID.fromString(
    SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_INFO_FIRMWARE_VERSION
  ) || this.uuid == UUID.fromString(
    SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_INFO_SOFTWARE_VERSION
  ))

  val version = this.getStringValue(0)

  val versionParts = version.split(".")

  if (versionParts.size < 3 || versionParts.size > 4) {
    return Version.emptyVersion()
  }

  try {
    val major = versionParts[0].toInt()
    val minor = versionParts[1].toInt()
    val patch = versionParts[2].toInt()

    if (versionParts.size == 4) {
      val build = versionParts[3].toInt()

      return Version(major, minor, patch, build)
    }

    return Version(major, minor, patch)
  } catch (nfe: NumberFormatException) {
    return Version.emptyVersion()
  }
}

fun BluetoothGattCharacteristic.deserializeGpsPosition(): GpsPosition {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_DEVICE_CONFIG))
  assert(uuid == UUID.fromString(SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_GEOLOCATION))

  val latitudeBits = this.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32, 0).reverseByteOrder()
  val longitudeBits = this.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32, SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_GEOLOCATION_BYTES_PER_COMPONENT).reverseByteOrder()

  val latitude = if (latitudeBits == SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_GEOLOCATION_LAT_DEFAULT) {
    Float.NaN
  } else {
    latitudeBits.toFloat() / 0x80000000L * 90.0f
  }

  val longitude = if (longitudeBits == SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_GEOLOCATION_LON_DEFAULT) {
    Float.NaN
  } else {
    longitudeBits.toFloat() / 0x80000000L * 180.0f
  }

  return GpsPosition(
    latitude = latitude,
    longitude = longitude
  )
}

fun BluetoothGattCharacteristic.deserializeTimeUnixTimstamp(): UnixTimestamp {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_DEVICE_CONFIG))
  assert(uuid == UUID.fromString(
    SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_UTC))

  return getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0).reverseByteOrder()
}

fun BluetoothGattCharacteristic.deserializeTimeTimeZoneOffset(): Int {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_DEVICE_CONFIG))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_TIME_ZONE_OFFSET
    )
  )

  // Convert seconds to hours (UI is working with hours currently)
  // TODO (SNO110): Round this?
  return getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32, 0).reverseByteOrder() / 3600
}

fun BluetoothGattCharacteristic.deserializeTimeTimeZoneIndex(): String {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_DEVICE_CONFIG))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_TIME_ZONE_INDEX
    )
  )

  return getStringValue(0)
}

fun BluetoothGattCharacteristic.deserializeTimeDaylightSavingTime(): ByteArray {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_DEVICE_CONFIG))
  assert(uuid == UUID.fromString(
    SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_DST)
  )

  return value
}

fun BluetoothGattCharacteristic.deserializeGeneralAstroClockEnabled() : Boolean {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SCHEDULER))
  assert(uuid == UUID.fromString(
    SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_ASTRO_CLOCK_ENABLED)
  )

  return SNO110_BOOLEAN_TRUE == this.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
}

fun BluetoothGattCharacteristic.deserializeDaliUserDefinedDeviceName() : String {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_DEVICE_CONFIG))
  assert(uuid == UUID.fromString(
    SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_NAME)
  )

  return getStringValue(0)
}

fun BluetoothGattCharacteristic.deserializeDaliSchedulerFadeTime(): Int {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SCHEDULER))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_FADE_TIME
    )
  )

  return getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0).toUShort().reverseByteOrder().toInt()
}

fun BluetoothGattCharacteristic.deserializeLightControlBrightness(): Int {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_LIGHT_CONTROL))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_BRIGHTNESS
    )
  )

  return getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
}

fun BluetoothGattCharacteristic.deserializeSchedulerEnabled(): Boolean {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SCHEDULER))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_ENABLED
    )
  )

  return getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) == SNO110_BOOLEAN_TRUE
}

fun BluetoothGattCharacteristic.deserializeLightControlOutputRange() : Range<Int> {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_LIGHT_CONTROL))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_OUTPUT_RANGE_CONFIG
    )
  )

  val min = getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
  val max = getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1)

  return Range(min, max)
}

fun BluetoothGattCharacteristic.deserializeLightControlTransitionTime() : Int {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_LIGHT_CONTROL))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_TRANSITION_TIME
    )
  )

  return getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0).toUShort().reverseByteOrder().toInt()
}

fun BluetoothGattCharacteristic.deserializeSchedulerNumberOfEntries() : Int {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SCHEDULER))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_NUMBER_OF_ENTRIES
    )
  )

  val activeEntries = getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
  val freeEntries = getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1)

  return activeEntries
}

fun BluetoothGattCharacteristic.deserializeSchedulerScheduleEntries() : List<ScheduleEntry> {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SCHEDULER))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_CONTROL_POINT
    )
  )

  val stream = ByteArrayInputStream(value)

  val responseOpcode = stream.read()  // Should be 0x00

  val entries = mutableListOf<ScheduleEntry>()

  while (stream.available() >= 6) {
    val entryIndex = stream.read()

    val version = stream.read()
    val startDateYear = stream.read()
    val startDateMonth = stream.read()
    val startDateDay = stream.read()
    val recurrence = stream.read()
    val numberOfSteps = stream.read()

    val steps = mutableListOf<DimStep>()

    for (stepIndex in 0 until numberOfSteps) {
      if (stream.available() < 3) break

      val hour = stream.read()
      val minute = stream.read()
      val level = stream.read()

      steps.add(
        DimStep(
          hour = hour,
          minute = minute,
          level = level
        )
      )
    }

    val entry = ScheduleEntry(
      version = version,
      startDateYear = startDateYear,
      startDateMonth = startDateMonth,
      startDateDay = startDateDay,
      recurrence = recurrence,
      steps = steps
    )

    entries.add(entry)
  }

  return entries
}

fun BluetoothGattCharacteristic.deserializeSchedulerMaxNumberOfDimSteps() : Int {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SCHEDULER))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_MAX_DIM_STEPS
    )
  )

  return getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
}

//fun BluetoothGattCharacteristic.deserializeSecuritySignature() : ByteArray {
//  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SECURITY))
//  assert(
//    uuid == UUID.fromString(
//      SNO110_BLUETOOTH_CHARACTERISTIC_SECURITY_UID_SIGNATURE
//    )
//  )
//
//  return value
//}
