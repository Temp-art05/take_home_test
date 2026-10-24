package com.takehome.drama.core.ui.foundation.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.takehome.drama.domain.model.LaunchLanguages
import java.util.Locale

/**
 * Ngôn ngữ UI app có bản dịch = 10 locale ra mắt ([LaunchLanguages], test khoá khớp). Phải khớp với
 * res/xml/locales_config.xml và localeFilters trong app/build.gradle.kts — thêm một chỗ mà quên hai chỗ kia
 * là picker không hoạt động.
 */
enum class AppLocale(val tag: String?) {
    /** Theo ngôn ngữ hệ thống. */
    System(null),
    English("en"),
    Hindi("hi"),
    Tamil("ta"),
    Indonesian("id"),
    Arabic("ar"),
    French("fr"),
    Spanish("es"),
    PortugueseBrazil("pt-BR"),
    Turkish("tr"),
    German("de");

    /** Tên ngôn ngữ viết bằng chính nó (không dịch); [System] không có. */
    val endonym: String? get() = tag?.let(LaunchLanguages::endonym)

    companion object {
        /** Khớp theo language subtag (`en-US` → English, `pt` → pt-BR); `in` là mã cũ của Indonesia. */
        fun fromTag(tag: String): AppLocale? = fromLocale(Locale.forLanguageTag(tag))

        fun fromLocale(locale: Locale): AppLocale? {
            val language = locale.language.let { if (it == "in") "id" else it }
            return entries.firstOrNull { it.tag != null && Locale.forLanguageTag(it.tag).language == language }
        }

        /**
         * Đọc locale user đã chọn. Không tự lưu vào DataStore: từ API 33 hệ thống
         * đã persist giúp, dưới 33 thì AppCompat persist — lưu thêm là hai nguồn sự thật.
         */
        fun current(): AppLocale =
            AppCompatDelegate.getApplicationLocales().takeIf { !it.isEmpty }?.get(0)?.let(::fromLocale) ?: System

        /**
         * `setApplicationLocales` luôn làm Activity dựng lại (một khung trống — "nháy"), kể cả khi chữ trên màn không
         * đổi. Bỏ qua khi [target] trùng [current], hoặc khi đang theo hệ thống và [target] chính là ngôn ngữ đang
         * hiển thị ([displayed]) — vd máy tiếng Anh, onboarding giữ English mặc định.
         */
        fun needsApply(target: AppLocale, current: AppLocale, displayed: Locale): Boolean = when {
            target == current -> false
            current == System && target == fromLocale(displayed) -> false
            else -> true
        }

        /** [apply] chỉ khi [needsApply]; [displayed] = locale đang hiển thị (`LocalConfiguration`). */
        fun applyIfChanged(target: AppLocale, displayed: Locale) {
            if (needsApply(target, current(), displayed)) apply(target)
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
