package com.remoticom.streetlighting.ui.signout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.remoticom.streetlighting.R
import com.remoticom.streetlighting.ui.login.LoginViewModel
import com.remoticom.streetlighting.utilities.InjectorUtils

class SignoutFragment : Fragment() {

  private val loginViewModel: LoginViewModel by activityViewModels {
    InjectorUtils.provideLoginViewModelFactory()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val root = inflater.inflate(R.layout.fragment_signout, container, false)

    root.findViewById<Button>(R.id.logoutButton).setOnClickListener {
      loginViewModel.logout()
    }

    return root
  }
}
