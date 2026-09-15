package ru.mgsu.molotkova.schedule.data

import java.time.LocalDate

enum class WeekType {
    ODD,
    EVEN,
    EXACT_DATE
}

data class Lesson(
    val dayOfWeek: Int,
    val pair: Int,
    val start: String,
    val end: String,
    val subject: String,
    val room: String,
    val group: String,
    val weekType: WeekType,
    val exactDates: Set<LocalDate> = emptySet()
)
