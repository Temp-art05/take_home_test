package com.takehome.drama.feature.main.presentation.settings

import androidx.lifecycle.viewModelScope
import com.takehome.drama.core.ui.foundation.error.toErrorRes
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.config.AppConfig
import com.takehome.drama.domain.model.ThemeMode
import com.takehome.drama.domain.usecase.settings.ObserveThemeUseCase
import com.takehome.drama.domain.usecase.settings.SetThemeUseCase
import com.takehome.drama.feature.main.R
import kotlinx.coroutines.launch

/**
 * SCR-0501. Ngôn ngữ app không ở đây: `AppLocale` là nguồn sự thật (hệ thống / AppCompat persist), Screen đọc
 * thẳng. Đổi theme làm cả app vẽ lại — trùng hoặc đang ghi thì bỏ qua để tap lặp không nháy.
 */
class SettingsViewModel(
    private val observeTheme: ObserveThemeUseCase,
    private val setTheme: SetThemeUseCase,
    private val appConfig: AppConfig,
) : BaseViewModel<SettingsIntent, SettingsViewState, SettingsSingleEvent>() {

    private var writingTheme: ThemeMode? = null

    override fun initState() = SettingsViewState(versionName = appConfig.versionName, versionCode = appConfig.versionCode)

    init {
        viewModelScope.launch {
            observeTheme().collect { theme -> updateState { copy(theme = theme) } }
        }
    }

    override fun processIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.OpenLanguage -> sendEvent(SettingsSingleEvent.OpenLanguage)
            is SettingsIntent.SelectTheme -> selectTheme(intent.theme)
            // Đề không cho URL chính sách — không bịa link; báo trung tính.
            SettingsIntent.OpenTerms, SettingsIntent.OpenPrivacy ->
                sendEvent(SettingsSingleEvent.ShowToast(R.string.settings_policy_unavailable, success = false))
            SettingsIntent.OpenLicenses -> sendEvent(SettingsSingleEvent.OpenLicenses)
        }
    }

    private fun selectTheme(theme: ThemeMode) {
        if (theme == currentState.theme || writingTheme != null) return
        val previous = currentState.theme
        writingTheme = theme
        // ✓ đổi ngay, không chờ DataStore. Ghi lỗi thì trả ✓ về: theme thật vẫn là cái cũ và
        // ObserveThemeUseCase (distinctUntilChanged) sẽ không phát lại để tự sửa.
        updateState { copy(theme = theme) }
        viewModelScope.launch {
            setTheme(theme).onFailure { error ->
                updateState { copy(theme = previous) }
                sendEvent(SettingsSingleEvent.ShowToast(error.toErrorRes(), success = false))
            }
            writingTheme = null
        }
    }
}
