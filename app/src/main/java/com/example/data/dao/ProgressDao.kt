package com.example.data.dao

import androidx.room.*
import com.example.data.entity.UserProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM user_progress WHERE userId = :userId")
    fun getUserProgress(userId: Int): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress WHERE userId = :userId AND courseId = :courseId")
    fun getUserProgressForCourse(userId: Int, courseId: Int): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress WHERE userId = :userId AND courseId = :courseId")
    suspend fun getUserProgressForCourseDirect(userId: Int, courseId: Int): List<UserProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: UserProgressEntity): Long

    @Query("SELECT COUNT(*) FROM user_progress WHERE userId = :userId AND isModuleCompleted = 1")
    fun getCompletedModulesCount(userId: Int): Flow<Int>

    @Query("SELECT COUNT(DISTINCT courseId) FROM user_progress WHERE userId = :userId")
    fun getEnrolledCoursesCount(userId: Int): Flow<Int>
}
