package com.takehome.drama.feature.main.presentation.licenses

import com.takehome.drama.core.ui.foundation.vm.BaseViewModel

class LicensesViewModel : BaseViewModel<LicensesIntent, LicensesViewState, LicensesSingleEvent>() {
    override fun initState() = LicensesViewState

    override fun processIntent(intent: LicensesIntent) {
        when (intent) {
            LicensesIntent.Back -> sendEvent(LicensesSingleEvent.NavigateBack)
        }
    }
}
