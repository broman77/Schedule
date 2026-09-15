@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ru.mgsu.molotkova.schedule

import android.content.Context
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import org.json.JSONArray
import org.json.JSONObject
import ru.mgsu.molotkova.schedule.data.Lesson
import ru.mgsu.molotkova.schedule.data.ScheduleRepository
import ru.mgsu.molotkova.schedule.data.WeekType
import ru.mgsu.molotkova.schedule.ui.theme.MolotkovaScheduleTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MolotkovaScheduleTheme {
                MgsuScheduleApp()
            }
        }
    }
}

private enum class HomeTab { TODAY, WEEK, MONTH, TASKS, SETTINGS }

private data class SavedTask(
    val id: String,
    val title: String,
    val dueDate: String,
    val done: Boolean
)

private val MgsuBlue = Color(0xFF0F3C73)
private val ScreenBg = Color(0xFFF5F7FA)
private val EvenGreen = Color(0xFF92D050)
private val OddYellow = Color(0xFFFFE800)
private val ExactBlue = Color(0xFF6E9FD1)
private val Ru = Locale("ru", "RU")

@Composable
private fun MgsuScheduleApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val today = LocalDate.now()
    var tab by remember { mutableStateOf(HomeTab.TODAY) }
    var weekAnchor by remember { mutableStateOf(today) }
    var selectedDate by remember { mutableStateOf(today) }
    var searchOpen by remember { mutableStateOf(false) }
    var tasks by remember { mutableStateOf(loadTasks(context)) }
    var taskDialogOpen by remember { mutableStateOf(false) }

    fun saveTaskList(value: List<SavedTask>) {
        tasks = value
        saveTasks(context, value)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Молоткова П.А.",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        val number = ScheduleRepository.academicWeekNumber(today)
                        val type = ScheduleRepository.weekTypeFor(today)
                        Text(
                            text = if (number != null && type != null) {
                                "Учебная неделя №$number · ${if (type == WeekType.EVEN) "чётная" else "нечётная"}"
                            } else {
                                "Вне учебного семестра"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { searchOpen = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Поиск")
                    }
                    IconButton(onClick = {
                        selectedDate = today
                        weekAnchor = today
                        tab = HomeTab.TODAY
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Сегодня")
                    }
                    IconButton(onClick = { tab = HomeTab.SETTINGS }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MgsuBlue,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.height(72.dp)) {
                NavigationBarItem(
                    selected = tab == HomeTab.TODAY,
                    onClick = { tab = HomeTab.TODAY },
                    icon = { Icon(Icons.Default.Today, null) },
                    label = { BottomLabel("Сегодня") }
                )
                NavigationBarItem(
                    selected = tab == HomeTab.WEEK,
                    onClick = { tab = HomeTab.WEEK },
                    icon = { Icon(Icons.Default.ViewWeek, null) },
                    label = { BottomLabel("Неделя") }
                )
                NavigationBarItem(
                    selected = tab == HomeTab.MONTH,
                    onClick = { tab = HomeTab.MONTH },
                    icon = { Icon(Icons.Default.CalendarMonth, null) },
                    label = { BottomLabel("Месяц") }
                )
                NavigationBarItem(
                    selected = tab == HomeTab.TASKS,
                    onClick = { tab = HomeTab.TASKS },
                    icon = { Icon(Icons.Default.TaskAlt, null) },
                    label = { BottomLabel("Задания") }
                )
                NavigationBarItem(
                    selected = tab == HomeTab.SETTINGS,
                    onClick = { tab = HomeTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { BottomLabel("Настройки") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(ScreenBg)
        ) {
            when (tab) {
                HomeTab.TODAY -> TodayScreen(
                    today = today,
                    onOpenWeek = {
                        weekAnchor = today
                        tab = HomeTab.WEEK
                    }
                )

                HomeTab.WEEK -> WeekScreen(
                    anchor = weekAnchor,
                    onAnchorChange = { weekAnchor = it },
                    onToday = { weekAnchor = today }
                )

                HomeTab.MONTH -> MonthScreen(
                    initialDate = selectedDate,
                    onSelectedDate = { selectedDate = it }
                )

                HomeTab.TASKS -> TasksScreen(
                    tasks = tasks,
                    onAdd = { taskDialogOpen = true },
                    onToggle = { id ->
                        saveTaskList(tasks.map { if (it.id == id) it.copy(done = !it.done) else it })
                    },
                    onDelete = { id -> saveTaskList(tasks.filterNot { it.id == id }) }
                )

                HomeTab.SETTINGS -> SettingsScreen(
                    onToday = {
                        selectedDate = today
                        weekAnchor = today
                        tab = HomeTab.TODAY
                    }
                )
            }
        }
    }

    if (searchOpen) {
        SearchDialog(onDismiss = { searchOpen = false })
    }

    if (taskDialogOpen) {
        AddTaskDialog(
            onDismiss = { taskDialogOpen = false },
            onSave = { title, dueDate ->
                saveTaskList(
                    tasks + SavedTask(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        dueDate = dueDate,
                        done = false
                    )
                )
                taskDialogOpen = false
            }
        )
    }
}

@Composable
private fun BottomLabel(text: String) {
    Text(text = text, fontSize = 10.sp, maxLines = 1)
}

@Composable
private fun TodayScreen(today: LocalDate, onOpenWeek: () -> Unit) {
    val lessons = ScheduleRepository.lessonsFor(today)
    val now = LocalTime.now()
    val nextLesson = lessons.firstOrNull { lesson ->
        runCatching { LocalTime.parse(lesson.end) }.getOrNull()?.let { !it.isBefore(now) } == true
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MgsuBlue,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = longDate(today),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (lessons.isEmpty()) "Сегодня занятий нет" else "Сегодня ${lessons.size} ${lessonWord(lessons.size)}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    )
                    val number = ScheduleRepository.academicWeekNumber(today)
                    val type = ScheduleRepository.weekTypeFor(today)
                    if (number != null && type != null) {
                        Spacer(Modifier.height(8.dp))
                        WeekBadge(number, type, dark = true)
                    }
                }
            }
        }

        if (nextLesson != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Today, null, tint = MgsuBlue)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Ближайшая пара", fontWeight = FontWeight.Bold)
                            Text(
                                "${nextLesson.start}–${nextLesson.end} · ${nextLesson.subject}",
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }

        if (lessons.isEmpty()) {
            item { EmptyScheduleCard("На сегодня занятий в расписании нет.") }
        } else {
            items(lessons) { lesson -> LessonCard(lesson) }
        }

        item {
            OutlinedButton(onClick = onOpenWeek, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.ViewWeek, null)
                Spacer(Modifier.width(8.dp))
                Text("Открыть всю неделю")
            }
        }
    }
}

@Composable
private fun WeekScreen(
    anchor: LocalDate,
    onAnchorChange: (LocalDate) -> Unit,
    onToday: () -> Unit
) {
    val monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = (0L..5L).map { monday.plusDays(it) }
    val weekNumber = ScheduleRepository.academicWeekNumber(monday)
    val weekType = ScheduleRepository.weekTypeFor(monday)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { onAnchorChange(anchor.minusWeeks(1)) }) {
                            Icon(Icons.Default.ChevronLeft, "Предыдущая неделя")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(weekRange(monday), fontWeight = FontWeight.Bold)
                            if (weekNumber != null && weekType != null) {
                                WeekBadge(weekNumber, weekType)
                            }
                        }
                        IconButton(onClick = { onAnchorChange(anchor.plusWeeks(1)) }) {
                            Icon(Icons.Default.ChevronRight, "Следующая неделя")
                        }
                    }
                    TextButton(onClick = onToday, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Текущая неделя")
                    }
                }
            }
        }

        days.forEach { date ->
            val lessons = ScheduleRepository.lessonsFor(date)
            item {
                DayHeader(date = date, count = lessons.size)
            }
            if (lessons.isEmpty()) {
                item { EmptyScheduleCard("Пар нет") }
            } else {
                items(lessons) { lesson -> LessonCard(lesson) }
            }
        }
    }
}

@Composable
private fun MonthScreen(
    initialDate: LocalDate,
    onSelectedDate: (LocalDate) -> Unit
) {
    var selected by remember { mutableStateOf(initialDate) }
    var month by remember { mutableStateOf(YearMonth.from(initialDate)) }
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val cells = (0 until offset).map< Int, LocalDate?> { null } +
        (1..month.lengthOfMonth()).map { month.atDay(it) }
    val rows = (cells.size + 6) / 7

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { month = month.minusMonths(1) }) {
                            Icon(Icons.Default.ChevronLeft, "Предыдущий месяц")
                        }
                        Text(monthTitle(month), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { month = month.plusMonths(1) }) {
                            Icon(Icons.Default.ChevronRight, "Следующий месяц")
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                            Box(
                                modifier = Modifier
                                    .width(46.dp)
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(day, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        userScrollEnabled = false,
                        modifier = Modifier.height((rows * 52).dp)
                    ) {
                        items(cells.size) { index ->
                            val date = cells[index]
                            if (date == null) {
                                Box(Modifier.height(50.dp))
                            } else {
                                MonthDayCell(
                                    date = date,
                                    selected = date == selected,
                                    onClick = {
                                        selected = date
                                        onSelectedDate(date)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            DayHeader(selected, ScheduleRepository.lessonsFor(selected).size)
        }

        val selectedLessons = ScheduleRepository.lessonsFor(selected)
        if (selectedLessons.isEmpty()) {
            item { EmptyScheduleCard("На выбранную дату занятий нет.") }
        } else {
            items(selectedLessons) { lesson -> LessonCard(lesson) }
        }
    }
}

@Composable
private fun MonthDayCell(date: LocalDate, selected: Boolean, onClick: () -> Unit) {
    val hasLessons = ScheduleRepository.lessonsFor(date).isNotEmpty()
    val type = ScheduleRepository.weekTypeFor(date)
    val dot = when (type) {
        WeekType.EVEN -> EvenGreen
        WeekType.ODD -> OddYellow
        else -> ExactBlue
    }

    Box(
        modifier = Modifier
            .height(50.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MgsuBlue else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                color = if (selected) Color.White else Color.Black,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            if (hasLessons) {
                Box(
                    Modifier
                        .padding(top = 3.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else dot)
                )
            }
        }
    }
}

@Composable
private fun TasksScreen(
    tasks: List<SavedTask>,
    onAdd: () -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = MgsuBlue) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Задания", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
                        Text("Личные заметки и дела", color = Color.White.copy(alpha = .86f))
                    }
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.White)
                    }
                }
            }
        }

        if (tasks.isEmpty()) {
            item { EmptyScheduleCard("Заданий пока нет. Нажмите +, чтобы добавить.") }
        } else {
            items(tasks, key = { it.id }) { task ->
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = task.done, onCheckedChange = { onToggle(task.id) })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, fontWeight = FontWeight.SemiBold)
                            if (task.dueDate.isNotBlank()) {
                                Text("Срок: ${task.dueDate}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        IconButton(onClick = { onDelete(task.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            }
        }

        item {
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Добавить задание")
            }
        }
    }
}

@Composable
private fun SettingsScreen(onToday: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = MgsuBlue) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("Настройки", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
                    Text("Расписание МГСУ", color = Color.White.copy(alpha = .86f))
                }
            }
        }
        item {
            InfoCard(
                title = "Профиль",
                text = "Молоткова П.А.\nПриложение сразу открывает персональное расписание — без выбора студент/преподаватель."
            )
        }
        item {
            InfoCard(
                title = "Источник расписания",
                text = "Расписание перенесено из Excel. Интернет для просмотра занятий не требуется."
            )
        }
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("Цвета недель", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    LegendLine(EvenGreen, "Чётная неделя — зелёный")
                    LegendLine(OddYellow, "Нечётная неделя — жёлтый")
                    LegendLine(ExactBlue, "Занятие по конкретной дате")
                }
            }
        }
        item {
            InfoCard(
                title = "Версия",
                text = "${BuildConfig.VERSION_NAME}\nПерсональная версия для Молотковой П.А."
            )
        }
        item {
            Button(onClick = onToday, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Today, null)
                Spacer(Modifier.width(8.dp))
                Text("Вернуться к сегодняшнему расписанию")
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, text: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(text, color = Color.DarkGray)
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, count: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE9F1FA)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(longDate(date), fontWeight = FontWeight.Bold)
                val number = ScheduleRepository.academicWeekNumber(date)
                val type = ScheduleRepository.weekTypeFor(date)
                if (number != null && type != null) {
                    Text(
                        "Неделя №$number · ${if (type == WeekType.EVEN) "чётная" else "нечётная"}",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            }
            Text("$count", color = MgsuBlue, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
    }
}

@Composable
private fun LessonCard(lesson: Lesson) {
    val accent = when (lesson.weekType) {
        WeekType.EVEN -> EvenGreen
        WeekType.ODD -> OddYellow
        WeekType.EXACT_DATE -> ExactBlue
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(accent)
        )
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "${lesson.start}–${lesson.end}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("${lesson.pair}-я пара", fontSize = 12.sp, color = Color.Gray)
                }
                TypeBadge(lesson.weekType)
            }
            Spacer(Modifier.height(9.dp))
            Text(lesson.subject, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.People, null, tint = Color.Gray, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(lesson.group, color = Color.DarkGray)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(lesson.room, color = Color.DarkGray)
            }
        }
    }
}

@Composable
private fun TypeBadge(type: WeekType) {
    val (text, color) = when (type) {
        WeekType.EVEN -> "чётная" to EvenGreen
        WeekType.ODD -> "нечётная" to OddYellow
        WeekType.EXACT_DATE -> "по дате" to ExactBlue
    }
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = color.copy(alpha = .25f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WeekBadge(number: Int, type: WeekType, dark: Boolean = false) {
    val color = if (type == WeekType.EVEN) EvenGreen else OddYellow
    val label = if (type == WeekType.EVEN) "чётная" else "нечётная"
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (dark) Color.White.copy(alpha = .16f) else color.copy(alpha = .24f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            "$number-я неделя · $label",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (dark) Color.White else Color.Black
        )
    }
}

@Composable
private fun EmptyScheduleCard(text: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
        Text(
            text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
    }
}

@Composable
private fun LegendLine(color: Color, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.DarkGray)
    }
}

@Composable
private fun SearchDialog(onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val all = ScheduleRepository.allLessons().distinctBy {
        "${it.subject}|${it.group}|${it.room}|${it.weekType}|${it.start}"
    }
    val q = query.trim().lowercase(Ru)
    val results = if (q.isBlank()) {
        all.take(20)
    } else {
        all.filter {
            it.subject.lowercase(Ru).contains(q) ||
                it.group.lowercase(Ru).contains(q) ||
                it.room.lowercase(Ru).contains(q)
        }.take(30)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Поиск по расписанию") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Предмет, группа или аудитория") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (results.isEmpty()) {
                        item { Text("Ничего не найдено", color = Color.Gray) }
                    } else {
                        items(results) { lesson ->
                            Surface(shape = RoundedCornerShape(12.dp), color = ScreenBg) {
                                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                    Text(lesson.subject, fontWeight = FontWeight.SemiBold)
                                    Text("${lesson.group} · ${lesson.room} · ${lesson.start}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(LocalDate.now().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое задание") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Срок, ГГГГ-ММ-ДД") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onSave(title.trim(), dueDate.trim()) },
                enabled = title.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

private fun longDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Ru)
    return date.format(formatter).replaceFirstChar { it.uppercase() }
}

private fun weekRange(monday: LocalDate): String {
    val saturday = monday.plusDays(5)
    val start = monday.format(DateTimeFormatter.ofPattern("d MMM", Ru))
    val end = saturday.format(DateTimeFormatter.ofPattern("d MMM yyyy", Ru))
    return "$start — $end"
}

private fun monthTitle(month: YearMonth): String {
    val name = month.month.getDisplayName(TextStyle.FULL, Ru)
    return "${name.replaceFirstChar { it.uppercase() }} ${month.year}"
}

private fun lessonWord(count: Int): String = when {
    count % 10 == 1 && count % 100 != 11 -> "занятие"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "занятия"
    else -> "занятий"
}

private fun loadTasks(context: Context): List<SavedTask> {
    val raw = context.getSharedPreferences("molotkova_schedule", Context.MODE_PRIVATE)
        .getString("tasks", "[]") ?: "[]"
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    SavedTask(
                        id = item.optString("id"),
                        title = item.optString("title"),
                        dueDate = item.optString("dueDate"),
                        done = item.optBoolean("done", false)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun saveTasks(context: Context, tasks: List<SavedTask>) {
    val array = JSONArray()
    tasks.forEach { task ->
        array.put(
            JSONObject()
                .put("id", task.id)
                .put("title", task.title)
                .put("dueDate", task.dueDate)
                .put("done", task.done)
        )
    }
    context.getSharedPreferences("molotkova_schedule", Context.MODE_PRIVATE)
        .edit()
        .putString("tasks", array.toString())
        .apply()
}
