package com.takehome.drama.data.remote.dto

import com.takehome.drama.data.remote.dto.serializer.LocalizedMapSerializer
import com.takehome.drama.data.remote.dto.serializer.SkipInvalidListSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET catalog.json`. Không có envelope [BaseResponse] — payload là object trần. */
@Serializable
data class CatalogDto(
    @SerialName("version") val version: Int = 0,
    @SerialName("defaultLanguage") val defaultLanguage: String = DEFAULT_LANGUAGE,
    @SerialName("availableLanguages") val availableLanguages: List<String> = emptyList(),
    @Serializable(with = SkipInvalidListSerializer::class)
    @SerialName("tags") val tags: List<TagDto> = emptyList(),
    @SerialName("home") val home: HomeDto? = null,
    @Serializable(with = SkipInvalidListSerializer::class)
    @SerialName("shows") val shows: List<ShowDto> = emptyList(),
) {
    companion object {
        const val DEFAULT_LANGUAGE = "en"
    }
}

@Serializable
data class TagDto(
    @SerialName("id") val id: String,
    @Serializable(with = LocalizedMapSerializer::class)
    @SerialName("name") val name: Map<String, String> = emptyMap(),
    @SerialName("isCategory") val isCategory: Boolean = false,
    @SerialName("orderPriority") val orderPriority: Int = Int.MAX_VALUE,
)

@Serializable
data class HomeDto(
    @SerialName("heroShowId") val heroShowId: String? = null,
    @SerialName("heroRank") val heroRank: Int? = null,
)

@Serializable
data class ShowDto(
    @SerialName("id") val id: String,
    @Serializable(with = LocalizedMapSerializer::class)
    @SerialName("title") val title: Map<String, String> = emptyMap(),
    @Serializable(with = LocalizedMapSerializer::class)
    @SerialName("synopsis") val synopsis: Map<String, String> = emptyMap(),
    @Serializable(with = LocalizedMapSerializer::class)
    @SerialName("hook") val hook: Map<String, String> = emptyMap(),
    @SerialName("posterUrl") val posterUrl: String = "",
    @SerialName("episodeCount") val episodeCount: Int = 0,
    @SerialName("status") val status: String? = null,
    // Server khai 64-bit; mẫu lớn nhất 4_707_000 vẫn vừa Int nhưng phim hot sẽ vượt.
    @SerialName("viewCount") val viewCount: Long = 0,
    // Một phần tử hỏng (null, số) chỉ mất phần tử đó, không mất cả show.
    @Serializable(with = SkipInvalidListSerializer::class)
    @SerialName("tagIds") val tagIds: List<String> = emptyList(),
)
