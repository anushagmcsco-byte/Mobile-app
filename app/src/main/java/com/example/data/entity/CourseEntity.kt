package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // "SOFT_SKILLS" or "ELECTRONIC_ENGINEERING"
    val description: String,
    val thumbnailKey: String, // e.g., "leadership", "circuits", "communication", "microcontroller", "time_mgmt", "digital_logic", "power_electronics"
    val level: String, // "Beginner", "Intermediate", "Advanced"
    val estimatedHours: Int,
    val instructorName: String,
    val isPublished: Boolean = true
)
