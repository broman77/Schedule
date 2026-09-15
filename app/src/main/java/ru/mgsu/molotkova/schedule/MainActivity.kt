package ru.mgsu.molotkova.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import java.time.LocalTime
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

private val ru = Locale("ru")
private val greenOdd = Color(0xFF92D050)
private val yellowEven = Color(0xFFFFE800)
private val exactBlue = Color(0xFF6E9FD1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleScreen() {
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }

    val weekMonday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = (0L..5L).map { weekMonday.plusDays(it) }
    val lessons = ScheduleRepository.lessonsFor(selectedDate)
    val weekNumber = ScheduleRepository.academicWeekNumber(selectedDate)
    val weekType = ScheduleRepository.weekTypeFor(selectedDate)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text(
                text = "Расписание",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Полина Молоткова",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

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
                        text = monthTitle(weekMonday),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    if (weekNumber != null && weekType != null) {
                        WeekBadge(weekNumber, weekType)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                days.forEach { date ->
                    DayChip(
                        date = date,
                        selected = date == selectedDate,
                        isToday = date == today,
                        onClick = { selectedDate = date },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = { selectedDate = today },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Сегодня")
            }
        }

        HorizontalDivider()

        AnimatedContent(
            targetState = selectedDate,
            label = "selected-date"
        ) { date ->
            val dateLessons = ScheduleRepository.lessonsFor(date)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 14.dp,
                    bottom = 28.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = longDate(date),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = lessonCountText(dateLessons.size),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (dateLessons.isEmpty()) {
                    item { EmptyDay(date) }
                } else {
                    items(dateLessons) { lesson ->
                        LessonCard(
                            lesson = lesson,
                            isToday = date == today,
                            now = LocalTime.now()
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Legend()
                }
            }
        }
    }
}

@Composable
private fun WeekBadge(number: Int, type: WeekType) {
    val isOdd = type == WeekType.ODD
    val color = if (isOdd) greenOdd else yellowEven
    val label = if (isOdd) "нечётная" else "чётная"
    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.28f))
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
        Text("$number-я неделя • $label", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DayChip(
    date: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val short = date.dayOfWeek.getDisplayName(TextStyle.SHORT, ru).replaceFirstChar { it.uppercase() }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(short.take(2), fontSize = 11.sp, color = fg.copy(alpha = 0.8f))
        Text(date.dayOfMonth.toString(), fontWeight = FontWeight.Bold, color = fg, fontSize = 17.sp)
        if (isToday) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (selected) fg else MaterialTheme.colorScheme.primary)
            )
        } else {
            Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
private fun LessonCard(lesson: Lesson, isToday: Boolean, now: LocalTime) {
    val accent = when (lesson.weekType) {
        WeekType.ODD -> greenOdd
        WeekType.EVEN -> yellowEven
        WeekType.EXACT_DATE -> exactBlue
    }
    val startTime = LocalTime.parse(lesson.start)
    val endTime = LocalTime.parse(lesson.end)
    val isCurrent = isToday && !now.isBefore(startTime) && now.isBefore(endTime)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(150.dp)
                    .background(accent)
            )
            Column(modifier = Modifier.padding(14.dp).weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${lesson.start}–${lesson.end}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${lesson.pair}-я пара",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    TypeBadge(lesson.weekType, isCurrent)
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = lesson.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(lesson.group, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(lesson.room, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(type: WeekType, isCurrent: Boolean) {
    val (label, color) = when (type) {
        WeekType.ODD -> "нечётная" to greenOdd
        WeekType.EVEN -> "чётная" to yellowEven
        WeekType.EXACT_DATE -> "по дате" to exactBlue
    }
    Text(
        text = if (isCurrent) "идёт сейчас" else label,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.25f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun EmptyDay(date: LocalDate) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Пар нет", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                text = if (ScheduleRepository.academicWeekNumber(date) == null)
                    "Расписание загружено на осенний семестр 2026 года."
                else "На выбранную дату занятий в Excel нет.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Legend() {
    Column {
        Text("Обозначения", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(greenOdd, "нечётная")
            LegendDot(yellowEven, "чётная")
            LegendDot(exactBlue, "точная дата")
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Цвета чётности перенесены из исходного Excel: зелёный — нечётные недели, жёлтый — чётные.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun monthTitle(monday: LocalDate): String {
    val sunday = monday.plusDays(6)
    val firstMonth = monday.month.getDisplayName(TextStyle.FULL, ru)
    val secondMonth = sunday.month.getDisplayName(TextStyle.FULL, ru)
    val title = if (monday.month == sunday.month) {
        "$firstMonth ${monday.year}"
    } else {
        "$firstMonth — $secondMonth ${sunday.year}"
    }
    return title.replaceFirstChar { it.uppercase() }
}

private fun longDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", ru)
    return date.format(formatter).replaceFirstChar { it.uppercase() }
}

private fun lessonCountText(count: Int): String {
    val form = when {
        count % 10 == 1 && count % 100 != 11 -> "занятие"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "занятия"
        else -> "занятий"
    }
    return "$count $form"
}
