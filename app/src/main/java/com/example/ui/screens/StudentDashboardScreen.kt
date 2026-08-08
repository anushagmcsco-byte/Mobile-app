package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.entity.UserEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    viewModel: MainViewModel,
    user: UserEntity,
    onBack: () -> Unit
) {
    val completedCount by viewModel.repository.getCompletedModulesCount(user.id)
        .collectAsStateWithLifecycle(initialValue = 0)
    val enrolledCount by viewModel.repository.getEnrolledCoursesCount(user.id)
        .collectAsStateWithLifecycle(initialValue = 0)
    val averageScore by viewModel.repository.getAverageScoreForUser(user.id)
        .collectAsStateWithLifecycle(initialValue = null)
    val quizAttempts by viewModel.repository.getAttemptsForUser(user.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val allCourses by viewModel.allCourses.collectAsStateWithLifecycle()

    var showCertificateDialog by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Progress & Certificates", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("dashboard_back_button")) {
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
            // Student Profile Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = CardDefaults.outlinedCardBorder(enabled = true)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(ExecutiveBlueSoft)
                                .border(1.dp, ExecutiveBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.avatarInitials,
                                style = MaterialTheme.typography.titleMedium,
                                color = ExecutiveBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = user.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${user.designation} • ${user.department}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Stats Cards Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Enrolled",
                        value = "$enrolledCount Courses",
                        icon = Icons.Default.School,
                        color = ExecutiveBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Modules Done",
                        value = "$completedCount Done",
                        icon = Icons.Default.CheckCircle,
                        color = PassGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Avg Score",
                        value = if (averageScore != null) "${averageScore!!.toInt()}%" else "N/A",
                        icon = Icons.Default.EmojiEvents,
                        color = Color(0xFFD97706),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Course Certificates Section
            item {
                Text(
                    text = "Corporate Completion Certificates",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                if (allCourses.isEmpty()) {
                    Text("No courses registered yet.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        allCourses.take(3).forEach { course ->
                            CertificatePreviewCard(
                                userName = user.fullName,
                                courseTitle = course.title,
                                onGenerateCertificate = {
                                    showCertificateDialog = course.title
                                }
                            )
                        }
                    }
                }
            }

            // Recent Quiz Attempt History
            item {
                Text(
                    text = "Assessment History Logs",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (quizAttempts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceSubtle),
                        border = CardDefaults.outlinedCardBorder(enabled = true)
                    ) {
                        Text(
                            text = "No quiz assessments attempted yet. Complete module lessons to start quizzes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(quizAttempts, key = { it.id }) { attempt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = CardDefaults.outlinedCardBorder(enabled = true)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (attempt.passed) PassGreenBg else Color(0xFFFEF2F2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (attempt.passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (attempt.passed) PassGreen else ErrorRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = if (attempt.passed) "Assessment Passed" else "Attempt Needs Improvement",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = dateFormat.format(Date(attempt.attemptedAt)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${attempt.scorePercentage}%",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (attempt.passed) PassGreen else ErrorRed,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${attempt.correctAnswers}/${attempt.totalQuestions} Correct",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Certificate Preview Dialog
    showCertificateDialog?.let { title ->
        AlertDialog(
            onDismissRequest = { showCertificateDialog = null },
            confirmButton = {
                Button(
                    onClick = { showCertificateDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ExecutiveBlue)
                ) {
                    Text("Close Certificate")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = ExecutiveBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Official Certificate", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    border = androidx.compose.foundation.BorderStroke(2.dp, ExecutiveBlue)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "EDUCORP TRAINING PLATFORM",
                            style = MaterialTheme.typography.labelSmall,
                            color = ExecutiveBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Certificate of Achievement",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        Text(
                            text = "This hereby certifies that",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.titleLarge,
                            color = CorporateNavy,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            text = "has successfully completed all modules and interactive assessments for",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = ExecutiveBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = BorderLight)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "ID: CERT-${user.id}9920", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(text = dateFormat.format(Date()), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun MetricCard(
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
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CertificatePreviewCard(
    userName: String,
    courseTitle: String,
    onGenerateCertificate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = courseTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(text = "Issued to $userName", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }

            OutlinedButton(
                onClick = onGenerateCertificate,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ExecutiveBlue),
                modifier = Modifier.testTag("view_certificate_button")
            ) {
                Text("View Cert", style = MaterialTheme.typography.labelMedium, color = ExecutiveBlue)
            }
        }
    }
}
