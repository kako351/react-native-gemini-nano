package com.gemininano

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.common.StreamingCallback
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerateContentResponse
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ReactModule(name = GeminiNanoModule.NAME)
class GeminiNanoModule(
  reactContext: ReactApplicationContext,
) : NativeGeminiNanoSpec(reactContext) {

  private val moduleScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var streamJob: Job? = null

  private val model: GenerativeModel by lazy {
    Generation.getClient()
  }

  override fun getName(): String = NAME

  @ReactMethod
  override fun isAvailable(promise: Promise) {
    toPromise(promise) {
      val status = model.checkStatus()
      status == FeatureStatus.AVAILABLE
    }
  }

  @ReactMethod
  override fun getAvailability(promise: Promise) {
    toPromise(promise) {
      createAvailabilityInfo(model.checkStatus()).toWritableMap()
    }
  }

  @ReactMethod
  override fun downloadModel(promise: Promise) {
    toPromise(promise) {
      model.download().collect { status ->
        emitDownloadStatus(status)
      }
    }
  }

  @ReactMethod
  override fun generateText(prompt: String, promise: Promise) {
    toPromise(promise) {
      val response = model.generateContent(prompt)
      extractText(response)
    }
  }

  @ReactMethod
  override fun generateTextStream(prompt: String) {
    streamJob?.cancel()
    streamJob = moduleScope.launch {
      val fullText = StringBuilder()

      try {
        model.generateContent(
          prompt,
          object : StreamingCallback {
            override fun onNewText(text: String) {
              val chunk = text
              if (chunk.isBlank()) {
                return
              }

              fullText.append(chunk)
              emitEvent(STREAM_CHUNK_EVENT) {
                putString("chunk", chunk)
              }
            }
          },
        )

        emitEvent(STREAM_END_EVENT) {
          putString("fullText", fullText.toString())
        }
      } catch (error: Throwable) {
        val normalized = normalizeError(error)
        emitEvent(STREAM_ERROR_EVENT) {
          putString("code", normalized.first)
          putString("message", normalized.second)
        }
      }
    }
  }

  override fun invalidate() {
    streamJob?.cancel()
    moduleScope.cancel()
    model.close()
    super.invalidate()
  }

  private fun <T> toPromise(
    promise: Promise,
    block: suspend () -> T,
  ) {
    val task = runAsTask(block)
    task
      .addOnSuccessListener { result ->
        promise.resolve(result)
      }
      .addOnFailureListener { error ->
        val normalized = normalizeError(error)
        val exception = if (error is Exception) error else Exception(error)
        promise.reject(normalized.first, normalized.second, exception)
      }
  }

  private fun <T> runAsTask(block: suspend () -> T): Task<T> {
    val taskSource = TaskCompletionSource<T>()

    moduleScope.launch {
      try {
        taskSource.setResult(block())
      } catch (error: Throwable) {
        val exception = if (error is Exception) error else Exception(error)
        taskSource.setException(exception)
      }
    }

    return taskSource.task
  }

  private fun createAvailabilityInfo(@FeatureStatus status: Int): AvailabilityInfo {
    return when (status) {
      FeatureStatus.AVAILABLE -> {
        AvailabilityInfo(
          status = "available",
          isAvailable = true,
          message = "Gemini Nano を利用できます。",
          errorCode = null,
        )
      }
      FeatureStatus.DOWNLOADABLE -> {
        AvailabilityInfo(
          status = "needs_download",
          isAvailable = false,
          message = "Gemini Nano のモデルをダウンロードできます。ダウンロード後に利用可能になります。",
          errorCode = "DOWNLOADABLE",
        )
      }
      FeatureStatus.DOWNLOADING -> {
        AvailabilityInfo(
          status = "downloading",
          isAvailable = false,
          message = "Gemini Nano のモデルをダウンロード中です。完了後に再試行してください。",
          errorCode = "DOWNLOADING",
        )
      }
      FeatureStatus.UNAVAILABLE -> {
        AvailabilityInfo(
          status = "unavailable",
          isAvailable = false,
          message = "この端末では現在 Gemini Nano を利用できません。",
          errorCode = "UNAVAILABLE",
        )
      }
      else -> {
        AvailabilityInfo(
          status = "unknown",
          isAvailable = false,
          message = "Gemini Nano の利用状態を判定できませんでした。",
          errorCode = "UNKNOWN",
        )
      }
    }
  }

  private fun emitDownloadStatus(status: DownloadStatus) {
    emitEvent(DOWNLOAD_EVENT) {
      when (status) {
        is DownloadStatus.DownloadStarted -> {
          putString("status", "started")
          putDouble("totalBytes", status.bytesToDownload.toDouble())
        }
        is DownloadStatus.DownloadProgress -> {
          putString("status", "in_progress")
          putDouble("bytesDownloaded", status.totalBytesDownloaded.toDouble())
        }
        DownloadStatus.DownloadCompleted -> {
          putString("status", "completed")
        }
        is DownloadStatus.DownloadFailed -> {
          putString("status", "failed")
          val normalized = normalizeError(status.e)
          putString("code", normalized.first)
          putString("message", normalized.second)
        }
      }
    }
  }

  private fun extractText(response: GenerateContentResponse): String {
    return response.candidates.joinToString(separator = "") { candidate ->
      candidate.text ?: ""
    }
  }

  private fun emitEvent(
    eventName: String,
    payloadBuilder: WritableMap.() -> Unit,
  ) {
    if (!reactApplicationContext.hasActiveReactInstance()) {
      return
    }

    val payload = Arguments.createMap().apply(payloadBuilder)
    reactApplicationContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, payload)
  }

  private fun normalizeError(error: Throwable): Pair<String, String> {
    return when (error) {
      is GenAiException -> {
        when (error.errorCode) {
          GenAiException.ErrorCode.NOT_AVAILABLE,
          GenAiException.ErrorCode.AICORE_INCOMPATIBLE -> {
            "NOT_AVAILABLE" to "この端末では現在 Gemini Nano を利用できません。"
          }
          GenAiException.ErrorCode.NEEDS_SYSTEM_UPDATE -> {
            "NEEDS_SYSTEM_UPDATE" to "システムまたは AICore の更新が必要です。"
          }
          GenAiException.ErrorCode.NOT_ENOUGH_DISK_SPACE -> {
            "NOT_ENOUGH_DISK_SPACE" to "Gemini Nano のダウンロードに必要な空き容量が不足しています。"
          }
          GenAiException.ErrorCode.BUSY -> {
            "BUSY" to "Gemini Nano がビジー状態です。少し待って再試行してください。"
          }
          else -> {
            "GENAI_ERROR" to (error.message ?: "Gemini Nano でエラーが発生しました。")
          }
        }
      }
      else -> {
        "UNKNOWN" to (error.message ?: "不明なエラーが発生しました。")
      }
    }
  }

  private data class AvailabilityInfo(
    val status: String,
    val isAvailable: Boolean,
    val message: String,
    val errorCode: String?,
  ) {
    fun toWritableMap(): WritableMap {
      return Arguments.createMap().apply {
        putString("status", status)
        putBoolean("isAvailable", isAvailable)
        putString("message", message)
        if (errorCode != null) {
          putString("errorCode", errorCode)
        }
      }
    }
  }

  companion object {
    const val NAME = "GeminiNano"

    const val STREAM_CHUNK_EVENT = "GeminiNano:onTextChunk"
    const val STREAM_END_EVENT = "GeminiNano:onTextStreamEnd"
    const val STREAM_ERROR_EVENT = "GeminiNano:onTextStreamError"
    const val DOWNLOAD_EVENT = "GeminiNano:onDownloadStatus"
  }
}
