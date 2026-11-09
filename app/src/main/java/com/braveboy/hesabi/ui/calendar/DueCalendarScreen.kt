package com.braveboy.hesabi.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.date.JalaliCalendar
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.ui.components.AmountText
import com.braveboy.hesabi.ui.components.EmptyState
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.HesabiTopBar
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.components.MonthSwitcher
import com.braveboy.hesabi.ui.components.SectionHeader
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.theme.HesabiSpacing

/** Monthly due-date calendar for every commitment and recurring entry. */
@Composable
fun DueCalendarScreen(
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: DueCalendarViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(HesabiSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
    ) {
        item {
            HesabiTopBar(
                title = "تقویم سررسید",
                subtitle = "چک، قسط، بدهی و دوره‌ای‌ها",
                onBack = onBack,
            )
        }
        item {
            MonthSwitcher(
                period = state.period,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onGoToCurrentMonth = viewModel::goToCurrentMonth,
                currentPeriod = MonthPeriod.current(),
            )
        }
        item {
            HesabiCard {
                Text(
                    text = "مجموع تعهدات این ماه",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AmountText(
                    amountToman = state.totalDueToman,
                    style = MaterialTheme.typography.headlineSmall,
                    hidden = null,
                )
                Text(
                    text = "${state.items.size} مورد در تقویم ثبت شده است.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            HesabiCard {
                Row(modifier = Modifier.fillMaxWidth()) {
                    JalaliCalendar.WEEKDAYS.forEach { weekday ->
                        Text(
                            text = weekday.take(1),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                state.cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { cell ->
                            CalendarDayCell(
                                cell = cell,
                                selected = cell.date == state.selectedDay,
                                today = cell.date == JalaliDate.today(),
                                onClick = { cell.date?.let(viewModel::selectDay) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        item {
            SectionHeader(
                title = "سررسید ${Digits.toPersian(state.selectedDay.day.toString())} ${state.selectedDay.monthName()}",
                actionLabel = if (state.items.isNotEmpty()) "همه" else null,
            )
        }
        if (state.selectedItems.isEmpty()) {
            item {
                EmptyState(
                    title = "برای این روز چیزی نیست",
                    message = "روزهای دارای نقطه را انتخاب کن یا تعهد جدید ثبت کن.",
                )
            }
        } else {
            items(state.selectedItems, key = { it.id }) { item ->
                DueItemRow(item)
            }
        }
        item {
            InfoBanner(
                text = "یادآورها با WorkManager حتی وقتی برنامه بسته است بررسی می‌شوند.",
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    cell: CalendarCell,
    selected: Boolean,
    today: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val date = cell.date
    Surface(
        onClick = onClick,
        enabled = date != null,
        modifier = modifier
            .height(42.dp)
            .padding(1.dp),
        shape = MaterialTheme.shapes.small,
        color = when {
            selected -> MaterialTheme.colorScheme.primary
            today -> MaterialTheme.colorScheme.secondaryContainer
            else -> Color.Transparent
        },
        border = if (today && !selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (date != null) {
                Text(
                    text = Digits.toPersian(date.day.toString()),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (cell.itemCount > 0) {
                    Surface(
                        modifier = Modifier.size(5.dp),
                        shape = CircleShape,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        content = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun DueItemRow(item: DueItem) {
    HesabiCard(contentPadding = PaddingValues(HesabiSpacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = kindLabel(item.kind) + " • ${JalaliDate.fromEpochDay(item.dueEpochDay).formatShort()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = Money.format(item.amountToman),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun kindLabel(kind: DueKind): String = when (kind) {
    DueKind.CHEQUE -> "چک"
    DueKind.INSTALLMENT -> "قسط"
    DueKind.DEBT -> "بدهی/طلب"
    DueKind.RECURRING -> "دوره‌ای"
    DueKind.GOAL -> "هدف"
    DueKind.SALARY -> "حقوق"
}
