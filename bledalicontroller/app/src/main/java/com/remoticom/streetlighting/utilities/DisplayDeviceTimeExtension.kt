package com.remoticom.streetlighting.utilities

import android.content.Context
import com.remoticom.streetlighting.R
import com.remoticom.streetlighting.services.bluetooth.data.DeviceType
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.TimeCharacteristics
import java.text.SimpleDateFormat
import java.util.*

fun TimeCharacteristics.toDisplayString(deviceType: DeviceType, context: Context?) : String? {
  if (null == context || null == unixTimestamp) return null

  when (deviceType) {
    DeviceType.Zsc010 -> {
      val date = Date(unixTimestamp.toLong() * 1000)
      val df = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US)
      df.timeZone = java.util.TimeZone.getTimeZone("UTC")

      return df.format(date)
    }
    DeviceType.Bdc -> {
      val date = Date(unixTimestamp.toLong() * 1000)
      val df = SimpleDateFormat("HH:mm", Locale.US)
      df.timeZone = java.util.TimeZone.getTimeZone("UTC")

      val result = df.format(date)

      return result
    }
    DeviceType.Sno110 -> {
      if (null == timezone) return null

      val date = Date(unixTimestamp.toLong() * 1000 + timezone.utcOffset * 60 * 60 * 1000)
      val df = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US)
      df.timeZone = java.util.TimeZone.getTimeZone("UTC")

      return df.format(date) + " " + context.getString(R.string.node_info_time_value_sno110_suffix)
    }
  }
}
