package com.remoticom.streetlighting.ui.login

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.remoticom.streetlighting.services.authentication.AuthenticationService

class LoginViewModel(private val authenticationService: AuthenticationService) : ViewModel() {

  val state = authenticationService.state.map {
    it
  }

  fun start(activity: Activity) {
    authenticationService.start(activity)
  }

  fun login(activity: Activity) {
    authenticationService.login(activity)
  }

  fun logout() {
    authenticationService.logout()
  }

  fun resume() {
    authenticationService.resume()
  }

//  fun acquireToken(activity: Activity) {
//    // authenticationService.acquireToken(activity)
//  }
}
