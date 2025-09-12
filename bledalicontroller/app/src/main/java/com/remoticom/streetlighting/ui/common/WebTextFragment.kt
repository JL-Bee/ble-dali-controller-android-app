package com.remoticom.streetlighting.ui.common

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs

import com.remoticom.streetlighting.R

class WebTextFragment : Fragment() {

  companion object {
    fun newInstance() = WebTextFragment()
  }

  private val viewModel: WebTextViewModel by viewModels()

  val args: WebTextFragmentArgs by navArgs()

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {

    val view = inflater.inflate(R.layout.fragment_web_text, container, false)

    val webView = view.findViewById<WebView>(R.id.web_text_webview).apply {
      settings.javaScriptEnabled = true
      settings.domStorageEnabled = true
    }

    webView.settings.cacheMode = WebSettings.LOAD_CACHE_ONLY
    webView.webViewClient = WebViewClient()

    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val webView = view.findViewById<WebView>(R.id.web_text_webview)

    context?.let {
      (requireActivity() as AppCompatActivity).supportActionBar?.title = it.getString(args.title)
      webView.loadUrl(it.getString(args.uri))
    }
  }
}
