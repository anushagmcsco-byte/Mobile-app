package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CourseEntity
import com.example.data.entity.ModuleEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCourseEditorScreen(
    viewModel: MainViewModel,
    courseId: Int,
    onBack: () -> Unit
) {
    val course by viewModel.repository.getCourseById(courseId)
        .collectAsStateWithLifecycle(initialValue = null)
    val modules by viewModel.repository.getModulesForCourse(courseId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var showCourseEditDialog by remember { mutableStateOf(false) }
    var editingModule by remember { mutableStateOf<ModuleEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<ModuleEntity?>(null) }

    val activeCourse = course

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course Content Manager", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_course_editor_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (activeCourse != null) {
                        IconButton(onClick = { showCourseEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Course Info", tint = ExecutiveBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingModule = ModuleEntity(
                        courseId = courseId,
                        title = "",
                        summary = "",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        videoDurationSeconds = 480,
                        moduleOrder = modules.size + 1,
                        transcript = "Add video transcript here...",
                        keyTakeaways = "Add key takeaway bullet points here..."
                    )
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Module") },
                containerColor = ExecutiveBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_module_fab")
            )
        },
        containerColor = BackgroundWhite
    ) { innerPadding ->
        if (activeCourse == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ExecutiveBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Course Summary Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = CardDefaults.outlinedCardBorder(enabled = true)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = ExecutiveBlue.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = activeCourse.category.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ExecutiveBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text("${modules.size} Modules Included", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = activeCourse.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = activeCourse.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Player Engine Format Explainer Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    border = CardDefaults.outlinedCardBorder(enabled = true)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ExecutiveBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Video Player Format Compatibility", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                "• HTML5 Web Engine plays direct MP4 files & YouTube links (embeds).\n" +
                                "• Native Player streams raw MP4 URLs with Android hardware acceleration.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Text("Course Modules", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)

                if (modules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No modules added yet. Tap '+ Add Module' below.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    }
                } else {
                    modules.forEachIndexed { index, module ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder(enabled = true)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = ExecutiveBlue,
                                            shape = CircleShape,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = module.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = { editingModule = module }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Module", tint = ExecutiveBlue)
                                        }
                                        IconButton(onClick = { showDeleteConfirmDialog = module }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Module", tint = Color.Red)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = module.summary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (module.videoUrl.contains("youtube")) "Format: YouTube HTML5 Stream" else "Format: Direct MP4 Stream",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Edit Course Information
    if (showCourseEditDialog && activeCourse != null) {
        EditCourseDialog(
            course = activeCourse,
            onDismiss = { showCourseEditDialog = false },
            onSave = { updatedCourse ->
                viewModel.adminSaveCourse(updatedCourse) {
                    showCourseEditDialog = false
                }
            }
        )
    }

    // Dialog: Edit / Add Module Information
    editingModule?.let { moduleToEdit ->
        EditModuleDialog(
            module = moduleToEdit,
            onDismiss = { editingModule = null },
            onSave = { updatedModule ->
                viewModel.adminSaveModule(updatedModule) {
                    editingModule = null
                }
            }
        )
    }

    // Dialog: Confirm Delete Module
    showDeleteConfirmDialog?.let { moduleToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Module?") },
            text = { Text("Are you sure you want to delete '${moduleToDelete.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminDeleteModule(moduleToDelete.id) {
                            showDeleteConfirmDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EditCourseDialog(
    course: CourseEntity,
    onDismiss: () -> Unit,
    onSave: (CourseEntity) -> Unit
) {
    var title by remember { mutableStateOf(course.title) }
    var description by remember { mutableStateOf(course.description) }
    var category by remember { mutableStateOf(course.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Course Meta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Course Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_course_title_input")
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Engineering, Sales)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_course_category_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("edit_course_desc_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(course.copy(title = title, description = description, category = category))
                },
                colors = ButtonDefaults.buttonColors(containerColor = ExecutiveBlue)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditModuleDialog(
    module: ModuleEntity,
    onDismiss: () -> Unit,
    onSave: (ModuleEntity) -> Unit
) {
    var title by remember { mutableStateOf(module.title) }
    var summary by remember { mutableStateOf(module.summary) }
    var videoUrl by remember { mutableStateOf(if (module.videoUrl.isBlank()) "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" else module.videoUrl) }
    var durationSecsStr by remember { mutableStateOf(if (module.videoDurationSeconds <= 0) "480" else module.videoDurationSeconds.toString()) }
    var transcript by remember { mutableStateOf(module.transcript) }
    var takeaways by remember { mutableStateOf(module.keyTakeaways) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (module.id == 0) "Add Module & Video Details" else "Edit Module & Video Details", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Module Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("module_editor_title_input")
                )

                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Module Summary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("module_editor_summary_input")
                )

                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text("Video MP4 / YouTube Stream URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("module_editor_video_url_input")
                )

                Text("Quick Preset Samples:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AssistChip(
                        onClick = { videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" },
                        label = { Text("MP4 Sample 1", style = MaterialTheme.typography.labelSmall) }
                    )
                    AssistChip(
                        onClick = { videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4" },
                        label = { Text("MP4 Sample 2", style = MaterialTheme.typography.labelSmall) }
                    )
                    AssistChip(
                        onClick = { videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ" },
                        label = { Text("YouTube Link", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                OutlinedTextField(
                    value = durationSecsStr,
                    onValueChange = { durationSecsStr = it.filter { char -> char.isDigit() } },
                    label = { Text("Video Duration (Seconds)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("module_editor_duration_input")
                )

                OutlinedTextField(
                    value = transcript,
                    onValueChange = { transcript = it },
                    label = { Text("Video Transcript Text") },
                    modifier = Modifier.fillMaxWidth().height(90.dp).testTag("module_editor_transcript_input")
                )

                OutlinedTextField(
                    value = takeaways,
                    onValueChange = { takeaways = it },
                    label = { Text("Key Takeaways (One per line)") },
                    modifier = Modifier.fillMaxWidth().height(90.dp).testTag("module_editor_takeaways_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duration = durationSecsStr.toIntOrNull() ?: 480
                    onSave(
                        module.copy(
                            title = title.ifBlank { "Untitled Module" },
                            summary = summary.ifBlank { "No summary provided." },
                            videoUrl = videoUrl,
                            videoDurationSeconds = duration,
                            transcript = transcript.ifBlank { "No transcript provided." },
                            keyTakeaways = takeaways.ifBlank { "No takeaways provided." }
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ExecutiveBlue),
                modifier = Modifier.testTag("save_module_button")
            ) {
                Text("Save Module")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
