package com.remoticom.streetlighting.services.web

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class OkHttpHttpClientFactory(/*private val authenticationProvider: AuthenticationProvider*/) : HttpClientFactory {
  override fun create(): HttpClient {
    return HttpClient(OkHttp) {
      install(ContentNegotiation) {
        json(Json {
          encodeDefaults = false
          ignoreUnknownKeys = true
        })
      }
    }
  }
}
