package com.takehome.drama.core.ads

import com.takehome.drama.domain.model.ads.AdPlacement

/**
 * Ad unit **test** chính thức của Google (README §2.3) — chỗ duy nhất trong app có ad unit id.
 * App id test nằm ở `app/src/main/AndroidManifest.xml`.
 */
internal object AdUnits {
    private const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
    private const val REWARDED = "ca-app-pub-3940256099942544/5224354917"

    fun of(placement: AdPlacement): String = when (placement) {
        AdPlacement.InterstitialOnboarding -> INTERSTITIAL
        AdPlacement.NativeHome -> NATIVE
        AdPlacement.RewardedBonus -> REWARDED
    }
}
