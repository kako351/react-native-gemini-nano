package com.gemininano.domain.service

import com.gemininano.data.GenerativeModelClient
import com.gemininano.domain.mapper.FeatureStatusAvailabilityMapper
import com.gemininano.domain.model.Availability
import com.gemininano.domain.model.DownloadState
import kotlinx.coroutines.flow.collect

class DefaultGeminiNanoService(
  private val client: GenerativeModelClient,
  private val availabilityMapper: FeatureStatusAvailabilityMapper,
) : GeminiNanoService {
  override suspend fun getAvailability(): Availability {
    return availabilityMapper.map(client.getFeatureStatus())
  }

  override suspend fun downloadModel(onStatus: (DownloadState) -> Unit) {
    client.download().collect(onStatus)
  }

  override suspend fun generateText(prompt: String): String {
    return client.generateText(prompt)
  }

  override suspend fun generateTextStream(
    prompt: String,
    onChunk: (String) -> Unit,
  ): String {
    val fullText = StringBuilder()

    client.generateTextStream(prompt) { chunk ->
      if (chunk.isBlank()) {
        return@generateTextStream
      }

      fullText.append(chunk)
      onChunk(chunk)
    }

    return fullText.toString()
  }

  override fun close() {
    client.close()
  }
}
