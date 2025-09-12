package com.remoticom.streetlighting

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.lifecycle.Observer
import androidx.navigation.ui.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.remoticom.streetlighting.services.authentication.AuthenticationService
import com.remoticom.streetlighting.utilities.checkBluetoothEnabled
import com.remoticom.streetlighting.ui.login.LoginViewModel
import com.remoticom.streetlighting.utilities.InjectorUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

class MainActivity : AppCompatActivity(), CoroutineScopeProvider {

  companion object {
    private const val TAG = "MainActivity"
  }

  private lateinit var mainScope: CoroutineScope

  private lateinit var appBarConfiguration: AppBarConfiguration

  private val loginViewModel: LoginViewModel by viewModels {
    InjectorUtils.provideLoginViewModelFactory()
  }

  private val REQUEST_ENABLE_BT = 4000
  private val REQUEST_PERMISSION_ACCESS_FINE_LOCATION = 5000

  private var authenticationStatus: AuthenticationService.Status? = null

  private var bluetoothPermissionsGranted = false

  private val bluetoothPermissionLauncher: ActivityResultLauncher<Array<String>> =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
      bluetoothPermissionsGranted =
        permissions[Manifest.permission.BLUETOOTH_SCAN] == true &&
        permissions[Manifest.permission.BLUETOOTH_CONNECT] == true

      permissions.entries.forEach { permission ->
        Log.d(TAG, "${permission.key} = ${permission.value}")
      }

      if (!bluetoothPermissionsGranted) {
        showBluetoothPermissionDialog()
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.AppTheme_NoActionBar)

    super.onCreate(savedInstanceState)

    mainScope = MainScope()

    setupDrawerNavigation()

    loginViewModel.state.observe(this, Observer {
      // Only update UI when status really changed
      // (because refresh token generates new account object on state
      //  resulting without this check in pop of settings fragment)
      if (it.status != authenticationStatus) {
        when (it.status) {
          AuthenticationService.Status.AUTHENTICATED -> setupAuthenticatedMode(
            it.account?.claims?.get("name").toString()
          )
          AuthenticationService.Status.AUTHENTICATING,
          AuthenticationService.Status.UNAUTHENTICATED -> setupUnauthenticatedMode()
        }
      }
      authenticationStatus = it.status
    })

    Log.d(TAG, "Request new Bluetooth permissions if needed...")

    checkBluetoothPermissions()

    Log.d(TAG, "Check whether Bluetooth is supported and enabled...")

    checkBluetoothEnabled(REQUEST_ENABLE_BT)

    // "ACCESS_FINE_LOCATION is necessary because, on Android 11 and lower,
    // a Bluetooth scan could potentially be used to gather information
    // about the location of the user."
    // https://developer.android.com/guide/topics/connectivity/bluetooth/permissions
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      Log.d(TAG, "Current Android version (${Build.VERSION.SDK_INT}) lower than ${Build.VERSION_CODES.S}: Checking location permissions...")

      checkLocationPermission(REQUEST_PERMISSION_ACCESS_FINE_LOCATION)
    } else {
      Log.d(TAG, "Current Android version (${Build.VERSION.SDK_INT}) higher than or equal to ${Build.VERSION_CODES.S}: Bluetooth permissions are sufficient")
    }
  }

  override fun onResume() {
    super.onResume()

    loginViewModel.resume()
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {

    when (requestCode) {
      REQUEST_PERMISSION_ACCESS_FINE_LOCATION -> {
        // If request is cancelled, the result arrays are empty.
        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
          Log.d(TAG, "Location permission granted.")
        } else {
          Log.w(TAG, "Location permission denied.")
        }
      }
      else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == REQUEST_ENABLE_BT) {
      if (resultCode != 0) {
        Log.i(TAG, "Bluetooth access allowed")
      } else {
        Log.w(TAG, "Bluetooth access denied.")
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()

    mainScope.cancel()
  }

  fun setupDrawerNavigation() {
    setContentView(R.layout.activity_main)
    val toolbar: Toolbar = findViewById(R.id.toolbar)
    setSupportActionBar(toolbar)

    val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
    val navView: NavigationView = findViewById(R.id.nav_view)
    val navController = findNavController(R.id.nav_host_fragment)
    // Passing each menu ID as a set of Ids because each
    // menu should be considered as top level destinations.
    appBarConfiguration = AppBarConfiguration(
      setOf(
        R.id.nav_signin,
        R.id.nav_home,
        R.id.nav_about,
        R.id.nav_help,
        R.id.nav_signout
      ), drawerLayout
    )
    setupActionBarWithNavController(navController, appBarConfiguration)
    navView.setupWithNavController(navController)
  }

  fun setupAuthenticatedMode(name: String?) {
    updateMenuItemVisibility(true, name)
    val navController = findNavController(R.id.nav_host_fragment)
    if (navController.currentDestination!!.id != R.id.nav_home) {
      navController.navigate(R.id.nav_global_home)
    }
  }

  fun setupUnauthenticatedMode() {
    updateMenuItemVisibility(false)
    val navController = findNavController(R.id.nav_host_fragment)
    if (navController.currentDestination!!.id != R.id.nav_signin) {
      navController.navigate(R.id.nav_global_signin)
    }
  }

  fun updateMenuItemVisibility(isAuthenticated: Boolean, name: String? = null) {
    val navView: NavigationView = findViewById(R.id.nav_view)

    val textView = navView.getHeaderView(0).findViewById<TextView>(R.id.nav_header_subtitle)
    textView.text = name ?: getString(R.string.menu_signout)

    val menu = navView.menu

    menu.forEach { menuItem ->
      menuItem.isVisible = when (menuItem.itemId) {
        R.id.nav_global_signin -> !isAuthenticated
        R.id.nav_global_about -> true
        R.id.nav_global_help -> true
        else -> isAuthenticated
      }
    }
  }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.main, menu)
//        return true
//    }

  override fun onSupportNavigateUp(): Boolean {
    val navController = findNavController(R.id.nav_host_fragment)
    return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
  }

  private fun checkBluetoothPermissions() {
    // https://developer.android.com/guide/topics/connectivity/bluetooth/permissions
    // https://stackoverflow.com/questions/67722950/android-12-new-bluetooth-permissions
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val scanGranted =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) ==
          PackageManager.PERMISSION_GRANTED
      val connectGranted =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
          PackageManager.PERMISSION_GRANTED

      bluetoothPermissionsGranted = scanGranted && connectGranted

      if (!bluetoothPermissionsGranted) {
        Log.d(TAG, "Requesting BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions...")
        bluetoothPermissionLauncher.launch(
          arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
          )
        )
      }
    } else {
      Log.d(TAG, "No need to request BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions on Android version ${Build.VERSION.SDK_INT}")
    }
  }

  private fun showBluetoothPermissionDialog() {
    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.main_dialog_permission_bluetooth_title)
      .setMessage(R.string.main_dialog_permission_bluetooth_message)
      .setNegativeButton(R.string.main_dialog_permission_bluetooth_negative_button) { _, _ ->
        Log.w(TAG, "User skipped bluetooth permission")
      }
      .setPositiveButton(R.string.main_dialog_permission_bluetooth_positive_button) { _, _ ->
        Log.d(TAG, "User wants bluetooth permission")
        bluetoothPermissionLauncher.launch(
          arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
          )
        )
      }
      .show()
  }

  private fun checkLocationPermission(requestCode: Int) {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
      != PackageManager.PERMISSION_GRANTED) {
      Log.w(TAG, "Location permission not granted. Presenting dialog...")

      MaterialAlertDialogBuilder(this)
        .setTitle(R.string.main_dialog_permission_location_title)
        .setMessage(R.string.main_dialog_permission_location_message)
        .setNegativeButton(R.string.main_dialog_permission_location_negative_button) { _, _ ->
          Log.w(TAG, "User skipped location permission")
        }
        .setPositiveButton(R.string.main_dialog_permission_location_positive_button) { _, _ ->
          Log.d(TAG, "User wants location permission")

          ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            requestCode)
        }
        .show()
    } else {
      Log.d(TAG, "Location permission is/was granted. Not requesting permission.")
    }
  }

  override fun provideScope(): CoroutineScope {
    return mainScope
  }
}
