package com.takehome.drama.feature.watch.di

import com.takehome.drama.feature.watch.presentation.detail.ShowDetailViewModel
import com.takehome.drama.feature.watch.presentation.player.PlayerViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val watchModule = module {
    viewModelOf(::ShowDetailViewModel)
    viewModelOf(::PlayerViewModel)
}
