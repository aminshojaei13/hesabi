package com.braveboy.hesabi.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.ui.preview.HesabiPreview
import com.braveboy.hesabi.ui.theme.HesabiRadius
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.LocalHesabiLumina
import com.braveboy.hesabi.ui.theme.LocalHesabiSemantic

/**
 * A frosted glass surface with luminous border.
 * Primary building block for the Lumina design system.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glowColor: Color = LocalHesabiLumina.current.glowPrimary,
    contentPadding: PaddingValues = PaddingValues(HesabiSpacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val lumina = LocalHesabiLumina.current
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier.then(clickable),
        shape = RoundedCornerShape(HesabiRadius.hero),
        color = lumina.glassBackground,
        border = BorderStroke(1.dp, lumina.glassBorder),
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/**
 * The main balance display card with mesh gradient background.
 * Designed to sit at the top of the home screen as a hero element.
 */
@Composable
fun HeroBalanceCard(
    totalBalance: String,
    monthExpense: String,
    monthIncome: String,
    modifier: Modifier = Modifier,
) {
    val lumina = LocalHesabiLumina.current
    val semantic = LocalHesabiSemantic.current

    val meshGradient = Brush.linearGradient(
        colors = listOf(
            lumina.cardMeshGradientStart.copy(alpha = 0.15f),
            lumina.cardMeshGradientMid.copy(alpha = 0.15f),
            lumina.cardMeshGradientEnd.copy(alpha = 0.15f),
        ),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HesabiRadius.hero))
            .background(meshGradient)
            .then(
                Modifier.background(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(HesabiRadius.hero),
                ),
            ),
    ) {
        // Glass border overlay
        Surface(
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(HesabiRadius.hero),
            color = Color.Transparent,
            border = BorderStroke(1.dp, lumina.glassBorder),
            content = {},
        )

        Column(
            modifier = Modifier.padding(HesabiSpacing.xl),
        ) {
            Text(
                text = "موجودی کل",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(HesabiSpacing.xs))

            Text(
                text = totalBalance,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(HesabiSpacing.lg))

            Row(
                horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
            ) {
                // Income pill
                Surface(
                    shape = RoundedCornerShape(HesabiRadius.chip),
                    color = semantic.income.copy(alpha = 0.15f),
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = HesabiSpacing.md,
                            vertical = HesabiSpacing.sm,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
                    ) {
                        Icon(
                            Icons.Rounded.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = semantic.income,
                        )
                        Text(
                            text = monthIncome,
                            style = MaterialTheme.typography.labelMedium,
                            color = semantic.income,
                        )
                    }
                }

                // Expense pill
                Surface(
                    shape = RoundedCornerShape(HesabiRadius.chip),
                    color = semantic.expense.copy(alpha = 0.15f),
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = HesabiSpacing.md,
                            vertical = HesabiSpacing.sm,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
                    ) {
                        Icon(
                            Icons.Rounded.TrendingDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = semantic.expense,
                        )
                        Text(
                            text = monthExpense,
                            style = MaterialTheme.typography.labelMedium,
                            color = semantic.expense,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A circular button with ambient glow effect.
 * Used for prominent quick-actions like voice entry, expense, and transfer.
 */
@Composable
fun GlowingActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = MaterialTheme.colorScheme.primary,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = glowColor.copy(alpha = 0.15f),
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = glowColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A horizontal row of quick action buttons (Voice, Expense, Transfer).
 * Displayed beneath the hero balance card on the home screen.
 */
@Composable
fun QuickActionRow(
    onVoiceEntry: () -> Unit,
    onQuickExpense: () -> Unit,
    onTransfer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        GlowingActionButton(
            icon = Icons.Rounded.Mic,
            label = "ثبت صوتی",
            onClick = onVoiceEntry,
            glowColor = MaterialTheme.colorScheme.secondary,
        )
        GlowingActionButton(
            icon = Icons.Rounded.Add,
            label = "هزینه سریع",
            onClick = onQuickExpense,
            glowColor = MaterialTheme.colorScheme.primary,
        )
        GlowingActionButton(
            icon = Icons.Rounded.SwapHoriz,
            label = "انتقال",
            onClick = onTransfer,
            glowColor = MaterialTheme.colorScheme.tertiary,
        )
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(name = "GlassCard - Light")
@Composable
private fun GlassCardLightPreview() {
    HesabiPreview {
        GlassCard {
            Text("محتوای کارت شیشه‌ای", style = MaterialTheme.typography.bodyLarge)
            Text("نمونه‌ای از کارت Lumina", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(name = "GlassCard - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GlassCardDarkPreview() {
    HesabiPreview(dark = true) {
        GlassCard {
            Text("محتوای کارت شیشه‌ای", style = MaterialTheme.typography.bodyLarge)
            Text("نمونه‌ای از کارت Lumina", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(name = "HeroBalanceCard - Light")
@Composable
private fun HeroBalanceCardLightPreview() {
    HesabiPreview {
        HeroBalanceCard(
            totalBalance = "۱۲,۴۵۰,۰۰۰ تومان",
            monthExpense = "۳,۲۰۰,۰۰۰",
            monthIncome = "۸,۵۰۰,۰۰۰",
        )
    }
}

@Preview(name = "HeroBalanceCard - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HeroBalanceCardDarkPreview() {
    HesabiPreview(dark = true) {
        HeroBalanceCard(
            totalBalance = "۱۲,۴۵۰,۰۰۰ تومان",
            monthExpense = "۳,۲۰۰,۰۰۰",
            monthIncome = "۸,۵۰۰,۰۰۰",
        )
    }
}

@Preview(name = "QuickActionRow - Light")
@Composable
private fun QuickActionRowLightPreview() {
    HesabiPreview {
        QuickActionRow(
            onVoiceEntry = {},
            onQuickExpense = {},
            onTransfer = {},
        )
    }
}

@Preview(name = "QuickActionRow - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuickActionRowDarkPreview() {
    HesabiPreview(dark = true) {
        QuickActionRow(
            onVoiceEntry = {},
            onQuickExpense = {},
            onTransfer = {},
        )
    }
}

@Preview(name = "Lumina Components - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LuminaCombinedDarkPreview() {
    HesabiPreview(dark = true) {
        HeroBalanceCard(
            totalBalance = "۱۲,۴۵۰,۰۰۰ تومان",
            monthExpense = "۳,۲۰۰,۰۰۰",
            monthIncome = "۸,۵۰۰,۰۰۰",
        )
        QuickActionRow(
            onVoiceEntry = {},
            onQuickExpense = {},
            onTransfer = {},
        )
        GlassCard {
            SectionHeader(title = "تراکنش‌های اخیر", actionLabel = "همه")
            Text(
                "۱۲ تراکنش این ماه",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
