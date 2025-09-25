package com.remoticom.streetlighting.data

import com.remoticom.streetlighting.services.bluetooth.gatt.connection.toGattStatusDescription

enum class NodeConnectionErrorCode {
  GattError,
  GattMethodFailed,
  PreconditionFailed,
  SerializationFailed,
  WriteCharacteristicValueMismatch,
  MissingPermission
}

data class NodeConnectionError(
  val code: NodeConnectionErrorCode,
  val reason: Int? = null,
  val operationIdentifier: String? = null
) {
  override fun toString() : String {
    val error = StringBuilder("$code")

    if (code == NodeConnectionErrorCode.GattError && null != reason) {
      error.append(": ${reason.toGattStatusDescription()}")
    }

    if (null != operationIdentifier) {
      error.append(" (${operationIdentifier})")
    }

    return error.toString()
  }
}
