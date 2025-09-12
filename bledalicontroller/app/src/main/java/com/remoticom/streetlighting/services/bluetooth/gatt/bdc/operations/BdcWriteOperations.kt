package com.remoticom.streetlighting.services.bluetooth.gatt.bdc.operations

import android.bluetooth.BluetoothGattCharacteristic
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.*
import com.remoticom.streetlighting.services.bluetooth.gatt.bdc.*

class BdcWriteGeneralModeOperation(value: GeneralMode?) :
  BdcWriteCharacteristicGattOperation<GeneralMode>(
    BDC_BLUETOOTH_SERVICE_GENERAL_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_GENERAL_MODE,
    value,
    BluetoothGattCharacteristic::serializeGeneralMode,
    BluetoothGattCharacteristic::deserializeGeneralMode
  )

class BdcWriteDimPresetOperation(value: DimPreset?) :
  BdcWriteCharacteristicGattOperation<DimPreset>(
    BDC_BLUETOOTH_SERVICE_DIM_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DIM_PRESET,
    value,
    BluetoothGattCharacteristic::serializeDimPreset,
    BluetoothGattCharacteristic::deserializeDimPreset
  )

class BdcWriteDimStepsOperation(value: List<DimStep>?) :
  BdcWriteCharacteristicGattOperation<List<DimStep>>(
    BDC_BLUETOOTH_SERVICE_DIM_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DIM_STEPS,
    value,
    BluetoothGattCharacteristic::serializeDimSteps,
    BluetoothGattCharacteristic::deserializeDimSteps
  )

class BdcWriteDimNominalLightLevelOperation(value: DimNominalLightLevel?) :
  BdcWriteCharacteristicGattOperation<DimNominalLightLevel>(
    BDC_BLUETOOTH_SERVICE_DIM_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DIM_NOMINAL_LIGHT_LEVEL,
    value,
    BluetoothGattCharacteristic::serializeDimNominalLightLevel,
    BluetoothGattCharacteristic::deserializeDimNominalLightLevel
  )

class BdcWriteDaliCloOperation(value: DaliClo?) :
  BdcWriteCharacteristicGattOperation<DaliClo>(
    BDC_BLUETOOTH_SERVICE_DALI_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DALI_CLO,
    value,
    BluetoothGattCharacteristic::serializeDaliClo,
    BluetoothGattCharacteristic::deserializeDaliClo
  )

class BdcWriteDaliPowerLevelOperation(value: DaliPowerLevel?) :
  BdcWriteCharacteristicGattOperation<DaliPowerLevel>(
    BDC_BLUETOOTH_SERVICE_DALI_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DALI_POWER_LEVEL,
    value,
    BluetoothGattCharacteristic::serializeDaliPowerLevel,
    BluetoothGattCharacteristic::deserializeDaliPowerLevel
  )

class BdcWriteDaliFadeTimeOperation(value: DaliFadeTime?) :
  BdcWriteCharacteristicGattOperation<DaliFadeTime>(
    BDC_BLUETOOTH_SERVICE_DALI_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DALI_FADE_TIME,
    value,
    BluetoothGattCharacteristic::serializeDaliFadeTime,
    BluetoothGattCharacteristic::deserializeDaliFadeTime
  )

class BdcWriteDaliMemoryRequestOperation(value: ByteArray?) :
  BdcWriteCharacteristicGattOperation<ByteArray>(
    BDC_BLUETOOTH_SERVICE_DALI_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_REQUEST,
    value,
    BluetoothGattCharacteristic::serializeDaliMemoryRequest,
    BluetoothGattCharacteristic::deserializeDaliMemoryRequest
  )

class BdcWriteTimeMidnightOffsetOperation(value: MidnightOffset?) :
  BdcWriteCharacteristicGattOperation<MidnightOffset>(
    BDC_BLUETOOTH_SERVICE_TIME_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_TIME_MIDNIGHT_OFFSET,
    value,
    BluetoothGattCharacteristic::serializeTimeMidnightOffset,
    BluetoothGattCharacteristic::deserializeTimeMidnightOffset
  )

class BdcWriteSecurityPasswordOperation(value: Int?) :
  BdcWriteCharacteristicGattOperation<Int>(
    BDC_BLUETOOTH_SERVICE_SECURITY_MASK,
    BDC_BLUETOOTH_CHARACTERISTIC_SECURITY_PASSWORD,
    value,
    BluetoothGattCharacteristic::serializeSecurityPassword,
    BluetoothGattCharacteristic::deserializeSecurityPassword
  )
