package com.takehome.drama.feature.watch.presentation.player.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import com.takehome.drama.core.designsystem.ext.applyIf
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.drama.core.designsystem.icon.AppIcons
import com.takehome.drama.core.designsystem.theme.AppTheme
import com.takehome.drama.core.designsystem.theme.AppTypography
import com.takehome.drama.feature.watch.R
import com.takehome.drama.feature.watch.presentation.player.model.PlaybackSpeed
import java.util.Locale

private val TextShadowBlur = 6f

@Composable
private fun shadowed(style: TextStyle): TextStyle =
    style.copy(shadow = Shadow(color = AppTheme.colors.media.textShadow, blurRadius = TextShadowBlur))

/** `.ptop`: back + tên show + "Ep. N / M". */
@Composable
internal fun PlayerTopBar(
    title: String,
    episodeNumber: Int,
    episodeTotal: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val media = AppTheme.colors.media
    Row(
        modifier = modifier.padding(horizontal = AppTheme.dimensions.s1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s1),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.watch_back),
                tint = media.text,
            )
        }
        Column {
            Text(
                text = title,
                style = shadowed(AppTypography.button),
                color = media.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (episodeTotal > 0) {
                Text(
                    text = stringResource(R.string.watch_player_episode_of, episodeNumber, episodeTotal),
                    style = shadowed(AppTypography.caption),
                    color = media.textSecondary,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}

/** `.rail`: like, "1×" (chạm = đổi tốc độ, nhấn giữ = sheet 0304), Eps. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PlayerRail(
    likeCount: String,
    isLiked: Boolean,
    speed: Float,
    onLike: () -> Unit,
    onSpeedClick: () -> Unit,
    onSpeedLongPress: () -> Unit,
    onEpisodes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.s5),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RailItem(
            label = likeCount,
            contentDescription = stringResource(R.string.watch_like_cd),
            onClick = onLike,
        ) {
            Icon(
                imageVector = if (isLiked) AppIcons.HeartFilled else AppIcons.Heart,
                contentDescription = null,
                tint = if (isLiked) MaterialTheme.colorScheme.primary else AppTheme.colors.media.text,
                modifier = Modifier.size(AppTheme.dimensions.railIcon),
            )
        }
        val speedLabel = stringResource(R.string.watch_speed_value, PlaybackSpeed.label(speed))
        RailItem(
            label = stringResource(R.string.watch_speed),
            contentDescription = null,
            onClick = onSpeedClick,
            onLongClick = onSpeedLongPress,
        ) {
            Text(
                text = speedLabel,
                style = shadowed(AppTypography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 29.sp)),
                color = AppTheme.colors.media.text,
                modifier = Modifier.height(AppTheme.dimensions.railIcon),
            )
        }
        RailItem(label = stringResource(R.string.watch_eps), contentDescription = null, onClick = onEpisodes) {
            Icon(
                imageVector = AppIcons.Episodes,
                contentDescription = null,
                tint = AppTheme.colors.media.text,
                modifier = Modifier.size(AppTheme.dimensions.railIcon),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RailItem(
    label: String,
    contentDescription: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = AppTheme.dimensions.touchTarget, minHeight = AppTheme.dimensions.touchTarget)
            .clip(AppTheme.shapes.md)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        icon()
        Text(
            text = label,
            style = shadowed(AppTypography.captionSemibold.copy(fontSize = 11.sp)),
            color = AppTheme.colors.media.text,
        )
    }
}

/**
 * `.pbot`: "Ep. N" + mô tả 2 dòng + more. "more" chỉ hiện khi mô tả thật sự bị cắt; mở rộng thì hiện
 * **đủ** văn bản, cuộn trong khung cao tối đa [ExpandedDescriptionMaxHeight].
 */
@Composable
internal fun PlayerInfo(
    episodeNumber: Int,
    description: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val media = AppTheme.colors.media
    var truncated by remember(description) { mutableStateOf(false) }
    Column(modifier = modifier) {
        // Tên tập rỗng ở cả 299 tập → chỉ hiện số tập.
        Text(
            text = stringResource(R.string.watch_episode_number, episodeNumber),
            style = shadowed(AppTypography.titleSmall.copy(fontSize = 16.sp)),
            color = media.text,
        )
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = shadowed(AppTypography.label.copy(fontWeight = FontWeight.Normal, lineHeight = 19.5.sp)),
                color = media.textBody,
                maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES,
                overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) truncated = it.hasVisualOverflow },
                modifier = Modifier
                    .padding(top = 6.dp)
                    .applyIf(expanded) { heightIn(max = ExpandedDescriptionMaxHeight).verticalScroll(rememberScrollState()) }
                    .tapToToggle(onToggle),
            )
            if (truncated || expanded) {
                Text(
                    text = stringResource(if (expanded) R.string.watch_less_lower else R.string.watch_more_lower),
                    style = AppTypography.label,
                    color = media.textFaint,
                    modifier = Modifier.tapToToggle(onToggle),
                )
            }
        }
    }
}

private const val COLLAPSED_LINES = 2
private val ExpandedDescriptionMaxHeight = 220.dp

private fun Modifier.tapToToggle(onClick: () -> Unit): Modifier =
    pointerInput(onClick) { detectTapGestures { onClick() } }

/**
 * `.ptrack`: thanh tiến độ kéo được + thời gian. [dimmed] khi buffering (SCR-0604): phần đã chạy
 * thành trắng mờ để không gợi ý là đang phát.
 */
@Composable
internal fun PlayerTrack(
    /** Lambda: vị trí đổi mỗi 500ms, chỉ thanh này được recompose theo nó (không kéo cả màn). */
    positionMs: () -> Long,
    durationMs: Long,
    dimmed: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    /** false = track tĩnh của trang lân cận (chỉ hiện chỗ đã xem tới). */
    seekable: Boolean = true,
) {
    val media = AppTheme.colors.media
    val canSeek = seekable && durationMs > 0
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val positionMs = positionMs()
    val fraction = dragFraction
        ?: if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val seekDescription = stringResource(R.string.watch_seek_cd)

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .semantics {
                    contentDescription = seekDescription
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                    if (canSeek) {
                        setProgress { target ->
                            onSeek((target.coerceIn(0f, 1f) * durationMs).toLong())
                            true
                        }
                    }
                }
                .pointerInput(durationMs, canSeek) {
                    if (!canSeek) return@pointerInput
                    detectTapGestures { offset -> onSeek((offset.x / size.width * durationMs).toLong()) }
                }
                .pointerInput(durationMs, canSeek) {
                    if (!canSeek) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> dragFraction = (offset.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = {
                            dragFraction?.let { onSeek((it * durationMs).toLong()) }
                            dragFraction = null
                        },
                        onDragCancel = { dragFraction = null },
                        onHorizontalDrag = { change, _ ->
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val thumb = 11.dp
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(media.track),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(CircleShape)
                        .then(
                            if (dimmed) Modifier.background(media.trackDimmed) else Modifier.background(AppTheme.colors.gradientCta)
                        ),
                )
            }
            Box(
                Modifier
                    .offset(x = maxWidth * fraction - thumb / 2)
                    .size(thumb)
                    .background(media.text, CircleShape),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            val shownPosition = dragFraction?.let { (it * durationMs).toLong() } ?: positionMs
            Text(formatTime(shownPosition), style = AppTypography.timecode, color = media.textMuted)
            Text(formatTime(durationMs), style = AppTypography.timecode, color = media.textMuted)
        }
    }
}

/** `.eppill`: "☰ Episodes". */
@Composable
internal fun EpisodesPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val media = AppTheme.colors.media
    val shape = AppTheme.shapes.full
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(media.glassStrong, shape)
            .border(1.dp, media.glassBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(Icons.Filled.Menu, contentDescription = null, tint = media.text, modifier = Modifier.size(16.dp))
        Text(stringResource(R.string.watch_episodes), style = AppTypography.labelSemibold, color = media.text)
    }
}

/** Nút play/pause lớn giữa màn khi user pause. */
@Composable
internal fun CenterPlayIcon(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(64.dp)
            .background(AppTheme.colors.media.glass, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = AppTheme.colors.media.text, modifier = Modifier.size(32.dp))
    }
}

/** `0:42`, `1:02:03`. */
internal fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0) / 1000)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}
