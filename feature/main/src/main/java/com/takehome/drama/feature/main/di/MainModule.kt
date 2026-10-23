package com.takehome.drama.feature.main.di

import com.takehome.drama.feature.main.presentation.language.LanguageViewModel
import com.takehome.drama.feature.main.presentation.main.MainViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Mỗi feature tự khai báo DI của mình — :app không phải biết từng ViewModel. */
val mainModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::LanguageViewModel)
}
