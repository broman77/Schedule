package ru.mgsu.molotkova.schedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleRepositoryTest {
    @Test
    fun firstAcademicWeekIsOdd() {
        assertEquals(1, ScheduleRepository.academicWeekNumber(LocalDate.of(2026, 9, 1)))
        assertEquals(WeekType.ODD, ScheduleRepository.weekTypeFor(LocalDate.of(2026, 9, 1)))
    }

    @Test
    fun secondAcademicWeekIsEven() {
        assertEquals(2, ScheduleRepository.academicWeekNumber(LocalDate.of(2026, 9, 7)))
        assertEquals(WeekType.EVEN, ScheduleRepository.weekTypeFor(LocalDate.of(2026, 9, 7)))
    }

    @Test
    fun mondayPairFourChangesByParity() {
        val odd = ScheduleRepository.lessonsFor(LocalDate.of(2026, 9, 14))
        val even = ScheduleRepository.lessonsFor(LocalDate.of(2026, 9, 21))
        assertTrue(odd.any { it.pair == 4 && it.group == "ИПГС 3-20" })
        assertTrue(even.any { it.pair == 4 && it.group == "ИПГС 3-2" })
    }

    @Test
    fun exactDateIdoAppearsOnlyOnSpecifiedTuesday() {
        val onDate = ScheduleRepository.lessonsFor(LocalDate.of(2026, 9, 29))
        val otherTuesday = ScheduleRepository.lessonsFor(LocalDate.of(2026, 10, 6))
        assertTrue(onDate.any { it.pair == 7 && it.group.contains("60") })
        assertTrue(otherTuesday.none { it.weekType == WeekType.EXACT_DATE })
    }
}
