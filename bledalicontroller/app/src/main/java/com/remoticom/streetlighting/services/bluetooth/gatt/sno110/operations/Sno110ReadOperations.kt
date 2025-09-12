package com.remoticom.streetlighting.services.bluetooth.gatt.sno110.operations

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Range
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DiagnosticsVersion
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.GpsPosition
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.UnixTimestamp
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.Version
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.*

class Sno110ReadDiagnosticsFirmwareVersionOperation() :
  Sno110ReadCharacteristicGattOperation<Version>(
    SNO110_BLUETOOTH_SERVICE_DEVICE_INFO,
    SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_INFO_FIRMWARE_VERSION,
    BluetoothGattCharacteristic::deserializeDiagnosticsVersion
  )

class Sno110ReadDiagnosticsSoftwareVersionOperation() :
  Sno110ReadCharacteristicGattOperation<Version>(
    SNO110_BLUETOOTH_SERVICE_DEVICE_INFO,
    SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_INFO_SOFTWARE_VERSION,
    BluetoothGattCharacteristic::deserializeDiagnosticsVersion
  )

class Sno110ReadGpsPositionOperation() :
  Sno110ReadCharacteristicGattOperation<GpsPosition>(
    SNO110_BLUETOOTH_SERVICE_DEVICE_CONFIG,
    SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_GEOLOCATION,
    BluetoothGattCharacteristic::deserializeGpsPosition
  )

class Sno110ReadTimeUTCOperation() :
  Sno110ReadCharacteristicGattOperation<UnixTimestamp>(
    SNO110_BLUETOOTH_SERVICE_DEVICE_CONFIG,
    SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_UTC,
    BluetoothGattCharacteristic::deserializeTimeUnixTimstamp
  )

class Sno110ReadTimeTimeZoneOffsetOperation() :
  Sno110ReadCharacteristicGattOperation<Int>(
    SNO110_BLUETOOTH_SERVICE_DEVICE_CONFIG,
    SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_TIME_ZONE_OFFSET,
    BluetoothGattCharacteristic::deserializeTimeTimeZoneOffset
  )

class Sno110ReadTimeTimeZoneIndexOperation() :
  Sno110ReadCharacteristicGattOperation<String>(
    SNO110_BLUETOOTH_SERVICE_DEVICE_CONFIG,
    SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_TIME_ZONE_INDEX,
    BluetoothGattCharacteristic::deserializeTimeTimeZoneIndex
  )

class Sno110ReadTimeDaylightSavingTimeOperation() :
  Sno110ReadCharacteristicGattOperation<ByteArray>(
    SNO110_BLUETOOTH_SERVICE_DEVICE_CONFIG,
    SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_DST,
    BluetoothGattCharacteristic::deserializeTimeDaylightSavingTime
  )

class Sno110ReadGeneralAstroClockSwitchingEnabledOperation() :
  Sno110ReadCharacteristicGattOperation<Boolean>(
    SNO110_BLUETOOTH_SERVICE_SCHEDULER,
    SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_ASTRO_CLOCK_ENABLED,
    BluetoothGattCharacteristic::deserializeGeneralAstroClockEnabled
  )

class Sno110ReadDaliUserDefinedDeviceNameOperation() :
  Sno110ReadCharacteristicGattOperation<String>(
    SNO110_BLUETOOTH_SERVICE_DEVICE_CONFIG,
    SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_NAME,
    BluetoothGattCharacteristic::deserializeDaliUserDefinedDeviceName
  )

class Sno110ReadDaliSchedulerFadeTimeOperation() :
  Sno110ReadCharacteristicGattOperation<Int>(
    SNO110_BLUETOOTH_SERVICE_SCHEDULER,
    SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_FADE_TIME,
    BluetoothGattCharacteristic::deserializeDaliSchedulerFadeTime
  )

//class Sno110ReadLightControlBrightnessOperation() :
//  Sno110ReadCharacteristicGattOperation<Int>(
//    SNO110_BLUETOOTH_SERVICE_LIGHT_CONTROL,
//    SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_BRIGHTNESS,
//    BluetoothGattCharacteristic::deserializeLightControlBrightness
//  )

class Sno110ReadSchedulerEnabledOperation() :
  Sno110ReadCharacteristicGattOperation<Boolean>(
    SNO110_BLUETOOTH_SERVICE_SCHEDULER,
    SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_ENABLED,
    BluetoothGattCharacteristic::deserializeSchedulerEnabled
  )

class Sno110ReadLightControlOutputRangeConfigurationOperation() :
  Sno110ReadCharacteristicGattOperation<Range<Int>>(
    SNO110_BLUETOOTH_SERVICE_LIGHT_CONTROL,
    SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_OUTPUT_RANGE_CONFIG,
    BluetoothGattCharacteristic::deserializeLightControlOutputRange
  )

class Sno110ReadLightControlTransitionTimeOperation() :
  Sno110ReadCharacteristicGattOperation<Int>(
    SNO110_BLUETOOTH_SERVICE_LIGHT_CONTROL,
    SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_TRANSITION_TIME,
    BluetoothGattCharacteristic::deserializeLightControlTransitionTime
  )

class Sno110ReadSchedulerNumberOfEntriesOperation() :
  Sno110ReadCharacteristicGattOperation<Int>(
    SNO110_BLUETOOTH_SERVICE_SCHEDULER,
    SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_NUMBER_OF_ENTRIES,
    BluetoothGattCharacteristic::deserializeSchedulerNumberOfEntries
  )

class Sno110ReadSchedulerMaxNumberOfDimStepsOperation() :
  Sno110ReadCharacteristicGattOperation<Int>(
    SNO110_BLUETOOTH_SERVICE_SCHEDULER,
    SNO110_BLUETOOTH_CHARACTERISTIC_SCHEDULER_MAX_DIM_STEPS,
    BluetoothGattCharacteristic::deserializeSchedulerMaxNumberOfDimSteps
  )

//class Sno110ReadSecuritySignatureOperation() :
//  Sno110ReadCharacteristicGattOperation<ByteArray>(
//    SNO110_BLUETOOTH_SERVICE_SECURITY,
//    SNO110_BLUETOOTH_CHARACTERISTIC_SECURITY_UID_SIGNATURE,
//    BluetoothGattCharacteristic::deserializeSecuritySignature
//  )
