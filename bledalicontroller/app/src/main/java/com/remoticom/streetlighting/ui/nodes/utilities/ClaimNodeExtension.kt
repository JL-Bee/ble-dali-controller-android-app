package com.remoticom.streetlighting.ui.nodes.utilities

import android.content.DialogInterface
import androidx.fragment.app.Fragment

fun Fragment.showErrorAlert(error: String?) {
  context?.let {
    com.google.android.material.dialog.MaterialAlertDialogBuilder(it)
      .setTitle(com.remoticom.streetlighting.R.string.node_list_claim_node_error_title)
      .setMessage(getString(com.remoticom.streetlighting.R.string.node_list_claim_node_error_message_format, error))
      .setNeutralButton(com.remoticom.streetlighting.R.string.node_list_claim_node_error_neutral_button_text) {
          dialogInterface: DialogInterface?, _ -> dialogInterface?.cancel()
      }
      .show()
  }
}
