package com.smithware.mvpstarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smithware.mvpstarter.data.ProjectEntity
import com.smithware.mvpstarter.data.defaultChecklist
import com.smithware.mvpstarter.ui.theme.Charcoal
import com.smithware.mvpstarter.ui.theme.Cream
import com.smithware.mvpstarter.ui.theme.Graphite
import com.smithware.mvpstarter.ui.theme.Lime
import com.smithware.mvpstarter.ui.theme.MvpStarterTheme
import com.smithware.mvpstarter.ui.theme.WarmOrange
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MvpStarterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            MvpStarterTheme(darkTheme = state.darkMode) {
                MvpStarterApp(state = state, viewModel = viewModel)
            }
        }
    }
}

private sealed class Route(val value: String, val label: String, val icon: @Composable () -> Unit) {
    data object Dashboard : Route("dashboard", "Dashboard", { Icon(Icons.Default.Dashboard, null) })
    data object Add : Route("edit", "Add", { Icon(Icons.Default.Add, null) })
    data object Export : Route("export", "Export", { Icon(Icons.Default.FileDownload, null) })
    data object Settings : Route("settings", "Settings", { Icon(Icons.Default.Settings, null) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MvpStarterApp(state: MvpStarterState, viewModel: MvpStarterViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: Route.Dashboard.value
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MVP Starter", fontWeight = FontWeight.Black)
                        Text("Your app ideas stay on this device.", style = MaterialTheme.typography.labelSmall)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                listOf(Route.Dashboard, Route.Add, Route.Export, Route.Settings).forEach { route ->
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
        NavHost(
            navController = nav,
            startDestination = Route.Dashboard.value,
            modifier = Modifier.padding(padding)
        ) {
            composable(Route.Dashboard.value) {
                DashboardScreen(state, nav, viewModel)
            }
            composable(Route.Add.value) {
                ProjectFormScreen(project = null, viewModel = viewModel, nav = nav, snackbar = snackbar)
            }
            composable(
                "edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val project = state.projects.firstOrNull { it.id == entry.arguments?.getString("id") }
                ProjectFormScreen(project = project, viewModel = viewModel, nav = nav, snackbar = snackbar)
            }
            composable(
                "detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val project = state.projects.firstOrNull { it.id == entry.arguments?.getString("id") }
                DetailScreen(project, nav, viewModel)
            }
            composable(Route.Export.value) {
                ExportScreen(state, nav, snackbar)
            }
            composable(Route.Settings.value) {
                SettingsScreen(state, viewModel)
            }
        }
    }
}

@Composable
private fun DashboardScreen(state: MvpStarterState, nav: NavHostController, viewModel: MvpStarterViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().studioBackground(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeroCard(
                title = "Ship local-first Android MVPs faster.",
                body = "A local product studio dashboard for MVP scope, launch readiness, saved records, and Codex-ready output.",
                action = { nav.navigate(Route.Add.value) }
            )
        }
        item { SectionHeader("Build progress", "Next actions for public-release planning") }
        item {
            StudioCard {
                Text("${state.averageProgress}% overall", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { state.averageProgress / 100f }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill("${state.projects.size} projects")
                    StatusPill("Room local")
                    StatusPill("No cloud")
                }
            }
        }
        item { SectionHeader("Projects", "Create, edit, inspect, archive, or delete") }
        if (state.projects.isEmpty()) {
            item { EmptyState("No plans yet", "Create the first focused app plan.") { nav.navigate(Route.Add.value) } }
        } else {
            items(state.projects, key = { it.id }) { project ->
                ProjectRow(project, nav, viewModel)
            }
        }
    }
}

@Composable
private fun ProjectFormScreen(
    project: ProjectEntity?,
    viewModel: MvpStarterViewModel,
    nav: NavHostController,
    snackbar: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var name by remember(project?.id) { mutableStateOf(project?.name ?: "") }
    var idea by remember(project?.id) { mutableStateOf(project?.idea ?: "") }
    var audience by remember(project?.id) { mutableStateOf(project?.audience ?: "Managers") }
    var problem by remember(project?.id) { mutableStateOf(project?.coreProblem ?: "") }
    var status by remember(project?.id) { mutableStateOf(project?.status ?: "Draft") }
    var notes by remember(project?.id) { mutableStateOf(project?.notes ?: "") }
    var progress by remember(project?.id) { mutableStateOf(project?.progress?.toFloat() ?: 25f) }
    val checklist = remember(project?.id) {
        mutableStateListOf<String>().apply { addAll(project?.checklistItems() ?: defaultChecklist.take(3)) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().studioBackground(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader(if (project == null) "Create project" else "Edit project", "Shape the core build workflow") }
        item {
            StudioCard {
                OutlinedTextField(name, { name = it }, label = { Text("Project name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(idea, { idea = it }, label = { Text("App goal") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(audience, { audience = it }, label = { Text("Target audience") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(problem, { problem = it }, label = { Text("Core problem") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(status, { status = it }, label = { Text("Status") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Text("MVP progress: ${progress.toInt()}%", fontWeight = FontWeight.Bold)
                Slider(value = progress, onValueChange = { progress = it }, valueRange = 0f..100f)
            }
        }
        item { SectionHeader("Launch checklist", "Must-have v1 steps") }
        items(checklist.size) { index ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = checklist[index],
                    onValueChange = { checklist[index] = it },
                    label = { Text("Checklist item") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = { checklist.removeAt(index) }) { Icon(Icons.Default.Delete, "Delete checklist item") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { checklist.add("") }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Item") }
                Button(onClick = {
                    val error = viewModel.saveProject(project, name, idea, audience, problem, status, notes, checklist, progress.toInt())
                    if (error == null) nav.navigate(Route.Dashboard.value) { popUpTo(Route.Dashboard.value) }
                    else scope.launch { snackbar.showSnackbar(error) }
                }) { Icon(Icons.Default.CheckCircle, null); Spacer(Modifier.width(6.dp)); Text("Save") }
            }
        }
    }
}

@Composable
private fun DetailScreen(project: ProjectEntity?, nav: NavHostController, viewModel: MvpStarterViewModel) {
    if (project == null) {
        EmptyState("Project not found", "Return to the dashboard and pick a saved plan.") { nav.navigate(Route.Dashboard.value) }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().studioBackground(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader(project.name, project.status) }
        item {
            StudioCard {
                Text(project.idea, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Audience: ${project.audience}")
                Text("Problem: ${project.coreProblem}")
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { project.progress / 100f }, modifier = Modifier.fillMaxWidth())
                Text("${project.progress}% complete", style = MaterialTheme.typography.labelLarge)
            }
        }
        item { SectionHeader("Checklist", "Launch readiness") }
        items(project.checklistItems()) { item ->
            StudioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Lime)
                    Spacer(Modifier.width(8.dp))
                    Text(item, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { nav.navigate("edit/${project.id}") }) { Icon(Icons.Default.Edit, null); Spacer(Modifier.width(6.dp)); Text("Edit") }
                FilledTonalButton(onClick = { viewModel.archiveProject(project.id); nav.navigate(Route.Dashboard.value) }) { Icon(Icons.Default.Archive, null); Spacer(Modifier.width(6.dp)); Text("Archive") }
                IconButton(onClick = { viewModel.deleteProject(project); nav.navigate(Route.Dashboard.value) }) { Icon(Icons.Default.Delete, "Delete project") }
            }
        }
    }
}

@Composable
private fun ExportScreen(state: MvpStarterState, nav: NavHostController, snackbar: SnackbarHostState) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val selected = state.activeProject
    val prompt = selected?.buildPrompt().orEmpty()
    LazyColumn(
        modifier = Modifier.fillMaxSize().studioBackground(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader("Export", "Prepare Codex-ready output") }
        if (selected == null) {
            item { EmptyState("No project to export", "Create a project before generating a build prompt.") { nav.navigate(Route.Add.value) } }
        } else {
            item {
                StudioCard {
                    Text("Selected: ${selected.name}", fontWeight = FontWeight.Black)
                    Text("Freemium v1: copy prompt text locally. Premium exports can add TXT, Markdown, PDF, templates, backup, and GitHub-ready briefs.")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        clipboard.setText(AnnotatedString(prompt))
                        scope.launch { snackbar.showSnackbar("Build prompt copied locally.") }
                    }) { Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Copy prompt") }
                }
            }
            item {
                StudioCard {
                    Text(prompt, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: MvpStarterState, viewModel: MvpStarterViewModel) {
    var title by remember(state.settings.title) { mutableStateOf(state.settings.title) }
    var notes by remember(state.settings.notes) { mutableStateOf(state.settings.notes) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().studioBackground(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader("Settings", "Local preferences and release guardrails") }
        item {
            StudioCard {
                PreferenceRow("Dark mode", "Use charcoal studio surfaces.", state.darkMode, viewModel::toggleDarkMode)
                PreferenceRow("Compact cards", "Tighter dashboard scanning.", state.compactCards, viewModel::toggleCompactCards)
                OutlinedTextField(title, { title = it }, label = { Text("Studio title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Privacy note") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Button(onClick = { viewModel.updateSettings(title, notes) }) { Text("Save settings") }
            }
        }
        item {
            StudioCard {
                Text("v1 boundaries", fontWeight = FontWeight.Black)
                Text("No login, no cloud, no paid APIs, and no uploads. Room stores projects locally and DataStore stores preferences.")
                Spacer(Modifier.height(8.dp))
                Text("Risks to watch: scope creep, unclear data model, overloaded first release, and vague launch copy.")
            }
        }
    }
}

@Composable
private fun ProjectRow(project: ProjectEntity, nav: NavHostController, viewModel: MvpStarterViewModel) {
    var confirmDelete by remember { mutableStateOf(false) }
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(project.coreProblem, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { project.progress / 100f }, modifier = Modifier.fillMaxWidth())
            }
            IconButton(onClick = { nav.navigate("detail/${project.id}") }) { Icon(Icons.Default.Workspaces, "Open detail") }
            IconButton(onClick = { nav.navigate("edit/${project.id}") }) { Icon(Icons.Default.Edit, "Edit") }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Delete") }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete project?") },
            text = { Text("This removes the local plan from this device.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteProject(project); confirmDelete = false }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HeroCard(title: String, body: String, action: () -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Charcoal),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Charcoal, Graphite, WarmOrange.copy(alpha = 0.85f))))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Smithware Studios", color = Lime, fontWeight = FontWeight.Bold)
                Text(title, color = Cream, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(body, color = Cream)
                Button(onClick = action) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Create plan") }
            }
        }
    }
}

@Composable
private fun StudioCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
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
private fun StatusPill(text: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PreferenceRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun EmptyState(title: String, body: String, action: () -> Unit) {
    Box(Modifier.fillMaxSize().studioBackground().padding(16.dp), contentAlignment = Alignment.Center) {
        StudioCard {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(body)
            Button(onClick = action) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Create") }
        }
    }
}

@Composable
private fun Modifier.studioBackground(): Modifier =
    this.background(
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        )
    )
