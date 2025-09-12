package com.remoticom.streetlighting.services.bluetooth.gatt.connection

import android.bluetooth.BluetoothGatt

object GattStatus {
  /** A GATT operation completed successfully  */
  const val GATT_SUCCESS = BluetoothGatt.GATT_SUCCESS // 0x00

  /** GATT read operation is not permitted  */
  const val GATT_READ_NOT_PERMITTED = BluetoothGatt.GATT_READ_NOT_PERMITTED // 0x2

  /** GATT write operation is not permitted  */
  const val GATT_WRITE_NOT_PERMITTED = BluetoothGatt.GATT_WRITE_NOT_PERMITTED // 0x3

  /** Insufficient authentication for a given operation  */
  const val GATT_INSUFFICIENT_AUTHENTICATION = BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION // 0x5

  /** The given request is not supported  */
  const val GATT_REQUEST_NOT_SUPPORTED = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED // 0x6

  /** Insufficient encryption for a given operation  */
  const val GATT_INSUFFICIENT_ENCRYPTION = BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION // 0xf

  /** A read or write operation was requested with an invalid offset  */
  const val GATT_INVALID_OFFSET = BluetoothGatt.GATT_INVALID_OFFSET // 0x7

  /** A write operation exceeds the maximum length of the attribute  */
  const val GATT_INVALID_ATTRIBUTE_LENGTH = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH // 0xd

  /** A remote device connection is congested.  */
  const val GATT_CONNECTION_CONGESTED = BluetoothGatt.GATT_CONNECTION_CONGESTED // 0x8f

  /** A GATT operation failed, errors other than the above  */
  const val GATT_FAILURE = BluetoothGatt.GATT_FAILURE // 0x101

  const val GATT_CONNECTION_TIMEOUT = 0x08 // (8)

  const val GATT_CONNECTION_TERMINATE_PEER_USER = 0x13 // (19)
  const val GATT_CONNECTION_TERMINATE_LOCAL_HOST = 0x16 // (22)

  const val GATT_ERROR = 0x85 // (133)
}

fun Int.toGattStatusDescription() : String {
  return when (this) {
    GattStatus.GATT_SUCCESS -> "($this) A GATT operation completed successfully"
    GattStatus.GATT_READ_NOT_PERMITTED -> "($this) GATT read operation is not permitted"
    GattStatus.GATT_WRITE_NOT_PERMITTED -> "($this) GATT write operation is not permitted"
    GattStatus.GATT_INSUFFICIENT_AUTHENTICATION -> "($this) Insufficient authentication for a given operation"
    GattStatus.GATT_REQUEST_NOT_SUPPORTED -> "($this) The given request is not supported"
    GattStatus.GATT_CONNECTION_TIMEOUT -> "($this) GATT connection timeout"
    GattStatus.GATT_INSUFFICIENT_ENCRYPTION -> "($this) Insufficient encryption for a given operation"
    GattStatus.GATT_INVALID_OFFSET -> "($this) A read or write operation was requested with an invalid offset"
    GattStatus.GATT_INVALID_ATTRIBUTE_LENGTH -> "($this) A write operation exceeds the maximum length of the attribute"
    GattStatus.GATT_ERROR -> "($this) GATT_ERROR"
    GattStatus.GATT_CONNECTION_CONGESTED -> "($this) A remote device connection is congested"
    GattStatus.GATT_CONNECTION_TERMINATE_PEER_USER -> "($this) Connection terminated by peer user"
    GattStatus.GATT_CONNECTION_TERMINATE_LOCAL_HOST -> "($this) Connection terminated by local host"
    GattStatus.GATT_FAILURE -> "($this) A GATT operation failed, errors other than the above"

    else -> "($this) Unknown"
  }
}
