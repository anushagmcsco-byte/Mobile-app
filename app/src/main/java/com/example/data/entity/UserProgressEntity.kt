package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val courseId: Int,
    val moduleId: Int,
    val isModuleCompleted: Boolean,
    val lastUpdated: Long = System.currentTimeMillis()
)
