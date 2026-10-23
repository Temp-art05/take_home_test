package com.takehome.drama.domain.usecase

import com.takehome.drama.domain.usecase.discovery.ClockTime
import com.takehome.drama.domain.usecase.discovery.Daypart
import com.takehome.drama.domain.usecase.discovery.daypartAt
import com.takehome.drama.domain.usecase.discovery.upcoming
import org.junit.Assert.assertEquals
import org.junit.Test

class DaypartTest {

    private fun at(hour: Int, minute: Int) = daypartAt(ClockTime(hour, minute))

    @Test
    fun `morning runs from 05_00 to 10_59`() {
        assertEquals(Daypart.Night, at(4, 59))
        assertEquals(Daypart.Morning, at(5, 0))
        assertEquals(Daypart.Morning, at(10, 59))
    }

    @Test
    fun `lunch runs from 11_00 to 16_59`() {
        assertEquals(Daypart.Lunch, at(11, 0))
        assertEquals(Daypart.Lunch, at(16, 59))
    }

    @Test
    fun `night covers the evening and wraps past midnight`() {
        assertEquals(Daypart.Night, at(17, 0))
        assertEquals(Daypart.Night, at(21, 45))
        assertEquals(Daypart.Night, at(23, 59))
        assertEquals(Daypart.Night, at(0, 0))
    }

    @Test
    fun `upcoming lists the other two dayparts in order of arrival`() {
        assertEquals(listOf(Daypart.Morning, Daypart.Lunch), Daypart.Night.upcoming())
        assertEquals(listOf(Daypart.Lunch, Daypart.Night), Daypart.Morning.upcoming())
        assertEquals(listOf(Daypart.Night, Daypart.Morning), Daypart.Lunch.upcoming())
    }
}
