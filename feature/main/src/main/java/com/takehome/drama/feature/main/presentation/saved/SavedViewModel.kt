package com.takehome.drama.feature.main.presentation.saved

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.takehome.drama.core.ui.foundation.error.toErrorRes
import com.takehome.drama.core.ui.foundation.error.toUiError
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.repository.UserPreferencesRepository
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.library.ObserveSavedShowsUseCase
import com.takehome.drama.domain.usecase.library.SavedFilter
import com.takehome.drama.domain.usecase.library.SavedShelf
import com.takehome.drama.domain.usecase.library.SetShowSavedUseCase
import com.takehome.drama.feature.main.R
import com.takehome.drama.feature.main.presentation.library.model.toSavedRow
import com.takehome.drama.feature.main.presentation.main.MainTabRequests
import com.takehome.drama.feature.main.presentation.main.model.MainTab
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * SCR-0403 + SCR-0602. Một stream Room cho cả màn; đổi chip chỉ lọc lại [SavedShelf] đang giữ —
 * không tải catalogue lại, không subscribe lại. Chip lưu trong [SavedStateHandle].
 */
class SavedViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getCatalog: GetCatalogUseCase,
    private val observeSavedShows: ObserveSavedShowsUseCase,
    private val setShowSaved: SetShowSavedUseCase,
    private val resolveContentLanguage: ResolveContentLanguageUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val mainTabRequests: MainTabRequests,
) : BaseViewModel<SavedIntent, SavedViewState, SavedSingleEvent>() {

    private var shelfJob: Job? = null
    private var catalog: Catalog? = null
    private var shelf = SavedShelf(emptyList())
    private var language = ""
    private val removing = mutableSetOf<String>()

    override fun initState() = SavedViewState(
        filter = savedStateHandle.get<String>(KEY_FILTER)?.let { name -> SavedFilter.entries.firstOrNull { it.name == name } }
            ?: SavedFilter.All,
    )

    init {
        load()
    }

    override fun processIntent(intent: SavedIntent) {
        when (intent) {
            SavedIntent.Back -> sendEvent(SavedSingleEvent.NavigateBack)
            SavedIntent.Retry -> if (!currentState.isLoading) load()
            is SavedIntent.SelectFilter -> selectFilter(intent.filter)
            SavedIntent.ToggleEdit -> updateState { copy(editing = !editing && items.isNotEmpty()) }
            is SavedIntent.OpenRow -> {
                updateState { copy(menuFor = null) }
                sendEvent(SavedSingleEvent.OpenDetail(intent.showId))
            }
            is SavedIntent.OpenMenu -> updateState { copy(menuFor = intent.row) }
            SavedIntent.DismissMenu -> updateState { copy(menuFor = null) }
            is SavedIntent.Remove -> remove(intent.showId)
            SavedIntent.FindNext -> {
                mainTabRequests.request(MainTab.Browse)
                sendEvent(SavedSingleEvent.GoBrowse)
            }
        }
    }

    private fun selectFilter(filter: SavedFilter) {
        if (filter == currentState.filter) return
        savedStateHandle[KEY_FILTER] = filter.name
        updateState { copy(filter = filter, editing = false) }
        render()
    }

    private fun load() {
        updateState { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getCatalog()
                .onSuccess(::observeShelf)
                .onFailure { error -> updateState { copy(isLoading = false, error = error.toUiError()) } }
        }
    }

    private fun observeShelf(loaded: Catalog) {
        catalog = loaded
        shelfJob?.cancel()
        shelfJob = viewModelScope.launch {
            combine(userPreferencesRepository.preferences, observeSavedShows(loaded)) { preferences, shelf ->
                preferences to shelf
            }.collect { (preferences, latest) ->
                language = resolveContentLanguage(preferences.contentLanguage, Locale.getDefault().language, loaded)
                shelf = latest
                render()
            }
        }
    }

    private fun render() {
        val catalog = catalog ?: return
        val filter = currentState.filter
        updateState {
            val items = shelf.shows(filter).map { it.toSavedRow(catalog, language) }
            copy(
                isLoading = false,
                error = null,
                items = items,
                totalCount = shelf.count(SavedFilter.All),
                editing = editing && items.isNotEmpty(),
            )
        }
    }

    private fun remove(showId: String) {
        updateState { copy(menuFor = null) }
        if (currentState.items.none { it.showId == showId } || !removing.add(showId)) return
        viewModelScope.launch {
            setShowSaved(showId, saved = false)
                .onSuccess { sendEvent(SavedSingleEvent.ShowToast(R.string.common_removed_from_library, success = true)) }
                .onFailure { sendEvent(SavedSingleEvent.ShowToast(it.toErrorRes(), success = false)) }
            removing.remove(showId)
        }
    }

    private companion object {
        const val KEY_FILTER = "saved_filter"
    }
}
