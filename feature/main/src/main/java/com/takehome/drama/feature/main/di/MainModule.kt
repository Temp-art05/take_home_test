package com.takehome.drama.feature.main.di

import com.takehome.drama.feature.main.presentation.main.browse.BrowseViewModel
import com.takehome.drama.feature.main.presentation.history.HistoryViewModel
import com.takehome.drama.feature.main.presentation.main.library.LibraryViewModel
import com.takehome.drama.feature.main.presentation.licenses.LicensesViewModel
import com.takehome.drama.feature.main.presentation.main.MainTabRequests
import com.takehome.drama.feature.main.presentation.saved.SavedViewModel
import com.takehome.drama.feature.main.presentation.main.settings.SettingsViewModel
import com.takehome.drama.feature.main.presentation.main.home.HomeViewModel
import com.takehome.drama.feature.main.presentation.language.LanguageViewModel
import com.takehome.drama.feature.main.presentation.main.MainViewModel
import com.takehome.drama.feature.main.presentation.main.reels.ReelsViewModel
import com.takehome.drama.feature.main.presentation.search.SearchViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Mỗi feature tự khai báo DI của mình — :app không phải biết từng ViewModel. */
val mainModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::BrowseViewModel)
    viewModelOf(::ReelsViewModel)
    viewModelOf(::LanguageViewModel)
    viewModelOf(::LibraryViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::SavedViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::LicensesViewModel)
    // Single: History/Saved ghi yêu cầu đổi tab, MainViewModel (back stack khác) đọc.
    singleOf(::MainTabRequests)
}
