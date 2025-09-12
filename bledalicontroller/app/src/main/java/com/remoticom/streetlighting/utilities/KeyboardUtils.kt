package com.remoticom.streetlighting.utilities

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager

fun Activity.hideKeyboard() {
  // Check if no view has focus
  val currentFocusedView = this.currentFocus
  currentFocusedView?.let {
    val inputMethodManager =
      this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(
      currentFocusedView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
    )
  }
}
