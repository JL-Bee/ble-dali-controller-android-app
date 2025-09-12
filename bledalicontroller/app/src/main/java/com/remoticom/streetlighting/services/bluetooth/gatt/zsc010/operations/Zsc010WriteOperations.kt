package com.remoticom.streetlighting.services.bluetooth.gatt.zsc010.operations

import android.bluetooth.BluetoothGattCharacteristic
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.*
import com.remoticom.streetlighting.services.bluetooth.gatt.zsc010.*

class Zsc010WriteGeneralModeOperation(value: GeneralMode?) :
  Zsc010WriteCharacteristicGattOperation<GeneralMode>(
    ZSC010_BLUETOOTH_SERVICE_GENERAL_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_GENERAL_MODE,
    value,
    BluetoothGattCharacteristic::serializeGeneralMode,
    BluetoothGattCharacteristic::deserializeGeneralMode
  )

class Zsc010WriteDimPresetOperation(value: DimPreset?) :
  Zsc010WriteCharacteristicGattOperation<DimPreset>(
    ZSC010_BLUETOOTH_SERVICE_DIM_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_DIM_PRESET,
    value,
    BluetoothGattCharacteristic::serializeDimPreset,
    BluetoothGattCharacteristic::deserializeDimPreset
  )

class Zsc010WriteDimStepsOperation(value: List<DimStep>?) :
  Zsc010WriteCharacteristicGattOperation<List<DimStep>>(
    ZSC010_BLUETOOTH_SERVICE_DIM_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_DIM_STEPS,
    value,
    BluetoothGattCharacteristic::serializeDimSteps,
    BluetoothGattCharacteristic::deserializeDimSteps
  )

class Zsc010WriteDaliCloOperation(value: DaliClo?) :
  Zsc010WriteCharacteristicGattOperation<DaliClo>(
    ZSC010_BLUETOOTH_SERVICE_DALI_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_DALI_CLO,
    value,
    BluetoothGattCharacteristic::serializeDaliClo,
    BluetoothGattCharacteristic::deserializeDaliClo
  )

class Zsc010WriteDaliPowerLevelOperation(value: DaliPowerLevel?) :
  Zsc010WriteCharacteristicGattOperation<DaliPowerLevel>(
    ZSC010_BLUETOOTH_SERVICE_DALI_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_DALI_POWER_LEVEL,
    value,
    BluetoothGattCharacteristic::serializeDaliPowerLevel,
    BluetoothGattCharacteristic::deserializeDaliPowerLevel
  )

class Zsc010WriteDaliFadeTimeOperation(value: DaliFadeTime?) :
  Zsc010WriteCharacteristicGattOperation<DaliFadeTime>(
    ZSC010_BLUETOOTH_SERVICE_DALI_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_DALI_FADE_TIME,
    value,
    BluetoothGattCharacteristic::serializeDaliFadeTime,
    BluetoothGattCharacteristic::deserializeDaliFadeTime
  )

class Zsc010WriteTimeTimeZoneOperation(value: TimeZone?) :
  Zsc010WriteCharacteristicGattOperation<TimeZone>(
    ZSC010_BLUETOOTH_SERVICE_TIME_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_TIME_TIMEZONE,
    value,
    BluetoothGattCharacteristic::serializeTimeTimeZone,
    BluetoothGattCharacteristic::deserializeTimeTimeZone
  )

class Zsc010WriteSecurityPasswordOperation(value: Int?) :
  Zsc010WriteCharacteristicGattOperation<Int>(
    ZSC010_BLUETOOTH_SERVICE_SECURITY_MASK,
    ZSC010_BLUETOOTH_CHARACTERISTIC_SECURITY_PASSWORD,
    value,
    BluetoothGattCharacteristic::serializeSecurityPassword,
    BluetoothGattCharacteristic::deserializeSecurityPassword
  )
