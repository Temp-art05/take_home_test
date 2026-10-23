package com.takehome.drama.feature.onboarding.presentation.valueprop

import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState

/** Key art full-bleed = poster show hero; null (catalogue lỗi / poster 404) thì nền gradient. */
data class ValuePropViewState(
    val posterUrl: String? = null,
    val artSeed: String = "",
) : ViewState

sealed interface ValuePropIntent : ViewIntent {
    data object Continue : ValuePropIntent
}

sealed interface ValuePropSingleEvent : SingleEvent {
    data object Next : ValuePropSingleEvent
}
