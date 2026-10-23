package com.github.dinhn.ads.core

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Primitive banner cho MỘT ad unit id — **nội bộ lib**, chỉ [AdsBanner] (theo placement) gọi để lo
 * waterfall high floor / all price + enable. App KHÔNG dùng trực tiếp: luôn qua `AdsBanner(name)`.
 * View tự huỷ khi rời composition.
 *
 * Chỉ request SAU khi SDK init xong ([AdsSdk.ready]) để tránh lỗi "Internal error".
 */
@Composable
internal fun AdsBannerView(
    adUnitId: String,
    modifier: Modifier = Modifier,
    placement: String = "",
    onLoaded: () -> Unit = {},
    onFailed: (String) -> Unit = {},
) {
    val adapter = AdsSdk.adapter() ?: return
    if (adUnitId.isBlank()) return

    val ready by AdsSdk.ready
    val canRequestAds by AdsSdk.canRequestAds
    val remoteConfigReady by AdsSdk.remoteConfigReady
    if (!ready || !canRequestAds || !remoteConfigReady) return   // [P2-3] consent + [P2-5] remote config nếu bật gate

    key(adUnitId) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { ctx ->
                // Log vòng đời load nằm ở adapter (AdMobAdapter); [placement] để log kèm tên vị trí.
                adapter.createBanner(ctx, adUnitId, object : BannerAdCallback {
                    override fun onLoaded() { onLoaded() }
                    override fun onFailed(error: String) { onFailed(error) }
                }, placement)
            },
            onRelease = { adapter.destroyBanner(it) },
        )
    }
}
