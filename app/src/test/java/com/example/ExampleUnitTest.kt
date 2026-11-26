package com.example

import com.example.ui.components.formatDurationMs
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun formatDurationMs_formatsCorrectly() {
    assertEquals("00:00", formatDurationMs(0L))
    assertEquals("00:32", formatDurationMs(32_000L))
    assertEquals("03:05", formatDurationMs(185_000L))
    assertEquals("00:00", formatDurationMs(-500L))
  }
}
