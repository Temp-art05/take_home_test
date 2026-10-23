package com.base.app.feature.main.presentation.profile

import com.base.app.core.ui.foundation.vm.SingleEvent
import com.base.app.core.ui.foundation.vm.ViewState

/** `userName == null` là khách — UI tự hiện text "Khách" theo ngôn ngữ app. */
data class ProfileViewState(
    val userName: String? = null,
) : ViewState

sealed interface ProfileSingleEvent : SingleEvent {
    data object NavigateToSettings : ProfileSingleEvent
}
