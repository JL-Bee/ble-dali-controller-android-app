package com.remoticom.streetlighting.utilities

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

fun AppCompatActivity.checkBluetoothEnabled(requestCode: Int) {
  val adapter = BluetoothAdapter.getDefaultAdapter()

  if (null == adapter) Log.e(this.javaClass.simpleName, "Current device does not support Bluetooth")

  // NOTE: Would assume that this should not be enclosed in version check
  // it's about bluetooth being on or off
  // TODO: refactor to registerForActivityResult
  adapter?.let {
    if (!it.isEnabled) {
      val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
      if (ActivityCompat.checkSelfPermission(
          this,
          Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        return
      }
      startActivityForResult(enableBtIntent, requestCode)
    }
  }
}
