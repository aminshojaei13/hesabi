package com.braveboy.hesabi.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = Color(0xFFF8FAFC),
    inversePrimary = Color(0xFF4FD8E4),
    scrim = Color(0x99000000),
)

val DarkScheme = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFF34D399),
    secondary = Color(0xFF8B5CF6),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFC4B5FD),
    tertiary = Color(0xFF3B82F6),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF1E3A5F),
    onTertiaryContainer = Color(0xFF93C5FD),
    background = Color(0xFF0B0E17),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF1E3A5F),
    outlineVariant = Color(0xFF0F2137),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFCA5A5),
    inverseSurface = Color(0xFFF1F5F9),
    inverseOnSurface = Color(0xFF0B0E17),
    inversePrimary = Color(0xFF059669),
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
    expense = Color(0xFFFB7185),
    income = Color(0xFF34D399),
    transfer = Color(0xFF94A3B8),
    warning = Color(0xFFFBBF24),
    success = Color(0xFF34D399),
    muted = Color(0xFF64748B),
)

/**
 * Extended palette for Lumina glassmorphic components.
 * Configured separately for Dark Mode (space neon glass) and Light Mode (luxury pastel surface).
 */
@Immutable
data class HesabiLuminaColors(
    val glassBorder: Color = Color(0x33FFFFFF),
    val glassBackground: Color = Color(0x2B151A28),
    val cardElevation: Dp = 0.dp,
    val textPrimary: Color = Color(0xFFFFFFFF),
    val textSecondary: Color = Color(0x99FFFFFF),
    val textMuted: Color = Color(0x66FFFFFF),
    val pillBg: Color = Color(0x2B1F273B),
    val pillBorder: Color = Color(0x33FFFFFF),
    val expensePillBg: Color = Color(0x2E8B5CF6),
    val expensePillBorder: Color = Color(0x448B5CF6),
    val expensePillText: Color = Color(0xEEFFFFFF),
    val expensePillDot: Color = Color(0xFF8B5CF6),
    val incomePillBg: Color = Color(0x2E10B981),
    val incomePillBorder: Color = Color(0x4410B981),
    val incomePillText: Color = Color(0xEEFFFFFF),
    val incomePillDot: Color = Color(0xFF10B981),
    val heroMeshColors: List<Color> = listOf(
        Color(0x3D10B981),
        Color(0x268B5CF6),
        Color(0x1F06B6D4),
        Color(0x33151A28),
    ),
    val heroMeshOverlay: Color = Color(0x260F131F),
    val voicePillBg: Color = Color(0x3D10B981),
    val voicePillBorder: Color = Color(0x6610B981),
    val voicePillIcon: Color = Color(0xFF10B981),
    val gaugeTrack: Color = Color(0x1F2B354C),
    val gaugeTickActive: Color = Color(0xB3FFFFFF),
    val gaugeTickInactive: Color = Color(0x26FFFFFF),
    val gaugeCenterPillBg: Color = Color(0x3B222D42),
    val navDockBg: Color = Color(0x3B151A28),
    val navDockBorder: Color = Color(0x33FFFFFF),
    val glowPrimary: Color = Color(0x3310B981),
    val glowSecondary: Color = Color(0x338B5CF6),
    val voiceWaveform: Color = Color(0xFF10B981),
    val voiceWaveformSecondary: Color = Color(0xFF06B6D4),
    val safeBudgetGreen: Color = Color(0xFF10B981),
    val cautionBudgetYellow: Color = Color(0xFFFBBF24),
    val dangerBudgetRed: Color = Color(0xFFEF4444),
    val cardMeshGradientStart: Color = Color(0xFF10B981),
    val cardMeshGradientMid: Color = Color(0xFF8B5CF6),
    val cardMeshGradientEnd: Color = Color(0xFF3B82F6),
)

val DarkLuminaColors = HesabiLuminaColors()

val LightLuminaColors = HesabiLuminaColors(
    glassBorder = Color(0xFFE2E8F0),
    glassBackground = Color(0xFFFFFFFF),
    cardElevation = 2.dp,
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF94A3B8),
    pillBg = Color(0xFFFFFFFF),
    pillBorder = Color(0xFFE2E8F0),
    expensePillBg = Color(0xFFF3E8FF),
    expensePillBorder = Color(0xFFDDD6FE),
    expensePillText = Color(0xFF5B21B6),
    expensePillDot = Color(0xFF7C3AED),
    incomePillBg = Color(0xFFD1FAE5),
    incomePillBorder = Color(0xFFA7F3D0),
    incomePillText = Color(0xFF065F46),
    incomePillDot = Color(0xFF059669),
    heroMeshColors = listOf(
        Color(0xFFE6F7F5),
        Color(0xFFF1F0FF),
        Color(0xFFEDF5FF),
        Color(0xFFF8FAFC),
    ),
    heroMeshOverlay = Color(0x40FFFFFF),
    voicePillBg = Color(0xFFD1FAE5),
    voicePillBorder = Color(0xFFA7F3D0),
    voicePillIcon = Color(0xFF059669),
    gaugeTrack = Color(0xFFE2E8F0),
    gaugeTickActive = Color(0xFF334155),
    gaugeTickInactive = Color(0xFFCBD5E1),
    gaugeCenterPillBg = Color(0xFFF1F5F9),
    navDockBg = Color(0xFFFFFFFF),
    navDockBorder = Color(0xFFE2E8F0),
    glowPrimary = Color(0x1A10B981),
    glowSecondary = Color(0x1A8B5CF6),
    voiceWaveform = Color(0xFF059669),
    voiceWaveformSecondary = Color(0xFF0284C7),
)
