package com.smithware.tidypilot

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RoomPreferences
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import com.smithware.tidypilot.data.AppSettingsEntity
import com.smithware.tidypilot.data.CleaningTaskEntity
import com.smithware.tidypilot.data.RoomEntity
import com.smithware.tidypilot.data.RoomPhotoScanEntity
import com.smithware.tidypilot.data.ScanIssueEntity
import com.smithware.tidypilot.data.WorkShiftEntity
import com.smithware.tidypilot.data.unpipe
import com.smithware.tidypilot.ui.theme.Charcoal
import com.smithware.tidypilot.ui.theme.Cream
import com.smithware.tidypilot.ui.theme.MutedOrange
import com.smithware.tidypilot.ui.theme.Sage
import com.smithware.tidypilot.ui.theme.TidyPilotTheme
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: TidyPilotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            TidyPilotTheme(themeMode = state.themeMode) {
                TidyPilotApp(state, viewModel)
            }
        }
    }
}

private sealed class Route(val value: String, val label: String, val icon: @Composable () -> Unit) {
    data object Dashboard : Route("dashboard", "Today", { Icon(Icons.Default.Home, null) })
    data object Add : Route("add", "Add/Edit", { Icon(Icons.Default.Add, null) })
    data object Rooms : Route("rooms", "Rooms", { Icon(Icons.Default.RoomPreferences, null) })
    data object Reports : Route("reports", "Reports", { Icon(Icons.Default.FileDownload, null) })
    data object Settings : Route("settings", "Settings", { Icon(Icons.Default.Settings, null) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TidyPilotApp(state: TidyPilotState, viewModel: TidyPilotViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: Route.Dashboard.value
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TidyPilot", fontWeight = FontWeight.Black)
                        Text("A tidy home plan that works around your real life.", style = MaterialTheme.typography.labelSmall)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                listOf(Route.Dashboard, Route.Add, Route.Rooms, Route.Reports, Route.Settings).forEach { route ->
                    NavigationBarItem(
                        selected = current == route.value,
                        onClick = { nav.navigate(route.value) { launchSingleTop = true } },
                        icon = route.icon,
                        label = { Text(route.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = Route.Dashboard.value, modifier = Modifier.padding(padding)) {
            composable(Route.Dashboard.value) { DashboardScreen(state, viewModel, nav) }
            composable(Route.Add.value) { AddEditScreen(state, viewModel, snackbar) }
            composable(Route.Rooms.value) { RoomManagementScreen(state, viewModel, snackbar) }
            composable(Route.Reports.value) { ReportsScreen(state, nav, viewModel, snackbar) }
            composable(Route.Settings.value) { SettingsScreen(state, viewModel) }
            composable("schedule") { WorkScheduleScreen(state, viewModel, snackbar) }
            composable("scan") { RoomPhotoScanScreen(state, viewModel, nav, snackbar) }
            composable("results") { PhotoResultsScreen(state, viewModel, nav) }
            composable("detail/{type}/{id}", arguments = listOf(navArgument("type") { type = NavType.StringType }, navArgument("id") { type = NavType.StringType })) { entry ->
                DetailScreen(entry.arguments?.getString("type").orEmpty(), entry.arguments?.getString("id").orEmpty(), state, viewModel, nav)
            }
        }
    }
}

@Composable
private fun DashboardScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController) {
    val todayText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    val plan = state.todayPlan
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            HeroCard(
                eyebrow = todayText,
                title = contextCopy(plan?.workStatus ?: state.todayShift?.let { "working today" } ?: "free day", state.latestCheckIn?.energyLevel),
                body = plan?.adaptedReason ?: "Check in once and TidyPilot will build a realistic plan around your day.",
                actionText = "Scan Room",
                onAction = { nav.navigate("scan") }
            )
        }
        item {
            StudioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Work, null, tint = Sage)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Work status: ${plan?.workStatus ?: "free day"}", fontWeight = FontWeight.Black)
                        Text(state.todayShift?.let { "${it.label}: ${it.startTime} - ${it.endTime}" } ?: "No shift on the calendar.")
                    }
                    IconButton(onClick = { nav.navigate("schedule") }) { Icon(Icons.Default.CalendarMonth, "Work Schedule") }
                }
            }
        }
        item { EnergyCheckInCard(state, viewModel) }
        item {
            SectionHeader("Today’s suggested tasks", "Good enough for today beats an abandoned plan.")
        }
        if (state.suggestedTasks.isEmpty()) {
            item { EmptyState("No plan yet", "Tap reset or check in to make a gentle plan.") { viewModel.replan() } }
        } else {
            items(state.suggestedTasks, key = { it.id }) { task -> TaskRow(task, state, viewModel, nav) }
        }
        item {
            QuickActions(
                onExhausted = { viewModel.replan(exhausted = true) },
                onTen = { viewModel.replan(availableMinutes = 10) },
                onMore = { viewModel.replan(availableMinutes = 35, energyLevel = "high") },
                onSkip = { state.suggestedTasks.firstOrNull()?.let(viewModel::skipTask) ?: viewModel.replan() },
                onReset = { viewModel.replan() },
                onScan = { nav.navigate("scan") }
            )
        }
        item {
            ProgressGrid(
                "Tasks done today" to "${state.completedTodayCount}",
                "Tidy streak" to "${state.streak} days",
                "Weekly progress" to "${state.weeklyCompletedCount} done",
                "Room status" to "${state.averageTidyScore}/100"
            )
        }
        state.lowEnergyTask?.let { fallback ->
            item {
                StudioCard {
                    Text("Low-energy fallback", fontWeight = FontWeight.Black)
                    Text("No guilt. Let’s do the smallest useful reset.")
                    Button(onClick = { viewModel.markComplete(fallback) }) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(6.dp))
                        Text("${fallback.name} (${fallback.estimatedMinutes} min)")
                    }
                }
            }
        }
    }
}

@Composable
private fun EnergyCheckInCard(state: TidyPilotState, viewModel: TidyPilotViewModel) {
    var energy by remember(state.latestCheckIn?.id) { mutableStateOf(state.latestCheckIn?.energyLevel ?: "medium") }
    var minutes by remember(state.latestCheckIn?.id) { mutableStateOf((state.latestCheckIn?.availableMinutes ?: 15).toString()) }
    var mood by remember(state.latestCheckIn?.id) { mutableStateOf(state.latestCheckIn?.moodLabel ?: "steady") }
    var exhausted by remember(state.latestCheckIn?.id) { mutableStateOf(state.latestCheckIn?.afterWorkExhaustion ?: false) }
    StudioCard {
        Text("Energy check-in", fontWeight = FontWeight.Black)
        Text("How much energy do you actually have?")
        OptionChips(listOf("low", "medium", "high"), energy) { energy = it }
        OutlinedTextField(mood, { mood = it }, label = { Text("Mood label") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit) }, label = { Text("Available minutes") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(exhausted, { exhausted = it })
            Text("After-work exhausted")
        }
        Button(onClick = { viewModel.saveEnergy(energy, mood, minutes.toIntOrNull() ?: 15, exhausted, "Dashboard check-in") }) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(6.dp))
            Text("Update plan")
        }
    }
}

@Composable
private fun QuickActions(onExhausted: () -> Unit, onTen: () -> Unit, onMore: () -> Unit, onSkip: () -> Unit, onReset: () -> Unit, onScan: () -> Unit) {
    StudioCard {
        Text("Quick actions", fontWeight = FontWeight.Black)
        WrapButtons(
            "I’m exhausted" to onExhausted,
            "I have 10 minutes" to onTen,
            "I can do more" to onMore,
            "Skip and replan" to onSkip,
            "Scan Room" to onScan,
            "Reset the plan" to onReset
        )
    }
}

@Composable
private fun AddEditScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf("task") }
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Add/Edit", "Create cleaning tasks, rooms, and work shifts.") }
        item { OptionChips(listOf("task", "room", "shift"), mode) { mode = it } }
        item {
            when (mode) {
                "room" -> RoomForm(state, viewModel, snackbar)
                "shift" -> ShiftForm(viewModel, snackbar)
                else -> TaskForm(state, viewModel, snackbar)
            }
        }
        item {
            StudioCard {
                Text("Saved tasks", fontWeight = FontWeight.Black)
                if (state.tasks.isEmpty()) Text("No cleaning tasks yet.")
                state.tasks.take(8).forEach { task ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(task.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { viewModel.deleteTask(task); scope.launch { snackbar.showSnackbar("Task removed.") } }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskForm(state: TidyPilotState, viewModel: TidyPilotViewModel, snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var roomId by remember(state.rooms) { mutableStateOf(state.rooms.firstOrNull()?.id.orEmpty()) }
    var notes by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("normal") }
    var minutes by remember { mutableStateOf("10") }
    var energy by remember { mutableStateOf("low") }
    var frequency by remember { mutableStateOf("weekly") }
    var preferred by remember { mutableStateOf("anytime") }
    var quick by remember { mutableStateOf(false) }
    var deep by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("clutter") }
    StudioCard {
        Text("Cleaning task", fontWeight = FontWeight.Black)
        OutlinedTextField(name, { name = it }, label = { Text("Task name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Room")
        OptionChips(state.rooms.map { it.name }, state.rooms.firstOrNull { it.id == roomId }?.name.orEmpty()) { chosen -> roomId = state.rooms.firstOrNull { it.name == chosen }?.id.orEmpty() }
        OutlinedTextField(notes, { notes = it }, label = { Text("Description/notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Text("Priority")
        OptionChips(listOf("low", "normal", "high", "urgent"), priority) { priority = it }
        Text("Estimated time")
        OptionChips(listOf("2", "5", "10", "15", "30"), minutes) { minutes = it }
        Text("Energy required")
        OptionChips(listOf("low", "medium", "high"), energy) { energy = it }
        Text("Frequency")
        OptionChips(listOf("one-time", "daily", "every few days", "weekly", "monthly"), frequency) { frequency = it }
        Text("Preferred time")
        OptionChips(listOf("before work", "after work", "day off", "anytime"), preferred) { preferred = it }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(quick, { quick = it }); Text("Quick reset task") }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(deep, { deep = it }); Text("Deep clean task") }
        Text("Photo-detectable category")
        OptionChips(listOf("clutter", "dishes", "trash", "laundry", "floor clutter", "surface wipe", "bed reset", "bathroom reset", "other"), category) { category = it }
        Button(onClick = {
            val error = viewModel.saveTask(null, CleaningTaskEntity(name = name, roomId = roomId, description = notes, priority = priority, estimatedMinutes = minutes.toIntOrNull() ?: 10, energyRequired = energy, frequencyType = frequency, preferredTime = preferred, isQuickResetTask = quick, isDeepCleanTask = deep, photoDetectableCategory = category, nextDueAt = LocalDate.now()))
            scope.launch { snackbar.showSnackbar(error ?: "Task saved.") }
            if (error == null) name = ""
        }) { Icon(Icons.Default.CheckCircle, null); Spacer(Modifier.width(6.dp)); Text("Save task") }
    }
}

@Composable
private fun RoomForm(state: TidyPilotState, viewModel: TidyPilotViewModel, snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Kitchen") }
    var priority by remember { mutableStateOf("normal") }
    var intensity by remember { mutableStateOf("medium") }
    var notes by remember { mutableStateOf("") }
    StudioCard {
        Text("Room", fontWeight = FontWeight.Black)
        OutlinedTextField(name, { name = it }, label = { Text("Room name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(type, { type = it }, label = { Text("Room type") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Priority")
        OptionChips(listOf("low", "normal", "high", "urgent"), priority) { priority = it }
        Text("Default task intensity")
        OptionChips(listOf("low", "medium", "high"), intensity) { intensity = it }
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Button(onClick = {
            val error = viewModel.saveRoom(RoomEntity(name = name, roomType = type, priority = priority, defaultTaskIntensity = intensity, notes = notes))
            scope.launch { snackbar.showSnackbar(error ?: "Room saved.") }
            if (error == null) name = ""
        }) { Text("Save room") }
    }
}

@Composable
private fun ShiftForm(viewModel: TidyPilotViewModel, snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var start by remember { mutableStateOf("09:00") }
    var end by remember { mutableStateOf("17:30") }
    var label by remember { mutableStateOf("Work shift") }
    var exhaustion by remember { mutableStateOf("medium") }
    var notes by remember { mutableStateOf("") }
    StudioCard {
        Text("Work shift", fontWeight = FontWeight.Black)
        OutlinedTextField(date, { date = it }, label = { Text("Date YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(start, { start = it }, label = { Text("Start time HH:MM") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(end, { end = it }, label = { Text("End time HH:MM") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(label, { label = it }, label = { Text("Shift label") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Expected exhaustion")
        OptionChips(listOf("low", "medium", "high"), exhaustion) { exhaustion = it }
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Button(onClick = {
            val error = viewModel.saveShift(WorkShiftEntity(date = parseDate(date), startTime = parseTime(start), endTime = parseTime(end), label = label, expectedExhaustionLevel = exhaustion, notes = notes))
            scope.launch { snackbar.showSnackbar(error ?: "Shift saved.") }
        }) { Text("Save shift") }
    }
}

@Composable
private fun RoomManagementScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Room Management", "Prioritize rooms without turning your home into a guilt list.") }
        items(state.rooms, key = { it.id }) { room ->
            StudioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bed, null, tint = Sage)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(room.name, fontWeight = FontWeight.Black)
                        Text("${room.roomType} • ${room.priority} priority • ${room.defaultTaskIntensity} intensity")
                        LinearProgressIndicator(progress = { room.tidyScore / 100f }, modifier = Modifier.fillMaxWidth())
                        Text("Tidy score ${room.tidyScore}/100")
                    }
                    IconButton(onClick = { viewModel.deleteRoom(room); scope.launch { snackbar.showSnackbar("Room removed.") } }) { Icon(Icons.Default.Delete, "Delete room") }
                }
            }
        }
        item { RoomForm(state, viewModel, snackbar) }
    }
}

@Composable
private fun WorkScheduleScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, snackbar: SnackbarHostState) {
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Work Schedule", "TidyPilot plans around shifts and recovery time.") }
        items(state.shifts, key = { it.id }) { shift ->
            StudioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${shift.date} • ${shift.label}", fontWeight = FontWeight.Black)
                        Text("${shift.startTime} - ${shift.endTime} • expected ${shift.expectedExhaustionLevel} exhaustion")
                        if (shift.notes.isNotBlank()) Text(shift.notes)
                    }
                    IconButton(onClick = { viewModel.deleteShift(shift) }) { Icon(Icons.Default.Delete, "Delete shift") }
                }
            }
        }
        item { ShiftForm(viewModel, snackbar) }
    }
}

@Composable
private fun RoomPhotoScanScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var roomId by remember(state.rooms) { mutableStateOf(state.rooms.firstOrNull()?.id.orEmpty()) }
    var note by remember { mutableStateOf("") }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    val selectedRoom = state.rooms.firstOrNull { it.id == roomId }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val room = selectedRoom
        val uri = pendingUri
        if (ok && room != null && uri != null) {
            viewModel.analyzePhoto(room, uri, note)
            nav.navigate("results")
        } else {
            scope.launch { snackbar.showSnackbar("Photo was not saved. You can retake it.") }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            pendingUri = uri
            takePicture.launch(uri)
        } else {
            scope.launch { snackbar.showSnackbar("Camera permission is needed to take a room photo.") }
        }
    }
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Room Photo Scan", "Save a local photo and get practical cleanup suggestions.") }
        item {
            StudioCard {
                Text("Choose room type", fontWeight = FontWeight.Black)
                OptionChips(state.rooms.map { it.name }, selectedRoom?.name.orEmpty()) { chosen -> roomId = state.rooms.firstOrNull { it.name == chosen }?.id.orEmpty() }
                OutlinedTextField(note, { note = it }, label = { Text("Optional note") }, placeholder = { Text("after work mess") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Text("Analysis runs through a local service abstraction for v1. No photo is uploaded.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Open camera")
                    }
                    FilledTonalButton(onClick = {
                        val room = selectedRoom ?: return@FilledTonalButton
                        viewModel.analyzePhoto(room, Uri.parse("demo://${room.name}/${System.currentTimeMillis()}"), note.ifBlank { "manual scan" })
                        nav.navigate("results")
                    }) { Text("Run local demo scan") }
                }
            }
        }
        pendingUri?.let { uri ->
            item {
                StudioCard {
                    Text("Last photo", fontWeight = FontWeight.Black)
                    Image(rememberAsyncImagePainter(uri), null, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) { Text("Retake photo") }
                }
            }
        }
    }
}

@Composable
private fun PhotoResultsScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController) {
    val scan = state.scans.firstOrNull()
    if (scan == null) {
        EmptyState("No scan results yet", "Scan a room to create photo-based cleanup suggestions.") { nav.navigate("scan") }
        return
    }
    val room = state.rooms.firstOrNull { it.id == scan.roomId }
    val issues = state.issues.filter { it.scanId == scan.id }
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Photo Analysis Results", room?.name ?: "Room scanned") }
        item {
            StudioCard {
                if (scan.imageUri.startsWith("content://")) {
                    Image(rememberAsyncImagePainter(Uri.parse(scan.imageUri)), null, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                }
                Text("Tidy score ${scan.tidyScore}/100 • mess score ${scan.messScore}/100", fontWeight = FontWeight.Black)
                LinearProgressIndicator(progress = { scan.tidyScore / 100f }, modifier = Modifier.fillMaxWidth())
                Text(scan.confidenceSummary)
                Text("Detected issues: ${scan.detectedIssueTags.unpipe().joinToString(", ")}")
            }
        }
        item { SectionHeader("Suggested actions", "${scan.estimatedCleanupMinutes} minutes total • ${energyRecommendation(issues)} energy recommended") }
        items(issues, key = { it.id }) { issue ->
            StudioCard {
                Text(issue.suggestedAction, fontWeight = FontWeight.Black)
                Text("${issue.label} • ${issue.estimatedMinutes} min • ${issue.energyLevel} energy • ${(issue.confidence * 100).toInt()}% rough confidence")
            }
        }
        item {
            StudioCard {
                WrapButtons(
                    "Make this today’s plan" to { viewModel.addTasksFromScan(scan); viewModel.replan(); nav.navigate(Route.Dashboard.value) },
                    "Add selected tasks" to { viewModel.addTasksFromScan(scan) },
                    "Save for later" to { nav.navigate(Route.Dashboard.value) },
                    "Compare before/after" to { nav.navigate("scan") }
                )
                Text("Was the analysis helpful?", fontWeight = FontWeight.Bold)
                WrapButtons(
                    "Accurate" to { viewModel.setScanFeedback(scan.id, "accurate") },
                    "Partly accurate" to { viewModel.setScanFeedback(scan.id, "partly accurate") },
                    "Inaccurate" to { viewModel.setScanFeedback(scan.id, "inaccurate") }
                )
            }
        }
    }
}

@Composable
private fun DetailScreen(type: String, id: String, state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController) {
    val task = state.tasks.firstOrNull { it.id == id }
    val scan = state.scans.firstOrNull { it.id == id }
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            type == "task" && task != null -> {
                item { SectionHeader(task.name, state.rooms.firstOrNull { it.id == task.roomId }?.name ?: "Room") }
                item {
                    StudioCard {
                        Text("${task.estimatedMinutes} min • ${task.energyRequired} energy • ${task.frequencyType}", fontWeight = FontWeight.Black)
                        Text("Priority: ${task.priority} • preferred ${task.preferredTime}")
                        Text("Last completed: ${task.lastCompletedAt ?: "not yet"}")
                        Text("Next due: ${task.nextDueAt ?: "done"}")
                        Text("Completion history: ${state.completions.count { it.taskId == task.id }} completed")
                        if (task.skippedCount > 1) Text("Recovery suggestion: break this into a smaller first step.")
                        if (task.description.isNotBlank()) Text(task.description)
                        WrapButtons(
                            "Mark complete" to { viewModel.markComplete(task); nav.navigate(Route.Dashboard.value) },
                            "Snooze" to { viewModel.snoozeTask(task) },
                            "Edit" to { nav.navigate(Route.Add.value) },
                            "Delete" to { viewModel.deleteTask(task); nav.navigate(Route.Dashboard.value) }
                        )
                    }
                }
            }
            type == "scan" && scan != null -> {
                item { SectionHeader("Photo scan detail", scan.scanDate.format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))) }
                item {
                    StudioCard {
                        Text("Detected issues: ${scan.detectedIssueTags.unpipe().joinToString(", ")}")
                        Text("Suggested quick reset: ${state.issues.firstOrNull { it.scanId == scan.id && it.energyLevel == "low" }?.suggestedAction ?: "Set a 5-minute timer"}")
                        Text("Suggested deep reset: ${state.issues.filter { it.scanId == scan.id }.maxByOrNull { it.estimatedMinutes }?.suggestedAction ?: "Reset the room"}")
                        Text("Feedback: ${scan.userFeedback.ifBlank { "not rated" }}")
                        Button(onClick = { viewModel.addTasksFromScan(scan) }) { Text("Add tasks from scan") }
                    }
                }
            }
            else -> item { EmptyState("Detail not found", "Return to today’s plan.") { nav.navigate(Route.Dashboard.value) } }
        }
    }
}

@Composable
private fun ReportsScreen(state: TidyPilotState, nav: NavHostController, viewModel: TidyPilotViewModel, snackbar: SnackbarHostState) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val report = buildReport(state)
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Export/Reports", "Progress summaries stay local and export as plain text.") }
        item {
            ProgressGrid(
                "Weekly cleaning" to "${state.weeklyCompletedCount} completions",
                "Most consistent room" to (mostConsistentRoom(state) ?: "Not enough data"),
                "Commonly skipped" to (state.tasks.maxByOrNull { it.skippedCount }?.name ?: "None"),
                "Scan resets" to "${state.scans.size}"
            )
        }
        item {
            StudioCard {
                Text("Energy vs cleaning progress", fontWeight = FontWeight.Black)
                Text("${state.latestCheckIn?.energyLevel ?: "medium"} energy with ${state.completedTodayCount} tasks completed today.")
                Text("Workday vs day-off progress: ${state.completions.count()} total completions, tracked locally.")
                Text("Scan-based improvement trends: average tidy score ${state.averageTidyScore}/100.")
                Text("Before/after room reset counts: ${state.scans.count { it.userFeedback == "accurate" || it.userFeedback == "partly accurate" }} helpful scan results.")
            }
        }
        item {
            StudioCard {
                Text("Plain-text report", fontWeight = FontWeight.Black)
                Text(report)
                Button(onClick = {
                    clipboard.setText(AnnotatedString(report))
                    scope.launch { snackbar.showSnackbar("Report copied.") }
                }) { Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Export report as plain text") }
            }
        }
        item {
            Button(onClick = { nav.navigate("results") }) { Text("Open latest scan result") }
        }
    }
}

@Composable
private fun SettingsScreen(state: TidyPilotState, viewModel: TidyPilotViewModel) {
    var intensity by remember(state.settings) { mutableStateOf(state.settings.defaultCleaningIntensity) }
    var recovery by remember(state.settings) { mutableStateOf(state.settings.defaultRecoveryMinutesAfterWork.toString()) }
    var reminders by remember(state.remindersEnabled) { mutableStateOf(state.remindersEnabled) }
    var reminderTime by remember(state.settings) { mutableStateOf(state.settings.preferredReminderTime) }
    var minimum by remember(state.settings) { mutableStateOf(state.settings.minimumExhaustedTaskMinutes.toString()) }
    var savePhotos by remember(state.savePhotosLocally) { mutableStateOf(state.savePhotosLocally) }
    var theme by remember(state.themeMode) { mutableStateOf(state.themeMode) }
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Settings", "Local preferences, reminders, privacy, and about.") }
        item {
            StudioCard {
                Text("Default cleaning intensity")
                OptionChips(listOf("gentle", "balanced", "deep"), intensity) { intensity = it }
                OutlinedTextField(recovery, { recovery = it.filter(Char::isDigit) }, label = { Text("Default work recovery minutes after shift") }, modifier = Modifier.fillMaxWidth())
                PreferenceRow("Energy check-in reminders", "Quick reset time? How’s your energy after work?", reminders) { reminders = it }
                OutlinedTextField(reminderTime, { reminderTime = it }, label = { Text("Cleaning reminder time") }, modifier = Modifier.fillMaxWidth())
                Text("Day-off cleaning preference")
                Text("Good day for a deeper reset if you feel up to it.")
                OutlinedTextField(minimum, { minimum = it.filter(Char::isDigit) }, label = { Text("Minimum task size for exhausted days") }, modifier = Modifier.fillMaxWidth())
                PreferenceRow("Save photos locally", "Room photos stay on this device.", savePhotos) { savePhotos = it }
                Text("Theme")
                OptionChips(listOf("system", "light", "dark"), theme) { theme = it }
                Button(onClick = {
                    viewModel.updateSettings(
                        AppSettingsEntity(defaultCleaningIntensity = intensity, defaultRecoveryMinutesAfterWork = recovery.toIntOrNull() ?: 45, reminderEnabled = reminders, preferredReminderTime = reminderTime, minimumExhaustedTaskMinutes = minimum.toIntOrNull() ?: 5, savePhotosLocally = savePhotos, themeMode = theme),
                        theme,
                        reminders,
                        savePhotos
                    )
                }) { Text("Save settings") }
            }
        }
        item {
            StudioCard {
                Text("Privacy note", fontWeight = FontWeight.Black)
                Text("All cleaning plans, work schedules, energy check-ins, and room photos stay on device unless you explicitly approve network upload later. No account. No cloud. No tracking.")
                Text("Camera/photo scan permissions are used only for local room scans.")
            }
        }
        item {
            StudioCard {
                Text("About Smithware Studios", fontWeight = FontWeight.Black)
                Text("TidyPilot is a calm local-first home reset planner for real-life energy, shifts, skipped tasks, and small wins.")
                FilledTonalButton(onClick = { viewModel.resetDemoData() }) { Icon(Icons.Default.RestartAlt, null); Spacer(Modifier.width(6.dp)); Text("Reset demo data") }
            }
        }
    }
}

@Composable
private fun TaskRow(task: CleaningTaskEntity, state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController) {
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CleaningServices, null, tint = MutedOrange)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(task.name, fontWeight = FontWeight.Black)
                Text("${state.rooms.firstOrNull { it.id == task.roomId }?.name ?: "Room"} • ${task.estimatedMinutes} min • ${task.energyRequired} energy")
                if (task.skippedCount > 1) Text("Try a smaller first step today.", color = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = { viewModel.markComplete(task) }) { Icon(Icons.Default.CheckCircle, "Mark complete") }
            IconButton(onClick = { viewModel.skipTask(task) }) { Icon(Icons.Default.SkipNext, "Skip") }
            IconButton(onClick = { nav.navigate("detail/task/${task.id}") }) { Icon(Icons.Default.Edit, "Detail") }
        }
    }
}

@Composable
private fun HeroCard(eyebrow: String, title: String, body: String, actionText: String, onAction: () -> Unit) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Charcoal), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.background(Brush.linearGradient(listOf(Charcoal, Charcoal, Sage.copy(alpha = 0.8f)))).padding(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(eyebrow, color = MutedOrange, fontWeight = FontWeight.Bold)
                Text(title, color = Cream, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(body, color = Cream)
                Button(onClick = onAction) { Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(6.dp)); Text(actionText) }
            }
        }
    }
}

@Composable
private fun StudioCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState(title: String, body: String, action: () -> Unit) {
    StudioCard {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(body)
        Button(onClick = action) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Start") }
    }
}

@Composable
private fun OptionChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.take(4).forEach { option ->
            FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) })
        }
    }
    if (options.size > 4) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            options.drop(4).take(5).forEach { option ->
                FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) })
            }
        }
    }
}

@Composable
private fun WrapButtons(vararg actions: Pair<String, () -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.toList().chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, action) ->
                    FilledTonalButton(onClick = action, modifier = Modifier.weight(1f)) { Text(label) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProgressGrid(vararg items: Pair<String, String>) {
    StudioCard {
        items.toList().chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    Column(Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(10.dp)) {
                        Text(value, fontWeight = FontWeight.Black)
                        Text(label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked, onChange)
    }
}

@Composable
private fun Modifier.tidyBackground(): Modifier =
    this.background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))))

private fun contextCopy(workStatus: String, energy: String?): String = when {
    energy == "low" -> "No guilt. Let's do the smallest useful reset."
    workStatus == "after work" -> "How much energy do you actually have?"
    workStatus == "day off" || workStatus == "free day" -> "Good day for a deeper reset if you feel up to it."
    workStatus == "working today" || workStatus == "before work" -> "Let's keep it light around your shift."
    else -> "Small reset. Good enough for today."
}

private fun energyRecommendation(issues: List<ScanIssueEntity>): String = when {
    issues.any { it.energyLevel == "high" } -> "high"
    issues.any { it.energyLevel == "medium" } -> "medium"
    else -> "low"
}

private fun mostConsistentRoom(state: TidyPilotState): String? {
    val roomCounts = state.completions.mapNotNull { done ->
        state.tasks.firstOrNull { it.id == done.taskId }?.roomId
    }.groupingBy { it }.eachCount()
    val id = roomCounts.maxByOrNull { it.value }?.key ?: return null
    return state.rooms.firstOrNull { it.id == id }?.name
}

private fun buildReport(state: TidyPilotState): String = """
    TidyPilot weekly report
    Date: ${LocalDate.now()}
    Completed tasks this week: ${state.weeklyCompletedCount}
    Tasks done today: ${state.completedTodayCount}
    Tidy streak: ${state.streak} days
    Average room tidy score: ${state.averageTidyScore}/100
    Most consistent room: ${mostConsistentRoom(state) ?: "Not enough data"}
    Tasks commonly skipped: ${state.tasks.sortedByDescending { it.skippedCount }.take(3).joinToString { it.name.ifBlank { "Task" } }}
    Energy vs cleaning progress: ${state.latestCheckIn?.energyLevel ?: "medium"} energy, ${state.latestCheckIn?.availableMinutes ?: 15} minutes available.
    Workday vs day-off cleaning progress: ${state.todayPlan?.workStatus ?: "free day"} plan type ${state.todayPlan?.planType ?: "adaptive"}.
    Scan-based improvement trends: ${state.scans.size} scans, latest mess score ${state.scans.firstOrNull()?.messScore ?: 0}.
    Before/after room reset counts: ${state.scans.count { it.userFeedback.isNotBlank() }} scan feedback entries.
""".trimIndent()

private fun createImageUri(context: Context): Uri {
    val dir = File(context.filesDir, "room_scans").apply { mkdirs() }
    val file = File(dir, "tidypilot_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
