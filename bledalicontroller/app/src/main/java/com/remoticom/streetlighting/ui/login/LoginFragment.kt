package com.remoticom.streetlighting.ui.login

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels

import com.remoticom.streetlighting.databinding.FragmentLoginBinding
import com.remoticom.streetlighting.utilities.InjectorUtils
import com.remoticom.streetlighting.utilities.hideKeyboard

class LoginFragment : Fragment() {
  private val viewModel: LoginViewModel by activityViewModels {
    InjectorUtils.provideLoginViewModelFactory()
  }

  private var _binding: FragmentLoginBinding? = null

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {

    val binding = FragmentLoginBinding.inflate(inflater, container, false).apply {
      viewmodel = viewModel
      lifecycleOwner = viewLifecycleOwner
    }

    binding.loginButtonLogin.setOnClickListener {
//      val username = binding.loginEdittextUsername.text.toString()
//      val password = binding.loginEdittextPassword.text.toString()

      this.activity?.let {
        viewModel.login(it)
      }

      activity?.hideKeyboard()
    }

    return binding.root
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    Log.d(TAG, "Activity created. Create app and acquire token if possible")

    viewModel.start(activity!!)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  companion object {
    const val TAG = "LoginFragment"
  }
}
