package com.gemininano.data

import com.gemininano.domain.model.DownloadState
import kotlinx.coroutines.flow.Flow

interface GenerativeModelClient {
  suspend fun getFeatureStatus(): Int

  fun download(): Flow<DownloadState>

  suspend fun generateText(prompt: String): String

  suspend fun generateTextStream(
    prompt: String,
    onChunk: (String) -> Unit,
  )

  fun close()
}
