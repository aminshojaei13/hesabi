package com.braveboy.hesabi.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.braveboy.hesabi.core.date.JalaliCalendar
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.ui.preview.HesabiPreview
import com.braveboy.hesabi.ui.preview.PreviewData
import com.braveboy.hesabi.ui.theme.HesabiSpacing

/**
 * Persian-first date picker. The week starts on Saturday and the grid is laid out
 * right-to-left because the surrounding layout is RTL.
 */
@Composable
fun JalaliDatePickerDialog(
    initial: JalaliDate,
    onDismiss: () -> Unit,
    onConfirm: (JalaliDate) -> Unit,
) {
    var visibleMonth by remember { mutableStateOf(MonthPeriod.of(initial)) }
    var selected by remember { mutableStateOf(initial) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(HesabiSpacing.lg), verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("انتخاب تاریخ", style = MaterialTheme.typography.titleMedium)
                    Row {
                        IconButton(onClick = { visibleMonth = visibleMonth.previous() }) {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = "ماه قبل")
                        }
                        IconButton(onClick = { visibleMonth = visibleMonth.next() }) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = "ماه بعد")
                        }
                    }
                }
                Text(
                    text = visibleMonth.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                WeekdayHeader()
                MonthGrid(
                    period = visibleMonth,
                    selected = selected,
                    onSelect = { selected = it },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = {
                        selected = JalaliDate.today()
                        visibleMonth = MonthPeriod.of(selected)
                    }) {
                        Text("امروز")
                    }
                    Row {
                        TextButton(onClick = onDismiss) { Text("انصراف") }
                        TextButton(onClick = { onConfirm(selected) }) {
                            Text("تأیید", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val labels = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    period: MonthPeriod,
    selected: JalaliDate,
    onSelect: (JalaliDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val offset = JalaliCalendar.monthGridOffset(period)
    val days = period.days
    val cells = buildList<JalaliDate?> {
        repeat(offset) { add(null) }
        for (day in 1..days) add(JalaliDate(period.year, period.month, day))
        while (size % 7 != 0) add(null)
    }
    val today = JalaliDate.today()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            val isSelected = date == selected
                            val isToday = date == today
                            Surface(
                                onClick = { onSelect(date) },
                                modifier = Modifier.size(38.dp),
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                border = if (!isSelected && isToday) {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    null
                                },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = Digits.toPersian(date.day.toString()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.height(38.dp))
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "تقویم جلالی", showBackground = true)
@Composable
private fun JalaliPickerPreview() {
    HesabiPreview {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(HesabiSpacing.lg), verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                Text(
                    text = MonthPeriod.of(PreviewData.today).label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                WeekdayHeader()
                MonthGrid(
                    period = MonthPeriod.of(PreviewData.today),
                    selected = PreviewData.today,
                    onSelect = {},
                )
            }
        }
    }
}
