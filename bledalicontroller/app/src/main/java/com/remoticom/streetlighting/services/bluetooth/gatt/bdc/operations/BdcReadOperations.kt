package com.remoticom.streetlighting.services.bluetooth.gatt.bdc

import android.bluetooth.BluetoothGattCharacteristic
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.*
import com.remoticom.streetlighting.services.bluetooth.gatt.bdc.operations.BdcReadCharacteristicGattOperation


class BdcReadGeneralModeOperation() :
  BdcReadCharacteristicGattOperation<GeneralMode>(
    BDC_BLUETOOTH_SERVICE_GENERAL_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_GENERAL_MODE,
    BluetoothGattCharacteristic::deserializeGeneralMode
  )

class BdcReadGeneralNameOperation() :
  BdcReadCharacteristicGattOperation<String>(
    BDC_BLUETOOTH_SERVICE_GENERAL_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_GENERAL_NAME,
    BluetoothGattCharacteristic::deserializeGeneralName
  )

class BdcReadDimPresetOperation() :
  BdcReadCharacteristicGattOperation<DimPreset>(
    BDC_BLUETOOTH_SERVICE_DIM_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DIM_PRESET,
    BluetoothGattCharacteristic::deserializeDimPreset
  )

class BdcReadDimStepsOperation() :
  BdcReadCharacteristicGattOperation<List<DimStep>>(
    BDC_BLUETOOTH_SERVICE_DIM_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS,
    BluetoothGattCharacteristic::deserializeDimSteps
  )

class BdcReadDimNominalLightLevelOperation() :
  BdcReadCharacteristicGattOperation<DimNominalLightLevel>(
    BDC_BLUETOOTH_SERVICE_DIM_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DIM_NOMINAL_LIGHT_LEVEL,
    BluetoothGattCharacteristic::deserializeDimNominalLightLevel
  )

class BdcReadDaliCloOperation() :
  BdcReadCharacteristicGattOperation<DaliClo>(
    BDC_BLUETOOTH_SERVICE_DALI_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DALI_CLO,
    BluetoothGattCharacteristic::deserializeDaliClo
  )

class BdcReadDaliPowerLevelOperation() :
  BdcReadCharacteristicGattOperation<DaliPowerLevel>(
    BDC_BLUETOOTH_SERVICE_DALI_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DALI_POWER_LEVEL,
    BluetoothGattCharacteristic::deserializeDaliPowerLevel
  )

class BdcReadDaliAvailablePowerLevelsOperation() :
  BdcReadCharacteristicGattOperation<List<DaliPowerLevel>>(
    BDC_BLUETOOTH_SERVICE_DALI_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DALI_AVAILABLE_POWER_LEVELS,
    BluetoothGattCharacteristic::deserializeDaliAvailablePowerLevels
  )

class BdcReadDaliFixtureNameOperation() :
  BdcReadCharacteristicGattOperation<String>(
    BDC_BLUETOOTH_SERVICE_DALI_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DALI_FIXTURE_NAME,
    BluetoothGattCharacteristic::deserializeDaliFixtureName
  )

class BdcReadDaliFadeTimeOperation() :
  BdcReadCharacteristicGattOperation<DaliFadeTime>(
    BDC_BLUETOOTH_SERVICE_DALI_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DALI_FADE_TIME,
    BluetoothGattCharacteristic::deserializeDaliFadeTime
  )

class BdcReadDaliMemoryResponseOperation() :
  BdcReadCharacteristicGattOperation<ByteArray>(
    BDC_BLUETOOTH_SERVICE_DALI_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_RESPONSE,
    BluetoothGattCharacteristic::deserializeDaliMemoryResponse
  )

class BdcReadTimeTimeZoneOperation() :
  BdcReadCharacteristicGattOperation<TimeZone>(
    BDC_BLUETOOTH_SERVICE_TIME_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_TIME_TIMEZONE,
    BluetoothGattCharacteristic::deserializeTimeTimeZone
  )

class BdcReadTimeUnixTimestampOperation() :
  BdcReadCharacteristicGattOperation<UnixTimestamp>(
    BDC_BLUETOOTH_SERVICE_TIME_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_TIME_UNIX_TIMESTAMP,
    BluetoothGattCharacteristic::deserializeTimeUnixTimestamp
  )

class BdcReadTimeMidnightOffsetOperation() :
  BdcReadCharacteristicGattOperation<MidnightOffset>(
    BDC_BLUETOOTH_SERVICE_TIME_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_TIME_MIDNIGHT_OFFSET,
    BluetoothGattCharacteristic::deserializeTimeMidnightOffset
  )

class BdcReadGpsPositionOperation() :
  BdcReadCharacteristicGattOperation<GpsPosition>(
    BDC_BLUETOOTH_SERVICE_GPS_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_GPS_POSITION,
    BluetoothGattCharacteristic::deserializeGpsPosition
  )

class BdcReadDiagnosticsStatusOperation() :
  BdcReadCharacteristicGattOperation<DiagnosticsStatus>(
    BDC_BLUETOOTH_SERVICE_DIAGNOSTICS_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DIAGNOSTICS_STATUS,
    BluetoothGattCharacteristic::deserializeDiagnosticsStatus
  )

class BdcReadDiagnosticsVersionOperation() :
  BdcReadCharacteristicGattOperation<DiagnosticsVersion>(
    BDC_BLUETOOTH_SERVICE_DIAGNOSTICS_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DIAGNOSTICS_VERSION,
    BluetoothGattCharacteristic::deserializeDiagnosticsVersion
  )
