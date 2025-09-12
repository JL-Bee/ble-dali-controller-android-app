package com.remoticom.streetlighting.services.bluetooth.gatt.bdc

fun ByteArray.getUInt16At(idx: Int) =
  ((this[idx].toUInt() and 0xFFu) shl 8) or
    (this[idx + 1].toUInt() and 0xFFu)

fun ByteArray.getUInt24At(idx: Int) =
  ((this[idx].toUInt() and 0xFFu) shl 16) or
    ((this[idx + 1].toUInt() and 0xFFu) shl 8) or
    (this[idx + 2].toUInt() and 0xFFu)

fun ByteArray.getUInt32At(idx: Int) =
  ((this[idx].toUInt() and 0xFFu) shl 24) or
    ((this[idx + 1].toUInt() and 0xFFu) shl 16) or
    ((this[idx + 2].toUInt() and 0xFFu) shl 8) or
    (this[idx + 3].toUInt() and 0xFFu)

fun ByteArray.getUInt48At(idx: Int) =
  ((this[idx].toULong() and 0xFFu) shl 40) or
    ((this[idx + 1].toULong() and 0xFFu) shl 32) or
    ((this[idx + 2].toULong() and 0xFFu) shl 24) or
    ((this[idx + 3].toULong() and 0xFFu) shl 16) or
    ((this[idx + 4].toULong() and 0xFFu) shl 8) or
    (this[idx + 5].toULong() and 0xFFu)

fun ByteArray.getUInt64At(idx: Int) =
  ((this[idx].toULong() and 0xFFu) shl 56) or
    ((this[idx + 1].toULong() and 0xFFu) shl 48) or
    ((this[idx + 2].toULong() and 0xFFu) shl 40) or
    ((this[idx + 3].toULong() and 0xFFu) shl 32) or
    ((this[idx + 4].toULong() and 0xFFu) shl 24) or
    ((this[idx + 5].toULong() and 0xFFu) shl 16) or
    ((this[idx + 6].toULong() and 0xFFu) shl 8) or
    (this[idx + 7].toULong() and 0xFFu)
