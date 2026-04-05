package com.gemininano

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.module.annotations.ReactModule
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.gemininano.bridge.GeminiNanoEventNames
import com.gemininano.bridge.ReactNativeEventEmitter
import com.gemininano.bridge.toWritableMap
import com.gemininano.data.MlKitGenerativeModelClient
import com.gemininano.domain.mapper.FeatureStatusAvailabilityMapper
import com.gemininano.domain.mapper.ThrowableErrorMapper
import com.gemininano.domain.model.TextGenerationResult
import com.gemininano.domain.service.DefaultGeminiNanoService
import com.gemininano.domain.service.GeminiNanoService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@ReactModule(name = GeminiNanoModule.NAME)
class GeminiNanoModule(
  reactContext: ReactApplicationContext,
  private val service: GeminiNanoService = DefaultGeminiNanoService(
    client = MlKitGenerativeModelClient(),
    availabilityMapper = FeatureStatusAvailabilityMapper(),
  ),
  private val errorMapper: ThrowableErrorMapper = ThrowableErrorMapper(),
) : NativeGeminiNanoSpec(reactContext) {

  private val moduleScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val eventEmitter = ReactNativeEventEmitter(reactContext)
  private var streamJob: Job? = null

  override fun getName(): String = NAME

  @ReactMethod
  override fun isAvailable(promise: Promise) {
    toPromise(promise) {
      service.getAvailability().isAvailable
    }
  }

  @ReactMethod
  override fun getAvailability(promise: Promise) {
    toPromise(promise) {
      service.getAvailability().toWritableMap()
    }
  }

  @ReactMethod
  override fun downloadModel(promise: Promise) {
    toPromise(promise) {
      service.downloadModel { status ->
        eventEmitter.emit(
          GeminiNanoEventNames.DOWNLOAD,
          status.toWritableMap(errorMapper),
        )
      }
    }
  }

  @ReactMethod
  override fun generateText(prompt: String, promise: Promise) {
    toPromise(promise) {
      service.generateText(prompt)
    }
  }

  @ReactMethod
  override fun generateTextStream(prompt: String) {
    streamJob?.cancel()
    streamJob = moduleScope.launch {
      try {
        val fullText = service.generateTextStream(prompt) { chunk ->
          eventEmitter.emit(
            GeminiNanoEventNames.STREAM_CHUNK,
            TextGenerationResult(chunk).toWritableMap(),
          )
        }

        eventEmitter.emit(
          GeminiNanoEventNames.STREAM_END,
          TextGenerationResult.FullText(fullText).toWritableMap(),
        )
      } catch (error: Throwable) {
        val normalized = errorMapper.map(error)
        eventEmitter.emit(
          GeminiNanoEventNames.STREAM_ERROR,
          normalized.toWritableMap(),
        )
      }
    }
  }

  override fun invalidate() {
    streamJob?.cancel()
    moduleScope.cancel()
    service.close()
    super.invalidate()
  }

  private fun <T> toPromise(
    promise: Promise,
    block: suspend () -> T,
  ) {
    runAsTask(block)
      .addOnSuccessListener { result ->
        promise.resolve(result)
      }
      .addOnFailureListener { error ->
        val normalized = errorMapper.map(error)
        promise.reject(normalized.code, normalized.message, error)
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

  companion object {
    const val NAME = "GeminiNano"
  }
}
