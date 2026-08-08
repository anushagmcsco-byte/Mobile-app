package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ExecutiveBlue
import com.example.ui.theme.PassGreen

data class CourseVisual(
    val icon: ImageVector,
    val primaryColor: Color,
    val containerColor: Color
)

fun getCourseVisual(key: String): CourseVisual {
    return when (key.lowercase()) {
        "leadership" -> CourseVisual(
            icon = Icons.Default.Groups,
            primaryColor = ExecutiveBlue,
            containerColor = Color(0xFFEFF6FF)
        )
        "communication" -> CourseVisual(
            icon = Icons.Default.RecordVoiceOver,
            primaryColor = Color(0xFF7C3AED),
            containerColor = Color(0xFFF5F3FF)
        )
        "time_mgmt" -> CourseVisual(
            icon = Icons.Default.Schedule,
            primaryColor = Color(0xFFD97706),
            containerColor = Color(0xFFFFFBEB)
        )
        "circuits" -> CourseVisual(
            icon = Icons.Default.ElectricalServices,
            primaryColor = ElectricCyan,
            containerColor = Color(0xFFF0F9FF)
        )
        "microcontroller" -> CourseVisual(
            icon = Icons.Default.Memory,
            primaryColor = Color(0xFF0284C7),
            containerColor = Color(0xFFE0F2FE)
        )
        "digital_logic" -> CourseVisual(
            icon = Icons.Default.DeveloperBoard,
            primaryColor = PassGreen,
            containerColor = Color(0xFFECFDF5)
        )
        "power_electronics" -> CourseVisual(
            icon = Icons.Default.Bolt,
            primaryColor = Color(0xFFEA580C),
            containerColor = Color(0xFFFFEDD5)
        )
        else -> CourseVisual(
            icon = Icons.Default.School,
            primaryColor = ExecutiveBlue,
            containerColor = Color(0xFFEFF6FF)
        )
    }
}
