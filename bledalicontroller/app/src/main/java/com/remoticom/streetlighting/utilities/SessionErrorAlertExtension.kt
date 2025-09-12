package com.remoticom.streetlighting.utilities

import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.remoticom.streetlighting.R
import com.remoticom.streetlighting.services.authentication.AuthenticationService

fun Fragment.showShouldLogoutAlert(@StringRes titleId: Int, error : String?) {
  context?.let {
    MaterialAlertDialogBuilder(it)
      .setTitle(titleId)
      .setMessage(getString(R.string.node_info_error_message_should_logout_format, error))
      .setNeutralButton(R.string.node_info_save_asset_name_neutral_button_text) {
          dialogInterface: DialogInterface?, _ ->
        run {
          dialogInterface?.cancel()

          AuthenticationService.getInstance().logout()
        }
      }
      .show()
  }
}
