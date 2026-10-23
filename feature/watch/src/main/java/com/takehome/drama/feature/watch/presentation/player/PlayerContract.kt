package com.takehome.drama.feature.watch.presentation.player

import androidx.annotation.StringRes
import com.takehome.drama.core.media.PlaybackState
import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState
import com.takehome.drama.domain.usecase.watch.EndAction
import com.takehome.drama.domain.usecase.watch.EpisodeCell
import com.takehome.drama.domain.usecase.watch.EpisodeRange
import com.takehome.drama.feature.watch.presentation.common.RewardedAdState
import com.takehome.drama.feature.watch.presentation.player.model.PlayerPage
import com.takehome.drama.feature.watch.presentation.player.model.PlayerSheet

data class PlayerViewState(
    val isLoading: Boolean = true,
    /** Lỗi không phát được (catalogue/file tập lỗi, tập không có source). */
    @StringRes val errorRes: Int? = null,
    val showTitle: String = "",
    val posterUrl: String = "",
    /** Trang của pager dọc — một trang mỗi tập, theo thứ tự file tập. */
    val pages: List<PlayerPage> = emptyList(),
    /** `hook` của show (tập không có mô tả riêng), thiếu thì `synopsis`. */
    val description: String = "",
    val descriptionExpanded: Boolean = false,
    val episodeIndex: Int = 0,
    val episodeNumber: Int = 0,
    val episodeTotal: Int = 0,
    val isLocked: Boolean = false,
    /** Nút "Watch an ad to unlock" của trang khoá; null = không có nút (tập free / `rewarded_bonus` tắt). */
    val unlockAd: RewardedAdState? = null,
    /**
     * Cờ phát đổi chậm. `positionMs`/`bufferedMs` luôn 0 ở đây: vị trí tick 500ms đi qua
     * [PlayerViewModel.playbackProgress] để không recompose cả màn.
     */
    val playback: PlaybackState = PlaybackState(),
    val speed: Float = 1f,
    /** Đã cộng lượt like cục bộ. */
    val likeCount: Long = 0,
    val isLiked: Boolean = false,
    val sheet: PlayerSheet = PlayerSheet.None,
    val cells: List<EpisodeCell> = emptyList(),
    val ranges: List<EpisodeRange> = emptyList(),
    /** Tab đang mở trong sheet 0303 — mặc định tab chứa tập đang phát. */
    val selectedRange: EpisodeRange? = null,
    val watchedCount: Int = 0,
    /** Quality có thật trong `sources` của ngôn ngữ đang phát, cao → thấp. */
    val qualities: List<String> = emptyList(),
    /** Null = Auto. */
    val quality: String? = null,
    val languages: List<String> = emptyList(),
    val language: String = "",
    val autoplayNext: Boolean = true,
    /** Auto đã tự hạ xuống quality này vì mạng chậm (SCR-0604). */
    val qualityDropped: String? = null,
    val endAction: EndAction? = null,
) : ViewState {
    val hasPlaybackError: Boolean get() = playback.error != null

    /** Vị trí trang của tập hiện tại trong [pages]. */
    val currentPage: Int get() = pages.indexOfFirst { it.episodeIndex == episodeIndex }.coerceAtLeast(0)
}

sealed interface PlayerIntent : ViewIntent {
    data object BackClick : PlayerIntent
    data object RetryClick : PlayerIntent
    data object TogglePlay : PlayerIntent
    data class SeekTo(val positionMs: Long) : PlayerIntent
    data object LikeClick : PlayerIntent
    data object SpeedClick : PlayerIntent
    data object SpeedLongPress : PlayerIntent
    data object EpisodesClick : PlayerIntent
    data object DismissSheet : PlayerIntent
    data class RangeSelect(val range: EpisodeRange) : PlayerIntent
    data class EpisodeSelect(val index: Int) : PlayerIntent
    data class SpeedSelect(val speed: Float) : PlayerIntent

    /** Null = Auto. */
    data class QualitySelect(val quality: String?) : PlayerIntent
    data class LanguageSelect(val language: String) : PlayerIntent
    data object AutoplayToggle : PlayerIntent
    data object ToggleDescription : PlayerIntent
    data object SwitchBackToHd : PlayerIntent

    /** Nút mở tập bằng rewarded trên trang khoá — chỉ user bấm mới hiện rewarded. */
    data object UnlockWithAdClick : PlayerIntent

    /** Rewarded đã đóng; [earned] = xem hết, nhận thưởng. */
    data class RewardResult(val episodeIndex: Int, val earned: Boolean) : PlayerIntent

    /** Pager dọc đã dừng ở trang của tập [index] (`Episode.index`) — user vuốt. */
    data class PageSettled(val index: Int) : PlayerIntent

    /** Lifecycle `ON_START` / `ON_STOP` của màn. Xoay máy cũng qua STOP/START: bỏ qua để không khựng tiếng. */
    data object ScreenStarted : PlayerIntent
    data class ScreenStopped(val isChangingConfigurations: Boolean = false) : PlayerIntent
}

sealed interface PlayerSingleEvent : SingleEvent {
    data object NavigateBack : PlayerSingleEvent
    data class ShowToast(@StringRes val messageRes: Int, val formatArg: Int?, val success: Boolean) : PlayerSingleEvent

    /** VM đổi tập không qua vuốt (sheet, autoplay) → pager cuộn tới trang của tập [index]. */
    data class ScrollToEpisode(val index: Int) : PlayerSingleEvent

    /** Screen cần Activity để hiện rewarded; kết quả quay lại bằng [PlayerIntent.RewardResult]. */
    data class ShowRewarded(val episodeIndex: Int) : PlayerSingleEvent
}
