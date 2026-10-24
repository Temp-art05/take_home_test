package com.takehome.drama.feature.main.presentation.library.model

import androidx.compose.runtime.Immutable
import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.ShowStatus
import com.takehome.drama.domain.usecase.history.HistoryItem
import com.takehome.drama.domain.usecase.history.HistoryRecency
import com.takehome.drama.feature.main.presentation.common.toCard
import java.util.TimeZone

/** Một dòng History / Recent. Chữ ("Ep. 12 / 74 · 4 min left") format ở Screen — plurals theo locale. */
@Immutable
data class HistoryRow(
    val showId: String,
    val title: String,
    val posterUrl: String,
    val episodeIndex: Int,
    val episodeNumber: Int,
    val episodeTotal: Int,
    /** 0..1 của tập đang dở; show đã xong = 1. */
    val progress: Float,
    val recency: HistoryRecency,
    val finished: Boolean,
)

fun HistoryItem.toRow(
    catalog: Catalog,
    language: String,
    nowMillis: Long,
    timeZone: TimeZone,
    finished: Boolean,
): HistoryRow = HistoryRow(
    showId = show.id,
    title = show.title.resolve(language, catalog.defaultLanguage),
    posterUrl = show.posterUrl,
    episodeIndex = progress.episodeIndex,
    episodeNumber = progress.episodeNumber,
    episodeTotal = progress.episodeTotal,
    progress = when {
        finished -> 1f
        progress.durationMs > 0 -> (progress.positionMs.toFloat() / progress.durationMs).coerceIn(0f, 1f)
        else -> 0f
    },
    recency = HistoryRecency.of(progress, nowMillis, timeZone),
    finished = finished,
)

/** Một dòng Saved: `{genre} · {N eps} · {Airing/Complete}`. */
@Immutable
data class SavedRow(
    val showId: String,
    val title: String,
    val posterUrl: String,
    val genre: String?,
    val episodeCount: Int,
    val status: ShowStatus,
)

fun Show.toSavedRow(catalog: Catalog, language: String): SavedRow =
    toCard(catalog, language).let { SavedRow(it.id, it.title, it.posterUrl, it.genre, it.episodeCount, it.status) }
