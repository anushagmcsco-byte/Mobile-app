package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ProfessionalLightColorScheme = lightColorScheme(
    primary = ExecutiveBlue,
    onPrimary = Color.White,
    primaryContainer = ExecutiveBlueSoft,
    onPrimaryContainer = CorporateNavy,
    
    secondary = ElectricCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F9FF),
    onSecondaryContainer = Color(0xFF0C4A6E),
    
    tertiary = PassGreen,
    onTertiary = Color.White,
    tertiaryContainer = PassGreenBg,
    onTertiaryContainer = Color(0xFF064E3B),
    
    background = BackgroundWhite,
    onBackground = TextPrimary,
    
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = TextSecondary,
    
    outline = BorderLight,
    outlineVariant = Color(0xFFE2E8F0),
    
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun EduCorpTheme(
    darkTheme: Boolean = false, // Force crisp white corporate theme as requested
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ProfessionalLightColorScheme,
        typography = Typography,
        content = content
    )
}
