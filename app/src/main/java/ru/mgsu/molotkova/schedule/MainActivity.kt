@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ru.mgsu.molotkova.schedule

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import ru.mgsu.molotkova.schedule.data.Lesson
import ru.mgsu.molotkova.schedule.data.ScheduleRepository
import ru.mgsu.molotkova.schedule.data.WeekType
import ru.mgsu.molotkova.schedule.ui.theme.MolotkovaScheduleTheme
import java.time.DayOfWeek
import java.time.Duration
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

private data class LocalLesson(
    val id: String,
    val date: String,
    val pair: Int,
    val start: String,
    val end: String,
    val subject: String,
    val room: String,
    val group: String
)

private data class LocalCalendarState(
    val custom: List<LocalLesson> = emptyList(),
    val hiddenOfficialKeys: Set<String> = emptySet()
)

private data class DisplayLesson(
    val stableId: String,
    val lesson: Lesson,
    val customId: String? = null,
    val officialKey: String? = null
)

private data class AppPalette(
    val id: String,
    val title: String,
    val primary: Color,
    val primarySoft: Color,
    val background: Color,
    val surface: Color,
    val text: Color,
    val subtext: Color,
    val even: Color,
    val odd: Color,
    val exact: Color
)

private val palettes = listOf(
    AppPalette(
        id = "ocean",
        title = "Спокойная синяя",
        primary = Color(0xFF214D73),
        primarySoft = Color(0xFFE7F0F7),
        background = Color(0xFFF4F7F9),
        surface = Color(0xFFFFFFFF),
        text = Color(0xFF1D2730),
        subtext = Color(0xFF65717B),
        even = Color(0xFF83AE96),
        odd = Color(0xFFD0AC62),
        exact = Color(0xFF789CBF)
    ),
    AppPalette(
        id = "graphite",
        title = "Графит",
        primary = Color(0xFF3F4A54),
        primarySoft = Color(0xFFECEFF1),
        background = Color(0xFFF5F5F4),
        surface = Color(0xFFFFFFFF),
        text = Color(0xFF25292D),
        subtext = Color(0xFF6F767C),
        even = Color(0xFF7FA08A),
        odd = Color(0xFFC1A06A),
        exact = Color(0xFF8296A8)
    ),
    AppPalette(
        id = "teal",
        title = "Бирюзовая",
        primary = Color(0xFF286A69),
        primarySoft = Color(0xFFE2F0EF),
        background = Color(0xFFF3F8F7),
        surface = Color(0xFFFFFFFF),
        text = Color(0xFF20302F),
        subtext = Color(0xFF607472),
        even = Color(0xFF7FAF91),
        odd = Color(0xFFC9A668),
        exact = Color(0xFF6E9EAA)
    ),
    AppPalette(
        id = "violet",
        title = "Лавандовая",
        primary = Color(0xFF65557A),
        primarySoft = Color(0xFFF0EBF5),
        background = Color(0xFFF7F5F9),
        surface = Color(0xFFFFFFFF),
        text = Color(0xFF2D2832),
        subtext = Color(0xFF756D7D),
        even = Color(0xFF86A58E),
        odd = Color(0xFFC2A16C),
        exact = Color(0xFF8B84AD)
    )
)

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
    var calendarState by remember { mutableStateOf(loadCalendarState(context)) }
    var editorDate by remember { mutableStateOf<LocalDate?>(null) }
    var editingLesson by remember { mutableStateOf<DisplayLesson?>(null) }
    var deleteCandidate by remember { mutableStateOf<Pair<LocalDate, DisplayLesson>?>(null) }
    var paletteId by remember { mutableStateOf(loadPaletteId(context)) }
    val palette = palettes.firstOrNull { it.id == paletteId } ?: palettes.first()
    var notificationsEnabled by remember { mutableStateOf(loadNotificationsEnabled(context)) }
    var reminderMinutes by remember { mutableIntStateOf(loadReminderMinutes(context)) }

    var pendingNotificationEnable by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingNotificationEnable) {
            pendingNotificationEnable = false
            if (granted) {
                notificationsEnabled = true
                saveNotificationsEnabled(context, true)
                LessonNotificationScheduler.scheduleNextMonth(context, reminderMinutes)
            } else {
                notificationsEnabled = false
                saveNotificationsEnabled(context, false)
            }
        }
    }

    LaunchedEffect(Unit) {
        LessonNotificationScheduler.createChannel(context)
        if (notificationsEnabled && hasNotificationPermission(context)) {
            LessonNotificationScheduler.scheduleNextMonth(context, reminderMinutes)
        }
    }

    fun saveTaskList(value: List<SavedTask>) {
        tasks = value
        saveTasks(context, value)
    }

    fun saveCalendar(value: LocalCalendarState) {
        calendarState = value
        saveCalendarState(context, value)
        if (notificationsEnabled && hasNotificationPermission(context)) {
            LessonNotificationScheduler.scheduleNextMonth(context, reminderMinutes)
        }
    }

    fun requestNotifications(enabled: Boolean) {
        if (!enabled) {
            notificationsEnabled = false
            saveNotificationsEnabled(context, false)
            LessonNotificationScheduler.cancelNextMonth(context)
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission(context)) {
            pendingNotificationEnable = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationsEnabled = true
            saveNotificationsEnabled(context, true)
            LessonNotificationScheduler.scheduleNextMonth(context, reminderMinutes)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Молоткова П.А.", fontWeight = FontWeight.Bold, maxLines = 1)
                        val number = ScheduleRepository.academicWeekNumber(today)
                        val type = ScheduleRepository.weekTypeFor(today)
                        Text(
                            text = if (number != null && type != null) {
                                "Неделя №$number · ${if (type == WeekType.EVEN) "чётная" else "нечётная"}"
                            } else "Вне учебного семестра",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { searchOpen = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Поиск")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = palette.surface, modifier = Modifier.height(72.dp)) {
                val navColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = palette.primary,
                    selectedTextColor = palette.primary,
                    indicatorColor = palette.primarySoft,
                    unselectedIconColor = palette.subtext,
                    unselectedTextColor = palette.subtext
                )
                NavigationBarItem(selected = tab == HomeTab.TODAY, onClick = { tab = HomeTab.TODAY }, icon = { Icon(Icons.Default.Today, null) }, label = { BottomLabel("Сегодня") }, colors = navColors)
                NavigationBarItem(selected = tab == HomeTab.WEEK, onClick = { tab = HomeTab.WEEK }, icon = { Icon(Icons.Default.ViewWeek, null) }, label = { BottomLabel("Неделя") }, colors = navColors)
                NavigationBarItem(selected = tab == HomeTab.MONTH, onClick = { tab = HomeTab.MONTH }, icon = { Icon(Icons.Default.CalendarMonth, null) }, label = { BottomLabel("Месяц") }, colors = navColors)
                NavigationBarItem(selected = tab == HomeTab.TASKS, onClick = { tab = HomeTab.TASKS }, icon = { Icon(Icons.Default.TaskAlt, null) }, label = { BottomLabel("Задания") }, colors = navColors)
                NavigationBarItem(selected = tab == HomeTab.SETTINGS, onClick = { tab = HomeTab.SETTINGS }, icon = { Icon(Icons.Default.Settings, null) }, label = { BottomLabel("Настройки") }, colors = navColors)
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(palette.background)
        ) {
            when (tab) {
                HomeTab.TODAY -> TodayScreen(
                    today = today,
                    palette = palette,
                    calendarState = calendarState,
                    onOpenWeek = { weekAnchor = today; tab = HomeTab.WEEK },
                    onAdd = { editorDate = today; editingLesson = null },
                    onEdit = { lesson -> editorDate = today; editingLesson = lesson },
                    onDelete = { lesson -> deleteCandidate = today to lesson }
                )

                HomeTab.WEEK -> WeekScreen(
                    anchor = weekAnchor,
                    palette = palette,
                    calendarState = calendarState,
                    onAnchorChange = { weekAnchor = it },
                    onToday = { weekAnchor = today },
                    onAdd = { date -> editorDate = date; editingLesson = null },
                    onEdit = { date, lesson -> editorDate = date; editingLesson = lesson },
                    onDelete = { date, lesson -> deleteCandidate = date to lesson }
                )

                HomeTab.MONTH -> MonthScreen(
                    initialDate = selectedDate,
                    palette = palette,
                    calendarState = calendarState,
                    onSelectedDate = { selectedDate = it },
                    onAdd = { date -> editorDate = date; editingLesson = null },
                    onEdit = { date, lesson -> editorDate = date; editingLesson = lesson },
                    onDelete = { date, lesson -> deleteCandidate = date to lesson }
                )

                HomeTab.TASKS -> TasksScreen(
                    tasks = tasks,
                    palette = palette,
                    onAdd = { taskDialogOpen = true },
                    onToggle = { id -> saveTaskList(tasks.map { if (it.id == id) it.copy(done = !it.done) else it }) },
                    onDelete = { id -> saveTaskList(tasks.filterNot { it.id == id }) }
                )

                HomeTab.SETTINGS -> SettingsScreen(
                    palette = palette,
                    selectedPaletteId = paletteId,
                    notificationsEnabled = notificationsEnabled,
                    reminderMinutes = reminderMinutes,
                    onPalette = { newId ->
                        paletteId = newId
                        savePaletteId(context, newId)
                    },
                    onNotifications = ::requestNotifications,
                    onReminder = { minutes ->
                        reminderMinutes = minutes
                        saveReminderMinutes(context, minutes)
                        if (notificationsEnabled && hasNotificationPermission(context)) {
                            LessonNotificationScheduler.scheduleNextMonth(context, minutes)
                        }
                    },
                    onResetLocal = { saveCalendar(LocalCalendarState()) }
                )
            }
        }
    }

    if (searchOpen) {
        SearchDialog(
            palette = palette,
            calendarState = calendarState,
            onDismiss = { searchOpen = false }
        )
    }

    if (taskDialogOpen) {
        AddTaskDialog(
            onDismiss = { taskDialogOpen = false },
            onSave = { title, dueDate ->
                saveTaskList(tasks + SavedTask(UUID.randomUUID().toString(), title, dueDate, false))
                taskDialogOpen = false
            }
        )
    }

    editorDate?.let { date ->
        LessonEditorDialog(
            date = date,
            initial = editingLesson,
            palette = palette,
            onDismiss = { editorDate = null; editingLesson = null },
            onSave = { local ->
                val initial = editingLesson
                if (initial?.customId != null) {
                    saveCalendar(calendarState.copy(custom = calendarState.custom.map { if (it.id == initial.customId) local.copy(id = initial.customId) else it }))
                } else if (initial?.officialKey != null) {
                    saveCalendar(
                        calendarState.copy(
                            hiddenOfficialKeys = calendarState.hiddenOfficialKeys + initial.officialKey,
                            custom = calendarState.custom + local.copy(id = UUID.randomUUID().toString())
                        )
                    )
                } else {
                    saveCalendar(calendarState.copy(custom = calendarState.custom + local.copy(id = UUID.randomUUID().toString())))
                }
                editorDate = null
                editingLesson = null
            }
        )
    }

    deleteCandidate?.let { (date, display) ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Удалить занятие?") },
            text = { Text("${longDate(date)}\n${display.lesson.start}–${display.lesson.end} · ${display.lesson.subject}\n\nИзменение применяется только к этой дате.") },
            confirmButton = {
                TextButton(onClick = {
                    val value = if (display.customId != null) {
                        calendarState.copy(custom = calendarState.custom.filterNot { it.id == display.customId })
                    } else if (display.officialKey != null) {
                        calendarState.copy(hiddenOfficialKeys = calendarState.hiddenOfficialKeys + display.officialKey)
                    } else calendarState
                    saveCalendar(value)
                    deleteCandidate = null
                }) { Text("Удалить", color = Color(0xFF9B3F3F)) }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun BottomLabel(text: String) {
    Text(text, fontSize = 10.sp, maxLines = 1)
}

@Composable
private fun TodayScreen(
    today: LocalDate,
    palette: AppPalette,
    calendarState: LocalCalendarState,
    onOpenWeek: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (DisplayLesson) -> Unit,
    onDelete: (DisplayLesson) -> Unit
) {
    val lessons = effectiveLessonsForDate(today, calendarState)
    val now = LocalTime.now()
    val next = lessons.firstOrNull { display ->
        runCatching { LocalTime.parse(display.lesson.end) }.getOrNull()?.let { !it.isBefore(now) } == true
    }
    val breaks = calculateBreaks(lessons)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = palette.primary, shadowElevation = 2.dp) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(longDate(today), color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (lessons.isEmpty()) "Сегодня занятий нет" else "Сегодня ${lessons.size} ${lessonWord(lessons.size)}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 27.sp
                    )
                    val week = ScheduleRepository.academicWeekNumber(today)
                    val type = ScheduleRepository.weekTypeFor(today)
                    if (week != null && type != null) {
                        Spacer(Modifier.height(10.dp))
                        SoftWeekBadge(week, type, palette, dark = true)
                    }
                }
            }
        }

        if (next != null) {
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = palette.surface) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = palette.primarySoft) {
                            Icon(Icons.Default.EventAvailable, null, tint = palette.primary, modifier = Modifier.padding(9.dp).size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Ближайшая пара", fontWeight = FontWeight.Bold, color = palette.text)
                            Text("${next.lesson.start}–${next.lesson.end} · ${next.lesson.subject}", color = palette.subtext)
                            if (next.lesson.room.isNotBlank()) Text(next.lesson.room, color = palette.subtext, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item { BreakSummaryCard(breaks, palette) }

        item {
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Добавить занятие")
            }
        }

        if (lessons.isEmpty()) {
            item { EmptyScheduleCard("На сегодня занятий в расписании нет.", palette) }
        } else {
            lessons.forEachIndexed { index, display ->
                item { LessonCard(display, palette, onEdit = { onEdit(display) }, onDelete = { onDelete(display) }) }
                breaks.firstOrNull { it.afterIndex == index }?.let { info ->
                    item { BreakCard(info, palette) }
                }
            }
        }

        item {
            OutlinedButton(onClick = onOpenWeek, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.ViewWeek, null)
                Spacer(Modifier.width(8.dp))
                Text("Открыть всю неделю")
            }
        }
    }
}

private data class BreakInfo(val afterIndex: Int, val minutes: Long)

private fun calculateBreaks(lessons: List<DisplayLesson>): List<BreakInfo> {
    if (lessons.size < 2) return emptyList()
    return lessons.zipWithNext().mapIndexedNotNull { index, (a, b) ->
        val end = runCatching { LocalTime.parse(a.lesson.end) }.getOrNull() ?: return@mapIndexedNotNull null
        val start = runCatching { LocalTime.parse(b.lesson.start) }.getOrNull() ?: return@mapIndexedNotNull null
        val minutes = Duration.between(end, start).toMinutes()
        if (minutes > 0) BreakInfo(index, minutes) else null
    }
}

@Composable
private fun BreakSummaryCard(breaks: List<BreakInfo>, palette: AppPalette) {
    val windows = breaks.filter { it.minutes >= 30 }
    Surface(shape = RoundedCornerShape(18.dp), color = palette.surface) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, null, tint = palette.primary)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(if (windows.isEmpty()) "Больших окон нет" else "Окон: ${windows.size}", fontWeight = FontWeight.Bold, color = palette.text)
                Text(
                    when {
                        breaks.isEmpty() -> "Сегодня пары не идут подряд или занятий меньше двух."
                        windows.isEmpty() -> "Между парами только короткие перерывы."
                        else -> windows.joinToString(" · ") { formatDuration(it.minutes) }
                    },
                    color = palette.subtext,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun BreakCard(info: BreakInfo, palette: AppPalette) {
    val isWindow = info.minutes >= 30
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isWindow) palette.primarySoft else palette.background
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, null, tint = palette.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isWindow) "Окно · ${formatDuration(info.minutes)}" else "Перерыв · ${formatDuration(info.minutes)}",
                color = palette.text,
                fontWeight = if (isWindow) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun WeekScreen(
    anchor: LocalDate,
    palette: AppPalette,
    calendarState: LocalCalendarState,
    onAnchorChange: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onAdd: (LocalDate) -> Unit,
    onEdit: (LocalDate, DisplayLesson) -> Unit,
    onDelete: (LocalDate, DisplayLesson) -> Unit
) {
    val monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = (0L..5L).map { monday.plusDays(it) }
    val weekNumber = ScheduleRepository.academicWeekNumber(monday)
    val weekType = ScheduleRepository.weekTypeFor(monday)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = palette.surface) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = { onAnchorChange(anchor.minusWeeks(1)) }) { Icon(Icons.Default.ChevronLeft, "Предыдущая неделя") }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(weekRange(monday), fontWeight = FontWeight.Bold, color = palette.text)
                            if (weekNumber != null && weekType != null) SoftWeekBadge(weekNumber, weekType, palette)
                        }
                        IconButton(onClick = { onAnchorChange(anchor.plusWeeks(1)) }) { Icon(Icons.Default.ChevronRight, "Следующая неделя") }
                    }
                    TextButton(onClick = onToday, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Текущая неделя") }
                }
            }
        }

        days.forEach { date ->
            val lessons = effectiveLessonsForDate(date, calendarState)
            item { DayHeader(date, lessons.size, palette, onAdd = { onAdd(date) }) }
            if (lessons.isEmpty()) {
                item { EmptyScheduleCard("Пар нет", palette) }
            } else {
                val breaks = calculateBreaks(lessons)
                lessons.forEachIndexed { index, display ->
                    item { LessonCard(display, palette, onEdit = { onEdit(date, display) }, onDelete = { onDelete(date, display) }) }
                    breaks.firstOrNull { it.afterIndex == index }?.let { info -> item { BreakCard(info, palette) } }
                }
            }
        }
    }
}

@Composable
private fun MonthScreen(
    initialDate: LocalDate,
    palette: AppPalette,
    calendarState: LocalCalendarState,
    onSelectedDate: (LocalDate) -> Unit,
    onAdd: (LocalDate) -> Unit,
    onEdit: (LocalDate, DisplayLesson) -> Unit,
    onDelete: (LocalDate, DisplayLesson) -> Unit
) {
    var selected by remember { mutableStateOf(initialDate) }
    var month by remember { mutableStateOf(YearMonth.from(initialDate)) }
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val cells: List<LocalDate?> = List(offset) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = palette.surface) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, "Предыдущий месяц") }
                        Text(monthTitle(month), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = palette.text)
                        IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, "Следующий месяц") }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                            Box(Modifier.weight(1f).padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
                                Text(day, fontSize = 11.sp, color = palette.subtext)
                            }
                        }
                    }
                    cells.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            (week + List(7 - week.size) { null }).forEach { date ->
                                Box(Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                    if (date != null) {
                                        val hasLessons = effectiveLessonsForDate(date, calendarState).isNotEmpty()
                                        val selectedNow = date == selected
                                        Column(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selectedNow) palette.primary else Color.Transparent)
                                                .clickable { selected = date; onSelectedDate(date) }
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(date.dayOfMonth.toString(), color = if (selectedNow) Color.White else palette.text, fontWeight = if (selectedNow) FontWeight.Bold else FontWeight.Normal)
                                            if (hasLessons) Box(Modifier.padding(top = 3.dp).size(5.dp).clip(CircleShape).background(if (selectedNow) Color.White else palette.primary))
                                            else Spacer(Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val lessons = effectiveLessonsForDate(selected, calendarState)
        item { DayHeader(selected, lessons.size, palette, onAdd = { onAdd(selected) }) }
        if (lessons.isEmpty()) item { EmptyScheduleCard("На выбранную дату занятий нет.", palette) }
        else {
            val breaks = calculateBreaks(lessons)
            lessons.forEachIndexed { index, display ->
                item { LessonCard(display, palette, onEdit = { onEdit(selected, display) }, onDelete = { onDelete(selected, display) }) }
                breaks.firstOrNull { it.afterIndex == index }?.let { info -> item { BreakCard(info, palette) } }
            }
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, count: Int, palette: AppPalette, onAdd: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = palette.primarySoft) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(longDate(date), fontWeight = FontWeight.Bold, color = palette.text)
                Text("$count ${lessonWord(count)}", fontSize = 12.sp, color = palette.subtext)
            }
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Добавить занятие", tint = palette.primary) }
        }
    }
}

@Composable
private fun LessonCard(display: DisplayLesson, palette: AppPalette, onEdit: () -> Unit, onDelete: () -> Unit) {
    val lesson = display.lesson
    val accent = weekAccent(lesson.weekType, palette)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(5.dp).background(accent))
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("${lesson.start}–${lesson.end}", fontWeight = FontWeight.Bold, color = palette.text, style = MaterialTheme.typography.titleMedium)
                    Text("${lesson.pair}-я пара", fontSize = 12.sp, color = palette.subtext)
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(38.dp)) { Icon(Icons.Default.Edit, "Редактировать", tint = palette.primary) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) { Icon(Icons.Default.Delete, "Удалить", tint = Color(0xFF9B5A5A)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(lesson.subject, fontWeight = FontWeight.SemiBold, color = palette.text, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            if (lesson.group.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, null, tint = palette.subtext, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(lesson.group, color = palette.subtext)
                }
            }
            if (lesson.room.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = palette.subtext, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(lesson.room, color = palette.subtext)
                }
            }
            Spacer(Modifier.height(9.dp))
            WeekTypeLabel(lesson.weekType, palette, custom = display.customId != null)
        }
    }
}

@Composable
private fun WeekTypeLabel(type: WeekType, palette: AppPalette, custom: Boolean) {
    val accent = weekAccent(type, palette)
    val text = if (custom) "изменено вручную" else when (type) {
        WeekType.EVEN -> "чётная · зелёный"
        WeekType.ODD -> "нечётная · жёлтый"
        WeekType.EXACT_DATE -> "по конкретной дате"
    }
    Surface(shape = RoundedCornerShape(100.dp), color = accent.copy(alpha = .18f)) {
        Text(text, Modifier.padding(horizontal = 9.dp, vertical = 4.dp), fontSize = 11.sp, color = palette.text)
    }
}

@Composable
private fun SoftWeekBadge(number: Int, type: WeekType, palette: AppPalette, dark: Boolean = false) {
    val accent = weekAccent(type, palette)
    Surface(shape = RoundedCornerShape(100.dp), color = if (dark) Color.White.copy(alpha = .15f) else accent.copy(alpha = .16f)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (dark) Color.White else accent))
            Spacer(Modifier.width(6.dp))
            Text(
                "$number-я неделя · ${if (type == WeekType.EVEN) "чётная" else "нечётная"}",
                color = if (dark) Color.White else palette.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun weekAccent(type: WeekType, palette: AppPalette): Color = when (type) {
    WeekType.EVEN -> palette.even
    WeekType.ODD -> palette.odd
    WeekType.EXACT_DATE -> palette.exact
}

@Composable
private fun EmptyScheduleCard(text: String, palette: AppPalette) {
    Surface(shape = RoundedCornerShape(18.dp), color = palette.surface) {
        Text(text, Modifier.fillMaxWidth().padding(22.dp), textAlign = TextAlign.Center, color = palette.subtext)
    }
}

@Composable
private fun TasksScreen(
    tasks: List<SavedTask>,
    palette: AppPalette,
    onAdd: () -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Задания", fontWeight = FontWeight.Black, fontSize = 26.sp, color = palette.text)
                    Text("Личные заметки и дедлайны", color = palette.subtext)
                }
                Button(onClick = onAdd) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Добавить") }
            }
        }
        if (tasks.isEmpty()) item { EmptyScheduleCard("Заданий пока нет.", palette) }
        items(tasks, key = { it.id }) { task ->
            Surface(shape = RoundedCornerShape(16.dp), color = palette.surface) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onToggle(task.id) }) {
                        Icon(if (task.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (task.done) palette.even else palette.primary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(task.title, fontWeight = FontWeight.SemiBold, color = palette.text)
                        if (task.dueDate.isNotBlank()) Text("Срок: ${task.dueDate}", fontSize = 12.sp, color = palette.subtext)
                    }
                    IconButton(onClick = { onDelete(task.id) }) { Icon(Icons.Default.Delete, "Удалить", tint = Color(0xFF9B5A5A)) }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    palette: AppPalette,
    selectedPaletteId: String,
    notificationsEnabled: Boolean,
    reminderMinutes: Int,
    onPalette: (String) -> Unit,
    onNotifications: (Boolean) -> Unit,
    onReminder: (Int) -> Unit,
    onResetLocal: () -> Unit
) {
    var confirmReset by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = palette.primary) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.mgsu_logo),
                            contentDescription = "Логотип МГСУ",
                            modifier = Modifier.padding(10.dp).size(64.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Расписание МГСУ", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("Молоткова П.А.", color = Color.White.copy(alpha = .88f))
                    }
                }
            }
        }

        item {
            SettingsCard("Уведомления", palette) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff, null, tint = palette.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Напоминания о занятиях", fontWeight = FontWeight.SemiBold, color = palette.text)
                        Text("Утром список занятий + напоминание перед каждой парой", color = palette.subtext, fontSize = 12.sp)
                    }
                    Switch(checked = notificationsEnabled, onCheckedChange = onNotifications)
                }
                Spacer(Modifier.height(12.dp))
                Text("Напомнить перед парой", fontWeight = FontWeight.SemiBold, color = palette.text)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(10, 30, 60).forEach { value ->
                        FilterChip(
                            selected = reminderMinutes == value,
                            onClick = { onReminder(value) },
                            label = { Text(if (value == 60) "1 час" else "$value мин") }
                        )
                    }
                }
            }
        }

        item {
            SettingsCard("Оформление", palette) {
                Text("Выберите спокойную цветовую тему", color = palette.subtext, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                palettes.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPalette(option.id) }
                            .padding(vertical = 9.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf(option.primary, option.even, option.odd).forEach { color -> Box(Modifier.size(16.dp).clip(CircleShape).background(color)) }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(option.title, Modifier.weight(1f), color = palette.text)
                        if (selectedPaletteId == option.id) Icon(Icons.Default.CheckCircle, null, tint = palette.primary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Зелёный всегда означает чётную неделю, жёлтый — нечётную; оттенки сделаны приглушёнными.", color = palette.subtext, fontSize = 12.sp)
            }
        }

        item {
            SettingsCard("Изменения расписания", palette) {
                Text("Добавленные, отредактированные и удалённые занятия хранятся только на этом устройстве.", color = palette.subtext, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) { Text("Сбросить мои изменения") }
            }
        }

        item {
            Text("Версия 0.2.0-beta", color = palette.subtext, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить изменения?") },
            text = { Text("Все вручную добавленные, отредактированные и удалённые занятия будут возвращены к исходному расписанию.") },
            confirmButton = { TextButton(onClick = { onResetLocal(); confirmReset = false }) { Text("Сбросить") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun SettingsCard(title: String, palette: AppPalette, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = palette.surface) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = palette.text)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SearchDialog(palette: AppPalette, calendarState: LocalCalendarState, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase(Ru)
    val dates = (0L..120L).map { ScheduleRepository.semesterStartMonday.plusDays(it) }
    val results = if (q.length < 2) emptyList() else dates.flatMap { date ->
        effectiveLessonsForDate(date, calendarState).filter { d ->
            listOf(d.lesson.subject, d.lesson.room, d.lesson.group).any { it.lowercase(Ru).contains(q) }
        }.map { date to it }
    }.take(40)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Поиск по расписанию") },
        text = {
            Column {
                OutlinedTextField(query, { query = it }, label = { Text("Предмет, группа или аудитория") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                if (q.length < 2) Text("Введите минимум 2 символа.", color = palette.subtext)
                else if (results.isEmpty()) Text("Ничего не найдено.", color = palette.subtext)
                else LazyColumn(Modifier.height(320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results) { (date, display) ->
                        Surface(shape = RoundedCornerShape(12.dp), color = palette.background) {
                            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                Text("${shortDate(date)} · ${display.lesson.start}", fontWeight = FontWeight.Bold, color = palette.text)
                                Text(display.lesson.subject, color = palette.text)
                                Text("${display.lesson.group} · ${display.lesson.room}", color = palette.subtext, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}

@Composable
private fun LessonEditorDialog(
    date: LocalDate,
    initial: DisplayLesson?,
    palette: AppPalette,
    onDismiss: () -> Unit,
    onSave: (LocalLesson) -> Unit
) {
    val original = initial?.lesson
    var subject by remember(initial?.stableId) { mutableStateOf(original?.subject.orEmpty()) }
    var group by remember(initial?.stableId) { mutableStateOf(original?.group.orEmpty()) }
    var room by remember(initial?.stableId) { mutableStateOf(original?.room.orEmpty()) }
    var pair by remember(initial?.stableId) { mutableStateOf(original?.pair?.toString() ?: "1") }
    var start by remember(initial?.stableId) { mutableStateOf(original?.start ?: "08:30") }
    var end by remember(initial?.stableId) { mutableStateOf(original?.end ?: "09:50") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Добавить занятие" else "Редактировать занятие") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text(longDate(date), color = palette.subtext, fontSize = 13.sp) }
                item { OutlinedTextField(subject, { subject = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(pair, { pair = it.filter(Char::isDigit).take(2) }, label = { Text("Пара") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(room, { room = it }, label = { Text("Аудитория") }, modifier = Modifier.weight(2f), singleLine = true)
                    }
                }
                item { OutlinedTextField(group, { group = it }, label = { Text("Группа") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(start, { start = it }, label = { Text("Начало") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(end, { end = it }, label = { Text("Конец") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
                if (error.isNotBlank()) item { Text(error, color = Color(0xFFA04444), fontSize = 12.sp) }
                item { Text("Редактирование исходной пары изменяет только выбранную дату.", color = palette.subtext, fontSize = 11.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val pairNumber = pair.toIntOrNull()
                val startTime = runCatching { LocalTime.parse(start) }.getOrNull()
                val endTime = runCatching { LocalTime.parse(end) }.getOrNull()
                if (subject.isBlank()) error = "Укажите название занятия."
                else if (pairNumber == null || pairNumber !in 1..12) error = "Номер пары должен быть от 1 до 12."
                else if (startTime == null || endTime == null || !endTime.isAfter(startTime)) error = "Проверьте время в формате ЧЧ:ММ."
                else onSave(
                    LocalLesson(
                        id = initial?.customId ?: UUID.randomUUID().toString(),
                        date = date.toString(),
                        pair = pairNumber,
                        start = start,
                        end = end,
                        subject = subject.trim(),
                        room = room.trim(),
                        group = group.trim()
                    )
                )
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое задание") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Задание") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(date, { date = it }, label = { Text("Срок, например 2026-10-01") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank()) onSave(title.trim(), date.trim()) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

private fun effectiveLessonsForDate(date: LocalDate, state: LocalCalendarState): List<DisplayLesson> {
    val official = ScheduleRepository.lessonsFor(date).mapNotNull { lesson ->
        val key = officialKey(date, lesson)
        if (key in state.hiddenOfficialKeys) null else DisplayLesson(stableId = key, lesson = lesson, officialKey = key)
    }
    val custom = state.custom.filter { it.date == date.toString() }.map { local ->
        DisplayLesson(
            stableId = "custom:${local.id}",
            customId = local.id,
            lesson = Lesson(
                dayOfWeek = date.dayOfWeek.value,
                pair = local.pair,
                start = local.start,
                end = local.end,
                subject = local.subject,
                room = local.room,
                group = local.group,
                weekType = WeekType.EXACT_DATE,
                exactDates = setOf(date)
            )
        )
    }
    return (official + custom).sortedWith(compareBy<DisplayLesson> { it.lesson.start }.thenBy { it.lesson.pair })
}

private fun officialKey(date: LocalDate, lesson: Lesson): String = listOf(
    date.toString(), lesson.pair, lesson.start, lesson.end, lesson.subject, lesson.room, lesson.group
).joinToString("|")

private fun formatDuration(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "$h ч $m мин"
        h > 0 -> "$h ч"
        else -> "$m мин"
    }
}

private fun lessonWord(count: Int): String = when {
    count % 10 == 1 && count % 100 != 11 -> "занятие"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "занятия"
    else -> "занятий"
}

private fun longDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Ru)).replaceFirstChar { it.uppercase() }
private fun shortDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d MMM", Ru))
private fun monthTitle(month: YearMonth): String = month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", Ru)).replaceFirstChar { it.uppercase() }
private fun weekRange(monday: LocalDate): String {
    val saturday = monday.plusDays(5)
    return "${monday.dayOfMonth} ${monday.month.getDisplayName(TextStyle.SHORT, Ru)} — ${saturday.dayOfMonth} ${saturday.month.getDisplayName(TextStyle.SHORT, Ru)}"
}

private const val PREFS = "molotkova_schedule_v2"

private fun loadTasks(context: Context): List<SavedTask> = runCatching {
    val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("tasks", "[]") ?: "[]"
    val array = JSONArray(raw)
    (0 until array.length()).map { index ->
        val obj = array.getJSONObject(index)
        SavedTask(obj.getString("id"), obj.getString("title"), obj.optString("dueDate"), obj.optBoolean("done"))
    }
}.getOrDefault(emptyList())

private fun saveTasks(context: Context, tasks: List<SavedTask>) {
    val array = JSONArray()
    tasks.forEach { task ->
        array.put(JSONObject().put("id", task.id).put("title", task.title).put("dueDate", task.dueDate).put("done", task.done))
    }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("tasks", array.toString()).apply()
}

private fun loadCalendarState(context: Context): LocalCalendarState = runCatching {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val customArray = JSONArray(prefs.getString("custom_lessons", "[]") ?: "[]")
    val custom = (0 until customArray.length()).map { i ->
        val o = customArray.getJSONObject(i)
        LocalLesson(
            id = o.getString("id"), date = o.getString("date"), pair = o.getInt("pair"),
            start = o.getString("start"), end = o.getString("end"), subject = o.getString("subject"),
            room = o.optString("room"), group = o.optString("group")
        )
    }
    val hiddenArray = JSONArray(prefs.getString("hidden_official", "[]") ?: "[]")
    val hidden = (0 until hiddenArray.length()).map { hiddenArray.getString(it) }.toSet()
    LocalCalendarState(custom, hidden)
}.getOrDefault(LocalCalendarState())

private fun saveCalendarState(context: Context, state: LocalCalendarState) {
    val custom = JSONArray()
    state.custom.forEach { l ->
        custom.put(JSONObject().put("id", l.id).put("date", l.date).put("pair", l.pair).put("start", l.start).put("end", l.end).put("subject", l.subject).put("room", l.room).put("group", l.group))
    }
    val hidden = JSONArray()
    state.hiddenOfficialKeys.forEach { hidden.put(it) }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString("custom_lessons", custom.toString())
        .putString("hidden_official", hidden.toString())
        .apply()
}

private fun loadPaletteId(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("palette", "ocean") ?: "ocean"
private fun savePaletteId(context: Context, id: String) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("palette", id).apply() }
private fun loadNotificationsEnabled(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("notifications", false)
private fun saveNotificationsEnabled(context: Context, value: Boolean) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("notifications", value).apply() }
private fun loadReminderMinutes(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("reminder_minutes", 30)
private fun saveReminderMinutes(context: Context, value: Int) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("reminder_minutes", value).apply() }

private fun hasNotificationPermission(context: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
