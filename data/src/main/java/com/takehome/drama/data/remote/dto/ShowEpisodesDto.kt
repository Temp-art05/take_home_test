package com.takehome.drama.data.remote.dto

import com.takehome.drama.data.remote.dto.serializer.LocalizedMapSerializer
import com.takehome.drama.data.remote.dto.serializer.PositionalListSerializer
import com.takehome.drama.data.remote.dto.serializer.SourcesSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET shows/{showId}.json`. */
@Serializable
data class ShowEpisodesDto(
    @SerialName("showId") val showId: String = "",
    // Phần tử hỏng là null tại đúng vị trí: vị trí gốc là định danh của tập.
    @Serializable(with = EpisodeListSerializer::class)
    @SerialName("episodes") val episodes: List<EpisodeDto?> = emptyList(),
)

object EpisodeListSerializer : PositionalListSerializer<EpisodeDto>(EpisodeDto.serializer())

@Serializable
data class EpisodeDto(
    @SerialName("number") val number: Int? = null,
    // Hiện là string (luôn rỗng); đọc như text đa ngôn ngữ để không vỡ nếu server chuyển sang map như title của show.
    @Serializable(with = LocalizedMapSerializer::class)
    @SerialName("title") val title: Map<String, String> = emptyMap(),
    // Thiếu thì coi là khoá: mở nhầm tập trả phí tệ hơn khoá nhầm, và khoá là trường hợp phổ biến (225/299).
    @SerialName("isFree") val isFree: Boolean = false,
    @SerialName("durationSeconds") val durationSeconds: Int = 0,
    @SerialName("likeCount") val likeCount: Long = 0,
    @Serializable(with = SourcesSerializer::class)
    @SerialName("sources") val sources: Map<String, Map<String, String>> = emptyMap(),
)
