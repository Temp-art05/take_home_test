package com.takehome.drama.domain.usecase.watch

import com.takehome.drama.domain.model.Quality

/**
 * Hạ chất lượng khi mạng chậm. Chỉ hạ xuống quality **có thật** trong `sources` — fixtures chỉ có
 * `720p` nên trên dữ liệu thật policy không bao giờ kích hoạt, thay vì bịa một bản 480p.
 */
object PlaybackQualityPolicy {

    const val STALL_THRESHOLD_MS = 5_000L

    /** Chỉ khi user để Auto: user đã chọn tay một quality thì app không tự đổi ý họ. */
    fun shouldDowngrade(isAuto: Boolean, bufferingMs: Long): Boolean = isAuto && bufferingMs > STALL_THRESHOLD_MS

    /** Quality số thấp hơn gần nhất. Key lạ (`"abc"`) không biết cao thấp nên không bao giờ là đích. */
    fun downgradeTarget(current: String, available: List<String>): String? {
        val currentHeight = Quality.heightOf(current) ?: return null
        return available
            .mapNotNull { label -> Quality.heightOf(label)?.let { label to it } }
            .filter { (_, height) -> height < currentHeight }
            .maxByOrNull { (_, height) -> height }
            ?.first
    }
}
