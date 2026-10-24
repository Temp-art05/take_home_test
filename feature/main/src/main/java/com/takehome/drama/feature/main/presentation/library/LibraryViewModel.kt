package com.takehome.drama.feature.main.presentation.library

import androidx.lifecycle.viewModelScope
import com.takehome.drama.core.ui.foundation.error.toUiError
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.repository.UserPreferencesRepository
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.history.EpochClock
import com.takehome.drama.domain.usecase.history.GetContinueEpisodeUseCase
import com.takehome.drama.domain.usecase.library.ObserveLibrarySummaryUseCase
import com.takehome.drama.feature.main.presentation.library.model.toRow
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * SCR-0401. Catalogue tải một lần (tiêu đề + ảnh); số đếm và Recent là stream Room nên lưu/xem ở màn khác
 * rồi quay lại tab là số tự đúng, không tải lại.
 */
class LibraryViewModel(
    private val getCatalog: GetCatalogUseCase,
    private val observeLibrarySummary: ObserveLibrarySummaryUseCase,
    private val getContinueEpisode: GetContinueEpisodeUseCase,
    private val resolveContentLanguage: ResolveContentLanguageUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val epochClock: EpochClock,
) : BaseViewModel<LibraryIntent, LibraryViewState, LibrarySingleEvent>() {

    private var summaryJob: Job? = null
    private var openJob: Job? = null

    override fun initState() = LibraryViewState()

    init {
        load()
    }

    override fun processIntent(intent: LibraryIntent) {
        when (intent) {
            LibraryIntent.Retry -> if (!currentState.isLoading) load()
            LibraryIntent.OpenHistory -> sendEvent(LibrarySingleEvent.OpenHistory)
            LibraryIntent.OpenSaved -> sendEvent(LibrarySingleEvent.OpenSaved)
            is LibraryIntent.OpenRecent -> openRecent(intent.showId, intent.episodeIndex)
        }
    }

    private fun load() {
        updateState { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getCatalog()
                .onSuccess(::observeSummary)
                .onFailure { error -> updateState { copy(isLoading = false, error = error.toUiError()) } }
        }
    }

    private fun observeSummary(catalog: Catalog) {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            combine(userPreferencesRepository.preferences, observeLibrarySummary(catalog)) { preferences, summary ->
                preferences to summary
            }.collect { (preferences, summary) ->
                val language = resolveContentLanguage(preferences.contentLanguage, Locale.getDefault().language, catalog)
                val now = epochClock.nowMillis()
                val zone = TimeZone.getDefault()
                updateState {
                    copy(
                        isLoading = false,
                        error = null,
                        watchedCount = summary.watchedCount,
                        savedCount = summary.savedCount,
                        recent = summary.recent.map { it.toRow(catalog, language, now, zone, finished = false) },
                    )
                }
            }
        }
    }

    /** Một tap một lần mở: tap lặp trong lúc tải file tập bị bỏ qua. */
    private fun openRecent(showId: String, savedEpisodeIndex: Int) {
        if (openJob?.isActive == true) return
        openJob = viewModelScope.launch {
            sendEvent(LibrarySingleEvent.OpenPlayer(showId, getContinueEpisode(showId, savedEpisodeIndex)))
        }
    }
}
