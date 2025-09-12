package com.remoticom.streetlighting.utilities

import kotlin.experimental.and

fun ByteArray.bcdDecodedString() : String {
  val sb = StringBuffer()

  for (b in this) {
    sb.append(bcdByteToString(b));
  }

  return sb.toString()
}

private fun bcdByteToString(bcd: Byte) : String {
  val sb = StringBuffer()

  var high = bcd and 0xf0.toByte()
  high = (high.toInt() shr 4).toByte()
  high = high and 0x0f
  val low = bcd and 0x0f

  sb.append(high);
  sb.append(low);

  return sb.toString();
}
