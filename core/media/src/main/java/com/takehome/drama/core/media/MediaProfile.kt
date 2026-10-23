package com.takehome.drama.core.media

/**
 * Ngân sách media theo [DeviceTier]. Số khởi điểm theo blog Media3 PreloadManager, chưa A/B.
 * Preload tốn RAM buffer + băng thông, không tốn decoder — nên máy yếu chỉ cắt bớt, không tắt hẳn.
 */
data class MediaProfile(
    val tier: DeviceTier,
    /** Khoảng cách tới tập hiện tại (âm = phía trước) → thời lượng preload. */
    val preloadRangesMs: Map<Int, Long>,
    /** Khoảng cách chỉ chuẩn bị tới bước chọn track (rẻ, chưa tải media). */
    val tracksSelectedDistances: Set<Int>,
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    /** Trần byte buffer; null = mặc định của ExoPlayer. */
    val targetBufferBytes: Int?,
    /** Trần độ phân giải track (video dọc: rộng × cao); null = không giới hạn. */
    val maxVideoSize: Pair<Int, Int>?,
    val cacheBytes: Long,
) {
    /** Mọi khoảng cách có preload — cửa sổ [preloadWindow] lấy từ đây. */
    val windowDistances: Set<Int> get() = preloadRangesMs.keys + tracksSelectedDistances

    companion object {
        fun forTier(tier: DeviceTier): MediaProfile = when (tier) {
            DeviceTier.NORMAL -> MediaProfile(
                tier = tier,
                preloadRangesMs = mapOf(1 to 5_000L, -1 to 2_000L),
                tracksSelectedDistances = setOf(2),
                minBufferMs = 10_000,
                maxBufferMs = 30_000,
                bufferForPlaybackMs = 750,
                targetBufferBytes = null,
                maxVideoSize = null,
                cacheBytes = 256L * 1024 * 1024,
            )
            DeviceTier.LOW -> MediaProfile(
                tier = tier,
                preloadRangesMs = mapOf(1 to 2_500L),
                tracksSelectedDistances = emptySet(),
                minBufferMs = 6_000,
                maxBufferMs = 15_000,
                bufferForPlaybackMs = 500,
                targetBufferBytes = 16 * 1024 * 1024,
                maxVideoSize = 720 to 1280,
                cacheBytes = 96L * 1024 * 1024,
            )
        }
    }
}
