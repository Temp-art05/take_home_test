package com.takehome.drama.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ShowStatusTest {

    @Test
    fun `known values ignore case and whitespace`() {
        assertEquals(ShowStatus.Completed, ShowStatus.parse("completed"))
        assertEquals(ShowStatus.Completed, ShowStatus.parse(" Completed "))
        assertEquals(ShowStatus.Ongoing, ShowStatus.parse("ongoing"))
        assertEquals(ShowStatus.Ongoing, ShowStatus.parse("AIRING"))
    }

    @Test
    fun `unknown value keeps raw string instead of failing`() {
        assertEquals(ShowStatus.Unknown("hiatus"), ShowStatus.parse("hiatus"))
    }

    @Test
    fun `missing value is unknown`() {
        assertEquals(ShowStatus.Unknown(""), ShowStatus.parse(null))
        assertEquals(ShowStatus.Unknown(""), ShowStatus.parse(""))
    }
}
