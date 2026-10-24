package com.takehome.drama.core.ads.di

import com.takehome.drama.core.ads.AdsInitializer
import com.takehome.drama.core.ads.AdsLogToggle
import com.takehome.drama.core.ads.LibAdsLogToggle
import com.takehome.drama.core.ads.config.AdsConfigRepositoryImpl
import com.takehome.drama.core.ads.config.AdsDebugOverride
import com.takehome.drama.core.ads.interstitial.InterstitialAdManager
import com.takehome.drama.core.ads.interstitial.InterstitialAdManagerImpl
import com.takehome.drama.core.ads.nativead.NativeAdManager
import com.takehome.drama.core.ads.nativead.NativeAdManagerImpl
import com.takehome.drama.core.ads.reward.RewardAdManager
import com.takehome.drama.core.ads.reward.RewardAdManagerImpl
import com.takehome.drama.core.ads.sdk.AdsSdkFacade
import com.takehome.drama.core.ads.sdk.LibAdsSdkFacade
import com.takehome.drama.domain.repository.AdsConfigRepository
import com.takehome.drama.domain.usecase.ads.IsAdPlacementEnabledUseCase
import com.takehome.drama.domain.usecase.ads.ObserveAdPlacementUseCase
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val adsModule = module {
    single { AdsDebugOverride() }
    single<AdsConfigRepository> { AdsConfigRepositoryImpl(get(), get()) }
    // Use case cạnh repository: dataModule không ngầm phụ thuộc adsModule.
    factory { ObserveAdPlacementUseCase(get(), get()) }
    factory { IsAdPlacementEnabledUseCase(get(), get()) }
    single<AdsSdkFacade> { LibAdsSdkFacade() }
    single<InterstitialAdManager> { InterstitialAdManagerImpl(androidContext(), get(), get()) }
    single<RewardAdManager> { RewardAdManagerImpl(androidContext(), get(), get()) }
    single<NativeAdManager> { NativeAdManagerImpl(androidContext(), get(), get(), get()) }
    single<AdsLogToggle> { LibAdsLogToggle(get(), get()) }
    single { AdsInitializer(androidApplication(), get(), get(), get(), get(), get(), get()) }
}
