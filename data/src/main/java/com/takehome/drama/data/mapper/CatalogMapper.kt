package com.takehome.drama.data.mapper

import com.takehome.drama.data.remote.dto.CatalogDto
import com.takehome.drama.data.remote.dto.EpisodeDto
import com.takehome.drama.data.remote.dto.ShowDto
import com.takehome.drama.data.remote.dto.ShowEpisodesDto
import com.takehome.drama.data.remote.dto.TagDto
import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Episode
import com.takehome.drama.domain.model.LocalizedText
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.model.ShowStatus
import com.takehome.drama.domain.model.Tag

fun CatalogDto.toDomain(): Catalog = Catalog(
    version = version,
    defaultLanguage = defaultLanguage.ifBlank { CatalogDto.DEFAULT_LANGUAGE },
    availableLanguages = availableLanguages,
    tags = tags.filter { it.id.isNotBlank() }.distinctBy { it.id }.map { it.toDomain() },
    heroShowId = home?.heroShowId?.takeIf { it.isNotBlank() },
    // Id trùng: giữ bản đầu, để tra theo id (hero, saved, history) luôn ra một show.
    shows = shows.filter { it.id.isNotBlank() }.distinctBy { it.id }.map { it.toDomain() },
)

fun TagDto.toDomain(): Tag = Tag(
    id = id,
    name = LocalizedText(name),
    isCategory = isCategory,
    orderPriority = orderPriority,
)

fun ShowDto.toDomain(): Show = Show(
    id = id,
    title = LocalizedText(title),
    synopsis = LocalizedText(synopsis),
    hook = LocalizedText(hook),
    posterUrl = posterUrl,
    catalogEpisodeCount = episodeCount.coerceAtLeast(0),
    status = ShowStatus.parse(status),
    viewCount = viewCount.coerceAtLeast(0),
    tagIds = tagIds,
)

/**
 * Thứ tự tập = thứ tự mảng; `index` là vị trí **gốc** (kể cả phần tử hỏng đã bỏ), nên có thể thủng
 * lỗ — để server sửa phần tử hỏng thì progress của các tập sau không trỏ nhầm.
 */
fun ShowEpisodesDto.toDomain(): ShowEpisodes = ShowEpisodes(
    showId = showId,
    episodes = episodes.mapIndexedNotNull { index, dto -> dto?.toDomain(index) },
)

fun EpisodeDto.toDomain(index: Int): Episode = Episode(
    index = index,
    number = number,
    title = LocalizedText(title),
    isFree = isFree,
    likeCount = likeCount.coerceAtLeast(0),
    durationSeconds = durationSeconds.coerceAtLeast(0),
    sources = sources,
)
