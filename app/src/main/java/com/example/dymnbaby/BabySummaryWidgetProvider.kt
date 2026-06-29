package com.example.dymnbaby

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class BabySummaryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleWidgetRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        when (intent.action) {
            ACTION_REFRESH -> {
                updateAllWidgets(context)
                scheduleWidgetRefresh(context)
                return
            }
            ACTION_PREV -> {
                changeWidgetPage(context, appWidgetId, -1)
                updateWidgetsAfterAction(context, appWidgetId)
                return
            }
            ACTION_NEXT -> {
                changeWidgetPage(context, appWidgetId, 1)
                updateWidgetsAfterAction(context, appWidgetId)
                return
            }
        }

        super.onReceive(context, intent)
        scheduleWidgetRefresh(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleWidgetRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelWidgetRefresh(context)
    }

    companion object {
        fun updateAll(context: Context) {
            updateAllWidgets(context)
            scheduleWidgetRefresh(context)
        }
    }
}

private fun updateAllWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, BabySummaryWidgetProvider::class.java))
    ids.forEach { id -> updateWidget(context, manager, id) }
}

private fun updateWidgetsAfterAction(context: Context, appWidgetId: Int) {
    val manager = AppWidgetManager.getInstance(context)
    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        updateWidget(context, manager, appWidgetId)
    } else {
        updateAllWidgets(context)
    }
    scheduleWidgetRefresh(context)
}

private fun updateWidget(
    context: Context,
    manager: AppWidgetManager,
    appWidgetId: Int,
) {
    val profile = loadWidgetProfile(context)
    val entries = profile?.let { loadWidgetEntries(context, it.id) }.orEmpty()
    val pageIndex = currentWidgetPage(context, appWidgetId)
    val page = WidgetPage.entries[pageIndex]
    val now = LocalDateTime.now()
    val today = LocalDate.now()
    val todayEntries = entries.filter { it.occursOn(today) }
    val language = loadDymnLanguage(context)

    val sleepMinutes = todayEntries.filter { it.type == TYPE_SLEEP }.sumOf { it.durationMinutesOn(today, now) }
    val activeMinutes = todayEntries.filter { it.type == TYPE_ACTIVE }.sumOf { it.durationMinutesOn(today, now) }
    val walkMinutes = todayEntries.filter { it.type == TYPE_WALK }.sumOf { it.durationMinutesOn(today, now) }
    val feedingCount = todayEntries.count { it.type == TYPE_FEEDING }
    val toiletCount = todayEntries.count { it.type == TYPE_TOILET }
    
    val sleepEntries = entries.filter { it.type == TYPE_SLEEP }
    val activityEntries = entries.filter { it.type == TYPE_ACTIVE || it.type == TYPE_WALK }
    val activeSleep = sleepEntries.firstOrNull { it.end == null }
    val activeActivity = activityEntries.firstOrNull { it.end == null }

    val content = when (page) {
        WidgetPage.Sleep -> {
            val currentOrLastSleepMinutes = activeSleep?.durationUntil(now)?.toMinutes() 
                ?: sleepEntries.firstOrNull { it.end != null }?.durationUntil(now)?.toMinutes() ?: 0
            val parts = formatDurationParts(currentOrLastSleepMinutes.coerceAtLeast(0), language)
            WidgetContent(
                title = dymnText(language, "Сон", "Sleep"),
                left = parts.first,
                right = parts.second,
                recentTitle = dymnText(language, "Загальна", "Total"),
                recent = listOf(formatLongMinutes(sleepMinutes, language)),
            )
        }
        WidgetPage.Feeding -> WidgetContent(
            title = dymnText(language, "Годування", "Feeding"),
            left = feedingCount.toString(),
            right = "",
            recent = entries.filter { it.type == TYPE_FEEDING }.recentLabels(language, "Їжа", "Food").take(1),
            showRight = false,
        )
        WidgetPage.Activity -> {
            val currentOrLastActivityMinutes = activeActivity?.durationUntil(now)?.toMinutes()
                ?: activityEntries.firstOrNull { it.end != null }?.durationUntil(now)?.toMinutes() ?: 0
            val parts = formatDurationParts(currentOrLastActivityMinutes.coerceAtLeast(0), language)
            WidgetContent(
                title = dymnText(language, "Активність", "Activity"),
                left = parts.first,
                right = parts.second,
                recentTitle = dymnText(language, "Загальна", "Total"),
                recent = listOf(formatLongMinutes(activeMinutes + walkMinutes, language)),
            )
        }
        WidgetPage.Toilet -> WidgetContent(
            title = dymnText(language, "Туалет", "Toilet"),
            left = toiletCount.toString(),
            right = "",
            recent = entries.filter { it.type == TYPE_TOILET }.recentLabels(language, "Подія", "Event").take(1),
            showRight = false,
        )
    }

    val views = RemoteViews(context.packageName, R.layout.widget_baby_summary)
    views.setTextViewText(R.id.widget_name, profile?.name?.ifBlank { dymnText(language, "Малюк", "Baby") } ?: "Dymn Baby")
    views.setTextViewText(R.id.widget_page_title, content.title)
    views.setTextViewText(R.id.widget_stat_left, content.left)
    views.setTextViewText(R.id.widget_stat_right, content.right)
    views.setViewVisibility(R.id.widget_stat_right, if (content.showRight) View.VISIBLE else View.GONE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        views.setViewLayoutMargin(
            R.id.widget_stat_left,
            RemoteViews.MARGIN_END,
            if (content.showRight) 5f else 0f,
            android.util.TypedValue.COMPLEX_UNIT_DIP,
        )
    }
    views.setTextViewText(R.id.widget_recent_title, content.recentTitle)
    views.setTextViewText(R.id.widget_recent_first, content.recent.getOrNull(0) ?: dymnText(language, "Записів ще немає", "No records yet"))
    val secondRecent = content.recent.getOrNull(1).orEmpty()
    views.setTextViewText(R.id.widget_recent_second, secondRecent)
    views.setViewVisibility(R.id.widget_recent_second, if (secondRecent.isBlank()) View.GONE else View.VISIBLE)

    val action = page.widgetAction()
    views.setOnClickPendingIntent(R.id.widget_prev, widgetIntent(context, appWidgetId, ACTION_PREV))
    views.setOnClickPendingIntent(R.id.widget_next, widgetIntent(context, appWidgetId, ACTION_NEXT))
    views.setOnClickPendingIntent(R.id.widget_open_app, openAppIntent(context, appWidgetId))

    manager.updateAppWidget(appWidgetId, views)
}

private enum class WidgetPage {
    Sleep,
    Feeding,
    Activity,
    Toilet,
}

private fun WidgetPage.widgetAction(): String {
    return when (this) {
        WidgetPage.Sleep -> ACTION_SLEEP
        WidgetPage.Feeding -> ACTION_FEEDING
        WidgetPage.Activity -> ACTION_ACTIVITY
        WidgetPage.Toilet -> ACTION_TOILET
    }
}


private data class WidgetContent(
    val title: String,
    val left: String,
    val right: String,
    val recent: List<String>,
    val recentTitle: String = dymnText(DymnLanguage.Ukrainian, "Останні", "Recent"),
    val showRight: Boolean = true,
)

private fun currentWidgetPage(context: Context, appWidgetId: Int): Int {
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return 0
    return context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
        .getInt("${WIDGET_PAGE_INDEX}_$appWidgetId", 0)
        .coerceIn(0, WidgetPage.entries.lastIndex)
}

private fun changeWidgetPage(context: Context, appWidgetId: Int, delta: Int) {
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
    val size = WidgetPage.entries.size
    val current = currentWidgetPage(context, appWidgetId)
    val next = ((current + delta) % size + size) % size
    context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE).edit()
        .putInt("${WIDGET_PAGE_INDEX}_$appWidgetId", next)
        .apply()
}


private fun handleAction(context: Context, action: String) {
    val profile = loadWidgetProfile(context) ?: return
    val entries = loadWidgetEntries(context, profile.id)
    val now = LocalDateTime.now()
    val nextEntries = when (action) {
        ACTION_SLEEP -> toggleSleep(entries, now)
        ACTION_FEEDING -> entries + WidgetEntry(newWidgetId(now), TYPE_FEEDING, now, now)
        ACTION_ACTIVITY -> toggleWalk(entries, now)
        ACTION_TOILET -> entries + WidgetEntry(newWidgetId(now), TYPE_TOILET, now, now)
        else -> entries
    }
    saveWidgetEntries(context, profile.id, nextEntries)
    BabySummaryWidgetProvider.updateAll(context)
}

private data class WidgetProfile(
    val id: String,
    val name: String,
) {
    val displayName: String
        get() = name.ifBlank { "Малюк" }
}

private data class WidgetEntry(
    val id: String,
    val type: String,
    val start: LocalDateTime,
    val end: LocalDateTime?,
    val note: String = "",
    val isDone: Boolean = false,
) {
    fun durationUntil(now: LocalDateTime): Duration {
        return Duration.between(start, end ?: now)
    }

    fun occursOn(date: LocalDate): Boolean {
        if (type == TYPE_TOILET || type == TYPE_FEEDING) return start.toLocalDate() == date
        val now = LocalDateTime.now()
        val startOfDay = date.atStartOfDay()
        val startOfNextDay = date.plusDays(1).atStartOfDay()
        val effectiveStart = if (start.isBefore(startOfDay)) startOfDay else start
        val effectiveEnd = (end ?: now).let { if (it.isAfter(startOfNextDay)) startOfNextDay else it }
        return !effectiveStart.isAfter(effectiveEnd)
    }

    fun durationMinutesOn(date: LocalDate, now: LocalDateTime): Long {
        val startOfDay = date.atStartOfDay()
        val startOfNextDay = date.plusDays(1).atStartOfDay()
        val effectiveStart = if (start.isBefore(startOfDay)) startOfDay else start
        val effectiveEnd = (end ?: now).let { if (it.isAfter(startOfNextDay)) startOfNextDay else it }
        if (effectiveStart.isAfter(effectiveEnd)) return 0
        return Duration.between(effectiveStart, effectiveEnd).toMinutes().coerceAtLeast(0)
    }
}

private fun toggleSleep(entries: List<WidgetEntry>, now: LocalDateTime): List<WidgetEntry> {
    val activeSleep = entries.firstOrNull { it.type == TYPE_SLEEP && it.end == null }
    return if (activeSleep == null) {
        val closedActivity = entries.map { entry ->
            if (entry.type == TYPE_ACTIVE && entry.end == null) entry.copy(end = now) else entry
        }
        closedActivity + WidgetEntry(newWidgetId(now), TYPE_SLEEP, now, null)
    } else {
        entries.map { entry ->
            if (entry.id == activeSleep.id) entry.copy(end = now) else entry
        } + WidgetEntry(newWidgetId(now), TYPE_ACTIVE, now, null)
    }
}

private fun toggleWalk(entries: List<WidgetEntry>, now: LocalDateTime): List<WidgetEntry> {
    val activeWalk = entries.firstOrNull { it.type == TYPE_WALK && it.end == null }
    return if (activeWalk == null) {
        entries + WidgetEntry(newWidgetId(now), TYPE_WALK, now, null)
    } else {
        entries.map { entry ->
            if (entry.id == activeWalk.id) entry.copy(end = now) else entry
        }
    }
}

private fun loadWidgetProfile(context: Context): WidgetProfile? {
    val preferences = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
    val ids = preferences.getString(WIDGET_PROFILES_LIST, "")
        .orEmpty()
        .lineSequence()
        .filter { it.isNotBlank() }
        .toList()
    val selectedId = preferences.getString(WIDGET_SELECTED_PROFILE, null)
        ?.takeIf { it in ids }
    val firstId = selectedId ?: ids.firstOrNull() ?: return null
    return WidgetProfile(
        id = firstId,
        name = preferences.getString("${WIDGET_PROFILE_NAME}_$firstId", "").orEmpty(),
    )
}

private fun loadWidgetEntries(context: Context, childId: String): List<WidgetEntry> {
    val raw = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
        .getString("${WIDGET_ENTRIES}_$childId", "")
        .orEmpty()

    return raw.lineSequence().mapNotNull { line ->
        val parts = line.split("|", limit = 6)
        val id = parts.getOrNull(0) ?: return@mapNotNull null
        val type = parts.getOrNull(1) ?: return@mapNotNull null
        val start = parts.getOrNull(2)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
            ?: return@mapNotNull null
        WidgetEntry(
            id = id,
            type = type,
            start = start,
            end = parts.getOrNull(3)
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
            note = parts.getOrNull(4)?.decodeStorageText().orEmpty(),
            isDone = parts.getOrNull(5) == "1",
        )
    }.sortedByDescending { it.start }.toList()
}

private fun saveWidgetEntries(context: Context, childId: String, entries: List<WidgetEntry>) {
    val encoded = entries
        .sortedByDescending { it.start }
        .joinToString(separator = "\n") { entry ->
            listOf(
                entry.id,
                entry.type,
                entry.start.toString(),
                entry.end?.toString().orEmpty(),
                entry.note.encodeStorageText(),
                if (entry.isDone) "1" else "0",
            ).joinToString("|")
        }
    context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE).edit()
        .putString("${WIDGET_ENTRIES}_$childId", encoded)
        .apply()
}

private fun List<WidgetEntry>.recentLabels(language: DymnLanguage, fallbackUk: String, fallbackEn: String): List<String> {
    return take(2).map { entry ->
        val label = when {
            entry.note.isNotBlank() -> entry.note
            entry.type == TYPE_WALK -> dymnText(language, "Прогулянка", "Walk")
            else -> dymnText(language, fallbackUk, fallbackEn)
        }
        "$label ${entry.displayTimeText(language)}"
    }
}

private fun WidgetEntry.displayTimeText(language: DymnLanguage): String {
    val startText = start.format(TIME_FORMATTER)
    if (type == TYPE_FEEDING || type == TYPE_TOILET) return startText
    val endText = end?.format(TIME_FORMATTER) ?: dymnText(language, "триває", "active")
    return "$startText - $endText"
}

private fun widgetIntent(context: Context, appWidgetId: Int, action: String): PendingIntent {
    val intent = Intent(context, BabySummaryWidgetProvider::class.java).apply {
        this.action = action
        data = Uri.parse("dymnbaby://widget/$appWidgetId/$action")
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    return PendingIntent.getBroadcast(
        context,
        appWidgetId * 31 + action.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}


private fun openAppIntent(context: Context, appWidgetId: Int): PendingIntent {
    val pageIndex = currentWidgetPage(context, appWidgetId)
    val intent = Intent(context, MainActivity::class.java).apply {
        data = Uri.parse("dymnbaby://widget/$appWidgetId/open/$pageIndex")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_WIDGET_PAGE_INDEX, pageIndex)
    }
    return PendingIntent.getActivity(
        context,
        appWidgetId * 31 + ACTION_OPEN_APP.hashCode(),
        intent,
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
private fun scheduleWidgetRefresh(context: Context) {
    val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, BabySummaryWidgetProvider::class.java).apply {
        action = ACTION_REFRESH
        data = Uri.parse("dymnbaby://widget/refresh")
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        WIDGET_REFRESH_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val triggerAt = SystemClock.elapsedRealtime() + WIDGET_REFRESH_INTERVAL_MS
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
    } else {
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
    }
}

private fun cancelWidgetRefresh(context: Context) {
    val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, BabySummaryWidgetProvider::class.java).apply {
        action = ACTION_REFRESH
        data = Uri.parse("dymnbaby://widget/refresh")
    }
    manager.cancel(
        PendingIntent.getBroadcast(
            context,
            WIDGET_REFRESH_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ),
    )
}

private fun newWidgetId(now: LocalDateTime): String {
    return DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS", WIDGET_LOCALE).format(now) + (0..999).random()
}

private fun formatDurationParts(minutes: Long, language: DymnLanguage): Pair<String, String> {
    val hour = dymnText(language, "год", "h")
    val minute = dymnText(language, "хв", "m")
    return Pair("${minutes / 60} $hour", "${minutes % 60} $minute")
}

private fun formatMinutes(minutes: Long, language: DymnLanguage = DymnLanguage.Ukrainian): String {
    val hours = minutes / 60
    val mins = minutes % 60
    val h = dymnText(language, "г", "h")
    val m = dymnText(language, "хв", "m")
    return when {
        hours > 0 && mins > 0 -> "${hours}$h ${mins}$m"
        hours > 0 -> "${hours}$h"
        else -> "${mins}$m"
    }
}

private fun formatLongMinutes(minutes: Long, language: DymnLanguage): String {
    val hours = minutes / 60
    val mins = minutes % 60
    val hour = dymnText(language, "год", "h")
    val minute = dymnText(language, "хв", "m")
    return when {
        hours > 0 && mins > 0 -> "${hours} $hour ${mins} $minute"
        hours > 0 -> "${hours} $hour"
        else -> "${mins} $minute"
    }
}

private fun String.encodeStorageText(): String {
    return replace("%", "%25")
        .replace("|", "%7C")
        .replace("\n", "%0A")
        .replace("\r", "")
}

private fun String.decodeStorageText(): String {
    return replace("%0A", "\n")
        .replace("%7C", "|")
        .replace("%25", "%")
}

private val WIDGET_LOCALE = Locale("uk", "UA")
private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", WIDGET_LOCALE)
private const val ACTION_SLEEP = "com.example.dymnbaby.widget.SLEEP"
private const val ACTION_FEEDING = "com.example.dymnbaby.widget.FEEDING"
private const val ACTION_ACTIVITY = "com.example.dymnbaby.widget.ACTIVITY"
private const val ACTION_TOILET = "com.example.dymnbaby.widget.TOILET"
private const val ACTION_PREV = "com.example.dymnbaby.widget.PREV"
private const val ACTION_NEXT = "com.example.dymnbaby.widget.NEXT"
private const val ACTION_REFRESH = "com.example.dymnbaby.widget.REFRESH"
private const val ACTION_OPEN_APP = "com.example.dymnbaby.widget.OPEN_APP"
private const val WIDGET_REFRESH_REQUEST_CODE = 7401
private const val WIDGET_REFRESH_INTERVAL_MS = 60_000L
private const val TYPE_SLEEP = "sleep"
private const val TYPE_ACTIVE = "active"
private const val TYPE_TOILET = "toilet"
private const val TYPE_FEEDING = "feeding"
private const val TYPE_WALK = "walk"
private const val WIDGET_PREFS = "dymn_baby_tracker"
private const val WIDGET_PAGE_INDEX = "widget_page_index"
private const val EXTRA_WIDGET_PAGE_INDEX = "widget_page_index"
private const val WIDGET_PROFILES_LIST = "profiles_list"
private const val WIDGET_SELECTED_PROFILE = "selected_profile_id"
private const val WIDGET_PROFILE_NAME = "profile_name"
private const val WIDGET_ENTRIES = "entries"













