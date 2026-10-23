package com.takehome.drama.data.mapper

import com.takehome.drama.data.local.entity.SavedShowEntity
import com.takehome.drama.data.local.entity.WatchProgressEntity
import com.takehome.drama.domain.model.SavedShow
import com.takehome.drama.domain.model.WatchProgress

fun WatchProgressEntity.toDomain(): WatchProgress = WatchProgress(
    showId = showId,
    episodeIndex = episodeIndex,
    episodeNumber = episodeNumber,
    episodeTotal = episodeTotal,
    positionMs = positionMs,
    durationMs = durationMs,
    updatedAt = updatedAt,
)

fun WatchProgress.toEntity(): WatchProgressEntity = WatchProgressEntity(
    showId = showId,
    episodeIndex = episodeIndex,
    episodeNumber = episodeNumber,
    episodeTotal = episodeTotal,
    positionMs = positionMs,
    durationMs = durationMs,
    updatedAt = updatedAt,
)

fun SavedShowEntity.toDomain(): SavedShow = SavedShow(showId = showId, savedAt = savedAt)
