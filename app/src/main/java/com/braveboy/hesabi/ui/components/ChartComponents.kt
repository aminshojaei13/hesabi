package com.braveboy.hesabi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.model.CategorySlice
import com.braveboy.hesabi.domain.model.DailyPoint
import com.braveboy.hesabi.ui.preview.HesabiPreview
import com.braveboy.hesabi.ui.preview.PreviewData
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.LocalHesabiSemantic
import kotlin.math.max

/**
 * RTL-safe donut. Slices are drawn clockwise starting at twelve o'clock; the
 * order is already sorted by amount, so the biggest category reads first.
 */
@Composable
fun DonutChart(
    slices: List<CategorySlice>,
    modifier: Modifier = Modifier,
    centerLabel: String,
    centerValue: String,
    strokeWidth: androidx.compose.ui.unit.Dp = 22.dp,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(190.dp)) {
            val stroke = strokeWidth.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            if (slices.isEmpty()) {
                drawArc(
                    color = Color.Gray.copy(alpha = 0.16f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
            } else {
                var start = -90f
                slices.forEach { slice ->
                    val sweep = (slice.share.coerceIn(0f, 1f) * 360f)
                    val gap = if (sweep > 6f) 3f else 0f
                    drawArc(
                        color = Color(slice.colorArgb),
                        startAngle = start + gap / 2f,
                        sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                    start += sweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = centerValue,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun CategoryBreakdown(
    slices: List<CategorySlice>,
    modifier: Modifier = Modifier,
    totalToman: Long = slices.sumOf { it.amountToman },
    maxRows: Int = 6,
) {
    val visible = slices.take(maxRows)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md)) {
        visible.forEach { slice ->
            val percent = (slice.share * 100).toInt()
            Column(verticalArrangement = Arrangement.spacedBy(HesabiSpacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                ) {
                    Surface(shape = CircleShape, color = Color(slice.colorArgb), modifier = Modifier.size(10.dp), content = {})
                    Icon(
                        imageVector = categoryIcon(slice.iconKey),
                        contentDescription = null,
                        tint = Color(slice.colorArgb),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = slice.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = Money.compact(slice.amountToman),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = "${Digits.toPersian(percent.toString())}٪",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(34.dp),
                    )
                }
                LinearProgressIndicator(
                    progress = { slice.share.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Color(slice.colorArgb),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )
            }
        }
        if (visible.isEmpty()) {
            Text(
                text = "این ماه هزینه‌ای ثبت نشده است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (totalToman > 0) {
            Text(
                text = "مجموع: " + Money.format(totalToman),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DailyBarChart(
    points: List<DailyPoint>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 120.dp,
) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.surfaceVariant
    val maxValue = max(1L, points.maxOfOrNull { it.expenseToman } ?: 0L)
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            val gap = if (points.size > 25) 2f else 4.dp.toPx()
            val barWidth = ((size.width - gap * (points.size - 1)) / max(1, points.size)).coerceAtLeast(1f)
            val radius = 3.dp.toPx()
            points.forEachIndexed { index, point ->
                val ratio = (point.expenseToman.toFloat() / maxValue).coerceIn(0f, 1f)
                val barHeight = (ratio * size.height).coerceAtLeast(if (point.expenseToman > 0) 4f else 2f)
                val x = size.width - (barWidth + gap) * (index + 1) + gap
                val isLast = index == points.lastIndex
                drawRoundRect(
                    color = if (point.expenseToman > 0) primary.copy(alpha = if (isLast) 1f else 0.45f) else muted,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = HesabiSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            points.firstOrNull()?.let { Text(Digits.toPersian(dayOfMonth(it.epochDay)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text("روزهای ماه", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            points.lastOrNull()?.let { Text(Digits.toPersian(dayOfMonth(it.epochDay)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

private fun dayOfMonth(epochDay: Long): String =
    com.braveboy.hesabi.core.date.JalaliDate.fromEpochDay(epochDay).day.toString()

@Composable
fun ComparisonBadge(
    deltaPercent: Int?,
    modifier: Modifier = Modifier,
    previousLabel: String = "ماه قبل",
) {
    if (deltaPercent == null) {
        Text(
            text = "داده‌ای برای مقایسه با $previousLabel نیست.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    val increased = deltaPercent > 0
    val flat = deltaPercent == 0
    val semantic = LocalHesabiSemantic.current
    val color = when {
        flat -> MaterialTheme.colorScheme.onSurfaceVariant
        increased -> semantic.expense
        else -> semantic.income
    }
    val icon = if (increased) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xxs),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(
            text = if (flat) "مثل $previousLabel" else "${Digits.toPersian(kotlin.math.abs(deltaPercent).toString())}٪ ${if (increased) "بیشتر" else "کمتر"} از $previousLabel",
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
fun BudgetProgressCard(
    spentToman: Long,
    limitToman: Long?,
    modifier: Modifier = Modifier,
    title: String = "بودجهٔ این ماه",
    onEdit: (() -> Unit)? = null,
) {
    val semantic = LocalHesabiSemantic.current
    val percent = if (limitToman == null || limitToman <= 0) 0f else (spentToman.toFloat() / limitToman)
    val over = limitToman != null && spentToman > limitToman
    val near = !over && percent >= 0.8f
    val color = when {
        over -> MaterialTheme.colorScheme.error
        near -> semantic.warning
        else -> MaterialTheme.colorScheme.primary
    }
    HesabiCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs)) {
                Icon(Icons.Rounded.Savings, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            if (onEdit != null) {
                Text(
                    text = if (limitToman == null) "تعیین بودجه" else "ویرایش",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(onClick = onEdit)
                        .padding(4.dp),
                )
            }
        }
        if (limitToman == null || limitToman <= 0) {
            Text(
                text = "برای کنترل خرجِ ماه یک سقف تعیین کن.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@HesabiCard
        }
        Spacer(Modifier.height(HesabiSpacing.xs))
        LinearProgressIndicator(
            progress = { percent.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = HesabiSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "مصرف: " + Money.compact(spentToman),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (over) {
                    "بیش از بودجه " + Money.compact(spentToman - limitToman)
                } else {
                    "باقی‌مانده " + Money.compact(limitToman - spentToman)
                },
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun BudgetRow(
    title: String,
    spentToman: Long,
    limitToman: Long,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
) {
    val percent = if (limitToman <= 0) 0f else spentToman.toFloat() / limitToman
    val over = spentToman > limitToman
    Column(modifier = modifier.fillMaxWidth().padding(vertical = HesabiSpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                text = Money.compact(spentToman) + " / " + Money.compact(limitToman),
                style = MaterialTheme.typography.labelSmall,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { percent.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 4.dp),
            color = if (over) MaterialTheme.colorScheme.error else color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Preview(name = "نمودار دونات", showBackground = true)
@Composable
private fun DonutPreview() {
    val slices = listOf(
        CategorySlice("cat_food", "خوراک", "restaurant", 0xFFD6455D, 1_850_000, 0.42f),
        CategorySlice("cat_home", "خانه", "home", 0xFFC9820A, 1_200_000, 0.27f),
        CategorySlice("cat_transport", "حمل‌ونقل", "directions_car", 0xFF3B82F6, 780_000, 0.18f),
        CategorySlice("cat_fun", "سرگرمی", "celebration", 0xFFEC4899, 570_000, 0.13f),
    )
    HesabiPreview(spacing = HesabiSpacing.md) {
        DonutChart(slices = slices, centerLabel = "هزینهٔ آبان", centerValue = Money.compact(4_400_000))
        CategoryBreakdown(slices = slices)
    }
}

@Preview(name = "نمودار روزانه", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BarsPreview() {
    val points = PreviewData.today.let { today ->
        val start = com.braveboy.hesabi.core.date.MonthPeriod(today.year, today.month).startEpochDay
        (0 until 30).map { offset ->
            DailyPoint(
                epochDay = start + offset,
                expenseToman = if (offset % 4 == 0) 0 else (offset * 97_000L) % 1_400_000,
                incomeToman = 0,
            )
        }
    }
    HesabiPreview(dark = true) {
        HesabiCard {
            SectionHeader("روند روزانه")
            DailyBarChart(points)
        }
        BudgetProgressCard(spentToman = 3_900_000, limitToman = 5_000_000)
        BudgetProgressCard(spentToman = 5_400_000, limitToman = 5_000_000)
        ComparisonBadge(deltaPercent = 18)
        ComparisonBadge(deltaPercent = -7)
        ComparisonBadge(deltaPercent = null)
    }
}

@Preview(name = "ردیف بودجه")
@Composable
private fun BudgetRowPreview() {
    HesabiPreview {
        BudgetRow("خوراک", 1_850_000, 2_000_000, color = Color(0xFFD6455D))
        BudgetRow("خانه", 1_400_000, 1_200_000, color = Color(0xFFC9820A))
        BudgetProgressCard(spentToman = 2_000_000, limitToman = null)
    }
}
