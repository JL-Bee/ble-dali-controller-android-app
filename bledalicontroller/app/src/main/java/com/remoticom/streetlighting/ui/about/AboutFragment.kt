package com.remoticom.streetlighting.ui.about

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.remoticom.streetlighting.R

class AboutFragment : Fragment() {

  private val aboutViewModel: AboutViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val root = inflater.inflate(R.layout.fragment_about, container, false)

    val versionTextView: TextView = root.findViewById(R.id.textview_version)
    versionTextView.text =
      "Version ${aboutViewModel.versionName} (${aboutViewModel.versionCode})"

    val applicationNameTextView =
      root.findViewById<TextView>(R.id.textview_application_name)
    applicationNameTextView.text = context?.packageManager?.let {
      context?.applicationInfo?.loadLabel(it).toString()
    }



    // Convenience method used:
    // button.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_nav_about_to_nav_disclaimer, null))

    context?.let { ctx ->
      val termsButton = root.findViewById<Button>(R.id.button_terms_and_conditions)

      termsButton.setOnClickListener {
        findNavController().navigate(
          AboutFragmentDirections.actionNavAboutToNavWebText(
            R.string.about_terms_of_use_url,
            R.string.about_terms_of_use_title
          )
        )
      }

      val privacyButton = root.findViewById<Button>(R.id.button_privacy_policy)

      privacyButton.setOnClickListener {
        findNavController().navigate(
          AboutFragmentDirections.actionNavAboutToNavWebText(
            R.string.about_privacy_notice_url,
            R.string.about_privacy_notice_title
          )
        )
      }
    }

    return root
  }
}
