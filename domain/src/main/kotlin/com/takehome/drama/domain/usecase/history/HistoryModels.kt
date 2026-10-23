package com.takehome.drama.domain.usecase.history

import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.WatchProgress

/** Tập sẽ mở khi bấm "Continue Ep. N" / "Play Ep 1". */
data class ContinueTarget(
    val episodeIndex: Int,
    val positionMs: Long,
    val hasProgress: Boolean,
)

data class HistoryItem(val show: Show, val progress: WatchProgress)

data class WatchHistory(
    val watching: List<HistoryItem>,
    val finished: List<HistoryItem>,
)
