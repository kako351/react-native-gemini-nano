package com.gemininano.domain.service

import com.gemininano.domain.model.Availability
import com.gemininano.domain.model.DownloadState

interface GeminiNanoService {
  suspend fun getAvailability(): Availability

  suspend fun downloadModel(onStatus: (DownloadState) -> Unit)

  suspend fun generateText(prompt: String): String

  suspend fun generateTextStream(
    prompt: String,
    onChunk: (String) -> Unit,
  ): String

  fun close()
}
