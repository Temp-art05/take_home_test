package com.takehome.drama.core.ads

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.takehome.drama.core.ads.nativead.NativeAdContent
import com.takehome.drama.core.ads.sdk.AdsSdkFacade
import com.takehome.drama.domain.model.ads.AdPlacement
import com.takehome.drama.domain.usecase.ads.IsAdPlacementEnabledUseCase
import com.takehome.drama.domain.usecase.ads.ObserveAdPlacementUseCase
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Cửa duy nhất feature dùng để hiện ad. Mọi hàm **không bao giờ ném** và luôn gọi callback đúng một
 * lần: ad hỏng (offline, không fill, SDK chưa init, placement bị tắt) = "không có ad", app đi tiếp.
 * Policy Remote Config được kiểm lại ngay trước mỗi lần load/show.
 */
interface AdsPresenter {
    /** Load sẵn full-screen ad để lúc cần hiện ngay, không bắt user chờ. */
    fun preload(placement: AdPlacement)

    /** Chỉ hiện khi đã load xong; chưa sẵn → [onDone] ngay (không chờ, không overlay). */
    fun showInterstitialIfReady(activity: Activity, placement: AdPlacement, onDone: () -> Unit)

    /** Load rewarded, chờ tối đa vài giây. `false` = không có ad để xem. */
    suspend fun loadRewarded(placement: AdPlacement): Boolean

    /** Chỉ gọi từ một nút user bấm. [onResult] `true` khi user xem hết và nhận thưởng. */
    fun showRewarded(activity: Activity, placement: AdPlacement, onResult: (earned: Boolean) -> Unit)

    /** Ô native; policy tắt hoặc chưa có ad → không vẽ gì (không giữ chỗ trống). */
    @Composable
    fun NativeAdSlot(placement: AdPlacement, modifier: Modifier)
}

class AdsPresenterImpl(
    context: Context,
    private val sdk: AdsSdkFacade,
    private val isEnabled: IsAdPlacementEnabledUseCase,
    private val observeEnabled: ObserveAdPlacementUseCase,
) : AdsPresenter {

    private val appContext: Context = context.applicationContext ?: context

    override fun preload(placement: AdPlacement) {
        if (!isEnabled(placement)) return
        safely("preload ${placement.id}") { sdk.preloadFullScreen(appContext, placement.id, {}, {}) }
    }

    override fun showInterstitialIfReady(activity: Activity, placement: AdPlacement, onDone: () -> Unit) {
        val done = once(onDone)
        if (!isEnabled(placement) || !safelyReady(placement)) {
            done()
            return
        }
        if (!safely("show ${placement.id}") { sdk.showInterstitial(activity, placement.id, done) }) done()
    }

    override suspend fun loadRewarded(placement: AdPlacement): Boolean {
        if (!isEnabled(placement)) return false
        if (safelyReady(placement)) return true
        val failed = AtomicBoolean(false)
        val requested = safely("preload ${placement.id}") {
            sdk.preloadFullScreen(appContext, placement.id, onLoaded = {}, onFailed = { failed.set(true) })
        }
        if (!requested) return false
        // Poll thay vì chỉ chờ callback: preload của lib no-op (không callback) khi đã có job đang load.
        return try {
            withTimeoutOrNull(REWARDED_LOAD_TIMEOUT_MS) {
                while (!failed.get() && !safelyReady(placement)) delay(POLL_MS)
                !failed.get()
            } ?: false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "loadRewarded failed")
            false
        }
    }

    override fun showRewarded(activity: Activity, placement: AdPlacement, onResult: (Boolean) -> Unit) {
        val result = onceWith(onResult)
        if (!isEnabled(placement) || !safelyReady(placement)) {
            result(false)
            return
        }
        if (!safely("show ${placement.id}") { sdk.showRewarded(activity, placement.id) { earned -> result(earned) } }) result(false)
    }

    @Composable
    override fun NativeAdSlot(placement: AdPlacement, modifier: Modifier) {
        val enabledFlow = remember(placement) { observeEnabled(placement) }
        NativeAdContent(
            placement = placement,
            enabledFlow = enabledFlow,
            initiallyEnabled = remember(placement) { isEnabled(placement) },
            request = { safely("native ${placement.id}") { sdk.requestNative(appContext, placement.id) } },
            modifier = modifier,
        )
    }

    private fun safelyReady(placement: AdPlacement): Boolean =
        runCatching { sdk.isFullScreenReady(placement.id) }.getOrDefault(false)

    /** `false` khi SDK ném — caller coi như không có ad. */
    private inline fun safely(what: String, block: () -> Unit): Boolean = try {
        block()
        true
    } catch (e: Exception) {
        Timber.tag(TAG).w(e, "Ads %s failed", what)
        false
    }

    private fun once(block: () -> Unit): () -> Unit {
        val called = AtomicBoolean(false)
        return { if (called.compareAndSet(false, true)) block() }
    }

    private fun <T> onceWith(block: (T) -> Unit): (T) -> Unit {
        val called = AtomicBoolean(false)
        return { value -> if (called.compareAndSet(false, true)) block(value) }
    }

    companion object {
        const val REWARDED_LOAD_TIMEOUT_MS = 8_000L
        private const val POLL_MS = 250L
        private const val TAG = "Ads"
    }
}
