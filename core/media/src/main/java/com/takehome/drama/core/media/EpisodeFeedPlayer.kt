package com.takehome.drama.core.media

import kotlinx.coroutines.flow.StateFlow

/**
 * Player cho luồng vuốt dọc kiểu TikTok: tập hiện tại phát, các tập lân cận mỗi tập giữ **một player
 * đã prepare + pause** (đã có khung hình đầu và vài giây buffer) — vuốt nhẹ là thấy video thật, vuốt
 * hẳn là phát ngay. ViewModel giữ và điều khiển nó mà không import Media3, test dùng bản giả. Gọi
 * [release] khi chủ sở hữu bị huỷ.
 *
 * Mục được chỉ định bằng **vị trí trong playlist**, không phải id nghiệp vụ. [state] và các lệnh
 * phát luôn nói về mục hiện tại.
 */
interface EpisodeFeedPlayer {
    val state: StateFlow<PlaybackState>

    /** Vị trí → player đang giữ mục đó (hiện tại + lân cận). UI vẽ surface của slot lên đúng trang. */
    val slots: StateFlow<Map<Int, VideoSlot>>

    /**
     * Thay danh sách mục (lần đầu, hoặc đổi quality/ngôn ngữ làm URL đổi). Không tự đổi mục đang
     * phát: gọi [setCurrent] sau đó để nạp URL mới.
     */
    fun setPlaylist(items: List<FeedItem>)

    /** Phát mục ở [position]. Mục không có URL (tập khoá) thì dừng phát, không nạp gì. */
    fun setCurrent(position: Int, startPositionMs: Long, playWhenReady: Boolean)

    /** Tắt khi màn vào nền: nhả player lân cận (decoder, RAM); bật lại thì chuẩn bị lại từ cache đĩa. */
    fun setPreloadActive(active: Boolean)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun setSpeed(speed: Float)

    /** Vị trí thật lúc gọi — [PlaybackState.positionMs] chỉ cập nhật mỗi nhịp tick. */
    fun currentPositionMs(): Long

    fun release()
}

/** Một player của pool, nhìn từ UI: chỉ cần biết đã có khung hình chưa để thôi hiện poster. */
interface VideoSlot {
    val firstFrameRendered: StateFlow<Boolean>
}

/**
 * [url] null = không phát được (tập khoá): không giữ player, không nạp. Player lân cận luôn đứng sẵn
 * ở **giây 0** — trong feed, vuốt tới tập nào cũng phát từ đầu.
 */
data class FeedItem(val id: String, val url: String?)

data class PlaybackState(
    /** Đang thực sự chạy (đã sẵn sàng, không buffer, không pause). */
    val isPlaying: Boolean = false,
    /** User muốn phát — vẫn `true` khi đang buffer giữa chừng. */
    val playWhenReady: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    /** Lấy từ player: `durationSeconds` của server luôn là 0. 0 = chưa biết. */
    val durationMs: Long = 0,
    val bufferedMs: Long = 0,
    val ended: Boolean = false,
    val error: PlaybackError? = null,
)

data class PlaybackError(val message: String?)

/** ViewModel tạo player qua factory để không phải giữ `Context`. */
fun interface EpisodeFeedPlayerFactory {
    fun create(): EpisodeFeedPlayer
}
