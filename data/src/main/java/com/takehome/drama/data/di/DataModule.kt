package com.takehome.drama.data.di

import com.takehome.drama.data.remote.http.ApiConfig
import com.takehome.drama.data.remote.http.provideHttpClient
import com.takehome.drama.domain.config.AppConfig
import org.koin.dsl.module

/**
 * Thứ tự khai: hạ tầng → datasource → repository → use case. Datasource và repository bind theo
 * interface (`single<X> { XImpl(...) }`); use case là `factory` (không giữ state).
 */
val dataModule = module {
    // Hạ tầng
    single { ApiConfig(baseUrl = get<AppConfig>().baseUrl) }
    single { provideHttpClient(apiConfig = get(), enableLogging = get<AppConfig>().isDebug) }
}
