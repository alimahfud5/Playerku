package com.example

import com.example.data.LrcParser
import com.example.ui.components.formatDurationMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun formatDurationMs_formatsCorrectly() {
    assertEquals("00:00", formatDurationMs(0L))
    assertEquals("00:32", formatDurationMs(32_000L))
    assertEquals("03:05", formatDurationMs(185_000L))
    assertEquals("00:00", formatDurationMs(-500L))
  }

  @Test
  fun lrcParser_parsesSyncedAndMultiTimestampLines() {
    val raw =
      """
      [ti:Contoh Lagu]
      [00:05.00] Baris pertama
      [00:10.50][00:25.25] Reff bersama
      [00:15.0] Baris tengah
      """.trimIndent()

    val lines = LrcParser.parse(raw)
    assertEquals(4, lines.size)
    assertEquals(5_000L, lines[0].timestampMs)
    assertEquals("Baris pertama", lines[0].text)
    assertEquals(10_500L, lines[1].timestampMs)
    assertEquals(15_000L, lines[2].timestampMs)
    assertEquals(25_250L, lines[3].timestampMs)
    assertEquals("Reff bersama", lines[3].text)

    assertEquals(-1, LrcParser.findActiveIndex(lines, 1_000L))
    assertEquals(0, LrcParser.findActiveIndex(lines, 5_100L))
    assertEquals(1, LrcParser.findActiveIndex(lines, 12_000L))
    assertEquals(3, LrcParser.findActiveIndex(lines, 30_000L))
  }

  @Test
  fun lrcParser_parsesPlainLyricsFallback() {
    val raw =
      """
      Bait pertama
      Bait kedua
      """.trimIndent()

    val lines = LrcParser.parse(raw)
    assertEquals(2, lines.size)
    assertNull(lines[0].timestampMs)
    assertEquals("Bait pertama", lines[0].text)
    assertEquals(-1, LrcParser.findActiveIndex(lines, 4_000L))
    assertTrue(LrcParser.parse("").isEmpty())
  }
}
