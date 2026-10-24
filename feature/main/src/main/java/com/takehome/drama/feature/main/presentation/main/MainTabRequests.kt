package com.takehome.drama.feature.main.presentation.main

import com.takehome.drama.feature.main.presentation.main.model.MainTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Màn con của Main (History, Saved) xin đổi tab rồi quay về Main — không chồng thêm một Main lên back
 * stack. Koin single: [MainViewModel] nhận và [consume] đúng một lần.
 */
class MainTabRequests {
    private val _pending = MutableStateFlow<MainTab?>(null)
    val pending: StateFlow<MainTab?> = _pending.asStateFlow()

    fun request(tab: MainTab) {
        _pending.value = tab
    }

    fun consume(tab: MainTab) {
        _pending.compareAndSet(tab, null)
    }
}
