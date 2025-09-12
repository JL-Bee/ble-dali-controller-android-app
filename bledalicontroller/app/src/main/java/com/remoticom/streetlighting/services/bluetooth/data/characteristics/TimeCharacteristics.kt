package com.remoticom.streetlighting.services.bluetooth.data.characteristics

typealias UnixTimestamp = Int
typealias TimeOffset = Int
typealias MidnightOffset = Int

data class TimeCharacteristics(val timezone: TimeZone?, val unixTimestamp: UnixTimestamp?, val midnightOffset: MidnightOffset? = null) {
}

data class TimeZone(val utcOffset: Int = 0, val daylightSavingTimeEnabled : Boolean = false) {
}
