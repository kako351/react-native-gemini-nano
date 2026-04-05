package com.gemininano.domain.service

import com.google.mlkit.genai.common.FeatureStatus
import com.gemininano.data.GenerativeModelClient
import com.gemininano.domain.mapper.FeatureStatusAvailabilityMapper
import com.gemininano.domain.model.DownloadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultGeminiNanoServiceTest {
  @Test
  fun `ストリーミング生成は空白チャンクを除外して連結する`() = runTest {
    val service = DefaultGeminiNanoService(
      client = FakeGenerativeModelClient(
        streamedChunks = listOf("hello", "", " ", " world"),
      ),
      availabilityMapper = FeatureStatusAvailabilityMapper(),
    )
    val emittedChunks = mutableListOf<String>()

    val fullText = service.generateTextStream("prompt") { chunk ->
      emittedChunks += chunk
    }

    assertEquals(listOf("hello", " world"), emittedChunks)
    assertEquals("hello world", fullText)
  }

  @Test
  fun `利用可否は feature status から変換される`() = runTest {
    val service = DefaultGeminiNanoService(
      client = FakeGenerativeModelClient(
        featureStatus = FeatureStatus.AVAILABLE,
      ),
      availabilityMapper = FeatureStatusAvailabilityMapper(),
    )

    val availability = service.getAvailability()

    assertTrue(availability.isAvailable)
    assertEquals("available", availability.status)
  }

  @Test
  fun `ダウンロード状態はそのまま購読側へ転送される`() = runTest {
    val expectedStates = listOf(
      DownloadState.Started(totalBytes = 10L),
      DownloadState.InProgress(bytesDownloaded = 5L),
      DownloadState.Completed,
    )
    val service = DefaultGeminiNanoService(
      client = FakeGenerativeModelClient(
        downloadStates = expectedStates,
      ),
      availabilityMapper = FeatureStatusAvailabilityMapper(),
    )
    val actualStates = mutableListOf<DownloadState>()

    service.downloadModel { status ->
      actualStates += status
    }

    assertEquals(expectedStates, actualStates)
  }

  private class FakeGenerativeModelClient(
    private val featureStatus: Int = FeatureStatus.UNAVAILABLE,
    private val downloadStates: List<DownloadState> = emptyList(),
    private val streamedChunks: List<String> = emptyList(),
  ) : GenerativeModelClient {
    override suspend fun getFeatureStatus(): Int = featureStatus

    override fun download(): Flow<DownloadState> = flowOf(*downloadStates.toTypedArray())

    override suspend fun generateText(prompt: String): String = "generated:$prompt"

    override suspend fun generateTextStream(
      prompt: String,
      onChunk: (String) -> Unit,
    ) {
      streamedChunks.forEach(onChunk)
    }

    override fun close() = Unit
  }
}
