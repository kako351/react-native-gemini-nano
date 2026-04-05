package com.gemininano.domain.model

data class Availability(
  val status: String,
  val isAvailable: Boolean,
  val message: String,
  val errorCode: String?,
)
