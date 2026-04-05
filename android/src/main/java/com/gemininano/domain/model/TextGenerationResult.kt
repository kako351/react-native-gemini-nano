package com.gemininano.domain.model

data class TextGenerationResult(
  val chunk: String,
) {
  data class FullText(
    val fullText: String,
  )
}
