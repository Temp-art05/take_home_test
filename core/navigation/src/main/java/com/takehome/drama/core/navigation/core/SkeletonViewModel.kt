package com.takehome.drama.core.navigation.core

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * ViewModel rỗng cho destination mới chỉ có khung (`ScreenSkeleton`). [ScreenProvider] bắt buộc có
 * ViewModel; dựng màn thật thì đổi sang ViewModel riêng của màn. Hết chỗ dùng thì xoá cả file.
 *
 * [savedStateHandle] để khung đọc arg của destination (`savedStateHandle.toRoute<T>()`) đúng entry của
 * mình — đọc `currentBackStackEntry` sẽ lấy nhầm entry đích trong lúc animation chuyển màn.
 */
class SkeletonViewModel(val savedStateHandle: SavedStateHandle) : ViewModel()

val skeletonModule = module {
    viewModelOf(::SkeletonViewModel)
}
