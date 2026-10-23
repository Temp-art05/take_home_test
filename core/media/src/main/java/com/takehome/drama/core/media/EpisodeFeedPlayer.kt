package com.takehome.drama.core.media

import kotlinx.coroutines.flow.StateFlow

/**
 * Player cho luồng vuốt dọc kiểu TikTok: **một** player phát mục hiện tại, các mục lân cận được
 * preload sẵn vài giây để vuốt tới là lên hình ngay. ViewModel giữ và điều khiển nó mà không import
 * Media3, test dùng bản giả. Gọi [release] khi chủ sở hữu bị huỷ.
 *
 * Mục được chỉ định bằng **vị trí trong playlist**, không phải id nghiệp vụ.
 */
interface EpisodeFeedPlayer {
    val state: StateFlow<PlaybackState>

    /** Mục hiện tại đã vẽ khung hình đầu — trước đó UI hiện poster thay cho màn đen. */
    val firstFrameRendered: StateFlow<Boolean>

    /**
     * Thay danh sách mục (lần đầu, hoặc khi đổi quality/ngôn ngữ làm URL đổi). Không tự đổi mục
     * đang phát: gọi [setCurrent] sau đó để nạp URL mới.
     */
    fun setPlaylist(items: List<FeedItem>)

    /** Phát mục ở [position]. Mục không có URL (tập khoá) thì dừng phát, không nạp gì. */
    fun setCurrent(position: Int, startPositionMs: Long, playWhenReady: Boolean)

    /** Tắt khi màn vào nền: bỏ hết preload (nhả RAM, không tốn data); bật lại thì preload tiếp. */
    fun setPreloadActive(active: Boolean)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun setSpeed(speed: Float)

    /** Vị trí thật lúc gọi — [PlaybackState.positionMs] chỉ cập nhật mỗi nhịp tick. */
    fun currentPositionMs(): Long

    fun release()
}

/** [url] null = không phát được (tập khoá): không preload, không nạp. */
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
