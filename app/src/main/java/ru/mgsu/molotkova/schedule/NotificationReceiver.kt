package ru.mgsu.molotkova.schedule

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import ru.mgsu.molotkova.schedule.data.Lesson
import ru.mgsu.molotkova.schedule.data.ScheduleRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class LessonNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LessonNotificationScheduler.createChannel(context)
        if (!LessonNotificationScheduler.hasPermission(context)) return

        val title = intent.getStringExtra("title") ?: "Расписание МГСУ"
        val text = intent.getStringExtra("text") ?: return
        val notificationId = intent.getIntExtra("notification_id", text.hashCode())

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                9001,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, LessonNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}

class ScheduleBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val prefs = context.getSharedPreferences(LessonNotificationScheduler.PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notifications", false)) return
        val minutes = prefs.getInt("reminder_minutes", 30)
        LessonNotificationScheduler.scheduleNextMonth(context, minutes)
    }
}

object LessonNotificationScheduler {
    const val CHANNEL_ID = "molotkova_lessons"
    const val PREFS = "molotkova_schedule_v2"

    private data class AlertLesson(
        val pair: Int,
        val start: String,
        val end: String,
        val subject: String,
        val room: String,
        val group: String,
        val key: String
    )

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Занятия Молотковой П.А.",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Список занятий на сегодня и напоминания перед парами"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun hasPermission(context: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun scheduleNextMonth(context: Context, reminderMinutes: Int) {
        createChannel(context)
        cancelNextMonth(context)
        val today = LocalDate.now()
        (0L..30L).forEach { offset ->
            val date = today.plusDays(offset)
            val lessons = effectiveLessons(context, date)
            if (lessons.isEmpty()) return@forEach

            val morningText = lessons.take(5).joinToString(" • ") { "${it.start} ${it.subject}" } +
                if (lessons.size > 5) " • ещё ${lessons.size - 5}" else ""
            schedule(
                context = context,
                requestCode = morningCode(date),
                at = LocalDateTime.of(date, LocalTime.of(7, 30)),
                title = "Сегодня ${lessons.size} ${lessonWord(lessons.size)}",
                text = morningText,
                notificationId = morningCode(date)
            )

            lessons.forEach { lesson ->
                val start = runCatching { LocalTime.parse(lesson.start) }.getOrNull() ?: return@forEach
                schedule(
                    context = context,
                    requestCode = lessonCode(date, lesson.key),
                    at = LocalDateTime.of(date, start).minusMinutes(reminderMinutes.toLong()),
                    title = "Скоро занятие · ${lesson.start}",
                    text = buildString {
                        append(lesson.subject)
                        if (lesson.group.isNotBlank()) append(" · ${lesson.group}")
                        if (lesson.room.isNotBlank()) append(" · ${lesson.room}")
                    },
                    notificationId = lessonCode(date, lesson.key)
                )
            }
        }
    }

    fun cancelNextMonth(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val today = LocalDate.now()
        (0L..30L).forEach { offset ->
            val date = today.plusDays(offset)
            pendingIntent(context, morningCode(date), "", "", morningCode(date), PendingIntent.FLAG_NO_CREATE)?.let(alarm::cancel)
            effectiveLessons(context, date).forEach { lesson ->
                pendingIntent(context, lessonCode(date, lesson.key), "", "", lessonCode(date, lesson.key), PendingIntent.FLAG_NO_CREATE)?.let(alarm::cancel)
            }
        }
    }

    private fun schedule(
        context: Context,
        requestCode: Int,
        at: LocalDateTime,
        title: String,
        text: String,
        notificationId: Int
    ) {
        val triggerAt = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerAt <= System.currentTimeMillis()) return
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context, requestCode, title, text, notificationId, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }

    private fun pendingIntent(
        context: Context,
        requestCode: Int,
        title: String,
        text: String,
        notificationId: Int,
        modeFlag: Int
    ): PendingIntent? {
        val intent = Intent(context, LessonNotificationReceiver::class.java)
            .putExtra("title", title)
            .putExtra("text", text)
            .putExtra("notification_id", notificationId)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            modeFlag or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun effectiveLessons(context: Context, date: LocalDate): List<AlertLesson> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val hidden = runCatching {
            val array = JSONArray(prefs.getString("hidden_official", "[]") ?: "[]")
            (0 until array.length()).map { array.getString(it) }.toSet()
        }.getOrDefault(emptySet())

        val official = ScheduleRepository.lessonsFor(date).mapNotNull { lesson ->
            val key = officialKey(date, lesson)
            if (key in hidden) null else AlertLesson(lesson.pair, lesson.start, lesson.end, lesson.subject, lesson.room, lesson.group, key)
        }

        val custom = runCatching {
            val array = JSONArray(prefs.getString("custom_lessons", "[]") ?: "[]")
            (0 until array.length()).mapNotNull { i ->
                val o = array.getJSONObject(i)
                if (o.optString("date") != date.toString()) return@mapNotNull null
                AlertLesson(
                    pair = o.optInt("pair", 1),
                    start = o.optString("start", "08:30"),
                    end = o.optString("end", "09:50"),
                    subject = o.optString("subject"),
                    room = o.optString("room"),
                    group = o.optString("group"),
                    key = "custom:${o.optString("id", "$i")}" 
                )
            }
        }.getOrDefault(emptyList())

        return (official + custom).sortedWith(compareBy<AlertLesson> { it.start }.thenBy { it.pair })
    }

    private fun officialKey(date: LocalDate, lesson: Lesson): String = listOf(
        date.toString(), lesson.pair, lesson.start, lesson.end, lesson.subject, lesson.room, lesson.group
    ).joinToString("|")

    private fun morningCode(date: LocalDate): Int = "morning:$date".hashCode()
    private fun lessonCode(date: LocalDate, key: String): Int = "lesson:$date:$key".hashCode()

    private fun lessonWord(count: Int): String = when {
        count % 10 == 1 && count % 100 != 11 -> "занятие"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "занятия"
        else -> "занятий"
    }
}
