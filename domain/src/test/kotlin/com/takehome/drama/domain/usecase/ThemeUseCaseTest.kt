package com.takehome.drama.domain.usecase

import app.cash.turbine.test
import com.takehome.drama.domain.fake.FakeUserPreferencesRepository
import com.takehome.drama.domain.model.ThemeMode
import com.takehome.drama.domain.usecase.settings.ObserveThemeUseCase
import com.takehome.drama.domain.usecase.settings.SetThemeUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeUseCaseTest {

    private val preferences = FakeUserPreferencesRepository()

    @Test
    fun `theme defaults to dark and emits only on change`() = runTest {
        ObserveThemeUseCase(preferences)().test {
            assertEquals(ThemeMode.Dark, awaitItem())
            preferences.setContentLanguage("ar")
            SetThemeUseCase(preferences)(ThemeMode.Light)
            assertEquals(ThemeMode.Light, awaitItem())
            expectNoEvents()
        }
    }
}
