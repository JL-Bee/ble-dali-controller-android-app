package com.remoticom.streetlighting.services.bluetooth.gatt.zsc010

import android.bluetooth.BluetoothGattCharacteristic
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.*
import com.remoticom.streetlighting.services.bluetooth.gatt.zsc010.operations.Zsc010ReadCharacteristicGattOperation


class Zsc010ReadGeneralModeOperation() :
  Zsc010ReadCharacteristicGattOperation<GeneralMode>(
    ZSC010_BLUETOOTH_SERVICE_GENERAL_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_GENERAL_MODE,
    BluetoothGattCharacteristic::deserializeGeneralMode
  )

class Zsc010ReadDimPresetOperation() :
  Zsc010ReadCharacteristicGattOperation<DimPreset>(
    ZSC010_BLUETOOTH_SERVICE_DIM_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_DIM_PRESET,
    BluetoothGattCharacteristic::deserializeDimPreset
  )

class Zsc010ReadDimStepsOperation() :
  Zsc010ReadCharacteristicGattOperation<List<DimStep>>(
    ZSC010_BLUETOOTH_SERVICE_DIM_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_DIM_STEPS,
    BluetoothGattCharacteristic::deserializeDimSteps
  )

class Zsc010ReadDaliCloOperation() :
  Zsc010ReadCharacteristicGattOperation<DaliClo>(
    ZSC010_BLUETOOTH_SERVICE_DALI_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_DALI_CLO,
    BluetoothGattCharacteristic::deserializeDaliClo
  )

class Zsc010ReadDaliPowerLevelOperation() :
  Zsc010ReadCharacteristicGattOperation<DaliPowerLevel>(
    ZSC010_BLUETOOTH_SERVICE_DALI_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_DALI_POWER_LEVEL,
    BluetoothGattCharacteristic::deserializeDaliPowerLevel
  )

class Zsc010ReadDaliAvailablePowerLevelsOperation() :
  Zsc010ReadCharacteristicGattOperation<List<DaliPowerLevel>>(
    ZSC010_BLUETOOTH_SERVICE_DALI_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_DALI_AVAILABLE_POWER_LEVELS,
    BluetoothGattCharacteristic::deserializeDaliAvailablePowerLevels
  )

class Zsc010ReadDaliFixtureNameOperation() :
  Zsc010ReadCharacteristicGattOperation<String>(
    ZSC010_BLUETOOTH_SERVICE_DALI_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_DALI_FIXTURE_NAME,
    BluetoothGattCharacteristic::deserializeDaliFixtureName
  )

class Zsc010ReadDaliFadeTimeOperation() :
  Zsc010ReadCharacteristicGattOperation<DaliFadeTime>(
    ZSC010_BLUETOOTH_SERVICE_DALI_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_DALI_FADE_TIME,
    BluetoothGattCharacteristic::deserializeDaliFadeTime
  )

class Zsc010ReadTimeTimeZoneOperation() :
  Zsc010ReadCharacteristicGattOperation<TimeZone>(
    ZSC010_BLUETOOTH_SERVICE_TIME_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_TIME_TIMEZONE,
    BluetoothGattCharacteristic::deserializeTimeTimeZone
  )

class Zsc010ReadTimeUnixTimestampOperation() :
  Zsc010ReadCharacteristicGattOperation<UnixTimestamp>(
    ZSC010_BLUETOOTH_SERVICE_TIME_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_TIME_UNIX_TIMESTAMP,
    BluetoothGattCharacteristic::deserializeTimeUnixTimestamp
  )

class Zsc010ReadGpsPositionOperation() :
  Zsc010ReadCharacteristicGattOperation<GpsPosition>(
    ZSC010_BLUETOOTH_SERVICE_GPS_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_GPS_POSITION,
    BluetoothGattCharacteristic::deserializeGpsPosition
  )

class Zsc010ReadDiagnosticsStatusOperation() :
  Zsc010ReadCharacteristicGattOperation<DiagnosticsStatus>(
    ZSC010_BLUETOOTH_SERVICE_DIAGNOSTICS_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_DIAGNOSTICS_STATUS,
    BluetoothGattCharacteristic::deserializeDiagnosticsStatus
  )

class Zsc010ReadDiagnosticsVersionOperation() :
  Zsc010ReadCharacteristicGattOperation<DiagnosticsVersion>(
    ZSC010_BLUETOOTH_SERVICE_DIAGNOSTICS_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_DIAGNOSTICS_VERSION,
    BluetoothGattCharacteristic::deserializeDiagnosticsVersion
  )
