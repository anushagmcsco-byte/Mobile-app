package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CourseEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenUserManagement: () -> Unit,
    onEditCourse: (Int) -> Unit
) {
    val totalUsers by viewModel.totalUserCount.collectAsStateWithLifecycle()
    val totalStudents by viewModel.totalStudentCount.collectAsStateWithLifecycle()
    val totalCourses by viewModel.totalCourseCount.collectAsStateWithLifecycle()
    val allCoursesList by viewModel.allCourses.collectAsStateWithLifecycle()

    var courseToDelete by remember { mutableStateOf<CourseEntity?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("L&D Corporate Admin Dashboard", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_dashboard_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
            )
        },
        containerColor = BackgroundWhite
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            statusMessage?.let { msg ->
                item {
                    Surface(
                        color = PassGreenBg,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PassGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PassGreen,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
            // Metrics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminMetricCard(
                        title = "Total Accounts",
                        value = "$totalUsers Users",
                        icon = Icons.Default.Group,
                        color = ExecutiveBlue,
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = "Trainees",
                        value = "$totalStudents Employees",
                        icon = Icons.Default.Badge,
                        color = PassGreen,
                        modifier = Modifier.weight(1f)
                    )
                    AdminMetricCard(
                        title = "Published",
                        value = "$totalCourses Courses",
                        icon = Icons.Default.MenuBook,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Operations Row
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenUserManagement() }
                        .testTag("open_user_crud_management_card"),
                    colors = CardDefaults.cardColors(containerColor = ExecutiveBlueSoft),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ExecutiveBlue.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ExecutiveBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ManageAccounts,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "User Account CRUD Operations",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Create, search, update roles/departments, and delete corporate user accounts",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = ExecutiveBlue
                        )
                    }
                }
            }

            // Courses Management Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Course Content Management",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )

                    Button(
                        onClick = { onEditCourse(0) },
                        colors = ButtonDefaults.buttonColors(containerColor = ExecutiveBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("admin_create_new_course_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Course", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            items(allCoursesList, key = { it.id }) { c ->
                AdminCourseRowCard(
                    course = c,
                    onEdit = { onEditCourse(c.id) },
                    onDelete = { courseToDelete = c }
                )
            }
        }
    }

    courseToDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { courseToDelete = null },
            title = { Text("Delete Course?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete course '${course.title}'? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminDeleteCourse(course.id) {
                            statusMessage = "Deleted course '${course.title}'"
                            courseToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier.testTag("confirm_delete_course_button")
                ) {
                    Text("Delete Course")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { courseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AdminCourseRowCard(
    course: CourseEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("admin_course_row_${course.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryTag(category = course.category)
                    LevelTag(level = course.level)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Instructor: ${course.instructorName} • ${course.estimatedHours} Hours",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_course_btn_${course.id}")) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Course", tint = ExecutiveBlue)
                }

                IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_course_btn_${course.id}")) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Course", tint = ErrorRed)
                }
            }
        }
    }
}
