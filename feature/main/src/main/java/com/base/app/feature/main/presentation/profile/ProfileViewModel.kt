package com.base.app.feature.main.presentation.profile

import com.base.app.core.ui.foundation.vm.BaseViewModel

class ProfileViewModel : BaseViewModel<ProfileViewState, ProfileSingleEvent>() {

    override fun initState() = ProfileViewState()

    fun onSettingsClick() {
        sendEvent(ProfileSingleEvent.NavigateToSettings)
    }
}
