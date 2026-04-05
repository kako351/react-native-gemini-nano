package com.gemininano.data

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.StreamingCallback
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerateContentResponse
import com.google.mlkit.genai.prompt.GenerativeModel
import com.gemininano.domain.model.DownloadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MlKitGenerativeModelClient : GenerativeModelClient {
  private val model: GenerativeModel by lazy {
    Generation.getClient()
  }

  override suspend fun getFeatureStatus(): Int = model.checkStatus()

  override fun download(): Flow<DownloadState> {
    return model.download().map { status ->
      when (status) {
        is DownloadStatus.DownloadStarted -> DownloadState.Started(
          totalBytes = status.bytesToDownload,
        )
        is DownloadStatus.DownloadProgress -> DownloadState.InProgress(
          bytesDownloaded = status.totalBytesDownloaded,
        )
        DownloadStatus.DownloadCompleted -> DownloadState.Completed
        is DownloadStatus.DownloadFailed -> DownloadState.Failed(status.e)
      }
    }
  }

  override suspend fun generateText(prompt: String): String {
    return extractText(model.generateContent(prompt))
  }

  override suspend fun generateTextStream(
    prompt: String,
    onChunk: (String) -> Unit,
  ) {
    model.generateContent(
      prompt,
      object : StreamingCallback {
        override fun onNewText(text: String) {
          onChunk(text)
        }
      },
    )
  }

  override fun close() {
    model.close()
  }

  private fun extractText(response: GenerateContentResponse): String {
    return response.candidates.joinToString(separator = "") { candidate ->
      candidate.text ?: ""
    }
  }
}
