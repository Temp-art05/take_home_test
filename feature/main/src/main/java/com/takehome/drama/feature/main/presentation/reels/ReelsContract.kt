package com.takehome.drama.feature.main.presentation.reels

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.takehome.drama.core.media.PlaybackState
import com.takehome.drama.core.ui.foundation.error.UiError
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState

/** Trạng thái nạp tập của một reel (tập/URL nạp lười quanh trang đang xem). */
enum class ClipStatus { Loading, Ready, Failed, Unavailable }

/** Một trang Reels = một show, phát tập free đầu tiên. Đủ thông tin để overlay trượt cùng trang. */
@Immutable
data class ReelPage(
    val showId: String,
    val title: String,
    /** `hook` của show, thiếu thì `synopsis`. */
    val description: String,
    val posterUrl: String,
    val status: ClipStatus = ClipStatus.Loading,
    val episodeIndex: Int? = null,
    val episodeNumber: Int? = null,
    /** Số tập của file tập — "Watch all N episodes". */
    val episodeTotal: Int? = null,
    /** Đã cộng like cục bộ. */
    val likeCount: Long = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
)

data class ReelsViewState(
    val isLoading: Boolean = true,
    val error: UiError? = null,
    val pages: List<ReelPage> = emptyList(),
    /** Vị trí trang đang phát — VM là nguồn sự thật, pager chỉ báo `PageSettled`. */
    val current: Int = 0,
    val speed: Float = 1f,
    val descriptionExpanded: Boolean = false,
    /** Cờ phát đổi chậm; tiến độ tick đi qua `ReelsViewModel.playbackProgress`. */
    val playback: PlaybackState = PlaybackState(),
) : ViewState

sealed interface ReelsIntent : ViewIntent {
    data class PageSettled(val page: Int) : ReelsIntent
    data object TogglePlay : ReelsIntent
    data object CycleSpeed : ReelsIntent
    data object ToggleLike : ReelsIntent
    data object ToggleSave : ReelsIntent
    data object ToggleDescription : ReelsIntent
    data object WatchAll : ReelsIntent
    data object Retry : ReelsIntent

    /** Thử lại reel hiện tại (lỗi tải file tập hoặc lỗi phát). */
    data object RetryClip : ReelsIntent

    /** Tab ẩn vẫn RESUMED (pager giữ tab) → gate phát theo tab đang chọn. */
    data class TabVisible(val visible: Boolean) : ReelsIntent
    data object ScreenStarted : ReelsIntent

    /** Xoay máy cũng qua STOP/START: bỏ qua để không khựng tiếng. */
    data class ScreenStopped(val isChangingConfigurations: Boolean = false) : ReelsIntent
}

sealed interface ReelsSingleEvent : SingleEvent {
    /** Reel hết → tự cuộn sang reel kế (như Player tự sang tập). */
    data class ScrollToPage(val page: Int) : ReelsSingleEvent
    data class OpenPlayer(val showId: String, val episodeIndex: Int) : ReelsSingleEvent
    data class ShowToast(@StringRes val messageRes: Int, val success: Boolean) : ReelsSingleEvent
}
