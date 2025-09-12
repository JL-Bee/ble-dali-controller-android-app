package com.remoticom.streetlighting.services.bluetooth.gatt

import com.remoticom.streetlighting.services.bluetooth.gatt.connection.*

abstract class GattOperationsService constructor(
  protected val connectionProvider: ConnectionProvider
) : OperationsService {
  override suspend fun disconnect() {
    connectionProvider.performOperation(DisconnectGattOperation())
  }
}
