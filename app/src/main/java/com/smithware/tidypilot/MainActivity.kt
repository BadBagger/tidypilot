package com.smithware.tidypilot

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
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
import com.smithware.tidypilot.data.PlanningEngine
import com.smithware.tidypilot.data.RoomEntity
import com.smithware.tidypilot.data.RoomPhotoScanEntity
import com.smithware.tidypilot.data.ScheduleImportCandidate
import com.smithware.tidypilot.data.ScheduleImportParser
import com.smithware.tidypilot.data.ScanIssueEntity
import com.smithware.tidypilot.data.WorkShiftEntity
import com.smithware.tidypilot.data.calculateRoomScore
import com.smithware.tidypilot.data.unpipe
import com.smithware.tidypilot.ui.theme.Charcoal
import com.smithware.tidypilot.ui.theme.Cream
import com.smithware.tidypilot.ui.theme.Graphite
import com.smithware.tidypilot.ui.theme.MutedOrange
import com.smithware.tidypilot.ui.theme.Sage
import com.smithware.tidypilot.ui.theme.TidyPilotTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.time.Duration
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
    val topLevelRoutes = listOf(Route.Dashboard.value, Route.Add.value, Route.Rooms.value, Route.Reports.value, Route.Settings.value)
    val showBack = current !in topLevelRoutes
    if (!state.onboardingComplete) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            OnboardingScreen(state, viewModel, snackbar, Modifier.padding(padding))
        }
        return
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        BrandMark(Modifier.size(34.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("TidyPilot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Text(
                                "Calm home control",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = {
                            if (!nav.popBackStack()) nav.navigate(Route.Dashboard.value) { launchSingleTop = true }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
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
                        onClick = {
                            nav.navigate(route.value) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(Route.Dashboard.value) { saveState = true }
                            }
                        },
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
            composable("editTask/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                AddEditScreen(state, viewModel, snackbar, initialTaskId = entry.arguments?.getString("id"))
            }
            composable(Route.Rooms.value) { RoomManagementScreen(state, viewModel, snackbar, nav) }
            composable(Route.Reports.value) { ReportsScreen(state, nav, viewModel, snackbar) }
            composable(Route.Settings.value) { SettingsScreen(state, viewModel) }
            composable("schedule") { WorkScheduleScreen(state, viewModel, snackbar) }
            composable("energy") { EnergyCheckInScreen(state, viewModel, nav, snackbar) }
            composable("scan") { RoomPhotoScanScreen(state, viewModel, nav, snackbar) }
            composable("results") { PhotoResultsScreen(state, viewModel, nav, snackbar) }
            composable("detail/{type}/{id}", arguments = listOf(navArgument("type") { type = NavType.StringType }, navArgument("id") { type = NavType.StringType })) { entry ->
                val type = entry.arguments?.getString("type").orEmpty()
                val id = entry.arguments?.getString("id").orEmpty()
                if (type == "room") {
                    RoomDetailScreen(id, state, nav)
                } else {
                    DetailScreen(type, id, state, viewModel, nav, snackbar)
                }
            }
        }
    }
}

@Composable
private fun OnboardingScreen(
    state: TidyPilotState,
    viewModel: TidyPilotViewModel,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf(0) }
    var reminders by rememberSaveable { mutableStateOf(false) }
    var permissionNote by rememberSaveable { mutableStateOf("") }
    var starterRooms by remember {
        mutableStateOf(setOf("Kitchen", "Bathroom", "Bedroom", "Living Room"))
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        reminders = granted
        permissionNote = if (granted) {
            "Reminders can be enabled after setup."
        } else {
            "Notification permission was denied. Reminders will stay off."
        }
    }
    fun setReminderPreference(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            reminders = enabled
            permissionNote = if (enabled) "Reminders can be enabled after setup." else "Reminders will stay off."
        }
    }
    val pages = onboardingPages()
    val setupPage = page == pages.lastIndex
    LazyColumn(
        modifier.fillMaxSize().tidyBackground(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Charcoal), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("TidyPilot", color = Cream, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("${page + 1} of ${pages.size}", color = MutedOrange, fontWeight = FontWeight.Bold)
                    Text(pages[page].title, color = Cream, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(pages[page].body, color = Cream, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        if (setupPage) {
            item {
                StudioCard {
                    Text("Choose starter rooms", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Pick the rooms you want TidyPilot to prioritize first. You can change rooms later.")
                    StarterRoomChips(state.rooms.map { it.name }.ifEmpty { defaultStarterRooms }, starterRooms) { room ->
                        starterRooms = if (room in starterRooms) starterRooms - room else starterRooms + room
                    }
                }
            }
            item {
                StudioCard {
                    Text("Reminder preference", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Reminders are optional, local, and quiet by default.")
                    PreferenceRow("Enable gentle reminders", "Tiny reset time? One quick task can help.", reminders) { setReminderPreference(it) }
                    if (permissionNote.isNotBlank()) Text(permissionNote, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(
                    onClick = { page = (page - 1).coerceAtLeast(0) },
                    enabled = page > 0,
                    modifier = Modifier.weight(1f)
                ) { Text("Back") }
                Button(
                    onClick = {
                        if (setupPage) {
                            viewModel.completeOnboarding(starterRooms.ifEmpty { defaultStarterRooms.toSet() }, reminders)
                            scope.launch { snackbar.showSnackbar("Setup complete. Welcome to TidyPilot.") }
                        } else {
                            page++
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(if (setupPage) "Finish setup" else "Next") }
            }
        }
    }
}

private data class OnboardingPage(val title: String, val body: String)

private val defaultStarterRooms = listOf("Kitchen", "Bathroom", "Bedroom", "Living Room", "Laundry", "Entryway")

private fun onboardingPages(): List<OnboardingPage> = listOf(
    OnboardingPage("Welcome", "TidyPilot helps you keep your space manageable."),
    OnboardingPage("Energy-aware planning", "Pick chores based on your time and energy."),
    OnboardingPage("Rooms and tasks", "Organize chores by room."),
    OnboardingPage("Work schedule", "Plan around shifts and days off."),
    OnboardingPage("Privacy", "No login. No cloud. Photos and tasks stay on your device."),
    OnboardingPage("Setup", "Choose starter rooms and whether you want gentle reminders.")
)

@Composable
private fun StarterRoomChips(rooms: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rooms.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { room ->
                    FilterChip(
                        selected = room in selected,
                        onClick = { onToggle(room) },
                        label = { Text(room, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DashboardScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController) {
    val todayText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    val plan = state.todayPlan
    val nextTask = state.suggestedTasks.firstOrNull() ?: state.lowEnergyTask ?: state.tasks.firstOrNull()
    val attentionRooms = state.rooms.sortedWith(compareBy<RoomEntity> { roomScore(it, state).score }.thenByDescending { priorityScore(it.priority) }).take(3)
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            DashboardHeroCard(
                date = todayText,
                title = contextCopy(plan?.workStatus ?: state.todayShift?.let { "working today" } ?: "free day", state.latestCheckIn?.energyLevel),
                planType = plan?.planType ?: "adaptive daily plan",
                energy = state.latestCheckIn?.energyLevel ?: "medium",
                minutes = state.latestCheckIn?.availableMinutes ?: 15,
                onScan = { nav.navigate("scan") },
                onReplan = { nav.navigate("energy") }
            )
        }
        item {
            DashboardSummaryGrid(
                state = state,
                workStatus = plan?.workStatus ?: state.todayShift?.let { "working today" } ?: "free day",
                onSchedule = { nav.navigate("schedule") }
            )
        }
        item {
            QuickStartCard(
                task = nextTask,
                state = state,
                onStart = { nextTask?.let(viewModel::markComplete) },
                onOpen = { nextTask?.let { nav.navigate("detail/task/${it.id}") } },
                onScan = { nav.navigate("scan") }
            )
        }
        item { QuickCleanCard(state, viewModel) }
        item {
            SectionHeader("Today's cleaning plan", plan?.adaptedReason ?: "A realistic reset plan for the next useful step.")
        }
        if (state.suggestedTasks.isNotEmpty()) {
            items(state.suggestedTasks.take(4), key = { it.id }) { task -> PlanTaskCard(task, state, viewModel, nav) }
        } else {
            item { EmptyState("No chores queued.", "Add a task or run a quick room scan.", "Add task") { nav.navigate(Route.Add.value) } }
        }
        item { RoomsNeedingAttentionCard(attentionRooms, state, nav) }
        item { RecentCompletionsCard(state) }
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
    }
}

@Composable
private fun DashboardHeroCard(
    date: String,
    title: String,
    planType: String,
    energy: String,
    minutes: Int,
    onScan: () -> Unit,
    onReplan: () -> Unit
) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Charcoal), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.background(Brush.linearGradient(listOf(Charcoal, Graphite, Charcoal))).padding(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrandMark(Modifier.size(40.dp))
                    Column {
                        Text("TidyPilot", color = Cream, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text(date.uppercase(), color = MutedOrange, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                    }
                }
                Text(title, color = Cream, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Plan: $planType", color = Cream, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardMetricPill("Energy", energy, Modifier.weight(1f))
                    DashboardMetricPill("Time", "$minutes min", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onScan, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Scan room")
                    }
                    FilledTonalButton(onClick = onReplan, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Check in")
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardMetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Cream.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(label, color = Cream.copy(alpha = 0.78f), style = MaterialTheme.typography.labelMedium)
        Text(value, color = Cream, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun DashboardSummaryGrid(state: TidyPilotState, workStatus: String, onSchedule: () -> Unit) {
    StudioCard {
        Text("Home control", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardMiniCard("Done today", "${state.completedTodayCount}", "Soft green wins", Icons.Default.CheckCircle, Modifier.weight(1f))
            DashboardMiniCard("Rooms", "${state.averageTidyScore}/100", "Average tidy score", Icons.Default.RoomPreferences, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardMiniCard("Streak", "${state.streak} days", "Keep it gentle", Icons.Default.CleaningServices, Modifier.weight(1f))
            DashboardMiniCard("Work", workStatus, workShiftImpact(state), Icons.Default.Work, Modifier.weight(1f), onClick = onSchedule)
        }
    }
}

@Composable
private fun DashboardMiniCard(
    label: String,
    value: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier,
        onClick = onClick ?: {}
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = if (label == "Done today" || label == "Streak") Sage else MutedOrange, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QuickStartCard(
    task: CleaningTaskEntity?,
    state: TidyPilotState,
    onStart: () -> Unit,
    onOpen: () -> Unit,
    onScan: () -> Unit
) {
    StudioCard {
        Text("Quick start", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        if (task == null) {
            Text("No task is ready yet. Scan a room or reset the plan to pick one small win.")
            Button(onClick = onScan) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(6.dp))
                Text("Scan room")
            }
        } else {
            Text("Next recommended task", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PlanLabelChip(planLabel(task, state))
            Text(task.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(taskMeta(task, state), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Start task")
                }
                FilledTonalButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
                    Text("Details")
                }
            }
        }
    }
}

@Composable
private fun QuickCleanCard(state: TidyPilotState, viewModel: TidyPilotViewModel) {
    val energy = state.latestCheckIn?.energyLevel ?: "medium"
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Quick Clean", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Pick your time. TidyPilot builds a local mini plan from your tasks.")
            }
            Icon(Icons.Default.CleaningServices, null, tint = MutedOrange)
        }
        RoomStatGrid(
            "5 min" to "Trash or one surface",
            "10 min" to "Dishes and counter",
            "30 min" to "Kitchen, floors, laundry",
            "Energy" to energyLabel(energy)
        )
        WrapButtons(
            "5 minutes" to { viewModel.quickClean(5, energy) },
            "10 minutes" to { viewModel.quickClean(10, energy) },
            "15 minutes" to { viewModel.quickClean(15, energy) },
            "30 minutes" to { viewModel.quickClean(30, energy) },
            "Full reset" to { viewModel.quickClean(null, "high") }
        )
    }
}

@Composable
private fun PlanTaskCard(task: CleaningTaskEntity, state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController) {
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PlanLabelChip(planLabel(task, state))
                Text(task.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(taskMeta(task, state), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (task.skippedCount > 1) Text("Break this into the smallest visible step.", color = MutedOrange, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { viewModel.markComplete(task) }) { Icon(Icons.Default.CheckCircle, "Mark complete", tint = Sage) }
            IconButton(onClick = { nav.navigate("detail/task/${task.id}") }) { Icon(Icons.Default.Edit, "Detail") }
        }
    }
}

@Composable
private fun RoomsNeedingAttentionCard(rooms: List<RoomEntity>, state: TidyPilotState, nav: NavHostController) {
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Rooms needing attention", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Tidy score and priority at a glance.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { nav.navigate("scan") }) { Icon(Icons.Default.CameraAlt, "Scan room", tint = MutedOrange) }
        }
        if (rooms.isEmpty()) {
            Text("No rooms yet.", fontWeight = FontWeight.Black)
            Text("Add your first room to start planning.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            rooms.forEach { room ->
                val score = roomScore(room, state)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(room.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text("${score.score}/100", color = roomScoreColor(score), fontWeight = FontWeight.Black)
                        IconButton(onClick = { nav.navigate("detail/room/${room.id}") }) { Icon(Icons.Default.RoomPreferences, "Room detail") }
                    }
                    LinearProgressIndicator(progress = { score.score / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("${score.label} - ${score.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RecentCompletionsCard(state: TidyPilotState) {
    StudioCard {
        Text("Recent completions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        val recent = state.completions.take(3)
        if (recent.isEmpty()) {
            Text("No recent completions yet.")
            Text("Finish a task when you are ready.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            recent.forEach { completion ->
                val task = state.tasks.firstOrNull { it.id == completion.taskId }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Sage, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(task?.name ?: "Completed task", fontWeight = FontWeight.Black)
                        Text(completion.completedAt.format(DateTimeFormatter.ofPattern("MMM d, h:mm a")), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun EnergyCheckInScreen(
    state: TidyPilotState,
    viewModel: TidyPilotViewModel,
    nav: NavHostController,
    snackbar: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var energy by remember(state.latestCheckIn?.id) { mutableStateOf(state.latestCheckIn?.energyLevel ?: "medium") }
    var minutes by remember(state.latestCheckIn?.id) { mutableStateOf((state.latestCheckIn?.availableMinutes ?: 15).toString()) }
    var note by remember(state.latestCheckIn?.id) { mutableStateOf(state.latestCheckIn?.notes.orEmpty()) }
    val friendlyCopy = when (energy) {
        "very low" -> "Let's pick something doable."
        "low" -> "A small reset still counts."
        "high" -> "You have room for a bigger reset if it helps."
        else -> "Here's the easiest useful task."
    }

    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            HeroCard(
                eyebrow = "Energy check-in",
                title = friendlyCopy,
                body = "TidyPilot will rebuild Today's Plan around how you actually feel right now.",
                actionText = "Scan room",
                onAction = { nav.navigate("scan") }
            )
        }
        item {
            StudioCard {
                Text("How do you feel?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Pick the closest match. This is just for planning, not a score.")
                OptionChips(listOf("very low", "low", "medium", "high"), energy) { selected ->
                    energy = selected
                    minutes = when (selected) {
                        "very low" -> "5"
                        "low" -> "10"
                        "high" -> "35"
                        else -> "20"
                    }
                }
                Text(energySuggestionCopy(energy), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            StudioCard {
                Text("How much time do you have?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                WrapButtons(
                    "I only have 5 minutes" to {
                        minutes = "5"
                        if (energy == "medium" || energy == "high") energy = "low"
                    },
                    "I can do a full clean" to {
                        minutes = "45"
                        energy = "high"
                    }
                )
                OutlinedTextField(
                    minutes,
                    { minutes = it.filter(Char::isDigit) },
                    label = { Text("Available minutes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
        item {
            StudioCard {
                Text("Optional note", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                OutlinedTextField(
                    note,
                    { note = it },
                    label = { Text("Anything affecting today?") },
                    placeholder = { Text("Long shift, sore feet, guests coming over") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Text("The plan should fit the day you actually have.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = {
                    val available = minutes.toIntOrNull()?.coerceIn(5, 180) ?: if (energy == "high") 45 else 15
                    viewModel.saveEnergy(
                        level = energy,
                        mood = energyLabel(energy),
                        minutes = available,
                        exhausted = energy == "very low",
                        notes = note.ifBlank { friendlyCopy }
                    )
                    scope.launch { snackbar.showSnackbar("Today's Plan updated.") }
                    nav.navigate(Route.Dashboard.value) { popUpTo(Route.Dashboard.value) }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Update Today's Plan")
                }
            }
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
private fun AddEditScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, snackbar: SnackbarHostState, initialTaskId: String? = null) {
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableStateOf("task") }
    var editingTask by remember(initialTaskId, state.tasks) { mutableStateOf(state.tasks.firstOrNull { it.id == initialTaskId }) }
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Add/Edit", "Create cleaning tasks, rooms, and work shifts.") }
        item {
            OptionChips(listOf("task", "room", "shift"), mode) {
                mode = it
                if (it != "task") editingTask = null
            }
        }
        item {
            when (mode) {
                "room" -> RoomForm(state, viewModel, snackbar)
                "shift" -> ShiftForm(viewModel, snackbar)
                else -> TaskForm(state, viewModel, snackbar, editingTask) { editingTask = null }
            }
        }
        item {
            StudioCard {
                Text("Saved tasks", fontWeight = FontWeight.Black)
                if (state.tasks.isEmpty()) {
                    Text("No chores queued.", fontWeight = FontWeight.Black)
                    Text("Add a task or run a quick room scan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.tasks.take(8).forEach { task ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(task.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { mode = "task"; editingTask = task }) { Icon(Icons.Default.Edit, "Edit") }
                        IconButton(onClick = { viewModel.deleteTask(task); scope.launch { snackbar.showSnackbar("Task removed.") } }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskForm(
    state: TidyPilotState,
    viewModel: TidyPilotViewModel,
    snackbar: SnackbarHostState,
    existing: CleaningTaskEntity? = null,
    onSaved: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var roomId by remember(existing?.id, state.rooms) { mutableStateOf(existing?.roomId ?: state.rooms.firstOrNull()?.id.orEmpty()) }
    var notes by remember(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var priority by remember(existing?.id) { mutableStateOf(existing?.priority ?: "normal") }
    var minutes by remember(existing?.id) { mutableStateOf((existing?.estimatedMinutes ?: 10).toString()) }
    var difficulty by remember(existing?.id) { mutableStateOf(existing?.difficulty ?: taskDifficultyFrom(existing)) }
    var energy by remember(existing?.id) { mutableStateOf(existing?.energyRequired ?: "low") }
    var frequency by remember(existing?.id) { mutableStateOf(existing?.frequencyType ?: "weekly") }
    var category by remember(existing?.id) { mutableStateOf(existing?.photoDetectableCategory ?: "clutter") }
    var dueChoice by remember(existing?.id) { mutableStateOf(dueChoiceFor(existing?.nextDueAt)) }

    fun chooseRoomByHint(hint: String) {
        val match = state.rooms.firstOrNull { room ->
            room.name.contains(hint, ignoreCase = true) || room.roomType.contains(hint, ignoreCase = true)
        }
        if (match != null) roomId = match.id
    }

    fun applyTemplate(template: TaskTemplate) {
        name = template.name
        minutes = template.minutes.toString()
        difficulty = template.difficulty
        energy = template.energy
        frequency = template.frequency
        priority = template.priority
        category = template.category
        chooseRoomByHint(template.roomHint)
    }

    fun resetForAnother() {
        name = ""
        notes = ""
        priority = "normal"
        minutes = "10"
        difficulty = "easy"
        energy = "low"
        frequency = "weekly"
        category = "clutter"
        dueChoice = "today"
    }

    fun save(resetAfter: Boolean) {
        val estimatedMinutes = minutes.toIntOrNull() ?: 10
        val task = CleaningTaskEntity(
            name = name,
            roomId = roomId,
            description = notes,
            priority = priority,
            estimatedMinutes = estimatedMinutes,
            difficulty = difficulty,
            energyRequired = energy,
            frequencyType = frequency,
            preferredTime = existing?.preferredTime ?: "anytime",
            isQuickResetTask = difficulty == "easy" && estimatedMinutes <= 10,
            isDeepCleanTask = difficulty == "hard" || estimatedMinutes >= 30,
            photoDetectableCategory = category,
            nextDueAt = dueDateFromChoice(dueChoice)
        )
        val error = viewModel.saveTask(existing, task)
        scope.launch { snackbar.showSnackbar(error ?: if (existing == null) "Task saved." else "Task updated.") }
        if (error == null) {
            onSaved()
            if (resetAfter) resetForAnother()
        }
    }

    StudioCard {
        Text(if (existing == null) "Quick add task" else "Edit task", fontWeight = FontWeight.Black)
        Text("Start from a common chore, then adjust only what matters.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Templates", fontWeight = FontWeight.SemiBold)
        TaskTemplateChips(taskTemplates) { applyTemplate(it) }
        OutlinedTextField(name, { name = it }, label = { Text("Task name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Room")
        OptionChips(state.rooms.map { it.name }, state.rooms.firstOrNull { it.id == roomId }?.name.orEmpty()) { chosen -> roomId = state.rooms.firstOrNull { it.name == chosen }?.id.orEmpty() }
        Text("Estimated time")
        OptionChips(listOf("2", "5", "10", "15", "30"), minutes) { minutes = it }
        Text("Difficulty")
        OptionChips(listOf("easy", "medium", "hard"), difficulty) { difficulty = it }
        Text("Energy required")
        OptionChips(listOf("low", "medium", "high"), energy) { energy = it }
        Text("Repeat frequency")
        OptionChips(listOf("one-time", "daily", "every few days", "weekly", "monthly"), frequency) { frequency = it }
        Text("Priority")
        OptionChips(listOf("low", "normal", "high", "urgent"), priority) { priority = it }
        Text("Optional due date")
        OptionChips(dueOptions(dueChoice), dueChoice) { dueChoice = it }
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { save(false) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(Modifier.width(6.dp))
                Text(if (existing == null) "Save task" else "Update task")
            }
            FilledTonalButton(onClick = { save(true) }, modifier = Modifier.weight(1f)) { Text("Save and add another") }
        }
    }
}

private data class TaskTemplate(
    val name: String,
    val roomHint: String,
    val minutes: Int,
    val difficulty: String,
    val energy: String,
    val frequency: String,
    val priority: String,
    val category: String
)

private val taskTemplates = listOf(
    TaskTemplate("Take out trash", "Kitchen", 3, "easy", "low", "every few days", "normal", "trash"),
    TaskTemplate("Do dishes", "Dishes", 10, "medium", "medium", "daily", "high", "dishes"),
    TaskTemplate("Wipe counters", "Kitchen", 5, "easy", "low", "daily", "normal", "surface wipe"),
    TaskTemplate("Vacuum floor", "Floors", 15, "medium", "medium", "weekly", "normal", "floor clutter"),
    TaskTemplate("Start laundry", "Laundry", 7, "easy", "low", "every few days", "high", "laundry"),
    TaskTemplate("Switch laundry", "Laundry", 5, "easy", "low", "every few days", "normal", "laundry"),
    TaskTemplate("Fold laundry", "Laundry", 15, "medium", "medium", "weekly", "normal", "laundry"),
    TaskTemplate("Clean bathroom sink", "Bathroom", 5, "easy", "low", "every few days", "normal", "bathroom reset"),
    TaskTemplate("Clear one surface", "Other", 5, "easy", "low", "daily", "normal", "clutter"),
    TaskTemplate("10-minute room reset", "Other", 10, "medium", "medium", "one-time", "high", "general reset")
)

@Composable
private fun TaskTemplateChips(templates: List<TaskTemplate>, onSelect: (TaskTemplate) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        templates.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { template ->
                    FilledTonalButton(onClick = { onSelect(template) }, modifier = Modifier.weight(1f)) {
                        Text(template.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun taskDifficultyFrom(task: CleaningTaskEntity?): String = when {
    task == null -> "easy"
    task.difficulty.isNotBlank() -> task.difficulty
    task.isDeepCleanTask || task.estimatedMinutes >= 30 || task.energyRequired == "high" -> "hard"
    task.estimatedMinutes >= 15 || task.energyRequired == "medium" -> "medium"
    else -> "easy"
}

private fun dueChoiceFor(date: LocalDate?): String {
    val today = LocalDate.now()
    return when (date) {
        null -> "no date"
        today -> "today"
        today.plusDays(1) -> "tomorrow"
        today.plusWeeks(1) -> "next week"
        else -> date.toString()
    }
}

private fun dueOptions(selected: String): List<String> {
    val base = listOf("no date", "today", "tomorrow", "next week")
    return if (selected in base) base else base + selected
}

private fun dueDateFromChoice(choice: String): LocalDate? {
    val today = LocalDate.now()
    return when (choice) {
        "no date" -> null
        "today" -> today
        "tomorrow" -> today.plusDays(1)
        "next week" -> today.plusWeeks(1)
        else -> runCatching { LocalDate.parse(choice) }.getOrNull()
    }
}

@Composable
private fun RoomForm(state: TidyPilotState, viewModel: TidyPilotViewModel, snackbar: SnackbarHostState, existing: RoomEntity? = null, onSaved: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var type by remember(existing?.id) { mutableStateOf(existing?.roomType ?: "Kitchen") }
    var priority by remember(existing?.id) { mutableStateOf(existing?.priority ?: "normal") }
    var intensity by remember(existing?.id) { mutableStateOf(existing?.defaultTaskIntensity ?: "medium") }
    var frequency by remember(existing?.id) { mutableStateOf(existing?.defaultTaskFrequency ?: "weekly") }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    StudioCard {
        Text(if (existing == null) "Add room" else "Edit room", fontWeight = FontWeight.Black)
        OutlinedTextField(name, { name = it }, label = { Text("Room name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(type, { type = it }, label = { Text("Room type") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Priority")
        OptionChips(listOf("low", "normal", "high", "urgent"), priority) { priority = it }
        Text("Default task intensity")
        OptionChips(listOf("low", "medium", "high"), intensity) { intensity = it }
        Text("Default task frequency")
        OptionChips(listOf("daily", "every few days", "weekly", "monthly"), frequency) { frequency = it }
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Button(onClick = {
            val error = viewModel.saveRoom((existing ?: RoomEntity(name = name, roomType = type)).copy(name = name, roomType = type, priority = priority, defaultTaskIntensity = intensity, defaultTaskFrequency = frequency, notes = notes))
            scope.launch { snackbar.showSnackbar(error ?: "Room saved.") }
            if (error == null) {
                if (existing == null) name = ""
                onSaved()
            }
        }) { Text("Save room") }
    }
}

@Composable
private fun ShiftForm(viewModel: TidyPilotViewModel, snackbar: SnackbarHostState, existing: WorkShiftEntity? = null, onSaved: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var date by remember(existing?.id) { mutableStateOf(existing?.date?.toString() ?: LocalDate.now().toString()) }
    var start by remember(existing?.id) { mutableStateOf(existing?.startTime?.toString() ?: "09:00") }
    var end by remember(existing?.id) { mutableStateOf(existing?.endTime?.toString() ?: "17:30") }
    var label by remember(existing?.id) { mutableStateOf(existing?.label ?: "Work shift") }
    var exhaustion by remember(existing?.id) { mutableStateOf(existing?.expectedExhaustionLevel ?: "medium") }
    var repeat by remember(existing?.id) { mutableStateOf("none") }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    StudioCard {
        Text(if (existing == null) "Add shift" else "Edit shift", fontWeight = FontWeight.Black)
        OutlinedTextField(date, { date = it }, label = { Text("Date YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(start, { start = it }, label = { Text("Start time HH:MM") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(end, { end = it }, label = { Text("End time HH:MM") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(label, { label = it }, label = { Text("Optional shift label") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Expected exhaustion")
        OptionChips(listOf("low", "medium", "high"), exhaustion) { exhaustion = it }
        if (existing == null) {
            Text("Repeat pattern")
            OptionChips(listOf("none", "weekly x4", "weekdays x2"), repeat) { repeat = it }
        }
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                val baseDate = parseDate(date)
                val base = (existing ?: WorkShiftEntity(date = baseDate, startTime = parseTime(start), endTime = parseTime(end))).copy(
                    date = baseDate,
                    startTime = parseTime(start),
                    endTime = parseTime(end),
                    label = label.ifBlank { "Work shift" },
                    expectedExhaustionLevel = exhaustion,
                    notes = notes
                )
                val repeated = shiftRepeatDates(baseDate, repeat).mapIndexed { index, repeatDate ->
                    if (index == 0) base else base.copy(id = java.util.UUID.randomUUID().toString(), date = repeatDate)
                }
                val error = if (existing == null && repeated.size > 1) {
                    if (base.endTime.isAfter(base.startTime)) {
                        viewModel.saveShifts(repeated)
                        null
                    } else {
                        "End time must be after start time."
                    }
                } else {
                    viewModel.saveShift(base)
                }
                scope.launch { snackbar.showSnackbar(error ?: if (repeated.size > 1) "${repeated.size} shifts saved." else "Shift saved.") }
                if (error == null) onSaved()
            }, modifier = Modifier.weight(1f)) { Text(if (existing == null) "Save shift" else "Update shift") }
            FilledTonalButton(onClick = {
                val parsedDate = parseDate(date)
                viewModel.markDayOff(parsedDate)
                scope.launch { snackbar.showSnackbar("Marked $parsedDate as a day off.") }
                onSaved()
            }, modifier = Modifier.weight(1f)) { Text("Mark day off") }
        }
    }
}

private fun shiftRepeatDates(startDate: LocalDate, repeat: String): List<LocalDate> = when (repeat) {
    "weekly x4" -> (0L..3L).map { startDate.plusWeeks(it) }
    "weekdays x2" -> generateSequence(startDate) { it.plusDays(1) }
        .filter { it.dayOfWeek.value <= 5 }
        .take(10)
        .toList()
    else -> listOf(startDate)
}

@Composable
private fun RoomManagementScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, snackbar: SnackbarHostState, nav: NavHostController) {
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Room Management", "Create, prioritize, and understand rooms without a spreadsheet.") }
        if (state.rooms.isEmpty()) {
            item { EmptyState("No rooms yet.", "Add your first room to start planning.") }
        }
        item {
            ProgressGrid(
                "Rooms" to "${state.rooms.size}",
                "Active tasks" to "${state.tasks.size}",
                "Open issues" to "${state.issues.size}",
                "Avg score" to "${state.averageTidyScore}/100"
            )
        }
        items(state.rooms, key = { it.id }) { room ->
            RoomManagementCard(room, state, viewModel, snackbar, nav)
        }
        item { RoomForm(state, viewModel, snackbar) }
    }
}

@Composable
private fun RoomManagementCard(room: RoomEntity, state: TidyPilotState, viewModel: TidyPilotViewModel, snackbar: SnackbarHostState, nav: NavHostController) {
    val scope = rememberCoroutineScope()
    var editing by remember(room.id) { mutableStateOf(false) }
    var showHistory by remember(room.id) { mutableStateOf(false) }
    val stats = roomStats(room, state)
    val score = roomScore(room, state)
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.RoomPreferences, null, tint = roomScoreColor(score), modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(room.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("${score.label} - ${score.reason}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${score.score}/100", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = roomScoreColor(score))
        }
        LinearProgressIndicator(progress = { score.score / 100f }, modifier = Modifier.fillMaxWidth())
        RoomStatGrid(
            "Active tasks" to "${stats.activeTasks}",
            "Overdue" to "${score.overdueTasks}",
            "Open issues" to "${stats.openIssues}",
            "Last reset" to (score.lastCompletedLabel),
            "Last scanned" to (stats.lastScanned ?: "Not yet")
        )
        Text("Default: ${room.defaultTaskFrequency} - ${room.defaultTaskIntensity} intensity", color = MaterialTheme.colorScheme.onSurfaceVariant)
        WrapButtons(
            (if (editing) "Close edit" else "Rename / edit") to { editing = !editing },
            (if (showHistory) "Hide history" else "View history") to { showHistory = !showHistory },
            "Room detail" to { nav.navigate("detail/room/${room.id}") },
            "Archive room" to {
                viewModel.archiveRoom(room)
                scope.launch { snackbar.showSnackbar("${room.name} archived.") }
            },
            "Delete room" to {
                if (stats.safeToDelete) {
                    viewModel.deleteRoom(room)
                    scope.launch { snackbar.showSnackbar("${room.name} deleted.") }
                } else {
                    scope.launch { snackbar.showSnackbar("Archive this room first. It still has tasks, scans, or scan issues.") }
                }
            }
        )
        if (editing) {
            RoomForm(state, viewModel, snackbar, existing = room) { editing = false }
        }
        if (showHistory) {
            RoomHistory(room, state)
        }
    }
}

@Composable
private fun RoomHistory(room: RoomEntity, state: TidyPilotState) {
    val roomTaskIds = state.tasks.filter { it.roomId == room.id }.map { it.id }.toSet()
    val completions = state.completions.filter { it.taskId in roomTaskIds }.take(4)
    val scans = state.scans.filter { it.roomId == room.id }.take(4)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Room history", fontWeight = FontWeight.Black)
        if (completions.isEmpty() && scans.isEmpty()) {
            Text("No room history yet.")
        }
        completions.forEach {
            Text("Cleaned: ${it.completedAt.format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        scans.forEach {
            Text("Scanned: ${it.scanDate.format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))} - score ${it.tidyScore}/100", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LegacyWorkScheduleScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, snackbar: SnackbarHostState) {
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
private fun WorkScheduleScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, snackbar: SnackbarHostState) {
    var editingShift by remember { mutableStateOf<WorkShiftEntity?>(null) }
    val today = LocalDate.now()
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Work Schedule", "TidyPilot plans around shifts, days off, and recovery time.") }
        item { SchedulePhotoImportCard(viewModel, snackbar) }
        if (state.shifts.isEmpty()) {
            item { EmptyState("No shifts added.", "Add your schedule so TidyPilot can plan around work.") }
        }
        item {
            ProgressGrid(
                "Today" to workDayLabel(state.todayShift, state.todayPlan?.workStatus),
                "This week" to "${state.shifts.count { it.date in today..today.plusDays(6) }} shifts",
                "Next shift" to (state.shifts.firstOrNull { !it.date.isBefore(today) }?.let { "${it.date} ${it.startTime}" } ?: "None"),
                "Planning" to shiftPlanningSummary(state)
            )
        }
        items(state.shifts, key = { it.id }) { shift ->
            WorkShiftCard(
                shift = shift,
                onEdit = { editingShift = shift },
                onDelete = { viewModel.deleteShift(shift) },
                onDayOff = { viewModel.markDayOff(shift.date) }
            )
        }
        item { ShiftForm(viewModel, snackbar, editingShift) { editingShift = null } }
    }
}

@Composable
private fun SchedulePhotoImportCard(viewModel: TidyPilotViewModel, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var isReading by rememberSaveable { mutableStateOf(false) }
    var rawText by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("Photos stay on this device. Review before saving.") }
    var candidates by remember { mutableStateOf<List<ScheduleImportCandidate>>(emptyList()) }

    fun updateImportText(text: String) {
        rawText = text
        candidates = ScheduleImportParser.parse(text)
        message = if (candidates.isEmpty()) {
            "No shifts found yet. Edit the text below or add shifts manually."
        } else {
            "${candidates.size} possible shift${if (candidates.size == 1) "" else "s"} found. Review before saving."
        }
    }

    fun readScheduleImage(uri: Uri) {
        isReading = true
        message = "Reading schedule image locally..."
        runCatching { InputImage.fromFilePath(context, uri) }
            .onSuccess { image ->
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .addOnSuccessListener { recognized ->
                        isReading = false
                        updateImportText(recognized.text)
                    }
                    .addOnFailureListener {
                        isReading = false
                        message = "Could not read that image. You can select another photo or add shifts manually."
                    }
            }
            .onFailure {
                isReading = false
                message = "Could not open that image. Try selecting a screenshot instead."
            }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingUri
        if (ok && uri != null) {
            readScheduleImage(uri)
        } else {
            scope.launch { snackbar.showSnackbar("Schedule photo was not saved.") }
        }
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pendingUri = uri
            readScheduleImage(uri)
        } else {
            scope.launch { snackbar.showSnackbar("No schedule image selected.") }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            runCatching {
                val uri = createImageUri(context)
                pendingUri = uri
                takePicture.launch(uri)
            }.onFailure {
                scope.launch { snackbar.showSnackbar("Could not open camera. Try selecting a screenshot.") }
            }
        } else {
            scope.launch { snackbar.showSnackbar("Camera permission is only used for local schedule import.") }
        }
    }

    StudioCard {
        Text("Import schedule photo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text("Use a schedule screenshot or photo to draft shifts. OCR runs on this device, and nothing is saved until you confirm.")
        Text("Photos stay on this device.", color = Sage, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { permission.launch(Manifest.permission.CAMERA) }, enabled = !isReading, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(6.dp))
                Text("Take photo")
            }
            FilledTonalButton(onClick = { pickPhoto.launch("image/*") }, enabled = !isReading, modifier = Modifier.weight(1f)) {
                Text("Select image")
            }
        }
        if (isReading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (rawText.isNotBlank()) {
            OutlinedTextField(
                value = rawText,
                onValueChange = ::updateImportText,
                label = { Text("Recognized schedule text") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (candidates.isNotEmpty()) {
            Text("Preview shifts", fontWeight = FontWeight.SemiBold)
            candidates.take(5).forEach { candidate ->
                ScheduleImportCandidateRow(candidate) {
                    candidates = candidates.filterNot { it == candidate }
                }
            }
            if (candidates.size > 5) Text("${candidates.size - 5} more shifts will be saved.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = {
                    viewModel.saveShifts(
                        candidates.map {
                            WorkShiftEntity(
                                date = it.date,
                                startTime = it.startTime,
                                endTime = it.endTime,
                                label = it.label,
                                expectedExhaustionLevel = it.expectedExhaustionLevel,
                                notes = "Imported from schedule photo. Review confidence: ${it.confidenceLabel}. Source: ${it.sourceLine}"
                            )
                        }
                    )
                    scope.launch { snackbar.showSnackbar("${candidates.size} imported shift${if (candidates.size == 1) "" else "s"} saved.") }
                    rawText = ""
                    candidates = emptyList()
                    message = "Import saved. Photos stay on this device."
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm imported shifts")
            }
        }
    }
}

@Composable
private fun ScheduleImportCandidateRow(candidate: ScheduleImportCandidate, onIgnore: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${candidate.date} - ${candidate.startTime} to ${candidate.endTime}", fontWeight = FontWeight.Black)
                    Text("${candidate.label} - expected ${candidate.expectedExhaustionLevel} exhaustion", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Confidence: ${candidate.confidenceLabel}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onIgnore) { Text("Ignore") }
            }
        }
    }
}

@Composable
private fun WorkShiftCard(shift: WorkShiftEntity, onEdit: () -> Unit, onDelete: () -> Unit, onDayOff: () -> Unit) {
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Work, null, tint = MutedOrange)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${shift.date} - ${shift.label}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("${shift.startTime} - ${shift.endTime} - ${shiftLengthLabel(shift)}")
                Text("Planning: ${shiftPlanningLabel(shift)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (shift.notes.isNotBlank()) Text(shift.notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        WrapButtons(
            "Edit shift" to onEdit,
            "Delete shift" to onDelete,
            "Mark day off" to onDayOff
        )
    }
}

private fun shiftLengthHours(shift: WorkShiftEntity): Long = Duration.between(shift.startTime, shift.endTime).toHours()

private fun shiftLengthLabel(shift: WorkShiftEntity): String {
    val hours = shiftLengthHours(shift)
    return "$hours hr shift, expected ${shift.expectedExhaustionLevel} exhaustion"
}

private fun shiftPlanningLabel(shift: WorkShiftEntity): String = when {
    shiftLengthHours(shift) >= 8 && shift.expectedExhaustionLevel == "high" -> "After work: low-energy tasks"
    shiftLengthHours(shift) >= 8 -> "Before work: quick tasks only"
    else -> "Before work: time-limited tasks"
}

private fun workDayLabel(shift: WorkShiftEntity?, workStatus: String?): String = when {
    shift == null -> "Day off reset"
    workStatus == "before work" -> "Before work"
    workStatus == "after work" -> "After work"
    workStatus == "too tight today" -> "Too tight today"
    else -> "Working today"
}

private fun shiftPlanningSummary(state: TidyPilotState): String = when (state.todayPlan?.workStatus) {
    "before work" -> "quick tasks only"
    "after work" -> "low-energy plan"
    "too tight today" -> "too tight today"
    "day off" -> "day off reset"
    else -> "adaptive"
}

@Composable
private fun RoomPhotoScanScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var roomId by remember(state.rooms) { mutableStateOf(state.rooms.firstOrNull()?.id.orEmpty()) }
    var note by rememberSaveable { mutableStateOf("") }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var isAnalyzing by rememberSaveable { mutableStateOf(false) }
    var scanStartCount by rememberSaveable { mutableStateOf(state.scans.size) }
    val selectedRoom = state.rooms.firstOrNull { it.id == roomId }

    fun startLocalScan(room: RoomEntity, uri: Uri, scanNote: String) {
        scanStartCount = state.scans.size
        isAnalyzing = true
        viewModel.analyzePhoto(room, uri, scanNote)
    }

    LaunchedEffect(state.scans.size, isAnalyzing) {
        if (isAnalyzing && state.scans.size > scanStartCount) {
            isAnalyzing = false
            nav.navigate("results")
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val room = selectedRoom
        val uri = pendingUri
        if (ok && room != null && uri != null) {
            startLocalScan(room, uri, note)
        } else {
            scope.launch { snackbar.showSnackbar("Photo was not saved. You can retake it.") }
        }
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val room = selectedRoom
        if (uri != null && room != null) {
            pendingUri = uri
            startLocalScan(room, uri, note)
        } else {
            scope.launch { snackbar.showSnackbar("Choose a room and photo to run a scan.") }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            runCatching {
                val uri = createImageUri(context)
                pendingUri = uri
                takePicture.launch(uri)
            }.onFailure {
                scope.launch { snackbar.showSnackbar("Could not open the camera file. Try selecting a photo instead.") }
            }
        } else {
            scope.launch { snackbar.showSnackbar("Camera permission is only used to take a local room photo.") }
        }
    }
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Room Photo Scan", "Turn a room photo into a practical cleanup plan.") }
        item {
            StudioCard {
                Text("How scanning works", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("1. Pick a room\n2. Take or select a photo\n3. TidyPilot runs a local estimate\n4. Review, edit, or ignore suggestions\n5. Create tasks from the useful ones")
                Text("Photos stay on this device.", color = Sage, fontWeight = FontWeight.Black)
            }
        }
        item {
            StudioCard {
                Text("Pick room", fontWeight = FontWeight.Black)
                OptionChips(state.rooms.map { it.name }, selectedRoom?.name.orEmpty()) { chosen -> roomId = state.rooms.firstOrNull { it.name == chosen }?.id.orEmpty() }
                OutlinedTextField(note, { note = it }, label = { Text("Optional note") }, placeholder = { Text("after work mess") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        }
        item {
            StudioCard {
                Text("Photo source", fontWeight = FontWeight.Black)
                Text("Camera access is only used to capture a room photo for local analysis. No upload, no account, no network AI.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Take photo")
                    }
                    FilledTonalButton(onClick = { pickPhoto.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("Select photo") }
                }
                FilledTonalButton(
                    onClick = {
                        val room = selectedRoom
                        if (room == null) {
                            scope.launch { snackbar.showSnackbar("Add or choose a room before running a sample scan.") }
                            return@FilledTonalButton
                        }
                        startLocalScan(room, Uri.parse("sample://${room.name}/${System.currentTimeMillis()}"), note.ifBlank { "manual scan" })
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Try sample scan") }
            }
        }
        if (isAnalyzing) {
            item {
                StudioCard {
                    Text("Running local analysis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Checking for practical cleanup signals like dishes, laundry, trash, floor clutter, and surfaces.")
                    Text("Photos stay on this device.", color = Sage, fontWeight = FontWeight.Black)
                }
            }
        }
        pendingUri?.let { uri ->
            item {
                StudioCard {
                    Text("Selected photo", fontWeight = FontWeight.Black)
                    if (uri.toString().startsWith("content://")) {
                        Image(rememberAsyncImagePainter(uri), null, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    } else {
                        Text("Sample scan preview")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { permission.launch(Manifest.permission.CAMERA) }, modifier = Modifier.weight(1f)) { Text("Retake") }
                        FilledTonalButton(onClick = { pickPhoto.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("Choose another") }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegacyPhotoResultsScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController) {
    val scan = state.scans.firstOrNull()
    if (scan == null) {
        EmptyState("No room scans yet.", "Scan a room to create suggested tasks.", "Scan room") { nav.navigate("scan") }
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
private fun PhotoResultsScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController, snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    val scan = state.scans.firstOrNull()
    if (scan == null) {
        EmptyState("No room scans yet.", "Scan a room to create suggested tasks.", "Scan room") { nav.navigate("scan") }
        return
    }
    val room = state.rooms.firstOrNull { it.id == scan.roomId }
    val issues = state.issues.filter { it.scanId == scan.id }
    val drafts = remember(scan.id, issues.size) {
        mutableStateListOf<ScanIssueDraft>().apply {
            addAll(issues.map { issue ->
                ScanIssueDraft(
                    sourceIssueId = issue.id,
                    tag = issue.tag,
                    label = issue.label,
                    action = issue.suggestedAction,
                    minutes = issue.estimatedMinutes.toString(),
                    energy = issue.energyLevel,
                    status = "review",
                    editing = false,
                    confidence = issue.confidence
                )
            })
        }
    }

    fun createTaskFromDraft(draft: ScanIssueDraft): Boolean {
        if (room == null || draft.action.isBlank() || draft.status == "ignored" || draft.status == "handled" || draft.status == "created") {
            return false
        }
        val minutes = draft.minutes.toIntOrNull()?.coerceAtLeast(1) ?: 5
        viewModel.saveTask(
            null,
            CleaningTaskEntity(
                name = draft.action,
                roomId = room.id,
                description = "Suggested from room scan: ${draft.label}",
                priority = if (draft.energy == "low") "normal" else "high",
                estimatedMinutes = minutes,
                difficulty = scanDraftDifficulty(draft),
                energyRequired = draft.energy,
                frequencyType = "one-time",
                preferredTime = "anytime",
                isQuickResetTask = minutes <= 10 && draft.energy == "low",
                isDeepCleanTask = minutes >= 30 || draft.energy == "high",
                photoDetectableCategory = draft.tag,
                nextDueAt = LocalDate.now()
            )
        )
        return true
    }

    fun createTasksFromDrafts(makePlan: Boolean) {
        val selected = drafts.filter { it.status == "review" && it.action.isNotBlank() }
        if (room == null || selected.isEmpty()) {
            scope.launch { snackbar.showSnackbar("Select at least one useful action first.") }
            return
        }
        selected.forEach { draft -> createTaskFromDraft(draft) }
        drafts.indices.forEach { index ->
            if (drafts[index].status == "review" && drafts[index].action.isNotBlank()) {
                drafts[index] = drafts[index].copy(status = "created", editing = false)
            }
        }
        if (makePlan) viewModel.replan()
        scope.launch { snackbar.showSnackbar("${selected.size} scan task${if (selected.size == 1) "" else "s"} created.") }
        if (makePlan) nav.navigate(Route.Dashboard.value)
    }

    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Photo Analysis Results", room?.name ?: "Room scanned") }
        item {
            StudioCard {
                if (scan.imageUri.startsWith("content://")) {
                    Image(rememberAsyncImagePainter(Uri.parse(scan.imageUri)), null, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                }
                Text("Local scan complete", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Tidy score ${scan.tidyScore}/100 - mess score ${scan.messScore}/100", fontWeight = FontWeight.Black)
                LinearProgressIndicator(progress = { scan.tidyScore / 100f }, modifier = Modifier.fillMaxWidth())
                Text(scan.confidenceSummary)
                Text("Photos stay on this device.", color = Sage, fontWeight = FontWeight.Black)
            }
        }
        item { SectionHeader("Review detected issues", "Review before adding. These are local estimates, not perfect detection.") }
        item {
            StudioCard {
                Text("${scan.estimatedCleanupMinutes} minutes estimated - ${energyRecommendation(issues)} energy recommended", fontWeight = FontWeight.Black)
                Text("Possible scan findings can be edited or ignored before they become chores.")
                WrapButtons(
                    "Create all suggested tasks" to { createTasksFromDrafts(false) },
                    "Ignore all low-confidence issues" to {
                        var ignored = 0
                        drafts.indices.forEach { index ->
                            if (confidenceLabel(drafts[index].confidence) == "low" && drafts[index].status == "review") {
                                drafts[index] = drafts[index].copy(status = "ignored", editing = false)
                                ignored++
                            }
                        }
                        scope.launch { snackbar.showSnackbar("$ignored low-confidence issue${if (ignored == 1) "" else "s"} ignored.") }
                    }
                )
            }
        }
        if (drafts.isEmpty()) {
            item {
                StudioCard {
                    Text("No scan issues found.", fontWeight = FontWeight.Black)
                    Text("Add a manual action if there is something useful to turn into a task.")
                }
            }
        } else {
            items(drafts.size, key = { drafts[it].sourceIssueId }) { index ->
                val draft = drafts[index]
                ScanIssueEditor(
                    draft = draft,
                    roomName = room?.name ?: "Room",
                    onChange = { drafts[index] = it },
                    onCreate = {
                        if (createTaskFromDraft(draft)) {
                            drafts[index] = draft.copy(status = "created", editing = false)
                            scope.launch { snackbar.showSnackbar("Task created from scan.") }
                        } else {
                            scope.launch { snackbar.showSnackbar("Edit the suggested task before creating it.") }
                        }
                    },
                    onIgnore = { drafts[index] = draft.copy(status = "ignored", editing = false) },
                    onHandled = { drafts[index] = draft.copy(status = "handled", editing = false) }
                )
            }
        }
        item {
            StudioCard {
                Text("Manual correction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("If the local scan missed something, add a simple visible action before creating tasks.")
                Button(onClick = {
                    drafts.add(
                        ScanIssueDraft(
                            sourceIssueId = "manual-${System.currentTimeMillis()}",
                            tag = "general_reset_needed",
                            label = "Manual issue",
                            action = "Reset one visible area",
                            minutes = "5",
                            energy = "low",
                            status = "review",
                            editing = true,
                            confidence = 1f
                        )
                    )
                }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add manual issue")
                }
            }
        }
        item {
            StudioCard {
                WrapButtons(
                    "Make this today's plan" to { createTasksFromDrafts(true) },
                    "Add selected tasks" to { createTasksFromDrafts(false) },
                    "Save for later" to { nav.navigate(Route.Dashboard.value) },
                    "Compare before/after" to { nav.navigate("scan") }
                )
                Text("Was the analysis helpful?", fontWeight = FontWeight.Bold)
                WrapButtons(
                    "Accurate" to {
                        viewModel.setScanFeedback(scan.id, "accurate")
                        scope.launch { snackbar.showSnackbar("Feedback saved.") }
                    },
                    "Partly accurate" to {
                        viewModel.setScanFeedback(scan.id, "partly accurate")
                        scope.launch { snackbar.showSnackbar("Feedback saved.") }
                    },
                    "Inaccurate" to {
                        viewModel.setScanFeedback(scan.id, "inaccurate")
                        scope.launch { snackbar.showSnackbar("Feedback saved.") }
                    }
                )
            }
        }
    }
}

@Composable
private fun ScanIssueEditor(
    draft: ScanIssueDraft,
    roomName: String,
    onChange: (ScanIssueDraft) -> Unit,
    onCreate: () -> Unit,
    onIgnore: () -> Unit,
    onHandled: () -> Unit
) {
    val confidence = confidenceLabel(draft.confidence)
    val difficulty = scanDraftDifficulty(draft)
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(scanIssueTitle(draft), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Room: $roomName", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(confidence, color = confidenceColor(confidence), fontWeight = FontWeight.Black)
        }
        RoomStatGrid(
            "Suggested" to draft.action,
            "Estimated time" to "${draft.minutes.ifBlank { "5" }} min",
            "Difficulty" to difficulty,
            "Status" to draft.status
        )
        Text("Suggested: ${draft.action.ifBlank { "Add a task before creating" }}", fontWeight = FontWeight.SemiBold)
        Text("Review before adding. This is a local estimate, not perfect AI detection.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (draft.editing) {
            OutlinedTextField(
                draft.action,
                { onChange(draft.copy(action = it)) },
                label = { Text("Suggested task") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text("Estimated time")
            OutlinedTextField(
                draft.minutes,
                { onChange(draft.copy(minutes = it.filter(Char::isDigit))) },
                label = { Text("Minutes") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text("Energy")
            OptionChips(listOf("low", "medium", "high"), draft.energy) { onChange(draft.copy(energy = it)) }
        }
        WrapButtons(
            "Create task" to onCreate,
            "Ignore" to onIgnore,
            (if (draft.editing) "Done editing" else "Edit") to { onChange(draft.copy(editing = !draft.editing)) },
            "Already handled" to onHandled
        )
        if (draft.status == "created") {
            Text("Task created.", color = Sage, fontWeight = FontWeight.Black)
        } else if (draft.status == "ignored") {
            Text("Ignored for now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (draft.status == "handled") {
            Text("Marked already handled.", color = Sage, fontWeight = FontWeight.Black)
        }
    }
}

private data class ScanIssueDraft(
    val sourceIssueId: String,
    val tag: String,
    val label: String,
    val action: String,
    val minutes: String,
    val energy: String,
    val status: String,
    val editing: Boolean,
    val confidence: Float
)

private fun scanIssueTitle(draft: ScanIssueDraft): String = when (draft.tag) {
    "cluttered_surface" -> "Possible clutter on counter"
    "dishes_visible" -> "Possible dishes visible"
    "trash_visible" -> "Possible trash buildup"
    "laundry_visible" -> "Possible laundry to reset"
    "floor_clutter" -> "Possible floor clutter"
    "unmade_bed" -> "Possible bed reset"
    "bathroom_counter_mess" -> "Possible bathroom counter mess"
    "sink_full" -> "Possible full sink"
    "wipe_needed" -> "Possible surface wipe needed"
    else -> draft.label.ifBlank { "Possible room reset" }
}

private fun confidenceLabel(confidence: Float): String = when {
    confidence >= 0.7f -> "high"
    confidence >= 0.55f -> "medium"
    else -> "low"
}

@Composable
private fun confidenceColor(label: String) = when (label) {
    "high" -> Sage
    "medium" -> MutedOrange
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun scanDraftDifficulty(draft: ScanIssueDraft): String {
    val minutes = draft.minutes.toIntOrNull() ?: 5
    return when {
        draft.energy == "high" || minutes >= 30 -> "hard"
        draft.energy == "medium" || minutes >= 15 -> "medium"
        else -> "easy"
    }
}

@Composable
private fun RoomDetailScreen(id: String, state: TidyPilotState, nav: NavHostController) {
    val room = state.rooms.firstOrNull { it.id == id }
    if (room == null) {
        EmptyState("Room not found", "Return to your rooms and choose another room.") { nav.navigate(Route.Rooms.value) }
        return
    }
    val score = roomScore(room, state)
    val stats = roomStats(room, state)
    val roomTasks = state.tasks.filter { it.roomId == room.id }
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader(room.name, "Room score and next useful reset.") }
        item {
            StudioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RoomPreferences, null, tint = roomScoreColor(score), modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(score.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(score.reason, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${score.score}/100", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = roomScoreColor(score))
                }
                LinearProgressIndicator(progress = { score.score / 100f }, modifier = Modifier.fillMaxWidth())
                RoomStatGrid(
                    "Open tasks" to "${score.openTasks}",
                    "Overdue tasks" to "${score.overdueTasks}",
                    "Open scan issues" to "${score.openIssues}",
                    "Last reset" to score.lastCompletedLabel,
                    "Priority" to room.priority,
                    "Last scanned" to (stats.lastScanned ?: "Not yet")
                )
            }
        }
        item {
            StudioCard {
                Text("Room tasks", fontWeight = FontWeight.Black)
                if (roomTasks.isEmpty()) {
                    Text("No chores queued for this room.")
                } else {
                    roomTasks.take(6).forEach { itemTask ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(itemTask.name, fontWeight = FontWeight.SemiBold)
                                Text("${itemTask.estimatedMinutes} min - ${itemTask.energyRequired} energy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { nav.navigate("detail/task/${itemTask.id}") }) { Icon(Icons.Default.Edit, "Task detail") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailScreen(type: String, id: String, state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController, snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
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
                            "Snooze" to {
                                viewModel.snoozeTask(task)
                                scope.launch { snackbar.showSnackbar("Task snoozed.") }
                            },
                            "Edit" to { nav.navigate("editTask/${task.id}") },
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
                        Button(onClick = {
                            viewModel.addTasksFromScan(scan)
                            scope.launch { snackbar.showSnackbar("Scan tasks added.") }
                        }) { Text("Add tasks from scan") }
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stats = reportStats(state)
    val report = buildReport(state, stats)
    val csv = buildReportCsv(state, stats)
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Export/Reports", "Useful local summaries you can copy or save.") }
        if (state.completions.isEmpty() && state.scans.isEmpty()) {
            item { EmptyState("Nothing to report yet.", "Complete a few tasks to see progress.") }
        }
        item {
            ProgressGrid(
                "Completed this week" to "${stats.completedThisWeek}",
                "Best streak" to "${stats.bestStreak} days",
                "Average energy" to stats.averageEnergy,
                "Overdue/missed" to "${stats.overdueTasks.size}"
            )
        }
        item {
            StudioCard {
                Text("Rooms", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Most improved room: ${stats.mostImprovedRoom ?: "Not enough scan history yet."}")
                Text("Rooms needing attention: ${stats.roomsNeedingAttention.ifEmpty { listOf("None right now") }.joinToString()}")
                Text("Most consistent room: ${mostConsistentRoom(state) ?: "Not enough completion history."}")
            }
        }
        item {
            StudioCard {
                Text("Work and energy impact", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Average energy level: ${stats.averageEnergy}")
                Text("Work schedule impact: ${stats.workScheduleImpact}")
                Text("Day-off completions: ${stats.dayOffCompletions}")
                Text("Workday completions: ${stats.workdayCompletions}")
            }
        }
        item {
            StudioCard {
                Text("Missed and scan follow-up", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Missed/overdue tasks: ${stats.overdueTasks.ifEmpty { listOf("None") }.joinToString()}")
                Text("Scan issues detected: ${stats.scanIssuesDetected}")
                Text("Scan issues acted on: ${stats.scanIssuesActedOn}")
                Text("Scan-created tasks completed: ${stats.scanTasksCompleted}")
            }
        }
        item {
            StudioCard {
                Text("Local report preview", fontWeight = FontWeight.Black)
                Text(report)
                WrapButtons(
                    "Copy text summary" to {
                        clipboard.setText(AnnotatedString(report))
                        scope.launch { snackbar.showSnackbar("Report copied.") }
                    },
                    "Export local report as TXT" to {
                        val message = runCatching {
                            val file = writeLocalReport(context, "tidypilot_report_${LocalDate.now()}.txt", report)
                            "Saved TXT locally: ${file.name}"
                        }.getOrElse { "Could not save TXT report. Try again from Settings." }
                        scope.launch { snackbar.showSnackbar(message) }
                    },
                    "Export local report as CSV" to {
                        val message = runCatching {
                            val file = writeLocalReport(context, "tidypilot_report_${LocalDate.now()}.csv", csv)
                            "Saved CSV locally: ${file.name}"
                        }.getOrElse { "Could not save CSV report. Try again from Settings." }
                        scope.launch { snackbar.showSnackbar(message) }
                    }
                )
                Text("Exports are saved locally in the app's external files folder. Nothing is uploaded.")
            }
        }
        item {
            Button(onClick = { nav.navigate("results") }) { Text("Open latest scan result") }
        }
    }
}

@Composable
private fun LegacySettingsScreen(state: TidyPilotState, viewModel: TidyPilotViewModel) {
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
                FilledTonalButton(onClick = { viewModel.resetDemoData() }) { Icon(Icons.Default.RestartAlt, null); Spacer(Modifier.width(6.dp)); Text("Reset starter data") }
            }
        }
    }
}

@Composable
private fun LegacySettingsScreenV2(state: TidyPilotState, viewModel: TidyPilotViewModel) {
    val context = LocalContext.current
    var intensity by remember(state.settings) { mutableStateOf(state.settings.defaultCleaningIntensity) }
    var recovery by remember(state.settings) { mutableStateOf(state.settings.defaultRecoveryMinutesAfterWork.toString()) }
    var reminders by remember(state.remindersEnabled) { mutableStateOf(state.remindersEnabled) }
    var reminderTime by remember(state.settings) { mutableStateOf(state.settings.preferredReminderTime) }
    var quietStart by remember(state.settings) { mutableStateOf(state.settings.quietHoursStart) }
    var quietEnd by remember(state.settings) { mutableStateOf(state.settings.quietHoursEnd) }
    var lowEnergyMode by remember(state.settings) { mutableStateOf(state.settings.lowEnergyReminderMode) }
    var workdayBehavior by remember(state.settings) { mutableStateOf(state.settings.workdayReminderBehavior) }
    var dayOffBehavior by remember(state.settings) { mutableStateOf(state.settings.dayOffReminderBehavior) }
    var minimum by remember(state.settings) { mutableStateOf(state.settings.minimumExhaustedTaskMinutes.toString()) }
    var savePhotos by remember(state.savePhotosLocally) { mutableStateOf(state.savePhotosLocally) }
    var theme by remember(state.themeMode) { mutableStateOf(state.themeMode) }
    var permissionNote by remember { mutableStateOf("") }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionNote = if (granted) {
            reminders = true
            "Reminders can show at the selected time."
        } else {
            reminders = false
            "Notification permission was denied. Reminders stay off."
        }
    }

    fun enableReminders(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            reminders = enabled
            permissionNote = if (enabled) "Reminders are on. TidyPilot will keep them gentle." else "Reminders are off."
        }
    }

    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Settings", "Local preferences, reminders, privacy, and about.") }
        item {
            StudioCard {
                Text("Defaults", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Default cleaning intensity")
                OptionChips(listOf("gentle", "balanced", "deep"), intensity) { intensity = it }
                OutlinedTextField(recovery, { recovery = it.filter(Char::isDigit) }, label = { Text("Default work recovery minutes after shift") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(minimum, { minimum = it.filter(Char::isDigit) }, label = { Text("Minimum task size for exhausted days") }, modifier = Modifier.fillMaxWidth())
                PreferenceRow("Save photos locally", "Room photos stay on this device.", savePhotos) { savePhotos = it }
                Text("Theme")
                OptionChips(listOf("system", "light", "dark"), theme) { theme = it }
            }
        }
        item {
            StudioCard {
                Text("Reminders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                PreferenceRow("Enable reminders", "One gentle local reminder, never repeated spam.", reminders) { enableReminders(it) }
                if (permissionNote.isNotBlank()) Text(permissionNote, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(reminderTime, { reminderTime = it }, label = { Text("Reminder time HH:MM") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(quietStart, { quietStart = it }, label = { Text("Quiet start") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(quietEnd, { quietEnd = it }, label = { Text("Quiet end") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Text("Low-energy reminder mode")
                OptionChips(listOf("gentle", "5-minute reset", "off"), lowEnergyMode) { lowEnergyMode = it }
                Text("Workday reminder behavior")
                OptionChips(listOf("after shift", "before work", "manual only"), workdayBehavior) { workdayBehavior = it }
                Text("Day-off reminder behavior")
                OptionChips(listOf("morning reset", "midday nudge", "manual only"), dayOffBehavior) { dayOffBehavior = it }
                Text("Reminder copy preview", fontWeight = FontWeight.Bold)
                reminderCopyExamples(state, lowEnergyMode).forEach { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        item {
            StudioCard {
                Button(onClick = {
                    viewModel.updateSettings(
                        AppSettingsEntity(
                            defaultCleaningIntensity = intensity,
                            defaultRecoveryMinutesAfterWork = recovery.toIntOrNull() ?: 45,
                            reminderEnabled = reminders,
                            preferredReminderTime = reminderTime,
                            quietHoursStart = quietStart.ifBlank { "21:00" },
                            quietHoursEnd = quietEnd.ifBlank { "08:00" },
                            lowEnergyReminderMode = lowEnergyMode,
                            workdayReminderBehavior = workdayBehavior,
                            dayOffReminderBehavior = dayOffBehavior,
                            minimumExhaustedTaskMinutes = minimum.toIntOrNull() ?: 5,
                            savePhotosLocally = savePhotos,
                            themeMode = theme
                        ),
                        theme,
                        reminders,
                        savePhotos
                    )
                }) { Text("Save settings") }
                Text("Reminders are local and user-controlled. TidyPilot does not upload schedules or cleaning data.")
            }
        }
        item {
            StudioCard {
                Text("Privacy note", fontWeight = FontWeight.Black)
                Text("All cleaning plans, work schedules, energy check-ins, and room photos stay on device unless you explicitly approve network upload later. No account. No cloud. No tracking.")
                Text("Notification permission is requested only when reminders are enabled.")
            }
        }
        item {
            StudioCard {
                Text("About Smithware Studios", fontWeight = FontWeight.Black)
                Text("TidyPilot is a calm local-first home reset planner for real-life energy, shifts, skipped tasks, and small wins.")
                FilledTonalButton(onClick = { viewModel.resetDemoData() }) { Icon(Icons.Default.RestartAlt, null); Spacer(Modifier.width(6.dp)); Text("Reset starter data") }
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: TidyPilotState, viewModel: TidyPilotViewModel) {
    val context = LocalContext.current
    var actionNote by rememberSaveable { mutableStateOf("") }
    var intensity by remember(state.settings) { mutableStateOf(state.settings.defaultCleaningIntensity) }
    var accent by remember(state.settings) { mutableStateOf(state.settings.accentStyle) }
    var defaultEnergy by remember(state.settings) { mutableStateOf(state.settings.defaultEnergyLevel) }
    var defaultDuration by remember(state.settings) { mutableStateOf(state.settings.defaultTaskDurationMinutes.toString()) }
    var workdayBehavior by remember(state.settings) { mutableStateOf(state.settings.workdayPlanningBehavior) }
    var dayOffBehavior by remember(state.settings) { mutableStateOf(state.settings.dayOffPlanningBehavior) }
    var recovery by remember(state.settings) { mutableStateOf(state.settings.defaultRecoveryMinutesAfterWork.toString()) }
    var reminders by remember(state.remindersEnabled) { mutableStateOf(state.remindersEnabled) }
    var reminderTime by remember(state.settings) { mutableStateOf(state.settings.preferredReminderTime) }
    var quietStart by remember(state.settings) { mutableStateOf(state.settings.quietHoursStart) }
    var quietEnd by remember(state.settings) { mutableStateOf(state.settings.quietHoursEnd) }
    var savePhotos by remember(state.savePhotosLocally) { mutableStateOf(state.savePhotosLocally) }
    var theme by remember(state.themeMode) { mutableStateOf(state.themeMode) }
    var confirmDeleteAll by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        reminders = granted
        actionNote = if (granted) "Reminders enabled." else "Notification permission denied. Reminders stay off."
    }

    fun toggleReminders(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            reminders = enabled
            actionNote = if (enabled) "Reminders enabled." else "Reminders disabled."
        }
    }

    fun saveSettings() {
        viewModel.updateSettings(
            state.settings.copy(
                defaultCleaningIntensity = intensity,
                accentStyle = accent,
                defaultEnergyLevel = defaultEnergy,
                defaultTaskDurationMinutes = defaultDuration.toIntOrNull() ?: 10,
                workdayPlanningBehavior = workdayBehavior,
                dayOffPlanningBehavior = dayOffBehavior,
                defaultRecoveryMinutesAfterWork = recovery.toIntOrNull() ?: 45,
                reminderEnabled = reminders,
                preferredReminderTime = reminderTime.ifBlank { "18:30" },
                quietHoursStart = quietStart.ifBlank { "21:00" },
                quietHoursEnd = quietEnd.ifBlank { "08:00" },
                savePhotosLocally = savePhotos,
                themeMode = theme
            ),
            theme,
            reminders,
            savePhotos
        )
        actionNote = "Settings saved locally."
    }

    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Settings", "Local controls for how TidyPilot looks, plans, reminds, and stores data.") }
        item {
            StudioCard {
                Text("Appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Theme")
                OptionChips(listOf("system", "light", "dark"), theme) { theme = it }
                Text("Accent style")
                OptionChips(listOf("warm orange", "soft sage", "graphite"), accent) { accent = it }
            }
        }
        item {
            StudioCard {
                Text("Planning", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Default energy level")
                OptionChips(listOf("low", "medium", "high"), defaultEnergy) { defaultEnergy = it }
                OutlinedTextField(defaultDuration, { defaultDuration = it.filter(Char::isDigit) }, label = { Text("Default task duration minutes") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(recovery, { recovery = it.filter(Char::isDigit) }, label = { Text("Work recovery minutes") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Workday planning behavior")
                OptionChips(listOf("keep it light", "before work", "after shift"), workdayBehavior) { workdayBehavior = it }
                Text("Day-off planning behavior")
                OptionChips(listOf("allow bigger resets", "balanced", "gentle only"), dayOffBehavior) { dayOffBehavior = it }
            }
        }
        item {
            StudioCard {
                Text("Reminders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                PreferenceRow("Enable reminders", "One gentle reminder, quiet by default.", reminders) { toggleReminders(it) }
                OutlinedTextField(reminderTime, { reminderTime = it }, label = { Text("Reminder time HH:MM") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(quietStart, { quietStart = it }, label = { Text("Quiet start") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(quietEnd, { quietEnd = it }, label = { Text("Quiet end") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        }
        item {
            StudioCard {
                Text("Photos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                PreferenceRow("Save room photos", "Photos stay on this device for local scans.", savePhotos) { savePhotos = it }
                Text("Room photos are never uploaded. Scan results are local suggestions you can review before creating tasks.")
                FilledTonalButton(onClick = {
                    val deleted = deleteSavedScanPhotos(context)
                    viewModel.clearScanData()
                    actionNote = if (deleted) "Saved scan photos and scan results deleted." else "Scan results deleted. Some photo files may already be gone."
                }) { Text("Delete saved scan photos") }
            }
        }
        item {
            StudioCard {
                Text("Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                WrapButtons(
                    "Export data" to {
                        val stats = reportStats(state)
                        actionNote = runCatching {
                            val file = writeLocalReport(context, "tidypilot_data_export_${LocalDate.now()}.txt", buildReport(state, stats))
                            "Export saved locally: ${file.name}"
                        }.getOrElse { "Could not save export locally. Please try again." }
                    },
                    "Reset starter data" to {
                        viewModel.resetDemoData()
                        actionNote = "Starter data reset."
                    }
                )
                FilledTonalButton(onClick = { confirmDeleteAll = !confirmDeleteAll }) { Text(if (confirmDeleteAll) "Cancel delete" else "Delete all local data") }
                if (confirmDeleteAll) {
                    Text("This removes rooms, tasks, shifts, scans, reports, settings, and local onboarding state.")
                    Button(onClick = {
                        deleteSavedScanPhotos(context)
                        viewModel.deleteAllLocalData()
                        actionNote = "All local TidyPilot data deleted."
                        confirmDeleteAll = false
                    }) { Text("Confirm delete all local data") }
                }
            }
        }
        item {
            StudioCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrandMark(Modifier.size(44.dp))
                    Column {
                        Text("TidyPilot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text("Smithware Studios", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text("Version ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("A local-first chore planner for energy-aware home resets, room scans, and workday-friendly cleaning.")
                Text("Privacy: no login, no cloud, no tracking. Cleaning plans, schedules, check-ins, and room photos stay on this device unless you export them.")
            }
        }
        item {
            Button(onClick = { saveSettings() }, modifier = Modifier.fillMaxWidth()) { Text("Save settings") }
            if (actionNote.isNotBlank()) Text(actionNote, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun deleteSavedScanPhotos(context: Context): Boolean =
    runCatching { File(context.filesDir, "room_scans").deleteRecursively() }.getOrDefault(false)

private fun reminderCopyExamples(state: TidyPilotState, lowEnergyMode: String): List<String> {
    val room = state.rooms.minByOrNull { it.tidyScore }
    val roomTasks = room?.let { selected -> state.tasks.count { it.roomId == selected.id && it.estimatedMinutes <= 10 } } ?: 0
    val lowEnergy = if (lowEnergyMode == "off") "Reminder pauses when energy is low." else "Low energy? Try a 5-minute reset."
    return listOf(
        "Tiny reset time?",
        "One quick task can help.",
        room?.let { "Your ${it.name.lowercase()} has $roomTasks small task${if (roomTasks == 1) "" else "s"}." } ?: "Pick one small room reset.",
        lowEnergy
    )
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
private fun BrandMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.tidypilot_icon),
        contentDescription = "TidyPilot",
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Fit
    )
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
private fun EmptyState(title: String, body: String, actionLabel: String = "Start", action: (() -> Unit)? = null) {
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MutedOrange.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Home, null, tint = MutedOrange)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (action != null) {
            Button(onClick = action) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(actionLabel) }
        }
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
private fun RoomStatGrid(vararg items: Pair<String, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.toList().chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    Column(
                        Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(value, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PlanLabelChip(label: String) {
    Box(
        modifier = Modifier
            .background(
                color = if (label == PlanningEngine.LOW_ENERGY_TASK || label == PlanningEngine.QUICK_WIN) {
                    Sage.copy(alpha = 0.18f)
                } else {
                    MutedOrange.copy(alpha = 0.18f)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (label == PlanningEngine.LOW_ENERGY_TASK || label == PlanningEngine.QUICK_WIN) Sage else MutedOrange,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black
        )
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

private fun priorityScore(priority: String): Int = when (priority) {
    "urgent" -> 4
    "high" -> 3
    "normal" -> 2
    else -> 1
}

private fun taskMeta(task: CleaningTaskEntity, state: TidyPilotState): String {
    val room = state.rooms.firstOrNull { it.id == task.roomId }?.name ?: "Room"
    return "$room - ${task.estimatedMinutes} min - ${task.energyRequired} energy"
}

private fun planLabel(task: CleaningTaskEntity, state: TidyPilotState): String =
    PlanningEngine.displayLabelFor(
        task = task,
        room = state.rooms.firstOrNull { it.id == task.roomId },
        workStatus = state.todayPlan?.workStatus,
        energy = state.latestCheckIn?.energyLevel,
        availableMinutes = state.latestCheckIn?.availableMinutes ?: state.todayPlan?.availableMinutes ?: 15,
        today = state.today
    )

private data class RoomStats(
    val activeTasks: Int,
    val openIssues: Int,
    val scanCount: Int,
    val lastCleaned: String?,
    val lastScanned: String?,
    val safeToDelete: Boolean
)

private data class RoomScore(
    val score: Int,
    val label: String,
    val reason: String,
    val openTasks: Int,
    val overdueTasks: Int,
    val openIssues: Int,
    val lastCompletedLabel: String
)

private fun roomScore(room: RoomEntity, state: TidyPilotState): RoomScore {
    val score = calculateRoomScore(room, state.tasks, state.scans, state.issues, state.completions, state.today)
    return RoomScore(
        score = score.score,
        label = score.label,
        reason = score.reason,
        openTasks = score.openTasks,
        overdueTasks = score.overdueTasks,
        openIssues = score.openIssues,
        lastCompletedLabel = score.lastCompletedLabel
    )
}

@Composable
private fun roomScoreColor(score: RoomScore) = when (score.label) {
    "Good" -> Sage
    "Needs a quick reset" -> MutedOrange
    "Priority room" -> MutedOrange
    else -> MaterialTheme.colorScheme.error
}

private fun roomStats(room: RoomEntity, state: TidyPilotState): RoomStats {
    val taskIds = state.tasks.filter { it.roomId == room.id }.map { it.id }.toSet()
    val activeTasks = taskIds.size
    val roomScanIds = state.scans.filter { it.roomId == room.id }.map { it.id }.toSet()
    val openIssues = state.issues.count { it.scanId in roomScanIds }
    val lastCleaned = state.completions
        .filter { it.taskId in taskIds }
        .maxByOrNull { it.completedAt }
        ?.completedAt
        ?.format(DateTimeFormatter.ofPattern("MMM d"))
    val lastScanned = state.scans
        .filter { it.roomId == room.id }
        .maxByOrNull { it.scanDate }
        ?.scanDate
        ?.format(DateTimeFormatter.ofPattern("MMM d"))
    return RoomStats(
        activeTasks = activeTasks,
        openIssues = openIssues,
        scanCount = roomScanIds.size,
        lastCleaned = lastCleaned,
        lastScanned = lastScanned,
        safeToDelete = activeTasks == 0 && openIssues == 0 && roomScanIds.isEmpty()
    )
}

private fun cleanlinessStatus(score: Int): String = when {
    score >= 85 -> "Steady"
    score >= 70 -> "Mostly okay"
    score >= 55 -> "Needs attention"
    else -> "Reset needed"
}

private fun roomAttentionText(room: RoomEntity): String = when {
    room.tidyScore < 60 -> "Needs a small visible reset first."
    room.priority == "urgent" || room.priority == "high" -> "High priority room. Keep the next task manageable."
    room.tidyScore < 75 -> "Worth checking when you have a few minutes."
    else -> "Mostly steady. Maintain with a quick pass."
}

private fun workShiftImpact(state: TidyPilotState): String {
    val shift = state.todayShift ?: return "Free day planning"
    return "${shift.startTime} - ${shift.endTime}, ${shift.expectedExhaustionLevel} recovery"
}

private fun energyLabel(energy: String): String = when (energy) {
    "very low" -> "very low energy"
    "low" -> "low energy"
    "high" -> "ready for more"
    else -> "steady"
}

private fun energySuggestionCopy(energy: String): String = when (energy) {
    "very low" -> "Try trash, dishes, clear one surface, or a laundry switch."
    "low" -> "A five-to-ten minute reset is enough to keep momentum."
    "high" -> "TidyPilot can include deep clean tasks, overdue chores, or a room scan."
    else -> "A room reset or two to three short tasks is a practical target."
}

private fun contextCopy(workStatus: String, energy: String?): String = when {
    energy == "very low" || energy == "low" -> "Let's do the smallest useful reset."
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

private data class ReportStats(
    val completedThisWeek: Int,
    val mostImprovedRoom: String?,
    val roomsNeedingAttention: List<String>,
    val averageEnergy: String,
    val bestStreak: Int,
    val overdueTasks: List<String>,
    val workScheduleImpact: String,
    val dayOffCompletions: Int,
    val workdayCompletions: Int,
    val scanIssuesDetected: Int,
    val scanIssuesActedOn: Int,
    val scanTasksCompleted: Int
)

private fun reportStats(state: TidyPilotState): ReportStats {
    val today = LocalDate.now()
    val taskById = state.tasks.associateBy { it.id }
    val scanSuggestedTasks = state.tasks.filter { it.description.startsWith("Suggested from room scan") }
    val scanSuggestedIds = scanSuggestedTasks.map { it.id }.toSet()
    val roomScanHistory = state.scans.groupBy { it.roomId }
    val improved = roomScanHistory.mapNotNull { (roomId, scans) ->
        val ordered = scans.sortedBy { it.scanDate }
        val oldest = ordered.firstOrNull() ?: return@mapNotNull null
        val latest = ordered.lastOrNull() ?: return@mapNotNull null
        val delta = latest.tidyScore - oldest.tidyScore
        if (ordered.size >= 2 && delta > 0) state.rooms.firstOrNull { it.id == roomId }?.name to delta else null
    }.maxByOrNull { it.second }?.let { "${it.first} (+${it.second})" }
    val workDates = state.shifts.map { it.date }.toSet()
    val weeklyCompletions = state.completions.filter { it.completedAt.toLocalDate() >= today.minusDays(6) }
    val dayOffCompletions = weeklyCompletions.count { it.completedAt.toLocalDate() !in workDates }
    val workdayCompletions = weeklyCompletions.count { it.completedAt.toLocalDate() in workDates }
    return ReportStats(
        completedThisWeek = weeklyCompletions.size,
        mostImprovedRoom = improved,
        roomsNeedingAttention = state.rooms.filter { it.tidyScore < 70 }.sortedBy { it.tidyScore }.map { "${it.name} (${it.tidyScore}/100)" },
        averageEnergy = averageEnergyLabel(state.energy.take(14).map { it.energyLevel } + weeklyCompletions.map { it.energyLevelAtCompletion }),
        bestStreak = bestCleaningStreak(state.completions),
        overdueTasks = state.tasks
            .filter { task -> task.nextDueAt?.isBefore(today) == true || task.skippedCount > 0 }
            .sortedWith(compareByDescending<CleaningTaskEntity> { it.skippedCount }.thenBy { it.nextDueAt ?: today })
            .take(6)
            .map { task -> "${task.name}${task.nextDueAt?.let { " due $it" } ?: ""}" },
        workScheduleImpact = when {
            workdayCompletions > dayOffCompletions -> "Most progress happened on workdays, mostly through smaller tasks."
            dayOffCompletions > workdayCompletions -> "Most progress happened on days off, a good fit for bigger resets."
            weeklyCompletions.isEmpty() -> "No completions this week yet."
            else -> "Progress was balanced between workdays and days off."
        },
        dayOffCompletions = dayOffCompletions,
        workdayCompletions = workdayCompletions,
        scanIssuesDetected = state.issues.size,
        scanIssuesActedOn = scanSuggestedTasks.size,
        scanTasksCompleted = state.completions.count { it.taskId in scanSuggestedIds }
    )
}

private fun averageEnergyLabel(values: List<String>): String {
    if (values.isEmpty()) return "Not enough data"
    val score = values.map {
        when (it) {
            "very low" -> 1
            "low" -> 2
            "high" -> 4
            else -> 3
        }
    }.average()
    return when {
        score < 1.5 -> "very low"
        score < 2.5 -> "low"
        score < 3.5 -> "medium"
        else -> "high"
    }
}

private fun bestCleaningStreak(completions: List<com.smithware.tidypilot.data.TaskCompletionEntity>): Int {
    val dates = completions.map { it.completedAt.toLocalDate() }.toSet().sorted()
    if (dates.isEmpty()) return 0
    var best = 1
    var current = 1
    dates.zipWithNext().forEach { (previous, next) ->
        if (next == previous.plusDays(1)) {
            current++
            best = maxOf(best, current)
        } else {
            current = 1
        }
    }
    return best
}

private fun buildReport(state: TidyPilotState, stats: ReportStats): String = """
    TidyPilot weekly report
    Date: ${LocalDate.now()}
    Tasks completed this week: ${stats.completedThisWeek}
    Most improved room: ${stats.mostImprovedRoom ?: "Not enough scan history yet"}
    Rooms needing attention: ${stats.roomsNeedingAttention.ifEmpty { listOf("None right now") }.joinToString()}
    Average energy level: ${stats.averageEnergy}
    Best cleaning streak: ${stats.bestStreak} days
    Missed/overdue tasks: ${stats.overdueTasks.ifEmpty { listOf("None") }.joinToString()}
    Work schedule impact: ${stats.workScheduleImpact}
    Day-off completions: ${stats.dayOffCompletions}
    Workday completions: ${stats.workdayCompletions}
    Scan issues detected: ${stats.scanIssuesDetected}
    Scan issues acted on: ${stats.scanIssuesActedOn}
    Scan-created tasks completed: ${stats.scanTasksCompleted}
    Current average room tidy score: ${state.averageTidyScore}/100
    Current tidy streak: ${state.streak} days
""".trimIndent()

private fun buildReportCsv(state: TidyPilotState, stats: ReportStats): String {
    fun csv(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
    val rows = listOf(
        "metric,value",
        "Tasks completed this week,${csv(stats.completedThisWeek.toString())}",
        "Most improved room,${csv(stats.mostImprovedRoom ?: "Not enough scan history yet")}",
        "Rooms needing attention,${csv(stats.roomsNeedingAttention.ifEmpty { listOf("None right now") }.joinToString())}",
        "Average energy level,${csv(stats.averageEnergy)}",
        "Best cleaning streak,${csv("${stats.bestStreak} days")}",
        "Missed/overdue tasks,${csv(stats.overdueTasks.ifEmpty { listOf("None") }.joinToString())}",
        "Work schedule impact,${csv(stats.workScheduleImpact)}",
        "Day-off completions,${csv(stats.dayOffCompletions.toString())}",
        "Workday completions,${csv(stats.workdayCompletions.toString())}",
        "Scan issues detected,${csv(stats.scanIssuesDetected.toString())}",
        "Scan issues acted on,${csv(stats.scanIssuesActedOn.toString())}",
        "Scan-created tasks completed,${csv(stats.scanTasksCompleted.toString())}",
        "Average room tidy score,${csv("${state.averageTidyScore}/100")}"
    )
    return rows.joinToString("\n")
}

private fun writeLocalReport(context: Context, fileName: String, content: String): File {
    val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "reports").apply { mkdirs() }
    require(dir.exists() && dir.isDirectory) { "Report folder is not available." }
    return File(dir, fileName).apply { writeText(content) }
}

private fun createImageUri(context: Context): Uri {
    val dir = File(context.filesDir, "room_scans").apply { mkdirs() }
    require(dir.exists() && dir.isDirectory) { "Room scan folder is not available." }
    val file = File(dir, "tidypilot_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}


