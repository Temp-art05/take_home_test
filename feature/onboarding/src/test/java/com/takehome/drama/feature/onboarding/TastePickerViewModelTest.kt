package com.takehome.drama.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.takehome.drama.domain.fake.FakeCatalogRepository
import com.takehome.drama.domain.fake.FakeUserPreferencesRepository
import com.takehome.drama.domain.fake.MainDispatcherRule
import com.takehome.drama.domain.fake.catalog
import com.takehome.drama.domain.fake.tag
import com.takehome.drama.domain.error.DomainException
import com.takehome.drama.core.ui.foundation.error.ErrorKind
import com.takehome.drama.domain.model.UserPreferences
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.takehome.drama.domain.usecase.onboarding.GetMoodOptionsUseCase
import com.takehome.drama.domain.usecase.onboarding.Mood
import com.takehome.drama.domain.usecase.onboarding.SaveTasteUseCase
import com.takehome.drama.feature.onboarding.presentation.taste.TastePickerIntent
import com.takehome.drama.feature.onboarding.presentation.taste.TastePickerSingleEvent
import com.takehome.drama.feature.onboarding.presentation.taste.TastePickerViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TastePickerViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val tags = listOf("revenge", "comeback", "urban", "love", "ceo", "plot-twist", "fantasy", "comedy")
        .mapIndexed { i, id -> tag(id, isCategory = true, orderPriority = i) }
    private val preferences = FakeUserPreferencesRepository(UserPreferences())
    private val savedState = SavedStateHandle()

    private val catalogRepository = FakeCatalogRepository(Result.success(catalog(emptyList(), tags = tags)))

    private fun viewModel() = TastePickerViewModel(
        savedStateHandle = savedState,
        getCatalog = GetCatalogUseCase(catalogRepository),
        getMoodOptions = GetMoodOptionsUseCase(),
        saveTaste = SaveTasteUseCase(preferences),
        completeOnboarding = CompleteOnboardingUseCase(preferences),
        resolveContentLanguage = ResolveContentLanguageUseCase(),
        userPreferencesRepository = preferences,
    )

    private val rage = setOf("revenge", "comeback", "urban")

    @Test
    fun `picking a mood switches on its chips and enables continue`() = runTest {
        val vm = viewModel()
        assertFalse(vm.viewState.value.canContinue)
        vm.processIntent(TastePickerIntent.ToggleMood(Mood.Rage))
        assertEquals(rage, vm.viewState.value.selectedTags)
        assertTrue(vm.viewState.value.canContinue)
    }

    @Test
    fun `switching off every chip of the picked mood disables continue`() = runTest {
        val vm = viewModel()
        vm.processIntent(TastePickerIntent.ToggleMood(Mood.Rage))
        rage.forEach { vm.processIntent(TastePickerIntent.ToggleTag(it)) }
        assertFalse(vm.viewState.value.canContinue)
    }

    @Test
    fun `double tap on continue navigates once`() = runTest {
        val vm = viewModel()
        vm.processIntent(TastePickerIntent.ToggleMood(Mood.Thrill))
        val gate = CompletableDeferred<Unit>()
        preferences.writeGate = gate
        vm.singleEvent.test {
            vm.processIntent(TastePickerIntent.Continue(sdkInt = 34, notificationsGranted = false))
            vm.processIntent(TastePickerIntent.Continue(sdkInt = 34, notificationsGranted = false))
            gate.complete(Unit)
            assertEquals(TastePickerSingleEvent.Next(showNotificationPrompt = true), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `mood subtitles name real catalogue tags`() = runTest {
        val rageItem = viewModel().viewState.value.moods.first { it.mood == Mood.Rage }
        assertEquals(listOf("revenge", "comeback"), rageItem.tagNames)
    }

    @Test
    fun `dropping a mood switches its chips off but keeps chips picked by hand`() = runTest {
        val vm = viewModel()
        vm.processIntent(TastePickerIntent.ToggleTag("comedy"))
        vm.processIntent(TastePickerIntent.ToggleMood(Mood.Rage))
        vm.processIntent(TastePickerIntent.ToggleTag("urban"))
        assertEquals(setOf("comedy", "revenge", "comeback"), vm.viewState.value.selectedTags)
        vm.processIntent(TastePickerIntent.ToggleMood(Mood.Rage))
        assertEquals(setOf("comedy"), vm.viewState.value.selectedTags)
    }

    @Test
    fun `continue saves the taste and goes to the pre-prompt on Android 13 without permission`() = runTest {
        val vm = viewModel()
        vm.processIntent(TastePickerIntent.ToggleMood(Mood.Thrill))
        vm.singleEvent.test {
            vm.processIntent(TastePickerIntent.Continue(sdkInt = 34, notificationsGranted = false))
            assertEquals(TastePickerSingleEvent.Next(showNotificationPrompt = true), awaitItem())
        }
        assertEquals(setOf("plot-twist", "fantasy"), preferences.state.value.tasteTagIds)
        assertFalse(preferences.state.value.onboardingDone)
    }

    @Test
    fun `below Android 13 onboarding finishes here`() = runTest {
        val vm = viewModel()
        vm.processIntent(TastePickerIntent.ToggleMood(Mood.Swoon))
        vm.singleEvent.test {
            vm.processIntent(TastePickerIntent.Continue(sdkInt = 32, notificationsGranted = false))
            assertEquals(TastePickerSingleEvent.Next(showNotificationPrompt = false), awaitItem())
        }
        assertTrue(preferences.state.value.onboardingDone)
    }

    @Test
    fun `skip saves an empty taste`() = runTest {
        preferences.state.value = UserPreferences(tasteTagIds = setOf("old"))
        val vm = viewModel()
        vm.processIntent(TastePickerIntent.ToggleMood(Mood.Rage))
        vm.singleEvent.test {
            vm.processIntent(TastePickerIntent.Skip(sdkInt = 34, notificationsGranted = true))
            assertEquals(TastePickerSingleEvent.Next(showNotificationPrompt = false), awaitItem())
        }
        assertEquals(emptySet<String>(), preferences.state.value.tasteTagIds)
    }

    @Test
    fun `chips collapse to six with a more chip that expands in place`() = runTest {
        val vm = viewModel()
        assertEquals(6, vm.viewState.value.visibleChips.size)
        assertEquals(2, vm.viewState.value.hiddenChipCount)
        vm.processIntent(TastePickerIntent.ExpandChips)
        assertEquals(8, vm.viewState.value.visibleChips.size)
        assertEquals(0, vm.viewState.value.hiddenChipCount)
    }

    @Test
    fun `toggling never reorders chips`() = runTest {
        val vm = viewModel()
        vm.processIntent(TastePickerIntent.ExpandChips)
        val order = vm.viewState.value.chips.map { it.id }
        assertEquals(tags.map { it.id }, order)
        vm.processIntent(TastePickerIntent.ToggleTag("comedy"))
        vm.processIntent(TastePickerIntent.ToggleMood(Mood.Rage))
        assertEquals(order, vm.viewState.value.chips.map { it.id })
    }

    @Test
    fun `a mood whose chip is collapsed expands the chips in place`() = runTest {
        val vm = viewModel()
        vm.processIntent(TastePickerIntent.ToggleMood(Mood.Thrill))
        val state = vm.viewState.value
        assertTrue("fantasy" in state.selectedTags)
        assertTrue(state.visibleChips.any { it.id == "fantasy" })
        assertEquals(0, state.hiddenChipCount)
    }

    @Test
    fun `selection survives process death`() = runTest {
        viewModel().processIntent(TastePickerIntent.ToggleMood(Mood.Rage))
        val restored = viewModel().viewState.value
        assertEquals(setOf(Mood.Rage), restored.selectedMoods)
        assertEquals(rage, restored.selectedTags)
    }

    @Test
    fun `offline with no catalogue shows the offline state instead of an empty grid`() = runTest {
        catalogRepository.catalog = Result.failure(DomainException.Network())
        val state = viewModel().viewState.value
        assertEquals(ErrorKind.Offline, state.error?.kind)
        assertTrue(state.moods.isEmpty())
    }

    @Test
    fun `retry after reconnecting loads the moods`() = runTest {
        catalogRepository.catalog = Result.failure(DomainException.Network())
        val vm = viewModel()

        catalogRepository.catalog = Result.success(catalog(emptyList(), tags = tags))
        vm.processIntent(TastePickerIntent.Retry)

        assertEquals(null, vm.viewState.value.error)
        assertTrue(vm.viewState.value.moods.isNotEmpty())
        assertEquals(2, catalogRepository.catalogCalls)
    }
}
