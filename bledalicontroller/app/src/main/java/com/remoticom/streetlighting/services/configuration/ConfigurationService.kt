package com.remoticom.streetlighting.services.configuration

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.remoticom.streetlighting.data.NodeRepository
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.GeneralMode
import com.remoticom.streetlighting.ui.nodes.settings.EditableNode
import com.remoticom.streetlighting.ui.nodes.settings.NodeSettingsViewModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.lang.Exception

class ConfigurationService {
  // TODO: Needed? And what statuses?
  // TODO: Or switch to inProgress boolean?
  enum class Status {
    None,
    Loading,
    Loaded,
    Saving,
    Saved,
  }

  data class State(
    val loadedConfiguration: EditableNode? = null,
    val status: Status = Status.None,
    val lastConfigurationName: String? = null
  )

  private val jsonSerialized = Json { encodeDefaults = true }

  val state: MutableLiveData<State> = MutableLiveData(State())

  private val configurationsDirectory = "configurations"
  private val configurationFileSuffix = ".json"

  fun saveConfiguration(context: Context, editableNode: EditableNode, name: String) {
    Log.d(TAG, "Saving configuration with name ${name}")

    val configuration = Configuration.fromEditableNode(editableNode)

    val json = jsonSerialized.encodeToString(configuration)

    Log.d(TAG, "Saving JSON: $json")

    val file = File(context.getDir(configurationsDirectory, Context.MODE_PRIVATE), "${name}${configurationFileSuffix}")

    try {
      file.writeBytes(json.toByteArray(charset = Charsets.UTF_8))
    } catch (ex: Exception) {
      Log.e(TAG, "Error saving configuration: ${ex.message}")
    }

    state.value?.let {
      state.postValue(it.copy(
        lastConfigurationName = name
      ))
    }
  }

  fun loadConfiguration(context: Context, name: String) {
    Log.d(TAG, "Loading configuration with name $name")

    val file = File(context.getDir(configurationsDirectory, Context.MODE_PRIVATE), "${name}${configurationFileSuffix}")

    try {
      val json = file.readBytes().toString(Charsets.UTF_8)

      Log.d(TAG, "Loaded JSON: ${json}")

      val configuration = Json.decodeFromString<Configuration>(json)

      state.postValue(
        State(
          loadedConfiguration = configuration.toEditableNode(),
          lastConfigurationName = name
        )
      )
    } catch (ex: Exception) {
      Log.e(TAG, "Error loading configuration: ${ex.message}")
    }
  }

//    // Testing purposes
//    state.postValue(State(
//      loadedConfiguration = EditableNode(
//        generalMode = GeneralMode.NOMINAL,
//        dimPreset = null,
//        dimSteps = null,
//        dimLevel = 20.0f,
//        dimPlanningEnabled = null,
//        daliClo = null,
//        daliPowerLevel = null,
//        daliFadeTime = 0.0f,
//        timeTimeZoneUtcOffset = null,
//        timeTimeZoneDayLightSavingTimeEnabled = null,
//        timeMidnightOffset = 0.0f,
//      )
//    ))

  fun listConfigurations(context: Context) : List<String> {
    return context.getDir(configurationsDirectory, Context.MODE_PRIVATE).listFiles { _, s -> s.endsWith(configurationFileSuffix) }?.map {
      it.name.removeSuffix(configurationFileSuffix)
    } ?: emptyList()
  }

  fun clearConfigurations(context: Context) {
    context.getDir(configurationsDirectory, Context.MODE_PRIVATE).listFiles { _, s -> s.endsWith(configurationFileSuffix) }?.forEach {
      it.delete()
    }
  }

  companion object {
    private const val TAG = "ConfigurationService"
  }
}
