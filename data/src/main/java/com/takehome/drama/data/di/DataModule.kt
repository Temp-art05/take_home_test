package com.takehome.drama.data.di

import androidx.room.Room
import com.takehome.drama.data.local.database.AppDatabase
import com.takehome.drama.data.local.datasource.SavedShowLocalDataSource
import com.takehome.drama.data.local.datasource.SavedShowLocalDataSourceImpl
import com.takehome.drama.data.local.datasource.UserPreferencesLocalDataSource
import com.takehome.drama.data.local.datasource.UserPreferencesLocalDataSourceImpl
import com.takehome.drama.data.local.datasource.WatchProgressLocalDataSource
import com.takehome.drama.data.local.datasource.WatchProgressLocalDataSourceImpl
import com.takehome.drama.data.remote.datasource.CatalogAssetDataSourceImpl
import com.takehome.drama.data.remote.datasource.CatalogDataSource
import com.takehome.drama.data.remote.datasource.CatalogRemoteDataSourceImpl
import com.takehome.drama.data.remote.http.ApiConfig
import com.takehome.drama.data.remote.http.defaultJson
import com.takehome.drama.data.remote.http.provideHttpClient
import com.takehome.drama.data.repository.CatalogRepositoryImpl
import com.takehome.drama.data.repository.SavedShowRepositoryImpl
import com.takehome.drama.data.repository.UserPreferencesRepositoryImpl
import com.takehome.drama.data.repository.WatchProgressRepositoryImpl
import com.takehome.drama.domain.config.AppConfig
import com.takehome.drama.domain.repository.CatalogRepository
import com.takehome.drama.domain.repository.SavedShowRepository
import com.takehome.drama.domain.repository.UserPreferencesRepository
import com.takehome.drama.domain.repository.WatchProgressRepository
import com.takehome.drama.domain.usecase.catalog.CatalogUseCase
import com.takehome.drama.domain.usecase.history.WatchProgressUseCase
import com.takehome.drama.domain.usecase.library.SavedShowsUseCase
import com.takehome.drama.domain.usecase.watch.EpisodesUseCase
import org.koin.android.ext.koin.androidContext
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
    single<CatalogDataSource> {
        if (get<AppConfig>().useFixtures) {
            CatalogAssetDataSourceImpl(androidContext().assets, defaultJson)
        } else {
            CatalogRemoteDataSourceImpl(get())
        }
    }
    single<WatchProgressLocalDataSource> { WatchProgressLocalDataSourceImpl(get()) }
    single<SavedShowLocalDataSource> { SavedShowLocalDataSourceImpl(get()) }
    single<UserPreferencesLocalDataSource> { UserPreferencesLocalDataSourceImpl(androidContext()) }

    // Repository — CatalogRepositoryImpl giữ cache in-memory nên bắt buộc single.
    single<CatalogRepository> { CatalogRepositoryImpl(get()) }
    single<WatchProgressRepository> { WatchProgressRepositoryImpl(get()) }
    single<SavedShowRepository> { SavedShowRepositoryImpl(get()) }
    single<UserPreferencesRepository> { UserPreferencesRepositoryImpl(get()) }

    // Use case
    factory { CatalogUseCase(get()) }
    factory { EpisodesUseCase(get()) }
    factory { WatchProgressUseCase(get()) }
    factory { SavedShowsUseCase(get()) }
}
