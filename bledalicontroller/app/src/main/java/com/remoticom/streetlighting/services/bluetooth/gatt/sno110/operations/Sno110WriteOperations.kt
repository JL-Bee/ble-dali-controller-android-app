package com.remoticom.streetlighting.services.bluetooth.gatt.sno110.operations

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Range
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.*
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.characteristics.ScheduleEntry

class Sno110WriteLightControlIdentifyOperation(value: Int) :
  Sno110WriteCharacteristicGattOperation<Int>(
    SNO110_BLUETOOTH_SERVICE_LIGHT_CONTROL,
    SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_IDENTIFY,
    value,
    BluetoothGattCharacteristic::serializeDeviceConfigIdentify
  )

//class Sno110WriteLightControlBrightnessOperation(value: Int?) :
//  Sno110WriteCharacteristicGattOperation<Int>(
//    SNO110_BLUETOOTH_SERVICE_LIGHT_CONTROL,
//    SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_BRIGHTNESS,
//    value,
//    BluetoothGattCharacteristic::serializeLightControlBrightness
//  )

class Sno110WriteLightControlTransitionTimeOperation(value: Int) :
  Sno110WriteCharacteristicGattOperation<Int>(
    SNO110_BLUETOOTH_SERVICE_LIGHT_CONTROL,
    SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_TRANSITION_TIME,
    value,
    BluetoothGattCharacteristic::serializeLightControlTransitionTime
  )

class Sno110WriteLightControlLightOutputRangeConfiguration(value: Range<Int>) :
  Sno110WriteCharacteristicGattOperation<Range<Int>>(
    SNO110_BLUETOOTH_SERVICE_LIGHT_CONTROL,
    SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_OUTPUT_RANGE_CONFIG,
    value,
    BluetoothGattCharacteristic::serializeLightControlLightOutputRangeConfiguration
  )

class Sno110WriteSchedulerEnabledOperation(enabled: Boolean) :
  Sno110WriteCharacteristicGattOperation<Boolean>(
    SNO110_BLUETOOTH_SERVICE_SCHEDULER,
    SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_ENABLED,
    enabled,
    BluetoothGattCharacteristic::serializeSchedulerEnabled
  )

class Sno110WriteSchedulerFadeTimeOperation(value: Int) :
  Sno110WriteCharacteristicGattOperation<Int>(
    SNO110_BLUETOOTH_SERVICE_SCHEDULER,
    SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_FADE_TIME,
    value,
    BluetoothGattCharacteristic::serializeSchedulerFadeTime
  )

class Sno110WriteAstroClockSwitchingEnabledOperation(enabled: Boolean) :
  Sno110WriteCharacteristicGattOperation<Boolean>(
    SNO110_BLUETOOTH_SERVICE_SCHEDULER,
    SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_ASTRO_CLOCK_ENABLED,
    enabled,
    BluetoothGattCharacteristic::serializeAstroClockSwitchingEnabled
  )

class Sno110WriteLightControlOnOperation(on: Boolean) :
  Sno110WriteCharacteristicGattOperation<Boolean>(
    SNO110_BLUETOOTH_SERVICE_LIGHT_CONTROL,
    SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_ON,
    on,
    BluetoothGattCharacteristic::serializeLightControlOn
  )

class Sno110WriteTimeTimeZoneOffsetOperation(offset: Int?) :
  Sno110WriteCharacteristicGattOperation<Int>(
    SNO110_BLUETOOTH_SERVICE_DEVICE_CONFIG,
    SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_TIME_ZONE_OFFSET,
    offset,
    BluetoothGattCharacteristic::serializeTimeTimeZoneOffset
  )

class Sno110WriteTimeDaylightSavingTimeOperation(struct: ByteArray?) :
  Sno110WriteCharacteristicGattOperation<ByteArray>(
    SNO110_BLUETOOTH_SERVICE_DEVICE_CONFIG,
    SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_DST,
    struct,
    BluetoothGattCharacteristic::serializeTimeDaylightSavingTime
  )

class Sno110WriteSchedulerStartWriteOperation() :
  Sno110WriteCharacteristicGattOperation<Int>(
    SNO110_BLUETOOTH_SERVICE_SCHEDULER,
    SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_CONTROL_POINT,
    SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_CONTROL_POINT_OPCODE_START_WRITE,
    BluetoothGattCharacteristic::serializeSchedulerControlPointOpcode
  )

class Sno110WriteSchedulerFinishWriteOperation() :
  Sno110WriteCharacteristicGattOperation<Int>(
    SNO110_BLUETOOTH_SERVICE_SCHEDULER,
    SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_CONTROL_POINT,
    SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_CONTROL_POINT_OPCODE_FINISH_WRITE,
    BluetoothGattCharacteristic::serializeSchedulerControlPointOpcode
  )

class Sno110WriteSchedulerEntryWriteOperation(entry: ScheduleEntry) :
  Sno110WriteCharacteristicGattOperation<ScheduleEntry>(
    SNO110_BLUETOOTH_SERVICE_SCHEDULER,
    SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_CONTROL_POINT,
    entry,
    BluetoothGattCharacteristic::serializeSchedulerControlPointScheduleEntry
  )
