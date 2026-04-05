package com.gemininano

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
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

  override fun isAvailable(promise: Promise) {
    moduleScope.launch {
      try {
        promise.resolve(service.getAvailability().isAvailable)
      } catch (error: Throwable) {
        rejectPromise(promise, error)
      }
    }
  }

  override fun getAvailability(promise: Promise) {
    moduleScope.launch {
      try {
        promise.resolve(service.getAvailability().toWritableMap())
      } catch (error: Throwable) {
        rejectPromise(promise, error)
      }
    }
  }

  override fun downloadModel(promise: Promise) {
    moduleScope.launch {
      try {
        service.downloadModel { status ->
          eventEmitter.emit(
            GeminiNanoEventNames.DOWNLOAD,
            status.toWritableMap(errorMapper),
          )
        }
        promise.resolve(null)
      } catch (error: Throwable) {
        rejectPromise(promise, error)
      }
    }
  }

  override fun generateText(prompt: String, promise: Promise) {
    moduleScope.launch {
      try {
        promise.resolve(service.generateText(prompt))
      } catch (error: Throwable) {
        rejectPromise(promise, error)
      }
    }
  }

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

  private fun rejectPromise(
    promise: Promise,
    error: Throwable,
  ) {
    val normalized = errorMapper.map(error)
    promise.reject(normalized.code, normalized.message, error)
  }

  companion object {
    const val NAME = "GeminiNano"
  }
}
