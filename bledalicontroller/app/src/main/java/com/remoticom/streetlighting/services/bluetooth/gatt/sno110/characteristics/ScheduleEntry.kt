package com.remoticom.streetlighting.services.bluetooth.gatt.sno110.characteristics

import com.remoticom.streetlighting.services.bluetooth.data.characteristics.DimStep

data class ScheduleEntry(
  val version: Int = 0x02,
  val startDateYear: Int,
  val startDateMonth: Int,
  val startDateDay: Int,
  val recurrence: Int,
  val steps: List<DimStep>
)
