package com.takehome.drama.domain.model

data class Catalog(
    val version: Int,
    val defaultLanguage: String,
    val availableLanguages: List<String>,
    val tags: List<Tag>,
    /** Có thể trỏ tới show không nằm đầu [shows], hoặc tới show không tồn tại. */
    val heroShowId: String?,
    val shows: List<Show>,
)

data class Tag(
    val id: String,
    val name: LocalizedText,
    /** `false` = rail merchandising ở Home, `true` = genre cho chip Browse. */
    val isCategory: Boolean,
    /** Số nhỏ đứng trước. */
    val orderPriority: Int,
)

data class Show(
    val id: String,
    val title: LocalizedText,
    val synopsis: LocalizedText,
    val hook: LocalizedText,
    val posterUrl: String,
    /**
     * Số tập theo catalogue — có thể lệch file tập (`13077` ghi 65, file có 50). Chỉ dùng ở chỗ
     * chưa tải danh sách tập (card); lưới tập tin [ShowEpisodes.episodes].
     */
    val catalogEpisodeCount: Int,
    val status: ShowStatus,
    val viewCount: Long,
    val tagIds: List<String>,
)
