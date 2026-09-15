package ru.mgsu.molotkova.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.mgsu.molotkova.schedule.data.Lesson
import ru.mgsu.molotkova.schedule.data.ScheduleRepository
import ru.mgsu.molotkova.schedule.data.WeekType
import ru.mgsu.molotkova.schedule.ui.theme.MolotkovaScheduleTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MolotkovaScheduleTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScheduleScreen()
                }
            }
        }
    }
}

private val ruLocale = Locale("ru")
private val oddGreen = Color(0xFF92D050)
private val evenYellow = Color(0xFFFFE800)
private val exactBlue = Color(0xFF6E9FD1)

@Composable
private fun ScheduleScreen() {
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }

    val weekMonday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = (0L..5L).map { weekMonday.plusDays(it) }
    val weekNumber = ScheduleRepository.academicWeekNumber(selectedDate)
    val weekType = ScheduleRepository.weekTypeFor(selectedDate)
    val lessons = ScheduleRepository.lessonsFor(selectedDate)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Расписание Полины Молотковой",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "НИУ МГСУ • преподаватель",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { selectedDate = selectedDate.minusWeeks(1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущая неделя")
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = weekTitle(weekMonday),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (weekNumber != null && weekType != null) {
                        WeekLabel(weekNumber, weekType)
                    } else {
                        Text(
                            text = "вне семестра",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { selectedDate = selectedDate.plusWeeks(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Следующая неделя")
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(days) { date ->
                    DayChip(
                        date = date,
                        selected = date == selectedDate,
                        isToday = date == today,
                        onClick = { selectedDate = date }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = { selectedDate = today },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Сегодня")
            }
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = longDate(selectedDate),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = lessonCountText(lessons.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (lessons.isEmpty()) {
                item { EmptyDay(selectedDate) }
            } else {
                items(lessons) { lesson ->
                    LessonCard(lesson)
                }
            }

            item { Legend() }
        }
    }
}

@Composable
private fun WeekLabel(number: Int, type: WeekType) {
    val odd = type == WeekType.ODD
    val color = if (odd) oddGreen else evenYellow
    val text = if (odd) "нечётная" else "чётная"

    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.25f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$number-я неделя • $text",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DayChip(
    date: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val foreground = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val dayName = date.dayOfWeek
            .getDisplayName(TextStyle.SHORT, ruLocale)
            .replaceFirstChar { it.uppercase() }

        Text(
            text = dayName.take(2),
            color = foreground.copy(alpha = 0.8f),
            fontSize = 11.sp
        )
        Text(
            text = date.dayOfMonth.toString(),
            color = foreground,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        if (isToday) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(foreground)
            )
        } else {
            Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
private fun LessonCard(lesson: Lesson) {
    val accent = when (lesson.weekType) {
        WeekType.ODD -> oddGreen
        WeekType.EVEN -> evenYellow
        WeekType.EXACT_DATE -> exactBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(accent)
        )

        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${lesson.start}–${lesson.end}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${lesson.pair}-я пара",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TypeLabel(lesson.weekType)
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = lesson.subject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Группа: ${lesson.group}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Аудитория: ${lesson.room}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TypeLabel(type: WeekType) {
    val pair = when (type) {
        WeekType.ODD -> "нечётная" to oddGreen
        WeekType.EVEN -> "чётная" to evenYellow
        WeekType.EXACT_DATE -> "по дате" to exactBlue
    }

    Surface(
        color = pair.second.copy(alpha = 0.22f),
        shape = RoundedCornerShape(100.dp)
    ) {
        Text(
            text = pair.first,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyDay(date: LocalDate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Пар нет",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (ScheduleRepository.academicWeekNumber(date) == null) {
                    "Выбранная дата находится вне осеннего семестра 2026 года."
                } else {
                    "На выбранную дату занятий в расписании нет."
                },
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Legend() {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            text = "Обозначения",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(7.dp))
        LegendLine(oddGreen, "Нечётная неделя")
        LegendLine(evenYellow, "Чётная неделя")
        LegendLine(exactBlue, "Занятие по конкретной дате")
    }
}

@Composable
private fun LegendLine(color: Color, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun weekTitle(monday: LocalDate): String {
    val saturday = monday.plusDays(5)
    val firstMonth = monday.month.getDisplayName(TextStyle.FULL, ruLocale)
    val secondMonth = saturday.month.getDisplayName(TextStyle.FULL, ruLocale)

    val text = if (monday.month == saturday.month) {
        "$firstMonth ${monday.year}"
    } else {
        "$firstMonth — $secondMonth ${saturday.year}"
    }

    return text.replaceFirstChar { it.uppercase() }
}

private fun longDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", ruLocale)
    return date.format(formatter).replaceFirstChar { it.uppercase() }
}

private fun lessonCountText(count: Int): String {
    val word = when {
        count % 10 == 1 && count % 100 != 11 -> "занятие"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "занятия"
        else -> "занятий"
    }
    return "$count $word"
}
