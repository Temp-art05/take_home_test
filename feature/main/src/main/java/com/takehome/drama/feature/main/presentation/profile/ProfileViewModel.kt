package com.takehome.drama.feature.main.presentation.profile

import com.takehome.drama.core.ui.foundation.vm.BaseViewModel

class ProfileViewModel : BaseViewModel<ProfileViewState, ProfileSingleEvent>() {

    override fun initState() = ProfileViewState()

    fun onSettingsClick() {
        sendEvent(ProfileSingleEvent.NavigateToSettings)
    }
}
