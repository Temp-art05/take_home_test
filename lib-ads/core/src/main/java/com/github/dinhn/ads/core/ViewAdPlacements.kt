package com.github.dinhn.ads.core

import android.view.LayoutInflater
import android.view.View
import androidx.annotation.LayoutRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Placement cho ad **View-based** (banner / native) — app đăng ký 1 lần qua `AdsSdk.registerBanner`
 * / `registerNative`. Ad unit id + enable key do app cấp (Rule 3). Layout XML (native) vẫn ở app
 * nên truyền vào composable lúc render, KHÔNG đăng ký ở đây.
 */
class ViewAdPlacement internal constructor(
    val name: String,
    val highFloorId: String,
    val normalId: String,
    val enableHighFloorKey: String,
    val enableNormalKey: String,
)

/** Registry các placement view-based; [AdsBanner]/[AdsNative] tra theo tên. */
object ViewAdRegistry {
    private val placements = HashMap<String, ViewAdPlacement>()
    fun register(p: ViewAdPlacement) { placements[p.name] = p }
    fun get(name: String): ViewAdPlacement? = placements[name]

    /** Danh sách id theo waterfall HF→AP, chỉ gồm price tier đang bật. */
    fun waterfallIds(name: String): List<String> {
        val p = placements[name] ?: return emptyList()
        return buildList {
            if (AdsSdk.isAdEnabled(p.enableHighFloorKey)) add(p.highFloorId)
            if (AdsSdk.isAdEnabled(p.enableNormalKey)) add(p.normalId)
        }.filter { it.isNotBlank() }
    }

    fun isEnabled(name: String): Boolean {
        val p = placements[name] ?: return false
        return AdsSdk.isAdEnabled(p.enableHighFloorKey) || AdsSdk.isAdEnabled(p.enableNormalKey)
    }
}

/**
 * Banner theo placement đã đăng ký: waterfall HF→AP (rớt tier khi fail), chỉ fade-in khi load xong.
 * Tắt cả 2 key → không request (return). Mọi app integrate dùng chung — không tự ghép waterfall.
 *
 * [shimmerLayoutId] = layout chờ (0 = không). Khi có shimmer: hiện shimmer lúc đang load để **giữ chỗ,
 * không nhảy layout** (B01); load xong → banner; fail hết tier → **ẩn** cả shimmer lẫn banner (B04).
 * Shimmer nên có kích thước ~ banner để reserve đúng chỗ (do app cấp — layout XML thuộc app, Rule 1).
 */
@Composable
fun AdsBanner(
    name: String,
    modifier: Modifier = Modifier,
    @LayoutRes shimmerLayoutId: Int = 0,
    onLoaded: () -> Unit = {},
    onFailed: () -> Unit = {},
) {
    val ids = ViewAdRegistry.waterfallIds(name)
    if (ids.isEmpty()) return

    var tier by remember(name, ids) { mutableIntStateOf(0) }
    var show by remember(name) { mutableStateOf(false) }
    var failedAll by remember(name, ids) { mutableStateOf(false) }
    if (tier >= ids.size) return

    // Bọc Box để shimmer (giữ chỗ) nằm dưới banner; banner fade-in đè lên khi loaded.
    // Fail hết tier → collapse (ẩn cả 2). Còn đang waterfall → vẫn coi là "đang load" → giữ shimmer.
    Box(modifier = modifier.fillMaxWidth()) {
        if (failedAll) return@Box

        if (!show && shimmerLayoutId != 0) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx -> LayoutInflater.from(ctx).inflate(shimmerLayoutId, null, false) },
            )
        }
        // LUÔN compose AdsBannerView để nó request (đừng ẩn bằng visible=false → không compose → không load).
        val alpha by animateFloatAsState(targetValue = if (show) 1f else 0f, label = "bannerAlpha")
        AdsBannerView(
            adUnitId = ids[tier],
            modifier = Modifier.fillMaxWidth().alpha(alpha),
            placement = name,
            onLoaded = { show = true; onLoaded() },
            onFailed = {
                show = false
                if (tier < ids.lastIndex) tier++ else { failedAll = true; onFailed() }   // rớt AP; hết tier → ẩn
            },
        )
    }
}

/**
 * Native theo placement đã đăng ký, render bằng [layoutId] (layout XML của app). Load qua
 * [NativeAdPool] (scope-độc-lập, debounce/TTL/waterfall, giữ ad cũ tới khi ad mới sẵn). Composable
 * chỉ bind View + tự destroy View khi rời màn; handle do pool giữ. [shimmerLayoutId] = layout chờ (0 = không).
 */
@Composable
fun AdsNative(
    name: String,
    @LayoutRes layoutId: Int,
    modifier: Modifier = Modifier,
    @LayoutRes shimmerLayoutId: Int = 0,
    onStartLoad: () -> Unit = {},
    onLoaded: () -> Unit = {},
    onFailed: () -> Unit = {},
) {
    if (!ViewAdRegistry.isEnabled(name)) return
    val ids = ViewAdRegistry.waterfallIds(name)
    if (ids.isEmpty()) return

    val appContext = LocalContext.current.applicationContext
    val renderContext = LocalContext.current
    val adapter = AdsSdk.adapter()

    // [Rule 4] chỉ request sau khi SDK init xong (+ consent [P2-3] + remote config [P2-5] nếu bật gate).
    // Key theo ready/canRequestAds/remoteConfigReady để khi các cờ này lật true (vd native ở màn
    // cold-start, hoặc remote config vừa fetch xong), LaunchedEffect re-fire → không kẹt.
    val ready by AdsSdk.ready
    val canRequestAds by AdsSdk.canRequestAds
    val remoteConfigReady by AdsSdk.remoteConfigReady

    val state by NativeAdPool.state(name).collectAsState()
    LaunchedEffect(name, ids, ready, canRequestAds, remoteConfigReady) {
        if (ready && canRequestAds && remoteConfigReady) NativeAdPool.request(appContext, name, ids)
    }
    LaunchedEffect(state is NativeAdPool.State.Loading) {
        if (state is NativeAdPool.State.Loading) onStartLoad()   // pool bắt đầu load
    }

    var boundView by remember(name) { mutableStateOf<View?>(null) }
    var boundHandle by remember(name) { mutableStateOf<AdHandle?>(null) }

    // Bind/swap khi pool có handle mới. Giữ ad cũ tới khi view mới dựng xong rồi mới huỷ view cũ.
    LaunchedEffect(state) {
        when (val s = state) {
            is NativeAdPool.State.Loaded -> {
                if (adapter != null && s.handle !== boundHandle) {
                    val newView = adapter.bindNative(renderContext, s.handle, layoutId)
                    val old = boundView
                    boundView = newView
                    boundHandle = s.handle
                    old?.let { adapter.destroyNative(it) }
                    onLoaded()
                }
            }
            is NativeAdPool.State.Failed -> if (boundView == null) onFailed()
            else -> Unit
        }
    }

    DisposableEffect(name) {
        onDispose {
            // Huỷ View khi rời màn; handle vẫn ở pool để reuse khi quay lại.
            boundView?.let { adapter?.destroyNative(it) }
            boundView = null
            boundHandle = null
        }
    }

    val v = boundView
    val waiting = state is NativeAdPool.State.Idle || state is NativeAdPool.State.Loading
    when {
        v != null -> key(v) { AndroidView(modifier = modifier.fillMaxWidth(), factory = { v }) }
        // Chỉ hiện shimmer khi đang chờ; Failed (vd offline) → ẩn cả shimmer lẫn ad (N011).
        waiting && shimmerLayoutId != 0 -> AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { ctx -> LayoutInflater.from(ctx).inflate(shimmerLayoutId, null, false) },
        )
        else -> Unit
    }
}
