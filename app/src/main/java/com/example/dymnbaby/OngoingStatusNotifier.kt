package com.example.dymnbaby

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import java.time.LocalDateTime
import java.time.ZoneId

enum class OngoingStatusType(
    val storedValue: String,
    val title: String,
    val pageIndex: Int,
) {
    Sleep("sleep", "Сон", 0),
    Activity("activity", "Активність", 2),
}

object OngoingStatusNotifier {
    private const val PrefsName = "dymn_baby_tracker"
    private const val TypeKey = "ongoing_status_type"
    private const val NameKey = "ongoing_status_name"
    private const val StartKey = "ongoing_status_start"
    private const val NotificationId = 7420
    private const val RequestCode = 7420
    private const val ChannelId = "ongoing_baby_status"

    fun sync(
        context: Context,
        childName: String,
        type: OngoingStatusType?,
        start: LocalDateTime?,
    ) {
        if (type == null || start == null) {
            cancel(context)
            return
        }

        val startMillis = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE).edit {
            putString(TypeKey, type.storedValue)
            putString(NameKey, childName)
            putLong(StartKey, startMillis)
        }
        show(context, childName, type, startMillis)
    }

    fun restore(context: Context) {
        val prefs = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        val type = OngoingStatusType.entries.firstOrNull {
            it.storedValue == prefs.getString(TypeKey, null)
        } ?: return
        val startMillis = prefs.getLong(StartKey, -1L).takeIf { it > 0L } ?: return
        val childName = prefs.getString(NameKey, null).orEmpty().ifBlank { dymnText(context, "Малюк", "Baby") }
        show(context, childName, type, startMillis)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NotificationId)
        context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE).edit {
            remove(TypeKey)
            remove(NameKey)
            remove(StartKey)
        }
    }

    private fun show(
        context: Context,
        childName: String,
        type: OngoingStatusType,
        startMillis: Long,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannel(context)
        val language = loadDymnLanguage(context)
        val chronometerBase = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startMillis)
        val compactView = statusRemoteView(
            context = context,
            layoutId = R.layout.ongoing_status_notification,
            childName = childName,
            type = type,
            chronometerBase = chronometerBase,
        )
        val expandedView = statusRemoteView(
            context = context,
            layoutId = R.layout.ongoing_status_notification_expanded,
            childName = childName,
            type = type,
            chronometerBase = chronometerBase,
        )
        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("$childName • ${type.title(language)}")
            .setContentText(dymnText(language, "Триває", "Active"))
            .setShowWhen(false)
            .setUsesChronometer(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPageIntent(context, type))
            .setCustomContentView(compactView)
            .setCustomBigContentView(expandedView)
            .build()
        NotificationManagerCompat.from(context).notify(NotificationId, notification)
    }

    private fun statusRemoteView(
        context: Context,
        layoutId: Int,
        childName: String,
        type: OngoingStatusType,
        chronometerBase: Long,
    ): RemoteViews {
        return RemoteViews(context.packageName, layoutId).apply {
            setTextViewText(R.id.ongoing_status_name, childName)
            setTextViewText(R.id.ongoing_status_type, type.title(loadDymnLanguage(context)))
            setChronometer(R.id.ongoing_status_timer, chronometerBase, null, true)
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ChannelId,
            dymnText(context, "Поточний стан малюка", "Current baby status"),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = dymnText(context, "Постійна інформація про поточний сон або активність", "Persistent information about current sleep or activity")
            setSound(null, null)
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun openPageIntent(context: Context, type: OngoingStatusType): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("dymnbaby://ongoing-status/${type.storedValue}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("widget_page_index", type.pageIndex)
        }
        return PendingIntent.getActivity(
            context,
            RequestCode + type.pageIndex,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

private fun OngoingStatusType.title(language: DymnLanguage): String {
    return when (this) {
        OngoingStatusType.Sleep -> dymnText(language, "Сон", "Sleep")
        OngoingStatusType.Activity -> dymnText(language, "Активність", "Activity")
    }
}