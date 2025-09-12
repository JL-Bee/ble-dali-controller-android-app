package com.remoticom.streetlighting.services.bluetooth.gatt.sno110

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Base64
import android.util.Log
import android.util.Range
import com.remoticom.streetlighting.services.bluetooth.data.Device
import com.remoticom.streetlighting.services.bluetooth.data.DeviceType
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.*
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.TimeZone
import com.remoticom.streetlighting.services.bluetooth.gatt.ConnectionProvider
import com.remoticom.streetlighting.services.bluetooth.gatt.GattOperationsService
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.ConnectGattOperation
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.DiscoverServicesOperation
import com.remoticom.streetlighting.services.bluetooth.gatt.connection.EndReliableWriteOperation
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.characteristics.ScheduleEntry
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.operations.*
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.operations.authentication.Sno110EnableAuthenticationIndicationOperation
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.operations.authentication.Sno110EnableServiceChangedIndicationOperation
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.operations.authentication.Sno110WriteChallengeOperation
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.operations.authentication.Sno110WriteTokenOperation
import com.remoticom.streetlighting.services.web.InitiateAuthResult
import com.remoticom.streetlighting.services.web.RequestTokenResult
import com.remoticom.streetlighting.services.web.TokenProvider
import com.remoticom.streetlighting.services.web.data.Peripheral
import com.remoticom.streetlighting.ui.nodes.settings.DIM_LEVEL_STEP_SIZE
import com.remoticom.streetlighting.ui.nodes.settings.utilities.percentageToDimLevel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class Sno110OperationsService constructor(
  connectionProvider: ConnectionProvider
) : GattOperationsService(connectionProvider) {

  override suspend fun connect(
    device: Device,
    tokenProvider: TokenProvider,
    peripheral: Peripheral?
  ) {
    // receive message1 (and context) from backend
    when (val challengeResult = tokenProvider.initiateAuth(device.uuid)) {
      is InitiateAuthResult.Failure -> {
        Log.w(TAG, "Initiate failed")

        return
      }
      is InitiateAuthResult.Success -> {
        Log.d(TAG, "Initiate succeeded")

        val challenge = challengeResult.challenge
        // Log.d(TAG, challenge.context)

        if (!performConnectOperation()) {
          return
        }

        if (!performPreAuthenticateOperations()) {
          connectionProvider.tearDown()
          return
        }

        // message1: RA | NA
        val challengeBytes = Base64.decode(challenge.challenge, Base64.NO_WRAP)
        // Log.d(WebService.TAG, "Challenge: ${challengeBytes.joinToString("") { "%02x".format(it) }}")

        // Send message1, receive message2 via indication
        val message2 = requestDeviceChallenge(challengeBytes)

        val challengeResponse = Base64.encodeToString(message2, Base64.NO_WRAP)

        // receive message3 from backend
        when (val requestTokenResult = tokenProvider.requestToken(device.uuid, challenge.context, challengeResponse)) {

          is RequestTokenResult.Failure -> {
            Log.w(TAG, "Request token failed")
            connectionProvider.tearDown()
            return
          }

          is RequestTokenResult.Success -> {
            Log.d(TAG, "Request token succeeded")
            // Log.d(TAG, "Token: ${requestTokenResult.token.token}")

            val tokenBytes = Base64.decode(requestTokenResult.token.token, Base64.NO_WRAP)

            // send message3
            if (!authenticateToDeviceWithToken(tokenBytes)) {
              Log.w(TAG, "Authentication to device: failed / not authorized")
              connectionProvider.tearDown()
              return
            }

            Log.d(TAG, "Authentication to device: success / authorized")

            if (!performPostAuthenticateOperations()) {
              connectionProvider.tearDown()
              return
            }
          }
        }
      }
    }
  }

  private suspend fun performConnectOperation(): Boolean {
    return connectionProvider.performOperation(ConnectGattOperation(), false)
  }

  private suspend fun performPreAuthenticateOperations(): Boolean {
    // When app aborts halfway a reliable write future write operations will not succeed
    // Cleaning up any old write operations
    if (!connectionProvider.performOperation(
        EndReliableWriteOperation(false),
        false
      )
    ) {
      return false
    }

    if (!connectionProvider.performOperation(
        DiscoverServicesOperation(),
        false
      )
    ) {
      return false
    }

    if (!connectionProvider.performOperation(
        Sno110EnableAuthenticationIndicationOperation(),
        false
      )
    ) {
      return false
    }

    if (!connectionProvider.performOperation(
        Sno110EnableServiceChangedIndicationOperation(),
        false
      )
    ) {
      return false
    }
    return true
  }

  private suspend fun requestDeviceChallenge(challengeBytes: ByteArray?): ByteArray? {
    // return value is message2
    return connectionProvider.performOperation(
      Sno110WriteChallengeOperation(
        SNO110_BLUETOOTH_SERVICE_SECURITY,
        SNO110_BLUETOOTH_CHARACTERISTIC_SECURITY_AUTHENTICATE,
        challengeBytes,
        BluetoothGattCharacteristic::serializeChallenge,
        BluetoothGattCharacteristic::deserializeChallenge,
        BluetoothGattCharacteristic::deserializeChallengeResponse
      )
    )
  }

  private suspend fun authenticateToDeviceWithToken(tokenBytes: ByteArray): Boolean {
    return connectionProvider.performOperation(
      Sno110WriteTokenOperation(
        SNO110_BLUETOOTH_SERVICE_SECURITY,
        SNO110_BLUETOOTH_CHARACTERISTIC_SECURITY_AUTHENTICATE,
        tokenBytes /* + byteArrayOf(0xff.toByte(), 0x80.toByte(), 0x01.toByte()) */,
        BluetoothGattCharacteristic::serializeToken,
        BluetoothGattCharacteristic::deserializeToken
      ),
      false
    )
  }

  private suspend fun performPostAuthenticateOperations(): Boolean {
    // More services available after authentication,
    // but seems some devices do not refresh local cache (e.g. Sony XZ2?)
    // Signify: "refresh android gatt database"
    connectionProvider.refresh()

    // Allow node to enable services after authentication
    // Signify: "wait for service changed notification - comes ~200 ms after the Indication"
    delay(500)

    if (!connectionProvider.performOperation(
        DiscoverServicesOperation(),
        false
      )
    ) {
      return false
    }

    // Breathe (blinking 15 times)
    return connectionProvider.performSno110WriteTransaction(listOf(
      Sno110WriteLightControlIdentifyOperation(
        SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_IDENTIFY_BREATHE
      )
    ))

    // TODO (REQUIREMENTS): Send UID signature to backend for extra validation/security when claiming device
    // See SNO110 authentication flow
    // val signature = connectionProvider.performOperation(Sno110ReadSecuritySignatureOperation())
  }

  override suspend fun readGeneralCharacteristics() : GeneralCharacteristics {
    val astroClockSwitchingEnabled = connectionProvider.performOperation(Sno110ReadGeneralAstroClockSwitchingEnabledOperation())

    return GeneralCharacteristics(
      mode = when (astroClockSwitchingEnabled) {
        true -> GeneralMode.ASTRO
        false -> GeneralMode.DIM
        else -> GeneralMode.UNKNOWN // TODO (SNO110): or null ?
      }
    )
  }

  override suspend fun readDimCharacteristics() : DimCharacteristics {
    val schedulerEnabled = connectionProvider.performOperation(Sno110ReadSchedulerEnabledOperation(), false)
    Log.d(TAG, "Scheduler enabled: $schedulerEnabled")

    val numberOfEntries = connectionProvider.performOperation(Sno110ReadSchedulerNumberOfEntriesOperation())
    Log.d(TAG, "Number of scheduler entries: $numberOfEntries")

    // 5
    val maxNumberOfDimSteps = connectionProvider.performOperation(Sno110ReadSchedulerMaxNumberOfDimStepsOperation())
    Log.d(TAG, "Maximum number of dim steps: $maxNumberOfDimSteps")

    val indicationEnabled = connectionProvider.performOperation(Sno110EnableSchedulerControlPointIndicationOperation())

    if (null != indicationEnabled && !indicationEnabled) {
      Log.e(TAG, "Error registering for indications on scheduler control point")
    }

    val entries = connectionProvider.performOperation(Sno110ReadSchedulerScheduleEntriesOperation())

    val lightOutputRangeConfiguration = connectionProvider.performOperation(Sno110ReadLightControlOutputRangeConfigurationOperation())
      ?: return DimCharacteristics()

    // Coerce to avoid being outside slider range (and round to step size)
    val steppedBrightnessPercentage = ((lightOutputRangeConfiguration.upper.coerceIn(
      minimumValue = SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_OUTPUT_RANGE_CONFIG_MIN_PERCENTAGE,
      maximumValue = SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_OUTPUT_RANGE_CONFIG_MAX_PERCENTAGE
    ) / DIM_LEVEL_STEP_SIZE).roundToInt() * DIM_LEVEL_STEP_SIZE).toInt()

    Log.d(TAG, "Brightness: $steppedBrightnessPercentage")

    val stepsFromNode = entries?.first()?.steps

    // Sensible default is to have a single step with current brightness
    val steps = if (stepsFromNode == null || stepsFromNode.isEmpty()) {
      listOf(DimStep(hour = 0, minute = 0, level = percentageToDimLevel(DeviceType.Sno110, steppedBrightnessPercentage)))
    } else {
      stepsFromNode
    }

    val preset = if (schedulerEnabled) steps.size else SNO110_PROXY_DIM_PRESET_PLAN_DISABLED

    return DimCharacteristics(
      preset = preset,
      steps = steps,
      level = steppedBrightnessPercentage
    )
  }

  override suspend fun readDaliCharacteristics() : DaliCharacteristics {
    val fixtureName = connectionProvider.performOperation(Sno110ReadDaliUserDefinedDeviceNameOperation())
    val fadeTimeTenthOfSeconds = connectionProvider.performOperation(Sno110ReadDaliSchedulerFadeTimeOperation(), 0)
    val transitionTimeTenthOfSeconds = connectionProvider.performOperation(Sno110ReadLightControlTransitionTimeOperation())

    Log.d(TAG, "fade time: $fadeTimeTenthOfSeconds / transition time: $transitionTimeTenthOfSeconds")

    val fadeTime: Int = when (fadeTimeTenthOfSeconds / 10.0f) {
      in 0f..0.25f -> 0
      in 0.25f..0.75f -> 1
      in 0.75f..1.25f -> 2
      in 1.25f..1.75f -> 3
      in 1.75f..2.5f -> 4
      in 2.5f..3.5f -> 5
      in 3.5f..5f -> 6
      in 5f..7f -> 7
      in 7f..9f -> 8
      in 9f..12.5f -> 9
      in 17.5f..25f -> 10
      in 17.5f..25f -> 11
      in 25f..37.5f -> 12
      in 37.5f..52.5f -> 13
      in 52.5f..75f -> 14
      else -> 15
    }

    // TODO (SNO110): Else clause might be confusing to user, because node will behave..
    // ..with different value than is being displayed in app
    // Idea: write our max value to device? (Supporting SNO110 range in app
    // might be more complex)

    return DaliCharacteristics(
      fixtureName = fixtureName,
      fadeTime = fadeTime
    )
  }

  override suspend fun readTimeCharacteristics() : TimeCharacteristics {
    val unixTimestamp = connectionProvider.performOperation(Sno110ReadTimeUTCOperation())

    val utcOffset = connectionProvider.performOperation(Sno110ReadTimeTimeZoneOffsetOperation())

    // val timezoneIndex = connectionProvider.performOperation(Sno110ReadTimeTimeZoneIndexOperation())

    // TODO (SNO110): Reading this as a byte array, so we can reuse this operation for writing
    // the complete struct (or is it allowed/possible to change single byte?)
    val dstSettings = connectionProvider.performOperation(Sno110ReadTimeDaylightSavingTimeOperation())

    val dstEnabled = dstSettings?.let {
      if (dstSettings.size == SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_DST_BYTES)
        dstSettings[SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_DST_INDEX_ENABLE_DISABLE] == SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_DST_ENABLED.toByte()
      else
        false
    } == true

    return TimeCharacteristics(
      timezone = TimeZone(
        utcOffset = utcOffset ?: 0,
        daylightSavingTimeEnabled = dstEnabled
      ),
      unixTimestamp = unixTimestamp
    )
  }

  override suspend fun readGpsCharacteristics() : GpsCharacteristics {
    val position = connectionProvider.performOperation(Sno110ReadGpsPositionOperation())

    return GpsCharacteristics(
      position = position
    )
  }

  override suspend fun readDiagnosticsCharacteristics(health: Int?, state: Int?) : DiagnosticsCharacteristics {
    // 16 most significant bits for health
    // 16 least significant bits for state
    val status = ((health ?: 0) shl 16) or (state ?: 0)

    val softwareVersion = connectionProvider.performOperation(
      Sno110ReadDiagnosticsSoftwareVersionOperation(),
      Version.emptyVersion()
    )

    val firmwareVersion = connectionProvider.performOperation(
      Sno110ReadDiagnosticsFirmwareVersionOperation(),
      Version.emptyVersion()
    )

    val version = DiagnosticsVersion(
      firmwareVersion = firmwareVersion,
      libraryVersion = softwareVersion
    )

    return DiagnosticsCharacteristics(status, version)
  }

  override suspend fun readDaliBank(bank: Int): Map<Int, Any> {
    return emptyMap()
  }

  override suspend fun writeGeneralCharacteristics(generalCharacteristics: GeneralCharacteristics?) : Boolean {
    val mode = generalCharacteristics?.mode ?: return false

    return connectionProvider.performSno110WriteTransaction(listOf(
      Sno110WriteAstroClockSwitchingEnabledOperation(mode == GeneralMode.ASTRO)
    ))
  }

  override suspend fun writeDimCharacteristics(dimCharacteristics: DimCharacteristics?) : Boolean {
    Log.d(TAG, "writeDimCharacteristics")

    val preset = dimCharacteristics?.preset ?: return false

    val success = if (preset == 0) {

      dimCharacteristics.level?.let { originalDimLevel ->

        // Specs: The value of Min Percentage field shall be less than or equal to the Max Percentage field.
        // Signify (and app) default to 10% as minimum.
        // Because Remoticom slider goes to 0, we limit to 1%.
        // (0% or turning off does not seem to be possible)
        val dimLevelConstrainedToValidRange = originalDimLevel.coerceIn(
          SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_OUTPUT_RANGE_CONFIG_MIN_PERCENTAGE,
          SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_OUTPUT_RANGE_CONFIG_MAX_PERCENTAGE
        )

        connectionProvider.performSno110WriteTransaction(
          listOf(
            Sno110WriteSchedulerEnabledOperation(false),
            Sno110WriteLightControlLightOutputRangeConfiguration(
              Range(
                SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_OUTPUT_RANGE_CONFIG_MIN_PERCENTAGE,
                dimLevelConstrainedToValidRange
              )
            )
          )
        )
      } ?: false // if dim level is null
    } else {
      connectionProvider.performSno110WriteTransaction(listOf(
        Sno110WriteSchedulerEnabledOperation(true),
        // Setting output range to maximum is not needed; entry with 100% = 100%
      ))
    }

    val successSteps = dimCharacteristics.steps?.let { steps ->
      val entry = ScheduleEntry(
        version = 0x02,
        startDateYear = 0,
        startDateMonth = 1,
        startDateDay = 1,
        recurrence = 0x7f,
        steps = steps
      )

      connectionProvider.performSno110WriteTransaction(listOf(
        Sno110WriteSchedulerStartWriteOperation(),
        Sno110WriteSchedulerEntryWriteOperation(entry),
        Sno110WriteSchedulerFinishWriteOperation()
      ))
    } ?: false

    val successAll = success && successSteps

    Log.d(TAG, "Result writing dim settings: $successAll ($success / $successSteps)")

    return success
  }

  override suspend fun writeDaliCharacteristics(daliCharacteristics: DaliCharacteristics?) : Boolean {
    val fadeTimeInSeconds = when(daliCharacteristics?.fadeTime) {
      0 -> 0f
      1 -> 0.5f
      2 -> 1f
      3 -> 1.5f
      4 -> 2f
      5 -> 3f
      6 -> 4f
      7 -> 6f
      8 -> 8f
      9 -> 10f
      10 -> 15f
      11 -> 20f
      12 -> 30f
      13 -> 45f
      14 -> 60f
      15 -> 90f
      else -> null
    } ?: return false

    val fadeTimeTenthsOfSeconds = (fadeTimeInSeconds * 10).roundToInt()

    return connectionProvider.performSno110WriteTransaction(listOf(
      // TODO (SNO110 / REQUIREMENTS): Ok to write default value of 700ms for transition time?
      Sno110WriteLightControlTransitionTimeOperation(7),
      Sno110WriteSchedulerFadeTimeOperation(fadeTimeTenthsOfSeconds)
    ))
  }

  override suspend fun writeTimeCharacteristics(timeCharacteristics: TimeCharacteristics?) : Boolean {

    val daylightSavingTimeStruct = connectionProvider.performOperation(Sno110ReadTimeDaylightSavingTimeOperation())

    val daylightSavingTimeEnabled = timeCharacteristics?.timezone?.daylightSavingTimeEnabled ?: return false

    daylightSavingTimeStruct?.let {
      it[SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_DST_INDEX_ENABLE_DISABLE] =
        if (daylightSavingTimeEnabled)
          SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_DST_ENABLED.toByte()
        else
          SNO110_BLUETOOTH_CHARACTERISTIC_DEVICE_CONFIG_DST_DISABLED.toByte()
    }

    return connectionProvider.performSno110WriteTransaction(listOf(
      Sno110WriteTimeTimeZoneOffsetOperation(timeCharacteristics.timezone.utcOffset),

      // TODO (SNO110): Not sure whether this has effect on SNO110
      Sno110WriteTimeDaylightSavingTimeOperation(daylightSavingTimeStruct)
    ))
  }

  companion object {
    private const val TAG = "Sno110OperationsService"
  }
}
