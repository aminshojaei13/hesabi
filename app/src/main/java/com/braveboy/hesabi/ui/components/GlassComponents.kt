package com.braveboy.hesabi.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.ui.preview.HesabiPreview
import com.braveboy.hesabi.ui.theme.LocalHesabiLumina

/**
 * A frosted glass surface with luminous border.
 * Adapts dynamically to Light Mode (pastel depth surface) and Dark Mode (space neon glass).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glowColor: Color = LocalHesabiLumina.current.glowPrimary,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val lumina = LocalHesabiLumina.current
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier.then(clickable),
        shape = RoundedCornerShape(26.dp),
        color = lumina.glassBackground,
        border = BorderStroke(1.dp, lumina.glassBorder),
        shadowElevation = lumina.cardElevation,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/**
 * The main balance display hero card with glowing mesh gradient background.
 * Matches the Lumina design system in both light and dark themes.
 */
@Composable
fun HeroBalanceCard(
    totalBalance: String,
    monthExpense: String,
    monthIncome: String,
    modifier: Modifier = Modifier,
    title: String = "تحلیل حساب‌های حسابی",
) {
    val lumina = LocalHesabiLumina.current

    val meshGradient = Brush.linearGradient(
        colors = lumina.heroMeshColors,
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(meshGradient)
            .background(lumina.heroMeshOverlay),
    ) {
        // Glass border overlay
        Surface(
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, lumina.glassBorder),
            content = {},
        )

        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            // Header row with subtle dots on left and title on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = lumina.textSecondary,
                    fontWeight = FontWeight.Medium,
                )

                Text(
                    text = "••••",
                    style = MaterialTheme.typography.titleMedium,
                    color = lumina.textMuted,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(14.dp))

            // Total balance digits display
            Text(
                text = totalBalance,
                style = MaterialTheme.typography.headlineLarge,
                color = lumina.textPrimary,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(Modifier.height(20.dp))

            // Income and Expense glass pills row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Monthly Expense Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = lumina.expensePillBg,
                    border = BorderStroke(1.dp, lumina.expensePillBorder),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(lumina.expensePillDot),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (monthExpense.isNotBlank()) "خرج ماهانه" else "خرج ماهانه",
                            style = MaterialTheme.typography.labelMedium,
                            color = lumina.expensePillText,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                // Income Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = lumina.incomePillBg,
                    border = BorderStroke(1.dp, lumina.incomePillBorder),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(lumina.incomePillDot),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (monthIncome.isNotBlank()) "درآمد" else "درآمد",
                            style = MaterialTheme.typography.labelMedium,
                            color = lumina.incomePillText,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mini animated waveform bars indicator for Voice pill.
 */
@Composable
fun WaveformMiniLines(
    modifier: Modifier = Modifier,
    color: Color = LocalHesabiLumina.current.voicePillIcon,
) {
    Canvas(modifier = modifier.size(width = 22.dp, height = 16.dp)) {
        val barWidth = 2.5.dp.toPx()
        val space = 2.dp.toPx()
        val heights = listOf(0.4f, 0.85f, 1.0f, 0.65f, 0.35f)
        var startX = 0f
        heights.forEach { h ->
            val barH = size.height * h
            val topY = (size.height - barH) / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(startX, topY),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
            startX += barWidth + space
        }
    }
}

/**
 * Quick access action row with 3 pill buttons:
 * 1. Glowing Mic Voice capsule
 * 2. Quick Transfer capsule
 * 3. Scan / Quick Entry capsule
 */
@Composable
fun QuickActionRow(
    onVoiceEntry: () -> Unit,
    onQuickExpense: () -> Unit,
    onTransfer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lumina = LocalHesabiLumina.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "دسترسی سریع",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = lumina.textPrimary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Voice Pill (Green glow background + waveform)
            Surface(
                onClick = onVoiceEntry,
                shape = RoundedCornerShape(22.dp),
                color = lumina.voicePillBg,
                border = BorderStroke(1.dp, lumina.voicePillBorder),
                modifier = Modifier.weight(1.2f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Rounded.Mic,
                        contentDescription = "ثبت صوتی",
                        tint = lumina.voicePillIcon,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    WaveformMiniLines(color = lumina.voicePillIcon)
                }
            }

            // Quick Transfer Pill
            Surface(
                onClick = onTransfer,
                shape = RoundedCornerShape(22.dp),
                color = lumina.pillBg,
                border = BorderStroke(1.dp, lumina.pillBorder),
                shadowElevation = lumina.cardElevation,
                modifier = Modifier.weight(1.3f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "انتقال سریع",
                        style = MaterialTheme.typography.labelMedium,
                        color = lumina.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Rounded.SwapHoriz,
                        contentDescription = null,
                        tint = lumina.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Scan / Quick Entry Pill
            Surface(
                onClick = onQuickExpense,
                shape = RoundedCornerShape(22.dp),
                color = lumina.pillBg,
                border = BorderStroke(1.dp, lumina.pillBorder),
                shadowElevation = lumina.cardElevation,
                modifier = Modifier.weight(1.3f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "ثبت سریع",
                        style = MaterialTheme.typography.labelMedium,
                        color = lumina.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        tint = lumina.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(name = "HeroBalanceCard - Lumina Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HeroBalanceCardDarkPreview() {
    HesabiPreview(dark = true) {
        HeroBalanceCard(
            totalBalance = "۰۱۴۲,۲۷۵,۰۰۰",
            monthExpense = "۳,۲۰۰,۰۰۰",
            monthIncome = "۸,۵۰۰,۰۰۰",
        )
    }
}

@Preview(name = "HeroBalanceCard - Lumina Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun HeroBalanceCardLightPreview() {
    HesabiPreview(dark = false) {
        HeroBalanceCard(
            totalBalance = "۰۱۴۲,۲۷۵,۰۰۰",
            monthExpense = "۳,۲۰۰,۰۰۰",
            monthIncome = "۸,۵۰۰,۰۰۰",
        )
    }
}
