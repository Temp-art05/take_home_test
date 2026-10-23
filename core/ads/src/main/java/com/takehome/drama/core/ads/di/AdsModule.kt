package com.takehome.drama.core.ads.di

import com.takehome.drama.core.ads.AdsInitializer
import com.takehome.drama.core.ads.AdsPresenter
import com.takehome.drama.core.ads.AdsPresenterImpl
import com.takehome.drama.core.ads.config.AdsConfigRepositoryImpl
import com.takehome.drama.core.ads.config.AdsDebugOverride
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
    single<AdsPresenter> { AdsPresenterImpl(androidContext(), get(), get(), get()) }
    single { AdsInitializer(androidApplication(), get(), get(), get()) }
}
