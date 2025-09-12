package com.remoticom.streetlighting

import kotlinx.coroutines.CoroutineScope

interface CoroutineScopeProvider {
  fun provideScope() : CoroutineScope
}
