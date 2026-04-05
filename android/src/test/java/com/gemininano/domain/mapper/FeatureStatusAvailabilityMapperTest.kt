package com.gemininano.domain.mapper

import com.google.mlkit.genai.common.FeatureStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureStatusAvailabilityMapperTest {
  private val mapper = FeatureStatusAvailabilityMapper()

  @Test
  fun `AVAILABLE は利用可能として変換される`() {
    val result = mapper.map(FeatureStatus.AVAILABLE)

    assertEquals("available", result.status)
    assertTrue(result.isAvailable)
    assertNull(result.errorCode)
  }

  @Test
  fun `DOWNLOADABLE はダウンロード待ちとして変換される`() {
    val result = mapper.map(FeatureStatus.DOWNLOADABLE)

    assertEquals("needs_download", result.status)
    assertFalse(result.isAvailable)
    assertEquals("DOWNLOADABLE", result.errorCode)
  }

  @Test
  fun `未知の値は unknown にフォールバックする`() {
    val result = mapper.map(-1)

    assertEquals("unknown", result.status)
    assertFalse(result.isAvailable)
    assertEquals("UNKNOWN", result.errorCode)
  }
}
