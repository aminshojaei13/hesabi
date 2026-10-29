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
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.domain.model.SmsEvent
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.ui.preview.HesabiPreview
import com.braveboy.hesabi.ui.preview.PreviewData
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.LocalHesabiSemantic
import java.time.Instant
import java.time.ZoneId

@Composable
fun SmsReviewCard(
    event: SmsEvent,
    categoryName: String?,
    onAccept: () -> Unit,
    onIgnore: () -> Unit,
    onIgnoreSender: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = LocalHesabiSemantic.current
    val lowConfidence = event.confidence < 0.7f
    HesabiCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Sms, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.sender,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${com.braveboy.hesabi.core.date.JalaliDate.fromEpochDay(event.occurredOnEpochDay).formatDayMonth()} • ${timeLabel(event.receivedAtMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = if (lowConfidence) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
            ) {
                Text(
                    text = "اطمینان ${Digits.toPersian((event.confidence * 100).toInt().toString())}٪",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (lowConfidence) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(HesabiSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                if (event.type != null) TypePill(event.type)
                Spacer(Modifier.height(HesabiSpacing.xs))
                AmountText(
                    amountToman = event.amountToman ?: 0L,
                    type = event.type,
                    style = MaterialTheme.typography.titleLarge,
                    showSign = false,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (event.merchant != null) {
                    Text(
                        text = event.merchant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = buildString {
                        append("دسته: ")
                        append(categoryName ?: "انتخاب نشده")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (event.last4 != null) {
                    Text(
                        text = "کارت ••" + Digits.toPersian(event.last4),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (event.notePreview.isNotBlank()) {
            Spacer(Modifier.height(HesabiSpacing.sm))
            Text(
                text = event.notePreview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (lowConfidence) {
            Spacer(Modifier.height(HesabiSpacing.sm))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
            ) {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = semantic.warning,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "این پیامک کامل خوانده نشد؛ قبل از ثبت بررسی کن.",
                    style = MaterialTheme.typography.labelSmall,
                    color = semantic.warning,
                )
            }
        }
        Spacer(Modifier.height(HesabiSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
        ) {
            HesabiButton(
                text = "ثبت",
                onClick = onAccept,
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.MarkEmailRead,
            )
            TextButton(onClick = onIgnore) { Text("نادیده") }
        }
        TextButton(onClick = onIgnoreSender) {
            Text(
                text = "دیگر از «${event.sender}» پیشنهاد نده",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val SmsEvent.notePreview: String
    get() = body.replace(Regex("\\s+"), " ").take(140)

private fun timeLabel(millis: Long): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    return Digits.toPersian("%02d:%02d".format(time.hour, time.minute))
}

@Composable
fun PendingSmsBanner(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    InfoBanner(
        text = "${Digits.toPersian(count.toString())} پیامک بانکی آمادهٔ بررسی است.",
        modifier = modifier,
        icon = Icons.Rounded.Sms,
        actionLabel = "بررسی",
        onAction = onClick,
    )
}

@Composable
fun SmsImportDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
) {
    var text = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialText) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("چسباندن متن پیامک") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                Text(
                    "متن پیامک بانکی را اینجا بچسبان. چند پیامک را می‌توانی با یک خط خالی از هم جدا کنی.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = text.value,
                    onValueChange = { text.value = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("مثال: خرید ۴۵۰,۰۰۰ ریال کارت ...۱۲۳۴") },
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(text.value) },
                enabled = text.value.isNotBlank(),
            ) { Text("بررسی") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )
}

@Preview(name = "کارت پیامک", showBackground = true)
@Composable
private fun SmsCardPreview() {
    HesabiPreview(spacing = HesabiSpacing.md) {
        PendingSmsBanner(count = 3, onClick = {})
        SmsReviewCard(
            event = PreviewData.sms,
            categoryName = "خرید",
            onAccept = {},
            onIgnore = {},
            onIgnoreSender = {},
        )
        SmsReviewCard(
            event = PreviewData.smsLowConfidence,
            categoryName = null,
            onAccept = {},
            onIgnore = {},
            onIgnoreSender = {},
        )
    }
}

@Preview(name = "کارت پیامک - تیره", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SmsCardDarkPreview() {
    HesabiPreview(dark = true) {
        SmsReviewCard(
            event = PreviewData.sms.copy(type = TransactionType.INCOME, amountToman = 24_000_000, merchant = "شرکت نمونه", confidence = 0.95f),
            categoryName = "حقوق",
            onAccept = {},
            onIgnore = {},
            onIgnoreSender = {},
        )
    }
}
