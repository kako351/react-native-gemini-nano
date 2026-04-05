package com.gemininano.domain.mapper

import com.google.mlkit.genai.common.GenAiException
import com.gemininano.domain.model.GeminiNanoError

class ThrowableErrorMapper {
  fun map(error: Throwable): GeminiNanoError {
    return when (error) {
      is GenAiException -> mapGenAiError(error)
      else -> GeminiNanoError(
        code = "UNKNOWN",
        message = error.message ?: "不明なエラーが発生しました。",
      )
    }
  }

  private fun mapGenAiError(error: GenAiException): GeminiNanoError {
    return when (error.errorCode) {
      GenAiException.ErrorCode.NOT_AVAILABLE,
      GenAiException.ErrorCode.AICORE_INCOMPATIBLE -> GeminiNanoError(
        code = "NOT_AVAILABLE",
        message = "この端末では現在 Gemini Nano を利用できません。",
      )
      GenAiException.ErrorCode.NEEDS_SYSTEM_UPDATE -> GeminiNanoError(
        code = "NEEDS_SYSTEM_UPDATE",
        message = "システムまたは AICore の更新が必要です。",
      )
      GenAiException.ErrorCode.NOT_ENOUGH_DISK_SPACE -> GeminiNanoError(
        code = "NOT_ENOUGH_DISK_SPACE",
        message = "Gemini Nano のダウンロードに必要な空き容量が不足しています。",
      )
      GenAiException.ErrorCode.BUSY -> GeminiNanoError(
        code = "BUSY",
        message = "Gemini Nano がビジー状態です。少し待って再試行してください。",
      )
      else -> GeminiNanoError(
        code = "GENAI_ERROR",
        message = error.message ?: "Gemini Nano でエラーが発生しました。",
      )
    }
  }
}
