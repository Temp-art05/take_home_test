package com.takehome.drama.navigation

import com.takehome.drama.core.navigation.destination.Destination
import com.takehome.drama.core.navigation.destination.WatchRoutes
import com.takehome.drama.feature.watch.presentation.player.PlayerScreenProvider
import com.takehome.drama.feature.watch.presentation.detail.ShowDetailScreenProvider

/** Cài [WatchRoutes] bằng destination thật của `:feature:watch` — chỉ `:app` thấy được cả hai đầu. */
object AppWatchRoutes : WatchRoutes {
    override fun showDetail(showId: String): Destination = ShowDetailScreenProvider.ShowDetail(showId)

    override fun player(showId: String, episodeIndex: Int): Destination =
        PlayerScreenProvider.Player(showId = showId, episodeIndex = episodeIndex)
}
