package com.takehome.drama.core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Bề mặt vẽ video của [player], phủ kín khung (crop) như mockup: video dọc 9:16 trên màn 20:9 chỉ
 * mất ~10% hai bên thay vì viền đen trên dưới. Màn sáng suốt lúc đang phát. Player không phải
 * [ExoEpisodeFeedPlayer] (preview, test) thì không vẽ gì.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoSurface(player: EpisodeFeedPlayer, modifier: Modifier = Modifier) {
    // Chỉ đọc isPlaying: vị trí đổi mỗi 500ms không được kéo bề mặt video recompose.
    val isPlaying by remember(player) { player.state.map { it.isPlaying }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(initialValue = false)
    val view = LocalView.current
    DisposableEffect(view, isPlaying) {
        view.keepScreenOn = isPlaying
        onDispose { view.keepScreenOn = false }
    }

    val exoPlayer = (player as? ExoEpisodeFeedPlayer)?.exoPlayer ?: return
    ContentFrame(
        player = exoPlayer,
        modifier = modifier,
        surfaceType = SURFACE_TYPE_SURFACE_VIEW,
        contentScale = ContentScale.Crop,
    )
}
