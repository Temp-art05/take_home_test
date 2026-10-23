package com.takehome.drama.domain.model

data class UserPreferences(
    /** Ngôn ngữ nội dung (title/audio/sub), một trong `availableLanguages`. Null = chưa chọn. */
    val contentLanguage: String? = null,
    val tasteTagIds: Set<String> = emptySet(),
    val theme: ThemeMode = ThemeMode.Dark,
    val onboardingDone: Boolean = false,
    val autoplayPreviewWifiOnly: Boolean = true,
)

enum class ThemeMode { Dark, Light }
