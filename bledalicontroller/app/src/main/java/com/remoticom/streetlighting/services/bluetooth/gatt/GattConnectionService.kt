package com.remoticom.streetlighting.services.bluetooth.gatt

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.remoticom.streetlighting.services.bluetooth.data.BluetoothDeviceManager
import com.remoticom.streetlighting.services.bluetooth.data.Device
import com.remoticom.streetlighting.services.bluetooth.data.DeviceType
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DeviceCharacteristics
import com.remoticom.streetlighting.services.bluetooth.gatt.bdc.*
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.*
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.Sno110OperationsService
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.operations.Sno110WriteCharacteristicGattOperation
import com.remoticom.streetlighting.services.bluetooth.gatt.zsc010.*
import com.remoticom.streetlighting.services.web.TokenProvider
import com.remoticom.streetlighting.services.web.data.Peripheral
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.IllegalStateException
import android.Manifest
import com.remoticom.streetlighting.utilities.BluetoothPermissionProvider


class GattConnectionService constructor(
  private val scope: CoroutineScope,
  private val context: Context,
  private val deviceManager: BluetoothDeviceManager,
  private val keepAliveInterval: Long = DEFAULT_KEEP_ALIVE_INTERVAL
) : ConnectionService, ConnectionProvider {

  private var operationServices: Map<DeviceType, OperationsService> = mapOf(
    DeviceType.Zsc010 to Zsc010OperationsService(this),
    DeviceType.Bdc to BdcOperationsService(this),
    DeviceType.Sno110 to Sno110OperationsService(this)
  )

  private var currentConnection: GattConnection? = null

  private var keepAliveJob: Job? = null

  private val _state = MutableLiveData(ConnectionService.State())
  override val state: LiveData<ConnectionService.State> = _state

  private var serviceState = ServiceState()
    set(value) {
      Log.d(TAG, "Service state updated with $value")
      field = value
      _state.postValue(value.toConnectionServiceState())
    }

  private val operationMutex = Mutex()

  private var isPermissionErrorReported = false

  private fun startKeepAlive() {
    if (serviceState.characteristics == null) {
      Log.d(TAG, "Keep-alive start deferred; characteristics not loaded yet")
      return
    }

    if (keepAliveJob?.isActive == true) {
      Log.d(TAG, "Keep-alive already running")
      return
    }

    keepAliveJob?.cancel()
    keepAliveJob = scope.launch(Dispatchers.IO + SupervisorJob()) {
      performKeepAlivePing(isInitial = true)
      while (isActive) {
        delay(keepAliveInterval)
        performKeepAlivePing(isInitial = false)
      }
    }
  }

  private suspend fun performKeepAlivePing(isInitial: Boolean) {
    if (serviceState.characteristics == null) {
      Log.d(TAG, "Skipping keep-alive ${if (isInitial) "initial " else ""}ping; characteristics not loaded")
      return
    }

    val (startMessage, successMessage, failureMessage) = if (isInitial) {
      Triple(
        "Keep-alive initial ping",
        "Keep-alive initial ping successful",
        "Keep-alive initial ping failed"
      )
    } else {
      Triple(
        "Keep-alive ping",
        "Keep-alive ping successful",
        "Keep-alive read failed"
      )
    }

    try {
      Log.d(TAG, startMessage)
      currentOperationsService().readDiagnosticsCharacteristics(null, null)
      Log.d(TAG, successMessage)
    } catch (cancellationException: CancellationException) {
      throw cancellationException
    } catch (e: Exception) {
      Log.w(TAG, failureMessage, e)
      currentConnection?.resetOperation()
    }
  }

  private fun stopKeepAlive() {
    keepAliveJob?.cancel()
    keepAliveJob = null
  }

  private val isConnected
    get() = serviceState.connectionStatus == GattConnectionStatus.Connected

  private fun areBluetoothPermissionsGranted(): Boolean {
    val provider = context as? BluetoothPermissionProvider
    val granted = provider?.areBluetoothPermissionsGranted() ?: true
    if (granted) {
      isPermissionErrorReported = false
    }
    return granted
  }

    private fun requestBluetoothPermissions() {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
      val provider = context as? BluetoothPermissionProvider ?: return
      if (!provider.isBluetoothPermissionRequestInProgress()) {
        provider.requestBluetoothPermissions()
      }
    }

  private fun handleMissingPermission() {
    if (!isPermissionErrorReported) {
      isPermissionErrorReported = true
      requestBluetoothPermissions()
    }
  }

  private data class ServiceState(
    val device: Device? = null,
    val characteristics: DeviceCharacteristics? = null,
    val isConnecting: Boolean = false,
    val connectionStatus: GattConnectionStatus = GattConnectionStatus.Disconnected,
    val lastGattError: GattError<*>? = null
  ) {
    val safeCharacteristics = characteristics ?: DeviceCharacteristics()

    fun toConnectionServiceState() = ConnectionService.State(
      connectionStatus = if (null != device) {
        if (isConnecting) {
          GattConnectionStatus.Connecting
        } else {
          connectionStatus
        }
      } else {
        GattConnectionStatus.Disconnected
      },
      device = device,
      characteristics = characteristics,
      lastGattError = lastGattError
    )
  }

  init {
    scope.launch {
      onScopeCancelled()
    }
  }

  override suspend fun connect(
    device: Device,
    tokenProvider: TokenProvider,
    peripheral: Peripheral?
  ): Boolean {
    if (!areBluetoothPermissionsGranted()) {
      Log.w(TAG, "Bluetooth permissions not granted, cannot connect")
      handleMissingPermission()
      serviceState = serviceState.copy(
        lastGattError = GattError<Unit>(GattErrorCode.MissingPermission)
      )
      return false
    }
    // Automatically close a previous connection
    disconnect()

    deviceManager.bluetoothDeviceFor(device)?.let {

      serviceState = ServiceState(
        isConnecting = true,
        device = device
      )

      // TODO: Refactor MAC address dependency?
      val macAddressServiceMask = when (device.type) {
        DeviceType.Bdc -> BDC_BLUETOOTH_SERVICE_GENERAL_MASK
        DeviceType.Zsc010 -> ZSC010_BLUETOOTH_SERVICE_GENERAL_MASK
        DeviceType.Sno110 -> "" // TODO: Not used
      }

      currentConnection = GattConnection(context, it, callback, macAddressServiceMask)

      internalConnectWithRetry(
        device,
        tokenProvider,
        peripheral
      )

      serviceState = serviceState.copy(isConnecting = false)
    }

    return isConnected
  }

  override suspend fun readCharacteristics(health: Int?, state: Int?): DeviceCharacteristics {
    val operationsService = currentOperationsService()

    val general = operationsService.readGeneralCharacteristics()
    val dim = operationsService.readDimCharacteristics()
    val dali = operationsService.readDaliCharacteristics()
    val time = operationsService. readTimeCharacteristics()
    val diagnostics = operationsService.readDiagnosticsCharacteristics(health, state)

    val characteristics =
      DeviceCharacteristics(
        general,
        dim,
        dali,
        time,
        null,
        diagnostics
      )
    serviceState = serviceState.copy(
      characteristics = characteristics
    )

    startKeepAlive()

    return characteristics
  }

  override suspend fun readDaliBanks() {
    val operationsService = currentOperationsService()

    // TODO: This might be device specific?! Refactor:
    //   doing this here to achieve partial updates of state
    val banks = arrayOf(
      BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_1,
      BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_202,
      BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_203,
      BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_204,
      BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_205,
      BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_206,
      BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_BANK_207,
    )

    banks.forEach { bank ->
      val bankData: Map<Int, Any?> = operationsService.readDaliBank(bank)

      serviceState = serviceState.copy(
        characteristics = serviceState.characteristics?.copy(
          daliBanks = serviceState.characteristics?.daliBanks?.let { addBankData(it, bank, bankData) } ?: mapOf(bank to bankData)
        )
      )
    }
  }

  private fun addBankData(daliBanks: Map<Int, Map<Int, Any?>>, bank: Int, data: Map<Int, Any?>): Map<Int, Map<Int, Any?>> {
    val copy = HashMap(daliBanks)

    copy[bank] = data

    return copy
  }

  override suspend fun writeCharacteristics(characteristics: DeviceCharacteristics): Boolean {
    val operationsService = currentOperationsService()

    val (general, dim, dali, time) = characteristics

    val generalResult = operationsService.writeGeneralCharacteristics(general)
    val dimResult = operationsService.writeDimCharacteristics(dim)
    val daliResult = operationsService.writeDaliCharacteristics(dali)
    val timeResult = operationsService.writeTimeCharacteristics(time)

    serviceState = serviceState.copy(
      characteristics = serviceState.safeCharacteristics.copy(
        general = if (generalResult) general?.copy() else serviceState.safeCharacteristics.general,
        dim = if (dimResult) dim?.copy() else serviceState.safeCharacteristics.dim,
        dali = if (daliResult) dali?.copy() else serviceState.safeCharacteristics.dali,
        time = if (timeResult) time?.copy() else serviceState.safeCharacteristics.time
      )
    )

    return generalResult && dimResult && daliResult && timeResult
  }

  private suspend fun internalConnectWithRetry(
    device: Device,
    tokenProvider: TokenProvider,
    peripheral: Peripheral?
  ) {
    val backoffDelays =
      listOf(0L, 100L, 200L, 400L) // what are the recommendations?
    var attempts = 0
    while (!isConnected && attempts < backoffDelays.size) {
      val backoffDelay = backoffDelays[attempts]

      if (backoffDelay > 0) {
        Log.d(TAG, "Delay next connect attempt for $backoffDelay msec")
        delay(backoffDelay)
      }

      attempts += 1

      Log.d(TAG, "Connect attempt #$attempts")

      serviceState = serviceState.copy(
        connectionStatus = GattConnectionStatus.Connecting
      )

      val operationsService = currentOperationsService()
      operationsService.connect(
        device,
        tokenProvider,
        peripheral
      )
    }

    if (!isConnected) {
      // Setting up the connection failed, close it now to cleanup resources
      // (what if we want to propagate error message?)
      disconnect()
    }
  }

  private fun currentOperationsService(): OperationsService {
    // TODO (REFACTOR): More graceful?

    return serviceState.device?.let {
      operationServices[it.type]
    } ?: throw IllegalStateException("No operations service for this device type")
  }

  override fun isOperationInProgress(): Boolean {
    currentConnection?.let {
      return it.isOperationInProgress()
    }

    return false
  }

  private suspend fun onScopeCancelled() {
    try {
      suspendCancellableCoroutine<Unit> {
        Log.d(TAG, "ConnectionService started")
        // Only a cancel will resume processing
      }
    } finally {
      Log.d(TAG, "ConnectionService stopping")
      disconnect()
      Log.d(TAG, "ConnectionService stopped")
    }
  }

  override suspend fun tearDown() {
    if (!isConnected) return

    serviceState = serviceState.copy(
      connectionStatus = GattConnectionStatus.Disconnecting
    )

    val operationsService = currentOperationsService()

    operationsService.disconnect()
  }

  override suspend fun refresh() {
    currentConnection?.let {
      it.refresh()
    }
  }

  override suspend fun disconnect() = withContext(NonCancellable) {
    stopKeepAlive()
    if (null == currentConnection) return@withContext

    // Will perform DisconnectOperation
    // (and close when STATE_DISCONNECTED)
    tearDown()

    // Should normally do nothing, because on onConnectionStateChange
    // will call close() on connection/BluetoothGatt after disconnect
    // (Avoid leaking bluetooth interfaces, because of 32 limit)
    currentConnection?.close()

    currentConnection = null

    serviceState = ServiceState()
  }

  override suspend fun <T> performOperation(
    operation: GattOperation<T>,
    defaultValue: T
  ): T {
    return performOperation(operation) ?: defaultValue
  }

  override suspend fun <T> performOperation(operation: GattOperation<T>): T? = operationMutex.withLock {
    var value: T? = null

    currentConnection?.perform(operation) {
      when (it) {
        is GattData -> value = !it
        is GattError -> {
          when (!it) {
            GattErrorCode.GattError -> serviceState = serviceState.copy(lastGattError = it)
            GattErrorCode.MissingPermission -> {
              serviceState = serviceState.copy(lastGattError = it)
              handleMissingPermission()
            }
            GattErrorCode.PreconditionFailed,
            GattErrorCode.SerializationFailed,
            GattErrorCode.GattMethodFailed,
            GattErrorCode.WriteCharacteristicValueMismatch -> Log.e(
              TAG, "Error while executing ${it.operationIdentifier}: ${it.code}")
          }
        }
        // TODO (REFACTOR): Fatal error seems not to be used
        is GattFatalError -> {
          Log.e(TAG, "Exception while executing operation: ${it.exception.message}")
        }
      }
    }

    return value
  }

  override suspend fun performWriteTransaction(operations: List<GattOperation<*>>): Boolean = operationMutex.withLock {
    var success = false

    if (currentConnection?.beginReliableWrite() == true) {
      var noWriteFailures = false
      for (operation in operations) {
        noWriteFailures = false
        currentConnection?.perform(operation) { result ->
          when(result) {
            is GattData -> noWriteFailures = true
            is GattError -> {
              serviceState = serviceState.copy(lastGattError = result)
              if (result.code == GattErrorCode.MissingPermission) handleMissingPermission()
            }
            else -> {}
          }
        }
        if (!noWriteFailures) break
      }

      var noEndReliableWriteFailure = false
      currentConnection?.perform(EndReliableWriteOperation(noWriteFailures)) { result ->
        when(result) {
          is GattData -> noEndReliableWriteFailure = !result
          is GattError -> {
            serviceState = serviceState.copy(lastGattError = result)
            if (result.code == GattErrorCode.MissingPermission) handleMissingPermission()
          }
          else -> {}
        }
      }

      success = noWriteFailures && noEndReliableWriteFailure
    }

    success
  }

  override suspend fun performSno110WriteTransaction(operations: List<Sno110WriteCharacteristicGattOperation<*>>): Boolean = operationMutex.withLock {
    var success = false

    if (currentConnection?.beginReliableWrite() == true) {
      var noWriteFailures = false
      for (operation in operations) {
        noWriteFailures = false
        currentConnection?.perform(operation) { result ->
          when(result) {
            is GattData -> noWriteFailures = true
            is GattError -> {
              serviceState = serviceState.copy(lastGattError = result)
              if (result.code == GattErrorCode.MissingPermission) handleMissingPermission()
            }
            else -> {}
          }
        }
        if (!noWriteFailures) break
      }

      var noEndReliableWriteFailure = false
      currentConnection?.perform(EndReliableWriteOperation(noWriteFailures)) { result ->
        when(result) {
          is GattData -> noEndReliableWriteFailure = !result
          is GattError -> {
            serviceState = serviceState.copy(lastGattError = result)
            if (result.code == GattErrorCode.MissingPermission) handleMissingPermission()
          }
          else -> {}
        }
      }

      success = noWriteFailures && noEndReliableWriteFailure
    }

    success
  }

  protected val callback: BluetoothGattCallback = object : BluetoothGattCallback() {
    override fun onConnectionStateChange(
      gatt: BluetoothGatt,
      status: Int,
      newState: Int
    ) {
      super.onConnectionStateChange(gatt, status, newState)
      val stateDescription = when (newState) {
        BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
        BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
        BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
        BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
        else -> "UNKNOWN"
      }

      Log.d(
        TAG,
        "onConnectionStateChange address=${gatt.device.address}, status=${status.toGattStatusDescription()}, newState=$stateDescription ($newState)"
      )

      val connectionState: GattConnectionStatus? = when (newState) {
        BluetoothProfile.STATE_DISCONNECTED -> GattConnectionStatus.Disconnected
        BluetoothProfile.STATE_CONNECTING -> GattConnectionStatus.Connecting
        BluetoothProfile.STATE_CONNECTED -> {
          Log.d(TAG, "Device connected")
          if (currentConnection?.isOperationInProgress() != true) {
            requestHigherMtu(gatt)
          }
          GattConnectionStatus.Connected
        }
        BluetoothProfile.STATE_DISCONNECTING -> GattConnectionStatus.Disconnecting
        else -> null
      }

      val gattError = if (status != BluetoothGatt.GATT_SUCCESS)
        GattError<Unit>(GattErrorCode.GattError, status)
      else null

      Log.d(TAG, "state = $connectionState, error = $gattError")

      val nextServiceState = if (null != gattError)
        serviceState.copy(lastGattError = gattError)
      else
        serviceState

      serviceState = if (null != connectionState) {
        nextServiceState.copy(connectionStatus = connectionState)
      } else {
        nextServiceState
      }

      when (connectionState) {
        GattConnectionStatus.Connected -> startKeepAlive()
        GattConnectionStatus.Disconnected -> {
          currentConnection?.resetOperation()
          stopKeepAlive()
        }
        else -> {}
      }
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
      super.onMtuChanged(gatt, mtu, status)
      if (status == BluetoothGatt.GATT_SUCCESS) {
        Log.d(TAG, "MTU changed to $mtu")
      } else {
        Log.e(TAG, "Failed to change MTU, status: $status")
      }
    }

    private fun requestHigherMtu(gatt: BluetoothGatt) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
          ) != PackageManager.PERMISSION_GRANTED
        ) {
          // Permission is not granted, do not proceed
          Log.e(TAG, "BLUETOOTH_CONNECT permission not granted, cannot request MTU")
          return
        }
      }
      // Permission is granted (or not needed), proceed with requesting MTU
      Log.d(TAG, "Requesting MTU...")
      val success = gatt.requestMtu(517)
      if (success) {
        Log.d(TAG, "MTU request sent successfully")
      } else {
        Log.e(TAG, "MTU request failed")
      }
    }
  }

  companion object {
    private const val TAG = "GattConnectionService"
    private const val DEFAULT_KEEP_ALIVE_INTERVAL = 15_000L

    @Volatile
    private var instance: GattConnectionService? = null

    fun getInstance(
      scope: CoroutineScope,
      context: Context,
      deviceManager: BluetoothDeviceManager,
      keepAliveInterval: Long = DEFAULT_KEEP_ALIVE_INTERVAL
    ) =
      instance
        ?: synchronized(this) {
          instance
            ?: GattConnectionService(
              scope,
              context,
              deviceManager,
              keepAliveInterval
            ).also {
              instance = it
            }
        }

  }
}
