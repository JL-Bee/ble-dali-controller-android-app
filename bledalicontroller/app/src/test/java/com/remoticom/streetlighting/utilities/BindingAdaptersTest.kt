package com.remoticom.streetlighting.utilities

import android.test.mock.MockContext
import android.view.View
import com.remoticom.streetlighting.data.NodeConnectionStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BindingAdaptersTest {

  @Test
  fun `enabledWhenConnected disables view until node is connected`() {
    val view = TestView()
    view.isEnabled = true

    enabledWhenConnected(view, NodeConnectionStatus.CONNECTING)
    assertFalse(view.isEnabled)

    enabledWhenConnected(view, NodeConnectionStatus.CONNECTED)
    assertTrue(view.isEnabled)
  }

  private class TestView : View(MockContext())
}
