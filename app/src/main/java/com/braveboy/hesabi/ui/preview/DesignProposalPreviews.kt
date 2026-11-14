package com.braveboy.hesabi.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme

// ==========================================
// 1. TOP APP BAR PROPOSALS
// ==========================================

@Composable
fun TopBarOptionA(modifier: Modifier = Modifier) {
    // Option A: Floating Greeting Header (Modern Dribbble / Monzo Style)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HesabiSpacing.md, vertical = HesabiSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "👋",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Column {
                Text(
                    text = "سلام، خوش‌آمدید",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "حسابی",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.height(38.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "۷ اسفند",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    BadgedBox(
                        badge = { Badge { Text("۳") } }
                    ) {
                        Icon(
                            Icons.Rounded.Notifications,
                            contentDescription = "پیامک‌ها",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = "تنظیمات",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun TopBarOptionB(modifier: Modifier = Modifier) {
    // Option B: Integrated Glassmorphic Header with Account Selector
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HesabiSpacing.md, vertical = HesabiSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.height(42.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "همه حساب‌ها",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Icon(
                    Icons.Rounded.UnfoldMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
        ) {
            IconButton(onClick = {}) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "جستجو",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = "تنظیمات",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun TopBarOptionC(modifier: Modifier = Modifier) {
    // Option C: Compact Tonal Header with Month Chip
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HesabiSpacing.md, vertical = HesabiSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
        ) {
            Text(
                text = "حسابی",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = "اسفند ۱۴۰۳",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        IconButton(onClick = {}) {
            Icon(Icons.Rounded.Settings, contentDescription = "تنظیمات")
        }
    }
}

// ==========================================
// 2. BOTTOM NAVIGATION PROPOSALS
// ==========================================

@Composable
fun BottomNavOptionA(modifier: Modifier = Modifier) {
    // Option A: Floating Pill Bar + Center Floating CTA (+)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(12.dp, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Home (Selected)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Rounded.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "خانه",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Transactions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(6.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Center Floating (+) CTA
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "ثبت",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            // Reports
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(6.dp)
            ) {
                Icon(
                    Icons.Rounded.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }

            // More
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(6.dp)
            ) {
                BadgedBox(
                    badge = { Badge { Text("۲") } }
                ) {
                    Icon(
                        Icons.Rounded.MoreHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavOptionB(modifier: Modifier = Modifier) {
    // Option B: Minimalist Glassmorphic Dock with Active Glowing Indicator
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .shadow(8.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Tab 1: Home (Selected)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            // Tab 2: Transactions
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Tab 3: Reports
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Tab 4: More
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BadgedBox(
                    badge = { Badge { Text("۲") } }
                ) {
                    Icon(
                        Icons.Rounded.MoreHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavOptionC(modifier: Modifier = Modifier) {
    // Option C: Segmented Tonal Bar
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tabs = listOf(
                "خانه" to Icons.Rounded.Home,
                "تراکنش" to Icons.Rounded.ReceiptLong,
                "گزارش" to Icons.Rounded.BarChart,
                "بیشتر" to Icons.Rounded.MoreHoriz,
            )
            tabs.forEachIndexed { index, pair ->
                val selected = index == 0
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        Icon(
                            pair.second,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = pair.first,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// PREVIEW COMBINATIONS
// ==========================================

@Preview(name = "طرح A - هدر و نوار شناور", showBackground = true)
@Composable
fun PreviewOptionA() {
    HesabiTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "پیش‌نمایش طرح A (کپسولی شناور با دکمه +)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    TopBarOptionA()
                    Spacer(Modifier.height(40.dp))
                    BottomNavOptionA()
                }
            }
        }
    }
}

@Preview(name = "طرح B - هدر شیشه‌ای و داک مینیمال", showBackground = true)
@Composable
fun PreviewOptionB() {
    HesabiTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "پیش‌نمایش طرح B (شیشه‌ای و مینیمال)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    TopBarOptionB()
                    Spacer(Modifier.height(40.dp))
                    BottomNavOptionB()
                }
            }
        }
    }
}

@Preview(name = "طرح C - هدر تال و بار قطعه‌ای", showBackground = true)
@Composable
fun PreviewOptionC() {
    HesabiTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "پیش‌نمایش طرح C (قطعه‌ای و فشرده)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    TopBarOptionC()
                    Spacer(Modifier.height(40.dp))
                    BottomNavOptionC()
                }
            }
        }
    }
}

@Preview(name = "طرح شیشه‌ای متحرک - Lumina Target UI", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewLuminaFuturisticUi() {
    HesabiTheme(themeMode = com.braveboy.hesabi.domain.model.ThemeMode.DARK) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B0E17))
                    .padding(16.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    // 1. Top Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0x3D10B981),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)),
                                modifier = Modifier.size(44.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("👤", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Column {
                                Text(
                                    text = "سلام، ماهگین",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                )
                                Text(
                                    text = "درختی ترینی کن !",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }

                        // Glass Bell with Pink Badge
                        Surface(
                            shape = CircleShape,
                            color = Color(0x2B1F273B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                BadgedBox(
                                    badge = {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEC4899)),
                                        )
                                    }
                                ) {
                                    Icon(
                                        Icons.Rounded.Notifications,
                                        contentDescription = "نوتیفیکیشن",
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }

                    // 2. Main Hero Balance Card
                    com.braveboy.hesabi.ui.components.HeroBalanceCard(
                        totalBalance = "۰۱۴۲,۲۷۵,۰۰۰",
                        monthExpense = "۳,۲۰۰,۰۰۰",
                        monthIncome = "۸,۵۰۰,۰۰۰",
                    )

                    // 3. Daily Safe To Spend Circular Gauge Dial
                    com.braveboy.hesabi.ui.components.DailySafeToSpendWidget(
                        safeToSpend = com.braveboy.hesabi.domain.logic.DailySafeToSpend(
                            safeToSpendTodayToman = 600_000,
                            spentTodayToman = 360_000,
                            daysRemaining = 12,
                            totalAvailableToman = 5_000_000,
                            totalExpenseThisCycleToman = 2_000_000,
                            cycleStartDate = com.braveboy.hesabi.core.date.JalaliDate.today(),
                            nextPaydayDate = com.braveboy.hesabi.core.date.JalaliDate.today(),
                            burnRateStatus = com.braveboy.hesabi.domain.logic.BurnRateStatus.EXCELLENT,
                            dailyBudgetCapToman = 500_000,
                        )
                    )

                    // 4. Quick Action Pills Row
                    com.braveboy.hesabi.ui.components.QuickActionRow(
                        onVoiceEntry = {},
                        onQuickExpense = {},
                        onTransfer = {},
                    )
                }
            }
        }
    }
}

@Preview(name = "Lumina UI - Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
fun PreviewLuminaUiLight() {
    val lumina = com.braveboy.hesabi.ui.theme.LightLuminaColors
    HesabiTheme(themeMode = com.braveboy.hesabi.domain.model.ThemeMode.LIGHT) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(16.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    // 1. Top Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = lumina.voicePillBg,
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, lumina.voicePillIcon),
                                modifier = Modifier.size(44.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("👤", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Column {
                                Text(
                                    text = "سلام، ماهگین",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = lumina.textSecondary,
                                )
                                Text(
                                    text = "درختی ترینی کن !",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = lumina.textPrimary,
                                )
                            }
                        }

                        // Glass Bell with Pink Badge
                        Surface(
                            shape = CircleShape,
                            color = lumina.pillBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, lumina.pillBorder),
                            shadowElevation = lumina.cardElevation,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                BadgedBox(
                                    badge = {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEC4899)),
                                        )
                                    }
                                ) {
                                    Icon(
                                        Icons.Rounded.Notifications,
                                        contentDescription = "نوتیفیکیشن",
                                        tint = lumina.textPrimary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }

                    // 2. Main Hero Balance Card
                    com.braveboy.hesabi.ui.components.HeroBalanceCard(
                        totalBalance = "۰۱۴۲,۲۷۵,۰۰۰",
                        monthExpense = "۳,۲۰۰,۰۰۰",
                        monthIncome = "۸,۵۰۰,۰۰۰",
                    )

                    // 3. Daily Safe To Spend Circular Gauge Dial
                    com.braveboy.hesabi.ui.components.DailySafeToSpendWidget(
                        safeToSpend = com.braveboy.hesabi.domain.logic.DailySafeToSpend(
                            safeToSpendTodayToman = 600_000,
                            spentTodayToman = 360_000,
                            daysRemaining = 12,
                            totalAvailableToman = 5_000_000,
                            totalExpenseThisCycleToman = 2_000_000,
                            cycleStartDate = com.braveboy.hesabi.core.date.JalaliDate.today(),
                            nextPaydayDate = com.braveboy.hesabi.core.date.JalaliDate.today(),
                            burnRateStatus = com.braveboy.hesabi.domain.logic.BurnRateStatus.EXCELLENT,
                            dailyBudgetCapToman = 500_000,
                        )
                    )

                    // 4. Quick Action Pills Row
                    com.braveboy.hesabi.ui.components.QuickActionRow(
                        onVoiceEntry = {},
                        onQuickExpense = {},
                        onTransfer = {},
                    )
                }
            }
        }
    }
}
