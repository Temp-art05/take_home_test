package com.takehome.drama.feature.main.presentation.home

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState
import com.takehome.drama.domain.usecase.discovery.ClockTime
import com.takehome.drama.domain.usecase.discovery.Daypart
import com.takehome.drama.feature.main.presentation.common.ShowCard

/** Một thẻ "Pick up your flow": tập đang dở + tiến độ trong tập. */
@Immutable
data class ContinueCard(
    val show: ShowCard,
    val episodeIndex: Int,
    val episodeNumber: Int,
    /** 0..1 trong tập đang dở. */
    val progress: Float,
)

@Immutable
data class HomeRail(val title: String, val shows: List<ShowCard>)

data class HomeViewState(
    val isLoading: Boolean = true,
    @StringRes val errorRes: Int? = null,
    val daypart: Daypart = Daypart.Night,
    val clock: ClockTime = ClockTime(0, 0),
    /** Hai khung giờ còn lại theo thứ tự sắp tới (2 thẻ nhỏ). */
    val upcoming: List<Daypart> = emptyList(),
    val hero: ShowCard? = null,
    val continueRow: List<ContinueCard> = emptyList(),
    /** Rail theo tag `isCategory = false` — fixtures không show nào gắn nên thường rỗng. */
    val rails: List<HomeRail> = emptyList(),
    val alsoFlowing: List<ShowCard> = emptyList(),
) : ViewState

sealed interface HomeIntent : ViewIntent {
    /** `ON_START`: tính lại khung giờ (mở app lại sau vài tiếng). */
    data object ScreenStarted : HomeIntent
    data object Retry : HomeIntent
    data class OpenShow(val showId: String) : HomeIntent
    data class ContinueShow(val showId: String, val episodeIndex: Int) : HomeIntent
    data object OpenSearch : HomeIntent
    data object SeeAll : HomeIntent

    /** Inbox đã bị design gỡ — chuông dẫn tới Settings (bật/tắt thông báo). */
    data object OpenBell : HomeIntent
}

sealed interface HomeSingleEvent : SingleEvent {
    data class OpenShow(val showId: String) : HomeSingleEvent
    data class OpenPlayer(val showId: String, val episodeIndex: Int) : HomeSingleEvent
    data object OpenSearch : HomeSingleEvent
    data object OpenBrowse : HomeSingleEvent
    data object OpenSettings : HomeSingleEvent
}
