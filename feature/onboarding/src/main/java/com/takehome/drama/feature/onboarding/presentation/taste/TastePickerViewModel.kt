package com.takehome.drama.feature.onboarding.presentation.taste

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.takehome.drama.core.ui.foundation.error.toUiError
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.model.Tag
import com.takehome.drama.domain.repository.UserPreferencesRepository
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.takehome.drama.domain.usecase.onboarding.GetMoodOptionsUseCase
import com.takehome.drama.domain.usecase.onboarding.Mood
import com.takehome.drama.domain.usecase.onboarding.MoodOption
import com.takehome.drama.domain.usecase.onboarding.NotificationPromptPolicy
import com.takehome.drama.domain.usecase.onboarding.SaveTasteUseCase
import com.takehome.drama.domain.usecase.onboarding.TasteSelection
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * SCR-0004 mood-first: 1 tap mood là đủ đi tiếp; chip genre để chỉnh sâu. Lựa chọn nằm trong
 * [SavedStateHandle] (sống qua xoay máy / process death). Catalogue lỗi → không có mood/chip, vẫn Skip được.
 */
class TastePickerViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getCatalog: GetCatalogUseCase,
    private val getMoodOptions: GetMoodOptionsUseCase,
    private val saveTaste: SaveTasteUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
    private val resolveContentLanguage: ResolveContentLanguageUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : BaseViewModel<TastePickerIntent, TastePickerViewState, TastePickerSingleEvent>() {

    private var options: List<MoodOption> = emptyList()
    private var genres: List<Tag> = emptyList()
    private var language: String = ""
    private var defaultLanguage: String = ""
    private var finishJob: Job? = null
    private var selection = TasteSelection(
        moods = savedStateHandle.get<ArrayList<String>>(KEY_MOODS).orEmpty().mapNotNull { name -> Mood.entries.firstOrNull { it.name == name } }.toSet(),
        addedTags = savedStateHandle.get<ArrayList<String>>(KEY_ADDED).orEmpty().toSet(),
        removedTags = savedStateHandle.get<ArrayList<String>>(KEY_REMOVED).orEmpty().toSet(),
    )

    override fun initState() = TastePickerViewState(chipsExpanded = savedStateHandle[KEY_EXPANDED] ?: false)

    init {
        load()
    }

    override fun processIntent(intent: TastePickerIntent) {
        when (intent) {
            is TastePickerIntent.ToggleMood -> toggleMood(intent.mood)
            is TastePickerIntent.ToggleTag -> update(selection.toggleTag(intent.tagId, options))
            TastePickerIntent.ExpandChips -> expand()
            TastePickerIntent.Retry -> if (!currentState.isLoading) load()
            is TastePickerIntent.Continue -> finish(selection.tagIds(options), intent.sdkInt, intent.notificationsGranted)
            is TastePickerIntent.Skip -> finish(emptySet(), intent.sdkInt, intent.notificationsGranted)
        }
    }

    private fun load() {
        updateState { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = getCatalog()
            val catalog = result.getOrNull()
            if (catalog != null) {
                val saved = userPreferencesRepository.preferences.first().contentLanguage
                language = resolveContentLanguage(saved, Locale.getDefault().language, catalog)
                defaultLanguage = catalog.defaultLanguage
                options = getMoodOptions(catalog)
                genres = getMoodOptions.fineTuneTags(catalog)
            }
            updateState {
                copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.toUiError(),
                    moods = options.map { option ->
                        MoodItem(option.mood, option.tags.take(SUBTITLE_TAGS).map { it.label() })
                    },
                )
            }
            render()
        }
    }

    private fun toggleMood(mood: Mood) {
        val option = options.firstOrNull { it.mood == mood } ?: return
        update(selection.toggleMood(option))
    }

    private fun update(next: TasteSelection) {
        selection = next
        savedStateHandle[KEY_MOODS] = ArrayList(next.moods.map { it.name })
        savedStateHandle[KEY_ADDED] = ArrayList(next.addedTags)
        savedStateHandle[KEY_REMOVED] = ArrayList(next.removedTags)
        render()
    }

    private fun render() {
        val selected = selection.tagIds(options)
        // Thứ tự chip cố định theo catalog: bấm chip/mood không được xáo cả hàng. Chip đang bật mà nằm ngoài
        // phần thu gọn thì mở rộng tại chỗ (chỉ dài thêm phía dưới) để vẫn thấy được.
        val hiddenSelected = genres.drop(TastePickerViewState.COLLAPSED_CHIPS).any { it.id in selected }
        if (hiddenSelected) savedStateHandle[KEY_EXPANDED] = true
        updateState {
            copy(
                selectedMoods = selection.moods,
                selectedTags = selected,
                chips = genres.map { TasteChip(it.id, it.label()) },
                chipsExpanded = chipsExpanded || hiddenSelected,
            )
        }
    }

    private fun expand() {
        savedStateHandle[KEY_EXPANDED] = true
        updateState { copy(chipsExpanded = true) }
    }

    /** Không cần pre-prompt (API < 33 hoặc đã có quyền) thì onboarding xong ngay ở đây. */
    private fun finish(tagIds: Set<String>, sdkInt: Int, granted: Boolean) {
        // Bấm lặp Continue / Skip khi đang ghi → một lần Next.
        if (finishJob?.isActive == true) return
        finishJob = viewModelScope.launch {
            saveTaste(tagIds)
            val showPrompt = NotificationPromptPolicy.shouldShow(sdkInt, granted)
            if (!showPrompt) completeOnboarding()
            sendEvent(TastePickerSingleEvent.Next(showNotificationPrompt = showPrompt))
        }
    }

    private fun Tag.label(): String = name.resolveTagLabel(language, defaultLanguage)

    private companion object {
        const val SUBTITLE_TAGS = 2
        const val KEY_MOODS = "taste_moods"
        const val KEY_ADDED = "taste_added"
        const val KEY_REMOVED = "taste_removed"
        const val KEY_EXPANDED = "taste_expanded"
    }
}
