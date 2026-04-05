package com.gemininano.domain.mapper

import com.google.mlkit.genai.common.FeatureStatus
import com.gemininano.domain.model.Availability

class FeatureStatusAvailabilityMapper {
  fun map(@FeatureStatus status: Int): Availability {
    return when (status) {
      FeatureStatus.AVAILABLE -> Availability(
        status = "available",
        isAvailable = true,
        message = "Gemini Nano を利用できます。",
        errorCode = null,
      )
      FeatureStatus.DOWNLOADABLE -> Availability(
        status = "needs_download",
        isAvailable = false,
        message = "Gemini Nano のモデルをダウンロードできます。ダウンロード後に利用可能になります。",
        errorCode = "DOWNLOADABLE",
      )
      FeatureStatus.DOWNLOADING -> Availability(
        status = "downloading",
        isAvailable = false,
        message = "Gemini Nano のモデルをダウンロード中です。完了後に再試行してください。",
        errorCode = "DOWNLOADING",
      )
      FeatureStatus.UNAVAILABLE -> Availability(
        status = "unavailable",
        isAvailable = false,
        message = "この端末では現在 Gemini Nano を利用できません。",
        errorCode = "UNAVAILABLE",
      )
      else -> Availability(
        status = "unknown",
        isAvailable = false,
        message = "Gemini Nano の利用状態を判定できませんでした。",
        errorCode = "UNKNOWN",
      )
    }
  }
}
