package com.takehome.drama.feature.main.presentation.library.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.takehome.drama.core.ui.foundation.format.bidiIsolated
import com.takehome.drama.domain.model.ShowStatus
import com.takehome.drama.domain.usecase.history.HistoryRecency
import com.takehome.drama.feature.main.R
import java.text.DateFormat
import java.util.Date

/** `Ep. 12 / 74 · 4 min left`. */
@Composable
@ReadOnlyComposable
internal fun HistoryRow.meta(): String = stringResource(R.string.history_meta, episodeNumber, episodeTotal, recency.label())

@Composable
@ReadOnlyComposable
internal fun HistoryRecency.label(): String = when (this) {
    is HistoryRecency.MinutesLeft -> pluralStringResource(R.plurals.history_min_left, minutes, minutes)
    HistoryRecency.Today -> stringResource(R.string.history_today)
    HistoryRecency.Yesterday -> stringResource(R.string.history_yesterday)
    is HistoryRecency.DaysAgo -> pluralStringResource(R.plurals.history_days_ago, days, days)
    HistoryRecency.LastWeek -> stringResource(R.string.history_last_week)
    is HistoryRecency.OnDate -> {
        DateFormat.getDateInstance(DateFormat.MEDIUM, LocalLocale.current.platformLocale).format(Date(epochMillis))
    }
}

/** `Romance · 45 eps · Airing` — phần nào thiếu (không genre, status lạ) thì bỏ, không đoán. */
@Composable
@ReadOnlyComposable
internal fun SavedRow.meta(): String = listOfNotNull(
    genre?.bidiIsolated(),
    pluralStringResource(R.plurals.common_episode_count, episodeCount, episodeCount),
    when (status) {
        ShowStatus.Ongoing -> stringResource(R.string.saved_filter_airing)
        ShowStatus.Completed -> stringResource(R.string.saved_filter_complete)
        is ShowStatus.Unknown -> null
    },
).joinToString(" · ")
