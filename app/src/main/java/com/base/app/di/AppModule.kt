package com.base.app.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Scope sống theo process, cho việc chạy nền không gắn với màn hình nào. */
val AppScope = named("AppScope")

val appModule = module {
    single(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
}
