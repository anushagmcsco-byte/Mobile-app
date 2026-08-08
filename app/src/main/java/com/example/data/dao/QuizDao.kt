package com.example.data.dao

import androidx.room.*
import com.example.data.entity.QuizAttemptEntity
import com.example.data.entity.QuizQuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {
    @Query("SELECT * FROM quiz_questions WHERE moduleId = :moduleId ORDER BY id ASC")
    fun getQuestionsForModule(moduleId: Int): Flow<List<QuizQuestionEntity>>

    @Query("SELECT * FROM quiz_questions WHERE moduleId = :moduleId ORDER BY id ASC")
    suspend fun getQuestionsForModuleDirect(moduleId: Int): List<QuizQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuizQuestionEntity): Long

    @Update
    suspend fun updateQuestion(question: QuizQuestionEntity)

    @Query("DELETE FROM quiz_questions WHERE id = :id")
    suspend fun deleteQuestionById(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: QuizAttemptEntity): Long

    @Query("SELECT * FROM quiz_attempts WHERE userId = :userId ORDER BY attemptedAt DESC")
    fun getAttemptsForUser(userId: Int): Flow<List<QuizAttemptEntity>>

    @Query("SELECT * FROM quiz_attempts WHERE userId = :userId AND moduleId = :moduleId ORDER BY attemptedAt DESC")
    fun getAttemptsForUserAndModule(userId: Int, moduleId: Int): Flow<List<QuizAttemptEntity>>

    @Query("SELECT AVG(scorePercentage) FROM quiz_attempts WHERE userId = :userId")
    fun getAverageScoreForUser(userId: Int): Flow<Double?>
}
