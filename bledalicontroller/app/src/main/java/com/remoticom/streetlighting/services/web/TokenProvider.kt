package com.remoticom.streetlighting.services.web

import kotlinx.serialization.Serializable
import java.lang.Exception

@Serializable
data class Challenge(
  val challenge: String,   // Base64(RA|NA)
  val context: String   // Base64(tokenContext(UUID, Timestamp, RA, NA))
)

sealed class InitiateAuthResult {
  data class Success(val challenge: Challenge) : InitiateAuthResult()
  data class Failure(
    val exception: Exception,
    val reason: FailureReason
  ) : InitiateAuthResult()
}

sealed class RequestTokenResult {
  data class Success(val token: Token) : RequestTokenResult()
  data class Failure(
    val exception: Exception,
    val reason: FailureReason
  ) : RequestTokenResult()
}

@Serializable
data class Token(
  val token: String
)

interface TokenProvider {
  suspend fun initiateAuth(uuid: String) : InitiateAuthResult
  suspend fun requestToken(uuid: String, context: String, challengeResponse: String) : RequestTokenResult
}
