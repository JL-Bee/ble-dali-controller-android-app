package com.remoticom.streetlighting.services.web

import android.util.Base64
import android.util.Log
import com.remoticom.streetlighting.services.bluetooth.gatt.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class MockWebService : TokenProvider {
  override suspend fun initiateAuth(uuid: String) : InitiateAuthResult {
    // Use secure random in production
    val randomChallengeA: ByteArray = Random.nextBytes(16)
    Log.d(WebService.TAG, "RA (initiate): ${randomChallengeA.joinToString("") { "%02x".format(it) }}")

    val nonceContributionA: ByteArray = Random.nextBytes(4)
    Log.d(WebService.TAG, "NA (initiate): ${nonceContributionA.joinToString("") { "%02x".format(it) }}")

    val message1ByteArray: ByteArray = randomChallengeA + nonceContributionA

    val challenge = Base64.encodeToString(message1ByteArray, Base64.NO_WRAP)

    // Fetch
    // ...
    // End fetch

    return InitiateAuthResult.Success(challenge = Challenge(
      challenge = challenge,
      context = uuid + "," + "2021-06-05T14:17:00Z" + "," + Base64.encodeToString(randomChallengeA, Base64.NO_WRAP) + "," + Base64.encodeToString(nonceContributionA, Base64.NO_WRAP)
    ))
  }

  override suspend fun requestToken(uuid: String, context: String, challengeResponse: String) : RequestTokenResult {
    // Validate UUID & IoT Token context
    val contextElements = context.split(",")
    val contextUUID = contextElements[0]
    val contextTimestamp = contextElements[1]

    if (contextUUID != uuid) {
      Log.e(WebService.TAG, "UUID does not match context")

      return RequestTokenResult.Failure(Exception("UUID does not match context"), FailureReason.BadRequest)
    }

    // Retrieve RA | NA
    val contextRandomChallengeA = Base64.decode(contextElements[2], Base64.NO_WRAP)
    Log.d(WebService.TAG, "RA: ${contextRandomChallengeA.joinToString("") { "%02x".format(it) }}")

    val contextNonceContributionA = Base64.decode(contextElements[3], Base64.NO_WRAP)
    Log.d(WebService.TAG, "NA: ${contextNonceContributionA.joinToString("") { "%02x".format(it) }}")


    // Validate message 2

    val message2Decoded = Base64.decode(challengeResponse, Base64.NO_WRAP)

    val message2RandomChallengeB = message2Decoded.copyOfRange(0, 16)
    Log.d(WebService.TAG, "RB: ${message2RandomChallengeB.joinToString("") { "%02x".format(it) }}")

    val message2RandomNonceContributionB = message2Decoded.copyOfRange(16, 16+4)
    Log.d(WebService.TAG, "NB: ${message2RandomNonceContributionB.joinToString("") { "%02x".format(it) }}")

    val message2Hash = message2Decoded.copyOfRange(20, 16+4+16)
    Log.d(WebService.TAG, "Hash: ${message2Hash.joinToString("") { "%02x".format(it) }}")

    val accessKey = kdf(
      deviceKey = "TODO",
      accessKeyLabel = "PHL-BLE-AKE-Key-Label",
      accessKeyContext = "PHL-BLE-AKE-Key-Context")

    Log.d(WebService.TAG, "AK: ${accessKey.joinToString("") { "%02x".format(it) }}")

    val parsedUUID = UUID.fromString(uuid)
    val uuidBuffer = ByteBuffer.allocate(16)
    uuidBuffer.putLong(parsedUUID.mostSignificantBits)
    uuidBuffer.putLong(parsedUUID.leastSignificantBits)
    val uuidBytes = uuidBuffer.array()    //.reversed()

    Log.d(WebService.TAG, "UUID: ${uuidBytes.joinToString("") { "%02x".format(it) }}")

    val message2HashInput =
      contextRandomChallengeA +
        message2RandomChallengeB +
        uuidBytes +
        contextNonceContributionA +
        message2RandomNonceContributionB

    Log.d(WebService.TAG, "Input: ${message2HashInput.joinToString("") { "%02x".format(it) }}")

    val ourHash = aesCMAC(accessKey, message2HashInput)
    Log.d(WebService.TAG, "Our hash: ${ourHash.joinToString("") { "%02x".format(it) }}")

    if (!message2Hash.contentEquals(ourHash)) {
      Log.e(WebService.TAG, "Hashes don't match")

      return RequestTokenResult.Failure(Exception("Hashes don't match"), FailureReason.Forbidden)
    }

    val tokenInput = message2RandomChallengeB + contextRandomChallengeA

    val token = aesCMAC(accessKey, tokenInput)

    val encodedToken = Base64.encodeToString(token, Base64.NO_WRAP)

    return RequestTokenResult.Success(token = Token(token = encodedToken))
  }

  private fun kdf(
    deviceKey: String,
    accessKeyLabel: String,
    accessKeyContext: String
  ) : ByteArray {
    val i1 = byteArrayOf(
      0x00,
      0x00,
      0x00,
      0x01
    )

    val i2 = byteArrayOf(
      0x00,
      0x00,
      0x00,
      0x02
    )

    val l = byteArrayOf(
      0x00,
      0x00,
      0x01,
      0x00
    )

    val labelBytes = accessKeyLabel.toByteArray(charset = Charsets.UTF_8)
    val contextBytes = accessKeyContext.toByteArray(charset = Charsets.UTF_8)

    val deviceKeyBytes = stringToByteArray(deviceKey)

    // CMAC(KI, i||Label||0x00||Context||L) || CMAC(..)

    val inputPart1 = i1 + labelBytes + 0x00 + contextBytes + l

    Log.d(WebService.TAG, "Input 1: ${inputPart1.joinToString("") { "%02x".format(it) }}")

    val derivedKeyPart1 = aesCMAC(deviceKeyBytes, inputPart1)

    val inputPart2 = i2 + labelBytes + 0x00 + contextBytes + l

    Log.d(WebService.TAG, "Input 2: ${inputPart2.joinToString("") { "%02x".format(it) }}")


    val derivedKeyPart2 = aesCMAC(deviceKeyBytes, inputPart2)


    val derivedKey = derivedKeyPart1 + derivedKeyPart2

    return derivedKey
  }

  private fun logBytesWithLabel(label: String, bytes: ByteArray) {
    Log.d(WebService.TAG, "${label}: ${bytes.joinToString("") { "%02x".format(it) }}")
  }

  private fun stringToByteArray(hexString: String) : ByteArray {
    return hexString.chunked(2)
      .map { it.toInt(16).toByte() }
      .toByteArray()
  }

  private fun aesEncrypt(key: ByteArray, iv: ByteArray, data: ByteArray) : ByteArray {
    val c = Cipher.getInstance("AES/CBC/NoPadding")
    val sk = SecretKeySpec(key, "AES")
    val iv = IvParameterSpec(iv)
    c.init(Cipher.ENCRYPT_MODE, sk, iv)

    return c.doFinal(data)
  }

  @ExperimentalUnsignedTypes
  private fun aesCMAC(key: ByteArray, data: ByteArray) : ByteArray {
    // Calculate the AES-CMAC-128 according RFC4493
    // See: https://tools.ietf.org/html/rfc4493

    val preparedData = ByteArrayOutputStream(data.count())

    // SubKey generation
    // step 1, AES-128 with key K is applied to an all-zero input block.
    val L = aesEncrypt(key, ByteArray(16), ByteArray(16))

    // step 2, K1 is derived through the following operation:
    val firstSubkey = rol(L)

    // If the most significant bit of L is equal to 0, K1 is the left-shift of L by 1 bit.
    if ((L[0].toInt() and 0x80) == 0x80) {
      val lastFirstSubkeyByte = firstSubkey[15].toUByte()

      // Otherwise, K1 is the exclusive-OR of const_Rb and the left-shift of L by 1 bit.
      firstSubkey[15] = (lastFirstSubkeyByte.toInt() xor 0x87).toByte()
    }

    // step 3, K2 is derived through the following operation:
    val secondSubkey = rol(firstSubkey)

    // If the most significant bit of K1 is equal to 0, K2 is the left-shift of K1 by 1 bit.
    if ((firstSubkey[0].toInt() and 0x80) == 0x80) {
      val lastSecondSubkeyByte = secondSubkey[15].toUByte()

      // Otherwise, K2 is the exclusive-OR of const_Rb and the left-shift of K1 by 1 bit.
      secondSubkey[15] = (lastSecondSubkeyByte.toInt() xor 0x87).toByte()
    }

    // MAC computing
    if ((data.count() != 0) && (data.count() % 16 == 0)) {
      preparedData.write(data, 0, data.count() - 16)

      // If the size of the input message block is equal to a positive multiple of the block size (namely, 128 bits),
      // the last block shall be exclusive-OR'ed with K1 before processing
      for (j in 0 until firstSubkey.count()) {
        val dataByte = data[data.count() - 16 + j].toUByte()
        val firstSubkeyByte = firstSubkey[j].toUByte()

        preparedData.write(dataByte.toInt() xor firstSubkeyByte.toInt())
      }
    } else {
      val lastBlockDataCount = data.count() % 16

      preparedData.write(data, 0, data.count() - lastBlockDataCount)

      for (j in 0 until secondSubkey.count()) {
        val secondSubkeyByte = secondSubkey[j].toUByte()

        if (j < lastBlockDataCount) {
          // Data exclusive-OR'ed with K2
          val dataByte = data[data.count() - lastBlockDataCount + j].toUByte()

          preparedData.write(dataByte.toInt() xor secondSubkeyByte.toInt())
        } else if (j == lastBlockDataCount) {
          // First byte of padding is 0x80 (complete last block is xor'ed with K2)
          preparedData.write(0x80 xor secondSubkeyByte.toInt())
        } else {
          // All other padding is 0x00 (complete last block is xor'ed with K2)
          preparedData.write(0x00 xor secondSubkeyByte.toInt())
        }
      }
    }

    val data = preparedData.toByteArray()

    logBytesWithLabel("data", data)

    val encryptResult = aesEncrypt(key, ByteArray(16), data)

    logBytesWithLabel("result", encryptResult)

    return encryptResult.copyOfRange(encryptResult.count() - 16, encryptResult.count())
  }

  @ExperimentalUnsignedTypes
  private fun rol(bytes: ByteArray) : ByteArray {
    val r = ByteArray(bytes.count())
    var carry = 0.toUByte()

    for (i in bytes.count() - 1 downTo 0) {
      val byte = bytes[i].toUByte()

      val u = (byte.toUInt() shl 1)

      // Log.d(TAG, "u: ${u.toString(2)}")

      r[i] = ((u and 0xff.toUInt()) + carry.toUInt()).toByte()

      // Log.d(TAG, "ri: ${r[i].toString(2)}")

      carry = ((u and 0xff00.toUInt()) shr 8).toUByte()

      // Log.d(TAG, "carry: ${carry.toString(2)}")
    }

    return r
  }
}
