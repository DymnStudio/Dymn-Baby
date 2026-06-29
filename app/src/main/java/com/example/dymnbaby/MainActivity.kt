package com.example.dymnbaby

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory

private data class WidgetLaunchRequest(
    val page: BabyPage,
    val nonce: Long = System.currentTimeMillis(),
)

class MainActivity : ComponentActivity() {
    private var widgetLaunchRequest by mutableStateOf<WidgetLaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetLaunchRequest = widgetLaunchRequestFromIntent(intent)
        enableEdgeToEdge()
        setContent {
            DymnBabyTheme {
                DymnBabyApp(widgetLaunchRequest = widgetLaunchRequest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        widgetLaunchRequest = widgetLaunchRequestFromIntent(intent)
    }
}

@Composable
private fun lt(uk: String, en: String): String = dymnText(LocalDymnLanguage.current, uk, en)

@Composable
private fun DymnBabyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = DymnBlue,
            secondary = DymnCyan,
            background = DymnBlack,
            surface = DymnGraphite,
            onBackground = DymnText,
            onSurface = DymnText,
            onSurfaceVariant = DymnMuted,
        ),
        content = content,
    )
}

@Composable
private fun DymnBabyApp(widgetLaunchRequest: WidgetLaunchRequest? = null) {
    val context = LocalContext.current
    var appLanguage by remember { mutableStateOf(loadDymnLanguage(context)) }
    var profiles by remember { mutableStateOf(loadBabyProfiles(context)) }
    var selectedProfileId by remember { mutableStateOf(loadSelectedProfileId(context)) }
    
    val selectedProfile = remember(selectedProfileId, profiles) {
        profiles.find { it.id == selectedProfileId }
    }

    var entries by remember(selectedProfileId) { 
        mutableStateOf(if (selectedProfileId != null) loadBabyEntries(context, selectedProfileId!!) else emptyList()) 
    }
    
    var noteTarget by remember { mutableStateOf<BabyEventType?>(null) }
    var isProfileEditorOpen by remember { mutableStateOf(false) }
    var isPhotoEditorOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var manualEntryType by remember { mutableStateOf<BabyEventType?>(null) }
    var entryToEdit by remember { mutableStateOf<BabyEntry?>(null) }
    var isMeasurementDialogOpen by remember { mutableStateOf(false) }
    var isBirthMeasurementEditorOpen by remember { mutableStateOf(false) }
    var isMilestoneDialogOpen by remember { mutableStateOf(false) }
    var isArchiveOpen by remember { mutableStateOf(false) }
    var isUpdateReminderOpen by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<BabyProfile?>(null) }
    var tileOpacity by remember { mutableStateOf(loadTileOpacity(context)) }
    var tileFillKey by remember { mutableStateOf(loadTileFillKey(context)) }
    var customTileFillColor by remember { mutableStateOf(loadCustomTileFillColor(context)) }
    var wallpaperUri by remember { mutableStateOf(loadWallpaperUri(context)) }
    var activityLimitMinutes by remember(selectedProfileId) {
        mutableStateOf(loadActivityLimitMinutes(context, selectedProfileId))
    }
    var notificationPermissionRevision by remember { mutableStateOf(0) }
    var canScheduleExactAlarms by remember {
        mutableStateOf(canScheduleExactWidgetAlarms(context))
    }

    LaunchedEffect(isSettingsOpen) {
        while (isSettingsOpen) {
            canScheduleExactAlarms = canScheduleExactWidgetAlarms(context)
            kotlinx.coroutines.delay(800)
        }
    }

    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            wallpaperUri = uri.toString()
            saveWallpaperUri(context, wallpaperUri)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) notificationPermissionRevision++
    }

    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    runtimeDymnLanguage = appLanguage
    val tileFillColor = tileFillColorFor(tileFillKey, customTileFillColor).copy(alpha = tileOpacity)

    LaunchedEffect(selectedProfile?.id, selectedProfile?.name, entries, activityLimitMinutes, notificationPermissionRevision) {
        val profile = selectedProfile
        if (profile == null) {
            ActivityReminderScheduler.cancel(context)
            OngoingStatusNotifier.cancel(context)
        } else {
            val activeActivity = entries.firstOrNull {
                it.type == BabyEventType.Active && it.end == null
            }
            val activeSleep = entries.firstOrNull {
                it.type == BabyEventType.Sleep && it.end == null
            }
            ActivityReminderScheduler.sync(
                context = context,
                profileId = profile.id,
                childName = profile.displayName,
                activeStart = activeSleep?.start ?: activeActivity?.start,
                activeType = when {
                    activeSleep != null -> OngoingStatusType.Sleep
                    activeActivity != null -> OngoingStatusType.Activity
                    else -> null
                },
                limitMinutes = activityLimitMinutes,
            )
            OngoingStatusNotifier.sync(
                context = context,
                childName = profile.displayName,
                type = when {
                    activeSleep != null -> OngoingStatusType.Sleep
                    activeActivity != null -> OngoingStatusType.Activity
                    else -> null
                },
                start = activeSleep?.start ?: activeActivity?.start,
            )
        }
    }

    LaunchedEffect(selectedProfileId, selectedProfile?.birthDate) {
        saveSelectedProfileId(context, selectedProfileId)
        BabySummaryWidgetProvider.updateAll(context)
        
        // Check for monthly update reminder
        selectedProfile?.let { profile ->
            val birthDate = parseBirthDateTime(profile.birthDate)?.toLocalDate()
            val today = LocalDate.now()
            
            if (birthDate != null && !birthDate.isEqual(today)) {
                // Визначаємо, чи сьогодні "той самий день" або останній день місяця, якщо день народження пізніше
                val lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth()).dayOfMonth
                val isBirthdayDay = today.dayOfMonth == birthDate.dayOfMonth || 
                                   (birthDate.dayOfMonth > lastDayOfMonth && today.dayOfMonth == lastDayOfMonth)

                if (isBirthdayDay) {
                    val lastReminderKey = "last_reminder_${profile.id}"
                    val lastReminder = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
                        .getString(lastReminderKey, "")
                    if (lastReminder != today.toString()) {
                        isUpdateReminderOpen = true
                    }
                }
            }
        }
    }

    fun updateProfile(nextProfile: BabyProfile) {
        val nextProfiles = if (profiles.any { it.id == nextProfile.id }) {
            profiles.map { if (it.id == nextProfile.id) nextProfile else it }
        } else {
            profiles + nextProfile
        }
        profiles = nextProfiles
        saveBabyProfiles(context, nextProfiles)
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null && selectedProfile != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            updateProfile(
                selectedProfile.copy(
                    photoUri = uri.toString(),
                    photoScale = 1f,
                    photoRotation = 0f,
                    photoOffsetX = 0f,
                    photoOffsetY = 0f,
                ),
            )
            isPhotoEditorOpen = true
        }
    }

    fun persistEntries(nextEntries: List<BabyEntry>) {
        if (selectedProfileId == null) return
        entries = normalizeActivityIntervals(nextEntries).sortedByDescending { it.start }
        saveBabyEntries(context, entries, selectedProfileId!!)
    }

    fun addEntry(type: BabyEventType, durationMinutes: Long? = null, note: String = "") {
        val now = LocalDateTime.now()
        val next = BabyEntry(
            id = now.format(IdFormatter) + (0..999).random(),
            type = type,
            start = durationMinutes?.let { now.minusMinutes(it) } ?: now,
            end = durationMinutes?.let { now },
            note = note.trim(),
        )
        persistEntries(entries + next)
    }

    fun toggleQuestion(id: String) {
        persistEntries(entries.map { 
            if (it.id == id) it.copy(isDone = !it.isDone) else it 
        })
    }

    fun updateQuestionText(id: String, text: String) {
        persistEntries(entries.map { 
            if (it.id == id) it.copy(note = text) else it 
        })
    }

    fun startSleep() {
        val now = LocalDateTime.now()
        if (entries.any { it.type == BabyEventType.Sleep && it.end == null }) {
            Toast.makeText(context, dymnText(appLanguage, "Сон уже триває", "Sleep is already active"), Toast.LENGTH_SHORT).show()
            return
        }

        val closedActivity = entries.map { entry ->
            if (entry.type == BabyEventType.Active && entry.end == null) {
                entry.copy(end = now)
            } else {
                entry
            }
        }
        val sleep = BabyEntry(
            id = now.format(IdFormatter),
            type = BabyEventType.Sleep,
            start = now,
            end = null,
        )
        persistEntries(closedActivity + sleep)
    }

    fun finishSleepAndStartActivity() {
        val now = LocalDateTime.now()
        val activeSleep = entries.firstOrNull { it.type == BabyEventType.Sleep && it.end == null }
        if (activeSleep == null) {
            Toast.makeText(context, dymnText(appLanguage, "Немає активного сну", "No active sleep"), Toast.LENGTH_SHORT).show()
            return
        }
        persistEntries(
            entries.map { entry -> if (entry.id == activeSleep.id) entry.copy(end = now) else entry } +
                BabyEntry(id = now.format(IdFormatter), type = BabyEventType.Active, start = now, end = null)
        )
    }

    CompositionLocalProvider(LocalDymnLanguage provides appLanguage) {
        Surface(color = Color.Transparent, modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DymnBlack),
            ) {
                WallpaperBackground(wallpaperUri)
                if (selectedProfile == null) {
                    ChildSelectionPage(
                        profiles = profiles,
                        tileColor = tileFillColor,
                        onSelect = { selectedProfileId = it.id },
                        onDeleteRequest = { profileToDelete = it },
                        onAdd = {
                            val newId = "child_" + System.currentTimeMillis()
                            val newProfile = BabyProfile(id = newId)
                            updateProfile(newProfile)
                            selectedProfileId = newId
                            isProfileEditorOpen = true
                        },
                    )
                } else {
                    BabyPager(
                        profile = selectedProfile,
                        entries = entries,
                        tileColor = tileFillColor,
                        widgetLaunchRequest = widgetLaunchRequest,
                        onEditProfile = { isProfileEditorOpen = true },
                        onEditPhoto = { isPhotoEditorOpen = true },
                        onOpenSettings = { isSettingsOpen = true },
                        onStartSleep = ::startSleep,
                        onFinishSleep = ::finishSleepAndStartActivity,
                        onToilet = { addEntry(BabyEventType.Toilet, durationMinutes = 0) },
                        onFeeding = { addEntry(BabyEventType.Feeding, durationMinutes = 0) },
                        onNote = { noteTarget = BabyEventType.Note },
                        onAddQuestion = { note -> addEntry(BabyEventType.Question, note = note) },
                        onToggleQuestion = ::toggleQuestion,
                        onUpdateQuestion = ::updateQuestionText,
                        onManualEntry = { manualEntryType = it },
                        activityLimitMinutes = activityLimitMinutes,
                        onActivityLimitChange = { minutes ->
                            activityLimitMinutes = minutes
                            saveActivityLimitMinutes(context, selectedProfile.id, minutes)
                        },
                        onAddMeasurement = { isMeasurementDialogOpen = true },
                        onEditBirthMeasurements = { isBirthMeasurementEditorOpen = true },
                        onClearBirthMeasurements = { updateProfile(selectedProfile.copy(weight = "", height = "")) },
                        onAddMilestone = { isMilestoneDialogOpen = true },
                        onStartWalk = {
                            val now = LocalDateTime.now()
                            if (entries.any { it.type == BabyEventType.Walk && it.end == null }) {
                                Toast.makeText(context, dymnText(appLanguage, "Прогулянка вже триває", "Walk is already active"), Toast.LENGTH_SHORT).show()
                            } else {
                                persistEntries(entries + BabyEntry(id = now.format(IdFormatter), type = BabyEventType.Walk, start = now, end = null))
                            }
                        },
                        onFinishWalk = {
                            val now = LocalDateTime.now()
                            val activeWalk = entries.firstOrNull { it.type == BabyEventType.Walk && it.end == null }
                            if (activeWalk != null) persistEntries(entries.map { if (it.id == activeWalk.id) it.copy(end = now) else it })
                        },
                        onDeleteEntry = { entry ->
                            persistEntries(entries.filterNot { it.id == entry.id })
                            Toast.makeText(context, dymnText(appLanguage, "Подію видалено", "Event deleted"), Toast.LENGTH_SHORT).show()
                        },
                        onEditEntry = { entryToEdit = it },
                        onUpdateEntries = { persistEntries(it) },
                    )
                }
            }
        }

        if (isSettingsOpen) {
            AppSettingsDialog(
                tileColor = tileFillColor,
                currentOpacity = tileOpacity,
                currentFillKey = tileFillKey,
                customFillColor = customTileFillColor,
                onOpacityChange = { tileOpacity = it; saveTileOpacity(context, it) },
                onFillChange = { tileFillKey = it; saveTileFillKey(context, it) },
                onCustomFillChange = {
                    customTileFillColor = it
                    tileFillKey = CustomTileFillKeyValue
                    saveCustomTileFillColor(context, it)
                    saveTileFillKey(context, CustomTileFillKeyValue)
                },
                onPickWallpaper = { wallpaperPicker.launch(arrayOf("image/*")) },
                onSelectBuiltIn = { name -> wallpaperUri = name; saveWallpaperUri(context, name) },
                canScheduleExactAlarms = canScheduleExactAlarms,
                currentLanguage = appLanguage,
                onLanguageChange = { language ->
                    appLanguage = language
                    runtimeDymnLanguage = language
                    saveDymnLanguage(context, language)
                    BabySummaryWidgetProvider.updateAll(context)
                    OngoingStatusNotifier.restore(context)
                },
                onOpenExactAlarmSettings = { openExactAlarmPermissionSettings(context) },
                onSwitchChild = { selectedProfileId = null; isSettingsOpen = false },
                onOpenArchive = { isArchiveOpen = true; isSettingsOpen = false },
                onDismiss = { isSettingsOpen = false },
            )
        }

        if (isArchiveOpen) {
            ArchiveDialog(entries = entries, tileColor = tileFillColor, now = LocalDateTime.now(), onDismiss = { isArchiveOpen = false })
        }
        if (isProfileEditorOpen && selectedProfile != null) {
            ProfileDialog(
                profile = selectedProfile,
                tileColor = tileFillColor,
                onDismiss = { isProfileEditorOpen = false },
                onSave = { updateProfile(it); isProfileEditorOpen = false },
                onDelete = {
                    profiles = profiles.filterNot { it.id == selectedProfile.id }
                    saveBabyProfiles(context, profiles)
                    selectedProfileId = null
                    isProfileEditorOpen = false
                },
            )
        }
        if (isPhotoEditorOpen && selectedProfile != null) {
            PhotoDialog(
                profile = selectedProfile,
                tileColor = tileFillColor,
                onPickPhoto = { photoPicker.launch(arrayOf("image/*")) },
                onDismiss = { isPhotoEditorOpen = false },
                onSave = { updateProfile(it); isPhotoEditorOpen = false },
            )
        }
        noteTarget?.let { type ->
            NoteDialog(
                title = lt("Нотатка", "Note"),
                tileColor = tileFillColor,
                onDismiss = { noteTarget = null },
                onSave = { note -> addEntry(type = type, durationMinutes = 0, note = note); noteTarget = null },
            )
        }
        manualEntryType?.let { type ->
            ManualEntryDialog(
                type = type,
                tileColor = tileFillColor,
                onDismiss = { manualEntryType = null },
                onSave = { start, end ->
                    val now = LocalDateTime.now()
                    persistEntries(entries + BabyEntry(id = now.format(IdFormatter), type = type, start = start, end = end))
                    manualEntryType = null
                },
            )
        }
        entryToEdit?.let { entry ->
            EditEntryDialog(
                entry = entry,
                tileColor = tileFillColor,
                onDismiss = { entryToEdit = null },
                onSave = { updated -> persistEntries(entries.map { if (it.id == updated.id) updated else it }); entryToEdit = null },
            )
        }
        if (isMilestoneDialogOpen) {
            MilestoneDialog(
                tileColor = tileFillColor,
                onDismiss = { isMilestoneDialogOpen = false },
                onSave = { title, date ->
                    val now = LocalDateTime.now()
                    persistEntries(entries + BabyEntry(id = now.format(IdFormatter), type = BabyEventType.Milestone, start = date, end = date, note = title.ifBlank { dymnText(appLanguage, "Важлива подія", "Important event") }))
                    isMilestoneDialogOpen = false
                },
            )
        }
        if (isBirthMeasurementEditorOpen && selectedProfile != null) {
            BirthMeasurementDialog(
                profile = selectedProfile,
                tileColor = tileFillColor,
                onDismiss = { isBirthMeasurementEditorOpen = false },
                onSave = { weight, height -> updateProfile(selectedProfile.copy(weight = weight, height = height)); isBirthMeasurementEditorOpen = false },
            )
        }
        if (isMeasurementDialogOpen) {
            HistoricalMeasurementDialog(
                profile = selectedProfile ?: BabyProfile(),
                tileColor = tileFillColor,
                onDismiss = { isMeasurementDialogOpen = false },
                onSave = { date, weight, height ->
                    val now = LocalDateTime.now()
                    persistEntries(entries + BabyEntry(id = now.format(IdFormatter), type = BabyEventType.Measurement, start = date.atStartOfDay(), end = null, note = "Вага: $weight кг, Зріст: $height см"))
                    isMeasurementDialogOpen = false
                },
            )
        }
        if (isUpdateReminderOpen && selectedProfile != null) {
            ReminderMeasurementDialog(
                profile = selectedProfile,
                isYearlyBirthday = false,
                tileColor = tileFillColor,
                onDismiss = { isUpdateReminderOpen = false },
                onSave = { weight, height ->
                    val now = LocalDateTime.now()
                    persistEntries(entries + BabyEntry(id = now.format(IdFormatter), type = BabyEventType.Measurement, start = now, end = null, note = "Вага: $weight кг, Зріст: $height см"))
                    isUpdateReminderOpen = false
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BabyPager(
    profile: BabyProfile,
    entries: List<BabyEntry>,
    tileColor: Color,
    widgetLaunchRequest: WidgetLaunchRequest?,
    onEditProfile: () -> Unit,
    onEditPhoto: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartSleep: () -> Unit,
    onFinishSleep: () -> Unit,
    onToilet: () -> Unit,
    onFeeding: () -> Unit,
    onNote: () -> Unit,
    onAddQuestion: (String) -> Unit,
    onToggleQuestion: (String) -> Unit,
    onUpdateQuestion: (String, String) -> Unit,
    onManualEntry: (BabyEventType) -> Unit,
    activityLimitMinutes: Int,
    onActivityLimitChange: (Int) -> Unit,
    onAddMeasurement: () -> Unit,
    onEditBirthMeasurements: () -> Unit,
    onClearBirthMeasurements: () -> Unit,
    onAddMilestone: () -> Unit,
    onStartWalk: () -> Unit,
    onFinishWalk: () -> Unit,
    onDeleteEntry: (BabyEntry) -> Unit,
    onEditEntry: (BabyEntry) -> Unit,
    onUpdateEntries: (List<BabyEntry>) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { BabyPage.entries.size })
    val scope = rememberCoroutineScope()
    LaunchedEffect(widgetLaunchRequest?.nonce) { widgetLaunchRequest?.let { pagerState.scrollToPage(it.page.ordinal) } }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); kotlinx.coroutines.delay(10_000) } }
    Column(modifier = Modifier.fillMaxSize().padding(top = topPadding + 16.dp)) {
        PageSwitcher(BabyPage.entries[pagerState.currentPage], tileColor) { page -> scope.launch { pagerState.animateScrollToPage(page.ordinal) } }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
            val contentPadding = PaddingValues(top = 12.dp, bottom = bottomPadding + 24.dp, start = 20.dp, end = 20.dp)
            when (BabyPage.entries[pageIndex]) {
                BabyPage.Home -> HomePage(profile, entries, tileColor, contentPadding, now, onEditProfile, onEditPhoto, onOpenSettings, onDeleteEntry, onEditEntry)
                BabyPage.Sleep -> SleepPage(entries, tileColor, contentPadding, now, onStartSleep, onFinishSleep, { onManualEntry(BabyEventType.Sleep) }, onDeleteEntry, onEditEntry)
                BabyPage.Feeding -> FeedingPage(entries, tileColor, contentPadding, onFeeding, { onManualEntry(BabyEventType.Feeding) }, onDeleteEntry, onEditEntry)
                BabyPage.Activity -> ActivityPage(entries, tileColor, contentPadding, now, activityLimitMinutes, onActivityLimitChange, { onManualEntry(BabyEventType.Walk) }, onStartWalk, onFinishWalk, onDeleteEntry, onEditEntry)
                BabyPage.Toilet -> ToiletPage(entries, tileColor, contentPadding, onToilet, { onManualEntry(BabyEventType.Toilet) }, onDeleteEntry, onEditEntry)
                BabyPage.Questions -> QuestionsPage(entries, tileColor, contentPadding, onAddQuestion, onToggleQuestion, onUpdateQuestion, onDeleteEntry)
                BabyPage.Monitor -> MonitorPage(entries, profile, tileColor, contentPadding, onAddMeasurement, onEditBirthMeasurements, onClearBirthMeasurements, onUpdateEntries)
                BabyPage.Events -> EventsPage(entries, tileColor, contentPadding, onAddMilestone, onDeleteEntry, onEditEntry)
            }
        }
    }
}

@Composable
private fun PageSwitcher(currentPage: BabyPage, tileColor: Color, onPageSelected: (BabyPage) -> Unit) {
    val scrollState = rememberLazyListState()
    LaunchedEffect(currentPage) { scrollState.animateScrollToItem(currentPage.ordinal) }
    LazyRow(state = scrollState, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        items(BabyPage.entries) { page ->
            val selected = page == currentPage
            Box(modifier = Modifier.height(42.dp).widthIn(min = 90.dp).clip(RoundedCornerShape(21.dp)).background(if (selected) tileColor else tileColor.copy(alpha = tileColor.alpha * 0.42f)).border(0.6.dp, if (selected) DymnLightBorder else Color.White.copy(alpha = 0.18f), RoundedCornerShape(21.dp)).clickable { onPageSelected(page) }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                Text(page.localizedTitle(), color = DymnText, style = dymnText(MaterialTheme.typography.labelLarge), maxLines = 1)
            }
        }
    }
}

@Composable
private fun HomePage(profile: BabyProfile, entries: List<BabyEntry>, tileColor: Color, contentPadding: PaddingValues, now: LocalDateTime, onEditProfile: () -> Unit, onEditPhoto: () -> Unit, onOpenSettings: () -> Unit, onDeleteEntry: (BabyEntry) -> Unit, onEditEntry: (BabyEntry) -> Unit) {
    val todayEntries = entries.today()
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Header(profile, entries, onOpenSettings) }
        item { ProfileCard(profile, tileColor, now, onEditProfile, onEditPhoto) }
        item { SummaryWidget(todayEntries, tileColor, profile.displayName) }
        item { SectionTitle(lt("Сьогодні", "Today")) }
        if (todayEntries.isEmpty()) item { EmptyTimeline(tileColor) } else items(todayEntries, key = { it.id }) { entry -> TimelineItem(entry, tileColor, { onEditEntry(entry) }, { onDeleteEntry(entry) }) }
    }
}

@Composable
private fun SleepPage(entries: List<BabyEntry>, tileColor: Color, contentPadding: PaddingValues, now: LocalDateTime, onStartSleep: () -> Unit, onFinishSleep: () -> Unit, onManualEntry: () -> Unit, onDeleteEntry: (BabyEntry) -> Unit, onEditEntry: (BabyEntry) -> Unit) {
    val activeSleep = entries.firstOrNull { it.type == BabyEventType.Sleep && it.end == null }
    val sleepEntries = entries.filter { it.type == BabyEventType.Sleep }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader(lt("Сон", "Sleep"), if (activeSleep == null) lt("Періоди відпочинку", "Rest periods") else lt("Зараз триває сон", "Sleep is active"), onManualEntry) }
        item { FocusWidget(formatMinutes(sleepEntries.today().sumDurationMinutes()), lt("Сон за сьогодні", "Sleep today"), activeSleep?.let { lt("Триває", "Active for") + " ${formatDuration(it.durationUntil(now))}" } ?: lt("Активного сну немає", "No active sleep"), DymnBlue, tileColor) }
        item { ActionPill(title = if (activeSleep == null) lt("Почати сон", "Start sleep") else lt("Завершити сон", "End sleep"), icon = EventIcons.Sleep, color = DymnBlue, tileColor = tileColor, onClick = if (activeSleep == null) onStartSleep else onFinishSleep) }
        item { SectionTitle(lt("Сьогодні", "Today")) }
        val today = sleepEntries.today()
        if (today.isEmpty()) item { EmptyTimeline(tileColor) } else items(today, key = { it.id }) { entry -> TimelineItem(entry, tileColor, { onEditEntry(entry) }, { onDeleteEntry(entry) }) }
    }
}

@Composable
private fun ActivityPage(entries: List<BabyEntry>, tileColor: Color, contentPadding: PaddingValues, now: LocalDateTime, activityLimitMinutes: Int, onActivityLimitChange: (Int) -> Unit, onManualEntry: () -> Unit, onStartWalk: () -> Unit, onFinishWalk: () -> Unit, onDeleteEntry: (BabyEntry) -> Unit, onEditEntry: (BabyEntry) -> Unit) {
    var settingsOpen by remember { mutableStateOf(false) }
    val activeActivity = entries.firstOrNull { it.type == BabyEventType.Active && it.end == null }
    val activeWalk = entries.firstOrNull { it.type == BabyEventType.Walk && it.end == null }
    val activityEntries = entries.filter { it.type == BabyEventType.Active || it.type == BabyEventType.Walk }
    val walkEntries = entries.filter { it.type == BabyEventType.Walk }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader(lt("Активність", "Activity"), activeActivity?.let { lt("Малюк не спить", "Baby has been awake for") + " ${formatDuration(it.durationUntil(now))}" } ?: lt("Період неспання", "Awake period"), onManualEntry, { settingsOpen = true }) }
        item { FocusWidget(formatMinutes(entries.today().filter { it.type == BabyEventType.Active }.sumDurationMinutes()), lt("Активність за сьогодні", "Activity today"), lt("Без врахування прогулянок", "Walks excluded"), DymnGreen, tileColor) }
        item { FocusWidget(formatMinutes(walkEntries.today().sumDurationMinutes()), lt("Прогулянка за сьогодні", "Walk today"), activeWalk?.let { lt("Триває", "Active for") + " ${formatDuration(it.durationUntil(now))}" } ?: lt("Зараз не на вулиці", "Not outside now"), DymnCyan, tileColor) }
        item { ActionPill(title = if (activeWalk == null) lt("Почати", "Start") else lt("Завершити", "End"), icon = EventIcons.Walk, color = DymnCyan, tileColor = tileColor, onClick = if (activeWalk == null) onStartWalk else onFinishWalk) }
        item { SectionTitle(lt("Сьогодні", "Today")) }
        val today = activityEntries.today()
        if (today.isEmpty()) item { EmptyTimeline(tileColor) } else items(today, key = { it.id }) { entry -> TimelineItem(entry, tileColor, { onEditEntry(entry) }, { onDeleteEntry(entry) }) }
    }
    if (settingsOpen) ActivitySettingsDialog(activityLimitMinutes, tileColor, { settingsOpen = false }, { onActivityLimitChange(it); settingsOpen = false })
}

@Composable
private fun FeedingPage(entries: List<BabyEntry>, tileColor: Color, contentPadding: PaddingValues, onFeeding: () -> Unit, onManualEntry: () -> Unit, onDeleteEntry: (BabyEntry) -> Unit, onEditEntry: (BabyEntry) -> Unit) {
    val feedingEntries = entries.filter { it.type == BabyEventType.Feeding }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader(lt("Годування", "Feeding"), lt("Запис прийомів їжі", "Meal log"), onManualEntry) }
        item { FocusWidget(feedingEntries.today().size.toString(), lt("Годувань за сьогодні", "Feedings today"), lt("Одне натискання додає новий запис", "One tap adds a new record"), DymnCyan, tileColor) }
        item { ActionPill(title = lt("Додати", "Add"), icon = EventIcons.Feeding, color = DymnCyan, tileColor = tileColor, onClick = onFeeding) }
        item { SectionTitle(lt("Сьогодні", "Today")) }
        val today = feedingEntries.today()
        if (today.isEmpty()) item { EmptyTimeline(tileColor) } else items(today, key = { it.id }) { entry -> TimelineItem(entry, tileColor, { onEditEntry(entry) }, { onDeleteEntry(entry) }) }
    }
}

@Composable
private fun ToiletPage(entries: List<BabyEntry>, tileColor: Color, contentPadding: PaddingValues, onToilet: () -> Unit, onManualEntry: () -> Unit, onDeleteEntry: (BabyEntry) -> Unit, onEditEntry: (BabyEntry) -> Unit) {
    val toiletEntries = entries.filter { it.type == BabyEventType.Toilet }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader(lt("Туалет", "Toilet"), lt("Одне натискання - одна подія", "One tap adds one event"), onManualEntry) }
        item { FocusWidget(toiletEntries.today().size.toString(), lt("Подій за сьогодні", "Events today"), lt("Кожне натискання додає новий запис", "Each tap adds a new record"), DymnYellow, tileColor) }
        item { ActionPill(title = lt("Додати", "Add"), icon = EventIcons.Toilet, iconText = "WC", color = DymnYellow, tileColor = tileColor, onClick = onToilet) }
        item { SectionTitle(lt("Сьогодні", "Today")) }
        val today = toiletEntries.today()
        if (today.isEmpty()) item { EmptyTimeline(tileColor) } else items(today, key = { it.id }) { entry -> TimelineItem(entry, tileColor, { onEditEntry(entry) }, { onDeleteEntry(entry) }) }
    }
}

@Composable
private fun QuestionsPage(entries: List<BabyEntry>, tileColor: Color, contentPadding: PaddingValues, onAddQuestion: (String) -> Unit, onToggleQuestion: (String) -> Unit, onUpdateQuestion: (String, String) -> Unit, onDeleteEntry: (BabyEntry) -> Unit) {
    val questionEntries = entries.filter { it.type == BabyEventType.Question }.sortedBy { it.start }
    var newQuestionText by remember { mutableStateOf("") }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader(lt("Запитання", "Questions"), lt("Список для педіатра", "List for the pediatrician")) }
        item {
            LauncherWidgetCard(modifier = Modifier.fillMaxWidth(), tileColor = tileColor) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    questionEntries.forEach { entry -> QuestionRow(entry, { onToggleQuestion(entry.id) }, { onUpdateQuestion(entry.id, it) }, { onDeleteEntry(entry) }) }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = DymnCyan, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = newQuestionText,
                            onValueChange = { newQuestionText = it },
                            modifier = Modifier.weight(1f),
                            textStyle = dymnText(MaterialTheme.typography.bodyLarge).copy(color = DymnText),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(DymnText),
                            decorationBox = { innerTextField ->
                                if (newQuestionText.isEmpty()) Text(lt("Додати запитання...", "Add a question..."), color = DymnMuted.copy(alpha = 0.5f), style = dymnText(MaterialTheme.typography.bodyLarge))
                                innerTextField()
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { if (newQuestionText.isNotBlank()) { onAddQuestion(newQuestionText); newQuestionText = "" } }),
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuestionRow(
    entry: BabyEntry,
    onToggle: () -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .border(
                    width = 1.5.dp,
                    color = if (entry.isDone) DymnCyan else DymnMuted.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(11.dp)
                )
                .background(if (entry.isDone) DymnCyan.copy(alpha = 0.2f) else Color.Transparent)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (entry.isDone) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(DymnCyan)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        androidx.compose.foundation.text.BasicTextField(
            value = entry.note,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            textStyle = dymnText(MaterialTheme.typography.bodyLarge).copy(
                color = if (entry.isDone) DymnMuted else DymnText,
                textDecoration = if (entry.isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(DymnText)
        )

        if (entry.isDone) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = lt("Видалити", "Delete"),
                    tint = DymnRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private data class MonthStats(
    val monthIdx: Int,
    val label: String,
    val sleepHours: Double,
    val sleepDelta: Double = 0.0,
    val activeHours: Double,
    val activeDelta: Double = 0.0,
    val toiletCount: Double,
    val toiletDelta: Double = 0.0,
    val feedingCount: Double,
    val feedingDelta: Double = 0.0,
    val weight: String,
    val weightDelta: String = "",
    val height: String,
    val heightDelta: String = ""
)

@Composable
private fun MonitorPage(
    entries: List<BabyEntry>,
    profile: BabyProfile,
    tileColor: Color,
    contentPadding: PaddingValues,
    onAddMeasurement: () -> Unit,
    onEditBirthMeasurements: () -> Unit,
    onClearBirthMeasurements: () -> Unit,
    onDeleteEntries: (List<BabyEntry>) -> Unit,
) {
    val context = LocalContext.current
    val birthDate = parseBirthDateTime(profile.birthDate) ?: LocalDateTime.now()
    var monthIdxToDelete by remember { mutableStateOf<Int?>(null) }
    var monthIdxToEdit by remember { mutableStateOf<Int?>(null) }

    val monthSummaries = remember(entries, birthDate, profile.weight, profile.height) {
        val groups = entries.groupBy { entry ->
            monitorMonthIndexForEntry(birthDate.toLocalDate(), entry)
        }.filterKeys { it >= 0 }.toSortedMap()
        
        val monthIndexes = (groups.keys + 0).distinct().sorted()
        val stats = monthIndexes.map { monthIdx ->
            val monthEntries = groups[monthIdx].orEmpty()
            val trackedEntries = monthEntries.filter { it.type != BabyEventType.Measurement }
            val trackedDays = trackedEntries.groupBy { it.start.toLocalDate() }.values
            val sleepHours = trackedDays.averageOfOrZero { dayEntries ->
                val date = dayEntries.first().start.toLocalDate()
                dayEntries.filter { it.type == BabyEventType.Sleep }
                    .sumDurationMinutesOn(date, LocalDateTime.now()).toDouble() / 60.0
            }
            val activeHours = trackedDays.averageOfOrZero { dayEntries ->
                val date = dayEntries.first().start.toLocalDate()
                dayEntries.filter { it.type == BabyEventType.Active }
                    .sumDurationMinutesOn(date, LocalDateTime.now()).toDouble() / 60.0
            }
            val toilet = trackedDays.averageOfOrZero { dayEntries ->
                dayEntries.count { it.type == BabyEventType.Toilet }.toDouble()
            }
            val feeding = trackedDays.averageOfOrZero { dayEntries ->
                dayEntries.count { it.type == BabyEventType.Feeding }.toDouble()
            }
            
            val weightEntry = monthEntries.filter { it.type == BabyEventType.Measurement && it.note.contains("Вага:") }.maxWithOrNull(compareBy<BabyEntry> { it.start }.thenBy { it.id })
            val heightEntry = monthEntries.filter { it.type == BabyEventType.Measurement && it.note.contains("Зріст:") }.maxWithOrNull(compareBy<BabyEntry> { it.start }.thenBy { it.id })
            
            val wStr = if (monthIdx == 0) {
                profile.weight
            } else {
                weightEntry?.note?.substringAfter("Вага:")?.substringBefore("кг")?.trim().orEmpty()
            }
            val hStr = if (monthIdx == 0) {
                profile.height
            } else {
                heightEntry?.note?.substringAfter("Зріст:")?.substringBefore("см")?.trim().orEmpty()
            }
            
            MonthStats(
                monthIdx = monthIdx,
                label = monitorMonthLabel(monthIdx),
                sleepHours = sleepHours,
                activeHours = activeHours,
                toiletCount = toilet,
                feedingCount = feeding,
                weight = wStr,
                height = hStr
            )
        }
        
        stats.mapIndexed { index, current ->
            if (index == 0) current else {
                val prev = stats[index - 1]
                val wDelta = if (current.weight.isNotBlank() && prev.weight.isNotBlank()) {
                    val d = (current.weight.toDoubleOrNull() ?: 0.0) - (prev.weight.toDoubleOrNull() ?: 0.0)
                    if (d > 0) "+${String.format(UaLocale, "%.3f", d)}" else String.format(UaLocale, "%.3f", d)
                } else ""
                
                val hDelta = if (current.height.isNotBlank() && prev.height.isNotBlank()) {
                    val d = (current.height.toIntOrNull() ?: 0) - (prev.height.toIntOrNull() ?: 0)
                    if (d > 0) "+$d" else "$d"
                } else ""

                current.copy(
                    sleepDelta = current.sleepHours - prev.sleepHours,
                    activeDelta = current.activeHours - prev.activeHours,
                    toiletDelta = current.toiletCount - prev.toiletCount,
                    feedingDelta = current.feedingCount - prev.feedingCount,
                    weightDelta = wDelta,
                    heightDelta = hDelta
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PageHeader(
                title = lt("Монітор", "Monitor"),
                subtitle = lt("Динаміка розвитку", "Growth trends"),
                onAddClick = onAddMeasurement
            )
        }

        if (monthSummaries.isNotEmpty()) {
            item {
                DynamicsWidget(stats = monthSummaries, tileColor = tileColor)
            }
            item {
                SectionTitle(lt("Історія за місяцями", "Monthly history"))
            }
            items(monthSummaries.size) { index ->
                val stats = monthSummaries.reversed()[index]
                MonitorCard(
                    stats = stats, 
                    tileColor = tileColor, 
                    showDeltas = false,
                    onClick = {
                        if (stats.monthIdx == 0) onEditBirthMeasurements() else monthIdxToEdit = stats.monthIdx
                    },
                    onDelete = { monthIdxToDelete = stats.monthIdx }
                )
            }
        } else {
            item { EmptyTimeline(tileColor = tileColor) }
        }
    }

    monthIdxToEdit?.let { idx ->
        val targetMonth = monthSummaries.find { it.monthIdx == idx }
        if (targetMonth != null) {
            BirthMeasurementDialog(
                profile = profile.copy(weight = targetMonth.weight, height = targetMonth.height),
                tileColor = tileColor,
                title = targetMonth.label,
                isBirthMeasurement = false,
                onDismiss = { monthIdxToEdit = null },
                onSave = { weight, height ->
                    val existing = entries
                        .filter { it.type == BabyEventType.Measurement && monitorMonthIndexForEntry(birthDate.toLocalDate(), it) == idx }
                        .maxWithOrNull(compareBy<BabyEntry> { it.start }.thenBy { it.id })
                    val note = "Вага: $weight кг, Зріст: $height см"
                    val updatedEntries = if (existing != null) {
                        entries.map { entry -> if (entry.id == existing.id) entry.copy(note = note) else entry }
                    } else {
                        val measurementDate = birthDate.toLocalDate().plusMonths(idx.toLong()).atStartOfDay()
                        entries + BabyEntry(
                            id = LocalDateTime.now().format(IdFormatter) + (0..999).random(),
                            type = BabyEventType.Measurement,
                            start = measurementDate,
                            end = null,
                            note = note,
                        )
                    }
                    onDeleteEntries(updatedEntries)
                    monthIdxToEdit = null
                    Toast.makeText(context, "Вимірювання оновлено", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }
    monthIdxToDelete?.let { idx ->
        val targetMonth = monthSummaries.find { it.monthIdx == idx }
        AlertDialog(
            onDismissRequest = { monthIdxToDelete = null },
            containerColor = tileColor,
            shape = RoundedCornerShape(32.dp),
            title = { Text("Видалити дані?", color = DymnText) },
            text = { Text("Ви дійсно хочете видалити всі записи за ${targetMonth?.label ?: "цей місяць"}?", color = DymnMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedEntries = entries.filterNot { entry ->
                            monitorMonthIndexForEntry(birthDate.toLocalDate(), entry) == idx
                        }
                        onDeleteEntries(updatedEntries)
                        if (idx == 0) onClearBirthMeasurements()
                        monthIdxToDelete = null
                        Toast.makeText(context, "Дані за місяць видалено", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Так, видалити", color = DymnRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { monthIdxToDelete = null }) {
                    Text(lt("Скасувати", "Cancel"), color = DymnCyan)
                }
            }
        )
    }
}

@Composable
private fun EventsPage(
    entries: List<BabyEntry>,
    tileColor: Color,
    contentPadding: PaddingValues,
    onAddMilestone: () -> Unit,
    onDeleteEntry: (BabyEntry) -> Unit,
    onEditEntry: (BabyEntry) -> Unit,
) {
    val milestones = entries
        .filter { it.type == BabyEventType.Milestone }
        .sortedByDescending { it.start }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PageHeader(
                title = lt("Події", "Events"),
                subtitle = lt("Перші рази і важливі моменти", "Firsts and milestones"),
                onAddClick = onAddMilestone,
            )
        }
        if (milestones.isEmpty()) {
            item {
                LauncherWidgetCard(
                    modifier = Modifier.fillMaxWidth(),
                    tileColor = tileColor,
                ) {
                    Text(
                        text = lt("Тут можна зберігати перший переворот, перше гуління, першу усмішку та інші маленькі перемоги.", "Save first rolls, first coos, first smiles, and other small victories here."),
                        color = DymnMuted,
                        style = dymnText(MaterialTheme.typography.bodyMedium),
                    )
                }
            }
        } else {
            items(milestones, key = { it.id }) { entry ->
                MilestoneItem(
                    entry = entry,
                    tileColor = tileColor,
                    onClick = { onEditEntry(entry) },
                    onDelete = { onDeleteEntry(entry) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MilestoneItem(
    entry: BabyEntry,
    tileColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(tileColor)
            .border(0.6.dp, DymnLightBorder, RoundedCornerShape(28.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDelete,
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(DymnYellow.copy(alpha = 0.2f))
                .border(0.8.dp, DymnYellow.copy(alpha = 0.58f), RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = EventIcons.Milestone,
                contentDescription = null,
                tint = DymnYellow,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.note.ifBlank { lt("Важлива подія", "Important event") },
                color = DymnText,
                style = dymnText(MaterialTheme.typography.titleMedium),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.start.format(BirthDateFormatter),
                color = DymnMuted,
                style = dymnText(MaterialTheme.typography.bodySmall),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DynamicsWidget(stats: List<MonthStats>, tileColor: Color) {
    val pagerState = rememberPagerState(pageCount = { 6 })
    
    LauncherWidgetCard(
        modifier = Modifier.fillMaxWidth(),
        tileColor = tileColor
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { pageIndex ->
            Column {
                val title = when(pageIndex) {
                    0 -> lt("Тренд ваги", "Weight trend")
                    1 -> lt("Тренд зросту", "Height trend")
                    2 -> lt("Тренд сну", "Sleep trend")
                    3 -> lt("Тренд активності", "Activity trend")
                    4 -> lt("Тренд годування", "Feeding trend")
                    else -> lt("Тренд туалету", "Toilet trend")
                }
                
                val values = when(pageIndex) {
                    0 -> stats.mapNotNull { it.weight.toDoubleOrNull() }
                    1 -> stats.mapNotNull { it.height.toDoubleOrNull() }
                    2 -> stats.map { it.sleepHours.toDouble() }
                    3 -> stats.map { it.activeHours.toDouble() }
                    4 -> stats.map { it.feedingCount.toDouble() }
                    else -> stats.map { it.toiletCount.toDouble() }
                }
                
                val color = when(pageIndex) {
                    0 -> DymnGreen
                    1 -> DymnCyan
                    2 -> DymnBlue
                    3 -> DymnGreen
                    4 -> DymnCyan
                    else -> DymnYellow
                }

                Text(
                    text = title ?: lt("Сон за сьогодні", "Sleep today"),
                    color = DymnText,
                    style = dymnText(MaterialTheme.typography.titleLarge).copy(fontWeight = FontWeight.Bold),
                )
                
                if (values.size >= 2) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.Bottom) {
                        val displayStats = if (pageIndex <= 1) stats.filter { (if(pageIndex==0) it.weight else it.height).isNotBlank() } else stats
                        displayStats.takeLast(8).forEach { s ->
                            val v = when(pageIndex) {
                                0 -> s.weight.toDoubleOrNull() ?: 0.0
                                1 -> s.height.toDoubleOrNull() ?: 0.0
                                2 -> s.sleepHours.toDouble()
                                3 -> s.activeHours.toDouble()
                                4 -> s.feedingCount.toDouble()
                                else -> s.toiletCount.toDouble()
                            }
                            val maxV = values.maxOrNull() ?: 1.0
                            val minV = values.minOrNull() ?: 0.0
                            val ratio = if (maxV != minV) (v - minV) / (maxV - minV) else 0.5
                            val barHeight = 20.dp + (40.dp * ratio.toFloat())
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(color.copy(alpha = 0.6f))
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(76.dp), contentAlignment = Alignment.Center) {
                        Text(lt("Недостатньо даних", "Not enough data"), color = DymnMuted, style = dymnText(MaterialTheme.typography.bodySmall))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                val footerLabel = when(pageIndex) {
                    0 -> lt("Загальний приріст", "Total gain")
                    1 -> lt("Загальний ріст", "Total growth")
                    else -> lt("Середнє за день", "Daily average")
                }
                
                val footerValue = when(pageIndex) {
                    0 -> {
                        val gain = (values.lastOrNull() ?: 0.0) - (values.firstOrNull() ?: 0.0)
                        if (gain > 0) "+${String.format(UaLocale, "%.2f", gain)} ${dymnText(LocalDymnLanguage.current, "кг", "kg")}" else "${String.format(UaLocale, "%.2f", gain)} ${dymnText(LocalDymnLanguage.current, "кг", "kg")}"
                    }
                    1 -> {
                        val gain = (values.lastOrNull() ?: 0.0) - (values.firstOrNull() ?: 0.0)
                        if (gain > 0) "+${gain.toInt()} ${dymnText(LocalDymnLanguage.current, "см", "cm")}" else "${gain.toInt()} ${dymnText(LocalDymnLanguage.current, "см", "cm")}"
                    }
                    2, 3 -> "${formatMonitorAverage(values.lastOrNull() ?: 0.0)}${dymnText(LocalDymnLanguage.current, "г", "h")}"
                    else -> formatMonitorAverage(values.lastOrNull() ?: 0.0)
                }

                val recentDelta = values.takeIf { it.size >= 2 }
                    ?.let { it.last() - it[it.lastIndex - 1] }
                val recentDeltaLabel = lt("Остання різниця", "Latest change")
                val recentDeltaValue = recentDelta?.let { delta ->
                    val sign = if (delta > 0) "+" else ""
                    when (pageIndex) {
                        0 -> "$sign${String.format(UaLocale, "%.2f", delta)} ${dymnText(LocalDymnLanguage.current, "кг", "kg")}"
                        1 -> "$sign${String.format(UaLocale, "%.1f", delta)} ${dymnText(LocalDymnLanguage.current, "см", "cm")}"
                        2, 3 -> "$sign${formatMonitorAverage(delta)} ${dymnText(LocalDymnLanguage.current, "год", "h")}"
                        else -> "$sign${formatMonitorAverage(delta)}"
                    }
                }
                val recentDeltaColor = when {
                    recentDelta == null || recentDelta == 0.0 -> DymnMuted
                    recentDelta > 0 -> DymnGreen
                    else -> DymnRed
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(footerLabel, color = DymnMuted, style = dymnText(MaterialTheme.typography.labelSmall))
                        Text(footerValue, color = color, style = dymnText(MaterialTheme.typography.titleLarge).copy(fontWeight = FontWeight.Bold))
                    }
                    recentDeltaValue?.let { deltaText ->
                        Column(horizontalAlignment = Alignment.End) {
                            Text(recentDeltaLabel, color = DymnMuted, style = dymnText(MaterialTheme.typography.labelSmall))
                            Text(deltaText, color = recentDeltaColor, style = dymnText(MaterialTheme.typography.titleLarge).copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
        
        // Індикатори сторінок
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(6) { iteration ->
                val color = if (pagerState.currentPage == iteration) DymnCyan else DymnMuted.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                        .size(16.dp, 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonitorCard(stats: MonthStats, tileColor: Color, showDeltas: Boolean = true, onClick: () -> Unit = {}, onDelete: () -> Unit) {
    LauncherWidgetCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDelete
            ),
        tileColor = tileColor
    ) {
        Text(
            text = stats.label,
            color = DymnText,
            style = dymnText(MaterialTheme.typography.titleMedium).copy(fontWeight = FontWeight.Bold),
        )
        
        if (stats.sleepHours > 0 || stats.activeHours > 0 || stats.toiletCount > 0) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (stats.sleepHours > 0) MonitorStat(lt("Сон", "Sleep"), "${formatMonitorAverage(stats.sleepHours)}г", if (showDeltas) stats.sleepDelta else 0.0, DymnBlue)
                if (stats.activeHours > 0) MonitorStat(lt("Рух", "Move"), "${formatMonitorAverage(stats.activeHours)}г", if (showDeltas) stats.activeDelta else 0.0, DymnGreen)
                if (stats.toiletCount > 0) MonitorStat("WC", formatMonitorAverage(stats.toiletCount), if (showDeltas) stats.toiletDelta else 0.0, DymnYellow)
            }
        }
        
        if (stats.weight.isNotBlank() || stats.height.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(0.6.dp).background(DymnText.copy(alpha = 0.1f)))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (stats.weight.isNotBlank()) {
                    Column {
                        Text(lt("Вага", "Weight"), color = DymnMuted, style = dymnText(MaterialTheme.typography.labelSmall))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${stats.weight} кг", color = DymnText, style = dymnText(MaterialTheme.typography.titleMedium))
                            if (showDeltas && stats.weightDelta.isNotBlank()) {
                                Text(" (${stats.weightDelta})", color = if (stats.weightDelta.startsWith("+")) DymnGreen else DymnRed, style = dymnText(MaterialTheme.typography.bodySmall))
                            }
                        }
                    }
                }
                if (stats.height.isNotBlank()) {
                    Column {
                        Text(lt("Зріст", "Height"), color = DymnMuted, style = dymnText(MaterialTheme.typography.labelSmall))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${stats.height} см", color = DymnText, style = dymnText(MaterialTheme.typography.titleMedium))
                            if (showDeltas && stats.heightDelta.isNotBlank()) {
                                Text(" (${stats.heightDelta})", color = if (stats.heightDelta.startsWith("+")) DymnGreen else DymnRed, style = dymnText(MaterialTheme.typography.bodySmall))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonitorStat(label: String, value: String, delta: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = DymnMuted, style = dymnText(MaterialTheme.typography.labelSmall))
        Text(text = value, color = color, style = dymnText(MaterialTheme.typography.titleLarge).copy(fontWeight = FontWeight.Bold))
        if (delta != 0.0) {
            Text(
                text = if (delta > 0) "+${formatMonitorAverage(delta)}" else formatMonitorAverage(delta),
                color = if (delta > 0) DymnGreen else DymnRed,
                style = dymnText(MaterialTheme.typography.labelSmall)
            )
        }
    }
}

@Composable
private fun Header(
    profile: BabyProfile,
    entries: List<BabyEntry>,
    onOpenSettings: () -> Unit,
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            kotlinx.coroutines.delay(30_000)
        }
    }
    val activeSleep = entries.firstOrNull { it.type == BabyEventType.Sleep && it.end == null }
    val language = LocalDymnLanguage.current
    val date = remember(now, language) {
        val locale = dymnLocale(language)
        val day = now.dayOfWeek.getDisplayName(JavaTextStyle.FULL, locale)
        val month = now.month.getDisplayName(JavaTextStyle.FULL, locale)
        "$day, ${now.dayOfMonth} $month"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = now.format(TimeFormatter),
                color = DymnText,
                style = dymnText(MaterialTheme.typography.displayMedium).copy(fontWeight = FontWeight.Light),
            )
            Text(
                text = activeSleep?.let { lt("Сон триває", "Sleep active for") + " ${formatDuration(it.durationUntil(now))}" }
                    ?: date,
                color = DymnMuted,
                style = dymnText(MaterialTheme.typography.titleLarge),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = lt("Налаштування", "Settings"),
                tint = DymnMuted.copy(alpha = 0.7f),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun PageHeader(
    title: String,
    subtitle: String,
    onAddClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title ?: lt("Сон за сьогодні", "Sleep today"),
                color = DymnText,
                style = dymnText(MaterialTheme.typography.displayMedium).copy(fontWeight = FontWeight.Light),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = DymnMuted,
                style = dymnText(MaterialTheme.typography.titleLarge),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onAddClick != null || onSettingsClick != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onSettingsClick != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onSettingsClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = lt("Налаштування активності", "Activity settings"),
                            tint = DymnMuted.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                if (onAddClick != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onAddClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = lt("Додати вручну", "Add manually"),
                            tint = DymnMuted.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChildSelectionPage(
    profiles: List<BabyProfile>,
    tileColor: Color,
    onSelect: (BabyProfile) -> Unit,
    onDeleteRequest: (BabyProfile) -> Unit,
    onAdd: () -> Unit
) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topPadding + 40.dp, bottom = 40.dp, start = 24.dp, end = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = lt("Оберіть дитину", "Choose a child"),
                color = DymnText,
                style = dymnText(MaterialTheme.typography.displayMedium).copy(fontWeight = FontWeight.Light),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        items(profiles) { profile ->
            LauncherWidgetCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onSelect(profile) },
                        onLongClick = { onDeleteRequest(profile) }
                    ),
                tileColor = tileColor
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    BabyPhoto(
                        profile = profile,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(
                            text = profile.displayName,
                            color = DymnText,
                            style = dymnText(MaterialTheme.typography.headlineSmall)
                        )
                        Text(
                            text = profile.ageLabel(),
                            color = DymnMuted,
                            style = dymnText(MaterialTheme.typography.bodyMedium)
                        )
                    }
                }
            }
        }

        item {
            LauncherWidgetCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAdd() },
                tileColor = tileColor.copy(alpha = tileColor.alpha * 0.5f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = DymnCyan,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = lt("Додати дитину", "Add child"),
                        color = DymnCyan,
                        style = dymnText(MaterialTheme.typography.titleMedium)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: BabyProfile,
    tileColor: Color,
    now: LocalDateTime,
    onEditProfile: () -> Unit,
    onEditPhoto: () -> Unit,
) {
    LauncherWidgetCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp),
        tileColor = tileColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BabyPhoto(
                profile = profile,
                modifier = Modifier
                    .size(160.dp)
                    .clickable(onClick = onEditPhoto),
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column(
                modifier = Modifier
                    .weight(1f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = profile.displayName,
                        color = DymnText,
                        style = dymnText(MaterialTheme.typography.headlineMedium),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onEditProfile),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = lt("Редагувати", "Edit"),
                            tint = DymnMuted.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    text = profile.ageLabel(now),
                    color = DymnMuted,
                    style = dymnText(MaterialTheme.typography.titleMedium),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                profile.birthdayCountdown(now)?.let { countdown ->
                    Text(
                        text = countdown,
                        color = DymnCyan.copy(alpha = 0.85f),
                        style = dymnText(MaterialTheme.typography.bodyMedium).copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            WidgetStat(
                label = lt("Вага", "Weight"),
                value = if (profile.weight.isNotBlank()) {
                    val parts = profile.weight.split(".")
                    val kg = parts.getOrNull(0) ?: "0"
                    val g = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    if (g > 0) "$kg кг $g г" else "$kg кг"
                } else "--",
                color = DymnGreen
            )
            WidgetStat(
                label = lt("Зріст", "Height"),
                value = if (profile.height.isNotBlank()) "${profile.height} см" else "--",
                color = DymnCyan
            )
            WidgetStat(
                label = lt("Народження", "Birth"),
                value = profile.birthDate.ifBlank { "--" },
                color = DymnYellow
            )
        }
    }
}

@Composable
private fun BabyPhoto(
    profile: BabyProfile,
    modifier: Modifier = Modifier,
) {
    val bitmap = rememberUriImage(profile.photoUri)
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(DymnBlue)
            .border(0.6.dp, DymnLightBorder, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            BoxWithPhotoOffset(profile = profile) { offsetModifier ->
                Image(
                    bitmap = bitmap,
                    contentDescription = profile.displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(offsetModifier)
                        .graphicsLayer {
                            scaleX = profile.photoScale
                            scaleY = profile.photoScale
                            rotationZ = profile.photoRotation
                        },
                    contentScale = ContentScale.Crop,
                )
            }
        } else {
            Text(
                text = profile.initials,
                color = Color.White,
                style = dymnText(MaterialTheme.typography.displaySmall).copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BoxWithPhotoOffset(
    profile: BabyProfile,
    content: @Composable (Modifier) -> Unit,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val translationX = profile.photoOffsetX * constraints.maxWidth
        val translationY = profile.photoOffsetY * constraints.maxHeight
        val offsetModifier = Modifier.graphicsLayer {
            this.translationX = translationX
            this.translationY = translationY
        }
        content(offsetModifier)
    }
}

@Composable
private fun ActionStrip(
    title: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(color.copy(alpha = 0.75f))
            .border(0.5.dp, DymnLightBorder.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title ?: lt("Сон за сьогодні", "Sleep today"),
            color = DymnText,
            style = dymnText(MaterialTheme.typography.titleMedium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SummaryWidget(entries: List<BabyEntry>, tileColor: Color, title: String? = null) {
    val summaryDate = LocalDate.now()
    val now = LocalDateTime.now()
    val dayEntries = entries.onDate(summaryDate)
    val sleepMinutes = entries.filter { it.type == BabyEventType.Sleep }.sumDurationMinutesOn(summaryDate, now)
    val activeMinutes = entries.filter { it.type == BabyEventType.Active }.sumDurationMinutesOn(summaryDate, now)
    val toiletCount = dayEntries.count { it.type == BabyEventType.Toilet }
    val feedingCount = dayEntries.count { it.type == BabyEventType.Feeding }

    LauncherWidgetCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp),
        tileColor = tileColor
    ) {
        Text(
            text = formatMinutes(sleepMinutes),
            color = DymnText,
            style = dymnText(MaterialTheme.typography.displayMedium).copy(fontWeight = FontWeight.Light),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = title ?: lt("Сон за сьогодні", "Sleep today"),
            color = DymnMuted,
            style = dymnText(MaterialTheme.typography.titleMedium),
        )
        Text(
            text = lt("Активність", "Activity") + " ${formatMinutes(activeMinutes)}",
            color = DymnMuted,
            style = dymnText(MaterialTheme.typography.titleSmall),
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            WidgetStat(label = lt("Сон", "Sleep"), value = formatMinutes(sleepMinutes), color = DymnBlue)
            WidgetStat(label = lt("Рух", "Move"), value = formatMinutes(activeMinutes), color = DymnGreen)
            WidgetStat(label = "WC", value = toiletCount.toString(), color = DymnYellow)
            WidgetStat(label = lt("Їжа", "Food"), value = feedingCount.toString(), color = DymnCyan)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SummaryRow(
    label: String,
    value: String,
    buttonLabel: String,
    color: Color,
    onClick: (() -> Unit)?,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(0.6.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = DymnText,
            style = dymnText(MaterialTheme.typography.titleLarge).copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = value,
            color = DymnText,
            style = dymnText(MaterialTheme.typography.bodyLarge),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        if (onClick != null) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(color.copy(alpha = 0.9f))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buttonLabel,
                    color = Color.White,
                    style = dymnText(MaterialTheme.typography.labelMedium).copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LegacyEditEntryDialog(
    entry: BabyEntry,
    tileColor: Color,
    onDismiss: () -> Unit,
    onSave: (BabyEntry) -> Unit
) {
    var startTime by remember { mutableStateOf(entry.start.toLocalTime()) }
    var endTime by remember { mutableStateOf(entry.end?.toLocalTime() ?: java.time.LocalTime.now()) }
    var isOngoing by remember { mutableStateOf(entry.end == null) }
    var isPickingStart by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tileColor,
        shape = RoundedCornerShape(32.dp),
        title = { Text(dymnText(LocalDymnLanguage.current, "Редагування", "Editing") + ": ${entry.type.localizedLabel()}", color = DymnText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ManualTimeField(label = lt("Початок", "Start"), time = startTime) { isPickingStart = true }
                
                if (entry.type == BabyEventType.Sleep || entry.type == BabyEventType.Walk || entry.type == BabyEventType.Active) {
                    OngoingToggle(selected = isOngoing, tileColor = tileColor, onClick = { isOngoing = !isOngoing })
                    if (!isOngoing) {
                        ManualTimeField(label = lt("Кінець", "End"), time = endTime) { isPickingStart = false }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WheelPicker(
                            items = (0..23).map { it.toString().padStart(2, '0') },
                            initialIndex = if (isPickingStart) startTime.hour else endTime.hour,
                            onSelectionChanged = { h ->
                                if (isPickingStart) startTime = startTime.withHour(h) else endTime = endTime.withHour(h)
                            },
                            width = 50.dp
                        )
                        Text(":", color = DymnText, style = dymnText(MaterialTheme.typography.titleLarge))
                        WheelPicker(
                            items = (0..59).map { it.toString().padStart(2, '0') },
                            initialIndex = if (isPickingStart) startTime.minute else endTime.minute,
                            onSelectionChanged = { m ->
                                if (isPickingStart) startTime = startTime.withMinute(m) else endTime = endTime.withMinute(m)
                            },
                            width = 50.dp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val today = entry.start.toLocalDate()
                onSave(entry.copy(
                    start = LocalDateTime.of(today, startTime),
                    end = if (isOngoing) null else LocalDateTime.of(today, endTime)
                ))
            }) { Text(lt("Зберегти", "Save"), color = DymnCyan) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(lt("Скасувати", "Cancel"), color = DymnMuted) }
        }
    )
}

@Composable
private fun FocusWidget(
    value: String,
    title: String,
    subtitle: String,
    color: Color,
    tileColor: Color,
) {
    LauncherWidgetCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(174.dp),
        tileColor = tileColor
    ) {
        Text(
            text = value,
            color = DymnText,
            style = dymnText(MaterialTheme.typography.displayMedium).copy(fontWeight = FontWeight.Light),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = title ?: lt("Сон за сьогодні", "Sleep today"),
            color = DymnMuted,
            style = dymnText(MaterialTheme.typography.titleLarge),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.78f)),
        )
        Text(
            text = subtitle,
            color = DymnMuted,
            style = dymnText(MaterialTheme.typography.bodySmall),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ActionPill(
    title: String,
    icon: ImageVector,
    iconText: String? = null,
    color: Color,
    tileColor: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier = Modifier
            .widthIn(min = 220.dp, max = 304.dp)
            .height(104.dp)
            .clip(shape)
            .background(tileColor)
            .border(0.6.dp, DymnLightBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            if (iconText != null) {
                Text(
                    text = iconText,
                    color = Color.White,
                    style = dymnText(MaterialTheme.typography.titleLarge).copy(fontWeight = FontWeight.Bold),
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(26.dp))
        Text(
            text = title ?: lt("Сон за сьогодні", "Sleep today"),
            color = DymnText,
            style = dymnText(MaterialTheme.typography.titleLarge),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LauncherWidgetCard(
    modifier: Modifier = Modifier,
    tileColor: Color = DymnTileFill,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(32.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(tileColor)
            .border(0.6.dp, DymnLightBorder, shape)
            .padding(horizontal = 24.dp, vertical = 22.dp),
        content = content,
    )
}

@Composable
private fun WidgetStat(
    label: String,
    value: String,
    color: Color,
) {
    Column {
        Text(
            text = label,
            color = DymnMuted,
            style = dymnText(MaterialTheme.typography.labelLarge),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .padding(top = 6.dp, bottom = 6.dp)
                .height(3.dp)
                .width(30.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        Text(
            text = value,
            color = DymnText,
            style = dymnText(MaterialTheme.typography.titleMedium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineItem(
    entry: BabyEntry,
    tileColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(tileColor)
            .border(0.6.dp, DymnLightBorder, RoundedCornerShape(26.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDelete,
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(entry.type.color.copy(alpha = 0.18f))
                .border(0.8.dp, entry.type.color.copy(alpha = 0.55f), RoundedCornerShape(23.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (entry.type == BabyEventType.Toilet) {
                Text(
                    text = "WC",
                    color = entry.type.color,
                    style = dymnText(MaterialTheme.typography.titleMedium).copy(fontWeight = FontWeight.Bold),
                )
            } else {
                Icon(
                    imageVector = entry.type.icon,
                    contentDescription = null,
                    tint = entry.type.color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.type.localizedLabel(),
                color = DymnText,
                style = dymnText(MaterialTheme.typography.titleMedium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.timeLabel(),
                color = DymnMuted,
                style = dymnText(MaterialTheme.typography.bodySmall),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.note.isNotBlank()) {
                Text(
                    text = entry.note,
                    color = DymnText.copy(alpha = 0.74f),
                    style = dymnText(MaterialTheme.typography.bodySmall),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyTimeline(tileColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(tileColor)
            .border(0.6.dp, DymnLightBorder, RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = lt("Записів ще немає", "No records yet"),
            color = DymnMuted,
            style = dymnText(MaterialTheme.typography.bodyLarge),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = DymnText,
        style = dymnText(MaterialTheme.typography.headlineSmall).copy(fontWeight = FontWeight.Light),
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun ProfileDialog(
    profile: BabyProfile,
    tileColor: Color,
    onDismiss: () -> Unit,
    onSave: (BabyProfile) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(profile.name) }
    var birthDate by remember { mutableStateOf(profile.birthDate) }
    var weight by remember { mutableStateOf(profile.weight) }
    var height by remember { mutableStateOf(profile.height) }
    var isPickerOpen by remember { mutableStateOf(false) }
    var isWeightPickerOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tileColor,
        shape = RoundedCornerShape(32.dp),
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = lt("Дані дитини", "Child details"), color = DymnText)
                if (profile.name.isNotBlank()) {
                    TextButton(onClick = onDelete) {
                        Text(lt("Видалити", "Delete"), color = DymnRed.copy(alpha = 0.7f))
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DymnTextField(value = name, onValueChange = { name = it }, label = lt("Ім'я", "Name"))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPickerOpen = true }
                ) {
                    DymnTextField(
                        value = birthDate,
                        onValueChange = { },
                        label = lt("Дата народження", "Birth date"),
                        enabled = false,
                    )
                    Box(modifier = Modifier.matchParentSize())
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isWeightPickerOpen = true }
                ) {
                    DymnTextField(
                        value = if (weight.isNotBlank()) {
                            val parts = weight.split(".")
                            val kg = parts.getOrNull(0) ?: "0"
                            val g = parts.getOrNull(1)?.padEnd(3, '0')?.take(3) ?: "000"
                            "$kg кг $g г"
                        } else "",
                        onValueChange = { },
                        label = lt("Вага при народженні", "Birth weight"),
                        enabled = false,
                    )
                    Box(modifier = Modifier.matchParentSize())
                }
                
                DymnTextField(value = height, onValueChange = { height = it }, label = lt("Зріст при народженні (см)", "Birth height (cm)"))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        profile.copy(
                            name = name.trim(),
                            birthDate = birthDate.trim(),
                            weight = weight.trim(),
                            height = height.trim(),
                        ),
                    )
                },
            ) {
                Text(lt("Зберегти", "Save"), color = DymnCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(lt("Скасувати", "Cancel"), color = DymnRed)
            }
        },
    )

    if (isPickerOpen) {
        DymnDateTimePicker(
            initialDateTime = parseBirthDateTime(birthDate) ?: LocalDateTime.now(),
            tileColor = tileColor,
            onDismiss = { isPickerOpen = false },
            onConfirm = { picked ->
                birthDate = picked.format(BirthDateFormatter)
                isPickerOpen = false
            }
        )
    }

    if (isWeightPickerOpen) {
        DymnWeightPicker(
            initialWeight = weight,
            tileColor = tileColor,
            onDismiss = { isWeightPickerOpen = false },
            onConfirm = { picked ->
                weight = picked
                isWeightPickerOpen = false
            }
        )
    }
}

@Composable
private fun PhotoDialog(
    profile: BabyProfile,
    tileColor: Color,
    onPickPhoto: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (BabyProfile) -> Unit,
) {
    var scale by remember(profile.photoUri, profile.photoScale) { mutableStateOf(profile.photoScale.coerceIn(1f, 3f)) }
    var rotation by remember(profile.photoUri, profile.photoRotation) { mutableStateOf(normalizeRotation(profile.photoRotation)) }
    var offsetX by remember(profile.photoUri, profile.photoOffsetX) { mutableStateOf(profile.photoOffsetX.coerceIn(-2f, 2f)) }
    var offsetY by remember(profile.photoUri, profile.photoOffsetY) { mutableStateOf(profile.photoOffsetY.coerceIn(-2f, 2f)) }
    val previewProfile = profile.copy(
        photoScale = scale,
        photoRotation = rotation,
        photoOffsetX = offsetX,
        photoOffsetY = offsetY,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tileColor,
        shape = RoundedCornerShape(32.dp),
        title = { Text(text = lt("Фото малюка", "Baby photo"), color = DymnText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val sizePx = with(density) { 240.dp.toPx() }
                    
                    BabyPhoto(
                        profile = previewProfile,
                        modifier = Modifier
                            .size(240.dp)
                            .pointerInput(profile.photoUri) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    // Збільшуємо діапазон для вільного перетягування
                                    if (sizePx > 0) {
                                        offsetX = (offsetX + dragAmount.x / sizePx).coerceIn(-2f, 2f)
                                        offsetY = (offsetY + dragAmount.y / sizePx).coerceIn(-2f, 2f)
                                    }
                                }
                            },
                    )
                }
                
                Text(
                    text = lt("Тягніть фото пальцем, щоб вирівняти", "Drag the photo to align it"),
                    color = DymnMuted,
                    style = dymnText(MaterialTheme.typography.bodySmall),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Text(
                    text = lt("Масштаб", "Zoom"),
                    color = DymnMuted,
                    style = dymnText(MaterialTheme.typography.labelLarge),
                )
                Slider(
                    value = scale,
                    onValueChange = { scale = it.coerceIn(1f, 3f) },
                    valueRange = 1f..3f,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DialogActionButton(
                        title = lt("Фото", "Photo"),
                        color = DymnCyan,
                        onClick = onPickPhoto,
                        modifier = Modifier.weight(1f),
                    )
                    DialogActionButton(
                        title = lt("Повернути", "Rotate"),
                        color = DymnBlue,
                        onClick = { rotation = normalizeRotation(rotation + 90f) },
                        modifier = Modifier.weight(1f),
                    )
                }
                DialogActionButton(
                    title = lt("Скинути кадр", "Reset frame"),
                    color = DymnGraphite,
                    onClick = {
                        scale = 1f
                        rotation = 0f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        profile.copy(
                            photoScale = scale,
                            photoRotation = rotation,
                            photoOffsetX = offsetX,
                            photoOffsetY = offsetY,
                        ),
                    )
                },
            ) {
                Text(lt("Зберегти", "Save"), color = DymnCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(lt("Скасувати", "Cancel"), color = DymnRed)
            }
        },
    )
}

@Composable
private fun DialogActionButton(
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(color.copy(alpha = 0.76f))
            .border(0.5.dp, DymnLightBorder.copy(alpha = 0.42f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title ?: lt("Сон за сьогодні", "Sleep today"),
            color = DymnText,
            style = dymnText(MaterialTheme.typography.labelLarge),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun canScheduleExactWidgetAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return alarmManager.canScheduleExactAlarms()
}

private fun openExactAlarmPermissionSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        Toast.makeText(
            context,
            dymnText(context, "Для цієї версії Android окремий дозвіл не потрібний", "This Android version does not need a separate permission"),
            Toast.LENGTH_SHORT,
        ).show()
        return
    }

    val packageUri = Uri.parse("package:${context.packageName}")
    val exactAlarmIntent = Intent(
        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
        packageUri,
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val fallbackIntent = Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        packageUri,
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching {
        context.startActivity(exactAlarmIntent)
    }.onFailure {
        context.startActivity(fallbackIntent)
    }
}

@Composable
private fun BirthMeasurementDialog(
    profile: BabyProfile,
    tileColor: Color,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    title: String = "Місяць народження",
    isBirthMeasurement: Boolean = true,
) {
    var weight by remember(profile.weight) { mutableStateOf(profile.weight) }
    var height by remember(profile.height) { mutableStateOf(profile.height) }
    var isWeightPickerOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tileColor,
        shape = RoundedCornerShape(32.dp),
        title = { Text(title, color = DymnText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.fillMaxWidth().clickable { isWeightPickerOpen = true }) {
                    DymnTextField(
                        value = if (weight.isNotBlank()) {
                            val parts = weight.split(".")
                            val kg = parts.getOrNull(0) ?: "0"
                            val g = parts.getOrNull(1)?.padEnd(3, '0')?.take(3) ?: "000"
                            "$kg кг $g г"
                        } else "",
                        onValueChange = {},
                        label = if (isBirthMeasurement) lt("Вага при народженні", "Birth weight") else lt("Вага", "Weight"),
                        enabled = false,
                    )
                    Box(modifier = Modifier.matchParentSize())
                }
                DymnTextField(value = height, onValueChange = { height = it }, label = if (isBirthMeasurement) lt("Зріст при народженні (см)", "Birth height (cm)") else "Зріст (см)")
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(weight.trim(), height.trim()) }) {
                Text(lt("Зберегти", "Save"), color = DymnCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(lt("Скасувати", "Cancel"), color = DymnMuted) }
        },
    )

    if (isWeightPickerOpen) {
        DymnWeightPicker(
            initialWeight = weight,
            tileColor = tileColor,
            onDismiss = { isWeightPickerOpen = false },
            onConfirm = { picked ->
                weight = picked
                isWeightPickerOpen = false
            },
        )
    }
}
@Composable
private fun ReminderMeasurementDialog(
    profile: BabyProfile,
    isYearlyBirthday: Boolean,
    tileColor: Color,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var weight by remember { mutableStateOf(profile.weight) }
    var height by remember { mutableStateOf(profile.height) }
    var isWeightPickerOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tileColor,
        shape = RoundedCornerShape(32.dp),
        title = { 
            Column {
                if (isYearlyBirthday) {
                    Text(lt("З днем народження!", "Happy birthday!"), color = DymnText, style = dymnText(MaterialTheme.typography.titleLarge))
                } else {
                    Text(lt("Планове оновлення", "Scheduled update"), color = DymnText, style = dymnText(MaterialTheme.typography.titleLarge))
                }
                Text(lt("Малюку виповнилося", "Baby age") + ": ${profile.ageLabel()}", color = DymnMuted, style = dymnText(MaterialTheme.typography.bodyMedium))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isYearlyBirthday) lt("Час оновити параметри у цей святковий день:", "Time to update measurements on this special day:") else lt("Час оновити параметри:", "Time to update measurements:"),
                    color = DymnText,
                    style = dymnText(MaterialTheme.typography.bodyMedium)
                )
                
                Box(
                    modifier = Modifier.fillMaxWidth().clickable { isWeightPickerOpen = true }
                ) {
                    DymnTextField(
                        value = if (weight.isNotBlank()) {
                            val parts = weight.split(".")
                            val kg = parts.getOrNull(0) ?: "0"
                            val g = parts.getOrNull(1)?.padEnd(3, '0')?.take(3) ?: "000"
                            "$kg кг $g г"
                        } else "",
                        onValueChange = { },
                        label = lt("Вага", "Weight"),
                        enabled = false,
                    )
                    Box(modifier = Modifier.matchParentSize())
                }

                DymnTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = lt("Зріст (см)", "Height (cm)")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(weight, height) }
            ) {
                Text(lt("Оновити", "Update"), color = DymnCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(lt("Пізніше", "Later"), color = DymnMuted)
            }
        }
    )

    if (isWeightPickerOpen) {
        DymnWeightPicker(
            initialWeight = weight,
            tileColor = tileColor,
            onDismiss = { isWeightPickerOpen = false },
            onConfirm = { picked ->
                weight = picked
                isWeightPickerOpen = false
            }
        )
    }
}

@Composable
private fun HistoricalMeasurementDialog(
    profile: BabyProfile,
    tileColor: Color,
    onDismiss: () -> Unit,
    onSave: (LocalDate, String, String) -> Unit
) {
    val birthDate = parseBirthDateTime(profile.birthDate)?.toLocalDate() ?: LocalDate.now()
    val months = remember { (0..24).map { monitorMonthLabel(it) } }
    var selectedMonthIdx by remember { mutableStateOf(0) }
    
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var isWeightPickerOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tileColor,
        shape = RoundedCornerShape(32.dp),
        title = { Text(lt("Параметри за минулий період", "Previous period measurements"), color = DymnText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(lt("Оберіть місяць:", "Choose month:"), color = DymnMuted, style = dymnText(MaterialTheme.typography.labelLarge))
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Box(Modifier.fillMaxWidth().height(0.6.dp).background(DymnCyan.copy(alpha = 0.42f)))
                        Spacer(Modifier.height(44.dp))
                        Box(Modifier.fillMaxWidth().height(0.6.dp).background(DymnCyan.copy(alpha = 0.42f)))
                    }
                    WheelPicker(
                        items = months,
                        initialIndex = selectedMonthIdx,
                        onSelectionChanged = { selectedMonthIdx = it },
                        width = 200.dp
                    )
                }

                Box(
                    modifier = Modifier.fillMaxWidth().clickable { isWeightPickerOpen = true }
                ) {
                    DymnTextField(
                        value = if (weight.isNotBlank()) {
                            val parts = weight.split(".")
                            val kg = parts.getOrNull(0) ?: "0"
                            val g = parts.getOrNull(1)?.padEnd(3, '0')?.take(3) ?: "000"
                            "$kg кг $g г"
                        } else "",
                        onValueChange = { },
                        label = lt("Вага", "Weight"),
                        enabled = false,
                    )
                    Box(modifier = Modifier.matchParentSize())
                }

                DymnTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = lt("Зріст (см)", "Height (cm)")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val date = birthDate.plusMonths(selectedMonthIdx.toLong())
                    onSave(date, weight, height)
                }
            ) {
                Text(lt("Зберегти", "Save"), color = DymnCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(lt("Скасувати", "Cancel"), color = DymnRed)
            }
        }
    )

    if (isWeightPickerOpen) {
        DymnWeightPicker(
            initialWeight = weight,
            tileColor = tileColor,
            onDismiss = { isWeightPickerOpen = false },
            onConfirm = { picked ->
                weight = picked
                isWeightPickerOpen = false
            }
        )
    }
}
    @Composable
private fun NoteDialog(
    title: String,
    tileColor: Color,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tileColor,
        shape = RoundedCornerShape(32.dp),
        title = { Text(text = title ?: lt("Сон за сьогодні", "Sleep today"), color = DymnText) },
        text = {
            DymnTextField(
                value = note,
                onValueChange = { note = it },
                label = lt("Коментар", "Comment"),
                minLines = 3,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(note) }) {
                Text(lt("Зберегти", "Save"), color = DymnCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(lt("Скасувати", "Cancel"), color = DymnRed)
            }
        },
    )
}

@Composable
private fun MilestoneDialog(
    tileColor: Color,
    onDismiss: () -> Unit,
    onSave: (String, LocalDateTime) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().atStartOfDay()) }
    var isDatePickerOpen by remember { mutableStateOf(false) }

    val defaultMilestoneTitle = lt("Важлива подія", "Important event")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tileColor,
        shape = RoundedCornerShape(32.dp),
        title = { Text(lt("Нова подія", "New event"), color = DymnText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DymnTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = lt("Що сталося?", "What happened?"),
                    minLines = 2,
                )
                DialogActionButton(
                    title = "Дата: ${date.format(BirthDateFormatter)}",
                    color = DymnCyan,
                    onClick = { isDatePickerOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanTitle = title.trim()
                    onSave(cleanTitle.ifBlank { defaultMilestoneTitle }, date)
                }
            ) {
                Text(lt("Зберегти", "Save"), color = DymnCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(lt("Скасувати", "Cancel"), color = DymnRed)
            }
        }
    )

    if (isDatePickerOpen) {
        DymnDateTimePicker(
            initialDateTime = date,
            tileColor = tileColor,
            onDismiss = { isDatePickerOpen = false },
            onConfirm = { picked ->
                date = picked.toLocalDate().atStartOfDay()
                isDatePickerOpen = false
            }
        )
    }
}

@Composable
private fun DymnTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    minLines: Int = 1,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = minLines == 1,
        minLines = minLines,
        enabled = enabled,
        readOnly = readOnly,
        shape = RoundedCornerShape(22.dp),
        colors = TextFieldDefaults.colors(
            focusedTextColor = DymnText,
            unfocusedTextColor = DymnText,
            disabledTextColor = DymnText,
            focusedContainerColor = DymnGraphite.copy(alpha = 0.76f),
            unfocusedContainerColor = DymnGraphite.copy(alpha = 0.56f),
            disabledContainerColor = DymnGraphite.copy(alpha = 0.56f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedLabelColor = DymnText,
            unfocusedLabelColor = DymnMuted,
            disabledLabelColor = DymnMuted,
            cursorColor = DymnText,
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(0.6.dp, DymnLightBorder.copy(alpha = 0.24f), RoundedCornerShape(22.dp)),
    )
}

@Composable
private fun ArchiveDialog(
    entries: List<BabyEntry>,
    tileColor: Color,
    now: LocalDateTime,
    onDismiss: () -> Unit
) {
    val availableDates = remember(entries) {
        entries.map { it.start.toLocalDate() }.distinct().sortedByDescending { it }
    }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var tempDateIdx by remember { mutableStateOf(0) }
    var isPickerOpen by remember { mutableStateOf(true) }

    if (isPickerOpen) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = tileColor,
            shape = RoundedCornerShape(32.dp),
            title = { Text(lt("Архів по дням", "Daily archive"), color = DymnText) },
            text = {
                if (availableDates.isEmpty()) {
                    Text(lt("Записів у минулі дні ще немає", "No records from past days yet"), color = DymnMuted)
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(0.6.dp).background(DymnCyan.copy(alpha = 0.42f)))
                            Spacer(Modifier.height(44.dp))
                            Box(Modifier.fillMaxWidth().height(0.6.dp).background(DymnCyan.copy(alpha = 0.42f)))
                        }
                        WheelPicker(
                            items = availableDates.map { it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) },
                            initialIndex = tempDateIdx,
                            onSelectionChanged = { tempDateIdx = it },
                            width = 200.dp
                        )
                    }
                }
            },
            confirmButton = {
                if (availableDates.isNotEmpty()) {
                    TextButton(onClick = { 
                        selectedDate = availableDates[tempDateIdx]
                        isPickerOpen = false 
                    }) {
                        Text(lt("Вибрати", "Select"), color = DymnCyan)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(lt("Скасувати", "Cancel"), color = DymnRed)
                }
            }
        )
    } else if (selectedDate != null) {
        val dayEntries = entries.filter { it.start.toLocalDate() == selectedDate }
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = tileColor,
            shape = RoundedCornerShape(32.dp),
            title = { Text("Підсумок за ${selectedDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}", color = DymnText) },
            text = {
                Column {
                    SummaryWidget(entries = dayEntries, tileColor = tileColor, title = lt("Підсумок", "Summary"))
                }
            },
            confirmButton = {
                TextButton(onClick = { isPickerOpen = true; selectedDate = null }) {
                    Text(lt("Інша дата", "Other date"), color = DymnCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(lt("Закрити", "Close"), color = DymnMuted)
                }
            }
        )
    }
}

@Composable
private fun WallpaperBackground(uri: String) {
    val customBitmap = rememberUriImage(uri)
    
    when {
        customBitmap != null -> {
            Image(
                bitmap = customBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        uri == "w1" -> Image(painter = painterResource(R.drawable.w1), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        uri == "w2" -> Image(painter = painterResource(R.drawable.w2), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        uri == "w3" -> Image(painter = painterResource(R.drawable.w3), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        uri == "w4" -> Image(painter = painterResource(R.drawable.w4), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else -> {
            Image(
                painter = painterResource(R.drawable.w1),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f)),
    )
}

@Composable
private fun AppSettingsDialog(
    tileColor: Color,
    currentOpacity: Float,
    currentFillKey: String,
    customFillColor: Color,
    onOpacityChange: (Float) -> Unit,
    onFillChange: (String) -> Unit,
    onCustomFillChange: (Color) -> Unit,
    onPickWallpaper: () -> Unit,
    onSelectBuiltIn: (String) -> Unit,
    canScheduleExactAlarms: Boolean,
    currentLanguage: DymnLanguage,
    onLanguageChange: (DymnLanguage) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onSwitchChild: () -> Unit,
    onOpenArchive: () -> Unit,
    onDismiss: () -> Unit,
) {
    var isCustomColorOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tileColor,
        shape = RoundedCornerShape(32.dp),
        title = { Text(text = lt("Налаштування", "Settings"), color = DymnText) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = dymnText(currentLanguage, "Діти", "Children"),
                        color = DymnMuted,
                        style = dymnText(MaterialTheme.typography.labelLarge)
                    )
                    DialogActionButton(
                        title = dymnText(currentLanguage, "Змінити дитину", "Switch child"),
                        color = DymnBlue,
                        onClick = onSwitchChild,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column {
                    Text(
                        text = lt("Прозорість тайтлів", "Tile opacity"),
                        color = DymnMuted,
                        style = dymnText(MaterialTheme.typography.labelLarge)
                    )
                    Slider(
                        value = currentOpacity,
                        onValueChange = onOpacityChange,
                        valueRange = 0.1f..1f,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = lt("Колір заливки", "Tile color"),
                        color = DymnMuted,
                        style = dymnText(MaterialTheme.typography.labelLarge)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TileFillOptions.forEach { option ->
                            val selected = option.key == currentFillKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(option.color.copy(alpha = currentOpacity))
                                    .border(
                                        width = if (selected) 1.4.dp else 0.6.dp,
                                        color = if (selected) DymnCyan else DymnLightBorder.copy(alpha = 0.32f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onFillChange(option.key) },
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                Text(
                                    text = dymnText(currentLanguage, option.title, option.titleEn),
                                    color = DymnText,
                                    style = dymnText(MaterialTheme.typography.labelSmall),
                                    maxLines = 1,
                                    modifier = Modifier.padding(bottom = 7.dp),
                                )
                            }
                        }
                    }
                    DialogActionButton(
                        title = lt("Свій колір", "Custom color"),
                        color = customFillColor,
                        onClick = { isCustomColorOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = lt("Стандартні шпалери", "Built-in wallpapers"),
                        color = DymnMuted,
                        style = dymnText(MaterialTheme.typography.labelLarge)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val builtIn = listOf(
                            "w1" to R.drawable.w1,
                            "w2" to R.drawable.w2,
                            "w3" to R.drawable.w3,
                            "w4" to R.drawable.w4
                        )
                        builtIn.forEach { (name, resId) ->
                            Image(
                                painter = painterResource(resId),
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(70.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectBuiltIn(name) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    
                    Text(
                        text = lt("Власні шпалери", "Custom wallpaper"),
                        color = DymnMuted,
                        style = dymnText(MaterialTheme.typography.labelLarge)
                    )
                    DialogActionButton(
                        title = lt("Вибрати з галереї", "Choose from gallery"),
                        color = DymnCyan,
                        onClick = onPickWallpaper,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = lt("Мова", "Language"),
                        color = DymnMuted,
                        style = dymnText(MaterialTheme.typography.labelLarge)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DymnLanguage.entries.forEach { language ->
                            val selected = language == currentLanguage
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (selected) DymnCyan.copy(alpha = 0.32f) else DymnText.copy(alpha = 0.08f))
                                    .border(
                                        width = if (selected) 1.2.dp else 0.6.dp,
                                        color = if (selected) DymnCyan else DymnLightBorder.copy(alpha = 0.36f),
                                        shape = RoundedCornerShape(18.dp),
                                    )
                                    .clickable { onLanguageChange(language) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = language.title,
                                    color = DymnText,
                                    style = dymnText(MaterialTheme.typography.labelLarge),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = lt("Дозволи", "Permissions"),
                        color = DymnMuted,
                        style = dymnText(MaterialTheme.typography.labelLarge)
                    )
                    ExactAlarmPermissionButton(
                        isGranted = canScheduleExactAlarms,
                        onClick = onOpenExactAlarmSettings,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = lt("Керування", "Manage"),
                        color = DymnMuted,
                        style = dymnText(MaterialTheme.typography.labelLarge)
                    )
                    DialogActionButton(
                        title = lt("Архів по дням", "Daily archive"),
                        color = DymnCyan,
                        onClick = onOpenArchive,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(lt("Закрити", "Close"), color = DymnCyan)
            }
        }
    )

    if (isCustomColorOpen) {
        CustomTileFillDialog(
            initialColor = customFillColor,
            tileColor = tileColor,
            onDismiss = { isCustomColorOpen = false },
            onSave = { color ->
                onCustomFillChange(color)
                isCustomColorOpen = false
            }
        )
    }
}

@Composable
private fun ActivitySettingsDialog(
    currentLimitMinutes: Int,
    tileColor: Color,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var limitMinutes by remember(currentLimitMinutes) {
        mutableStateOf(currentLimitMinutes.coerceIn(30, 360).toFloat())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tileColor,
        shape = RoundedCornerShape(32.dp),
        title = { Text(lt("Налаштування активності", "Activity settings"), color = DymnText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = lt("Ліміт активності", "Activity limit"),
                    color = DymnMuted,
                    style = dymnText(MaterialTheme.typography.labelLarge),
                )
                Text(
                    text = formatMinutes(limitMinutes.toLong()),
                    color = DymnGreen,
                    style = dymnText(MaterialTheme.typography.displaySmall).copy(fontWeight = FontWeight.Bold),
                )
                Slider(
                    value = limitMinutes,
                    onValueChange = { value ->
                        limitMinutes = ((value / 30f).toInt() * 30).coerceIn(30, 360).toFloat()
                    },
                    valueRange = 30f..360f,
                    steps = 10,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(limitMinutes.toInt()) }) {
                Text(lt("Зберегти", "Save"), color = DymnCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(lt("Скасувати", "Cancel"), color = DymnRed)
            }
        },
    )
}

@Composable
private fun ExactAlarmPermissionButton(
    isGranted: Boolean,
    onClick: () -> Unit,
) {
    val statusColor = if (isGranted) DymnGreen else DymnYellow
    val subtitle = if (isGranted) {
        lt("Дозвіл активний, віджет може стабільно оновлюватись.", "Permission is active, the widget can update reliably.")
    } else {
        lt("Потрібно для стабільного оновлення віджета у фоні.", "Needed for reliable background widget updates.")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(statusColor.copy(alpha = 0.28f))
            .border(
                width = 0.6.dp,
                color = DymnLightBorder.copy(alpha = 0.42f),
                shape = RoundedCornerShape(22.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = lt("Аларми й нагадування", "Alarms and reminders"),
                color = DymnText,
                style = dymnText(MaterialTheme.typography.labelLarge),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = DymnText.copy(alpha = 0.76f),
                style = dymnText(MaterialTheme.typography.bodySmall),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CustomTileFillDialog(
    initialColor: Color,
    tileColor: Color,
    onDismiss: () -> Unit,
    onSave: (Color) -> Unit,
) {
    var red by remember { mutableStateOf((initialColor.red * 255f).toInt().coerceIn(0, 255).toFloat()) }
    var green by remember { mutableStateOf((initialColor.green * 255f).toInt().coerceIn(0, 255).toFloat()) }
    var blue by remember { mutableStateOf((initialColor.blue * 255f).toInt().coerceIn(0, 255).toFloat()) }
    val preview = Color(red.toInt(), green.toInt(), blue.toInt())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tileColor,
        shape = RoundedCornerShape(32.dp),
        title = { Text(lt("Своя заливка", "Custom fill"), color = DymnText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(preview)
                        .border(0.6.dp, DymnLightBorder.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                )
                ColorSlider(label = "R", value = red, color = Color(0xFFFF6B6B), onChange = { red = it })
                ColorSlider(label = "G", value = green, color = Color(0xFF51CF66), onChange = { green = it })
                ColorSlider(label = "B", value = blue, color = Color(0xFF4DABF7), onChange = { blue = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(preview) }) {
                Text(lt("Вибрати", "Select"), color = DymnCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(lt("Скасувати", "Cancel"), color = DymnRed)
            }
        }
    )
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    color: Color,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = color, style = dymnText(MaterialTheme.typography.labelLarge))
            Text(value.toInt().toString(), color = DymnMuted, style = dymnText(MaterialTheme.typography.labelSmall))
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..255f,
            steps = 254,
        )
    }
}

@Composable
private fun rememberUriImage(uri: String): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        bitmap = if (uri.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }

    return bitmap
}

private fun BabyEntry.durationUntil(now: LocalDateTime): Duration {
    return Duration.between(start, end ?: now)
}

private fun BabyEntry.timeLabel(): String {
    val started = start.format(ShortTimeFormatter)
    val finishedAt = end
    val finished = finishedAt?.format(ShortTimeFormatter)
    
    val isIntervalType = type == BabyEventType.Sleep || type == BabyEventType.Active || type == BabyEventType.Walk
    
    return if (!isIntervalType) {
        started
    } else if (finished == null) {
        "$started - ${dymnText(runtimeDymnLanguage, "триває", "active")}"
    } else {
        val duration = durationUntil(finishedAt)
        if (duration.toMinutes() <= 0) started 
        else "$started - $finished, ${formatDuration(duration)}"
    }
}

private fun BabyEntry.durationMinutesOn(date: LocalDate, now: LocalDateTime): Long {
    val startOfDay = date.atStartOfDay()
    val startOfNextDay = date.plusDays(1).atStartOfDay()
    val effectiveStart = if (start.isBefore(startOfDay)) startOfDay else start
    val effectiveEnd = (end ?: now).let { if (it.isAfter(startOfNextDay)) startOfNextDay else it }
    if (effectiveStart.isAfter(effectiveEnd)) return 0
    return Duration.between(effectiveStart, effectiveEnd).toMinutes().coerceAtLeast(0)
}

private fun List<BabyEntry>.sumDurationMinutesOn(date: LocalDate, now: LocalDateTime): Long {
    return sumOf { it.durationMinutesOn(date, now) }
}

private fun List<BabyEntry>.today(): List<BabyEntry> {
    val today = LocalDate.now()
    // Включаємо також події, які почалися раніше, але тривають сьогодні
    return filter { 
        it.occursOn(today)
    }
}

private fun List<BabyEntry>.onDate(date: LocalDate): List<BabyEntry> {
    return filter { it.occursOn(date) }
}

private fun BabyEntry.occursOn(date: LocalDate): Boolean {
    if (!type.supportsDuration) return start.toLocalDate() == date
    return durationMinutesOn(date, LocalDateTime.now()) > 0
}

private fun List<BabyEntry>.sumDurationMinutes(): Long {
    val now = LocalDateTime.now()
    val today = LocalDate.now()
    return sumDurationMinutesOn(today, now)
}

private inline fun <T> Collection<T>.averageOfOrZero(selector: (T) -> Double): Double {
    return if (isEmpty()) 0.0 else sumOf(selector) / size
}

private fun formatMonitorAverage(value: Double): String {
    return String.format(UaLocale, "%.1f", value).trimEnd('0').trimEnd(',', '.')
}

private fun normalizeActivityIntervals(entries: List<BabyEntry>): List<BabyEntry> {
    val nonActivityEntries = entries.filterNot { it.type == BabyEventType.Active }
    val sleeps = nonActivityEntries
        .filter { it.type == BabyEventType.Sleep }
        .sortedBy { it.start }
    
    if (sleeps.isEmpty()) return nonActivityEntries

    val activityEntries = mutableListOf<BabyEntry>()
    
    // 1. Період до першого сну (якщо він був зафіксований раніше)
    // В даній логіці ми вважаємо активність вторинною від сну, 
    // але якщо потрібно враховувати і початок дня, можна додати логіку тут.

    // 2. Періоди між снами та після останнього сну
    sleeps.forEachIndexed { index, sleep ->
        val activeStart = sleep.end ?: return@forEachIndexed
        val nextSleepStart = sleeps.getOrNull(index + 1)?.start
        
        if (nextSleepStart != null && !nextSleepStart.isAfter(activeStart)) return@forEachIndexed

        activityEntries.add(
            BabyEntry(
                id = buildActivityIntervalId(activeStart, nextSleepStart),
                type = BabyEventType.Active,
                start = activeStart,
                end = nextSleepStart,
            )
        )
    }

    return nonActivityEntries + activityEntries
}

private fun buildActivityIntervalId(start: LocalDateTime, end: LocalDateTime?): String {
    val startPart = start.format(IdFormatter)
    val endPart = end?.format(IdFormatter) ?: "ongoing"
    return "active_${startPart}_$endPart"
}

private fun formatMinutes(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    val hour = dymnText(runtimeDymnLanguage, "г", "h")
    val minute = dymnText(runtimeDymnLanguage, "хв", "m")
    return when {
        hours > 0 && mins > 0 -> "${hours}$hour ${mins}$minute"
        hours > 0 -> "${hours}$hour"
        else -> "${mins}$minute"
    }
}
private fun formatDuration(duration: Duration): String {
    return formatMinutes(duration.toMinutes().coerceAtLeast(0))
}

private fun monitorMonthLabel(monthIdx: Int): String {
    return if (monthIdx == 0) dymnText(runtimeDymnLanguage, "Місяць народження", "Birth month") else dymnText(runtimeDymnLanguage, "$monthIdx-й місяць", "Month $monthIdx")
}

private fun monitorMonthIndexForEntry(birthDate: LocalDate, entry: BabyEntry): Int {
    return if (entry.type == BabyEventType.Measurement) {
        completedMonthIndexFor(birthDate, entry.start.toLocalDate())
    } else {
        currentLifeMonthIndexFor(birthDate, entry.start.toLocalDate())
    }
}

private fun completedMonthIndexFor(birthDate: LocalDate, entryDate: LocalDate): Int {
    if (entryDate.isBefore(birthDate)) return -1
    val period = Period.between(birthDate, entryDate)
    return period.years * 12 + period.months
}

private fun currentLifeMonthIndexFor(birthDate: LocalDate, entryDate: LocalDate): Int {
    if (entryDate.isBefore(birthDate)) return -1
    if (entryDate.isEqual(birthDate)) return 0
    return completedMonthIndexFor(birthDate, entryDate) + 1
}

private fun loadBabyEntries(context: Context, childId: String): List<BabyEntry> {
    val raw = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        .getString(EntriesKey + "_" + childId, "")
        .orEmpty()

    return raw.lineSequence()
        .mapNotNull { line ->
            val parts = line.split("|", limit = 6)
            val type = BabyEventType.entries.firstOrNull { it.storageKey == parts.getOrNull(1) }
            val start = parts.getOrNull(2)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
            if (parts.size < 5 || type == null || start == null) return@mapNotNull null
            BabyEntry(
                id = parts[0],
                type = type,
                start = start,
                end = parts[3].takeIf { it.isNotBlank() }?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
                note = parts[4].decodeStorageText().withoutStoredDoneFlag(),
                isDone = parts.getOrNull(5) == "1",
            )
        }
        .sortedByDescending { it.start }
        .toList()
}

private fun saveBabyEntries(context: Context, entries: List<BabyEntry>, childId: String) {
    val encoded = entries.joinToString(separator = "\n") { entry ->
        listOf(
            entry.id,
            entry.type.storageKey,
            entry.start.toString(),
            entry.end?.toString().orEmpty(),
            entry.note.encodeStorageText(),
            if (entry.isDone) "1" else "0",
        ).joinToString("|")
    }
    context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE).edit {
        putString(EntriesKey + "_" + childId, encoded)
    }
    BabySummaryWidgetProvider.updateAll(context)
}

private fun loadBabyProfiles(context: Context): List<BabyProfile> {
    val preferences = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
    val raw = preferences.getString("profiles_list", "").orEmpty()
    if (raw.isBlank()) return emptyList()
    
    return raw.split("\n").mapNotNull { id ->
        if (id.isBlank()) return@mapNotNull null
        BabyProfile(
            id = id,
            name = preferences.getString(ProfileNameKey + "_" + id, "").orEmpty(),
            birthDate = preferences.getString(ProfileBirthDateKey + "_" + id, "").orEmpty(),
            weight = preferences.getString(ProfileWeightKey + "_" + id, "").orEmpty(),
            height = preferences.getString(ProfileHeightKey + "_" + id, "").orEmpty(),
            photoUri = preferences.getString(ProfilePhotoUriKey + "_" + id, "").orEmpty(),
            photoScale = preferences.getFloat(ProfilePhotoScaleKey + "_" + id, 1f).coerceIn(1f, 3f),
            photoRotation = normalizeRotation(preferences.getFloat(ProfilePhotoRotationKey + "_" + id, 0f)),
            photoOffsetX = preferences.getFloat(ProfilePhotoOffsetXKey + "_" + id, 0f).coerceIn(-2f, 2f),
            photoOffsetY = preferences.getFloat(ProfilePhotoOffsetYKey + "_" + id, 0f).coerceIn(-2f, 2f),
        )
    }
}

private fun saveBabyProfiles(context: Context, profiles: List<BabyProfile>) {
    context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE).edit {
        putString("profiles_list", profiles.joinToString("\n") { it.id })
        profiles.forEach { profile ->
            val id = profile.id
            putString(ProfileNameKey + "_" + id, profile.name)
            putString(ProfileBirthDateKey + "_" + id, profile.birthDate)
            putString(ProfileWeightKey + "_" + id, profile.weight)
            putString(ProfileHeightKey + "_" + id, profile.height)
            putString(ProfilePhotoUriKey + "_" + id, profile.photoUri)
            putFloat(ProfilePhotoScaleKey + "_" + id, profile.photoScale.coerceIn(1f, 3f))
            putFloat(ProfilePhotoRotationKey + "_" + id, normalizeRotation(profile.photoRotation))
            putFloat(ProfilePhotoOffsetXKey + "_" + id, profile.photoOffsetX.coerceIn(-2f, 2f))
            putFloat(ProfilePhotoOffsetYKey + "_" + id, profile.photoOffsetY.coerceIn(-2f, 2f))
        }
    }
    BabySummaryWidgetProvider.updateAll(context)
}

private fun loadSelectedProfileId(context: Context): String? {
    return context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        .getString(SelectedProfileKey, null)
        ?.takeIf { it.isNotBlank() }
}

private fun saveSelectedProfileId(context: Context, id: String?) {
    context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE).edit {
        if (id.isNullOrBlank()) {
            remove(SelectedProfileKey)
        } else {
            putString(SelectedProfileKey, id)
        }
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

private fun String.withoutStoredDoneFlag(): String {
    return removeSuffix("|0").removeSuffix("|1")
}

private fun dymnText(base: TextStyle): TextStyle {
    return base.copy(platformStyle = PlatformTextStyle(includeFontPadding = false))
}

private fun normalizeRotation(value: Float): Float {
    val normalized = value % 360f
    return if (normalized < 0f) normalized + 360f else normalized
}

private fun parseBirthDateTime(value: String): LocalDateTime? {
    val normalized = value.trim().replace('T', ' ')
    if (normalized.isBlank()) return null

    val formatters = listOf(
        BirthDateTimeFormatter,
        BirthDateFormatter,
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", UaLocale),
        DateTimeFormatter.ofPattern("dd.MM.yyyy", UaLocale),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", UaLocale),
        DateTimeFormatter.ofPattern("yyyy/MM/dd", UaLocale)
    )

    for (formatter in formatters) {
        runCatching {
            return if (formatter.toString().contains("HH:mm")) {
                LocalDateTime.parse(normalized, formatter)
            } else {
                LocalDate.parse(normalized, formatter).atStartOfDay()
            }
        }
    }
    return null
}

private data class BabyProfile(
    val id: String = "default",
    val name: String = "",
    val birthDate: String = "",
    val weight: String = "",
    val height: String = "",
    val photoUri: String = "",
    val photoScale: Float = 1f,
    val photoRotation: Float = 0f,
    val photoOffsetX: Float = 0f,
    val photoOffsetY: Float = 0f,
) {
    val displayName: String
        get() = name.ifBlank { dymnText(runtimeDymnLanguage, "Малюк", "Baby") }

    val initials: String
        get() = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "B"

    fun ageLabel(now: LocalDateTime = LocalDateTime.now()): String {
        val birth = parseBirthDateTime(birthDate) ?: return dymnText(runtimeDymnLanguage, "Вік не вказано", "Age not set")
        val birthDateOnly = birth.toLocalDate()
        val nowDateOnly = now.toLocalDate()
        
        if (birthDateOnly.isAfter(nowDateOnly)) return dymnText(runtimeDymnLanguage, "Дата народження у майбутньому", "Birth date is in the future")

        val period = Period.between(birthDateOnly, nowDateOnly)

        return buildList {
            if (period.years > 0) add("${period.years} ${dymnText(runtimeDymnLanguage, "р.", "y")}")
            if (period.months > 0) add("${period.months} ${dymnText(runtimeDymnLanguage, "міс.", "mo")}")
            if (period.days > 0 || (period.years == 0 && period.months == 0)) {
                add("${period.days} ${dymnText(runtimeDymnLanguage, "дн.", "d")}")
            }
        }.joinToString(" ")
    }

    fun birthdayCountdown(now: LocalDateTime = LocalDateTime.now()): String? {
        val birth = parseBirthDateTime(birthDate) ?: return null
        val today = now.toLocalDate()
        val birthMonthDay = birth.toLocalDate().withYear(today.year)
        
        val nextBirthday = if (birthMonthDay.isBefore(today) || birthMonthDay.isEqual(today)) {
            birthMonthDay.plusYears(1)
        } else {
            birthMonthDay
        }

        if (birthMonthDay.isEqual(today)) return dymnText(runtimeDymnLanguage, "Сьогодні день народження! 🎉", "Birthday today! 🎉")

        val period = Period.between(today, nextBirthday)
        return buildString {
            append(dymnText(runtimeDymnLanguage, "До Дня народження: ", "Until birthday: "))
            if (period.years > 0) append("${period.years} ${dymnText(runtimeDymnLanguage, "р.", "y")} ") // На випадок якщо ми рахуємо на роки вперед
            if (period.months > 0) append("${period.months} ${dymnText(runtimeDymnLanguage, "міс.", "mo")} ")
            // Завжди показуємо дні, навіть якщо їх 0, щоб було зрозуміліше
            append("${period.days} ${dymnText(runtimeDymnLanguage, "дн.", "d")}")
        }.trim()
    }
}

private data class BabyEntry(
    val id: String,
    val type: BabyEventType,
    val start: LocalDateTime,
    val end: LocalDateTime?,
    val note: String = "",
    val isDone: Boolean = false,
)

private enum class BabyEventType(
    val storageKey: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
) {
    Sleep("sleep", "Сон", EventIcons.Sleep, DymnBlue),
    Feeding("feeding", "Годування", EventIcons.Feeding, DymnCyan),
    Active("active", "Активність", EventIcons.Active, DymnGreen),
    Toilet("toilet", "Туалет", EventIcons.Toilet, DymnYellow),
    Note("note", "Нотатка", Icons.Default.Edit, DymnCyan),
    Question("question", "Запитання", Icons.Default.Info, DymnCyan),
    Measurement("measure", "Вимірювання", EventIcons.Measurement, DymnGreen),
    Walk("walk", "Прогулянка", EventIcons.Walk, DymnCyan),
    Milestone("milestone", "Подія", EventIcons.Milestone, DymnYellow),
}


@Composable
private fun BabyEventType.localizedLabel(): String = when (this) {
    BabyEventType.Sleep -> lt("Сон", "Sleep")
    BabyEventType.Feeding -> lt("Годування", "Feeding")
    BabyEventType.Active -> lt("Активність", "Activity")
    BabyEventType.Toilet -> lt("Туалет", "Toilet")
    BabyEventType.Note -> lt("Нотатка", "Note")
    BabyEventType.Question -> lt("Запитання", "Questions")
    BabyEventType.Measurement -> lt("Вимірювання", "Measurement")
    BabyEventType.Walk -> lt("Прогулянка", "Walk")
    BabyEventType.Milestone -> lt("Подія", "Event")
}

@Composable
private fun BabyPage.localizedTitle(): String = when (this) {
    BabyPage.Home -> lt("Дитина", "Child")
    BabyPage.Sleep -> lt("Сон", "Sleep")
    BabyPage.Feeding -> lt("Годування", "Feeding")
    BabyPage.Activity -> lt("Активність", "Activity")
    BabyPage.Toilet -> lt("Туалет", "Toilet")
    BabyPage.Questions -> lt("Запитання", "Questions")
    BabyPage.Monitor -> lt("Монітор", "Monitor")
    BabyPage.Events -> lt("Події", "Events")
}
private val BabyEventType.supportsDuration: Boolean
    get() = this == BabyEventType.Sleep || this == BabyEventType.Active || this == BabyEventType.Walk

private enum class BabyPage(val title: String) {
    Home("Дитина"),
    Sleep("Сон"),
    Feeding("Годування"),
    Activity("Активність"),
    Toilet("Туалет"),
    Questions("Запитання"),
    Monitor("Монітор"),
    Events("Події"),
}

private fun widgetLaunchRequestFromIntent(intent: Intent?): WidgetLaunchRequest? {
    val pageIndex = intent?.getIntExtra(ExtraWidgetPageIndex, -1)
        ?.takeIf { it >= 0 }
        ?: intent?.data?.lastPathSegment?.toIntOrNull()
        ?: return null
    val page = when (pageIndex) {
        0 -> BabyPage.Sleep
        1 -> BabyPage.Feeding
        2 -> BabyPage.Activity
        3 -> BabyPage.Toilet
        else -> return null
    }
    return WidgetLaunchRequest(page)
}

private val UaLocale = Locale("uk", "UA")
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm", UaLocale)
private val ShortTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", UaLocale)
private val IdFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS", UaLocale)
private val BirthDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", UaLocale)
private val BirthDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", UaLocale)
private const val PrefsName = "dymn_baby_tracker"
private const val EntriesKey = "entries"
private const val SelectedProfileKey = "selected_profile_id"
private const val ProfileNameKey = "profile_name"
private const val ProfileBirthDateKey = "profile_birth_date"
private const val ProfileWeightKey = "profile_weight"
private const val ProfileHeightKey = "profile_height"
private const val ProfilePhotoUriKey = "profile_photo_uri"
private const val ProfilePhotoScaleKey = "profile_photo_scale"
private const val ProfilePhotoRotationKey = "profile_photo_rotation"
private const val ProfilePhotoOffsetXKey = "profile_photo_offset_x"
private const val ProfilePhotoOffsetYKey = "profile_photo_offset_y"
private const val WallpaperUriKey = "app_wallpaper_uri"
private const val TileOpacityKey = "app_tile_opacity"
private const val TileFillKey = "app_tile_fill"
private const val CustomTileFillColorKey = "app_custom_tile_fill_color"
private const val CustomTileFillKeyValue = "custom"
private const val ActivityLimitKey = "activity_limit_minutes"
private const val ExtraWidgetPageIndex = "widget_page_index"

private data class TileFillOption(
    val key: String,
    val title: String,
    val titleEn: String,
    val color: Color,
)

private val TileFillOptions = listOf(
    TileFillOption("graphite", "Графіт", "Graphite", Color(0xFF77777C)),
    TileFillOption("charcoal", "Вугілля", "Charcoal", Color(0xFF34343A)),
    TileFillOption("mist", "Туман", "Mist", Color(0xFFA2A2A6)),
    TileFillOption("stone", "Камінь", "Stone", Color(0xFF5F625F)),
)
private fun tileFillColorFor(key: String, customColor: Color): Color {
    if (key == CustomTileFillKeyValue) return customColor
    return TileFillOptions.firstOrNull { it.key == key }?.color ?: DymnGraphite
}

private fun loadWallpaperUri(context: Context): String {
    return context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        .getString(WallpaperUriKey, "") ?: ""
}

private fun saveWallpaperUri(context: Context, uri: String) {
    context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE).edit {
        putString(WallpaperUriKey, uri)
    }
}

private fun loadActivityLimitMinutes(context: Context, profileId: String?): Int {
    if (profileId.isNullOrBlank()) return 90
    return context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        .getInt("${ActivityLimitKey}_$profileId", 90)
        .coerceIn(30, 360)
}

private fun saveActivityLimitMinutes(context: Context, profileId: String, minutes: Int) {
    context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE).edit {
        putInt("${ActivityLimitKey}_$profileId", minutes.coerceIn(30, 360))
    }
}

private fun loadTileOpacity(context: Context): Float {
    return context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        .getFloat(TileOpacityKey, 0.82f)
}

private fun saveTileOpacity(context: Context, opacity: Float) {
    context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE).edit {
        putFloat(TileOpacityKey, opacity)
    }
}

private fun loadTileFillKey(context: Context): String {
    val saved = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        .getString(TileFillKey, "graphite")
        .orEmpty()
    return saved.takeIf { key -> key == CustomTileFillKeyValue || TileFillOptions.any { it.key == key } } ?: "graphite"
}

private fun saveTileFillKey(context: Context, key: String) {
    context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE).edit {
        putString(TileFillKey, key.takeIf { value -> value == CustomTileFillKeyValue || TileFillOptions.any { it.key == value } } ?: "graphite")
    }
}

private fun loadCustomTileFillColor(context: Context): Color {
    val argb = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
        .getInt(CustomTileFillColorKey, Color(0xFF686C70).toArgb())
    return Color(argb)
}

private fun saveCustomTileFillColor(context: Context, color: Color) {
    context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE).edit {
        putInt(CustomTileFillColorKey, color.copy(alpha = 1f).toArgb())
    }
}

@Composable
private fun DymnWeightPicker(
    initialWeight: String,
    tileColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initialWeight.split(".")
    val initialKg = parts.getOrNull(0)?.toIntOrNull() ?: 3
    val initialGrams = parts.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toIntOrNull() ?: 500

    val kgItems = (0..35).map { it.toString() }
    val gramItems = (0..99).map { (it * 10).toString().padStart(3, '0') } // Крок 10г

    var kgIndex by remember { mutableStateOf(kgItems.indexOf(initialKg.toString()).coerceAtLeast(0)) }
    var gramIndex by remember { mutableStateOf((initialGrams / 10).coerceIn(0, 99)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tileColor,
        shape = RoundedCornerShape(32.dp),
        title = { Text(lt("Вага дитини", "Baby weight"), color = DymnText, style = dymnText(MaterialTheme.typography.titleLarge)) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Box(Modifier.fillMaxWidth().height(0.6.dp).background(DymnBlue.copy(alpha = 0.42f)))
                    Spacer(Modifier.height(44.dp))
                    Box(Modifier.fillMaxWidth().height(0.6.dp).background(DymnBlue.copy(alpha = 0.42f)))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPicker(items = kgItems, initialIndex = kgIndex, onSelectionChanged = { kgIndex = it }, width = 60.dp)
                    Text(lt("кг", "kg"), color = DymnText, style = dymnText(MaterialTheme.typography.titleMedium))
                    Spacer(Modifier.width(20.dp))
                    WheelPicker(items = gramItems, initialIndex = gramIndex, onSelectionChanged = { gramIndex = it }, width = 80.dp)
                    Text(lt("г", "g"), color = DymnText, style = dymnText(MaterialTheme.typography.titleMedium))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val weightStr = "${kgItems[kgIndex]}.${gramItems[gramIndex]}"
                onConfirm(weightStr)
            }) {
                Text(lt("Вибрати", "Select"), color = DymnCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(lt("Скасувати", "Cancel"), color = DymnRed)
            }
        }
    )
}

@Composable
private fun DymnDateTimePicker(
    initialDateTime: LocalDateTime,
    tileColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit
) {
    val years = remember { (2010..LocalDateTime.now().year).map { it.toString() } }
    val months = remember { (1..12).map { 
        java.time.Month.of(it).getDisplayName(JavaTextStyle.SHORT, UaLocale).replaceFirstChar { it.uppercase() } 
    } }
    
    var yearIndex by remember { mutableStateOf(years.indexOf(initialDateTime.year.toString()).coerceAtLeast(0)) }
    var monthIndex by remember { mutableStateOf(initialDateTime.monthValue - 1) }
    var dayIndex by remember { mutableStateOf(initialDateTime.dayOfMonth - 1) }

    val daysInMonth = remember(yearIndex, monthIndex) {
        val y = years[yearIndex].toInt()
        val m = monthIndex + 1
        runCatching { java.time.YearMonth.of(y, m).lengthOfMonth() }.getOrDefault(31)
    }
    val dayItems = remember(daysInMonth) { (1..daysInMonth).map { it.toString() } }
    
    LaunchedEffect(daysInMonth) {
        if (dayIndex >= daysInMonth) dayIndex = daysInMonth - 1
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tileColor,
        shape = RoundedCornerShape(32.dp),
        title = { Text(lt("Дата народження", "Birth date"), color = DymnText, style = dymnText(MaterialTheme.typography.titleLarge)) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Box(Modifier.fillMaxWidth().height(0.6.dp).background(DymnBlue.copy(alpha = 0.42f)))
                    Spacer(Modifier.height(44.dp))
                    Box(Modifier.fillMaxWidth().height(0.6.dp).background(DymnBlue.copy(alpha = 0.42f)))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPicker(items = dayItems, initialIndex = dayIndex, onSelectionChanged = { dayIndex = it }, width = 56.dp)
                    WheelPicker(items = months, initialIndex = monthIndex, onSelectionChanged = { monthIndex = it }, width = 90.dp)
                    WheelPicker(items = years, initialIndex = yearIndex, onSelectionChanged = { yearIndex = it }, width = 70.dp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val picked = LocalDateTime.of(
                    years[yearIndex].toInt(),
                    monthIndex + 1,
                    dayIndex + 1,
                    0, 0
                )
                onConfirm(picked)
            }) {
                Text(lt("Вибрати", "Select"), color = DymnCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(lt("Скасувати", "Cancel"), color = DymnRed)
            }
        }
    )
}

@Composable
private fun EditEntryDialog(
    entry: BabyEntry,
    tileColor: Color,
    onDismiss: () -> Unit,
    onSave: (BabyEntry) -> Unit,
) {
    val supportsDuration = entry.type.supportsDuration
    var startTime by remember(entry.id) { mutableStateOf(entry.start.toLocalTime()) }
    var endTime by remember(entry.id) { mutableStateOf((entry.end ?: LocalDateTime.now()).toLocalTime()) }
    var isOngoing by remember(entry.id) { mutableStateOf(supportsDuration && entry.end == null) }
    var isPickingStart by remember(entry.id) { mutableStateOf(true) }
    var note by remember(entry.id) { mutableStateOf(entry.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tileColor,
        shape = RoundedCornerShape(32.dp),
        title = {
            Text(
                text = dymnText(LocalDymnLanguage.current, "Редагувати", "Edit") + ": ${entry.type.localizedLabel()}",
                color = DymnText,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (supportsDuration) {
                    ManualTimeField(label = lt("Початок", "Start"), time = startTime) {
                        isPickingStart = true
                    }
                    OngoingToggle(
                        selected = isOngoing,
                        tileColor = tileColor,
                        onClick = {
                            isOngoing = !isOngoing
                            if (isOngoing) isPickingStart = true
                        },
                    )
                    if (!isOngoing) {
                        ManualTimeField(label = lt("Кінець", "End"), time = endTime) {
                            isPickingStart = false
                        }
                    }
                } else {
                    ManualTimeField(label = lt("Час події", "Event time"), time = startTime) {
                        isPickingStart = true
                    }
                }

                if (entry.type == BabyEventType.Note ||
                    entry.type == BabyEventType.Milestone ||
                    entry.type == BabyEventType.Measurement
                ) {
                    DymnTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = lt("Нотатка", "Note"),
                        minLines = 2,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column {
                        Box(Modifier.fillMaxWidth().height(0.6.dp).background(DymnBlue.copy(alpha = 0.42f)))
                        Spacer(Modifier.height(44.dp))
                        Box(Modifier.fillMaxWidth().height(0.6.dp).background(DymnBlue.copy(alpha = 0.42f)))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val hours = (0..23).map { it.toString().padStart(2, '0') }
                        val minutes = (0..59).map { it.toString().padStart(2, '0') }
                        val selectedTime = if (isPickingStart || isOngoing) startTime else endTime

                        WheelPicker(
                            items = hours,
                            initialIndex = selectedTime.hour,
                            onSelectionChanged = { h ->
                                if (isPickingStart || isOngoing) startTime = startTime.withHour(h)
                                else endTime = endTime.withHour(h)
                            },
                            width = 50.dp,
                        )
                        Text(":", color = DymnText, style = dymnText(MaterialTheme.typography.titleLarge))
                        WheelPicker(
                            items = minutes,
                            initialIndex = selectedTime.minute,
                            onSelectionChanged = { m ->
                                if (isPickingStart || isOngoing) startTime = startTime.withMinute(m)
                                else endTime = endTime.withMinute(m)
                            },
                            width = 50.dp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val start = LocalDateTime.of(entry.start.toLocalDate(), startTime)
                    val end = when {
                        !supportsDuration -> entry.end?.let { start }
                        isOngoing -> null
                        else -> {
                            val sameDayEnd = LocalDateTime.of((entry.end ?: entry.start).toLocalDate(), endTime)
                            if (sameDayEnd.isBefore(start)) sameDayEnd.plusDays(1) else sameDayEnd
                        }
                    }
                    onSave(entry.copy(start = start, end = end, note = note.trim()))
                },
            ) {
                Text(lt("Зберегти", "Save"), color = DymnCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(lt("Скасувати", "Cancel"), color = DymnRed)
            }
        },
    )
}

@Composable
private fun ManualEntryDialog(
    type: BabyEventType,
    tileColor: Color,
    onDismiss: () -> Unit,
    onSave: (LocalDateTime, LocalDateTime?) -> Unit
) {
    val today = LocalDate.now()
    var startTime by remember { mutableStateOf(java.time.LocalTime.now()) }
    var endTime by remember { mutableStateOf(java.time.LocalTime.now()) }
    val supportsDuration = type == BabyEventType.Sleep || type == BabyEventType.Active || type == BabyEventType.Walk
    var isOngoing by remember(type) { mutableStateOf(false) }
    var isPickingStart by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tileColor,
        shape = RoundedCornerShape(32.dp),
        title = { 
            Text(
                text = when(type) {
                    BabyEventType.Sleep -> lt("Додати сон", "Add sleep")
                    BabyEventType.Feeding -> lt("Додати годування", "Add feeding")
                    BabyEventType.Active -> lt("Додати активність", "Add activity")
                    BabyEventType.Walk -> lt("Додати прогулянку", "Add walk")
                    BabyEventType.Toilet -> lt("Додати туалет", "Add toilet")
                    else -> lt("Додати подію", "Add event")
                }, 
                color = DymnText 
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (supportsDuration) {
                    ManualTimeField(label = lt("Початок", "Start"), time = startTime) {
                        isPickingStart = true
                    }
                    OngoingToggle(
                        selected = isOngoing,
                        tileColor = tileColor,
                        onClick = {
                            isOngoing = !isOngoing
                            if (isOngoing) isPickingStart = true
                        },
                    )
                    if (!isOngoing) {
                        ManualTimeField(label = lt("Кінець", "End"), time = endTime) {
                            isPickingStart = false
                        }
                    }
                } else {
                    ManualTimeField(label = lt("Час події", "Event time"), time = startTime) {
                        isPickingStart = true
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Box(Modifier.fillMaxWidth().height(0.6.dp).background(DymnBlue.copy(alpha = 0.42f)))
                        Spacer(Modifier.height(44.dp))
                        Box(Modifier.fillMaxWidth().height(0.6.dp).background(DymnBlue.copy(alpha = 0.42f)))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hours = (0..23).map { it.toString().padStart(2, '0') }
                        val minutes = (0..59).map { it.toString().padStart(2, '0') }
                        
                        WheelPicker(
                            items = hours, 
                            initialIndex = if (isPickingStart || isOngoing) startTime.hour else endTime.hour, 
                            onSelectionChanged = { h ->
                                if (isPickingStart || isOngoing) startTime = startTime.withHour(h)
                                else endTime = endTime.withHour(h)
                            }, 
                            width = 50.dp
                        )
                        Text(":", color = DymnText, style = dymnText(MaterialTheme.typography.titleLarge))
                        WheelPicker(
                            items = minutes, 
                            initialIndex = if (isPickingStart || isOngoing) startTime.minute else endTime.minute, 
                            onSelectionChanged = { m ->
                                if (isPickingStart || isOngoing) startTime = startTime.withMinute(m)
                                else endTime = endTime.withMinute(m)
                            }, 
                            width = 50.dp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val start = LocalDateTime.of(today, startTime)
                    val end = if (!supportsDuration || isOngoing) null else LocalDateTime.of(today, endTime)
                    onSave(start, end)
                }
            ) {
                Text(lt("Зберегти", "Save"), color = DymnCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(lt("Скасувати", "Cancel"), color = DymnRed)
            }
        }
    )
}

@Composable
private fun OngoingToggle(
    selected: Boolean,
    tileColor: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) DymnCyan.copy(alpha = 0.34f) else tileColor.copy(alpha = 0.38f))
            .border(
                width = 0.6.dp,
                color = if (selected) DymnCyan else DymnLightBorder.copy(alpha = 0.18f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (selected) DymnCyan else DymnGraphite.copy(alpha = 0.42f))
                .border(0.6.dp, DymnLightBorder.copy(alpha = 0.36f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lt("Ще триває", "Still active"),
                color = DymnText,
                style = dymnText(MaterialTheme.typography.labelLarge),
            )
            Text(
                text = lt("Запис без часу завершення", "Record without an end time"),
                color = DymnMuted,
                style = dymnText(MaterialTheme.typography.labelSmall),
            )
        }
    }
}

@Composable
private fun ManualTimeField(
    label: String,
    time: java.time.LocalTime,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DymnGraphite.copy(alpha = 0.2f))
            .border(0.6.dp, DymnLightBorder.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(text = label, color = DymnMuted, style = dymnText(MaterialTheme.typography.labelSmall))
        Text(
            text = time.format(DateTimeFormatter.ofPattern("HH:mm")),
            color = DymnText,
            style = dymnText(MaterialTheme.typography.titleMedium)
        )
    }
}

@Composable
private fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 64.dp
) {
    if (items.isEmpty()) return

    val itemHeight = 44.dp
    val selectedIndex = initialIndex.coerceIn(items.indices)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = state)

    LaunchedEffect(selectedIndex, items.size) {
        if (state.firstVisibleItemIndex != selectedIndex) {
            state.scrollToItem(selectedIndex)
        }
    }
     
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) {
            onSelectionChanged(state.firstVisibleItemIndex)
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * 3)
            .width(width),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = state,
            flingBehavior = snapFlingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val isSelected by remember { 
                        derivedStateOf { state.firstVisibleItemIndex == index } 
                    }
                    Text(
                        text = items[index],
                        color = if (isSelected) DymnText else DymnMuted.copy(alpha = 0.24f),
                        style = dymnText(MaterialTheme.typography.titleLarge).copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private val DymnBlack = Color(0xFF0B0B10)
private val DymnText = Color(0xFFF7F7FA)
private val DymnMuted = Color(0xFFD1D1D6)
private val DymnGraphite = Color(0xFF77777C)
private val DymnBlue = Color(0xFF0A84FF)
private val DymnCyan = Color(0xFF32ADE6)
private val DymnGreen = Color(0xFF30D158)
private val DymnYellow = Color(0xFFFF9F0A)
private val DymnRed = Color(0xFFFF453A)
private val DymnTileFill = DymnGraphite.copy(alpha = 0.82f)
private val DymnLightBorder = Color.White.copy(alpha = 0.72f)
