package com.base.app.feature.main.di

import com.base.app.feature.main.presentation.feature.FeatureViewModel
import com.base.app.feature.main.presentation.home.HomeViewModel
import com.base.app.feature.main.presentation.main.MainViewModel
import com.base.app.feature.main.presentation.profile.ProfileViewModel
import com.base.app.feature.main.presentation.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Mỗi feature tự khai báo DI của mình — :app không phải biết từng ViewModel. */
val mainModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::FeatureViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::SettingsViewModel)
}
