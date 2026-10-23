package com.takehome.drama.feature.onboarding.presentation.language

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.takehome.drama.core.ui.foundation.locale.AppLocale
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.model.hero
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.onboarding.GetLanguageChoiceUseCase
import com.takehome.drama.domain.usecase.onboarding.SetContentLanguageUseCase
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * SCR-0002 — chọn ngôn ngữ trong 10 locale ra mắt: áp cho **cả UI** (`AppLocale`) lẫn **nội dung** (title/audio;
 * catalogue chưa có ngôn ngữ đó thì nội dung lùi về `en`).
 */
class OnboardingLanguageViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getCatalog: GetCatalogUseCase,
    private val getLanguageChoice: GetLanguageChoiceUseCase,
    private val setContentLanguage: SetContentLanguageUseCase,
) : BaseViewModel<OnboardingLanguageIntent, OnboardingLanguageViewState, OnboardingLanguageSingleEvent>() {

    private var confirmJob: Job? = null

    override fun initState() = OnboardingLanguageViewState()

    init {
        load()
    }

    override fun processIntent(intent: OnboardingLanguageIntent) {
        when (intent) {
            is OnboardingLanguageIntent.Select -> select(intent.code)
            OnboardingLanguageIntent.Continue -> confirm()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val catalog = getCatalog().getOrNull()
            val available = catalog?.availableLanguages.orEmpty()
            val default = catalog?.defaultLanguage ?: GetLanguageChoiceUseCase.FALLBACK_DEFAULT_LANGUAGE
            val choice = getLanguageChoice(available, default, Locale.getDefault().language)
            // Chọn lại trước process death thì giữ; thứ tự danh sách vẫn ghim mục chọn sẵn ban đầu.
            val selected = savedStateHandle.get<String>(KEY_SELECTED)?.takeIf { saved -> choice.options.any { it.code == saved } }
                ?: choice.selected
            val hero = catalog?.hero()
            updateState {
                copy(
                    isLoading = false,
                    options = choice.options,
                    selected = selected,
                    posterUrl = hero?.posterUrl,
                    artSeed = hero?.id.orEmpty(),
                )
            }
        }
    }

    private fun select(code: String) {
        savedStateHandle[KEY_SELECTED] = code
        updateState { copy(selected = code) }
    }

    private fun confirm() {
        val code = currentState.selected.ifEmpty { return }
        // Bấm lặp khi đang ghi → một lần Next. Không chặn vĩnh viễn: back từ SCR-0003 rồi bấm lại vẫn đi.
        if (confirmJob?.isActive == true) return
        confirmJob = viewModelScope.launch {
            setContentLanguage(code)
            // Mã nào cũng thuộc 10 locale ra mắt; mã catalogue lạ (nếu có) thì UI theo máy.
            sendEvent(OnboardingLanguageSingleEvent.Next(AppLocale.fromTag(code) ?: AppLocale.System))
        }
    }

    private companion object {
        const val KEY_SELECTED = "onboarding_language"
    }
}
