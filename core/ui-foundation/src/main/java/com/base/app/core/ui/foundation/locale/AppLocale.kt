package com.base.app.core.ui.foundation.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Ngôn ngữ app hỗ trợ. Phải khớp với res/xml/locales_config.xml và localeFilters
 * trong app/build.gradle.kts — thêm một chỗ mà quên hai chỗ kia là picker không hoạt động.
 */
enum class AppLocale(val tag: String?) {
    /** Theo ngôn ngữ hệ thống. */
    System(null),
    English("en"),
    Vietnamese("vi");

    companion object {
        /**
         * Đọc locale user đã chọn. Không tự lưu vào DataStore: từ API 33 hệ thống
         * đã persist giúp, dưới 33 thì AppCompat persist — lưu thêm là hai nguồn sự thật.
         */
        fun current(): AppLocale {
            val tag = AppCompatDelegate.getApplicationLocales()
                .takeIf { !it.isEmpty }
                ?.get(0)
                ?.language
            return entries.firstOrNull { it.tag == tag } ?: System
        }

        fun apply(locale: AppLocale) {
            AppCompatDelegate.setApplicationLocales(
                if (locale.tag == null) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(locale.tag)
                }
            )
        }
    }
}
