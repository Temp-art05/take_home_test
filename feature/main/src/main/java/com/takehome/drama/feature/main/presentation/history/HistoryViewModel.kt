package com.takehome.drama.feature.main.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.takehome.drama.core.ui.foundation.error.toErrorRes
import com.takehome.drama.core.ui.foundation.error.toUiError
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.repository.UserPreferencesRepository
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.history.EpochClock
import com.takehome.drama.domain.usecase.history.GetContinueEpisodeUseCase
import com.takehome.drama.domain.usecase.history.ObserveWatchHistoryUseCase
import com.takehome.drama.domain.usecase.history.RemoveFromHistoryUseCase
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.presentation.library.model.HistoryRow
import com.takehome.drama.feature.main.presentation.library.model.toRow
import com.takehome.drama.feature.main.presentation.main.MainTabRequests
import com.takehome.drama.feature.main.presentation.main.model.MainTab
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * SCR-0402. Room là nguồn sự thật nên History còn nguyên sau khi kill app; tab đang chọn nằm trong
 * [SavedStateHandle] (process death). Xoá chống bấm lặp theo từng show.
 */
class HistoryViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getCatalog: GetCatalogUseCase,
    private val observeWatchHistory: ObserveWatchHistoryUseCase,
    private val removeFromHistory: RemoveFromHistoryUseCase,
    private val getContinueEpisode: GetContinueEpisodeUseCase,
    private val resolveContentLanguage: ResolveContentLanguageUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val epochClock: EpochClock,
    private val mainTabRequests: MainTabRequests,
) : BaseViewModel<HistoryIntent, HistoryViewState, HistorySingleEvent>() {

    private var historyJob: Job? = null
    private var openJob: Job? = null
    private val removing = mutableSetOf<String>()

    override fun initState() = HistoryViewState(
        tab = savedStateHandle.get<String>(KEY_TAB)?.let { name -> HistoryTab.entries.firstOrNull { it.name == name } }
            ?: HistoryTab.Watching,
    )

    init {
        load()
    }

    override fun processIntent(intent: HistoryIntent) {
        when (intent) {
            HistoryIntent.Back -> sendEvent(HistorySingleEvent.NavigateBack)
            HistoryIntent.Retry -> if (!currentState.isLoading) load()
            is HistoryIntent.SelectTab -> selectTab(intent.tab)
            HistoryIntent.ToggleEdit -> updateState { copy(editing = !editing && rows.isNotEmpty()) }
            is HistoryIntent.OpenRow -> openRow(intent.row)
            is HistoryIntent.OpenMenu -> updateState { copy(menuFor = intent.row) }
            HistoryIntent.DismissMenu -> updateState { copy(menuFor = null) }
            is HistoryIntent.OpenDetails -> {
                updateState { copy(menuFor = null) }
                sendEvent(HistorySingleEvent.OpenDetail(intent.showId))
            }
            is HistoryIntent.Remove -> remove(intent.showId)
            HistoryIntent.FindNext -> {
                mainTabRequests.request(MainTab.Browse)
                sendEvent(HistorySingleEvent.GoBrowse)
            }
        }
    }

    private fun selectTab(tab: HistoryTab) {
        if (tab == currentState.tab) return
        savedStateHandle[KEY_TAB] = tab.name
        // Edit chỉ áp cho danh sách đang mở — đổi tab thì thoát.
        updateState { copy(tab = tab, editing = false) }
    }

    private fun load() {
        updateState { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getCatalog()
                .onSuccess(::observeHistory)
                .onFailure { error -> updateState { copy(isLoading = false, error = error.toUiError()) } }
        }
    }

    private fun observeHistory(catalog: Catalog) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            combine(userPreferencesRepository.preferences, observeWatchHistory(catalog)) { preferences, history ->
                preferences to history
            }.collect { (preferences, history) ->
                val language = resolveContentLanguage(preferences.contentLanguage, Locale.getDefault().language, catalog)
                val now = epochClock.nowMillis()
                val zone = TimeZone.getDefault()
                updateState {
                    val next = copy(
                        isLoading = false,
                        error = null,
                        watching = history.watching.map { it.toRow(catalog, language, now, zone, finished = false) },
                        finished = history.finished.map { it.toRow(catalog, language, now, zone, finished = true) },
                    )
                    if (next.rows.isEmpty()) next.copy(editing = false) else next
                }
            }
        }
    }

    /** Đang xem → Player (xem tiếp); đã xong → Detail (tìm lại show để giới thiệu, ghi chú 4.2). */
    private fun openRow(row: HistoryRow) {
        if (row.finished) {
            sendEvent(HistorySingleEvent.OpenDetail(row.showId))
            return
        }
        if (openJob?.isActive == true) return
        openJob = viewModelScope.launch {
            sendEvent(HistorySingleEvent.OpenPlayer(row.showId, getContinueEpisode(row.showId, row.episodeIndex)))
        }
    }

    private fun remove(showId: String) {
        updateState { copy(menuFor = null) }
        val listed = currentState.watching.any { it.showId == showId } || currentState.finished.any { it.showId == showId }
        if (!listed || !removing.add(showId)) return
        viewModelScope.launch {
            removeFromHistory(showId)
                .onSuccess { sendEvent(HistorySingleEvent.ShowToast(R.string.history_removed, success = true)) }
                .onFailure { sendEvent(HistorySingleEvent.ShowToast(it.toErrorRes(), success = false)) }
            removing.remove(showId)
        }
    }

    private companion object {
        const val KEY_TAB = "history_tab"
    }
}
