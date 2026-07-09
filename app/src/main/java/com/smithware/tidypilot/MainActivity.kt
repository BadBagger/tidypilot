package com.smithware.tidypilot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RoomPreferences
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.Color
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
import com.smithware.tidypilot.data.ChoreLibraryItem
import com.smithware.tidypilot.data.CleaningTaskEntity
import com.smithware.tidypilot.data.CleaningSupplyEntity
import com.smithware.tidypilot.data.GuidedCleaningPlan
import com.smithware.tidypilot.data.PlanningEngine
import com.smithware.tidypilot.data.RoomEntity
import com.smithware.tidypilot.data.RoomPhotoScanEntity
import com.smithware.tidypilot.data.ScheduleImportCandidate
import com.smithware.tidypilot.data.ScheduleImportGuidance
import com.smithware.tidypilot.data.ScheduleImportGuidanceClassifier
import com.smithware.tidypilot.data.ScheduleImportParser
import com.smithware.tidypilot.data.ScanIssueEntity
import com.smithware.tidypilot.data.StarterRoutineProfile
import com.smithware.tidypilot.data.TaskNeedScore
import com.smithware.tidypilot.data.WorkShiftEntity
import com.smithware.tidypilot.data.calculateTaskNeedScore
import com.smithware.tidypilot.data.calculateRoomScore
import com.smithware.tidypilot.data.choreLibrary
import com.smithware.tidypilot.data.generateGuidedPlan
import com.smithware.tidypilot.data.guidedCleaningPlans
import com.smithware.tidypilot.data.libraryFrequencyToTaskFrequency
import com.smithware.tidypilot.data.libraryPriority
import com.smithware.tidypilot.data.oneThingWhy
import com.smithware.tidypilot.data.pipe
import com.smithware.tidypilot.data.premiumFeatures
import com.smithware.tidypilot.data.premiumPlans
import com.smithware.tidypilot.data.selectOneThingTask
import com.smithware.tidypilot.data.suggestedSupplyNames
import com.smithware.tidypilot.data.unpipe
import com.smithware.tidypilot.ui.theme.Charcoal
import com.smithware.tidypilot.ui.theme.CleanWhite
import com.smithware.tidypilot.ui.theme.Cream
import com.smithware.tidypilot.ui.theme.Graphite
import com.smithware.tidypilot.ui.theme.MutedOrange
import com.smithware.tidypilot.ui.theme.Sage
import com.smithware.tidypilot.ui.theme.TidyAqua
import com.smithware.tidypilot.ui.theme.TidyCoral
import com.smithware.tidypilot.ui.theme.TidyDeepTeal
import com.smithware.tidypilot.ui.theme.TidyLeaf
import com.smithware.tidypilot.ui.theme.TidyMint
import com.smithware.tidypilot.ui.theme.TidyPilotTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
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
    data object Todo : Route("todo", "Tasks", { Icon(Icons.Default.CheckCircle, null) })
    data object Add : Route("add", "Add/Edit", { Icon(Icons.Default.Add, null) })
    data object Rooms : Route("rooms", "Rooms", { Icon(Icons.Default.RoomPreferences, null) })
    data object Plan : Route("plan", "Plan", { Icon(Icons.Default.CalendarMonth, null) })
    data object HomeSetup : Route("homeSetup", "Home setup", { Icon(Icons.Default.Home, null) })
    data object OneThing : Route("oneThing", "One Thing", { Icon(Icons.Default.CheckCircle, null) })
    data object ChoreLibrary : Route("choreLibrary", "Chore library", { Icon(Icons.Default.CleaningServices, null) })
    data object Import : Route("import", "Import", { Icon(Icons.Default.CalendarMonth, null) })
    data object Reports : Route("reports", "Reports", { Icon(Icons.Default.FileDownload, null) })
    data object Settings : Route("settings", "Settings", { Icon(Icons.Default.Settings, null) })
    data object Premium : Route("premium", "Premium", { Icon(Icons.Default.AutoAwesome, null) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TidyPilotApp(state: TidyPilotState, viewModel: TidyPilotViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: Route.Dashboard.value
    val topLevelRoutes = listOf(Route.Dashboard.value, Route.Rooms.value, Route.Todo.value, Route.Plan.value, Route.Settings.value)
    val showBack = current !in topLevelRoutes
    if (!state.onboardingComplete) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            OnboardingScreen(state, viewModel, snackbar, Modifier.padding(padding))
        }
        return
    }
    Scaffold(
        topBar = {
            if (showBack) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            screenTitle(current),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            if (!nav.popBackStack()) nav.navigate(Route.Dashboard.value) { launchSingleTop = true }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                listOf(Route.Dashboard, Route.Rooms, Route.Todo, Route.Plan, Route.Settings).forEach { route ->
                    NavigationBarItem(
                        selected = current == route.value,
                        onClick = {
                            if (route == Route.Dashboard) {
                                nav.navigate(Route.Dashboard.value) {
                                    launchSingleTop = true
                                    restoreState = false
                                    popUpTo(Route.Dashboard.value) {
                                        inclusive = false
                                        saveState = false
                                    }
                                }
                            } else {
                                nav.navigate(route.value) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(Route.Dashboard.value) { saveState = true }
                                }
                            }
                        },
                        icon = route.icon,
                        label = { Text(route.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TidyDeepTeal,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = TidyMint,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = Route.Dashboard.value, modifier = Modifier.padding(padding)) {
            composable(Route.Dashboard.value) { DashboardScreen(state, viewModel, nav) }
            composable(Route.Todo.value) { TodoListScreen(state, viewModel, nav) }
            composable(Route.Add.value) { AddEditScreen(state, viewModel, snackbar) }
            composable("editTask/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                AddEditScreen(state, viewModel, snackbar, initialTaskId = entry.arguments?.getString("id"))
            }
            composable(Route.Rooms.value) { RoomManagementScreen(state, viewModel, snackbar, nav) }
            composable(Route.Plan.value) { PlanScreen(state, viewModel, nav) }
            composable(Route.HomeSetup.value) { HomeSetupScreen(state, viewModel, nav, snackbar) }
            composable(Route.OneThing.value) { OneThingScreen(state, viewModel, nav) }
            composable(Route.ChoreLibrary.value) { ChoreLibraryScreen(state, viewModel, nav, snackbar) }
            composable(Route.Import.value) { ScheduleImportScreen(state, viewModel, snackbar, nav) }
            composable(Route.Reports.value) { ReportsScreen(state, nav, viewModel, snackbar) }
            composable(Route.Settings.value) { SettingsScreen(state, viewModel, nav) }
            composable(Route.Premium.value) { PremiumScreen(state, viewModel) }
            composable("schedule") { WorkScheduleScreen(state, viewModel, snackbar, nav) }
            composable("energy") { EnergyCheckInScreen(state, viewModel, nav, snackbar) }
            composable("scan") { RoomPhotoScanScreen(state, viewModel, nav, snackbar) }
            composable("results") { PhotoResultsScreen(state, viewModel, nav, snackbar) }
            composable("detail/{type}/{id}", arguments = listOf(navArgument("type") { type = NavType.StringType }, navArgument("id") { type = NavType.StringType })) { entry ->
                val type = entry.arguments?.getString("type").orEmpty()
                val id = entry.arguments?.getString("id").orEmpty()
                if (type == "room") {
                    RoomDetailScreen(id, state, viewModel, nav, snackbar)
                } else {
                    DetailScreen(type, id, state, viewModel, nav, snackbar)
                }
            }
        }
    }
}

private fun screenTitle(route: String): String = when {
    route == Route.Import.value -> "Import schedule"
    route == Route.Todo.value -> "Tasks"
    route == Route.Plan.value -> "Plan"
    route == Route.Add.value -> "Quick add"
    route == Route.HomeSetup.value -> "Home setup"
    route == Route.OneThing.value -> "One Thing"
    route == Route.ChoreLibrary.value -> "Chore library"
    route == Route.Reports.value -> "Reports"
    route == Route.Premium.value -> "Premium"
    route == "schedule" -> "Work schedule"
    route == "energy" -> "Energy check-in"
    route == "scan" -> "Room scan"
    route == "results" -> "Scan results"
    route.startsWith("editTask") -> "Edit task"
    route.startsWith("detail/room") -> "Room detail"
    route.startsWith("detail/task") -> "Task detail"
    route.startsWith("detail") -> "Details"
    else -> "TidyPilot"
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
    var bedroomCount by rememberSaveable { mutableStateOf(1) }
    var bathroomCount by rememberSaveable { mutableStateOf(1) }
    var householdType by rememberSaveable { mutableStateOf("Just me") }
    var delegationInterest by rememberSaveable { mutableStateOf(false) }
    var goals by remember {
        mutableStateOf(setOf("Basic routine"))
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
                    Text("Tell TidyPilot about your home", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("This shapes the starter routine so you are not stuck building everything manually.")
                    CountStepper("Bedrooms", bedroomCount, 1, 6) { bedroomCount = it }
                    CountStepper("Bathrooms", bathroomCount, 1, 5) { bathroomCount = it }
                }
            }
            item {
                StudioCard {
                    Text("Rooms you want included", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Pick the rooms that matter now. You can add custom rooms later.")
                    StarterRoomChips(onboardingRoomOptions, starterRooms) { room ->
                        starterRooms = if (room in starterRooms) starterRooms - room else starterRooms + room
                    }
                }
            }
            item {
                StudioCard {
                    Text("Household", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    OptionChips(listOf("Just me", "Partner / roommate", "Family with kids"), householdType) { householdType = it }
                    PreferenceRow("Interested in chore delegation", "TidyPilot will mark a few simple tasks as easy to assign later.", delegationInterest) { delegationInterest = it }
                }
            }
            item {
                StudioCard {
                    Text("Main goals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Choose what you want help with first.")
                    StarterGoalChips(onboardingGoalOptions, goals) { goal ->
                        goals = if (goal in goals && goals.size > 1) goals - goal else goals + goal
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
            item {
                StudioCard {
                    Text("Starter routine", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(starterRoutinePreview(starterRooms, bedroomCount, bathroomCount, householdType, goals, delegationInterest))
                    Text("You can also browse the full local chore library after setup and add starter tasks by room.")
                    Text("Everything stays editable. Delete anything that does not fit your home.")
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
                            val selectedRooms = starterRooms.ifEmpty { defaultStarterRooms.toSet() }
                            viewModel.completeOnboarding(
                                selectedRooms,
                                reminders,
                                starterProfile = StarterRoutineProfile(
                                    selectedRooms = selectedRooms,
                                    bedroomCount = bedroomCount,
                                    bathroomCount = bathroomCount,
                                    householdType = householdType,
                                    goals = goals,
                                    delegationInterest = delegationInterest || "Family delegation" in goals
                                )
                            )
                            scope.launch { snackbar.showSnackbar("Starter routine added. You can edit anything later.") }
                        } else {
                            page++
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(if (setupPage) "Start with routine" else "Next") }
            }
        }
    }
}

private data class OnboardingPage(val title: String, val body: String)

private val defaultStarterRooms = listOf("Kitchen", "Bathroom", "Bedroom", "Living Room", "Laundry", "Entryway", "Basement")

private val onboardingRoomOptions = listOf("Kitchen", "Bathroom", "Bedroom", "Living Room", "Laundry", "Entryway", "Basement", "Office", "Garage", "Kids Room", "Playroom")

private val onboardingGoalOptions = listOf("Basic routine", "Catch up from mess", "Low energy maintenance", "Guest ready", "Workday friendly", "Family delegation")

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
private fun StarterGoalChips(goals: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        goals.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { goal ->
                    FilterChip(
                        selected = goal in selected,
                        onClick = { onToggle(goal) },
                        label = { Text(goal, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CountStepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Black)
            Text("$value included", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        FilledTonalButton(onClick = { onChange((value - 1).coerceAtLeast(min)) }, enabled = value > min, colors = tidyTonalButtonColors()) {
            Text("-")
        }
        Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        FilledTonalButton(onClick = { onChange((value + 1).coerceAtMost(max)) }, enabled = value < max, colors = tidyTonalButtonColors()) {
            Text("+")
        }
    }
}

private fun starterRoutinePreview(
    selectedRooms: Set<String>,
    bedroomCount: Int,
    bathroomCount: Int,
    householdType: String,
    goals: Set<String>,
    delegationInterest: Boolean
): String {
    val roomCount = selectedRooms.count { it !in setOf("Bedroom", "Bathroom") } +
        if ("Bedroom" in selectedRooms) bedroomCount.coerceAtLeast(1) else 0 +
        if ("Bathroom" in selectedRooms) bathroomCount.coerceAtLeast(1) else 0
    val goalText = goals.joinToString(", ").ifBlank { "Basic routine" }
    val delegationText = if (delegationInterest || householdType == "Family with kids" || "Family delegation" in goals) {
        " It will also flag a few simple chores as good assignment candidates."
    } else {
        ""
    }
    return "TidyPilot will create a starter plan for $roomCount room${if (roomCount == 1) "" else "s"} around: $goalText.$delegationText"
}

@Composable
private fun DashboardScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController) {
    val topTasks = topTodayTasks(state)
    val nextTask = topTasks.firstOrNull()
    val quickWin = quickWinTask(state)
    val attentionRooms = state.rooms
        .sortedWith(compareBy<RoomEntity> { roomScore(it, state).score }.thenByDescending { priorityScore(it.priority) })
        .take(3)
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.rooms.isEmpty() && state.tasks.none { !it.isArchived }) {
            item { TodayEmptySetupCard(viewModel, nav) }
            return@LazyColumn
        }
        item { HomeStatusCard(state) }
        item { OneThingEntryCard(nav) }
        item { TopTasksTodayCard(topTasks, state, viewModel, nav) }
        item { QuickWinTodayCard(quickWin, state, viewModel, nav) }
        item { RoomsNeedingAttentionCard(attentionRooms, state, nav) }
        item { CompletedTodayCard(state, nav) }
    }
}

@Composable
private fun OneThingEntryCard(nav: NavHostController) {
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(TidyMint.copy(alpha = 0.24f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = TidyDeepTeal)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Feeling overloaded?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Skip the list. TidyPilot can pick one useful reset.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(onClick = { nav.navigate(Route.OneThing.value) }, modifier = Modifier.fillMaxWidth(), colors = tidyButtonColors()) {
            Text("Give me one thing")
        }
    }
}

@Composable
private fun TodayEmptySetupCard(viewModel: TidyPilotViewModel, nav: NavHostController) {
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandMark(Modifier.size(44.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Set up your home once.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("TidyPilot will tell you what needs cleaning after that.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(onClick = { nav.navigate(Route.HomeSetup.value) }, modifier = Modifier.fillMaxWidth(), colors = tidyButtonColors()) {
            Text("Set up my home")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(onClick = { nav.navigate(Route.Add.value) }, modifier = Modifier.weight(1f), colors = tidyTonalButtonColors()) {
                Text("Add a quick task")
            }
            FilledTonalButton(onClick = { viewModel.applyStarterRoutine() }, modifier = Modifier.weight(1f), colors = tidyTonalButtonColors()) {
                Text("Use starter template")
            }
        }
    }
}

@Composable
private fun HomeStatusCard(state: TidyPilotState) {
    val status = homeStatus(state)
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(58.dp)
                    .background(status.color.copy(alpha = 0.16f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("${status.score}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = status.color)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Home status", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(status.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(status.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LinearProgressIndicator(
            progress = { status.score / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = status.color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun TopTasksTodayCard(
    tasks: List<CleaningTaskEntity>,
    state: TidyPilotState,
    viewModel: TidyPilotViewModel,
    nav: NavHostController
) {
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Top 3 tasks today", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Chosen by urgency, room need, due date, and effort.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (tasks.isNotEmpty()) {
                Button(onClick = { nav.navigate("detail/task/${tasks.first().id}") }, colors = tidyButtonColors()) {
                    Text("Start next task")
                }
            }
        }
        if (tasks.isEmpty()) {
            Text("No chores queued.", fontWeight = FontWeight.Black)
            Text("Add a task or use the starter template to build your first plan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            tasks.take(3).forEachIndexed { index, task ->
                TodayTaskRow(index + 1, task, state, viewModel, nav)
            }
        }
    }
}

@Composable
private fun TodayTaskRow(
    rank: Int,
    task: CleaningTaskEntity,
    state: TidyPilotState,
    viewModel: TidyPilotViewModel,
    nav: NavHostController
) {
    val urgency = taskUrgency(task, state)
    val need = taskNeedScore(task, state)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(urgency.color.copy(alpha = 0.16f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("$rank", color = urgency.color, fontWeight = FontWeight.Black)
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(task.name, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    taskMeta(task, state),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                NeedStatusLine(need)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .background(needStatusColor(need).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text(
                    needFriendlyLabel(need.status),
                    color = needStatusColor(need),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FilledTonalIconButton(
                onClick = { viewModel.markComplete(task) },
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = TidyLeaf.copy(alpha = 0.18f))
            ) {
                Icon(Icons.Default.CheckCircle, "Complete", tint = TidyLeaf)
            }
            FilledTonalIconButton(
                onClick = { nav.navigate("detail/task/${task.id}") },
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.Edit, "Task detail")
            }
        }
    }
}

@Composable
private fun QuickWinTodayCard(
    task: CleaningTaskEntity?,
    state: TidyPilotState,
    viewModel: TidyPilotViewModel,
    nav: NavHostController
) {
    StudioCard {
        Text("One Quick Win", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        if (task == null) {
            Text("No 2-5 minute task is ready yet.", fontWeight = FontWeight.Black)
            Text("Add a tiny reset like trash, sink, counter, or laundry switch.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton(onClick = { nav.navigate(Route.Add.value) }, colors = tidyTonalButtonColors()) {
                Text("Add a quick task")
            }
        } else {
            val need = taskNeedScore(task, state)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.CleaningServices, null, tint = TidyAqua, modifier = Modifier.size(30.dp))
                Column(Modifier.weight(1f)) {
                    Text(task.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(taskMeta(task, state), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    NeedStatusLine(need)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { viewModel.markComplete(task) }, modifier = Modifier.weight(1f), colors = tidyButtonColors()) {
                    Text("Done")
                }
                FilledTonalButton(onClick = { nav.navigate("detail/task/${task.id}") }, modifier = Modifier.weight(1f), colors = tidyTonalButtonColors()) {
                    Text("Open")
                }
            }
        }
    }
}

@Composable
private fun CompletedTodayCard(state: TidyPilotState, nav: NavHostController) {
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(46.dp)
                    .background(TidyLeaf.copy(alpha = 0.16f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = TidyLeaf)
            }
            Column(Modifier.weight(1f)) {
                Text("Completed today", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("${state.completedTodayCount} reset${if (state.completedTodayCount == 1) "" else "s"} finished", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = { nav.navigate(Route.Todo.value) }, colors = tidyTonalButtonColors()) {
                Text("Tasks")
            }
        }
        Text(
            if (state.completedTodayCount == 0) "Start with one doable task. A small reset still counts." else "You made progress today.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OneThingScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController) {
    var selectedMode by rememberSaveable { mutableStateOf<String?>(null) }
    var excludedTaskIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var completedTaskName by rememberSaveable { mutableStateOf<String?>(null) }
    val mode = selectedMode
    val minutes = oneThingMinutes(mode)
    val energy = oneThingEnergy(mode, state)
    val selectedTask = if (mode == null) null else selectOneThingTask(
        tasks = state.tasks,
        rooms = state.rooms,
        completions = state.completions,
        today = state.today,
        availableMinutes = minutes,
        energyLevel = energy,
        excludedTaskIds = excludedTaskIds
    )

    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CompactBrandHeader("One Thing", "One useful reset. No giant checklist.") }
        if (completedTaskName != null) {
            item {
                StudioCard {
                    Text("Nice. ${completedTaskName} is handled.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Want one more or done for now?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                completedTaskName = null
                                selectedMode = null
                                excludedTaskIds = emptySet()
                            },
                            modifier = Modifier.weight(1f),
                            colors = tidyButtonColors()
                        ) {
                            Text("One more")
                        }
                        FilledTonalButton(
                            onClick = { nav.navigate(Route.Dashboard.value) { popUpTo(Route.Dashboard.value) } },
                            modifier = Modifier.weight(1f),
                            colors = tidyTonalButtonColors()
                        ) {
                            Text("Done for now")
                        }
                    }
                }
            }
        } else if (mode == null) {
            item {
                StudioCard {
                    Text("How much energy do you have?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("Pick what feels realistic. TidyPilot will choose just one task.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    WrapButtons(
                        "2 minutes" to { selectedMode = "2 minutes" },
                        "5 minutes" to { selectedMode = "5 minutes" },
                        "15 minutes" to { selectedMode = "15 minutes" },
                        "Full reset" to { selectedMode = "Full reset" }
                    )
                }
            }
        } else {
            item {
                OneThingTaskCard(
                    task = selectedTask,
                    state = state,
                    mode = mode,
                    energy = energy,
                    viewModel = viewModel,
                    nav = nav,
                    onComplete = { task ->
                        viewModel.markComplete(task)
                        completedTaskName = task.name
                        excludedTaskIds = excludedTaskIds + task.id
                    },
                    onSkip = { task ->
                        viewModel.skipTask(task)
                        excludedTaskIds = excludedTaskIds + task.id
                    },
                    onPickAnother = { task ->
                        excludedTaskIds = excludedTaskIds + task.id
                    },
                    onChangeMode = {
                        selectedMode = null
                        excludedTaskIds = emptySet()
                    }
                )
            }
        }
    }
}

@Composable
private fun OneThingTaskCard(
    task: CleaningTaskEntity?,
    state: TidyPilotState,
    mode: String,
    energy: String,
    viewModel: TidyPilotViewModel,
    nav: NavHostController,
    onComplete: (CleaningTaskEntity) -> Unit,
    onSkip: (CleaningTaskEntity) -> Unit,
    onPickAnother: (CleaningTaskEntity) -> Unit,
    onChangeMode: () -> Unit
) {
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Your one thing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("$mode - ${energyLabel(energy)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onChangeMode) { Text("Change") }
        }
        if (task == null) {
            Text("No task fits that window right now.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("Try a little more time, add a quick task, or scan a room to create fresh suggestions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            WrapButtons(
                "Pick again" to onChangeMode,
                "Add task" to { nav.navigate(Route.Add.value) },
                "Scan room" to { nav.navigate("scan") }
            )
        } else {
            val room = state.rooms.firstOrNull { it.id == task.roomId }
            val need = taskNeedScore(task, state)
            Text(task.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            RoomStatGrid(
                "Room" to (room?.name ?: "Room"),
                "Time" to "${task.estimatedMinutes} min",
                "Need" to needFriendlyLabel(need.status),
                "Energy" to task.energyRequired
            )
            Text(oneThingWhy(task, room, need), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(need.explanation, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { nav.navigate("detail/task/${task.id}") }, modifier = Modifier.fillMaxWidth(), colors = tidyButtonColors()) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Start")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = { onComplete(task) }, modifier = Modifier.weight(1f), colors = tidyTonalButtonColors()) {
                    Text("Complete")
                }
                FilledTonalButton(onClick = { onSkip(task) }, modifier = Modifier.weight(1f), colors = tidyTonalButtonColors()) {
                    Text("Skip")
                }
            }
            TextButton(onClick = { onPickAnother(task) }, modifier = Modifier.fillMaxWidth()) {
                Text("Pick another")
            }
        }
    }
}

@Composable
private fun TodayFocusCard(
    state: TidyPilotState,
    nextTask: CleaningTaskEntity?,
    attentionRoom: RoomEntity?,
    nav: NavHostController,
    viewModel: TidyPilotViewModel
) {
    val overdue = overdueTasks(state)
    val fiveMinuteTask = fiveMinuteTask(state)
    val roomScore = attentionRoom?.let { roomScore(it, state) }
    StudioCard {
        Text("Today at a glance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text("TidyPilot tells you what actually needs cleaning today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        RoomStatGrid(
            "Needs attention" to (attentionRoom?.name ?: "No room yet"),
            "Overdue" to if (overdue.isEmpty()) "None" else "${overdue.size} chores",
            "5-minute option" to (fiveMinuteTask?.name ?: "Quick scan"),
            "Completed today" to "${state.completedTodayCount}"
        )
        if (attentionRoom != null && roomScore != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .background(roomScoreColor(roomScore).copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(roomScore.label, color = roomScoreColor(roomScore), fontWeight = FontWeight.Black)
                }
                Text("${attentionRoom.name}: ${roomScore.reason}", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (fiveMinuteTask != null) nav.navigate("detail/task/${fiveMinuteTask.id}") else {
                        viewModel.quickClean(5, "low")
                        nav.navigate(Route.Todo.value)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = tidyButtonColors()
            ) {
                Text("Start 5-minute reset", maxLines = 2, overflow = TextOverflow.Clip)
            }
            FilledTonalButton(
                onClick = { nav.navigate(Route.Todo.value) },
                modifier = Modifier.weight(1f),
                colors = tidyTonalButtonColors()
            ) {
                Text("Open tasks")
            }
        }
        if (nextTask == null) {
            Text("No chores queued. Add a task or scan a room when you are ready.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    recommendedTask: CleaningTaskEntity?,
    onStartRecommended: () -> Unit,
    onOpenRecommended: () -> Unit,
    onScan: () -> Unit,
    onReplan: () -> Unit
) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.background(Brush.linearGradient(listOf(Cream, Color(0xFFE8FFF6), Color(0xFFD7F7EE)))).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrandMark(Modifier.size(34.dp))
                    Column {
                        Text("TidyPilot", color = Graphite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text("Calm home control", color = TidyDeepTeal, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Text(date.uppercase(), color = MutedOrange, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    }
                }
                Text(title, color = Graphite, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardMetricPill("Plan", planType, Modifier.weight(1.3f))
                    DashboardMetricPill("Energy", energy, Modifier.weight(1f))
                    DashboardMetricPill("Time", "$minutes min", Modifier.weight(1f))
                }
                Button(
                    onClick = { if (recommendedTask == null) onReplan() else onStartRecommended() },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
                    colors = tidyButtonColors()
                ) {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (recommendedTask == null) "Build today's reset" else "Start recommended reset")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(onClick = onScan, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), colors = tidyTonalButtonColors()) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Scan room")
                    }
                    FilledTonalButton(onClick = onReplan, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), colors = tidyTonalButtonColors()) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Check in")
                    }
                }
                if (recommendedTask != null) {
                    TextButton(onClick = onOpenRecommended) {
                        Text("View task details", color = TidyDeepTeal)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayDecisionCard(state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController) {
    val energy = state.latestCheckIn?.energyLevel ?: state.settings.defaultEnergyLevel
    StudioCard {
        Text("What are you up for?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text("Pick a time window. TidyPilot will rebuild the plan around that instead of dumping every chore here.")
        WrapButtons(
            "5 min" to { viewModel.quickClean(5, "low"); nav.navigate(Route.Todo.value) },
            "10 min" to { viewModel.quickClean(10, energy); nav.navigate(Route.Todo.value) },
            "15 min" to { viewModel.quickClean(15, energy); nav.navigate(Route.Todo.value) },
            "30 min" to { viewModel.quickClean(30, "high"); nav.navigate(Route.Todo.value) },
            "I'm exhausted" to { viewModel.replan(exhausted = true); nav.navigate(Route.Todo.value) },
            "Scan room" to { nav.navigate("scan") }
        )
        Text("A small reset still counts. You can change the plan anytime.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TodoListScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController) {
    val todayTasks = state.suggestedTasks
    val energy = state.latestCheckIn?.energyLevel ?: state.settings.defaultEnergyLevel
    val energyTasks = energyTodoTasks(state, energy).filter { task -> todayTasks.none { it.id == task.id } }
    val overdue = overdueTasks(state)
    val openTasks = (todayTasks + energyTasks + state.tasks)
        .filter { !it.isArchived }
        .distinctBy { it.id }
        .sortedWith(compareBy<CleaningTaskEntity> { it.nextDueAt ?: state.today }.thenBy { it.estimatedMinutes })
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CompactBrandHeader("Tasks", "Chores, quick wins, and recurring routines.") }
        item {
            StudioCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Today's task list", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(state.todayPlan?.adaptedReason ?: "Pick a time window to build a focused mini plan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = { nav.navigate(Route.Add.value) }, colors = tidyTonalButtonColors()) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
                WrapButtons(
                    "Browse chore library" to { nav.navigate(Route.ChoreLibrary.value) },
                    "5 min" to { viewModel.quickClean(5, "low") },
                    "10 min" to { viewModel.quickClean(10, energy) },
                    "Check in" to { nav.navigate("energy") }
                )
            }
        }
        if (overdue.isNotEmpty()) {
            item { SectionHeader("Overdue", "Start small. These need attention, not panic.") }
            items(overdue.take(4), key = { "overdue-${it.id}" }) { task -> PlanTaskCard(task, state, viewModel, nav) }
        }
        if (todayTasks.isNotEmpty()) {
            item { SectionHeader("Recommended next", "Start with these before browsing the full list.") }
            items(todayTasks.take(5), key = { "today-${it.id}" }) { task -> PlanTaskCard(task, state, viewModel, nav) }
        }
        item { SectionHeader("All open chores", "Grouped into one clear list. Complete, skip, or open details.") }
        if (openTasks.isEmpty()) {
            item { EmptyState("No chores queued.", "Add a task or run a quick room scan.", "Add task") { nav.navigate(Route.Add.value) } }
        } else {
            items(openTasks, key = { "open-${it.id}" }) { task -> TodoTaskRow(task, state, viewModel, nav) }
        }
    }
}

@Composable
private fun PlanScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController) {
    val plan = state.todayPlan
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CompactBrandHeader("Plan", "Schedules, routines, quick clean, and progress.") }
        item {
            StudioCard {
                Text("How today is planned", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(plan?.adaptedReason ?: "TidyPilot uses your rooms, tasks, energy, and work schedule to pick a practical reset.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                RoomStatGrid(
                    "Energy" to energyLabel(state.latestCheckIn?.energyLevel ?: state.settings.defaultEnergyLevel),
                    "Available" to "${state.latestCheckIn?.availableMinutes ?: plan?.availableMinutes ?: 15} min",
                    "Work" to dashboardWorkStatusLabel(plan?.workStatus ?: state.todayShift?.let { "working today" } ?: "free day", state),
                    "Open chores" to "${state.tasks.count { !it.isArchived }}"
                )
                WrapButtons(
                    "Energy check-in" to { nav.navigate("energy") },
                    "Work schedule" to { nav.navigate("schedule") },
                    "Import schedule" to { nav.navigate(Route.Import.value) },
                    "Reports" to { nav.navigate(Route.Reports.value) }
                )
            }
        }
        item { SectionHeader("Guided cleaning plans", "Pick a mission, skip rooms that do not apply, and save the checklist as normal tasks.") }
        items(guidedCleaningPlans, key = { it.id }) { template ->
            GuidedPlanCard(template, state, viewModel)
        }
        item { QuickCleanCard(state, viewModel) }
        item { RoutineAutopilotCard(state, viewModel, nav) }
        item { RecentCompletionsCard(state) }
        if (state.shifts.isEmpty()) {
            item { ScheduleImportPromptCard(nav) }
        }
    }
}

@Composable
private fun TodoTaskRow(task: CleaningTaskEntity, state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController) {
    val need = taskNeedScore(task, state)
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CheckCircle, null, tint = taskEnergyColor(task), modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(task.name, fontWeight = FontWeight.Black)
                Text(taskMeta(task, state), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                NeedStatusLine(need)
            }
            NeedStatusPill(need)
            IconButton(onClick = { viewModel.markComplete(task) }) { Icon(Icons.Default.CheckCircle, "Complete", tint = TidyLeaf) }
            IconButton(onClick = { nav.navigate("detail/task/${task.id}") }) { Icon(Icons.Default.Edit, "Details") }
        }
    }
}

@Composable
private fun GuidedPlanCard(template: GuidedCleaningPlan, state: TidyPilotState, viewModel: TidyPilotViewModel) {
    var expanded by rememberSaveable(template.id) { mutableStateOf(false) }
    var onlyThirty by rememberSaveable(template.id) { mutableStateOf(false) }
    var spreadDays by rememberSaveable(template.id) { mutableStateOf(1) }
    var includedRooms by rememberSaveable(template.id) { mutableStateOf(template.suggestedRooms.toSet()) }
    val generated = generateGuidedPlan(
        plan = template,
        existingRooms = state.rooms,
        includedRoomCategories = includedRooms,
        onlyThirtyMinutes = onlyThirty,
        spreadDays = spreadDays,
        today = state.today
    )
    StudioCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.CleaningServices, null, tint = if (template.id.contains("guest") || template.id.contains("holiday")) MutedOrange else TidyAqua, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(template.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(template.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        RoomStatGrid(
            "Checklist" to "${generated.tasks.size} steps",
            "Total" to "${generated.totalMinutes} min",
            "Pace" to if (spreadDays == 3) "3 days" else "Today",
            "Mode" to if (onlyThirty) "30 min" else "Full"
        )
        if (expanded) {
            Text("Skip irrelevant rooms", fontWeight = FontWeight.Bold)
            CompactChoiceToggles(template.suggestedRooms, includedRooms) { room ->
                includedRooms = if (room in includedRooms && includedRooms.size > 1) includedRooms - room else includedRooms + room
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = onlyThirty,
                    onClick = { onlyThirty = !onlyThirty },
                    label = { Text("I only have 30 minutes") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = spreadDays == 3,
                    onClick = { spreadDays = if (spreadDays == 3) 1 else 3 },
                    label = { Text("Spread over 3 days") },
                    modifier = Modifier.weight(1f)
                )
            }
            Text("Checklist preview", fontWeight = FontWeight.Bold)
            generated.tasks.take(8).forEach { task ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CheckCircle, null, tint = TidyLeaf, modifier = Modifier.size(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(task.name, fontWeight = FontWeight.SemiBold)
                        Text("${task.estimatedMinutes} min - due ${task.nextDueAt}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (generated.tasks.size > 8) {
                Text("+${generated.tasks.size - 8} more small steps", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = { viewModel.saveHomeSetup(generated.rooms, generated.tasks) },
                modifier = Modifier.fillMaxWidth(),
                enabled = generated.tasks.isNotEmpty(),
                colors = tidyButtonColors()
            ) {
                Text("Save checklist")
            }
            Text("Progress is saved through the generated tasks. Complete or skip steps from Tasks like any other chore.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(if (expanded) "Hide plan" else "Open plan")
        }
    }
}

@Composable
private fun CompactChoiceToggles(options: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    options.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            row.forEach { option ->
                FilterChip(
                    selected = option in selected,
                    onClick = { onToggle(option) },
                    label = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ChoreLibraryScreen(
    state: TidyPilotState,
    viewModel: TidyPilotViewModel,
    nav: NavHostController,
    snackbar: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var roomFilter by rememberSaveable { mutableStateOf("All") }
    var frequencyFilter by rememberSaveable { mutableStateOf("All") }
    var hiddenKeys by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val rooms = listOf("All") + choreLibrary.map { it.roomCategory }.distinct()
    val frequencies = listOf("All", "Daily", "Every few days", "Weekly", "Biweekly", "Monthly", "Seasonal", "Annual", "As needed")
    val visible = choreLibrary
        .filter { roomFilter == "All" || it.roomCategory == roomFilter }
        .filter { frequencyFilter == "All" || it.suggestedFrequency == frequencyFilter }
        .filter { choreLibraryKey(it) !in hiddenKeys }
        .sortedWith(compareBy<ChoreLibraryItem> { it.roomCategory }.thenBy { frequencyOrder(it.suggestedFrequency) }.thenBy { it.name })

    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CompactBrandHeader("Chore library", "Local starter chores. Free, editable, and optional.") }
        item {
            StudioCard {
                Text("Browse starter tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Pick by room or rhythm. Added chores become normal tasks you can edit, hide, or delete later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Room", fontWeight = FontWeight.Bold)
                CompactChoiceChips(rooms, roomFilter) { roomFilter = it }
                Text("Frequency", fontWeight = FontWeight.Bold)
                CompactChoiceChips(frequencies, frequencyFilter) { frequencyFilter = it }
            }
        }
        if (visible.isEmpty()) {
            item {
                EmptyState("No chores showing.", "Clear filters or restore hidden library items.", "Show hidden") {
                    hiddenKeys = emptySet()
                }
            }
        } else {
            items(visible, key = { choreLibraryKey(it) }) { item ->
                ChoreLibraryCard(
                    item = item,
                    state = state,
                    onAdd = { editAfter ->
                        val pair = buildLibraryTask(item, state)
                        viewModel.saveHomeSetup(listOf(pair.first), listOf(pair.second))
                        scope.launch { snackbar.showSnackbar("${item.name} added.") }
                        if (editAfter) nav.navigate("editTask/${pair.second.id}")
                    },
                    onHide = {
                        hiddenKeys = hiddenKeys + choreLibraryKey(item)
                        scope.launch { snackbar.showSnackbar("${item.name} hidden from this list.") }
                    }
                )
            }
        }
    }
}

@Composable
private fun ChoreLibraryCard(
    item: ChoreLibraryItem,
    state: TidyPilotState,
    onAdd: (Boolean) -> Unit,
    onHide: () -> Unit
) {
    val alreadyAdded = state.tasks.any { task ->
        !task.isArchived &&
            task.name.equals(item.name, ignoreCase = true) &&
            (state.rooms.firstOrNull { it.id == task.roomId }?.name?.let { roomMatchesLibraryCategory(it, item.roomCategory) } == true)
    }
    StudioCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.CleaningServices, null, tint = if (item.hygieneImportance == "high") MutedOrange else TidyAqua, modifier = Modifier.size(26.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("${item.roomCategory} - ${item.suggestedFrequency} - ${item.estimatedMinutes} min", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (alreadyAdded) {
                Box(Modifier.background(TidyLeaf.copy(alpha = 0.14f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text("Added", color = TidyLeaf, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                }
            }
        }
        RoomStatGrid(
            "Effort" to item.effortLevel,
            "Hygiene" to item.hygieneImportance,
            "Clutter" to item.clutterImpact,
            "Seasonal" to if (item.seasonal) "Yes" else "No"
        )
        if (item.suppliesNeeded.isNotBlank()) {
            Text("Supplies: ${item.suppliesNeeded}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onAdd(false) }, enabled = !alreadyAdded, modifier = Modifier.weight(1f), colors = tidyButtonColors()) {
                Text(if (alreadyAdded) "Added" else "Add")
            }
            FilledTonalButton(onClick = { onAdd(true) }, enabled = !alreadyAdded, modifier = Modifier.weight(1f), colors = tidyTonalButtonColors()) {
                Text("Add + edit")
            }
            TextButton(onClick = onHide, modifier = Modifier.weight(0.8f)) {
                Text("Hide")
            }
        }
    }
}

private fun choreLibraryKey(item: ChoreLibraryItem): String = "${item.roomCategory}|${item.name}|${item.suggestedFrequency}"

private fun frequencyOrder(frequency: String): Int = when (frequency) {
    "Daily" -> 1
    "Every few days" -> 2
    "Weekly" -> 3
    "Biweekly" -> 4
    "Monthly" -> 5
    "Seasonal" -> 6
    "Annual" -> 7
    "As needed" -> 8
    else -> 99
}

private fun buildLibraryTask(item: ChoreLibraryItem, state: TidyPilotState): Pair<RoomEntity, CleaningTaskEntity> {
    val room = state.rooms.firstOrNull { roomMatchesLibraryCategory(it.name, item.roomCategory) }
        ?: RoomEntity(
            name = if (item.roomCategory == "Whole home") "Whole home" else item.roomCategory,
            roomType = item.roomCategory,
            iconName = setupIconName(item.roomCategory),
            priority = if (item.hygieneImportance == "high") "high" else "normal",
            defaultTaskIntensity = item.effortLevel,
            defaultTaskFrequency = libraryFrequencyToTaskFrequency(item.suggestedFrequency),
            notes = "Added from chore library."
        )
    val task = CleaningTaskEntity(
        name = item.name,
        roomId = room.id,
        description = buildString {
            append("Added from TidyPilot chore library.")
            if (item.suppliesNeeded.isNotBlank()) append(" Supplies: ${item.suppliesNeeded}.")
            append(" Hygiene importance: ${item.hygieneImportance}. Clutter impact: ${item.clutterImpact}.")
        },
        priority = libraryPriority(item),
        estimatedMinutes = item.estimatedMinutes,
        difficulty = item.effortLevel,
        energyRequired = item.effortLevel,
        frequencyType = libraryFrequencyToTaskFrequency(item.suggestedFrequency),
        preferredTime = if (item.seasonal || item.estimatedMinutes >= 30) "day off" else "anytime",
        isQuickResetTask = item.estimatedMinutes <= 5,
        isDeepCleanTask = item.estimatedMinutes >= 30,
        photoDetectableCategory = item.category,
        nextDueAt = LocalDate.now()
    )
    return room to task
}

private fun roomMatchesLibraryCategory(roomName: String, category: String): Boolean {
    val room = roomName.lowercase().replace(" area", "").trim()
    val cat = category.lowercase().replace(" area", "").trim()
    return room == cat ||
        cat == "laundry" && room == "laundry" ||
        cat == "living room" && room in setOf("living room", "living") ||
        cat == "whole home" && room in setOf("whole home", "home", "entryway")
}

@Composable
private fun NeedStatusLine(need: TaskNeedScore) {
    Text(
        "${need.score}/100 - ${need.explanation}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun NeedStatusPill(need: TaskNeedScore) {
    Box(
        Modifier
            .background(needStatusColor(need).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            needFriendlyLabel(need.status),
            color = needStatusColor(need),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeSetupScreen(
    state: TidyPilotState,
    viewModel: TidyPilotViewModel,
    nav: NavHostController,
    snackbar: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableStateOf(0) }
    var homeType by rememberSaveable { mutableStateOf("Apartment") }
    var selectedRooms by rememberSaveable { mutableStateOf(defaultSetupRooms("Apartment")) }
    var customRoomName by rememberSaveable { mutableStateOf("") }
    var selectedTaskKeys by rememberSaveable { mutableStateOf(defaultSetupTaskKeys(selectedRooms)) }
    var cleaningStyle by rememberSaveable { mutableStateOf("Little every day") }
    val roomNames = setupRoomNames(selectedRooms, customRoomName)
    val preview = buildSetupPlan(roomNames, selectedTaskKeys, cleaningStyle, state)

    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CompactBrandHeader("Home setup", "Pick rooms once. TidyPilot builds the starter routine.") }
        item {
            StudioCard {
                Text("Step ${step + 1} of 5", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { (step + 1) / 5f }, modifier = Modifier.fillMaxWidth(), color = TidyAqua)
            }
        }
        item {
            when (step) {
                0 -> StudioCard {
                    Text("Choose home type", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("This only sets smart defaults. You can change every room next.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SetupChoiceGrid(listOf("Apartment", "House", "Shared house", "Studio", "Custom"), homeType) {
                        homeType = it
                        selectedRooms = defaultSetupRooms(it)
                        selectedTaskKeys = defaultSetupTaskKeys(selectedRooms)
                    }
                }
                1 -> StudioCard {
                    Text("Pick rooms", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Use the rooms you actually want TidyPilot to track.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    setupRoomOptions().chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { room ->
                                SetupCheckRow(
                                    label = room,
                                    checked = room in selectedRooms,
                                    modifier = Modifier.weight(1f)
                                ) { checked ->
                                    selectedRooms = if (checked) selectedRooms + room else selectedRooms - room
                                    selectedTaskKeys = if (checked) {
                                        selectedTaskKeys + defaultSetupTaskKeys(setOf(room))
                                    } else {
                                        selectedTaskKeys.filterNot { it.startsWith("$room|") }.toSet()
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    if ("Custom room" in selectedRooms) {
                        OutlinedTextField(
                            value = customRoomName,
                            onValueChange = { customRoomName = it },
                            label = { Text("Custom room name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
                2 -> StudioCard {
                    Text("Add starter tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Common chores are preselected from the local chore library. Keep the ones that fit.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalButton(onClick = { nav.navigate(Route.ChoreLibrary.value) }, modifier = Modifier.fillMaxWidth(), colors = tidyTonalButtonColors()) {
                        Text("Browse full chore library")
                    }
                    if (roomNames.isEmpty()) {
                        Text("No rooms selected yet.", fontWeight = FontWeight.Black)
                    } else {
                        roomNames.forEach { room ->
                            Text(room, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            val templates = setupTaskTemplates(room)
                            if (templates.isEmpty()) {
                                Text("No starter tasks for this room yet. You can add custom chores later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                templates.chunked(2).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        row.forEach { template ->
                                            val key = setupTaskKey(room, template.name)
                                            SetupCheckRow(
                                                label = template.name,
                                                checked = key in selectedTaskKeys,
                                                modifier = Modifier.weight(1f)
                                            ) { checked ->
                                                selectedTaskKeys = if (checked) selectedTaskKeys + key else selectedTaskKeys - key
                                            }
                                        }
                                        if (row.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> StudioCard {
                    Text("Choose cleaning style", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("TidyPilot uses this to set starter frequencies and effort.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SetupChoiceGrid(listOf("Little every day", "Weekly reset", "Weekend deep clean", "As-needed only", "Custom"), cleaningStyle) {
                        cleaningStyle = it
                    }
                    Text(cleaningStyleCopy(cleaningStyle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> StudioCard {
                    Text("Preview generated plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Review before saving. You can edit rooms and tasks later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    RoomStatGrid(
                        "Rooms" to "${preview.first.size}",
                        "Starter tasks" to "${preview.second.size}",
                        "Style" to cleaningStyle,
                        "Storage" to "Local only"
                    )
                    preview.first.take(6).forEach { room ->
                        val count = preview.second.count { it.roomId == room.id }
                        Text("${room.name} - $count starter task${if (count == 1) "" else "s"}", fontWeight = FontWeight.Bold)
                    }
                    if (preview.first.size > 6) {
                        Text("+${preview.first.size - 6} more rooms", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Starter reminders follow your local reminder settings. No account or upload required.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(
                    onClick = { if (step == 0) nav.popBackStack() else step -= 1 },
                    modifier = Modifier.weight(1f),
                    colors = tidyTonalButtonColors()
                ) {
                    Text(if (step == 0) "Close" else "Back")
                }
                if (step < 4) {
                    TextButton(onClick = { step += 1 }, modifier = Modifier.weight(1f)) { Text("Skip") }
                    Button(onClick = { step += 1 }, modifier = Modifier.weight(1f), colors = tidyButtonColors()) { Text("Next") }
                } else {
                    Button(
                        onClick = {
                            viewModel.saveHomeSetup(preview.first, preview.second)
                            scope.launch { snackbar.showSnackbar("Home setup saved.") }
                            nav.navigate(Route.Dashboard.value) { popUpTo(Route.Dashboard.value) }
                        },
                        modifier = Modifier.weight(2f),
                        colors = tidyButtonColors(),
                        enabled = preview.first.isNotEmpty()
                    ) {
                        Text("Save setup")
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupChoiceGrid(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    options.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            row.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TidyMint,
                        selectedLabelColor = Graphite,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SetupCheckRow(label: String, checked: Boolean, modifier: Modifier = Modifier, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DashboardMetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(CleanWhite.copy(alpha = 0.78f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(value, color = TidyDeepTeal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ScheduleImportPromptCard(nav: NavHostController) {
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.CalendarMonth, null, tint = TidyDeepTeal, modifier = Modifier.size(32.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Import your work schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(
                    "Use a screenshot so TidyPilot can keep chores lighter around shifts and suggest bigger resets on days off.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Button(onClick = { nav.navigate(Route.Import.value) }, modifier = Modifier.fillMaxWidth()) {
            Text("Import schedule screenshot")
        }
    }
}

@Composable
private fun DashboardSummaryGrid(state: TidyPilotState, workStatus: String, onSchedule: () -> Unit) {
    StudioCard {
        Text("Home control", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardMiniCard("Done today", "${state.completedTodayCount}", "Completed resets", Icons.Default.CheckCircle, Modifier.weight(1f))
            DashboardMiniCard("Avg room score", "${state.averageTidyScore}/100", "Across active rooms", Icons.Default.RoomPreferences, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DashboardMiniCard("Streak", "${state.streak} days", "Keep it gentle", Icons.Default.CleaningServices, Modifier.weight(1f))
            DashboardMiniCard("Today", dashboardWorkStatusLabel(workStatus, state), dashboardWorkStatusDetail(workStatus, state), Icons.Default.Work, Modifier.weight(1f), onClick = onSchedule)
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
                Icon(icon, null, tint = if (label == "Done today" || label == "Streak") TidyLeaf else TidyAqua, modifier = Modifier.size(22.dp))
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
            Button(onClick = onScan, colors = tidyButtonColors()) {
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
                Button(onClick = onStart, modifier = Modifier.weight(1f), colors = tidyButtonColors()) {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Start task")
                }
                FilledTonalButton(onClick = onOpen, modifier = Modifier.weight(1f), colors = tidyTonalButtonColors()) {
                    Text("Details")
                }
            }
        }
    }
}

@Composable
private fun EnergyTodoCard(state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController) {
    val energy = state.latestCheckIn?.energyLevel ?: state.settings.defaultEnergyLevel
    val tasks = energyTodoTasks(state, energy)
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Doable for your energy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(energyTodoCopy(energy), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                Modifier
                    .background(TidyAqua.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(energyLabel(energy), color = TidyAqua, fontWeight = FontWeight.Black)
            }
        }
        if (tasks.isEmpty()) {
            Text("No chores queued.", fontWeight = FontWeight.Black)
            Text("Add a task or run a quick room scan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            tasks.forEach { task ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CheckCircle, null, tint = taskEnergyColor(task), modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text(task.name, fontWeight = FontWeight.Black)
                        Text("${task.estimatedMinutes} min - ${task.energyRequired} energy - ${repeatLabel(task.frequencyType)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.markComplete(task) }) { Icon(Icons.Default.CheckCircle, "Mark complete", tint = TidyLeaf) }
                    IconButton(onClick = { nav.navigate("detail/task/${task.id}") }) { Icon(Icons.Default.Edit, "Task detail") }
                }
            }
        }
    }
}

@Composable
private fun StarterRoutineCard(viewModel: TidyPilotViewModel) {
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Start with a normal home routine", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Add everyday chores like dishes, counters, trash, laundry, bathroom reset, bedroom pickup, living room reset, and floors.")
            }
            Icon(Icons.Default.AutoAwesome, null, tint = MutedOrange)
        }
        RoomStatGrid(
            "Daily" to "Dishes, counters, bed",
            "Every few days" to "Trash and laundry",
            "Weekly" to "Bathroom and floors",
            "Editable" to "Keep what fits"
        )
        Button(
            onClick = { viewModel.applyStarterRoutine() },
            modifier = Modifier.fillMaxWidth(),
            colors = tidyButtonColors()
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text("Add starter routine")
        }
        Text("No pressure setup: every chore can be edited, snoozed, or deleted later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RoutineAutopilotCard(state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController) {
    val routines = state.tasks
        .filter { !it.isArchived && it.frequencyType != "one-time" }
        .sortedWith(compareBy<CleaningTaskEntity> { it.nextDueAt ?: state.today }.thenBy { it.estimatedMinutes })
        .take(4)
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Routine autopilot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Repeating chores reschedule after you mark them complete.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Refresh, null, tint = TidyAqua)
        }
        if (routines.isEmpty()) {
            Text("No repeating chores yet.", fontWeight = FontWeight.Black)
            Text("Set a repeat frequency when adding a task.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            routines.forEach { task ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .background(TidyMint.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(repeatLabel(task.frequencyType), color = TidyAqua, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(task.name, fontWeight = FontWeight.Black)
                        Text(nextDueLabel(task, state.today), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.markComplete(task) }) { Icon(Icons.Default.CheckCircle, "Complete routine", tint = TidyLeaf) }
                    IconButton(onClick = { nav.navigate("detail/task/${task.id}") }) { Icon(Icons.Default.Edit, "Edit routine") }
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
            Icon(Icons.Default.CleaningServices, null, tint = TidyAqua)
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
            "I'm exhausted" to onExhausted,
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
        item { CompactBrandHeader("Quick Add", "Create a chore fast, then fine-tune if needed.") }
        item {
            OptionChips(listOf("Task", "Room", "Shift"), modeLabel(mode)) {
                mode = it.lowercase()
                if (mode != "task") editingTask = null
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
    var showAdvanced by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var assignedTo by remember(existing?.id) { mutableStateOf(existing?.assignedTo.orEmpty()) }

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
        assignedTo = ""
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
            nextDueAt = dueDateFromChoice(dueChoice),
            assignedTo = assignedTo.trim().ifBlank { null },
            householdId = existing?.householdId,
            createdBy = existing?.createdBy
        )
        val error = viewModel.saveTask(existing, task)
        scope.launch { snackbar.showSnackbar(error ?: if (existing == null) "Task saved." else "Task updated.") }
        if (error == null) {
            onSaved()
            if (resetAfter) resetForAnother()
        }
    }

    StudioCard {
        Text(if (existing == null) "New task" else "Edit task", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text("Pick a shortcut or type your own chore.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(name, { name = it }, label = { Text("Task name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        TaskTemplateChips(taskTemplates) { applyTemplate(it) }
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
        RoutinePreviewCard(
            frequency = frequency,
            dueChoice = dueChoice,
            energy = energy,
            minutes = minutes.toIntOrNull() ?: 10
        )
        Text("Priority")
        OptionChips(listOf("low", "normal", "high", "urgent"), priority) { priority = it }
        Text("Optional due date")
        OptionChips(dueOptions(dueChoice), dueChoice) { dueChoice = it }
        TextButton(onClick = { showAdvanced = !showAdvanced }) {
            Text(if (showAdvanced) "Hide advanced options" else "Advanced options")
        }
        if (showAdvanced) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Optional household prep", fontWeight = FontWeight.Black)
                Text("Local-only for now. Sharing and invites are not enabled yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    assignedTo,
                    { assignedTo = it },
                    label = { Text("Assigned to") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { save(false) }, modifier = Modifier.weight(1f), colors = tidyButtonColors()) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(Modifier.width(6.dp))
                Text(if (existing == null) "Save task" else "Update task")
            }
            FilledTonalButton(onClick = { save(true) }, modifier = Modifier.weight(1f), colors = tidyTonalButtonColors()) { Text("Save and add another") }
        }
    }
}

@Composable
private fun RoutinePreviewCard(frequency: String, dueChoice: String, energy: String, minutes: Int) {
    val isRoutine = frequency != "one-time"
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                if (isRoutine) TidyMint.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Refresh, null, tint = if (isRoutine) TidyAqua else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Text(if (isRoutine) "Routine autopilot on" else "One-time task", fontWeight = FontWeight.Black)
            }
            Text(
                if (isRoutine) {
                    "After you mark it complete, TidyPilot moves the next due date to ${repeatPreview(frequency)}. It stays local and shows up when it fits your energy."
                } else {
                    "This chore leaves the plan after you complete it."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Starts: ${dueChoiceLabel(dueChoice)} - $minutes min - ${energyLabel(energy)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class TaskTemplate(
    val name: String,
    val categoryLabel: String,
    val roomHint: String,
    val minutes: Int,
    val difficulty: String,
    val energy: String,
    val frequency: String,
    val priority: String,
    val category: String
)

private val taskTemplates = listOf(
    TaskTemplate("Clear surface", "Quick reset", "Other", 5, "easy", "low", "daily", "normal", "clutter"),
    TaskTemplate("10-minute reset", "Quick reset", "Other", 10, "medium", "medium", "one-time", "high", "general reset"),
    TaskTemplate("Take out trash", "Quick reset", "Kitchen", 3, "easy", "low", "every few days", "normal", "trash"),
    TaskTemplate("Do dishes", "Kitchen", "Dishes", 10, "medium", "medium", "daily", "high", "dishes"),
    TaskTemplate("Wipe counters", "Kitchen", "Kitchen", 5, "easy", "low", "daily", "normal", "surface wipe"),
    TaskTemplate("Start laundry", "Laundry", "Laundry", 7, "easy", "low", "every few days", "high", "laundry"),
    TaskTemplate("Switch laundry", "Laundry", "Laundry", 5, "easy", "low", "every few days", "normal", "laundry"),
    TaskTemplate("Fold laundry", "Laundry", "Laundry", 15, "medium", "medium", "weekly", "normal", "laundry"),
    TaskTemplate("Clean sink", "Bathroom", "Bathroom", 5, "easy", "low", "every few days", "normal", "bathroom reset"),
    TaskTemplate("Vacuum", "Floors", "Floors", 15, "medium", "medium", "weekly", "normal", "floor clutter")
)

@Composable
private fun TaskTemplateChips(templates: List<TaskTemplate>, onSelect: (TaskTemplate) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf("Quick reset") }
    val categories = listOf("Quick reset", "Kitchen", "Laundry", "Bathroom", "Floors")
    val selectedTemplates = templates.filter { it.categoryLabel == selectedCategory }
    val visibleTemplates = if (expanded) selectedTemplates else selectedTemplates.take(4)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Templates", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        CompactChoiceChips(categories, selectedCategory) { selectedCategory = it; expanded = false }
        visibleTemplates.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { template ->
                    TemplateChip(template, Modifier.weight(1f)) { onSelect(template) }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (selectedTemplates.size > 4) {
            TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                Text(if (expanded) "Show fewer templates" else "More templates")
            }
        }
    }
}

@Composable
private fun TemplateChip(template: TaskTemplate, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            template.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Clip
        )
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
        Text("Common room types")
        OptionChips(listOf("Kitchen", "Bathroom", "Bedroom", "Living Room", "Laundry", "Entryway", "Basement", "Storage", "Garage", "Other"), type) { type = it }
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
        item { CompactBrandHeader("Rooms", "Prioritize rooms and see what needs attention.") }
        if (state.rooms.isEmpty()) {
            item { EmptyState("No rooms yet.", "Add your first room to start planning.", "Set up my home") { nav.navigate(Route.HomeSetup.value) } }
        } else {
            item {
                FilledTonalButton(onClick = { nav.navigate(Route.HomeSetup.value) }, modifier = Modifier.fillMaxWidth(), colors = tidyTonalButtonColors()) {
                    Icon(Icons.Default.Home, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add rooms from setup")
                }
            }
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
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.RoomPreferences, null, tint = roomScoreColor(score), modifier = Modifier.size(26.dp))
            Column(Modifier.weight(1f)) {
                Text(room.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(topRoomIssue(score, stats), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            RoomStatusBadge(score)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LinearProgressIndicator(
                progress = { score.score / 100f },
                modifier = Modifier.weight(1f),
                color = roomProgressColor(score),
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
            Text("${score.score}/100", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CompactRoomFacts(
            "Tasks" to "${stats.activeTasks}",
            "Overdue" to "${score.overdueTasks}",
            "Issues" to "${stats.openIssues}",
            "Reset" to (score.lastCompletedLabel),
            "Scan" to (stats.lastScanned ?: "Not yet"),
            "Default" to room.defaultTaskFrequency
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { nav.navigate("detail/room/${room.id}") }, modifier = Modifier.weight(1f), colors = tidyButtonColors()) {
                Text("Open room")
            }
            TextButton(onClick = { editing = !editing }) { Text(if (editing) "Close" else "Edit") }
            TextButton(onClick = { showHistory = !showHistory }) { Text(if (showHistory) "Hide" else "History") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = {
                viewModel.archiveRoom(room)
                scope.launch { snackbar.showSnackbar("${room.name} archived.") }
            }) { Text("Archive") }
            if (stats.safeToDelete) {
                TextButton(onClick = {
                    viewModel.deleteRoom(room)
                    scope.launch { snackbar.showSnackbar("${room.name} deleted.") }
                }) { Text("Delete", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
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
private fun RoomStatusBadge(score: RoomScore) {
    val color = roomScoreColor(score)
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(score.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
private fun CompactRoomFacts(vararg items: Pair<String, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.toList().chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    Column(
                        Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 7.dp)
                    ) {
                        Text(value, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun topRoomIssue(score: RoomScore, stats: RoomStats): String = when {
    score.openIssues > 0 -> "${score.openIssues} scan issue${if (score.openIssues == 1) "" else "s"} to review"
    score.overdueTasks > 0 -> "${score.overdueTasks} overdue task${if (score.overdueTasks == 1) "" else "s"}"
    stats.activeTasks > 0 -> "${stats.activeTasks} active task${if (stats.activeTasks == 1) "" else "s"}"
    else -> score.reason
}

@Composable
private fun LegacyWorkScheduleScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, snackbar: SnackbarHostState) {
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Work Schedule", "TidyPilot plans around shifts and recovery time.") }
        items(state.shifts, key = { it.id }) { shift ->
            StudioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${shift.date} - ${shift.label}", fontWeight = FontWeight.Black)
                        Text("${shift.startTime} - ${shift.endTime} - expected ${shift.expectedExhaustionLevel} exhaustion")
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
private fun ScheduleImportScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, snackbar: SnackbarHostState, nav: NavHostController) {
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionHeader(
                "Import schedule",
                "Turn a work schedule screenshot into shifts TidyPilot can plan around."
            )
        }
        item {
            StudioCard {
                Text("How it works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text("1. Choose a screenshot or take a photo of your schedule.")
                Text("2. Review the recognized text and shift preview.")
                Text("3. Save the shifts, then Today updates around work and days off.")
                Text("Photos stay on this device.", color = Sage, fontWeight = FontWeight.Black)
            }
        }
        item {
            SchedulePhotoImportCard(
                viewModel = viewModel,
                snackbar = snackbar,
                onSaved = { nav.navigate("schedule") }
            )
        }
        item {
            FilledTonalButton(onClick = { nav.navigate("schedule") }, modifier = Modifier.fillMaxWidth()) {
                Text("View work schedule")
            }
        }
        if (state.shifts.isNotEmpty()) {
            item {
                ProgressGrid(
                    "Saved shifts" to state.shifts.size.toString(),
                    "This week" to "${state.shifts.count { it.date in LocalDate.now()..LocalDate.now().plusDays(6) }} shifts",
                    "Next shift" to (state.shifts.firstOrNull { !it.date.isBefore(LocalDate.now()) }?.let { "${it.date} ${it.startTime}" } ?: "None"),
                    "Planning" to shiftPlanningSummary(state)
                )
            }
        }
    }
}

@Composable
private fun WorkScheduleScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, snackbar: SnackbarHostState, nav: NavHostController) {
    var editingShift by remember { mutableStateOf<WorkShiftEntity?>(null) }
    val listState = rememberLazyListState()
    val today = LocalDate.now()
    LaunchedEffect(editingShift?.id) {
        if (editingShift != null) listState.animateScrollToItem(2)
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Work Schedule", "TidyPilot plans around shifts, days off, and recovery time.") }
        item {
            StudioCard {
                Text("Import from screenshot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text("Use the schedule importer for faster setup, then review before saving.")
                Button(onClick = { nav.navigate(Route.Import.value) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open schedule importer")
                }
            }
        }
        editingShift?.let { shift ->
            item(key = "edit-${shift.id}") {
                ShiftForm(viewModel, snackbar, shift) { editingShift = null }
                TextButton(onClick = { editingShift = null }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel edit")
                }
            }
        }
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
                onDayOff = { viewModel.markDayOff(shift.date) },
                onExhaustionChange = { level -> viewModel.saveShift(shift.copy(expectedExhaustionLevel = level)) }
            )
        }
        if (editingShift == null) {
            item { ShiftForm(viewModel, snackbar) }
        }
    }
}

@Composable
private fun SchedulePhotoImportCard(viewModel: TidyPilotViewModel, snackbar: SnackbarHostState, onSaved: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var isReading by rememberSaveable { mutableStateOf(false) }
    var rawText by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("Photos stay on this device. Review before saving.") }
    var candidates by remember { mutableStateOf<List<ScheduleImportCandidate>>(emptyList()) }
    var guidance by remember { mutableStateOf<ScheduleImportGuidance?>(null) }

    fun updateImportText(text: String) {
        rawText = text
        candidates = ScheduleImportParser.parse(text)
        guidance = ScheduleImportGuidanceClassifier.fromText(text, candidates)
        message = if (candidates.isEmpty()) {
            "No shifts found yet. Edit the text below or add shifts manually."
        } else {
            val shiftCount = candidates.count { !it.isDayOff }
            val dayOffCount = candidates.count { it.isDayOff }
            "$shiftCount shift${if (shiftCount == 1) "" else "s"} and $dayOffCount day off entr${if (dayOffCount == 1) "y" else "ies"} ready to review."
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
        Text("Import schedule screenshot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text("Use a schedule screenshot or photo to draft shifts. OCR runs locally, and nothing is saved until you confirm.")
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
        guidance?.let { ScheduleImportGuidanceCard(it) }
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
            Text("Review before saving", fontWeight = FontWeight.SemiBold)
            candidates.take(5).forEach { candidate ->
                ScheduleImportCandidateRow(candidate) {
                    candidates = candidates.filterNot { it == candidate }
                }
            }
            if (candidates.size > 5) Text("${candidates.size - 5} more entries will be saved.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = {
                    val shifts = candidates.filterNot { it.isDayOff }
                    val daysOff = candidates.filter { it.isDayOff }
                    if (shifts.isNotEmpty()) {
                        viewModel.saveShifts(shifts.map {
                            WorkShiftEntity(
                                date = it.date,
                                startTime = it.startTime,
                                endTime = it.endTime,
                                label = it.label,
                                expectedExhaustionLevel = it.expectedExhaustionLevel,
                                notes = "Imported from schedule photo. Review confidence: ${it.confidenceLabel}. Source: ${it.sourceLine}"
                            )
                        })
                    }
                    daysOff.forEach { viewModel.markDayOff(it.date) }
                    scope.launch {
                        snackbar.showSnackbar("${shifts.size} shift${if (shifts.size == 1) "" else "s"} and ${daysOff.size} day off entr${if (daysOff.size == 1) "y" else "ies"} saved.")
                    }
                    rawText = ""
                    candidates = emptyList()
                    guidance = null
                    message = "Import saved. Photos stay on this device."
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm schedule import")
            }
        }
    }
}

@Composable
private fun ScheduleImportGuidanceCard(guidance: ScheduleImportGuidance) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TidyMint.copy(alpha = 0.20f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(guidance.issue.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            Text(guidance.issue.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            guidance.detail?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            guidance.issue.tips.forEach { tip ->
                Text(tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text(
                        if (candidate.isDayOff) "${candidate.date} - Day off" else "${candidate.date} - ${candidate.startTime} to ${candidate.endTime}",
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        if (candidate.isDayOff) "TidyPilot will plan this as a day off reset." else "${candidate.label} - expected ${candidate.expectedExhaustionLevel} exhaustion",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Confidence: ${candidate.confidenceLabel}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onIgnore) { Text("Ignore") }
            }
        }
    }
}

@Composable
private fun WorkShiftCard(
    shift: WorkShiftEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDayOff: () -> Unit,
    onExhaustionChange: (String) -> Unit
) {
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
        Text("Expected exhaustion after shift", fontWeight = FontWeight.SemiBold)
        OptionChips(listOf("low", "medium", "high"), shift.expectedExhaustionLevel, onExhaustionChange)
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
    var quickRoomName by rememberSaveable { mutableStateOf("") }
    var quickRoomType by rememberSaveable { mutableStateOf("Bedroom") }
    val selectedRoom = state.rooms.firstOrNull { it.id == roomId }
    val visibleContextOptions = visibleScanContextOptions(selectedRoom)

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
        val uri = pendingUri
        if (ok && uri != null) {
            scope.launch { snackbar.showSnackbar("Photo ready. Review it, then run local analysis.") }
        } else {
            scope.launch { snackbar.showSnackbar("Photo was not saved. You can retake it.") }
        }
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pendingUri = uri
            scope.launch { snackbar.showSnackbar("Photo ready. Review it, then run local analysis.") }
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
                Text("Need another room?", fontWeight = FontWeight.SemiBold)
                Text("Add rooms like Bedroom 2, Kids Room, Guest Bedroom, or Basement Storage before scanning.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    quickRoomName,
                    { quickRoomName = it },
                    label = { Text("New room name") },
                    placeholder = { Text("Bedroom 2") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OptionChips(roomTypeOptions, quickRoomType) { quickRoomType = it }
                FilledTonalButton(
                    onClick = {
                        val cleanName = quickRoomName.trim()
                        if (cleanName.isBlank()) {
                            scope.launch { snackbar.showSnackbar("Name the room first.") }
                            return@FilledTonalButton
                        }
                        val newRoom = RoomEntity(
                            name = cleanName,
                            roomType = quickRoomType,
                            iconName = quickRoomType.lowercase().replace(" ", "_"),
                            tidyScore = 60,
                            priority = if (quickRoomType in listOf("Basement", "Garage", "Storage")) "high" else "normal",
                            defaultTaskIntensity = if (quickRoomType in listOf("Basement", "Garage", "Storage")) "medium" else "low",
                            defaultTaskFrequency = "weekly"
                        )
                        val error = viewModel.saveRoom(newRoom)
                        if (error == null) {
                            roomId = newRoom.id
                            quickRoomName = ""
                        }
                        scope.launch { snackbar.showSnackbar(error ?: "$cleanName added for this scan.") }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add room for this scan") }
                if (visibleContextOptions.isNotEmpty()) {
                    Text("Visible in photo", fontWeight = FontWeight.SemiBold)
                    Text("Tap what you can see so the local scan suggestions match the room better.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SelectableContextChips(
                        options = scanMessLevelOptions,
                        selectedText = note,
                        onToggle = { token ->
                            note = toggleScanContextToken(note, token)
                        }
                    )
                    SelectableContextChips(
                        options = visibleContextOptions,
                        selectedText = note,
                        onToggle = { token ->
                            note = toggleScanContextToken(note, token)
                        }
                    )
                }
                OutlinedTextField(note, { note = it }, label = { Text("Optional note") }, placeholder = { Text("after work mess") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        }
        item {
            StudioCard {
                Text("Photo source", fontWeight = FontWeight.Black)
                Text("Camera access is only used to capture a room photo for local analysis. No upload, no account, no network AI.")
                Text("Try stepping back to capture more of the room. Lighting, blur, glare, or close-up photos can make suggestions less accurate.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    photoQualityNotes(note, uri.toString()).forEach {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            val room = selectedRoom
                            if (room == null) {
                                scope.launch { snackbar.showSnackbar("Choose a room before analyzing.") }
                            } else {
                                startLocalScan(room, uri, note)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Analyze locally") }
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
        item { SectionHeader("Scan Summary", room?.name ?: "Room scanned") }
        item {
            StudioCard {
                if (scan.imageUri.startsWith("content://")) {
                    Image(rememberAsyncImagePainter(Uri.parse(scan.imageUri)), null, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                }
                Text("Tidy score ${scan.tidyScore}/100 - mess score ${scan.messScore}/100", fontWeight = FontWeight.Black)
                LinearProgressIndicator(progress = { scan.tidyScore / 100f }, modifier = Modifier.fillMaxWidth())
                Text(scan.confidenceSummary)
                Text("Detected issues: ${scan.detectedIssueTags.unpipe().joinToString(", ")}")
            }
        }
        item { SectionHeader("Suggested actions", "${scan.estimatedCleanupMinutes} minutes total - ${energyRecommendation(issues)} energy recommended") }
        items(issues, key = { it.id }) { issue ->
            StudioCard {
                Text(issue.suggestedAction, fontWeight = FontWeight.Black)
                Text("${issue.label} - ${issue.estimatedMinutes} min - ${issue.energyLevel} energy - ${(issue.confidence * 100).toInt()}% rough confidence")
            }
        }
        item {
            StudioCard {
                WrapButtons(
                    "Make this today's plan" to { viewModel.addTasksFromScan(scan); viewModel.replan(); nav.navigate(Route.Dashboard.value) },
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
                    status = issue.status.ifBlank { "review" }.replace("suggested", "review"),
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
        val task = CleaningTaskEntity(
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
        viewModel.saveTask(
            null,
            task
        )
        viewModel.updateScanIssueStatus(draft.sourceIssueId, "accepted", task.id)
        return true
    }

    fun createTasksFromDrafts(makePlan: Boolean, quickOnly: Boolean = false) {
        val selected = drafts.filter {
            it.status == "review" && it.action.isNotBlank() && (!quickOnly || ((it.minutes.toIntOrNull() ?: 99) <= 10 && it.energy == "low"))
        }
        if (room == null || selected.isEmpty()) {
            scope.launch { snackbar.showSnackbar(if (quickOnly) "No quick low-energy suggestions are ready." else "Select at least one useful action first.") }
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

    val activeDrafts = drafts.filter { it.status == "review" }
    val ignoredDrafts = drafts.count { it.status == "ignored" || it.status == "not accurate" }
    val createdDrafts = drafts.count { it.status == "created" }

    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader("Scan Summary", room?.name ?: "Room scanned") }
        item {
            StudioCard {
                if (scan.imageUri.startsWith("content://")) {
                    Image(rememberAsyncImagePainter(Uri.parse(scan.imageUri)), null, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                }
                Text("Mess level detection", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(messLevelLabel(scan.messLevel), color = messLevelColor(scan.messLevel), fontWeight = FontWeight.Black)
                    Text("Confidence: ${scan.confidence}", color = confidenceColor(scan.confidence))
                }
                Text("Mess score ${scan.messScore}/100", fontWeight = FontWeight.Black)
                LinearProgressIndicator(progress = { scan.messScore / 100f }, modifier = Modifier.fillMaxWidth())
                Text(scan.summary.ifBlank { scan.confidenceSummary })
                RoomStatGrid(
                    "Found" to "${drafts.size} possible issues",
                    "Ready" to "${activeDrafts.size} to review",
                    "Created" to "$createdDrafts tasks",
                    "Ignored" to "$ignoredDrafts items"
                )
                if (activeDrafts.isNotEmpty()) {
                    Text("Start here: ${activeDrafts.first().action}", fontWeight = FontWeight.SemiBold)
                }
                Text("Scan results are estimates. Review before creating tasks.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Photos stay on this device.", color = Sage, fontWeight = FontWeight.Black)
            }
        }
        item {
            StudioCard {
                Text("Detected zones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                val zones = scan.detectedZones.unpipe()
                if (zones.isEmpty()) {
                    Text("Room view - Needs review", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    zones.forEach { zone ->
                        val parts = zone.split(":")
                        val name = parts.getOrNull(1) ?: parts.firstOrNull().orEmpty()
                        val score = parts.getOrNull(2)?.toIntOrNull()
                        Text("${name.ifBlank { "Room area" }}${score?.let { " - $it/100 clutter estimate" } ?: ""}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { SectionHeader("Review detected issues", "Keep useful findings, edit rough ones, or mark anything inaccurate before chores are created.") }
        item {
            StudioCard {
                Text("${scan.estimatedCleanupMinutes} minutes estimated - ${energyRecommendation(issues)} energy recommended", fontWeight = FontWeight.Black)
                Text("Possible scan findings can be edited or ignored before they become chores. No shame - make it manageable.")
                WrapButtons(
                    "Create all suggested tasks" to { createTasksFromDrafts(false) },
                    "Create only quick tasks" to { createTasksFromDrafts(false, quickOnly = true) },
                    "Ignore all low-confidence issues" to {
                        var ignored = 0
                        drafts.indices.forEach { index ->
                            if (confidenceLabel(drafts[index].confidence) == "low" && drafts[index].status == "review") {
                                drafts[index] = drafts[index].copy(status = "not accurate", editing = false)
                                viewModel.updateScanIssueStatus(drafts[index].sourceIssueId, "not_accurate")
                                ignored++
                            }
                        }
                        scope.launch { snackbar.showSnackbar("$ignored low-confidence issue${if (ignored == 1) "" else "s"} marked not accurate.") }
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
                    onIgnore = {
                        drafts[index] = draft.copy(status = "ignored", editing = false)
                        viewModel.updateScanIssueStatus(draft.sourceIssueId, "ignored")
                    },
                    onNotAccurate = {
                        drafts[index] = draft.copy(status = "not accurate", editing = false)
                        viewModel.updateScanIssueStatus(draft.sourceIssueId, "not_accurate")
                    },
                    onHandled = {
                        drafts[index] = draft.copy(status = "handled", editing = false)
                        viewModel.updateScanIssueStatus(draft.sourceIssueId, "completed")
                    }
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
                    "Mark reviewed" to {
                        viewModel.markScanReviewed(scan.id)
                        scope.launch { snackbar.showSnackbar("Scan marked reviewed.") }
                    },
                    "Mess level too high" to {
                        viewModel.setScanFeedback(scan.id, "mess level too high")
                        scope.launch { snackbar.showSnackbar("Correction saved.") }
                    },
                    "Mess level too low" to {
                        viewModel.setScanFeedback(scan.id, "mess level too low")
                        scope.launch { snackbar.showSnackbar("Correction saved.") }
                    },
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
                    },
                    "Wrong room" to {
                        viewModel.setScanFeedback(scan.id, "wrong room")
                        scope.launch { snackbar.showSnackbar("Correction saved.") }
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
    onNotAccurate: () -> Unit,
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
            "Energy" to draft.energy,
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
            "Not accurate" to onNotAccurate,
            (if (draft.editing) "Done editing" else "Edit") to { onChange(draft.copy(editing = !draft.editing)) },
            "Already handled" to onHandled
        )
        if (draft.status == "created") {
            Text("Task created.", color = Sage, fontWeight = FontWeight.Black)
        } else if (draft.status == "ignored") {
            Text("Ignored for now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (draft.status == "not accurate") {
            Text("Marked not accurate. This stays local and helps future scanner tuning.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private val roomTypeOptions = listOf("Kitchen", "Bathroom", "Bedroom", "Living Room", "Laundry", "Entryway", "Basement", "Storage", "Garage", "Office", "Kids Room", "Guest Bedroom", "Other")

private val scanMessLevelOptions = listOf("mostly clear", "quick reset", "moderate mess", "very messy", "lots of clutter", "piles visible")

private fun visibleScanContextOptions(room: RoomEntity?): List<String> {
    val text = "${room?.roomType.orEmpty()} ${room?.name.orEmpty()}".lowercase()
    return when {
        text.containsAny("basement", "storage", "garage") -> listOf(
            "super untidy",
            "floor path blocked",
            "shelves cluttered",
            "workout gear",
            "bike or equipment",
            "cords visible",
            "trash or cardboard",
            "needs bigger reset"
        )
        text.containsAny("kitchen") -> listOf("dishes visible", "sink full", "counter clutter", "stove area", "trash visible", "floor crumbs", "wipe needed", "needs bigger reset")
        text.containsAny("bedroom", "bed", "kids room", "guest bedroom") -> listOf("laundry visible", "clothes on floor", "floor clutter", "unmade bed", "nightstand clutter", "dresser clutter", "closet or boxes", "trash visible", "floor path blocked", "needs bigger reset")
        text.containsAny("bath") -> listOf("counter clutter", "sink area", "mirror spots", "towels", "shower or tub", "trash visible", "floor clutter", "floor path blocked", "wipe needed")
        text.containsAny("living", "family room", "den") -> listOf("floor clutter", "coffee table clutter", "couch blankets", "toys visible", "electronics cords", "trash visible", "vacuum needed", "needs bigger reset")
        text.containsAny("entry", "mudroom", "hall") -> listOf("shoes visible", "bags or coats", "mail or keys", "floor path blocked", "trash visible", "needs reset")
        text.containsAny("laundry") -> listOf("washer or dryer", "clothes on floor", "basket to fold", "machine top clutter", "shelves cluttered", "needs bigger reset")
        text.containsAny("office", "desk") -> listOf("desk clutter", "paper piles", "cords visible", "floor clutter", "trash visible", "needs reset")
        else -> listOf("floor clutter", "surface clutter", "trash visible", "cords visible", "paper piles", "wipe needed", "needs reset")
    }
}

private fun String.containsAny(vararg needles: String): Boolean = needles.any { contains(it, ignoreCase = true) }

@Composable
private fun SelectableContextChips(options: List<String>, selectedText: String, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { option ->
                    FilterChip(
                        selected = selectedText.contains(option, ignoreCase = true),
                        onClick = { onToggle(option) },
                        label = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun toggleScanContextToken(current: String, token: String): String {
    val parts = current.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toMutableList()
    val existing = parts.indexOfFirst { it.equals(token, ignoreCase = true) }
    if (existing >= 0) {
        parts.removeAt(existing)
    } else {
        parts += token
    }
    return parts.joinToString(", ")
}

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
    "basement_floor_path" -> "Possible floor path clutter"
    "storage_shelf_clutter" -> "Possible storage shelf clutter"
    "loose_gear_visible" -> "Possible loose gear"
    "cords_or_equipment_clutter" -> "Possible cords or equipment clutter"
    "mirror_wipe_needed" -> "Possible mirror wipe needed"
    "bathroom_towels_visible" -> "Possible towels to gather"
    "shower_reset_needed" -> "Possible shower or tub reset"
    "bedroom_surface_clutter" -> "Possible bedroom surface clutter"
    "closet_or_box_clutter" -> "Possible closet or box clutter"
    "living_surface_clutter" -> "Possible table or surface clutter"
    "couch_reset_needed" -> "Possible couch reset"
    "electronics_clutter" -> "Possible electronics or cord clutter"
    "floor_clean_needed" -> "Possible floor cleaning needed"
    "shoes_visible" -> "Possible shoe clutter"
    "entry_bag_clutter" -> "Possible bags or coats to reset"
    "mail_or_keys_clutter" -> "Possible entry drop-zone clutter"
    "entry_path_clutter" -> "Possible entry path clutter"
    "laundry_machine_reset" -> "Possible laundry machine reset"
    "folding_needed" -> "Possible folding needed"
    "laundry_surface_clutter" -> "Possible laundry surface clutter"
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

private fun messLevelLabel(level: String): String = when (level) {
    "clear" -> "Looks mostly clear"
    "light_reset" -> "Quick reset"
    "moderate_mess" -> "Needs attention"
    "heavy_reset" -> "Bigger reset"
    else -> "Needs review"
}

@Composable
private fun messLevelColor(level: String) = when (level) {
    "clear" -> Sage
    "light_reset" -> Sage
    "moderate_mess" -> MutedOrange
    "heavy_reset" -> MutedOrange
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun photoQualityNotes(note: String, imageUri: String): List<String> {
    val lower = "$note $imageUri".lowercase()
    val notes = mutableListOf("Review suggestions before creating tasks.")
    if (lower.containsAny("blurry", "blur")) notes += "This photo is a little blurry."
    if (lower.containsAny("dark", "dim")) notes += "Lighting may make detection less accurate."
    if (lower.containsAny("glare", "bright")) notes += "Glare or bright light may hide some clutter."
    if (lower.containsAny("close", "corner")) notes += "Try stepping back to capture more of the room."
    if (notes.size == 1) notes += "Try to include the main floor path and surfaces."
    return notes
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
private fun RoomDetailScreen(id: String, state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController, snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
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
                            IconButton(onClick = { nav.navigate("editTask/${itemTask.id}") }) { Icon(Icons.Default.Edit, "Edit task") }
                            IconButton(onClick = {
                                viewModel.deleteTask(itemTask)
                                scope.launch { snackbar.showSnackbar("${itemTask.name} removed.") }
                            }) { Icon(Icons.Default.Delete, "Delete task", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
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
                        Text("${task.estimatedMinutes} min - ${task.energyRequired} energy - ${task.frequencyType}", fontWeight = FontWeight.Black)
                        Text("Priority: ${task.priority} - preferred ${task.preferredTime}")
                        Text("Last completed: ${task.lastCompletedAt ?: "not yet"}")
                        Text("Next due: ${task.nextDueAt ?: "done"}")
                        if (task.frequencyType != "one-time") Text("Routine autopilot: completing this task schedules it again ${repeatPreview(task.frequencyType)}.")
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
                            "Delete" to {
                                viewModel.deleteTask(task)
                                scope.launch { snackbar.showSnackbar("Task removed.") }
                                if (!nav.popBackStack()) nav.navigate(Route.Rooms.value)
                            }
                        )
                    }
                }
                item { TaskSuppliesCard(task, state, viewModel, snackbar) }
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
            else -> item { EmptyState("Detail not found", "Return to today's plan.") { nav.navigate(Route.Dashboard.value) } }
        }
    }
}

@Composable
private fun TaskSuppliesCard(task: CleaningTaskEntity, state: TidyPilotState, viewModel: TidyPilotViewModel, snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var newSupplyName by rememberSaveable { mutableStateOf("") }
    var newSupplyCost by rememberSaveable { mutableStateOf("") }
    val linkedIds = state.taskSupplies.filter { it.taskId == task.id }.map { it.supplyId }.toSet()
    val linked = state.supplies.filter { it.id in linkedIds }
    val suggestedNames = suggestedSupplyNames(task)
    val suggested = suggestedNames.mapNotNull { name -> state.supplies.firstOrNull { it.name.equals(name, ignoreCase = true) } }
    val options = (linked + suggested + state.supplies.take(6)).distinctBy { it.id }

    StudioCard {
        Text("Supplies needed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text("Optional. Use this when supplies help you start faster.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (linked.isEmpty()) {
            Text("No supplies linked yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            linked.forEach { supply ->
                SupplyRow(
                    supply = supply,
                    trailing = {
                        TextButton(onClick = { viewModel.unlinkSupplyFromTask(task.id, supply.id) }) { Text("Remove") }
                    },
                    onLow = { viewModel.markSupplyRunningLow(supply, !supply.isRunningLow) },
                    onShopping = { viewModel.markSupplyOnShoppingList(supply, !supply.isOnShoppingList) }
                )
            }
        }
        if (options.any { it.id !in linkedIds }) {
            Text("Add to this task", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            options.filter { it.id !in linkedIds }.take(8).forEach { supply ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(supply.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    FilledTonalButton(onClick = { viewModel.linkSupplyToTask(task.id, supply.id) }, colors = tidyTonalButtonColors()) { Text("Link") }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(newSupplyName, { newSupplyName = it }, label = { Text("New supply") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(newSupplyCost, { newSupplyCost = it.filter { ch -> ch.isDigit() || ch == '.' }.take(7) }, label = { Text("Cost") }, modifier = Modifier.weight(0.7f), singleLine = true)
        }
        WrapButtons(
            "Add supply" to {
                val error = viewModel.saveSupply(newSupplyName, task.photoDetectableCategory, dollarsTextToCents(newSupplyCost), "Added from ${task.name}.")
                scope.launch { snackbar.showSnackbar(error ?: "Supply added.") }
                if (error == null) {
                    newSupplyName = ""
                    newSupplyCost = ""
                }
            },
            "Add missing supplies to shopping list" to {
                viewModel.addMissingSuppliesToShoppingList(task)
                scope.launch { snackbar.showSnackbar("Supplies added to shopping list.") }
            }
        )
    }
}

@Composable
private fun SupplyRow(
    supply: CleaningSupplyEntity,
    trailing: @Composable () -> Unit = {},
    onLow: () -> Unit,
    onShopping: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(supply.name, fontWeight = FontWeight.Black)
            Text(
                listOfNotNull(
                    supply.category,
                    if (supply.estimatedCostCents > 0) formatMoney(supply.estimatedCostCents) else null,
                    if (supply.isRunningLow) "running low" else null,
                    if (supply.isOnShoppingList) "shopping list" else null
                ).joinToString(" - "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onLow) { Text(if (supply.isRunningLow) "Stocked" else "Low") }
        TextButton(onClick = onShopping) { Text(if (supply.isOnShoppingList) "Listed" else "Shop") }
        trailing()
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
    val rankedRooms = state.rooms
        .sortedWith(compareBy<RoomEntity> { roomScore(it, state).score }.thenByDescending { priorityScore(it.priority) })
        .take(4)
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CompactBrandHeader("Reports", "Useful local summaries you can copy or save.") }
        if (state.completions.isEmpty() && state.scans.isEmpty()) {
            item { EmptyState("Nothing to report yet.", "Complete a few tasks to see progress.") }
        }
        item {
            StudioCard {
                Text("Weekly snapshot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ReportMiniStat("Completed", "${stats.completedThisWeek}", Modifier.weight(1f))
                    ReportMiniStat("Best streak", "${stats.bestStreak} days", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ReportMiniStat("Avg energy", stats.averageEnergy, Modifier.weight(1f))
                    ReportMiniStat("Overdue", "${stats.overdueTasks.size}", Modifier.weight(1f))
                }
            }
        }
        item {
            StudioCard {
                Text("Room highlights", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ReportHighlightCard("Most improved", stats.mostImprovedRoom ?: "More scans needed", TidyLeaf, Modifier.weight(1f))
                    ReportHighlightCard("Watch next", rankedRooms.firstOrNull()?.name ?: "None right now", MutedOrange, Modifier.weight(1f))
                }
                if (rankedRooms.isNotEmpty()) {
                    Text("Rooms needing attention", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                    rankedRooms.forEachIndexed { index, room ->
                        ReportRoomRankChip(index + 1, room, roomScore(room, state))
                    }
                } else {
                    Text("No rooms to rank yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            StudioCard {
                Text("Energy and work impact", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ReportMiniStat("Energy", stats.averageEnergy, Modifier.weight(1f))
                    ReportMiniStat("Day off", "${stats.dayOffCompletions}", Modifier.weight(1f))
                    ReportMiniStat("Workday", "${stats.workdayCompletions}", Modifier.weight(1f))
                }
                ReportBullet("Work impact", stats.workScheduleImpact)
                ReportBullet("Most consistent", mostConsistentRoom(state) ?: "More completions will reveal this.")
            }
        }
        item {
            StudioCard {
                Text("Follow-up", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                ReportBullet("Overdue or missed", stats.overdueTasks.ifEmpty { listOf("None right now") }.take(3).joinToString())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ReportMiniStat("Detected", "${stats.scanIssuesDetected}", Modifier.weight(1f))
                    ReportMiniStat("Acted on", "${stats.scanIssuesActedOn}", Modifier.weight(1f))
                    ReportMiniStat("Completed", "${stats.scanTasksCompleted}", Modifier.weight(1f))
                }
            }
        }
        item {
            StudioCard {
                Text("Supplies and time", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ReportMiniStat("Supplies", formatMoney(monthlySupplySpendCents(state)), Modifier.weight(1f))
                    ReportMiniStat("Budget", monthlyBudgetLabel(state), Modifier.weight(1f))
                    ReportMiniStat("Time", formatMinutes(monthlyCleaningMinutes(state)), Modifier.weight(1f))
                }
                ReportBullet("Shopping list", if (state.shoppingList.isEmpty()) "No supplies marked low right now." else state.shoppingList.take(4).joinToString { it.name })
            }
        }
        item {
            StudioCard {
                Text("Export actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                WrapButtons(
                    "Copy summary" to {
                        clipboard.setText(AnnotatedString(report))
                        scope.launch { snackbar.showSnackbar("Report copied.") }
                    },
                    "Export TXT" to {
                        val message = runCatching {
                            val file = writeLocalReport(context, "tidypilot_report_${LocalDate.now()}.txt", report)
                            "Saved TXT locally: ${file.name}"
                        }.getOrElse { "Could not save TXT report. Try again from Settings." }
                        scope.launch { snackbar.showSnackbar(message) }
                    },
                    "Export CSV" to {
                        val message = runCatching {
                            val file = writeLocalReport(context, "tidypilot_report_${LocalDate.now()}.csv", csv)
                            "Saved CSV locally: ${file.name}"
                        }.getOrElse { "Could not save CSV report. Try again from Settings." }
                        scope.launch { snackbar.showSnackbar(message) }
                    }
                )
                Text("Exports are saved locally. Nothing is uploaded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReportInsightRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            modifier = Modifier.weight(0.42f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            value,
            modifier = Modifier.weight(0.58f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ReportHighlightCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(accent.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ReportRoomRankChip(rank: Int, room: RoomEntity, score: RoomScore) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(roomScoreColor(score).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(roomScoreColor(score).copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("$rank", fontWeight = FontWeight.Black, color = roomScoreColor(score))
        }
        Column(Modifier.weight(1f)) {
            Text(room.name, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(score.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${score.score}/100", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReportBullet(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(TidyAqua, RoundedCornerShape(8.dp))
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReportMiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(TidyMint.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                PreferenceRow("Energy check-in reminders", "Quick reset time? How's your energy after work?", reminders) { reminders = it }
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
private fun SettingsScreen(state: TidyPilotState, viewModel: TidyPilotViewModel, nav: NavHostController) {
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
    var quietDays by remember(state.settings) { mutableStateOf(state.settings.quietDays.unpipe().toSet()) }
    var maxReminders by remember(state.settings) { mutableStateOf(state.settings.maxRemindersPerDay.toString()) }
    var reminderTone by remember(state.settings) { mutableStateOf(state.settings.reminderTone.ifBlank { "Gentle" }) }
    var reminderTypes by remember(state.settings) { mutableStateOf(state.settings.enabledReminderTypes.unpipe().ifEmpty { listOf("daily", "task", "room", "weekly", "seasonal", "quick_win") }.toSet()) }
    var savePhotos by remember(state.savePhotosLocally) { mutableStateOf(state.savePhotosLocally) }
    var saveProcessedImages by remember(state.settings) { mutableStateOf(state.settings.saveProcessedScanImages) }
    var requireScanReview by remember(state.settings) { mutableStateOf(state.settings.requireScanReview) }
    var scanConfidenceThreshold by remember(state.settings) { mutableStateOf(state.settings.defaultScanConfidenceThreshold) }
    var supplyTracking by remember(state.settings) { mutableStateOf(state.settings.supplyTrackingEnabled) }
    var monthlyBudget by remember(state.settings) { mutableStateOf(centsToDollarsText(state.settings.monthlyCleaningBudgetCents)) }
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
                quietDays = pipe(quietDays.toList()),
                maxRemindersPerDay = maxReminders.toIntOrNull()?.coerceIn(1, 6) ?: 1,
                reminderTone = reminderTone,
                enabledReminderTypes = pipe(reminderTypes.toList()),
                savePhotosLocally = savePhotos,
                saveProcessedScanImages = saveProcessedImages,
                requireScanReview = requireScanReview,
                defaultScanConfidenceThreshold = scanConfidenceThreshold,
                supplyTrackingEnabled = supplyTracking,
                monthlyCleaningBudgetCents = dollarsTextToCents(monthlyBudget),
                themeMode = theme
            ),
            theme,
            reminders,
            savePhotos
        )
        actionNote = "Settings saved locally."
    }

    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CompactBrandHeader("Settings", "Local controls for planning, reminders, and privacy.") }
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
                Text("Premium", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(premiumStatusCopy(state.settings), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Free keeps the core cleaning flow: rooms, tasks, Today, One Thing, starter chores, basic reminders, and basic scoring.")
                FilledTonalButton(onClick = { nav.navigate(Route.Premium.value) }, modifier = Modifier.fillMaxWidth(), colors = tidyTonalButtonColors()) {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(Modifier.width(6.dp))
                    Text("View Premium")
                }
            }
        }
        item {
            StudioCard {
                Text("Reminders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                PreferenceRow("Enable reminders", "Local reminders only. TidyPilot keeps them quiet by default.", reminders) { toggleReminders(it) }
                OutlinedTextField(reminderTime, { reminderTime = it }, label = { Text("Reminder time HH:MM") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(quietStart, { quietStart = it }, label = { Text("Quiet start") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(quietEnd, { quietEnd = it }, label = { Text("Quiet end") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(
                    maxReminders,
                    { maxReminders = it.filter(Char::isDigit).take(1) },
                    label = { Text("Max reminders per day") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Reminder tone", fontWeight = FontWeight.SemiBold)
                OptionChips(listOf("Gentle", "Direct", "Minimal"), reminderTone) { reminderTone = it }
                Text("Quiet days", fontWeight = FontWeight.SemiBold)
                MultiSelectChips(listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"), quietDays) { quietDays = it }
                Text("Reminder types", fontWeight = FontWeight.SemiBold)
                MultiSelectChips(
                    listOf("daily", "task", "room", "weekly", "seasonal", "quick_win"),
                    reminderTypes
                ) { reminderTypes = it.ifEmpty { setOf("daily") } }
                Text(
                    "Examples: Gentle says \"Tiny reset?\" Direct says what is due. Minimal keeps it short.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                WrapButtons(
                    "Save reminder settings" to { saveSettings() },
                    "Send test reminder" to {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            actionNote = "Allow notifications, then tap test again."
                        } else {
                            actionNote = if (viewModel.showTestReminder()) "Test reminder sent." else "Notification permission is off. Enable it to test reminders."
                        }
                    }
                )
                if (!reminders) {
                    Text("Reminders are off. You can still use TidyPilot normally.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    FilledTonalButton(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        },
                        colors = tidyTonalButtonColors()
                    ) { Text("Open notification permission") }
                }
            }
        }
        item {
            SuppliesBudgetSettingsCard(state, viewModel, supplyTracking, { supplyTracking = it }, monthlyBudget, { monthlyBudget = it }) { actionNote = it }
        }
        item {
            StudioCard {
                Text("Photos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                PreferenceRow("Save original room photos", "Photos stay on this device for local scans.", savePhotos) { savePhotos = it }
                PreferenceRow("Save processed scan images", "Reserved for future local-only crops or previews.", saveProcessedImages) { saveProcessedImages = it }
                PreferenceRow("Require review before tasks", "Scan suggestions stay editable before becoming chores.", requireScanReview) { requireScanReview = it }
                Text("Default scan confidence threshold")
                OptionChips(listOf("low", "medium", "high"), scanConfidenceThreshold) { scanConfidenceThreshold = it }
                Text("Room photos are never uploaded. Scan results are estimates, and TidyPilot asks you to review suggestions before creating tasks.")
                FilledTonalButton(onClick = {
                    val deleted = deleteSavedScanPhotos(context)
                    viewModel.clearScanData()
                    actionNote = if (deleted) "Saved scan photos and scan results deleted." else "Scan results deleted. Some photo files may already be gone."
                }, colors = tidyTonalButtonColors()) { Text("Delete saved scan photos") }
                FilledTonalButton(onClick = {
                    viewModel.clearScanData()
                    actionNote = "Scan history deleted."
                }, colors = tidyTonalButtonColors()) { Text("Delete scan history") }
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
                    "Add starter routine" to {
                        viewModel.applyStarterRoutine()
                        actionNote = "Starter routine checked. Any missing everyday chores were added."
                    },
                    "Reset starter data" to {
                        viewModel.resetDemoData()
                        actionNote = "Starter data reset."
                    }
                )
                FilledTonalButton(onClick = { confirmDeleteAll = !confirmDeleteAll }, colors = tidyTonalButtonColors()) { Text(if (confirmDeleteAll) "Cancel delete" else "Delete all local data") }
                if (confirmDeleteAll) {
                    Text("This removes rooms, tasks, shifts, scans, reports, settings, and local onboarding state.")
                    Button(onClick = {
                        deleteSavedScanPhotos(context)
                        viewModel.deleteAllLocalData()
                        actionNote = "All local TidyPilot data deleted."
                        confirmDeleteAll = false
                    }, colors = ButtonDefaults.buttonColors(containerColor = TidyCoral, contentColor = Color.White)) { Text("Confirm delete all local data") }
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
            Button(onClick = { saveSettings() }, modifier = Modifier.fillMaxWidth(), colors = tidyButtonColors()) { Text("Save settings") }
            if (actionNote.isNotBlank()) Text(actionNote, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PremiumScreen(state: TidyPilotState, viewModel: TidyPilotViewModel) {
    var actionNote by rememberSaveable { mutableStateOf("") }
    val activePlan = state.settings.premiumPlan.ifBlank { "free" }
    LazyColumn(Modifier.fillMaxSize().tidyBackground(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { CompactBrandHeader("Premium", "More automation for busy homes. Core cleaning stays free.") }
        item {
            StudioCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .background(TidyMint.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = TidyDeepTeal)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("TidyPilot Free is useful by design", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text("Rooms, custom tasks, recurring schedules, Today, One Thing, starter chores, reminders, and basic need scoring stay free.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("Premium is for bigger households, deeper plans, smart setup, widgets, supplies/budget detail, advanced stats, and tuning.")
                Text("If Premium expires later, existing rooms, tasks, reports, supplies, and history remain visible. Editing locked premium-only automations can be paused until Premium is active again.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            StudioCard {
                Text("Choose a plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Mock entitlement for testing only. Google Play Billing is not connected yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                premiumPlans.forEach { plan ->
                    PremiumPlanRow(
                        name = plan.name,
                        price = plan.priceLabel,
                        value = plan.valueLabel,
                        selected = activePlan == plan.id,
                        onSelect = {
                            viewModel.setMockPremium(plan.id)
                            actionNote = "${plan.name} mock Premium enabled."
                        }
                    )
                }
                FilledTonalButton(
                    onClick = {
                        viewModel.setMockPremium("free")
                        actionNote = "Mock Premium cleared. Free mode restored."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tidyTonalButtonColors()
                ) { Text("Use free mode") }
                if (actionNote.isNotBlank()) Text(actionNote, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            StudioCard {
                Text("Feature comparison", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Locked features explain the value instead of blocking the basics.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                premiumFeatures.forEach { feature ->
                    PremiumFeatureRow(feature.name, feature.freeIncluded, feature.premiumIncluded, feature.helpText)
                }
            }
        }
        item {
            StudioCard {
                Text("Restore purchase", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Placeholder until Google Play Billing is added. Restore will reconnect an existing purchase without changing local cleaning data.")
                FilledTonalButton(
                    onClick = { actionNote = "Restore purchase placeholder. Billing is not connected yet." },
                    modifier = Modifier.fillMaxWidth(),
                    colors = tidyTonalButtonColors()
                ) { Text("Restore purchase") }
            }
        }
    }
}

@Composable
private fun PremiumPlanRow(name: String, price: String, value: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) TidyMint.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(price, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = onSelect) { Text(if (selected) "Active" else "Test") }
        }
    }
}

@Composable
private fun PremiumFeatureRow(name: String, free: Boolean, premium: Boolean, help: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Black)
            Text(help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(if (free) "Free" else "Premium", color = if (free) TidyDeepTeal else MutedOrange, fontWeight = FontWeight.Black)
        Text(if (premium) "Yes" else "-", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
    }
}

private fun premiumStatusCopy(settings: AppSettingsEntity): String = when (settings.premiumEntitlement) {
    "premium" -> when (settings.premiumPlan) {
        "lifetime" -> "Lifetime mock Premium is active."
        "monthly", "yearly" -> "${settings.premiumPlan.replaceFirstChar { it.uppercase() }} mock Premium active until ${settings.premiumExpiresAt}."
        else -> "Mock Premium is active."
    }
    else -> "Free mode active. Basic cleaning stays fully usable."
}

private fun deleteSavedScanPhotos(context: Context): Boolean =
    runCatching { File(context.filesDir, "room_scans").deleteRecursively() }.getOrDefault(false)

@Composable
private fun SuppliesBudgetSettingsCard(
    state: TidyPilotState,
    viewModel: TidyPilotViewModel,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    monthlyBudget: String,
    onMonthlyBudgetChange: (String) -> Unit,
    onMessage: (String) -> Unit
) {
    var supplyName by rememberSaveable { mutableStateOf("") }
    var supplyCost by rememberSaveable { mutableStateOf("") }
    var purchaseName by rememberSaveable { mutableStateOf("") }
    var purchaseCost by rememberSaveable { mutableStateOf("") }
    val monthlySpend = monthlySupplySpendCents(state)
    val monthlyTime = monthlyCleaningMinutes(state)
    StudioCard {
        Text("Supplies and budget", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        PreferenceRow("Track supplies", "Optional list, shopping reminders, and monthly supply totals.", enabled, onEnabledChange)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ReportMiniStat("This month", formatMoney(monthlySpend), Modifier.weight(1f))
            ReportMiniStat("Budget", if (monthlyBudget.isBlank() || monthlyBudget == "0.00") "Optional" else "${'$'}$monthlyBudget", Modifier.weight(1f))
            ReportMiniStat("Cleaning time", formatMinutes(monthlyTime), Modifier.weight(1f))
        }
        OutlinedTextField(
            monthlyBudget,
            { onMonthlyBudgetChange(it.filter { ch -> ch.isDigit() || ch == '.' }.take(7)) },
            label = { Text("Monthly cleaning budget") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Text("Shopping list", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        if (state.shoppingList.isEmpty()) {
            Text("No supplies marked low right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            state.shoppingList.take(5).forEach { supply ->
                SupplyRow(
                    supply = supply,
                    onLow = { viewModel.markSupplyRunningLow(supply, !supply.isRunningLow) },
                    onShopping = { viewModel.markSupplyOnShoppingList(supply, !supply.isOnShoppingList) }
                )
            }
        }
        Text("Add supply", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(supplyName, { supplyName = it }, label = { Text("Supply") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(supplyCost, { supplyCost = it.filter { ch -> ch.isDigit() || ch == '.' }.take(7) }, label = { Text("Cost") }, modifier = Modifier.weight(0.7f), singleLine = true)
        }
        Text("Log purchase", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(purchaseName, { purchaseName = it }, label = { Text("Bought") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(purchaseCost, { purchaseCost = it.filter { ch -> ch.isDigit() || ch == '.' }.take(7) }, label = { Text("Cost") }, modifier = Modifier.weight(0.7f), singleLine = true)
        }
        WrapButtons(
            "Add supply" to {
                val error = viewModel.saveSupply(supplyName, "general", dollarsTextToCents(supplyCost))
                onMessage(error ?: "Supply added.")
                if (error == null) {
                    supplyName = ""
                    supplyCost = ""
                }
            },
            "Save purchase" to {
                val error = viewModel.saveSupplyExpense(purchaseName, purchaseCost)
                onMessage(error ?: "Supply cost saved.")
                if (error == null) {
                    purchaseName = ""
                    purchaseCost = ""
                }
            }
        )
        Text("Basic supply tracking is free. Advanced household budgeting can be premium later.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

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
            Icon(Icons.Default.CleaningServices, null, tint = TidyAqua)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(task.name, fontWeight = FontWeight.Black)
                Text("${state.rooms.firstOrNull { it.id == task.roomId }?.name ?: "Room"} - ${task.estimatedMinutes} min - ${task.energyRequired} energy")
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
private fun CompactBrandHeader(title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BrandMark(Modifier.size(28.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "TidyPilot",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
                Icon(Icons.Default.Home, null, tint = TidyAqua)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (action != null) {
            Button(onClick = action, colors = tidyButtonColors()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(actionLabel) }
        }
    }
}

@Composable
private fun OptionChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.take(4).forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TidyMint,
                    selectedLabelColor = Color(0xFF10211E),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
    if (options.size > 4) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            options.drop(4).take(5).forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TidyMint,
                        selectedLabelColor = Color(0xFF10211E),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun MultiSelectChips(options: List<String>, selected: Set<String>, onChange: (Set<String>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { option ->
                    val isSelected = option in selected
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onChange(if (isSelected) selected - option else selected + option)
                        },
                        label = {
                            Text(
                                option.replace("_", " "),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TidyMint.copy(alpha = 0.9f),
                            selectedLabelColor = Color(0xFF10211E),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CompactChoiceChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { option ->
                    FilterChip(
                        selected = selected == option,
                        onClick = { onSelect(option) },
                        label = {
                            Text(
                                option,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TidyMint.copy(alpha = 0.9f),
                            selectedLabelColor = Color(0xFF10211E),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
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
                    FilledTonalButton(
                        onClick = action,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        colors = tidyTonalButtonColors()
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Clip
                        )
                    }
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
    val assignee = task.assignedTo?.takeIf { it.isNotBlank() }?.let { " - assigned to $it" }.orEmpty()
    return "$room - ${task.estimatedMinutes} min - ${task.energyRequired} energy$assignee"
}

private data class HomeStatus(
    val score: Int,
    val label: String,
    val message: String,
    val color: Color
)

private data class TaskUrgency(
    val label: String,
    val color: Color
)

private fun homeStatus(state: TidyPilotState): HomeStatus {
    val score = if (state.rooms.isEmpty()) {
        0
    } else {
        state.rooms.map { roomScore(it, state).score }.average().toInt().coerceIn(0, 100)
    }
    val overdueCount = overdueTasks(state).size
    return when {
        score >= 85 && overdueCount == 0 -> HomeStatus(
            score = score,
            label = "Clean",
            message = "Mostly steady. A small maintenance reset is enough today.",
            color = TidyLeaf
        )
        score >= 70 && overdueCount <= 1 -> HomeStatus(
            score = score,
            label = "Okay",
            message = "A quick pass can keep the home feeling under control.",
            color = TidyAqua
        )
        score >= 50 || overdueCount <= 3 -> HomeStatus(
            score = score,
            label = "Needs attention",
            message = "Start with one visible win, then stop if that is enough.",
            color = MutedOrange
        )
        else -> HomeStatus(
            score = score,
            label = "Getting messy",
            message = "No guilt. Pick the smallest useful reset first.",
            color = TidyCoral
        )
    }
}

private fun topTodayTasks(state: TidyPilotState): List<CleaningTaskEntity> {
    val candidates = (state.suggestedTasks + state.tasks)
        .filter { !it.isArchived }
        .distinctBy { it.id }
    return candidates
        .sortedWith(compareByDescending<CleaningTaskEntity> { todayTaskScore(it, state) }.thenBy { it.estimatedMinutes })
        .take(3)
}

private fun todayTaskScore(task: CleaningTaskEntity, state: TidyPilotState): Int {
    val room = state.rooms.firstOrNull { it.id == task.roomId }
    val need = calculateTaskNeedScore(task, room, state.completions, state.today)
    val roomNeed = room?.let { 100 - roomScore(it, state).score } ?: 20
    val roomPriority = room?.priority?.let(::priorityScore) ?: 1
    val dueScore = when {
        task.nextDueAt == null -> 6
        task.nextDueAt.isBefore(state.today) -> 40
        task.nextDueAt == state.today -> 28
        task.nextDueAt == state.today.plusDays(1) -> 12
        else -> 0
    }
    val effortBonus = when {
        task.estimatedMinutes <= 5 -> 12
        task.estimatedMinutes <= 10 -> 9
        task.estimatedMinutes <= 15 -> 5
        else -> 0
    }
    val scanIssueBonus = state.scans
        .filter { it.roomId == task.roomId }
        .maxByOrNull { it.scanDate }
        ?.let { scan -> state.issues.count { it.scanId == scan.id } * 4 }
        ?: 0
    return need.score +
        dueScore +
        priorityScore(task.priority) * 12 +
        roomPriority * 6 +
        (roomNeed / 2) +
        effortBonus +
        scanIssueBonus +
        if (task.isQuickResetTask) 8 else 0
}

private fun taskUrgency(task: CleaningTaskEntity, state: TidyPilotState): TaskUrgency = when {
    task.nextDueAt?.isBefore(state.today) == true -> TaskUrgency("Overdue", TidyCoral)
    task.priority == "urgent" -> TaskUrgency("Urgent", MutedOrange)
    task.nextDueAt == state.today -> TaskUrgency("Due today", MutedOrange)
    task.estimatedMinutes <= 5 || task.isQuickResetTask -> TaskUrgency("Quick win", TidyAqua)
    else -> TaskUrgency("Worth doing", TidyLeaf)
}

private fun taskNeedScore(task: CleaningTaskEntity, state: TidyPilotState): TaskNeedScore =
    calculateTaskNeedScore(
        task = task,
        room = state.rooms.firstOrNull { it.id == task.roomId },
        completions = state.completions,
        today = state.today
    )

@Composable
private fun needStatusColor(need: TaskNeedScore): Color = when (need.status) {
    "Fresh" -> Sage
    "Fine" -> TidyLeaf
    "Due soon" -> TidyAqua
    "Needs attention" -> MutedOrange
    else -> TidyCoral
}

private fun needFriendlyLabel(status: String): String = when (status) {
    "Fresh" -> "Still fine"
    "Fine" -> "Still fine"
    "Due soon" -> "Coming up"
    "Needs attention" -> "Worth doing soon"
    "Overdue" -> "Needs attention"
    else -> status
}

private data class SetupTaskTemplate(
    val name: String,
    val frequency: String,
    val minutes: Int,
    val priority: String = "normal",
    val difficulty: String = "easy",
    val energy: String = "low",
    val category: String = "other"
)

private fun setupRoomOptions(): List<String> = listOf(
    "Kitchen",
    "Bathroom",
    "Bedroom",
    "Living room",
    "Entryway",
    "Laundry area",
    "Dining room",
    "Office",
    "Kids room",
    "Pet area",
    "Garage",
    "Basement",
    "Custom room"
)

private fun defaultSetupRooms(homeType: String): Set<String> = when (homeType) {
    "Studio" -> setOf("Kitchen", "Bathroom", "Bedroom", "Entryway")
    "House" -> setOf("Kitchen", "Bathroom", "Bedroom", "Living room", "Entryway", "Laundry area", "Garage", "Basement")
    "Shared house" -> setOf("Kitchen", "Bathroom", "Bedroom", "Living room", "Laundry area")
    "Custom" -> setOf("Kitchen", "Bathroom")
    else -> setOf("Kitchen", "Bathroom", "Bedroom", "Living room", "Entryway", "Laundry area")
}

private fun setupRoomNames(selectedRooms: Set<String>, customRoomName: String): List<String> =
    selectedRooms
        .filter { it != "Custom room" }
        .plus(if ("Custom room" in selectedRooms && customRoomName.isNotBlank()) listOf(customRoomName.trim()) else emptyList())
        .distinctBy { it.lowercase() }

private fun defaultSetupTaskKeys(roomNames: Set<String>): Set<String> =
    roomNames
        .filter { it != "Custom room" }
        .flatMap { room -> setupTaskTemplates(room).take(defaultSetupTaskCount(room)).map { setupTaskKey(room, it.name) } }
        .toSet()

private fun defaultSetupTaskCount(room: String): Int = when (room.lowercase()) {
    "kitchen" -> 5
    "bathroom" -> 5
    "bedroom" -> 4
    "living room" -> 3
    "laundry area" -> 3
    else -> 2
}

private fun setupTaskKey(room: String, taskName: String): String = "${room.trim()}|${taskName.trim()}"

private fun setupTaskTemplates(room: String): List<SetupTaskTemplate> = when (room.lowercase()) {
    "kitchen" -> listOf(
        SetupTaskTemplate("Wipe counters", "daily", 5, "high", category = "surface wipe"),
        SetupTaskTemplate("Wash dishes", "daily", 10, "high", "easy", "medium", "dishes"),
        SetupTaskTemplate("Clean sink", "every few days", 5, category = "sink_full"),
        SetupTaskTemplate("Sweep floor", "every few days", 8, category = "floor clutter"),
        SetupTaskTemplate("Mop floor", "weekly", 15, "normal", "medium", "medium", "floor clutter"),
        SetupTaskTemplate("Clean fridge", "monthly", 25, "low", "medium", "medium", "other"),
        SetupTaskTemplate("Take out trash", "every few days", 3, "high", category = "trash"),
        SetupTaskTemplate("Clean microwave", "weekly", 8, category = "surface wipe")
    )
    "bathroom" -> listOf(
        SetupTaskTemplate("Clean toilet", "weekly", 8, "high", category = "bathroom reset"),
        SetupTaskTemplate("Wipe sink", "every few days", 5, category = "surface wipe"),
        SetupTaskTemplate("Clean mirror", "weekly", 4, category = "surface wipe"),
        SetupTaskTemplate("Scrub shower/tub", "weekly", 20, "normal", "medium", "medium", "bathroom reset"),
        SetupTaskTemplate("Replace towels", "weekly", 5, category = "laundry"),
        SetupTaskTemplate("Empty trash", "weekly", 3, category = "trash"),
        SetupTaskTemplate("Mop floor", "weekly", 12, "normal", "medium", "medium", "floor clutter")
    )
    "bedroom" -> listOf(
        SetupTaskTemplate("Make bed", "daily", 3, category = "bed reset"),
        SetupTaskTemplate("Change sheets", "weekly", 12, "normal", "medium", "medium", "laundry"),
        SetupTaskTemplate("Put away clothes", "every few days", 10, "high", "easy", "low", "laundry"),
        SetupTaskTemplate("Vacuum", "weekly", 12, "normal", "medium", "medium", "floor clutter"),
        SetupTaskTemplate("Dust surfaces", "weekly", 8, category = "surface wipe")
    )
    "living room" -> listOf(
        SetupTaskTemplate("10-minute room reset", "daily", 10, "high", "easy", "medium", "floor clutter"),
        SetupTaskTemplate("Clear one surface", "daily", 5, category = "clutter"),
        SetupTaskTemplate("Vacuum", "weekly", 15, "normal", "medium", "medium", "floor clutter"),
        SetupTaskTemplate("Dust surfaces", "weekly", 8, category = "surface wipe")
    )
    "entryway" -> listOf(
        SetupTaskTemplate("Clear shoes and bags", "every few days", 5, category = "floor clutter"),
        SetupTaskTemplate("Take out trash or mail", "weekly", 5, category = "trash"),
        SetupTaskTemplate("Sweep entry", "weekly", 8, category = "floor clutter")
    )
    "laundry area" -> listOf(
        SetupTaskTemplate("Start laundry", "every few days", 5, "high", category = "laundry"),
        SetupTaskTemplate("Switch laundry", "every few days", 3, "high", category = "laundry"),
        SetupTaskTemplate("Fold one basket", "every few days", 12, "normal", "medium", "medium", "laundry"),
        SetupTaskTemplate("Clear lint and supplies", "weekly", 5, category = "clutter")
    )
    "dining room" -> listOf(
        SetupTaskTemplate("Clear table", "daily", 5, "high", category = "clutter"),
        SetupTaskTemplate("Wipe table", "daily", 4, category = "surface wipe"),
        SetupTaskTemplate("Sweep floor", "weekly", 8, category = "floor clutter")
    )
    "office" -> listOf(
        SetupTaskTemplate("Clear desk surface", "every few days", 5, category = "clutter"),
        SetupTaskTemplate("Sort loose papers", "weekly", 10, "normal", "medium", "low", "clutter"),
        SetupTaskTemplate("Empty trash", "weekly", 3, category = "trash")
    )
    "kids room" -> listOf(
        SetupTaskTemplate("Clear floor path", "daily", 5, "high", category = "floor clutter"),
        SetupTaskTemplate("Put toys in bins", "daily", 8, "normal", category = "clutter"),
        SetupTaskTemplate("Laundry to hamper", "every few days", 5, category = "laundry"),
        SetupTaskTemplate("Change sheets", "weekly", 12, "normal", "medium", "medium", "laundry")
    )
    "pet area" -> listOf(
        SetupTaskTemplate("Refresh pet station", "daily", 5, "high", category = "other"),
        SetupTaskTemplate("Sweep pet area", "every few days", 8, category = "floor clutter"),
        SetupTaskTemplate("Wash pet bowls", "every few days", 5, category = "dishes")
    )
    "garage" -> listOf(
        SetupTaskTemplate("Clear one walking path", "weekly", 10, "high", "medium", "medium", "floor clutter"),
        SetupTaskTemplate("Sort one shelf", "monthly", 15, "normal", "medium", "medium", "clutter"),
        SetupTaskTemplate("Take out trash", "weekly", 5, category = "trash")
    )
    "basement" -> listOf(
        SetupTaskTemplate("Clear one walking path", "weekly", 10, "high", "medium", "medium", "floor clutter"),
        SetupTaskTemplate("Sort one visible pile", "weekly", 12, "normal", "medium", "medium", "clutter"),
        SetupTaskTemplate("Laundry to hamper", "every few days", 5, category = "laundry"),
        SetupTaskTemplate("Take out trash", "weekly", 5, category = "trash")
    )
    else -> emptyList()
}

private fun cleaningStyleCopy(style: String): String = when (style) {
    "Little every day" -> "Short recurring chores show up often so the home does not need a big rescue reset."
    "Weekly reset" -> "Most starter chores are grouped into weekly maintenance."
    "Weekend deep clean" -> "Bigger tasks are kept for slower days, while tiny resets stay available."
    "As-needed only" -> "Tasks are created with light reminders so you can run the plan when the room needs it."
    else -> "Uses balanced weekly defaults that you can edit later."
}

private fun setupFrequency(template: SetupTaskTemplate, style: String): String = when (style) {
    "Little every day" -> when (template.frequency) {
        "monthly" -> "monthly"
        "weekly" -> "every few days"
        else -> template.frequency
    }
    "Weekly reset" -> if (template.frequency == "daily") "every few days" else template.frequency
    "Weekend deep clean" -> when {
        template.minutes >= 12 -> "weekly"
        template.frequency == "daily" -> "every few days"
        else -> template.frequency
    }
    "As-needed only" -> "one-time"
    else -> template.frequency
}

private fun buildSetupPlan(
    roomNames: List<String>,
    selectedTaskKeys: Set<String>,
    cleaningStyle: String,
    state: TidyPilotState
): Pair<List<RoomEntity>, List<CleaningTaskEntity>> {
    val existingRoomsByName = state.rooms.associateBy { it.name.trim().lowercase() }
    val rooms = roomNames.map { name ->
        existingRoomsByName[name.trim().lowercase()] ?: RoomEntity(
            name = name,
            roomType = name,
            iconName = setupIconName(name),
            priority = setupRoomPriority(name),
            defaultTaskIntensity = setupIntensity(cleaningStyle),
            defaultTaskFrequency = if (cleaningStyle == "As-needed only") "monthly" else "weekly",
            notes = "Added from home setup."
        )
    }
    val roomByName = rooms.associateBy { it.name.trim().lowercase() }
    val tasks = roomNames.flatMap { roomName ->
        setupTaskTemplates(roomName).filter { setupTaskKey(roomName, it.name) in selectedTaskKeys }.mapNotNull { template ->
            val room = roomByName[roomName.trim().lowercase()] ?: return@mapNotNull null
            CleaningTaskEntity(
                name = template.name,
                roomId = room.id,
                description = "Starter task from home setup. Cleaning style: $cleaningStyle. Dirtiness trigger: ${template.category}.",
                priority = template.priority,
                estimatedMinutes = template.minutes,
                difficulty = template.difficulty,
                energyRequired = template.energy,
                frequencyType = setupFrequency(template, cleaningStyle),
                preferredTime = if (cleaningStyle == "Weekend deep clean") "day off" else "anytime",
                isQuickResetTask = template.minutes <= 5,
                isDeepCleanTask = template.minutes >= 15,
                photoDetectableCategory = template.category,
                nextDueAt = LocalDate.now()
            )
        }
    }
    return rooms to tasks
}

private fun setupIconName(roomName: String): String = when (roomName.lowercase()) {
    "kitchen" -> "kitchen"
    "bathroom" -> "bathroom"
    "bedroom", "kids room" -> "bed"
    "laundry area" -> "laundry"
    "garage" -> "garage"
    "basement" -> "basement"
    else -> "room"
}

private fun setupRoomPriority(roomName: String): String = when (roomName.lowercase()) {
    "kitchen", "bathroom", "bedroom" -> "high"
    "kids room", "pet area", "basement" -> "normal"
    else -> "normal"
}

private fun setupIntensity(style: String): String = when (style) {
    "Little every day" -> "low"
    "Weekend deep clean" -> "high"
    else -> "medium"
}

private fun energyTodoTasks(state: TidyPilotState, energy: String): List<CleaningTaskEntity> {
    val base = state.suggestedTasks.ifEmpty {
        state.tasks.filter { !it.isArchived && (it.nextDueAt == null || !it.nextDueAt.isAfter(state.today)) }
    }
    val filtered = when (energy) {
        "very low" -> base.filter { it.energyRequired == "low" && it.estimatedMinutes <= 5 }
        "low" -> base.filter { it.energyRequired == "low" && it.estimatedMinutes <= 10 }
        "high" -> base
        else -> base.filter { it.energyRequired != "high" && it.estimatedMinutes <= 15 }
    }
    return filtered
        .sortedWith(compareBy<CleaningTaskEntity> { it.estimatedMinutes }.thenByDescending { priorityScore(it.priority) })
        .take(if (energy == "high") 5 else 3)
        .ifEmpty { state.lowEnergyTask?.let { listOf(it) } ?: emptyList() }
}

private fun dollarsTextToCents(value: String): Int {
    val cleaned = value.trim().removePrefix("$")
    if (cleaned.isBlank()) return 0
    val parts = cleaned.split(".")
    val dollars = parts.getOrNull(0)?.toIntOrNull() ?: return 0
    val cents = parts.getOrNull(1)?.padEnd(2, '0')?.take(2)?.toIntOrNull() ?: 0
    return (dollars * 100 + cents).coerceAtLeast(0)
}

private fun centsToDollarsText(cents: Int): String =
    "%d.%02d".format(cents / 100, cents % 100)

private fun formatMoney(cents: Int): String =
    "$${centsToDollarsText(cents)}"

private fun monthlySupplySpendCents(state: TidyPilotState, month: YearMonth = YearMonth.now()): Int =
    state.supplyExpenses
        .filter { YearMonth.from(it.purchasedAt) == month }
        .sumOf { it.costCents }

private fun monthlyCleaningMinutes(state: TidyPilotState, month: YearMonth = YearMonth.now()): Int =
    state.completions
        .filter { YearMonth.from(it.completedAt.toLocalDate()) == month }
        .sumOf { it.durationMinutes }

private fun formatMinutes(minutes: Int): String =
    if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"

private fun monthlyBudgetLabel(state: TidyPilotState): String {
    val budget = state.settings.monthlyCleaningBudgetCents
    return if (budget <= 0) "Optional" else "${formatMoney(monthlySupplySpendCents(state))} / ${formatMoney(budget)}"
}

private fun overdueTasks(state: TidyPilotState): List<CleaningTaskEntity> =
    state.tasks
        .filter { !it.isArchived && it.nextDueAt?.isBefore(state.today) == true }
        .sortedWith(compareByDescending<CleaningTaskEntity> { priorityScore(it.priority) }.thenBy { it.nextDueAt })

private fun fiveMinuteTask(state: TidyPilotState): CleaningTaskEntity? {
    val base = state.suggestedTasks + state.tasks
    return base
        .filter { !it.isArchived && it.estimatedMinutes <= 5 && it.energyRequired != "high" }
        .distinctBy { it.id }
        .sortedWith(
            compareByDescending<CleaningTaskEntity> { it.isQuickResetTask }
                .thenByDescending { priorityScore(it.priority) }
                .thenBy { it.nextDueAt ?: state.today }
        )
        .firstOrNull()
}

private fun quickWinTask(state: TidyPilotState): CleaningTaskEntity? {
    val commonQuickCategories = setOf("trash", "dishes", "laundry", "surface wipe", "clutter", "sink_full", "wipe_needed")
    return (state.suggestedTasks + state.tasks)
        .filter { !it.isArchived && it.estimatedMinutes in 2..5 && it.energyRequired == "low" }
        .distinctBy { it.id }
        .sortedWith(
            compareByDescending<CleaningTaskEntity> { it.photoDetectableCategory in commonQuickCategories || it.isQuickResetTask }
                .thenByDescending { todayTaskScore(it, state) }
                .thenBy { it.estimatedMinutes }
        )
        .firstOrNull()
}

private fun energyTodoCopy(energy: String): String = when (energy) {
    "very low" -> "No shame. Here are the tiniest useful resets."
    "low" -> "A small reset still counts. Keep it short."
    "high" -> "You have room for a bigger reset if you want it."
    else -> "A few manageable chores, not the whole house."
}

private fun repeatLabel(frequency: String): String = when (frequency) {
    "one-time" -> "once"
    "daily" -> "daily"
    "every few days" -> "3 days"
    "weekly" -> "weekly"
    "monthly" -> "monthly"
    else -> frequency
}

private fun repeatPreview(frequency: String): String = when (frequency) {
    "daily" -> "tomorrow"
    "every few days" -> "in about 3 days"
    "weekly" -> "next week"
    "monthly" -> "next month"
    else -> "only when you add it again"
}

private fun dueChoiceLabel(choice: String): String = when (choice) {
    "today" -> "today"
    "tomorrow" -> "tomorrow"
    "next week" -> "next week"
    "none" -> "when you choose it"
    else -> choice
}

private fun nextDueLabel(task: CleaningTaskEntity, today: LocalDate): String = when {
    task.nextDueAt == null -> "One-time task"
    task.nextDueAt.isBefore(today) -> "Overdue - repeats ${repeatLabel(task.frequencyType)}"
    task.nextDueAt == today -> "Due today - repeats ${repeatLabel(task.frequencyType)}"
    task.nextDueAt == today.plusDays(1) -> "Due tomorrow - repeats ${repeatLabel(task.frequencyType)}"
    else -> "Due ${task.nextDueAt.format(DateTimeFormatter.ofPattern("MMM d"))} - repeats ${repeatLabel(task.frequencyType)}"
}

private fun modeLabel(mode: String): String = mode.replaceFirstChar { it.uppercase() }

@Composable
private fun taskEnergyColor(task: CleaningTaskEntity): Color = when (task.energyRequired) {
    "low" -> TidyLeaf
    "medium" -> TidyAqua
    else -> TidyCoral
}

@Composable
private fun tidyButtonColors() = ButtonDefaults.buttonColors(
    containerColor = TidyAqua,
    contentColor = Color(0xFF101816)
)

@Composable
private fun tidyTonalButtonColors() = ButtonDefaults.filledTonalButtonColors(
    containerColor = TidyDeepTeal,
    contentColor = Cream
)

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
    "Needs a quick reset" -> TidyAqua
    "Priority room" -> if (score.score < 25 || score.overdueTasks >= 3) TidyCoral else MutedOrange
    else -> MutedOrange
}

@Composable
private fun roomProgressColor(score: RoomScore) = when {
    score.label == "Good" -> Sage
    score.label == "Needs a quick reset" -> TidyAqua
    score.label == "Priority room" && score.score < 25 -> TidyCoral
    else -> MutedOrange
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

private fun dashboardWorkStatusLabel(workStatus: String, state: TidyPilotState): String = when (workStatus.lowercase()) {
    "day off", "free day" -> "Day off"
    "before work" -> "Before work"
    "after work" -> "After work"
    "too tight today" -> "Too tight"
    "working today" -> "Working"
    else -> if (state.todayShift == null) "Day off" else "Working"
}

private fun dashboardWorkStatusDetail(workStatus: String, state: TidyPilotState): String {
    val normalized = workStatus.lowercase()
    val shift = state.todayShift
    return when {
        normalized == "day off" || normalized == "free day" || shift == null -> "Good for a reset"
        normalized == "before work" -> "Quick tasks before ${shift.startTime}"
        normalized == "after work" -> "Low-energy reset after shift"
        normalized == "too tight today" -> "Smallest useful task"
        else -> "${shift.startTime} - ${shift.endTime}"
    }
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

private fun oneThingMinutes(mode: String?): Int? = when (mode) {
    "2 minutes" -> 2
    "5 minutes" -> 5
    "15 minutes" -> 15
    "Full reset" -> null
    else -> null
}

private fun oneThingEnergy(mode: String?, state: TidyPilotState): String = when (mode) {
    "2 minutes" -> "very low"
    "5 minutes" -> "low"
    "15 minutes" -> state.latestCheckIn?.energyLevel ?: state.settings.defaultEnergyLevel
    "Full reset" -> "high"
    else -> state.latestCheckIn?.energyLevel ?: state.settings.defaultEnergyLevel
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
    val scanTasksCompleted: Int,
    val monthlySupplySpendCents: Int,
    val monthlyCleaningMinutes: Int,
    val shoppingListCount: Int
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
        scanTasksCompleted = state.completions.count { it.taskId in scanSuggestedIds },
        monthlySupplySpendCents = monthlySupplySpendCents(state),
        monthlyCleaningMinutes = monthlyCleaningMinutes(state),
        shoppingListCount = state.shoppingList.size
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
    Cleaning supply spend this month: ${formatMoney(stats.monthlySupplySpendCents)}
    Cleaning budget: ${monthlyBudgetLabel(state)}
    Supplies on shopping list: ${stats.shoppingListCount}
    Time spent cleaning this month: ${formatMinutes(stats.monthlyCleaningMinutes)}
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
        "Cleaning supply spend this month,${csv(formatMoney(stats.monthlySupplySpendCents))}",
        "Cleaning budget,${csv(monthlyBudgetLabel(state))}",
        "Supplies on shopping list,${csv(stats.shoppingListCount.toString())}",
        "Time spent cleaning this month,${csv(formatMinutes(stats.monthlyCleaningMinutes))}",
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


