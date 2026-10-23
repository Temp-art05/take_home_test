package com.base.app.data.di

import androidx.room.Room
import com.base.app.data.local.database.AppDatabase
import com.base.app.data.local.datasource.BookmarkLocalDataSource
import com.base.app.data.local.datasource.BookmarkLocalDataSourceImpl
import com.base.app.data.remote.datasource.PostRemoteDataSource
import com.base.app.data.remote.datasource.PostRemoteDataSourceImpl
import com.base.app.data.remote.http.ApiConfig
import com.base.app.data.remote.http.provideHttpClient
import com.base.app.data.repository.PostRepositoryImpl
import com.base.app.domain.config.AppConfig
import com.base.app.domain.repository.PostRepository
import com.base.app.domain.usecase.post.PostFeedUseCase
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
