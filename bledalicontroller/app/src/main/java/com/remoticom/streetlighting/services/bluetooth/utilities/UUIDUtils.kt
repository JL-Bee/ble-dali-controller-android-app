package com.remoticom.streetlighting.services.bluetooth.utilities

import java.util.*

infix fun UUID.and(other: UUID?): UUID {
  if (other == null) return UUID(0, 0)

  return UUID(
    this.mostSignificantBits and other.mostSignificantBits,
    this.leastSignificantBits and other.leastSignificantBits
  )
}

infix fun UUID.or(other: UUID?): UUID {
  if (other == null) return UUID(0, 0)

  return UUID(
    this.mostSignificantBits or other.mostSignificantBits,
    this.leastSignificantBits or other.leastSignificantBits
  )
}


