package ru.mgsu.molotkova.schedule.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object ScheduleRepository {
    // Первая учебная неделя осеннего семестра 2026: 31.08–06.09.
    // Она считается нечётной. В исходном Excel: зелёный = нечётная, жёлтый = чётная.
    val semesterStartMonday: LocalDate = LocalDate.of(2026, 8, 31)
    val semesterEnd: LocalDate = LocalDate.of(2026, 12, 31)

    private fun d(day: Int, month: Int) = LocalDate.of(2026, month, day)

    private val lessons: List<Lesson> = listOf(
        // Понедельник
        weekly(1, 2, "10:00", "11:20", "Основы аддитивных технологий", "611 КМК", "ИПГС 3-19", WeekType.ODD),
        weekly(1, 3, "11:30", "12:50", "Основы аддитивных технологий", "622 КМК", "ИПГС 3-4", WeekType.ODD),
        weekly(1, 4, "13:00", "14:20", "Основы аддитивных технологий", "621 КМК", "ИПГС 3-2", WeekType.EVEN),
        weekly(1, 4, "13:00", "14:20", "Основы аддитивных технологий", "622 КМК", "ИПГС 3-20", WeekType.ODD),
        weekly(1, 5, "14:30", "15:50", "Основы организации строительного производства", "621 КМК", "ИПГС 3-31", WeekType.EVEN),
        weekly(1, 6, "16:00", "17:20", "Основы аддитивных технологий", "619 КМК", "ИПГС 3-5", WeekType.ODD),

        // Вторник
        weekly(2, 3, "11:30", "12:50", "Основы аддитивных технологий", "622а КМК", "ИПГС 3-18", WeekType.EVEN),
        weekly(2, 4, "13:00", "14:20", "Основы аддитивных технологий", "622а КМК", "ИПГС 3-20", WeekType.EVEN),
        exact(2, 7, "18:10", "19:30", "Основы организации строительного производства", "ИДО", "ИДО 60, 61, 70, 71", setOf(d(29,9), d(13,10))),
        exact(2, 7, "18:10", "19:30", "Основы организации строительного производства", "ИДО", "ИДО 53", setOf(d(1,12), d(15,12))),
        exact(2, 8, "19:40", "21:00", "Основы организации строительного производства", "ИДО", "ИДО 60, 61, 70, 71", setOf(d(29,9), d(13,10))),
        exact(2, 8, "19:40", "21:00", "Основы организации строительного производства", "ИДО", "ИДО 53", setOf(d(1,12), d(15,12))),

        // Среда
        weekly(3, 1, "08:30", "09:50", "Основы организации строительного производства", "419 УЛК", "ИПГС 3-32", WeekType.ODD),
        weekly(3, 2, "10:00", "11:20", "Основы аддитивных технологий", "419 УЛК", "ИПГС 3-17", WeekType.ODD),
        weekly(3, 4, "13:00", "14:20", "Основы аддитивных технологий", "622 КМК", "ИПГС 3-17", WeekType.EVEN),
        weekly(3, 5, "14:30", "15:50", "Основы аддитивных технологий", "622 КМК", "ИПГС 3-16", WeekType.EVEN),
        weekly(3, 6, "16:00", "17:20", "Основы аддитивных технологий", "622 КМК", "ИПГС 3-3", WeekType.EVEN),

        // Четверг
        weekly(4, 2, "10:00", "11:20", "Основы аддитивных технологий", "425 КМК", "ИПГС 3-16", WeekType.ODD),
        weekly(4, 3, "11:30", "12:50", "Основы аддитивных технологий", "425 КМК", "ИПГС 3-3", WeekType.ODD),
        exact(4, 7, "18:10", "19:30", "Основы организации строительного производства", "ИДО", "ИДО 80, 81, 90", setOf(d(24,9), d(8,10))),
        exact(4, 7, "18:10", "19:30", "Основы организации строительного производства", "ИДО", "ИДО 55", setOf(d(15,10), d(29,10))),
        exact(4, 7, "18:10", "19:30", "Основы организации строительного производства", "ИДО", "ИДО 40, 45", setOf(d(19,11), d(3,12))),
        exact(4, 7, "18:10", "19:30", "Основы организации строительного производства", "ИДО", "ИДО 54", setOf(d(26,11), d(10,12))),
        exact(4, 8, "19:40", "21:00", "Основы организации строительного производства", "ИДО", "ИДО 80, 81, 90", setOf(d(24,9), d(8,10))),
        exact(4, 8, "19:40", "21:00", "Основы организации строительного производства", "ИДО", "ИДО 55", setOf(d(15,10), d(29,10))),
        exact(4, 8, "19:40", "21:00", "Основы организации строительного производства", "ИДО", "ИДО 40, 45", setOf(d(19,11), d(3,12))),
        exact(4, 8, "19:40", "21:00", "Основы организации строительного производства", "ИДО", "ИДО 54", setOf(d(26,11), d(10,12))),

        // Пятница
        weekly(5, 3, "11:30", "12:50", "Основы аддитивных технологий", "109 УЛК", "ИПГС 3-1", WeekType.EVEN),
        weekly(5, 4, "13:00", "14:20", "Основы аддитивных технологий", "620 КМК", "ИПГС 3-4", WeekType.EVEN),
        weekly(5, 4, "13:00", "14:20", "Основы аддитивных технологий", "419 УЛК", "ИПГС 3-1", WeekType.ODD),
        weekly(5, 5, "14:30", "15:50", "Основы аддитивных технологий", "620 КМК", "ИПГС 3-5", WeekType.EVEN),
        weekly(5, 5, "14:30", "15:50", "Основы аддитивных технологий", "419 УЛК", "ИПГС 3-2", WeekType.ODD),
        weekly(5, 6, "16:00", "17:20", "Основы аддитивных технологий", "620 КМК", "ИПГС 3-19", WeekType.EVEN),
        weekly(5, 6, "16:00", "17:20", "Основы аддитивных технологий", "419 УЛК", "ИПГС 3-18", WeekType.ODD),

        // Суббота — занятия строго по датам из Excel
        exact(6, 2, "10:00", "11:20", "Основы организации строительного производства", "730 КМК", "ИДО 4-52", setOf(d(12,9), d(26,9))),
        exact(6, 3, "11:30", "12:50", "Основы организации строительного производства", "730 КМК", "ИДО 4-52", setOf(d(12,9), d(26,9))),
        exact(6, 4, "13:00", "14:20", "Основы организации строительного производства", "730 КМК", "ИДО 4-51", setOf(d(12,9), d(26,9))),
        exact(6, 5, "14:30", "15:50", "Основы организации строительного производства", "730 КМК", "ИДО 4-51", setOf(d(12,9), d(26,9)))
    )

    fun academicWeekNumber(date: LocalDate): Int? {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        if (monday.isBefore(semesterStartMonday) || date.isAfter(semesterEnd)) return null
        return ChronoUnit.WEEKS.between(semesterStartMonday, monday).toInt() + 1
    }

    fun weekTypeFor(date: LocalDate): WeekType? {
        val number = academicWeekNumber(date) ?: return null
        return if (number % 2 == 0) WeekType.EVEN else WeekType.ODD
    }

    fun lessonsFor(date: LocalDate): List<Lesson> {
        val day = date.dayOfWeek.value
        val currentWeekType = weekTypeFor(date)
        return lessons
            .asSequence()
            .filter { it.dayOfWeek == day }
            .filter { lesson ->
                when (lesson.weekType) {
                    WeekType.EXACT_DATE -> date in lesson.exactDates
                    WeekType.ODD, WeekType.EVEN -> currentWeekType == lesson.weekType
                }
            }
            .sortedWith(compareBy<Lesson> { it.pair }.thenBy { it.start })
            .toList()
    }

    fun allLessons(): List<Lesson> = lessons

    private fun weekly(
        day: Int,
        pair: Int,
        start: String,
        end: String,
        subject: String,
        room: String,
        group: String,
        type: WeekType
    ) = Lesson(day, pair, start, end, subject, room, group, type)

    private fun exact(
        day: Int,
        pair: Int,
        start: String,
        end: String,
        subject: String,
        room: String,
        group: String,
        dates: Set<LocalDate>
    ) = Lesson(day, pair, start, end, subject, room, group, WeekType.EXACT_DATE, dates)
}
