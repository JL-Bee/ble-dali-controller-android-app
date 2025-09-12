package com.remoticom.streetlighting.ui.nodes.settings.data

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.sql.Time
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*

@Serializable
data class DimmingScheme(val name : String? = null, val preset: Int? = null, val steps: List<DimmingSchemeStep>? = null) {

  override fun toString(): String {
    return name ?: ""
  }
}

@Serializable
data class DimmingSchemeStep(@Serializable(with = TimeSerializer::class) val time: Date? = null, val level: Int? = null) {
}

@Serializer(forClass = Date::class)
object TimeSerializer : KSerializer<Date?> {
  private val df: DateFormat = SimpleDateFormat("HH:mm", Locale.US)

  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("Time", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Date?) {
    if (null != value)  encoder.encodeString(df.format(value))
  }

  override fun deserialize(decoder: Decoder): Date? {
    return df.parse(decoder.decodeString())
  }
}

