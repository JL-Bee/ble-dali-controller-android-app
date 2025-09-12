package com.remoticom.streetlighting.services.bluetooth.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.remoticom.streetlighting.services.bluetooth.data.BluetoothDeviceManager
import com.remoticom.streetlighting.services.bluetooth.data.Device
import com.remoticom.streetlighting.services.bluetooth.data.DeviceScanInfo
import com.remoticom.streetlighting.services.bluetooth.data.DeviceType
import com.remoticom.streetlighting.services.bluetooth.scanner.filters.BdcBluetoothScannerFilter
import com.remoticom.streetlighting.services.bluetooth.scanner.filters.Sno110BluetoothScannerFilter
import com.remoticom.streetlighting.services.bluetooth.scanner.filters.Zsc010BluetoothScannerFilter
import com.remoticom.streetlighting.services.bluetooth.utilities.toDeviceUUID
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

//const val BLUETOOTH_REMOTICOM_SCAN_FILTER_SERVICE_UUID = "A55D1C55-004B-FFFF-FFFF-FFFFFFFFFFFF"
//const val BLUETOOTH_REMOTICOM_SCAN_FILTER_UUID_MASK = "FFFFFFFF-FFFF-0000-0000-000000000000"

const val BLUETOOTH_SIGNIFY_SCAN_FILTER_SERVICE_UUID = "0000fe0f-0000-1000-8000-00805f9b34fb"
const val BLUETOOTH_SIGNIFY_SCAN_FILTER_SERVICE_DATA_FIRST_BYTE = 0x00.toByte()
const val BLUETOOTH_SIGNIFY_SCAN_FILTER_SERVICE_DATA_SECOND_BYTE = 0x30.toByte()

const val BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_IDENTIFIER = 0x060F

const val BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_UUID_INDEX = 0
const val BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_UUID_LENGTH = 16
//const val BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_UUID_SIG_INDEX = 16
//const val BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_UUID_SIG_LENGTH = 8
const val BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_HEALTH_INDEX = 24
const val BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_HEALTH_LENGTH = 2
const val BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_STATE_INDEX = 26
//const val BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_STATE_LENGTH = 1

//const val BLUETOOTH_SCAN_FILTER_SERVICE_UUID = "A55D1C55-004B-0000-0000-000000000000"
//const val BLUETOOTH_SCAN_FILTER_SERVICE_MASK = "FFFFFFFF-FFFF-0000-0000-000000000000"

class BluetoothScannerService private constructor(
  private val context: Context,
) :
  ScannerService,
  BluetoothDeviceManager
{
  data class DeviceImpl(
    override val uuid: String,
    val device: BluetoothDevice,
    override val type: DeviceType,
    override val serviceUuids: List<ParcelUuid>? = null,
    override val health: Int? = null,
    override val state: Int? = null
  ) : Device {
    override val address: String = device.address
    @SuppressLint("MissingPermission")
    override val name: String? = device.name
  }

  override fun bluetoothDeviceFor(device: Device) = (device as? DeviceImpl)?.device

  private val _state = MutableLiveData(ScannerService.State())
  override val state: LiveData<ScannerService.State> = _state

  val zsc010Filter = Zsc010BluetoothScannerFilter()
  val sno110Filter = Sno110BluetoothScannerFilter()
  val bdcFilter = BdcBluetoothScannerFilter()

  private var serviceState = ScannerService.State()
    set(value) {
      field = value
      _state.postValue(value)
    }

  // The default local adapter, or null if Bluetooth is not supported on this hardware platform
  // (getDefaultAdapter is now deprecated)
  // private val bluetoothScanner: BluetoothLeScanner? =
  //   BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner

  // New way of retrieving adapter (and scanner)
  private val bluetoothScanner: BluetoothLeScanner? = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.bluetoothLeScanner

  private val callback: ScanCallback = object : ScanCallback() {
    @SuppressLint("MissingPermission")
    override fun onScanResult(callbackType: Int, result: ScanResult?) {
      super.onScanResult(callbackType, result)
      Log.i(
        TAG,
        "start/onScanResult: ${result?.device?.address} - ${result?.device?.name}"
      )

      val serviceUuids = result?.scanRecord?.serviceUuids
      Log.d(TAG, "First advertised service UUID=${serviceUuids?.first()}")

//      val isZsc010Device = zsc010Filter.matches(result)
//
//      val isSno110Device = sno110Filter.matches(result)

      val isBdcDevice = bdcFilter.matches(result)

      val type = when {
//          isZsc010Device -> {
//            Log.d(TAG, "Remoticom device: ZSC010")
//
//            DeviceType.Zsc010
//          }
          isBdcDevice -> {
            // This app currently only supports BDC
            Log.d(TAG, "Remoticom device:  EnergyNode Inset")

            DeviceType.Bdc
          }
//          isSno110Device -> {
//            Log.d(TAG, "Signify device")
//
//            DeviceType.Sno110
//          }
          else -> {
            Log.d(TAG, "Unknown device")

            return
          }
      }

      val uuid = when (type) {
        // Based on address
        DeviceType.Zsc010 -> result?.toDeviceUUID()

        // This app currently only supports BDC
        DeviceType.Bdc -> result?.toDeviceUUID()

        // Based on manufacturer specific data
        DeviceType.Sno110 -> createUUIDFromSignifyData(result)
      } ?: return

      val device = result?.device ?: return

      val health = when (type) {
        DeviceType.Zsc010 -> null
        DeviceType.Bdc -> null
        DeviceType.Sno110 -> readHealthFromSignifyData(result)
      }

      val state = when (type) {
        DeviceType.Zsc010 -> null
        DeviceType.Bdc -> null
        DeviceType.Sno110 -> readStateFromSignifyData(result)
      }

      val results = serviceState.results.toMutableMap()
      results[uuid] = DeviceScanInfo(DeviceImpl(uuid, device, type, serviceUuids, health, state), result.rssi)

      Log.d(TAG, "UUID=$uuid")

      serviceState = serviceState.copy(results = results.toMap())
    }

    override fun onScanFailed(errorCode: Int) {
      super.onScanFailed(errorCode)
      Log.i(TAG, "start/onScanFailed: $errorCode")

      serviceState = serviceState.copy(isScanning = true, errorCode = errorCode)
    }
  }

  override fun startScan() {
    serviceState = ScannerService.State(isScanning = true)

    try {
      bluetoothScanner?.startScan(
        null, ScanSettings.Builder().build(),
        callback
      )
    } catch (ex: SecurityException) {
      Log.w(TAG, "startScan not allowed: ${ex.message}")
    }
  }

//    fun startScanMock(filter : List<ScanFilter>?, resultBlock: (Device) -> Unit) {
//        isScanningChannel.offer(true)
//
//        resultBlock(MockDevice("02:80:E1:80:00:01", "RTM-TRT", -20))
//    }

  override fun stopScan(isTimeout: Boolean) {
    // Needs to be same callback reference as used with start
    // Otherwise: "could not find callback wrapper"
    try {
      bluetoothScanner?.stopScan(
        callback
      )
    } catch (ex: SecurityException) {
      Log.w(TAG, "stopScan not allowed: ${ex.message}")
    }

    val isEmptyTimeout = isTimeout && serviceState.results.count() == 0

    serviceState = serviceState.copy(
      isScanning = false,
      hasTimedOutWithoutResults = isEmptyTimeout
    )
  }

  private fun signifyData(result: ScanResult?) : ByteArray? {
    val scanRecord = result?.scanRecord ?: return null

    return scanRecord.manufacturerSpecificData.get(BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_IDENTIFIER, null)
  }

  private fun readHealthFromSignifyData(result: ScanResult?) : Int? {
    val signifyData = signifyData(result) ?: return null

    val healthBytes = signifyData.copyOfRange(
      BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_HEALTH_INDEX, BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_HEALTH_INDEX + BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_HEALTH_LENGTH)

    return ByteBuffer.wrap(healthBytes).short.toInt()
  }

  private fun readStateFromSignifyData(result: ScanResult?) : Int? {
    val signifyData = signifyData(result) ?: return null

    return signifyData[BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_STATE_INDEX].toInt()
  }

  private fun createUUIDFromSignifyData(result: ScanResult?) : String? {
    val signifyData = signifyData(result) ?: return null

    // 16 byte UUID of the product, in little endian
    val uuidFromDeviceBytes = signifyData.copyOfRange(
      BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_UUID_INDEX,
      BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_UUID_INDEX + BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_UUID_LENGTH
    )
    val uuidFromDeviceLeastSignificant = ByteBuffer
      .wrap(uuidFromDeviceBytes, 0, 8)
      .order(ByteOrder.LITTLE_ENDIAN)
      .long

    val uuidFromDeviceMostSignificant = ByteBuffer
      .wrap(uuidFromDeviceBytes, 8, 8)
      .order(ByteOrder.LITTLE_ENDIAN)
      .long

    val uuidFromDevice = UUID(
      uuidFromDeviceMostSignificant,
      uuidFromDeviceLeastSignificant
    )

    Log.d(TAG, "UUID from device: $uuidFromDevice")

//    // 8 byte UUID signature of the product, in little endian
//    val uuidSignatureFromDeviceBytes = signifyData.copyOfRange(
//      BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_UUID_SIG_INDEX,
//      BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_UUID_SIG_INDEX + BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_UUID_SIG_LENGTH
//    )
//
//    // Device Health (see below) / 2 bytes
//    val health = signifyData.copyOfRange(
//      BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_HEALTH_INDEX,
//      BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_HEALTH_INDEX + BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_HEALTH_LENGTH
//    )
//
//    // Device State (see below) / 1 byte
//    val state = signifyData.copyOfRange(
//      BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_STATE_INDEX,
//      BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_STATE_INDEX + BLUETOOTH_SIGNIFY_MANUFACTURER_SPECIFIC_DATA_STATE_LENGTH
//    )

    return uuidFromDevice.toString().lowercase(Locale.ROOT)
  }

//  private fun createNameFilter(deviceName: String): List<ScanFilter> {
//    return listOf<ScanFilter>(
//      ScanFilter.Builder().setDeviceName(deviceName).build()
//    )
//  }
//
//  private fun createServiceMaskFilter(serviceUuid: ParcelUuid?, uuidMask: ParcelUuid?): List<ScanFilter> {
//
//    if (null == serviceUuid) return emptyList()
//
//    val uuidMaskForFilter = uuidMask ?: ParcelUuid.fromString("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF")
//
//    return listOf<ScanFilter>(
//      ScanFilter.Builder().setServiceUuid(
//        serviceUuid,
//        uuidMaskForFilter
//      ).build()
//    )
//  }

  companion object {
    private const val TAG = "BluetoothScannerService"

    @Volatile
    private var instance: BluetoothScannerService? = null

    fun getInstance(context: Context) =
      instance
        ?: synchronized(this) {
          instance
            ?: BluetoothScannerService(context = context).also {
              instance = it
            }
        }
  }
}
