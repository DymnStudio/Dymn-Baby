package com.example.dymnbaby

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ActivityReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                ActivityReminderScheduler.restore(context)
                OngoingStatusNotifier.restore(context)
            }

            ActivityReminderScheduler.ACTION_REMIND -> {
                ActivityReminderScheduler.showReminder(context)
                ActivityReminderScheduler.scheduleNext(context)
            }
        }
    }
}

object ActivityReminderScheduler {
    const val ACTION_REMIND = "com.example.dymnbaby.ACTIVITY_REMINDER"

    private const val PrefsName = "dymn_baby_tracker"
    private const val ActiveProfileKey = "activity_reminder_profile"
    private const val ActiveNameKey = "activity_reminder_name"
    private const val ActiveStartKey = "activity_reminder_start"
    private const val ActiveLimitKey = "activity_reminder_limit"
    private const val ActiveTargetKey = "activity_reminder_target"
    private const val ActiveTypeKey = "activity_reminder_type"
    private const val RequestCode = 7412
    private const val NotificationId = 7412
    private const val ChannelId = "activity_reminders"

    fun sync(
        context: Context,
        profileId: String,
        childName: String,
        activeStart: LocalDateTime?,
        activeType: OngoingStatusType?,
        limitMinutes: Int,
    ) {
        if (activeStart == null || activeType == null) {
            cancel(context)
            return
        }

        val startMillis = activeStart
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val prefs = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        val unchanged = prefs.getString(ActiveProfileKey, null) == profileId &&
            prefs.getLong(ActiveStartKey, -1L) == startMillis &&
            prefs.getString(ActiveTypeKey, null) == activeType.storedValue &&
            prefs.getInt(ActiveLimitKey, -1) == limitMinutes

        prefs.edit {
            putString(ActiveProfileKey, profileId)
            putString(ActiveNameKey, childName)
            putLong(ActiveStartKey, startMillis)
            putString(ActiveTypeKey, activeType.storedValue)
            putInt(ActiveLimitKey, limitMinutes)
        }
        if (!unchanged) scheduleNext(context)
    }

    fun restore(context: Context) {
        if (storedStartMillis(context) > 0L) scheduleNext(context)
    }

    fun cancel(context: Context) {
        alarmManager(context).cancel(reminderIntent(context))
        context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE).edit {
            remove(ActiveProfileKey)
            remove(ActiveNameKey)
            remove(ActiveStartKey)
            remove(ActiveLimitKey)
            remove(ActiveTargetKey)
            remove(ActiveTypeKey)
        }
    }

    fun scheduleNext(context: Context) {
        val startMillis = storedStartMillis(context)
        if (startMillis <= 0L) return

        val elapsedMinutes = Duration.between(
            Instant.ofEpochMilli(startMillis),
            Instant.now(),
        ).toMinutes().coerceAtLeast(0)
        val nextTarget = ((elapsedMinutes / 60) + 1) * 60
        val triggerAt = startMillis + nextTarget * 60_000L

        context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE).edit {
            putLong(ActiveTargetKey, nextTarget)
        }

        val manager = alarmManager(context)
        val pendingIntent = reminderIntent(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            manager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun showReminder(context: Context) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val prefs = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        val startMillis = storedStartMillis(context)
        if (startMillis <= 0L) return

        val elapsedMinutes = Duration.between(
            Instant.ofEpochMilli(startMillis),
            Instant.now(),
        ).toMinutes().coerceAtLeast(0)
        val target = prefs.getLong(ActiveTargetKey, -1L)
        val limit = prefs.getInt(ActiveLimitKey, 90).toLong()
        val language = loadDymnLanguage(context)
        val childName = prefs.getString(ActiveNameKey, null).orEmpty().ifBlank { dymnText(language, "Малюк", "Baby") }
        val statusType = storedStatusType(prefs) ?: OngoingStatusType.Activity
        val duration = formatMinutes(elapsedMinutes, language)
        val reachedLimit = statusType == OngoingStatusType.Activity && target == limit

        createChannel(context)
        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("$childName • ${statusType.title(language)} $duration")
            .setContentText(
                if (reachedLimit) dymnText(language, "Досягнуто встановленого ліміту активності", "Activity limit reached")
                else if (statusType == OngoingStatusType.Sleep) dymnText(language, "Сон триває", "Sleep is still active")
                else dymnText(language, "Період активності ще триває", "Activity period is still active"),
            )
            .setContentIntent(openStatusIntent(context, statusType))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NotificationId, notification)
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ChannelId,
            dymnText(context, "Нагадування про активність", "Activity reminders"),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = dymnText(context, "Сповіщення про тривалість періоду активності малюка", "Notifications about the baby activity period duration")
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun openStatusIntent(context: Context, statusType: OngoingStatusType): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("dymnbaby://activity-reminder/open/${statusType.storedValue}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("widget_page_index", statusType.pageIndex)
        }
        return PendingIntent.getActivity(
            context,
            RequestCode + statusType.pageIndex,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun reminderIntent(context: Context): PendingIntent {
        val intent = Intent(context, ActivityReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            data = Uri.parse("dymnbaby://activity-reminder/alarm")
        }
        return PendingIntent.getBroadcast(
            context,
            RequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun alarmManager(context: Context): AlarmManager {
        return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private fun storedStartMillis(context: Context): Long {
        return context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
            .getLong(ActiveStartKey, -1L)
    }

    private fun storedStatusType(prefs: SharedPreferences): OngoingStatusType? {
        return OngoingStatusType.entries.firstOrNull {
            it.storedValue == prefs.getString(ActiveTypeKey, null)
        }
    }

    private fun formatMinutes(totalMinutes: Long, language: DymnLanguage): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val hour = dymnText(language, "год", "h")
        val minute = dymnText(language, "хв", "m")
        return when {
            hours > 0 && minutes > 0 -> "$hours $hour $minutes $minute"
            hours > 0 -> "$hours $hour"
            else -> "$minutes $minute"
        }
    }
}

private fun OngoingStatusType.title(language: DymnLanguage): String {
    return when (this) {
        OngoingStatusType.Sleep -> dymnText(language, "Сон", "Sleep")
        OngoingStatusType.Activity -> dymnText(language, "Активність", "Activity")
    }
}