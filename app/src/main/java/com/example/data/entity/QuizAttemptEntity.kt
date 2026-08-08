package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val courseId: Int,
    val moduleId: Int,
    val scorePercentage: Int,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val passed: Boolean,
    val attemptedAt: Long = System.currentTimeMillis()
)
