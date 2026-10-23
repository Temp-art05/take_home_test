package com.takehome.drama.feature.onboarding.presentation.valueprop

import androidx.lifecycle.viewModelScope
import com.takehome.drama.core.ui.foundation.vm.BaseViewModel
import com.takehome.drama.domain.model.hero
import com.takehome.drama.domain.usecase.catalog.GetCatalogUseCase
import kotlinx.coroutines.launch

/** SCR-0003 — màn tĩnh; VM chỉ lấy key art (poster hero) và chuyển bước. */
class ValuePropViewModel(
    private val getCatalog: GetCatalogUseCase,
) : BaseViewModel<ValuePropIntent, ValuePropViewState, ValuePropSingleEvent>() {

    override fun initState() = ValuePropViewState()

    init {
        viewModelScope.launch {
            getCatalog().getOrNull()?.hero()?.let { hero ->
                updateState { copy(posterUrl = hero.posterUrl, artSeed = hero.id) }
            }
        }
    }

    override fun processIntent(intent: ValuePropIntent) {
        when (intent) {
            ValuePropIntent.Continue -> sendEvent(ValuePropSingleEvent.Next)
        }
    }
}
