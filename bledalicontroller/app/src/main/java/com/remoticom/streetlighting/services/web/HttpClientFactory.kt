package com.remoticom.streetlighting.services.web

import io.ktor.client.HttpClient

interface HttpClientFactory {
  fun create() : HttpClient
}
