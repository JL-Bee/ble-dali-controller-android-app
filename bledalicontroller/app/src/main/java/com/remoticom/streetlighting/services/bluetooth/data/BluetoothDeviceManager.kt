package com.remoticom.streetlighting.services.bluetooth.data

import android.bluetooth.BluetoothDevice

interface BluetoothDeviceManager {
  fun bluetoothDeviceFor(device: Device): BluetoothDevice?
}
