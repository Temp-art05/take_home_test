package com.takehome.drama.di

import com.takehome.drama.core.utils.network.AndroidConnectivityMonitor
import com.takehome.drama.core.utils.network.ConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Scope sống theo process, cho việc chạy nền không gắn với màn hình nào. */
val AppScope = named("AppScope")

val appModule = module {
    single(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    // Một network callback cho cả process (rule "autoplay preview Wi-Fi only" của Browse).
    single<ConnectivityMonitor> { AndroidConnectivityMonitor(androidContext()) }
    single { ThemeHolder(get(), get(AppScope)) }
}
