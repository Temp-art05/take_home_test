package com.takehome.drama.domain.fake

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Episode
import com.takehome.drama.domain.model.LocalizedText
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.ShowStatus
import com.takehome.drama.domain.model.Tag

fun show(
    id: String,
    tagIds: List<String> = emptyList(),
    status: ShowStatus = ShowStatus.Completed,
    viewCount: Long = 0,
    episodeCount: Int = 10,
    title: Map<String, String> = mapOf("en" to "Show $id"),
    hook: Map<String, String> = emptyMap(),
) = Show(
    id = id,
    title = LocalizedText(title),
    synopsis = LocalizedText.Empty,
    hook = LocalizedText(hook),
    posterUrl = "https://example.com/$id.jpg",
    catalogEpisodeCount = episodeCount,
    status = status,
    viewCount = viewCount,
    tagIds = tagIds,
)

fun tag(
    id: String,
    isCategory: Boolean,
    orderPriority: Int,
    name: Map<String, String> = mapOf("en" to id),
) = Tag(id = id, name = LocalizedText(name), isCategory = isCategory, orderPriority = orderPriority)

fun catalog(
    shows: List<Show>,
    tags: List<Tag> = emptyList(),
    heroShowId: String? = null,
    availableLanguages: List<String> = listOf("en", "ar", "hi", "id", "ta"),
) = Catalog(
    version = 1,
    defaultLanguage = "en",
    availableLanguages = availableLanguages,
    tags = tags,
    heroShowId = heroShowId,
    shows = shows,
)

fun episode(
    index: Int,
    sources: Map<String, Map<String, String>> = mapOf("en" to mapOf("720p" to "https://e/$index.mp4")),
    isFree: Boolean = true,
    number: Int? = index + 1,
    likeCount: Long = 0,
) = Episode(
    index = index,
    number = number,
    title = LocalizedText.Empty,
    isFree = isFree,
    likeCount = likeCount,
    durationSeconds = 0,
    sources = sources,
)
