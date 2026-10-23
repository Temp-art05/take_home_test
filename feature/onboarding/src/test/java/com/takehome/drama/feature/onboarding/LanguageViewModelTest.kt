package com.takehome.drama.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.takehome.drama.domain.fake.FakeCatalogRepository
import com.takehome.drama.domain.fake.FakeUserPreferencesRepository
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.domain.fake.catalog
import com.takehome.drama.core.ui.foundation.locale.AppLocale
import com.takehome.drama.domain.model.UserPreferences
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.onboarding.GetLanguageChoiceUseCase
import com.takehome.drama.domain.usecase.onboarding.SetContentLanguageUseCase
import com.takehome.drama.feature.onboarding.presentation.language.OnboardingLanguageIntent
import com.takehome.drama.feature.onboarding.presentation.language.OnboardingLanguageSingleEvent
import com.takehome.drama.feature.onboarding.presentation.language.OnboardingLanguageViewModel
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LanguageViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val catalogRepository = FakeCatalogRepository(Result.success(catalog(emptyList(), availableLanguages = listOf("en", "ar", "hi"))))
    private val preferences = FakeUserPreferencesRepository(UserPreferences())
    private val savedState = SavedStateHandle()
    private lateinit var deviceLocale: Locale

    @Before
    fun setUp() {
        deviceLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("hi-IN"))
    }

    @After
    fun tearDown() = Locale.setDefault(deviceLocale)

    private fun viewModel() = OnboardingLanguageViewModel(
        savedStateHandle = savedState,
        getCatalog = GetCatalogUseCase(catalogRepository),
        getLanguageChoice = GetLanguageChoiceUseCase(preferences, ResolveContentLanguageUseCase()),
        setContentLanguage = SetContentLanguageUseCase(preferences),
    )

    @Test
    fun `preselects the device language and pins it first`() = runTest {
        val state = viewModel().viewState.value
        assertEquals("hi", state.selected)
        assertEquals(listOf("hi", "en", "ta", "id", "ar", "fr", "es", "pt-BR", "tr", "de"), state.options.map { it.code })
    }

    @Test
    fun `continue saves the picked content language and hands its ui locale to the screen`() = runTest {
        val vm = viewModel()
        vm.processIntent(OnboardingLanguageIntent.Select("ar"))
        vm.singleEvent.test {
            vm.processIntent(OnboardingLanguageIntent.Continue)
            assertEquals(OnboardingLanguageSingleEvent.Next(AppLocale.Arabic), awaitItem())
        }
        assertEquals("ar", preferences.state.value.contentLanguage)
    }

    @Test
    fun `double tap on continue navigates once, and again after coming back`() = runTest {
        val vm = viewModel()
        val gate = CompletableDeferred<Unit>()
        preferences.writeGate = gate
        vm.singleEvent.test {
            vm.processIntent(OnboardingLanguageIntent.Continue)
            vm.processIntent(OnboardingLanguageIntent.Continue)
            gate.complete(Unit)
            assertEquals(OnboardingLanguageSingleEvent.Next(AppLocale.Hindi), awaitItem())
            expectNoEvents()
            // Back từ SCR-0003 rồi bấm lại vẫn đi tiếp.
            vm.processIntent(OnboardingLanguageIntent.Continue)
            assertEquals(OnboardingLanguageSingleEvent.Next(AppLocale.Hindi), awaitItem())
        }
    }

    @Test
    fun `offline catalogue still lists the ten launch languages`() = runTest {
        catalogRepository.catalog = Result.failure(Exception("offline"))
        val state = viewModel().viewState.value
        assertEquals(10, state.options.size)
        assertEquals("hi", state.selected)
    }

    @Test
    fun `selection survives process death`() = runTest {
        viewModel().processIntent(OnboardingLanguageIntent.Select("ar"))
        assertEquals("ar", viewModel().viewState.value.selected)
    }
}
