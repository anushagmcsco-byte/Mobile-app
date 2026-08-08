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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.QuizAttemptEntity
import com.example.data.entity.QuizQuestionEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: MainViewModel,
    moduleId: Int,
    onBack: () -> Unit,
    onQuizCompleted: () -> Unit
) {
    val questionsState by viewModel.repository.getQuestionsForModule(moduleId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val moduleState by viewModel.repository.getModuleById(moduleId)
        .collectAsStateWithLifecycle(initialValue = null)

    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var correctAnswersCount by remember { mutableIntStateOf(0) }
    var quizFinishedResult by remember { mutableStateOf<QuizAttemptEntity?>(null) }

    val questions = questionsState
    val module = moduleState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(module?.title?.let { "Assessment: $it" } ?: "Module Assessment", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("quiz_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
            )
        },
        containerColor = BackgroundWhite
    ) { innerPadding ->
        if (questions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AssignmentLate,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No assessment questions configured for this module.",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text("Return to Module")
                    }
                }
            }
        } else if (quizFinishedResult != null) {
            // Quiz Finish Result Dialog / Screen
            val result = quizFinishedResult!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp)
                        .padding(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = CardDefaults.outlinedCardBorder(enabled = true)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (result.passed) PassGreenBg else Color(0xFFFEF2F2))
                                .border(1.dp, if (result.passed) PassGreen else ErrorRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (result.passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (result.passed) PassGreen else ErrorRed,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (result.passed) "Assessment Passed!" else "Assessment Failed",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (result.passed) PassGreen else ErrorRed,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (result.passed)
                                "Great job! You have demonstrated core proficiency in this module."
                            else
                                "You scored below the 70% passing threshold. Review the lesson and try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        // Score Breakdown Surface
                        Surface(
                            color = SurfaceSubtle,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("SCORE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("${result.scorePercentage}%", style = MaterialTheme.typography.headlineLarge, color = ExecutiveBlue, fontWeight = FontWeight.Bold)
                                }
                                Divider(modifier = Modifier.height(32.dp).width(1.dp), color = BorderLight)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("CORRECT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("${result.correctAnswers} / ${result.totalQuestions}", style = MaterialTheme.typography.headlineLarge, color = CorporateNavy, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    // Reset Quiz
                                    currentQuestionIndex = 0
                                    selectedOptionIndex = null
                                    isSubmitted = false
                                    correctAnswersCount = 0
                                    quizFinishedResult = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("retake_quiz_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Retake Quiz")
                            }

                            Button(
                                onClick = onQuizCompleted,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("finish_quiz_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ExecutiveBlue)
                            ) {
                                Text("Continue")
                            }
                        }
                    }
                }
            }
        } else {
            val currentQuestion = questions[currentQuestionIndex]
            val totalQuestions = questions.size

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Question Counter & Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question ${currentQuestionIndex + 1} of $totalQuestions",
                        style = MaterialTheme.typography.titleMedium,
                        color = ExecutiveBlue,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        color = ExecutiveBlueSoft,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Module Quiz",
                            style = MaterialTheme.typography.labelSmall,
                            color = ExecutiveBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { (currentQuestionIndex + 1).toFloat() / totalQuestions },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ExecutiveBlue,
                    trackColor = ExecutiveBlueSoft
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Question Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = CardDefaults.outlinedCardBorder(enabled = true)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = currentQuestion.questionText,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Options A, B, C, D
                        val options = listOf(
                            currentQuestion.optionA,
                            currentQuestion.optionB,
                            currentQuestion.optionC,
                            currentQuestion.optionD
                        )

                        options.forEachIndexed { index, optionText ->
                            val isSelected = selectedOptionIndex == index
                            val isCorrect = index == currentQuestion.correctOptionIndex

                            val (cardBg, cardBorder, textCol) = when {
                                isSubmitted && isCorrect -> Triple(PassGreenBg, PassGreen, PassGreen)
                                isSubmitted && isSelected && !isCorrect -> Triple(Color(0xFFFEF2F2), ErrorRed, ErrorRed)
                                isSelected -> Triple(ExecutiveBlueSoft, ExecutiveBlue, ExecutiveBlue)
                                else -> Triple(SurfaceWhite, BorderLight, TextPrimary)
                            }

                            Surface(
                                onClick = {
                                    if (!isSubmitted) {
                                        selectedOptionIndex = index
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = cardBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .testTag("quiz_option_$index")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val optionLabel = when (index) {
                                        0 -> "A"
                                        1 -> "B"
                                        2 -> "C"
                                        else -> "D"
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected || (isSubmitted && isCorrect)) cardBorder else SurfaceSubtle),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = optionLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected || (isSubmitted && isCorrect)) Color.White else TextSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = optionText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = textCol,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isSubmitted) {
                                        if (isCorrect) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = PassGreen, modifier = Modifier.size(20.dp))
                                        } else if (isSelected) {
                                            Icon(Icons.Default.Cancel, contentDescription = "Incorrect", tint = ErrorRed, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Instant Feedback Explanation Box
                        if (isSubmitted) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                color = SurfaceSubtle,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = ExecutiveBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "EXPLANATION",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ExecutiveBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentQuestion.explanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Action Button (Submit Answer OR Next Question)
                if (!isSubmitted) {
                    Button(
                        onClick = {
                            if (selectedOptionIndex != null) {
                                isSubmitted = true
                                if (selectedOptionIndex == currentQuestion.correctOptionIndex) {
                                    correctAnswersCount++
                                }
                            }
                        },
                        enabled = selectedOptionIndex != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("submit_answer_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ExecutiveBlue)
                    ) {
                        Text("Submit Answer", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Button(
                        onClick = {
                            if (currentQuestionIndex < totalQuestions - 1) {
                                currentQuestionIndex++
                                selectedOptionIndex = null
                                isSubmitted = false
                            } else {
                                // Finalize Quiz and record attempt
                                if (module != null) {
                                    viewModel.submitQuiz(
                                        courseId = module.courseId,
                                        moduleId = module.id,
                                        totalQuestions = totalQuestions,
                                        correctAnswers = correctAnswersCount
                                    ) { attempt ->
                                        quizFinishedResult = attempt
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("next_question_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ExecutiveBlue)
                    ) {
                        Text(
                            text = if (currentQuestionIndex < totalQuestions - 1) "Next Question" else "Complete Assessment",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
