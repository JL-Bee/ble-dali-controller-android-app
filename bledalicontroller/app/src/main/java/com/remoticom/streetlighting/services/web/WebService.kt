package com.remoticom.streetlighting.services.web

import android.util.Base64
import android.util.Log
import com.remoticom.streetlighting.services.authentication.AuthenticationService
import com.remoticom.streetlighting.services.bluetooth.gatt.*
import com.remoticom.streetlighting.services.web.data.OwnershipStatus
import com.remoticom.streetlighting.services.web.data.Peripheral
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.takeFrom
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.InternalAPI
import io.ktor.utils.io.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// Prod:
private const val PERIPHERALS_BASE_URI = "https://user-management-functions.azurewebsites.net/api/peripherals"
private const val PERIPHERALS_AUTH_BASE_URI = "https://user-management-functions.azurewebsites.net/api/peripherals-auth"
// Acc:
// private const val PERIPHERALS_BASE_URI = "https://zsc-staging.azurewebsites.net/api/peripherals"

// SNO110:
// private const val PERIPHERALS_BASE_URI = "https://sven-zsc-backend.azurewebsites.net/api/peripherals"

// private const val PERIPHERALS_AUTH_BASE_URI = "https://sven-zsc-backend.azurewebsites.net/api/peripherals-auth"

class WebService(private val clientFactory: HttpClientFactory) : TokenProvider {

  @Serializable
  private data class PatchPeripheral(
    private val assetName: String?,
    private val ownershipStatus: OwnershipStatus?
  )

  @Serializable
  private data class InitiateAuthRequest(
    private val uuid: String
  )

  @Serializable
  private data class TokenRequest(
    private val uuid: String,
    private val challengeResponse: String,
    private val context: String
  )

  suspend fun requestPeripheral(uuid: String) : PeripheralResult {
    // TODO: Refactor refresh and retrieve to single function?
    AuthenticationService.getInstance().refreshToken()

    val accessToken = retrieveAccessToken()
    Log.w(
      TAG,
      "ACCESS TOKEN: $accessToken"
    )

    clientFactory.create().use {
      // Closing directly after use

      return try {
        val peripheral : Peripheral = it.get {
          url.takeFrom("${PERIPHERALS_BASE_URI}/${uuid}")
          header("Authorization", "Bearer: $accessToken")
        }.body()

        PeripheralResult.Success(peripheral)
      } catch (ex : Exception) {
        if (ex is ClientRequestException) {
          val errorMessage = ex.response.bodyAsText()
          Log.w(
            TAG,
            "Error requesting peripheral ($uuid): status=${ex.response.status.value}, message=$errorMessage"
          )
          when (ex.response.status.value) {
            400 -> PeripheralResult.Failure(ex, FailureReason.BadRequest)
            401 -> PeripheralResult.Failure(ex, FailureReason.Unauthorized)
            403 -> PeripheralResult.Failure(ex, FailureReason.Forbidden)
            404 -> PeripheralResult.Failure(ex, FailureReason.PeripheralNotFound)
            500 -> PeripheralResult.Failure(ex, FailureReason.ServerError)
            else -> PeripheralResult.Failure(ex, FailureReason.Other)
          }
        } else {
          PeripheralResult.Failure(ex, FailureReason.Other)
        }
      }
    }
  }

  @OptIn(InternalAPI::class)
  suspend fun updatePeripheral(uuid: String, peripheral: Peripheral) : PeripheralResult {
    // TODO: Refactor refresh and retrieve to single function?
    AuthenticationService.getInstance().refreshToken()

    val accessToken = retrieveAccessToken()

    clientFactory.create().use {
      return try {
        val jsonBody = Json.encodeToString(PatchPeripheral.serializer(), PatchPeripheral(peripheral.assetName, peripheral.ownershipStatus))
        Log.d(TAG,"Serialized body for the request: $jsonBody")
        val updatedPeripheral: Peripheral = it.patch {
          contentType(ContentType.Application.Json)
          url.takeFrom("${PERIPHERALS_BASE_URI}/${uuid}")
          header("Authorization", "Bearer: $accessToken")
          body = jsonBody
        }.body()

        PeripheralResult.Success(updatedPeripheral)
      } catch (ex: Exception) {
        if (ex is ClientRequestException) {
          val message = StringBuilder()
          do {
            val line = ex.response.content.readUTF8Line()
            message.append(line)
          } while (line !== null)

          Log.w(TAG, "Error updating peripheral (${uuid}): status=${ex.response.status.value} message=${message}")

          when (ex.response.status.value) {
            400 -> PeripheralResult.Failure(ex, FailureReason.BadRequest)
            401 -> PeripheralResult.Failure(ex, FailureReason.Unauthorized)
            403 -> PeripheralResult.Failure(ex, FailureReason.Forbidden)
            404 -> PeripheralResult.Failure(ex, FailureReason.PeripheralNotFound)
            500 -> PeripheralResult.Failure(ex, FailureReason.ServerError)
            else -> PeripheralResult.Failure(ex, FailureReason.Other)
          }
        } else {
          PeripheralResult.Failure(ex, FailureReason.Other)
        }
      }
    }
  }

  // TODO (REQUIREMENTS): async retrieve fresh access token
  private fun retrieveAccessToken() : String? {
    return AuthenticationService.getInstance().accessToken
  }

  companion object {
    const val TAG = "WebService"
  }

  @OptIn(InternalAPI::class)
  override suspend fun initiateAuth(uuid: String) : InitiateAuthResult {
    // TODO: Refactor refresh and retrieve to single function?
    AuthenticationService.getInstance().refreshToken()

    val accessToken = retrieveAccessToken()

    clientFactory.create().use {
      return try {
        val challenge: Challenge = it.post {
          contentType(ContentType.Application.Json)
          url.takeFrom("${PERIPHERALS_AUTH_BASE_URI}/initiate")
          header("Authorization", "Bearer: $accessToken")
          body = InitiateAuthRequest(
            uuid = uuid
          )
        }.body()

        InitiateAuthResult.Success(challenge)
      } catch (ex: Exception) {
        if (ex is ClientRequestException) {
          val message = StringBuilder()
          do {
            val line = ex.response.content.readUTF8Line()
            message.append(line)
          } while (line !== null)

          Log.w(TAG, "Error initiating auth (${uuid}): status=${ex.response.status.value} message=${message}")

          when (ex.response.status.value) {
            400 -> InitiateAuthResult.Failure(ex, FailureReason.BadRequest)    // (Bad request?)
            401 -> InitiateAuthResult.Failure(ex, FailureReason.Unauthorized)  // Invalid token
            403 -> InitiateAuthResult.Failure(ex, FailureReason.Forbidden)     // Insufficient permissions (token expired?)
            404 -> InitiateAuthResult.Failure(ex, FailureReason.PeripheralNotFound)  // Peripheral not found
            500 -> InitiateAuthResult.Failure(ex, FailureReason.ServerError)   //
            else -> InitiateAuthResult.Failure(ex, FailureReason.Other)        //
          }
        } else {
          InitiateAuthResult.Failure(ex, FailureReason.Other)
        }
      }
    }
  }

  @OptIn(InternalAPI::class)
  override suspend fun requestToken(uuid: String, context: String, challengeResponse: String) : RequestTokenResult {
    // TODO: Refactor refresh and retrieve to single function?
    AuthenticationService.getInstance().refreshToken()

    val accessToken = retrieveAccessToken()

    clientFactory.create().use {
      return try {
        val token: Token = it.post {
          contentType(ContentType.Application.Json)
          url.takeFrom("${PERIPHERALS_AUTH_BASE_URI}/token")
          header("Authorization", "Bearer: $accessToken")
          body = TokenRequest(
            uuid = uuid,
            challengeResponse = challengeResponse,
            context = context
          )
        }.body()

        RequestTokenResult.Success(token)
      } catch (ex: Exception) {
        if (ex is ClientRequestException) {
          val message = StringBuilder()
          do {
            val line = ex.response.content.readUTF8Line()
            message.append(line)
          } while (line !== null)

          Log.w(TAG, "Error initiating auth (${uuid}): status=${ex.response.status.value} message=${message}")

          when (ex.response.status.value) {
            400 -> RequestTokenResult.Failure(ex, FailureReason.BadRequest)    // (Bad request?)
            401 -> RequestTokenResult.Failure(ex, FailureReason.Unauthorized)  // Invalid token
            403 -> RequestTokenResult.Failure(ex, FailureReason.Forbidden)     // Insufficient permissions
            404 -> RequestTokenResult.Failure(ex, FailureReason.PeripheralNotFound)  // Peripheral not found
            500 -> RequestTokenResult.Failure(ex, FailureReason.ServerError)   //
            else -> RequestTokenResult.Failure(ex, FailureReason.Other)        //
          }
        } else {
          RequestTokenResult.Failure(ex, FailureReason.Other)
        }
      }
    }
  }
}
