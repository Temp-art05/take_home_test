package com.takehome.drama.data.di

import androidx.room.Room
import com.takehome.drama.data.local.database.AppDatabase
import com.takehome.drama.data.local.datasource.BookmarkLocalDataSource
import com.takehome.drama.data.local.datasource.BookmarkLocalDataSourceImpl
import com.takehome.drama.data.remote.datasource.PostRemoteDataSource
import com.takehome.drama.data.remote.datasource.PostRemoteDataSourceImpl
import com.takehome.drama.data.remote.http.ApiConfig
import com.takehome.drama.data.remote.http.provideHttpClient
import com.takehome.drama.data.repository.PostRepositoryImpl
import com.takehome.drama.domain.config.AppConfig
import com.takehome.drama.domain.repository.PostRepository
import com.takehome.drama.domain.usecase.post.PostFeedUseCase
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
    single { get<AppDatabase>().bookmarkDao() }

    // DataSource (SAMPLE)
    single<PostRemoteDataSource> { PostRemoteDataSourceImpl(get()) }
    single<BookmarkLocalDataSource> { BookmarkLocalDataSourceImpl(get()) }

    // Repository (SAMPLE)
    single<PostRepository> { PostRepositoryImpl(get(), get()) }

    // UseCase (SAMPLE)
    factory { PostFeedUseCase(get()) }
}
