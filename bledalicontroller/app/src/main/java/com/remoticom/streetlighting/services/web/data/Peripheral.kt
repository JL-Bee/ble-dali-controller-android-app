package com.remoticom.streetlighting.services.web.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OwnershipStatus {
  @SerialName("unclaimed")
  Unclaimed,
  @SerialName("claimed")
  Claimed
}

@Serializable
data class Peripheral(
  val uuid : String? = null,
  val ownershipStatus: OwnershipStatus? = null,
  val owner: String? = null,
  val assetName: String? = null,
  val password: String? = null,
  val pin: String? = null,
  val support: PeripheralSupport? = null
//  val product: PeripheralProduct? = null
)
