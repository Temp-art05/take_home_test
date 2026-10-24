package com.takehome.drama.feature.main.presentation.licenses

import com.takehome.drama.core.ui.foundation.vm.SingleEvent
import com.takehome.drama.core.ui.foundation.vm.ViewIntent
import com.takehome.drama.core.ui.foundation.vm.ViewState

data object LicensesViewState : ViewState

sealed interface LicensesIntent : ViewIntent {
    data object Back : LicensesIntent
}

sealed interface LicensesSingleEvent : SingleEvent {
    data object NavigateBack : LicensesSingleEvent
}
