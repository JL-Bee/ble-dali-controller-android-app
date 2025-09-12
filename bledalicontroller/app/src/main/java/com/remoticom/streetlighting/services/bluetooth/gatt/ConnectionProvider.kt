package com.remoticom.streetlighting.services.bluetooth.gatt

import com.remoticom.streetlighting.services.bluetooth.gatt.connection.GattOperation
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.operations.Sno110WriteCharacteristicGattOperation
import com.remoticom.streetlighting.services.bluetooth.gatt.zsc010.operations.Zsc010WriteCharacteristicGattOperation

interface ConnectionProvider {
  suspend fun <T> performOperation(operation: GattOperation<T>): T?

  suspend fun <T> performOperation(operation: GattOperation<T>, defaultValue: T): T

  suspend fun performWriteTransaction(operations: List<GattOperation<*>>): Boolean

  suspend fun performSno110WriteTransaction(operations: List<Sno110WriteCharacteristicGattOperation<*>>): Boolean

  suspend fun tearDown()

  suspend fun refresh()
}
