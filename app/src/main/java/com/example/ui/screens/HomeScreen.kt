package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.entity.UserEntity
import com.example.ui.components.getCourseVisual
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    user: UserEntity,
    onSelectCourse: (Int) -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val allCourses by viewModel.allCourses.collectAsStateWithLifecycle()
    val completedCount by viewModel.repository.getCompletedModulesCount(user.id)
        .collectAsStateWithLifecycle(initialValue = 0)
    val averageScore by viewModel.repository.getAverageScoreForUser(user.id)
        .collectAsStateWithLifecycle(initialValue = null)

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") } // "ALL", "SOFT_SKILLS", "ELECTRONIC_ENGINEERING"

    val filteredCourses = remember(allCourses, searchQuery, selectedCategory) {
        allCourses.filter { course ->
            val matchesCategory = when (selectedCategory) {
                "SOFT_SKILLS" -> course.category == "SOFT_SKILLS"
                "ELECTRONIC_ENGINEERING" -> course.category == "ELECTRONIC_ENGINEERING"
                else -> true
            }
            val matchesSearch = course.title.contains(searchQuery, ignoreCase = true) ||
                    course.description.contains(searchQuery, ignoreCase = true) ||
                    course.instructorName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = SurfaceWhite,
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Hello, ${user.fullName}",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            RoleBadge(role = user.role)
                        }
                        Text(
                            text = "${user.department} • ${user.designation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Row {
                        if (user.role == "ADMIN") {
                            IconButton(
                                onClick = onOpenAdminPanel,
                                modifier = Modifier.testTag("admin_panel_button")
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Panel",
                                    tint = ExecutiveBlue
                                )
                            }
                        }

                        IconButton(
                            onClick = onOpenDashboard,
                            modifier = Modifier.testTag("student_dashboard_button")
                        ) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = "Dashboard",
                                tint = CorporateNavy
                            )
                        }

                        IconButton(
                            onClick = onOpenProfile,
                            modifier = Modifier.testTag("profile_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ExecutiveBlueSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.avatarInitials,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ExecutiveBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
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
            // Stats Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceSubtle),
                    border = CardDefaults.outlinedCardBorder(enabled = true)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(
                            label = "Enrolled",
                            value = "${allCourses.size} Courses",
                            icon = Icons.Default.MenuBook,
                            color = ExecutiveBlue
                        )
                        Divider(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp),
                            color = BorderLight
                        )
                        StatItem(
                            label = "Completed",
                            value = "$completedCount Modules",
                            icon = Icons.Default.CheckCircle,
                            color = PassGreen
                        )
                        Divider(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp),
                            color = BorderLight
                        )
                        StatItem(
                            label = "Avg Quiz",
                            value = if (averageScore != null) "${averageScore!!.toInt()}%" else "N/A",
                            icon = Icons.Default.EmojiEvents,
                            color = Color(0xFFD97706)
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search soft skills or engineering courses...", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("course_search_bar"),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == "ALL",
                            onClick = { selectedCategory = "ALL" },
                            label = { Text("All Training (${allCourses.size})") },
                            modifier = Modifier.testTag("filter_chip_all")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == "SOFT_SKILLS",
                            onClick = { selectedCategory = "SOFT_SKILLS" },
                            label = { Text("Soft Skills") },
                            leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("filter_chip_soft_skills")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == "ELECTRONIC_ENGINEERING",
                            onClick = { selectedCategory = "ELECTRONIC_ENGINEERING" },
                            label = { Text("Electronic Engineering") },
                            leadingIcon = { Icon(Icons.Default.ElectricalServices, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("filter_chip_electronics")
                        )
                    }
                }
            }

            // Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Training Modules",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "${filteredCourses.size} Results",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            // Course List Items
            if (filteredCourses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = TextMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No courses match your filter",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(filteredCourses, key = { it.id }) { course ->
                    CourseCard(
                        course = course,
                        onClick = { onSelectCourse(course.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Text(text = value, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RoleBadge(role: String) {
    val (bgColor, textColor, label) = when (role) {
        "ADMIN" -> Triple(Color(0xFFF3E8FF), Color(0xFF6B21A8), "ADMIN")
        "ENGINEER_STUDENT" -> Triple(Color(0xFFECFDF5), Color(0xFF047857), "ENGINEER")
        else -> Triple(ExecutiveBlueSoft, ExecutiveBlue, "EMPLOYEE")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun CourseCard(
    course: CourseEntity,
    onClick: () -> Unit
) {
    val visual = getCourseVisual(course.thumbnailKey)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("course_card_${course.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = CardDefaults.outlinedCardBorder(enabled = true),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Badge
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(visual.containerColor)
                    .border(1.dp, visual.primaryColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = null,
                    tint = visual.primaryColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                    text = course.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = course.instructorName, style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "${course.estimatedHours}h", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Open",
                tint = TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun CategoryTag(category: String) {
    val isSoftSkills = category == "SOFT_SKILLS"
    val label = if (isSoftSkills) "Soft Skills" else "Electronic Eng"
    val color = if (isSoftSkills) Color(0xFF7C3AED) else ElectricCyan

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun LevelTag(level: String) {
    Surface(
        color = Color(0xFFF1F5F9),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = level,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
