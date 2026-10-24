package com.takehome.drama.core.ui.foundation.locale

import com.takehome.drama.domain.model.LaunchLanguages
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLocaleTest {

    @Test
    fun `app locales are exactly the ten launch languages`() {
        assertEquals(LaunchLanguages.codes, AppLocale.entries.mapNotNull { it.tag })
    }

    @Test
    fun `content language codes map to their ui locale`() {
        assertEquals(AppLocale.PortugueseBrazil, AppLocale.fromTag("pt-BR"))
        assertEquals(AppLocale.English, AppLocale.fromTag("en-US"))
        assertEquals(AppLocale.Arabic, AppLocale.fromTag("ar"))
        assertNull(AppLocale.fromTag("ja"))
    }

    @Test
    fun `indonesian matches both the modern and the legacy code`() {
        assertEquals(AppLocale.Indonesian, AppLocale.fromTag("id"))
        assertEquals(AppLocale.Indonesian, AppLocale.fromLocale(Locale("in")))
    }

    @Test
    fun `plain portuguese still picks the brazilian locale`() {
        assertEquals(AppLocale.PortugueseBrazil, AppLocale.fromLocale(Locale.forLanguageTag("pt-PT")))
    }

    @Test
    fun `following the system and picking the language already shown - nothing to apply`() {
        assertFalse(AppLocale.needsApply(AppLocale.English, current = AppLocale.System, displayed = Locale.forLanguageTag("en-US")))
    }

    @Test
    fun `same explicit locale - nothing to apply`() {
        assertFalse(AppLocale.needsApply(AppLocale.French, current = AppLocale.French, displayed = Locale.FRENCH))
    }

    @Test
    fun `a different language is applied`() {
        assertTrue(AppLocale.needsApply(AppLocale.Hindi, current = AppLocale.System, displayed = Locale.forLanguageTag("en-US")))
        assertTrue(AppLocale.needsApply(AppLocale.English, current = AppLocale.French, displayed = Locale.FRENCH))
    }

    @Test
    fun `going back to the system language from an explicit one is applied`() {
        assertTrue(AppLocale.needsApply(AppLocale.System, current = AppLocale.French, displayed = Locale.FRENCH))
    }
}
