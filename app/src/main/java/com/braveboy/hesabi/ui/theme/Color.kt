package com.braveboy.hesabi.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

internal val Teal = Color(0xFF12AFC0)
internal val TealDeep = Color(0xFF0C7F8B)

val LightScheme = lightColorScheme(
    primary = Color(0xFF0C7F8B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDF3F6),
    onPrimaryContainer = Color(0xFF00201F),
    secondary = Color(0xFF4A6365),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8EA),
    onSecondaryContainer = Color(0xFF051F21),
    tertiary = Color(0xFF4F5E7E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD7E2FF),
    onTertiaryContainer = Color(0xFF0A1B36),
    background = Color(0xFFF6F8F9),
    onBackground = Color(0xFF111A1C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111A1C),
    surfaceVariant = Color(0xFFE7EEF0),
    onSurfaceVariant = Color(0xFF3F494B),
    outline = Color(0xFFD5DEE1),
    outlineVariant = Color(0xFFE3EAEC),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF2A3234),
    inverseOnSurface = Color(0xFFECF1F2),
    inversePrimary = Color(0xFF4FD8E4),
    scrim = Color(0x99000000),
)

val DarkScheme = darkColorScheme(
    primary = Color(0xFF4FD8E4),
    onPrimary = Color(0xFF06282C),
    primaryContainer = Color(0xFF12383D),
    onPrimaryContainer = Color(0xFFB6F0F5),
    secondary = Color(0xFFB1CCCE),
    onSecondary = Color(0xFF1B3436),
    secondaryContainer = Color(0xFF334B4D),
    onSecondaryContainer = Color(0xFFCCE8EA),
    tertiary = Color(0xFFB7C6EA),
    onTertiary = Color(0xFF21304E),
    tertiaryContainer = Color(0xFF374766),
    onTertiaryContainer = Color(0xFFD7E2FF),
    background = Color(0xFF0D1315),
    onBackground = Color(0xFFE7EEF0),
    surface = Color(0xFF161F21),
    onSurface = Color(0xFFE7EEF0),
    surfaceVariant = Color(0xFF223033),
    onSurfaceVariant = Color(0xFFBEC8CB),
    outline = Color(0xFF2A3A3E),
    outlineVariant = Color(0xFF1E2B2E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE7EEF0),
    inverseOnSurface = Color(0xFF111A1C),
    inversePrimary = Color(0xFF0C7F8B),
    scrim = Color(0xCC000000),
)

/**
 * Semantic colors are intentionally NOT dynamic: income must always read as green
 * and expense as red, even when the user picks a custom system palette.
 */
@Immutable
data class HesabiSemanticColors(
    val expense: Color,
    val income: Color,
    val transfer: Color,
    val warning: Color,
    val success: Color,
    val muted: Color,
)

val LightSemantic = HesabiSemanticColors(
    expense = Color(0xFFD6455D),
    income = Color(0xFF1F9D6B),
    transfer = Color(0xFF6E7B80),
    warning = Color(0xFFC9820A),
    success = Color(0xFF1F9D6B),
    muted = Color(0xFF7A878B),
)

val DarkSemantic = HesabiSemanticColors(
    expense = Color(0xFFFF8A9B),
    income = Color(0xFF6FE0B0),
    transfer = Color(0xFFA9B8BD),
    warning = Color(0xFFF0C36A),
    success = Color(0xFF6FE0B0),
    muted = Color(0xFF93A1A5),
)
