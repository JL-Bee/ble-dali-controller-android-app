package com.remoticom.streetlighting.utilities

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.remoticom.streetlighting.BuildConfig
import com.remoticom.streetlighting.R
import com.remoticom.streetlighting.data.Node
import com.remoticom.streetlighting.services.bluetooth.data.DeviceType
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DiagnosticsStatus
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.*
import com.remoticom.streetlighting.ui.nodes.utilities.toText

fun Context.sendMail(emailAddress: String, subject: String?, body: String?) {
  val intent = Intent(Intent.ACTION_SENDTO)
  intent.data = Uri.parse("mailto:") // only email apps should handle this

  intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
  intent.putExtra(Intent.EXTRA_SUBJECT, subject)
  intent.putExtra(Intent.EXTRA_TEXT, body)

  try {
    this.startActivity(intent)
  } catch (ex: Exception) {
  }
}

fun Context.errorDescriptionForDiagnosticsStatus(deviceType: DeviceType, status: DiagnosticsStatus?) : String? {
  if (null == status) return null

  when (deviceType) {
    DeviceType.Zsc010 -> {
      if (status == 0) return this.getString(R.string.node_error_description_none)

      val builder = StringBuilder()

      addTextToBuilderIfStatusMatchesFlag(R.string.node_error_description_gps, builder, status, 0x01)
      addTextToBuilderIfStatusMatchesFlag(R.string.node_error_description_dali, builder, status, 0x02)
      addTextToBuilderIfStatusMatchesFlag(R.string.node_error_description_eeprom, builder, status, 0x04)
      addTextToBuilderIfStatusMatchesFlag(R.string.node_error_description_dim, builder, status, 0x08)
      addTextToBuilderIfStatusMatchesFlag(R.string.node_error_description_lux, builder, status, 0x10)
      addTextToBuilderIfStatusMatchesFlag(R.string.node_error_description_ble, builder, status, 0x20)
      addTextToBuilderIfStatusMatchesFlag(R.string.node_error_description_gps_no_fix, builder, status, 0x40)

      return builder.toString()
    }
    DeviceType.Bdc -> {
      if (status == 0) return this.getString(R.string.node_error_description_none)

      val builder = StringBuilder()

      addTextToBuilderIfStatusMatchesFlag(R.string.node_error_description_d4i, builder, status, 0x01) // 1
      addTextToBuilderIfStatusMatchesFlag(R.string.node_error_description_dali, builder, status, 0x02) // 2
      addTextToBuilderIfStatusMatchesFlag(R.string.node_error_description_eeprom, builder, status, 0x04) // 4
      addTextToBuilderIfStatusMatchesFlag(R.string.node_error_description_dim, builder, status, 0x08) // 8
      // addTextToBuilderIfStatusMatchesFlag(R.string.node_error_description_lux, builder, status, 0x10) // 16
      addTextToBuilderIfStatusMatchesFlag(R.string.node_error_description_ble, builder, status, 0x20) // 32
      addTextToBuilderIfStatusMatchesFlag(R.string.node_error_description_misc, builder, status, 0x80) // 128
      // addTextToBuilderIfStatusMatchesFlag(R.string.node_error_description_gps_no_fix, builder, status, 0x40)

      return builder.toString()
    }
    DeviceType.Sno110 -> {
      if (status == -1) return this.getString(R.string.node_error_description_none)

      val builder = StringBuilder()

      addTextToBuilderIfStatusMissingFlag(R.string.node_error_sno110_description_scheduler, builder, status, SNO110_HEALTH_FLAG_SETTINGS_SCHEDULER shl 16)
      addTextToBuilderIfStatusMissingFlag(R.string.node_error_sno110_description_alo, builder, status, SNO110_HEALTH_FLAG_SETTINGS_ALO shl 16)
      addTextToBuilderIfStatusMissingFlag(R.string.node_error_sno110_description_clock, builder, status, SNO110_HEALTH_FLAG_SETTINGS_CLOCK shl 16)
      addTextToBuilderIfStatusMissingFlag(R.string.node_error_sno110_description_gps, builder, status, SNO110_HEALTH_FLAG_STATUS_GPS shl 16)
      addTextToBuilderIfStatusMissingFlag(R.string.node_error_sno110_description_dali_bus, builder, status, SNO110_HEALTH_FLAG_STATUS_DALI_BUS shl 16)
      addTextToBuilderIfStatusMissingFlag(R.string.node_error_sno110_description_dali_driver, builder, status, SNO110_HEALTH_FLAG_STATUS_DALI_BUS shl 16)
      addTextToBuilderIfStatusMissingFlag(R.string.node_error_sno110_description_temp_mcu_1, builder, status, SNO110_HEALTH_FLAG_ALARM_TEMP_MCU_PRIMARY shl 16)
      addTextToBuilderIfStatusMissingFlag(R.string.node_error_sno110_description_temp_mcu_2, builder, status, SNO110_HEALTH_FLAG_ALARM_TEMP_MCU_SECONDARY shl 16)

      addTextToBuilderIfStatusMissingFlag(R.string.node_error_sno110_description_time_no_fix, builder, status, SNO110_STATE_FLAG_FIX_TIME)
      addTextToBuilderIfStatusMissingFlag(R.string.node_error_sno110_description_position_no_fix, builder, status, SNO110_STATE_FLAG_FIX_POSITION)

      return builder.toString()
    }
  }
}

fun Context.addTextToBuilderIfStatusMatchesFlag(resId: Int, builder: StringBuilder, status: DiagnosticsStatus, flag: Int) : StringBuilder {
  if (status and flag == flag) {
    if (builder.isNotEmpty()) {
      builder.append(", ")
    }

    builder.append(getString(resId))
  }

  return builder
}

fun Context.addTextToBuilderIfStatusMissingFlag(resId: Int, builder: StringBuilder, status: DiagnosticsStatus, flag: Int) : StringBuilder {
  if (status and flag == 0x00) {
    if (builder.isNotEmpty()) {
      builder.append(", ")
    }

    builder.append(getString(resId))
  }

  return builder
}

fun Context.sendReportProblemWithNodeMail(node: Node) {

  // Parameters below based on this template
  // \n\n\nNode address: %1$s\nNode rssi: %2$s\nNode firmware version: %3$s\nNode diagnostics code: %4$s\nApp version: \n%5$s

  // 1: name
  val name = node.device.name

  // 2: address
  val address = node.device.address

  // 3: UUID
  val uuid = node.id

  // 4: RSSI
  val rssi = "${node.rssi}dB" ?: getString(R.string.node_report_problem_value_null)

  // 5: firmware version
  val firmwareVersion = node.characteristics?.diagnostics?.version?.firmwareVersion?.toText(this) ?: getString(R.string.node_report_problem_value_null)

  // 6: diagnostics
  val errorCode = node.characteristics?.diagnostics?.status ?: getString(R.string.node_report_problem_value_null)

  // 7: diangostics text
  val errorDescription = errorDescriptionForDiagnosticsStatus(node.deviceType, node.characteristics?.diagnostics?.status) ?: getString(R.string.node_report_problem_value_null)

  // 8: owner
  val owner = node.info?.owner ?: getString(R.string.node_report_problem_value_null)

  // 9: asset name
  val assetName = node.info?.assetName ?: getString(R.string.node_report_problem_value_null)

  // 10: gps location
  val gpsLocation = if (node.gpsStatus == Node.GpsStatus.Fixed) (node.characteristics?.gps?.position?.toDisplayString() ?: getString(R.string.node_report_problem_value_null)) else getString(R.string.node_report_problem_gps_not_fixed);

  // 11: time
  val time = if (node.timeStatus == Node.TimeStatus.Fixed) (node.characteristics?.time?.toDisplayString(deviceType = node.deviceType, context = this) ?: getString(R.string.node_report_problem_value_null)) else getString(R.string.node_report_problem_time_not_fixed)

  // 12: app version
  val appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

  val emailAddress = node.info?.support?.email ?: getString(R.string.node_report_problem_email_address)
  val subject = getString(R.string.node_report_problem_subject_format, address)
  val body = getString(
    R.string.node_report_problem_body_format,
    name,     // 1
    address,  // 2
    uuid,     // 3
    rssi,     // 4
    firmwareVersion,    // 5
    errorCode,          // 6
    errorDescription,   // 7
    owner,              // 8
    assetName,          // 9
    gpsLocation,        // 10
    time,               // 11
    appVersion          // 12
  )

  sendMail(
    emailAddress,
    subject,
    body
  )
}
