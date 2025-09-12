package com.remoticom.streetlighting.services.web.data

import kotlinx.serialization.Serializable

@Serializable
data class PeripheralProduct(
  val id: String? = null,
  val code: String? = null
)
