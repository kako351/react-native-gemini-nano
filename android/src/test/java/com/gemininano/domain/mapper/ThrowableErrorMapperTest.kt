package com.gemininano.domain.mapper

import org.junit.Assert.assertEquals
import org.junit.Test

class ThrowableErrorMapperTest {
  private val mapper = ThrowableErrorMapper()

  @Test
  fun `通常の例外は UNKNOWN として変換される`() {
    val result = mapper.map(IllegalStateException("boom"))

    assertEquals("UNKNOWN", result.code)
    assertEquals("boom", result.message)
  }

  @Test
  fun `メッセージ無し例外は既定文言へフォールバックする`() {
    val result = mapper.map(RuntimeException())

    assertEquals("UNKNOWN", result.code)
    assertEquals("不明なエラーが発生しました。", result.message)
  }
}
