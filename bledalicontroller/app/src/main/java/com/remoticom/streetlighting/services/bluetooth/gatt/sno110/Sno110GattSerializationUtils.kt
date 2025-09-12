package com.remoticom.streetlighting.services.bluetooth.gatt.sno110

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Range
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.characteristics.ScheduleEntry
import io.ktor.utils.io.bits.*
import java.util.*

fun BluetoothGattCharacteristic.serializeChallenge(value: ByteArray?): Boolean {
  assert(this.service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SECURITY))

  assert(
    this.uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_SECURITY_AUTHENTICATE
    )
  )

  if (null == value) return false

  val success = setValue(value)

  return success
}

fun BluetoothGattCharacteristic.serializeToken(value: ByteArray?): Boolean {
  assert(this.service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SECURITY))

  assert(
    this.uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_SECURITY_AUTHENTICATE
    )
  )

  if (null == value) return false

  val success = setValue(value)

  return success
}

fun BluetoothGattCharacteristic.deserializeToken(): ByteArray {
  assert(this.service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SECURITY))

  assert(
    this.uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_SECURITY_AUTHENTICATE
    )
  )

  return this.value
}

fun BluetoothGattCharacteristic.serializeDeviceConfigIdentify(routine: Int?): Boolean {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_LIGHT_CONTROL))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_IDENTIFY
    )
  )

  if (null == routine) return false

  return setValue(routine, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
}

fun BluetoothGattCharacteristic.serializeLightControlBrightness(brightness: Int?) : Boolean {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_LIGHT_CONTROL))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_BRIGHTNESS
    )
  )

  if (null == brightness) return false

  return setValue(brightness, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
}

fun BluetoothGattCharacteristic.serializeLightControlTransitionTime(transitionTime: Int?) : Boolean {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_LIGHT_CONTROL))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_TRANSITION_TIME
    )
  )

  if (null == transitionTime) return false

  return setValue(transitionTime.toUShort().reverseByteOrder().toInt(), BluetoothGattCharacteristic.FORMAT_UINT16, 0)
}

fun BluetoothGattCharacteristic.serializeLightControlLightOutputRangeConfiguration(configuration: Range<Int>?) : Boolean {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_LIGHT_CONTROL))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_OUTPUT_RANGE_CONFIG
    )
  )

  if (null == configuration) return false

  val serializedConfiguration = ByteArray(2)

  serializedConfiguration[0] = configuration.lower.toByte()
  serializedConfiguration[1] = configuration.upper.toByte()

  return setValue(serializedConfiguration)
}


fun BluetoothGattCharacteristic.serializeSchedulerEnabled(enabled: Boolean?) : Boolean {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SCHEDULER))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_ENABLED
    )
  )

  if (null == enabled) return false

  return setValue(if (enabled) SNO110_BOOLEAN_TRUE else SNO110_BOOLEAN_FALSE, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
}

fun BluetoothGattCharacteristic.serializeSchedulerFadeTime(fadeTime: Int?) : Boolean {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SCHEDULER))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_FADE_TIME
    )
  )

  if (null == fadeTime) return false

  return setValue(fadeTime.toUShort().reverseByteOrder().toInt(), BluetoothGattCharacteristic.FORMAT_UINT16, 0)
}

fun BluetoothGattCharacteristic.serializeAstroClockSwitchingEnabled(enabled: Boolean?) : Boolean {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SCHEDULER))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_ASTRO_CLOCK_ENABLED
    )
  )

  if (null == enabled) return false

  return setValue(if (enabled) SNO110_BOOLEAN_TRUE else SNO110_BOOLEAN_FALSE, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
}

fun BluetoothGattCharacteristic.serializeLightControlOn(on: Boolean?) : Boolean {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_LIGHT_CONTROL))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_ON
    )
  )

  if (null == on) return false

  return setValue(if (on) SNO110_BOOLEAN_TRUE else SNO110_BOOLEAN_FALSE, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
}

fun BluetoothGattCharacteristic.serializeSchedulerControlPointOpcode(opcode: Int?) : Boolean {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SCHEDULER))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_CONTROL_POINT
    )
  )

  if (null == opcode) return false

  return setValue(opcode, BluetoothGattCharacteristic.FORMAT_UINT8, 0)
}

fun BluetoothGattCharacteristic.serializeTimeTimeZoneOffset(offset: Int?) : Boolean {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_DEVICE_CONFIG))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_TIME_ZONE_OFFSET
    )
  )

  if (null == offset) return false

  val offsetInSeconds = (offset * 3600).reverseByteOrder()

  return setValue(offsetInSeconds, BluetoothGattCharacteristic.FORMAT_SINT32, 0)
}

fun BluetoothGattCharacteristic.serializeTimeDaylightSavingTime(struct: ByteArray?) : Boolean {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_DEVICE_CONFIG))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_DST
    )
  )

  if (null == struct) return false

  return setValue(struct)
}

fun BluetoothGattCharacteristic.serializeSchedulerControlPointScheduleEntry(entry: ScheduleEntry?) : Boolean {
  assert(service.uuid == UUID.fromString(SNO110_BLUETOOTH_SERVICE_SCHEDULER))
  assert(
    uuid == UUID.fromString(
      SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_CONTROL_POINT
    )
  )

  if (null == entry) return false

  if (entry.steps.isEmpty()) return false

  val bytesSize =
    1 + SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_CONTROL_POINT_ENTRY_HEADER_SIZE +
      SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_CONTROL_POINT_ENTRY_DIM_STEP_SIZE * entry.steps.size

  val serializedEntry = ByteArray(bytesSize)

  serializedEntry[0] = SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_CONTROL_POINT_OPCODE_WRITE_ENTRY.toByte()
  serializedEntry[1] = 0x02.toByte()
  serializedEntry[2] = entry.startDateYear.toByte()
  serializedEntry[3] = entry.startDateMonth.toByte()
  serializedEntry[4] = entry.startDateDay.toByte()
  serializedEntry[5] = entry.recurrence.toByte()
  serializedEntry[6] = entry.steps.size.toByte()

  for (stepIndex in entry.steps.indices) {
    serializedEntry[7 + stepIndex * SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_CONTROL_POINT_ENTRY_DIM_STEP_SIZE] = entry.steps[stepIndex].hour.toByte()
    serializedEntry[8 + stepIndex * SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_CONTROL_POINT_ENTRY_DIM_STEP_SIZE] = entry.steps[stepIndex].minute.toByte()
    serializedEntry[9 + stepIndex * SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_CONTROL_POINT_ENTRY_DIM_STEP_SIZE] = entry.steps[stepIndex].level.toByte()
  }

  return setValue(serializedEntry)
}
