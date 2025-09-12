package com.remoticom.streetlighting.services.bluetooth.gatt.bdc

import android.util.Log
import android.util.Range
import com.remoticom.streetlighting.services.bluetooth.gatt.ConnectionProvider
import com.remoticom.streetlighting.services.bluetooth.gatt.bdc.operations.BdcWriteDaliMemoryRequestOperation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class BdcDaliMemoryOperationsService constructor(
  private val connectionProvider: ConnectionProvider
) {

  suspend fun readDaliMemoryString(bank: Byte, address: Byte, length: Int) : String? {
    val data = readDaliMemory(bank, address, length) ?: return null

    val str = data.toString(Charsets.US_ASCII)

    return str
  }

  suspend fun readDaliMemoryBool(bank: Byte, address: Byte) : Boolean? {
    val data = readDaliMemory(bank, address, BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_SIZE_BOOL) ?: return null

    return (data[0] != 0x00.toByte())
  }

  suspend fun readDaliMemoryInt8(bank: Byte, address: Byte) : Long? {
    val data = readDaliMemory(bank, address, BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_SIZE_INT8) ?: return null

    return data[0].toLong()
  }

//  suspend fun readDaliMemoryInt16(bank: Byte, address: Byte) : Long? {
//    val data = readDaliMemory(bank, address, BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_SIZE_INT16) ?: return null
//
//    return ByteBuffer.wrap(data).int.toLong()
//  }

  suspend fun readDaliMemoryUInt8(bank: Byte, address: Byte, rangeTo: UByte?) : Long? {
    val data = readDaliMemory(bank, address, BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_SIZE_UINT8) ?: return null

    val value = data[0].toUByte()

    if (null != rangeTo && value >= rangeTo) return null

    return value.toLong()
  }

  suspend fun readDaliMemoryUInt16(bank: Byte, address: Byte, rangeTo: UInt?) : Long? {
    val data = readDaliMemory(bank, address, BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_SIZE_UINT16) ?: return null

    val value = data.getUInt16At(0)

    if (null != rangeTo && value >= rangeTo) return null

    Log.d("UTILS", "bank: ${bank.toUByte()}, address: ${address.toUByte()}, uint16: ${value} [data=${data.joinToString("") { "%02x".format(it) }}]")

    val long = value.toLong()

    Log.d("UTILS", "bank: ${bank.toUByte()}, address: ${address.toUByte()}, uint16 (long): ${long}")

    return long
  }

  suspend fun readDaliMemoryUInt24(bank: Byte, address: Byte, rangeTo: ULong?) : Long? {
    val data = readDaliMemory(bank, address, BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_SIZE_UINT24) ?: return null

    val value = data.getUInt24At(0)

    if (null != rangeTo && value >= rangeTo) return null

    return value.toLong()
  }


  suspend fun readDaliMemoryUInt32(bank: Byte, address: Byte, rangeTo: ULong?) : Long? {
    val data = readDaliMemory(bank, address, BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_SIZE_UINT32) ?: return null

    val value = data.getUInt32At(0)

    if (null != rangeTo && value >= rangeTo) return null

    return value.toLong()
  }

  suspend fun readDaliMemoryUInt48(bank: Byte, address: Byte, rangeTo: ULong?) : Long? {
    val data = readDaliMemory(bank, address, BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_SIZE_UINT48) ?: return null

    val value = data.getUInt48At(0)

    if (null != rangeTo && value >= rangeTo) return null

    return value.toLong()
  }

  suspend fun readDaliMemoryUInt64(bank: Byte, address: Byte, rangeTo: ULong?) : Long? {
    val data = readDaliMemory(bank, address, BDC_BLUETOOTH_CHARACTERISTIC_DALI_MEMORY_SIZE_UINT64) ?: return null

    val value = data.getUInt64At(0)

    if (null != rangeTo && value >= rangeTo) return null

    return value.toLong()
  }

  suspend fun readDaliMemory(bank: Byte, address: Byte, size: Int) : ByteArray? {
    Log.d(TAG, "READ DALI STARTED")

    val request = byteArrayOf(bank, address, size.toByte())

    val headerSize = 3

    connectionProvider.performOperation(
      BdcWriteDaliMemoryRequestOperation(
        request
      )
    )

    try {
      delay(50)
    } catch (ex: CancellationException) {
      // NOTE: Cancellation only seen in
      Log.w(TAG, "Delay cancelled. Ignoring")
    }

    val rawData =
      connectionProvider.performOperation(BdcReadDaliMemoryResponseOperation())
        ?: return null

    val responseHeader = rawData.copyOfRange(0, headerSize)

    if (!responseHeader.contentEquals(request)) {
      Log.w(
        TAG,
        "Response (${responseHeader}) not matching request: ${request}"
      )

      return null
    }

    Log.d(TAG, "READ DALI FINISHED")

    return rawData.copyOfRange(headerSize, (size + headerSize).coerceAtMost(35))
  }

  companion object {
    private const val TAG = "BdcDaliMemoryOpService"
  }
}
