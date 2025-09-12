package com.remoticom.streetlighting.utilities

import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.databinding.BindingAdapter
import com.google.android.material.slider.Slider
import com.remoticom.streetlighting.R
import com.remoticom.streetlighting.data.Node
import com.remoticom.streetlighting.data.NodeConnectionStatus
import com.remoticom.streetlighting.services.bluetooth.data.DeviceType
import com.remoticom.streetlighting.services.bluetooth.data.characteristics.GeneralMode
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_OUTPUT_RANGE_CONFIG_MAX_PERCENTAGE
import com.remoticom.streetlighting.services.bluetooth.gatt.sno110.SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_OUTPUT_RANGE_CONFIG_MIN_PERCENTAGE
import com.remoticom.streetlighting.services.web.PeripheralStatus
import com.remoticom.streetlighting.services.web.data.OwnershipStatus
import com.remoticom.streetlighting.ui.nodes.info.NodeInfoListItemStatus
import kotlin.math.roundToInt


@BindingAdapter("app:invisibleIfFalse")
fun invisibleIfFalse(view: View, value: Boolean) {
  view.visibility = if (value) View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("app:unnamedIfNull")
fun unnamedIfNull(textView: TextView, value: String?) {
  textView.text =
    if (null == value || value.isEmpty()) textView.context.getText(R.string.node_list_item_unnamed_device) else value
}

@BindingAdapter("app:imageForConnectionStatus")
fun imageForConnectionStatus(
  imageView: ImageView,
  connectionStatus: NodeConnectionStatus
) {
  val color = ContextCompat.getColor(
    imageView.context,
    if (connectionStatus == NodeConnectionStatus.CONNECTED)
      R.color.colorNodeConnected
    else
      R.color.colorNodeDefault
  )
  ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(color));
  ImageViewCompat.setImageTintMode(imageView, PorterDuff.Mode.SRC_ATOP)
}

// @BindingAdapter(value = [ "app:buttonForNode", "app:peripheralStatus"], requireAll = true)
@BindingAdapter("app:buttonForNode")
fun buttonForNode(
  button: Button,
  node: Node
) {

  // Handle situation where we don't know ownership status
  if (null == node.info) {
    when (node.peripheralStatus) {
      PeripheralStatus.Loaded -> {
        // Should not occur, because node info would be known
        button.text = button.context.getString(R.string.node_button_text_loaded_null)
        button.visibility = View.VISIBLE
        button.isEnabled = false
      }
      PeripheralStatus.Error -> {
        // Another error
        // (usually not unauthorized, because that would logout user)
        button.text = button.context.getString(R.string.node_button_text_error)
        button.visibility = View.VISIBLE
        button.isEnabled = false
      }
      PeripheralStatus.Forbidden -> {
        // Probably owned by someone else
        button.text = button.context.getString(R.string.node_button_text_claim)
        button.visibility = View.VISIBLE
        button.isEnabled = false
      }
      PeripheralStatus.None, PeripheralStatus.Loading, null -> {
        // All other cases probably loading (or going to load)
        button.text = button.context.getString(R.string.node_button_text_null)
        button.visibility = View.VISIBLE
        button.isEnabled = false
      }
    }

    return
  }

  when (node.info.ownershipStatus) {
    OwnershipStatus.Unclaimed -> {
      when (node.peripheralStatus) {
        PeripheralStatus.None, PeripheralStatus.Loaded, PeripheralStatus.Error -> {
          button.text = button.context.getString(R.string.node_button_text_claim)
          button.visibility = View.VISIBLE
          button.isEnabled = true
        }
        PeripheralStatus.Loading -> {
          button.text = button.context.getString(R.string.node_button_text_loading)
          button.visibility = View.VISIBLE
          button.isEnabled = false
        }

        else -> {}
      }
    }
    OwnershipStatus.Claimed -> {
      when (node.connectionStatus) {
        NodeConnectionStatus.DISCONNECTED -> {
          button.text = button.context.getString(R.string.node_button_text_connect)
          button.visibility = View.VISIBLE
          button.isEnabled = (null != node.info.password)
        }
        NodeConnectionStatus.CONNECTING -> {
          button.text = button.context.getString(R.string.node_button_text_connecting)
          button.visibility = View.VISIBLE
          button.isEnabled = false
        }
        NodeConnectionStatus.CONNECTED -> {
          button.text = button.context.getString(R.string.node_button_text_disconnect)
          button.visibility = View.VISIBLE
          button.isEnabled = true
        }
        NodeConnectionStatus.DISCONNECTING -> {
          button.text = button.context.getString(R.string.node_button_text_disconnecting)
          button.visibility = View.VISIBLE
          button.isEnabled = false
        }
      }
    }
    else -> {
      button.text = button.context.getString(R.string.node_button_text_unknown)
      button.visibility = View.VISIBLE
      button.isEnabled = false
    }
  }
}

@BindingAdapter("app:enabledWhenConnected")
fun enabledWhenConnected(view : View, connectionStatus: NodeConnectionStatus) {
  when (connectionStatus) {
    NodeConnectionStatus.DISCONNECTED -> {
      view.isEnabled = false
    }
    NodeConnectionStatus.CONNECTING -> {
      view.isEnabled = false
    }
    NodeConnectionStatus.CONNECTED -> {
      view.isEnabled = true
    }
    NodeConnectionStatus.DISCONNECTING -> {
      view.isEnabled = false
    }
  }
}

@BindingAdapter(value = ["app:enabledWhenConnectedAndDimPlanDisabled","app:dimPlanEnabled"], requireAll = true)
fun enabledWhenConnectedAndDimPlanDisabled(view: View, connectionStatus: NodeConnectionStatus, dimPlanEnabled: Boolean) {
  val dimPlanDisabled = !dimPlanEnabled

  when (connectionStatus) {
    NodeConnectionStatus.DISCONNECTED -> {
      view.isEnabled = false
    }
    NodeConnectionStatus.CONNECTING -> {
      view.isEnabled = false
    }
    NodeConnectionStatus.CONNECTED -> {
      view.isEnabled = dimPlanDisabled
    }
    NodeConnectionStatus.DISCONNECTING -> {
      view.isEnabled = false
    }
  }
}

@BindingAdapter(value = ["app:enabledWhenConnectedAndDimPlanEnabled","app:dimPlanEnabled"], requireAll = true)
fun enabledWhenConnectedAndDimPlanEnabled(view: View, connectionStatus: NodeConnectionStatus, dimPlanEnabled: Boolean) {
  when (connectionStatus) {
    NodeConnectionStatus.DISCONNECTED -> {
      view.isEnabled = false
    }
    NodeConnectionStatus.CONNECTING -> {
      view.isEnabled = false
    }
    NodeConnectionStatus.CONNECTED -> {
      view.isEnabled = dimPlanEnabled
    }
    NodeConnectionStatus.DISCONNECTING -> {
      view.isEnabled = false
    }
  }
}

@BindingAdapter(value = ["app:enabledWhenConnectedAndModeNominal","app:generalMode"], requireAll = true)
fun enabledWhenConnectedAndModeNominal(view: View, connectionStatus: NodeConnectionStatus, generalMode: GeneralMode?) {
  when (connectionStatus) {
    NodeConnectionStatus.DISCONNECTED -> {
      view.isEnabled = false
    }
    NodeConnectionStatus.CONNECTING -> {
      view.isEnabled = false
    }
    NodeConnectionStatus.CONNECTED -> {
      view.isEnabled = (generalMode == GeneralMode.NOMINAL)
    }
    NodeConnectionStatus.DISCONNECTING -> {
      view.isEnabled = false
    }
  }
}

@BindingAdapter(value = ["app:enabledWhenConnectedAndModeStepDimming","app:generalMode"], requireAll = true)
fun enabledWhenConnectedAndModeStepDimming(view: View, connectionStatus: NodeConnectionStatus, generalMode: GeneralMode?) {
  when (connectionStatus) {
    NodeConnectionStatus.DISCONNECTED -> {
      view.isEnabled = false
    }
    NodeConnectionStatus.CONNECTING -> {
      view.isEnabled = false
    }
    NodeConnectionStatus.CONNECTED -> {
      view.isEnabled = (generalMode == GeneralMode.STEP_DIMMING)
    }
    NodeConnectionStatus.DISCONNECTING -> {
      view.isEnabled = false
    }
  }
}


@BindingAdapter("app:colorForStatus")
fun colorForStatus(textView: TextView, status: NodeInfoListItemStatus) {
  // val oldColor = textView.currentTextColor

  val attrs = intArrayOf(android.R.attr.textColorSecondary)
  val a = textView.context.theme.obtainStyledAttributes(R.style.AppTheme, attrs)
  val defaultTextColor = a.getColor(0, Color.YELLOW)
  a.recycle()

  when (status) {
    NodeInfoListItemStatus.Success -> textView.setTextColor(textView.resources.getColor(R.color.colorSuccess))
    NodeInfoListItemStatus.Error -> textView.setTextColor(textView.resources.getColor(R.color.colorError))
    else -> textView.setTextColor(defaultTextColor)
    // else default color
  }
}

@BindingAdapter("android:text")
fun setFloat(view: TextView, value: Float) {
  view.text = value.roundToInt().toString(10)
}

@BindingAdapter("app:rangeForDeviceType")
fun rangeForDeviceType(slider: Slider, deviceType: DeviceType) {
  when (deviceType) {
    DeviceType.Zsc010, DeviceType.Bdc -> {
      slider.valueFrom = 0.0f
      slider.valueTo = 100.0f
    }
    DeviceType.Sno110 -> {
      slider.valueFrom = SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_OUTPUT_RANGE_CONFIG_MIN_PERCENTAGE.toFloat()
      slider.valueTo = SNO110_BLUETOOTH_CHARACTERISTIC_LIGHT_CONTROL_OUTPUT_RANGE_CONFIG_MAX_PERCENTAGE.toFloat()
    }
  }
}

@BindingAdapter("app:rangeMinimumForDeviceType")
fun rangeMinimumForDeviceType(textView: TextView, deviceType: DeviceType) {
  when (deviceType) {
    DeviceType.Zsc010, DeviceType.Bdc -> textView.text = textView.resources.getText(R.string.node_settings_light_level_min_zsc010)
    DeviceType.Sno110 -> textView.text = textView.resources.getText(R.string.node_settings_light_level_min_sno110)
  }
}

@BindingAdapter("app:rangeMaximumForDeviceType")
fun rangeMaximumForDeviceType(textView: TextView, deviceType: DeviceType) {
  when (deviceType) {
    DeviceType.Zsc010, DeviceType.Bdc -> textView.text = textView.resources.getText(R.string.node_settings_light_level_max_zsc010)
    DeviceType.Sno110 -> textView.text = textView.resources.getText(R.string.node_settings_light_level_max_sno110)
  }
}
