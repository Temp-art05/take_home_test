package com.takehome.drama.domain.usecase

import com.takehome.drama.domain.fake.FakeUserPreferencesRepository
import com.takehome.drama.domain.fake.catalog
import com.takehome.drama.domain.fake.tag
import com.takehome.drama.domain.model.UserPreferences
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.takehome.drama.domain.usecase.onboarding.GetLanguageChoiceUseCase
import com.takehome.drama.domain.usecase.onboarding.GetMoodOptionsUseCase
import com.takehome.drama.domain.usecase.onboarding.IsOnboardingDoneUseCase
import com.takehome.drama.domain.usecase.onboarding.Mood
import com.takehome.drama.domain.usecase.onboarding.NotificationPromptPolicy
import com.takehome.drama.domain.usecase.onboarding.SaveTasteUseCase
import com.takehome.drama.domain.usecase.onboarding.SetContentLanguageUseCase
import com.takehome.drama.domain.usecase.onboarding.TasteSelection
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTest {

    private val preferences = FakeUserPreferencesRepository(UserPreferences())
    private val moodOptions = GetMoodOptionsUseCase()
    private val saveTaste = SaveTasteUseCase(preferences)
    private val languageChoice = GetLanguageChoiceUseCase(preferences, ResolveContentLanguageUseCase())

    /** Đúng 15 genre của fixtures + 1 rail merchandising. */
    private val fixtureTags = listOf(
        "fantasy", "ceo", "sweet-romance", "comedy", "plot-twist", "urban", "contract-marriage",
        "marriage-first-love-later", "angsty-romance", "reunion", "love", "comeback", "revenge",
        "underdog-triumph", "redemption",
    ).mapIndexed { i, id -> tag(id, isCategory = true, orderPriority = 40 + i * 10) } +
        tag("trending", isCategory = false, orderPriority = 10)

    @Test
    fun `every mood maps only to tags that exist in the fixture taxonomy`() {
        val moods = moodOptions(catalog(emptyList(), tags = fixtureTags))
        assertEquals(Mood.entries, moods.map { it.mood })
        val known = fixtureTags.map { it.id }.toSet()
        moods.forEach { option -> assertTrue(option.tags.all { it.id in known }) }
        assertEquals(Mood.Rage.tagIds, moods.first { it.mood == Mood.Rage }.tags.map { it.id })
    }

    @Test
    fun `tags missing from the catalogue are filtered and a mood left empty is hidden`() {
        val tags = listOf(tag("revenge", true, 1), tag("love", true, 2))
        val moods = moodOptions(catalog(emptyList(), tags = tags))
        assertEquals(listOf(Mood.Rage, Mood.Swoon), moods.map { it.mood })
        assertEquals(listOf("revenge"), moods.first().tags.map { it.id })
    }

    @Test
    fun `taste is the union of mood clusters and hand-picked chips, minus chips switched off`() {
        val moods = moodOptions(catalog(emptyList(), tags = fixtureTags))
        val thrill = moods.first { it.mood == Mood.Thrill }
        var selection = TasteSelection().toggleTag("comedy", moods).toggleTag("fantasy", moods).toggleMood(thrill)
        assertEquals(setOf("plot-twist", "fantasy", "comedy"), selection.tagIds(moods))
        selection = selection.toggleTag("plot-twist", moods)
        assertEquals(setOf("fantasy", "comedy"), selection.tagIds(moods))
        // Bỏ mood: chip của cụm tắt, chip bật tay (fantasy, comedy) giữ.
        selection = selection.toggleMood(thrill)
        assertEquals(setOf("fantasy", "comedy"), selection.tagIds(moods))
        // Chọn lại mood: cả cụm bật lại.
        assertEquals(setOf("plot-twist", "fantasy", "comedy"), selection.toggleMood(thrill).tagIds(moods))
        assertTrue(TasteSelection().isEmpty)
    }

    @Test
    fun `fine-tune chips are the genres in orderPriority`() {
        val chips = moodOptions.fineTuneTags(catalog(emptyList(), tags = fixtureTags))
        assertEquals(15, chips.size)
        assertEquals("fantasy", chips.first().id)
        assertFalse(chips.any { it.id == "trending" })
    }

    private val available = listOf("en", "ar", "hi", "id", "ta")

    @Test
    fun `language follows the device, pinned first, rest in launch order`() {
        val choice = languageChoice.choose(available, "en", deviceLanguage = "hi", saved = null)
        assertEquals("hi", choice.selected)
        assertEquals(listOf("hi", "en", "ta", "id", "ar", "fr", "es", "pt-BR", "tr", "de"), choice.options.map { it.code })
        assertEquals("हिन्दी", choice.options.first().label)
    }

    @Test
    fun `all ten launch languages are offered even when the catalogue lacks them`() {
        val choice = languageChoice.choose(available, "en", deviceLanguage = "pt-BR", saved = null)
        assertEquals("pt-BR", choice.selected)
        assertEquals(10, choice.options.size)
        assertEquals("Português (BR)", choice.options.first().label)
    }

    @Test
    fun `device language outside the launch list falls back to the default language`() {
        assertEquals("en", languageChoice.choose(available, "en", deviceLanguage = "ja", saved = null).selected)
    }

    @Test
    fun `a catalogue language outside the launch list is appended`() {
        val choice = languageChoice.choose(available + "vi", "en", deviceLanguage = "en", saved = null)
        assertEquals("vi", choice.options.last().code)
        assertEquals(11, choice.options.size)
    }

    @Test
    fun `a saved language wins over the device, region subtags match the base code`() {
        assertEquals("ar", languageChoice.choose(available, "en", deviceLanguage = "hi", saved = "ar").selected)
        assertEquals("id", languageChoice.choose(available, "en", deviceLanguage = "id-ID", saved = null).selected)
    }

    @Test
    fun `unknown codes get a readable name instead of crashing`() {
        assertEquals("Français", languageChoice.endonym("fr"))
        assertEquals("العربية", languageChoice.endonym("ar"))
    }

    @Test
    fun `taste, language and completion are persisted`() = runTest {
        saveTaste(setOf("revenge", "comedy"))
        SetContentLanguageUseCase(preferences)("ta")
        val isDone = IsOnboardingDoneUseCase(preferences)
        assertFalse(isDone())
        CompleteOnboardingUseCase(preferences)()
        assertTrue(isDone())
        assertEquals(setOf("revenge", "comedy"), preferences.state.value.tasteTagIds)
        assertEquals("ta", languageChoice(listOf("en", "ta", "hi"), "en", deviceLanguage = "hi").selected)
    }

    @Test
    fun `notification pre-prompt only on Android 13+ without the permission`() {
        assertFalse(NotificationPromptPolicy.shouldShow(sdkInt = 32, granted = false))
        assertFalse(NotificationPromptPolicy.shouldShow(sdkInt = 32, granted = true))
        assertTrue(NotificationPromptPolicy.shouldShow(sdkInt = 33, granted = false))
        assertFalse(NotificationPromptPolicy.shouldShow(sdkInt = 33, granted = true))
    }
}
