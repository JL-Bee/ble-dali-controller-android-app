package com.remoticom.streetlighting.ui.signout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SignoutViewModel : ViewModel() {

  private val _text = MutableLiveData<String>().apply {
    value = "This is signout Fragment"
  }
  val text: LiveData<String> = _text
}
