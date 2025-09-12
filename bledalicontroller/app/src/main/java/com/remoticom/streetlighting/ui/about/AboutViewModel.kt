package com.remoticom.streetlighting.ui.about

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.remoticom.streetlighting.BuildConfig

class AboutViewModel() : ViewModel() {

  val versionName = BuildConfig.VERSION_NAME
  val versionCode = BuildConfig.VERSION_CODE
}
