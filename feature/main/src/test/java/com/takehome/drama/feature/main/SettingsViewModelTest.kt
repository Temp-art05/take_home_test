package com.takehome.drama.feature.main

import app.cash.turbine.test
import com.takehome.drama.domain.fake.FakeUserPreferencesRepository
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.domain.fake.testAppConfig
import com.takehome.drama.domain.model.ThemeMode
import com.takehome.drama.domain.usecase.settings.ObserveThemeUseCase
import com.takehome.drama.domain.usecase.settings.SetThemeUseCase
import com.takehome.drama.feature.main.presentation.settings.SettingsIntent
import com.takehome.drama.feature.main.presentation.settings.SettingsSingleEvent
import com.takehome.drama.feature.main.presentation.settings.SettingsViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val preferences = FakeUserPreferencesRepository()

    private fun viewModel() = SettingsViewModel(
        observeTheme = ObserveThemeUseCase(preferences),
        setTheme = SetThemeUseCase(preferences),
        appConfig = testAppConfig,
    )

    @Test
    fun `shows the stored theme and the real version`() = runTest {
        val state = viewModel().viewState.value
        assertEquals(ThemeMode.Dark, state.theme)
        assertEquals("1.0.0", state.versionName)
        assertEquals(1, state.versionCode)
    }

    @Test
    fun `picking the current theme writes nothing, another theme writes once even when tapped twice`() = runTest {
        val vm = viewModel()
        vm.processIntent(SettingsIntent.SelectTheme(ThemeMode.Dark))
        assertEquals(0, preferences.themeWrites)

        preferences.writeGate = CompletableDeferred()
        vm.processIntent(SettingsIntent.SelectTheme(ThemeMode.Light))
        vm.processIntent(SettingsIntent.SelectTheme(ThemeMode.Light))
        preferences.writeGate?.complete(Unit)
        assertEquals(1, preferences.themeWrites)
        assertEquals(ThemeMode.Light, vm.viewState.value.theme)
    }

    @Test
    fun `a failed theme write puts the check back and shows an error toast`() = runTest {
        val vm = viewModel()
        preferences.themeFailure = IllegalStateException("disk full")
        vm.singleEvent.test {
            vm.processIntent(SettingsIntent.SelectTheme(ThemeMode.Light))
            assertEquals(false, (awaitItem() as SettingsSingleEvent.ShowToast).success)
        }
        assertEquals(ThemeMode.Dark, vm.viewState.value.theme)

        // Không kẹt cờ đang ghi: thử lại được.
        preferences.themeFailure = null
        vm.processIntent(SettingsIntent.SelectTheme(ThemeMode.Light))
        assertEquals(ThemeMode.Light, vm.viewState.value.theme)
        assertEquals(2, preferences.themeWrites)
    }

    @Test
    fun `terms and privacy have no url yet - info toast, licenses open a screen`() = runTest {
        val vm = viewModel()
        vm.singleEvent.test {
            vm.processIntent(SettingsIntent.OpenTerms)
            assertEquals(false, (awaitItem() as SettingsSingleEvent.ShowToast).success)
            vm.processIntent(SettingsIntent.OpenPrivacy)
            assertEquals(false, (awaitItem() as SettingsSingleEvent.ShowToast).success)
            vm.processIntent(SettingsIntent.OpenLicenses)
            assertEquals(SettingsSingleEvent.OpenLicenses, awaitItem())
            vm.processIntent(SettingsIntent.OpenLanguage)
            assertEquals(SettingsSingleEvent.OpenLanguage, awaitItem())
        }
    }
}
