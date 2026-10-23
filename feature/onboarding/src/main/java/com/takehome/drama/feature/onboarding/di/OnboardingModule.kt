package com.takehome.drama.feature.onboarding.di

import com.takehome.drama.feature.onboarding.presentation.flow.OnboardingFlowViewModel
import com.takehome.drama.feature.onboarding.presentation.language.OnboardingLanguageViewModel
import com.takehome.drama.feature.onboarding.presentation.notification.NotificationPromptViewModel
import com.takehome.drama.feature.onboarding.presentation.splash.SplashViewModel
import com.takehome.drama.feature.onboarding.presentation.taste.TastePickerViewModel
import com.takehome.drama.feature.onboarding.presentation.valueprop.ValuePropViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingModule = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::OnboardingFlowViewModel)
    viewModelOf(::OnboardingLanguageViewModel)
    viewModelOf(::ValuePropViewModel)
    viewModelOf(::TastePickerViewModel)
    viewModelOf(::NotificationPromptViewModel)
}
