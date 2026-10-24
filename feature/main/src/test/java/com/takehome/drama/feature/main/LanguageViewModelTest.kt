package com.takehome.drama.feature.main

import app.cash.turbine.test
import com.takehome.drama.core.ui.foundation.locale.AppLocale
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.feature.main.presentation.language.LanguageIntent
import com.takehome.drama.feature.main.presentation.language.LanguageSingleEvent
import com.takehome.drama.feature.main.presentation.language.LanguageViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LanguageViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `picking the selected language does nothing`() = runTest {
        val vm = LanguageViewModel()
        vm.singleEvent.test {
            vm.processIntent(LanguageIntent.SelectLocale(AppLocale.System))
            expectNoEvents()
        }
    }

    @Test
    fun `another language applies once even when tapped repeatedly`() = runTest {
        val vm = LanguageViewModel()
        vm.singleEvent.test {
            vm.processIntent(LanguageIntent.SelectLocale(AppLocale.Arabic))
            vm.processIntent(LanguageIntent.SelectLocale(AppLocale.Arabic))
            vm.processIntent(LanguageIntent.SelectLocale(AppLocale.French))
            assertEquals(LanguageSingleEvent.ApplyLocale(AppLocale.Arabic), awaitItem())
            expectNoEvents()
        }
        assertEquals(AppLocale.Arabic, vm.viewState.value.selected)
        assertTrue(vm.viewState.value.applying)
    }

    @Test
    fun `screen restart after the activity is recreated accepts taps again`() = runTest {
        val vm = LanguageViewModel()
        vm.processIntent(LanguageIntent.SelectLocale(AppLocale.Arabic))
        vm.processIntent(LanguageIntent.ScreenStarted)
        assertEquals(false, vm.viewState.value.applying)
    }

    @Test
    fun `displayed language change without a recreate accepts taps again`() = runTest {
        val vm = LanguageViewModel()
        vm.singleEvent.test {
            vm.processIntent(LanguageIntent.SelectLocale(AppLocale.French))
            assertEquals(LanguageSingleEvent.ApplyLocale(AppLocale.French), awaitItem())
            vm.processIntent(LanguageIntent.DisplayedLocaleChanged)
            assertEquals(false, vm.viewState.value.applying)
            vm.processIntent(LanguageIntent.SelectLocale(AppLocale.German))
            assertEquals(LanguageSingleEvent.ApplyLocale(AppLocale.German), awaitItem())
        }
    }

    @Test
    fun `apply that keeps the displayed language unlocks taps without a recreate`() = runTest {
        val vm = LanguageViewModel()
        vm.singleEvent.test {
            vm.processIntent(LanguageIntent.SelectLocale(AppLocale.English))
            assertEquals(LanguageSingleEvent.ApplyLocale(AppLocale.English), awaitItem())
            vm.processIntent(LanguageIntent.ApplyFinished)
            assertEquals(false, vm.viewState.value.applying)
            assertEquals(AppLocale.English, vm.viewState.value.selected)
            vm.processIntent(LanguageIntent.SelectLocale(AppLocale.French))
            assertEquals(LanguageSingleEvent.ApplyLocale(AppLocale.French), awaitItem())
        }
    }
}
