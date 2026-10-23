package com.takehome.drama.domain.model

data class ShowEpisodes(
    val showId: String,
    /** Thứ tự phát = thứ tự mảng của server. */
    val episodes: List<Episode>,
)

data class Episode(
    /**
     * Vị trí gốc trong mảng của server — định danh của tập (khoá watch progress). Phần tử hỏng bị
     * bỏ nên dãy có thể không liên tục: đừng dùng làm vị trí trong list. Không dùng [number] vì
     * nó có thể nhảy cóc hoặc không bắt đầu từ 1.
     */
    val index: Int,
    /** Số tập server ghi, chỉ để hiển thị nhãn; thiếu thì dùng [index] + 1. */
    val number: Int?,
    /** Gần như luôn rỗng (cả 299 tập trong fixtures) — UI hiển thị số tập, không hiển thị tên. */
    val title: LocalizedText,
    val isFree: Boolean,
    val likeCount: Long,
    val durationSeconds: Int,
    /** ngôn ngữ → nhãn chất lượng → URL. Đã bỏ rendition hỏng. */
    val sources: Map<String, Map<String, String>>,
) {
    val displayNumber: Int get() = number ?: (index + 1)
}

data class VideoSource(
    val language: String,
    val quality: String,
    val url: String,
)

object Quality {
    /** `"720p"` → 720. Key không có dạng số (`"abc"`) → null, xếp sau mọi key số. */
    fun heightOf(label: String): Int? =
        label.trim().lowercase().removeSuffix("p").toIntOrNull()?.takeIf { it > 0 }

    /** Cao → thấp, key lạ cuối cùng (giữ thứ tự gốc giữa chúng). */
    val HighestFirst: Comparator<String> = compareByDescending<String> { heightOf(it) ?: Int.MIN_VALUE }
}
