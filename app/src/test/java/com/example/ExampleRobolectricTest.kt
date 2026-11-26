package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.formatDurationMs
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Alunan", appName)
  }

  @Test
  fun `format duration formats minutes and seconds`() {
    assertEquals("00:00", formatDurationMs(0L))
    assertEquals("00:32", formatDurationMs(32_000L))
    assertEquals("03:05", formatDurationMs(185_000L))
  }
}
