package com.example.data

import android.content.Context
import android.net.Uri
import java.nio.charset.Charset

data class LyricLine(
  val timestampMs: Long?,
  val text: String,
)

object LrcParser {
  private val TIMESTAMP_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

  fun parse(raw: String?): List<LyricLine> {
    if (raw.isNullOrBlank()) return emptyList()
    val timedLines = mutableListOf<LyricLine>()
    val plainLines = mutableListOf<LyricLine>()

    for (rawLine in raw.lines()) {
      val trimmed = rawLine.trim()
      if (trimmed.isEmpty()) continue
      val matches = TIMESTAMP_REGEX.findAll(trimmed).toList()
      if (matches.isNotEmpty()) {
        val text = TIMESTAMP_REGEX.replace(trimmed, "").trim()
        if (text.isNotEmpty()) {
          for (m in matches) {
            val min = m.groupValues[1].toLongOrNull() ?: 0L
            val sec = m.groupValues[2].toLongOrNull() ?: 0L
            val fracStr = m.groupValues[3]
            val millis =
              when (fracStr.length) {
                0 -> 0L
                1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                else -> fracStr.take(3).toLongOrNull() ?: 0L
              }
            timedLines.add(LyricLine(timestampMs = min * 60_000L + sec * 1_000L + millis, text = text))
          }
        }
      } else if (!trimmed.startsWith("[")) {
        plainLines.add(LyricLine(timestampMs = null, text = trimmed))
      }
    }

    return if (timedLines.isNotEmpty()) {
      timedLines.sortedBy { it.timestampMs ?: 0L }
    } else {
      plainLines
    }
  }

  fun findActiveIndex(lines: List<LyricLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    var active = -1
    for (i in lines.indices) {
      val ts = lines[i].timestampMs ?: continue
      if (ts <= positionMs + 250L) {
        active = i
      } else {
        break
      }
    }
    return active
  }

  fun extractId3Lyrics(context: Context, uri: Uri): String? {
    return try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        val header = ByteArray(10)
        if (stream.read(header) < 10) return@use null
        if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
          return@use null
        }
        val version = header[3].toInt() and 0xFF
        val tagSize =
          ((header[6].toInt() and 0x7F) shl 21) or
            ((header[7].toInt() and 0x7F) shl 14) or
            ((header[8].toInt() and 0x7F) shl 7) or
            (header[9].toInt() and 0x7F)
        val maxRead = tagSize.coerceAtMost(256 * 1024)
        val tagBytes = ByteArray(maxRead)
        var offset = 0
        while (offset < maxRead) {
          val read = stream.read(tagBytes, offset, maxRead - offset)
          if (read <= 0) break
          offset += read
        }
        parseUsltFrame(tagBytes, offset, version)
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun parseUsltFrame(bytes: ByteArray, length: Int, id3Version: Int): String? {
    var pos = 0
    while (pos + 10 < length) {
      val frameId = String(bytes, pos, 4, Charsets.ISO_8859_1)
      if (frameId[0] == '\u0000') break
      val frameSize =
        if (id3Version >= 4) {
          ((bytes[pos + 4].toInt() and 0x7F) shl 21) or
            ((bytes[pos + 5].toInt() and 0x7F) shl 14) or
            ((bytes[pos + 6].toInt() and 0x7F) shl 7) or
            (bytes[pos + 7].toInt() and 0x7F)
        } else {
          ((bytes[pos + 4].toInt() and 0xFF) shl 24) or
            ((bytes[pos + 5].toInt() and 0xFF) shl 16) or
            ((bytes[pos + 6].toInt() and 0xFF) shl 8) or
            (bytes[pos + 7].toInt() and 0xFF)
        }
      pos += 10
      if (frameSize <= 0 || pos + frameSize > length) break
      if (frameId == "USLT" && frameSize > 4) {
        val encoding = bytes[pos].toInt() and 0xFF
        val charset: Charset =
          when (encoding) {
            1, 2 -> Charsets.UTF_16
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
          }
        var cursor = pos + 4 // skip encoding + 3-byte language code
        // skip content descriptor null terminator
        if (encoding == 1 || encoding == 2) {
          while (cursor + 1 < pos + frameSize) {
            if (bytes[cursor] == 0.toByte() && bytes[cursor + 1] == 0.toByte()) {
              cursor += 2
              break
            }
            cursor += 2
          }
        } else {
          while (cursor < pos + frameSize) {
            if (bytes[cursor] == 0.toByte()) {
              cursor++
              break
            }
            cursor++
          }
        }
        val textLen = pos + frameSize - cursor
        if (textLen > 0) {
          return String(bytes, cursor, textLen, charset).trim().takeIf { it.isNotBlank() }
        }
      }
      pos += frameSize
    }
    return null
  }
}
