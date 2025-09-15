package com.remoticom.streetlighting.utilities

interface BluetoothPermissionProvider {
  fun areBluetoothPermissionsGranted(): Boolean
  fun requestBluetoothPermissions()
}
