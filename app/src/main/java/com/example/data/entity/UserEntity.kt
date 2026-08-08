package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val passwordHash: String,
    val fullName: String,
    val role: String, // "ADMIN", "EMPLOYEE", "ENGINEER_STUDENT"
    val department: String, // e.g., "Hardware Engineering", "HR", "Product"
    val designation: String, // e.g., "Senior Systems Engineer", "Operations Lead"
    val avatarInitials: String = "CU",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
