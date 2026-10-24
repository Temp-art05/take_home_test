package com.takehome.drama.core.ads

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.takehome.drama.domain.model.ads.AdPlacement
import kotlinx.coroutines.CompletableDeferred

/** Presenter giả cho test ViewModel: ghi lại lời gọi, kết quả load rewarded do test đặt. */
class FakeAdsPresenter : AdsPresenter {
    var rewardedAvailable = true

    /** Đặt để `loadRewarded` treo tới khi test `complete` — giữ trạng thái Loading. */
    var rewardedGate: CompletableDeferred<Unit>? = null

    val preloaded = mutableListOf<AdPlacement>()
    var rewardedLoads = 0
        private set

    override fun preload(placement: AdPlacement) {
        preloaded += placement
    }

    override fun showInterstitialIfReady(activity: Activity, placement: AdPlacement, onDone: () -> Unit) = onDone()

    override suspend fun loadRewarded(placement: AdPlacement): Boolean {
        rewardedLoads++
        rewardedGate?.await()
        return rewardedAvailable
    }

    override fun showRewarded(activity: Activity, placement: AdPlacement, onResult: (Boolean) -> Unit) = onResult(true)

    @Composable
    override fun NativeAdSlot(placement: AdPlacement, modifier: Modifier) = Unit

    var resumeAdSuppressions = 0
        private set

    override fun suppressNextResumeAd() {
        resumeAdSuppressions++
    }

    @Composable
    override fun BlockResumeAds() = Unit
}
