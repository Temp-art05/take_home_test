package com.github.dinhn.ads.core

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Pool native scope-ĐỘC-LẬP (M2b / P2-12). Load chạy trên scope riêng — KHÔNG buộc vào lifecycle
 * của màn. Thoát màn lúc đang load thì kết quả vẫn về pool (preload/reuse khi quay lại), không mất.
 *
 * Sở hữu [AdHandle] (NativeAd): destroy khi bị thay bằng handle mới hoặc khi [clear].
 * Composable chỉ bind View từ handle (LocalContext) và tự destroy View — KHÔNG destroy handle.
 *
 * Kèm: debounce 15s, TTL ~1h, waterfall HF→AP, giữ ad cũ tới khi ad mới sẵn sàng (P2-11.2).
 * Nằm trong lib để mọi app dùng chung; composable [AdsNative] đọc pool này theo placement name.
 */
object NativeAdPool {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private const val TTL_MS = 60 * 60 * 1000L   // native ad ~1h
    private const val DEBOUNCE_MS = 15_000L       // P2-11.2

    sealed interface State {
        data object Idle : State
        data object Loading : State
        data class Loaded(val handle: AdHandle, val loadedAt: Long) : State
        data object Failed : State
    }

    private class Entry {
        val flow = MutableStateFlow<State>(State.Idle)
        var lastRequest = 0L
        var job: Job? = null
    }

    private val entries = HashMap<String, Entry>()
    private fun entry(key: String) = entries.getOrPut(key) { Entry() }

    fun state(key: String): StateFlow<State> = entry(key).flow.asStateFlow()

    /** Request load cho [key], waterfall qua [adUnitIds] (HF→AP). Debounce + TTL + giữ ad cũ. */
    fun request(appContext: Context, key: String, adUnitIds: List<String>) {
        if (adUnitIds.isEmpty()) return
        // Rule 4: chưa init xong (hoặc chưa consent / chưa áp remote config) thì KHÔNG request — tránh
        // "Internal error" + KHÔNG arm debounce 15s (return trước khi set lastRequest), để lần gọi sau
        // khi các cờ lật true còn load được.
        if (!AdsSdk.ready.value || !AdsSdk.canRequestAds.value || !AdsSdk.remoteConfigReady.value) return
        val e = entry(key)
        val now = System.currentTimeMillis()
        val cur = e.flow.value

        if (cur is State.Loaded && now - cur.loadedAt < TTL_MS) return     // còn tươi → reuse
        if (e.job?.isActive == true) return                                // đang load
        if (now - e.lastRequest < DEBOUNCE_MS) return                      // debounce 15s

        val adapter = AdsSdk.adapter() ?: run {
            if (cur !is State.Loaded) e.flow.value = State.Failed
            return
        }
        e.lastRequest = now
        if (cur !is State.Loaded) e.flow.value = State.Loading             // giữ ad cũ nếu đang có

        e.job = scope.launch {
            val handle = loadWaterfall(appContext, adapter, adUnitIds, key)
            if (handle != null) {
                (e.flow.value as? State.Loaded)?.let { adapter.destroyNativeHandle(it.handle) } // destroy cũ
                e.flow.value = State.Loaded(handle, System.currentTimeMillis())
            } else if (e.flow.value !is State.Loaded) {
                e.flow.value = State.Failed                                // fail mà đang có ad → giữ ad cũ
            }
        }
    }

    /** Huỷ handle hiện tại của [key] + cancel job. Gọi khi chắc chắn không còn consumer. */
    fun clear(key: String) {
        val e = entries[key] ?: return
        e.job?.cancel()
        (e.flow.value as? State.Loaded)?.let { AdsSdk.adapter()?.destroyNativeHandle(it.handle) }
        e.flow.value = State.Idle
    }

    private suspend fun loadWaterfall(ctx: Context, adapter: AdsAdapter, ids: List<String>, key: String): AdHandle? {
        for (id in ids) {
            loadOne(ctx, adapter, id, key)?.let { return it }
        }
        return null
    }

    private suspend fun loadOne(ctx: Context, adapter: AdsAdapter, adUnitId: String, key: String): AdHandle? =
        suspendCancellableCoroutine { cont ->
            adapter.loadNative(ctx, adUnitId, object : AdLoadResult {
                override fun onLoaded(handle: AdHandle) { if (cont.isActive) cont.resumeWith(Result.success(handle)) }
                override fun onFailed(error: String) {
                    AdsLogger.w("[NativeAdPool] fail placement='$key' id=$adUnitId: $error")
                    if (cont.isActive) cont.resumeWith(Result.success(null))
                }
            }, preload = true, placement = key)   // pool load ngầm để reuse → [PRE-LOAD]
        }
}
