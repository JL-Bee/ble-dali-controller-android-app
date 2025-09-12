package com.remoticom.streetlighting.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.remoticom.streetlighting.services.authentication.AuthenticationService

class LoginViewModelFactory(private val authenticationService: AuthenticationService) :
  ViewModelProvider.Factory {

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return LoginViewModel(authenticationService) as T
  }
}
