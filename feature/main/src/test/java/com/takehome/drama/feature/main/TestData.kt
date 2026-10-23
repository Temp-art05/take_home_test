package com.takehome.drama.feature.main

import com.takehome.drama.core.utils.network.ConnectivityMonitor
import com.takehome.drama.domain.fake.catalog
import com.takehome.drama.domain.fake.episode
import com.takehome.drama.domain.fake.show
import com.takehome.drama.domain.fake.tag
import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.usecase.discovery.ClockTime
import com.takehome.drama.domain.usecase.discovery.WallClock
import kotlinx.coroutines.flow.MutableStateFlow

class FakeConnectivityMonitor(unmetered: Boolean = true) : ConnectivityMonitor {
    override val isUnmetered = MutableStateFlow(unmetered)
}

class FakeWallClock(var time: ClockTime = ClockTime(21, 45)) : WallClock {
    override fun now(): ClockTime = time
}

/** Catalogue nhỏ giống fixtures: hero không nằm đầu mảng, 2 genre, lượt xem lệch nhau. */
val testCatalog = catalog(
    shows = listOf(
        show("a", tagIds = listOf("romance"), viewCount = 10, title = mapOf("en" to "Alpha Love")),
        show("hero", tagIds = listOf("revenge"), viewCount = 500, title = mapOf("en" to "The Revenge Plan")),
        show("c", tagIds = listOf("revenge"), viewCount = 900, title = mapOf("en" to "CEO Returns")),
        show("d", tagIds = listOf("romance"), viewCount = 50, title = mapOf("en" to "Dawn")),
    ),
    tags = listOf(
        tag("romance", isCategory = true, orderPriority = 1, name = mapOf("en" to "Romance")),
        tag("revenge", isCategory = true, orderPriority = 2, name = mapOf("en" to "Revenge")),
    ),
    heroShowId = "hero",
)

/** Mỗi show: tập 0 khoá, tập 1 free (URL `https://e/<show>/1.mp4`), tập 2 khoá. */
fun testEpisodes(showId: String) = ShowEpisodes(
    showId,
    listOf(
        episode(index = 0, isFree = false),
        episode(index = 1, sources = mapOf("en" to mapOf("720p" to clipUrl(showId))), likeCount = 1_000),
        episode(index = 2, isFree = false),
    ),
)

fun clipUrl(showId: String) = "https://e/$showId/1.mp4"
