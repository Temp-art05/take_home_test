package com.takehome.drama.core.media

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * [forceLowTier] để flavor dev ép chính sách máy yếu khi kiểm/chụp. `SimpleCache` chỉ được **một**
 * instance mỗi thư mục → `single`, player lẫn preload đọc qua cùng cache: vuốt ngược lại hay mở lại
 * tập phát từ đĩa, không tải lại.
 */
@OptIn(UnstableApi::class)
fun mediaModule(forceLowTier: () -> Boolean = { false }) = module {
    single { DeviceTierDetector(androidContext(), forceLowTier) }
    single { MediaProfile.forTier(get<DeviceTierDetector>().detect()) }
    single<Cache> {
        val context = androidContext()
        SimpleCache(
            File(context.cacheDir, "media"),
            LeastRecentlyUsedCacheEvictor(get<MediaProfile>().cacheBytes),
            StandaloneDatabaseProvider(context),
        )
    }
    single<DataSource.Factory> {
        CacheDataSource.Factory()
            .setCache(get())
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(androidContext()))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
    single<EpisodeFeedPlayerFactory> {
        val context = androidContext()
        val profile = get<MediaProfile>()
        val dataSourceFactory = get<DataSource.Factory>()
        EpisodeFeedPlayerFactory { ExoEpisodeFeedPlayer(context, profile, dataSourceFactory) }
    }
}
