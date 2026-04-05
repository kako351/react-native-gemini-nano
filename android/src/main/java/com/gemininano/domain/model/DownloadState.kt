package com.gemininano.domain.model

sealed interface DownloadState {
  data class Started(
    val totalBytes: Long,
  ) : DownloadState

  data class InProgress(
    val bytesDownloaded: Long,
  ) : DownloadState

  data object Completed : DownloadState

  data class Failed(
    val cause: Throwable,
  ) : DownloadState
}
