package com.takehome.drama.feature.main.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.takehome.drama.core.ui.foundation.error.toErrorRes
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.genres
import com.takehome.drama.domain.repository.UserPreferencesRepository
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import com.takehome.drama.domain.usecase.catalog.ResolveContentLanguageUseCase
import com.takehome.drama.domain.usecase.search.GetPopularShowsUseCase
import com.takehome.drama.domain.usecase.search.SearchResult
import com.takehome.drama.domain.usecase.search.SearchShowsUseCase
import com.takehome.drama.feature.main.navigation.SearchScreenProvider.Search
import com.takehome.drama.feature.main.presentation.common.toCard
import java.util.Locale
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * SCR-0201 — tìm client-side trên catalogue (cache in-memory của repository, không gọi mạng mỗi phím).
 * Gõ → debounce [DEBOUNCE_MS] → tìm; xoá hết thì về trạng thái chưa nhập **ngay**, không đợi debounce.
 * Query nằm trong [SavedStateHandle].
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getCatalog: GetCatalogUseCase,
    private val searchShows: SearchShowsUseCase,
    private val getPopularShows: GetPopularShowsUseCase,
    private val resolveContentLanguage: ResolveContentLanguageUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : BaseViewModel<SearchIntent, SearchViewState, SearchSingleEvent>() {

    /**
     * SharedFlow chứ không StateFlow: StateFlow so trùng giá trị, nên "ceo" → Clear → "ceo" gõ trong cửa
     * sổ debounce bị nuốt và màn kẹt ở Idle. Query gõ trước khi catalogue tải xong đã có `run(currentState.query)`.
     */
    private val queries = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var catalog: Catalog? = null
    private var language = ""

    override fun initState() = SearchViewState(
        query = savedStateHandle[KEY_QUERY] ?: savedStateHandle.toRoute<Search>().initialQuery.orEmpty(),
    )

    init {
        load()
    }

    override fun processIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> setQuery(intent.query)
            SearchIntent.Clear -> setQuery("")
            is SearchIntent.FillQuery -> setQuery(intent.text, immediate = true)
            is SearchIntent.OpenShow -> sendEvent(SearchSingleEvent.OpenShow(intent.showId))
            SearchIntent.Back -> sendEvent(SearchSingleEvent.NavigateBack)
            SearchIntent.Retry -> load()
        }
    }

    private fun load() {
        updateState { copy(errorRes = null) }
        viewModelScope.launch {
            val loaded = getCatalog().getOrElse { error ->
                updateState { copy(errorRes = error.toErrorRes()) }
                return@launch
            }
            val saved = userPreferencesRepository.preferences.first().contentLanguage
            language = resolveContentLanguage(saved, Locale.getDefault().language, loaded)
            catalog = loaded
            updateState {
                copy(
                    genres = loaded.genres().map { it.name.resolveTagLabel(language, loaded.defaultLanguage) },
                    popular = getPopularShows(loaded).map { it.toCard(loaded, language) },
                )
            }
            run(currentState.query)
            queries
                .debounce(DEBOUNCE_MS)
                // Không distinctUntilChanged (cùng lý do như [queries]); tìm client-side nên chạy lại rẻ.
                .map { it.trim() }
                .collect(::run)
        }
    }

    private fun setQuery(query: String, immediate: Boolean = false) {
        savedStateHandle[KEY_QUERY] = query
        updateState { copy(query = query) }
        // Xoá hết / chọn gợi ý: hiện ngay. Lượt debounce sau đó ra cùng kết quả nên vô hại.
        if (query.isBlank() || immediate) run(query)
        queries.tryEmit(query)
    }

    private fun run(query: String) {
        val catalog = catalog ?: return
        val result = searchShows(catalog, query, language)
        updateState {
            when (result) {
                SearchResult.Idle -> copy(phase = SearchPhase.Idle, suggestions = emptyList(), matches = emptyList())
                SearchResult.NoResults -> copy(phase = SearchPhase.NoResults, suggestions = emptyList(), matches = emptyList())
                is SearchResult.Matches -> copy(
                    phase = SearchPhase.Results,
                    suggestions = result.suggestions,
                    matches = result.shows.map { it.toCard(catalog, language) },
                )
            }
        }
    }

    companion object {
        const val DEBOUNCE_MS = 300L
        private const val KEY_QUERY = "search_query"
    }
}
