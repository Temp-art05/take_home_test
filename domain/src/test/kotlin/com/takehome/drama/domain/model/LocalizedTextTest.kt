package com.takehome.drama.domain.model

import com.takehome.drama.domain.model.languageSubtag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizedTextTest {

    private val text = LocalizedText(mapOf("en" to "Hello", "ar" to "مرحبا", "hi" to ""))

    @Test
    fun `user language present - uses it`() {
        assertEquals("مرحبا", text.resolve(preferred = "ar", fallback = "en"))
    }

    @Test
    fun `user language missing - falls back to default language`() {
        assertEquals("Hello", text.resolve(preferred = "ta", fallback = "en"))
    }

    @Test
    fun `blank value counts as missing`() {
        assertEquals("Hello", text.resolve(preferred = "hi", fallback = "en"))
    }

    @Test
    fun `both missing - first non blank value`() {
        val onlyArabic = LocalizedText(mapOf("hi" to " ", "ar" to "مرحبا"))
        assertEquals("مرحبا", onlyArabic.resolve(preferred = "ta", fallback = "en"))
    }

    @Test
    fun `region tag matches language key`() {
        assertEquals("Hello", text.resolve(preferred = "en-US", fallback = "ar"))
    }

    @Test
    fun `region key matches language preference`() {
        val regional = LocalizedText(mapOf("pt-BR" to "Olá", "en" to "Hello"))
        assertEquals("Olá", regional.resolve(preferred = "pt", fallback = "en"))
    }

    @Test
    fun `empty map resolves to empty string`() {
        assertEquals("", LocalizedText.Empty.resolve(preferred = "en", fallback = "en"))
        assertTrue(LocalizedText(mapOf("en" to "")).isEmpty)
    }

    @Test
    fun `legacy Indonesian code from Locale matches the catalogue id key`() {
        val text = LocalizedText(mapOf("en" to "Hello", "id" to "Halo"))
        assertEquals("Halo", text.resolve(preferred = "in", fallback = "en"))
        assertEquals("id", "in_ID".languageSubtag())
    }
}
