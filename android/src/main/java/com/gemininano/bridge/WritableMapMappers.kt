package com.gemininano.bridge

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.gemininano.domain.model.Availability
import com.gemininano.domain.model.DownloadState
import com.gemininano.domain.model.GeminiNanoError
import com.gemininano.domain.model.TextGenerationResult
import com.gemininano.domain.mapper.ThrowableErrorMapper

fun Availability.toWritableMap(): WritableMap {
  return Arguments.createMap().apply {
    putString("status", status)
    putBoolean("isAvailable", isAvailable)
    putString("message", message)
    errorCode?.let { putString("errorCode", it) }
  }
}

fun DownloadState.toWritableMap(errorMapper: ThrowableErrorMapper): WritableMap {
  return Arguments.createMap().apply {
    when (this@toWritableMap) {
      is DownloadState.Started -> {
        putString("status", "started")
        putDouble("totalBytes", totalBytes.toDouble())
      }
      is DownloadState.InProgress -> {
        putString("status", "in_progress")
        putDouble("bytesDownloaded", bytesDownloaded.toDouble())
      }
      DownloadState.Completed -> {
        putString("status", "completed")
      }
      is DownloadState.Failed -> {
        val normalized = errorMapper.map(cause)
        putString("status", "failed")
        putString("code", normalized.code)
        putString("message", normalized.message)
      }
    }
  }
}

fun GeminiNanoError.toWritableMap(): WritableMap {
  return Arguments.createMap().apply {
    putString("code", code)
    putString("message", message)
  }
}

fun String.toWritableMap(): WritableMap {
  return TextGenerationResult(chunk = this).toWritableMap()
}

fun TextGenerationResult.toWritableMap(): WritableMap {
  return Arguments.createMap().apply {
    putString("chunk", chunk)
  }
}

fun TextGenerationResult.FullText.toWritableMap(): WritableMap {
  return Arguments.createMap().apply {
    putString("fullText", fullText)
  }
}
