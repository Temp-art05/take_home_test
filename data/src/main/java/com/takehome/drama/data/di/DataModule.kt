package com.takehome.drama.data.di

import androidx.room.Room
import com.takehome.drama.data.local.database.AppDatabase
import com.takehome.drama.data.local.datasource.CatalogCacheLocalDataSource
import com.takehome.drama.data.local.datasource.CatalogCacheLocalDataSourceImpl
import com.takehome.drama.data.local.datasource.EpisodeUnlockLocalDataSource
import com.takehome.drama.data.local.datasource.EpisodeUnlockLocalDataSourceImpl
import com.takehome.drama.data.local.datasource.SavedShowLocalDataSource
import com.takehome.drama.data.local.datasource.SavedShowLocalDataSourceImpl
import com.takehome.drama.data.local.datasource.UserPreferencesLocalDataSource
import com.takehome.drama.data.local.datasource.UserPreferencesLocalDataSourceImpl
import com.takehome.drama.data.local.datasource.WatchProgressLocalDataSource
import com.takehome.drama.data.local.datasource.WatchProgressLocalDataSourceImpl
import com.takehome.drama.data.local.datasource.episodeUnlockDataStore
import com.takehome.drama.data.remote.datasource.CatalogAssetDataSourceImpl
import com.takehome.drama.data.remote.datasource.CatalogDataSource
import com.takehome.drama.data.remote.datasource.CatalogRemoteDataSourceImpl
import com.takehome.drama.data.remote.datasource.DebugCatalogFault
import com.takehome.drama.data.remote.datasource.FaultInjectingCatalogDataSource
import com.takehome.drama.data.remote.http.ApiConfig
import com.takehome.drama.data.remote.http.defaultJson
import com.takehome.drama.data.remote.http.provideHttpClient
import com.takehome.drama.data.repository.CatalogRepositoryImpl
import com.takehome.drama.data.repository.EpisodeUnlockRepositoryImpl
import com.takehome.drama.data.repository.SavedShowRepositoryImpl
import com.takehome.drama.data.repository.UserPreferencesRepositoryImpl
import com.takehome.drama.data.repository.WatchProgressRepositoryImpl
import com.takehome.drama.data.util.SystemWallClock
import com.takehome.drama.domain.config.AppConfig
import com.takehome.drama.domain.repository.CatalogRepository
import com.takehome.drama.domain.repository.EpisodeUnlockRepository
import com.takehome.drama.domain.repository.SavedShowRepository
import com.takehome.drama.domain.repository.UserPreferencesRepository
import com.takehome.drama.domain.repository.WatchProgressRepository
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.discovery.GetHomeFeedUseCase
import com.takehome.drama.domain.usecase.discovery.WallClock
import com.takehome.drama.domain.usecase.history.GetContinueTargetUseCase
import com.takehome.drama.domain.usecase.history.GetResumePositionUseCase
import com.takehome.drama.domain.usecase.history.ObserveShowProgressUseCase
import com.takehome.drama.domain.usecase.history.EpochClock
import com.takehome.drama.domain.usecase.history.GetContinueEpisodeUseCase
import com.takehome.drama.domain.usecase.history.ObserveWatchHistoryUseCase
import com.takehome.drama.domain.usecase.history.RemoveFromHistoryUseCase
import com.takehome.drama.domain.usecase.history.SaveWatchProgressUseCase
import com.takehome.drama.domain.usecase.library.ObserveIsShowSavedUseCase
import com.takehome.drama.domain.usecase.library.ObserveLibrarySummaryUseCase
import com.takehome.drama.domain.usecase.library.ObserveSavedShowsUseCase
import com.takehome.drama.domain.usecase.library.SetShowSavedUseCase
import com.takehome.drama.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.takehome.drama.domain.usecase.onboarding.GetLanguageChoiceUseCase
import com.takehome.drama.domain.usecase.onboarding.GetMoodOptionsUseCase
import com.takehome.drama.domain.usecase.onboarding.IsOnboardingDoneUseCase
import com.takehome.drama.domain.usecase.onboarding.SaveTasteUseCase
import com.takehome.drama.domain.usecase.onboarding.SetContentLanguageUseCase
import com.takehome.drama.domain.usecase.reels.GetReelClipUseCase
import com.takehome.drama.domain.usecase.reels.GetReelsFeedUseCase
import com.takehome.drama.domain.usecase.search.GetPopularShowsUseCase
import com.takehome.drama.domain.usecase.search.SearchShowsUseCase
import com.takehome.drama.domain.usecase.settings.ObserveThemeUseCase
import com.takehome.drama.domain.usecase.settings.SetThemeUseCase
import com.takehome.drama.domain.usecase.watch.GetEndActionUseCase
import com.takehome.drama.domain.usecase.watch.GetEpisodeGridUseCase
import com.takehome.drama.domain.usecase.watch.GetEpisodeRangesUseCase
import com.takehome.drama.domain.usecase.watch.GetShowEpisodesUseCase
import com.takehome.drama.domain.usecase.watch.ObserveUnlockedEpisodesUseCase
import com.takehome.drama.domain.usecase.watch.PickVideoSourceUseCase
import com.takehome.drama.domain.usecase.watch.UnlockEpisodeUseCase
import java.io.File
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.binds
import org.koin.dsl.module

/**
 * Thứ tự khai: hạ tầng → datasource → repository → use case. Datasource và repository bind theo
 * interface (`single<X> { XImpl(...) }`); use case là `factory` (không giữ state).
 */
val dataModule = module {
    // Hạ tầng
    single { ApiConfig(baseUrl = get<AppConfig>().baseUrl) }
    single { provideHttpClient(apiConfig = get(), enableLogging = get<AppConfig>().isDebug) }
    single { Room.databaseBuilder(androidContext(), AppDatabase::class.java, AppDatabase.NAME).build() }
    single { get<AppDatabase>().watchProgressDao() }
    single { get<AppDatabase>().savedShowDao() }

    // Datasource — flavor dev đọc fixtures trong assets, prod gọi API.
    single { DebugCatalogFault() }
    single<CatalogDataSource> {
        val config = get<AppConfig>()
        val source = if (config.useFixtures) {
            CatalogAssetDataSourceImpl(androidContext().assets, defaultJson)
        } else {
            CatalogRemoteDataSourceImpl(get())
        }
        // Dev: cho launcher debug ép tải chậm / lỗi mạng để chụp SCR-0601 / SCR-0603.
        if (config.isDevFlavor) FaultInjectingCatalogDataSource(source, get()) else source
    }
    single<CatalogCacheLocalDataSource> {
        CatalogCacheLocalDataSourceImpl(File(androidContext().filesDir, "catalog_cache"), defaultJson)
    }
    single<WatchProgressLocalDataSource> { WatchProgressLocalDataSourceImpl(get()) }
    single<SavedShowLocalDataSource> { SavedShowLocalDataSourceImpl(get()) }
    single<UserPreferencesLocalDataSource> { UserPreferencesLocalDataSourceImpl(androidContext()) }
    single<EpisodeUnlockLocalDataSource> { EpisodeUnlockLocalDataSourceImpl(androidContext().episodeUnlockDataStore) }

    // Repository — CatalogRepositoryImpl giữ cache in-memory nên bắt buộc single.
    single<CatalogRepository> { CatalogRepositoryImpl(get(), get()) }
    single<WatchProgressRepository> { WatchProgressRepositoryImpl(get()) }
    single<SavedShowRepository> { SavedShowRepositoryImpl(get()) }
    single<UserPreferencesRepository> { UserPreferencesRepositoryImpl(get()) }
    single<EpisodeUnlockRepository> { EpisodeUnlockRepositoryImpl(get()) }
    // AdsConfigRepository + use case ads bind ở adsModule (:core:ads) — impl đọc Remote Config.

    // Use case — mỗi hành động một class (`operator fun invoke`), không giữ state → factory.
    factory { GetCatalogUseCase(get()) }
    factory { ResolveContentLanguageUseCase() }
    factory { GetShowEpisodesUseCase(get()) }
    factory { PickVideoSourceUseCase() }
    factory { GetEpisodeGridUseCase() }
    factory { GetEpisodeRangesUseCase() }
    factory { GetEndActionUseCase() }
    factory { ObserveUnlockedEpisodesUseCase(get()) }
    factory { UnlockEpisodeUseCase(get()) }
    factory { SaveWatchProgressUseCase(get()) }
    factory { GetResumePositionUseCase(get()) }
    factory { GetContinueTargetUseCase(get()) }
    factory { ObserveWatchHistoryUseCase(get()) }
    factory { ObserveShowProgressUseCase(get()) }
    factory { RemoveFromHistoryUseCase(get()) }
    factory { GetContinueEpisodeUseCase(get(), get()) }
    factory { ObserveLibrarySummaryUseCase(get(), get()) }
    factory { SetShowSavedUseCase(get()) }
    factory { ObserveIsShowSavedUseCase(get()) }
    factory { ObserveSavedShowsUseCase(get()) }
    factory { GetHomeFeedUseCase() }
    factory { GetMoodOptionsUseCase() }
    factory { SaveTasteUseCase(get()) }
    factory { GetLanguageChoiceUseCase(get(), get()) }
    factory { SetContentLanguageUseCase(get()) }
    factory { IsOnboardingDoneUseCase(get()) }
    factory { CompleteOnboardingUseCase(get()) }
    factory { SearchShowsUseCase() }
    factory { GetPopularShowsUseCase() }
    factory { GetReelsFeedUseCase() }
    factory { GetReelClipUseCase(get(), get()) }
    factory { ObserveThemeUseCase(get()) }
    factory { SetThemeUseCase(get()) }

    // Đồng hồ máy cho Day Cycle + History — single để flavor dev ép giờ một lần cho cả phiên.
    single { SystemWallClock() } binds arrayOf(WallClock::class, EpochClock::class)
}
