package com.braveboy.hesabi.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.ui.preview.HesabiPreview
import com.braveboy.hesabi.ui.theme.HesabiRadius
import com.braveboy.hesabi.ui.theme.HesabiSpacing

enum class HesabiButtonVariant { PRIMARY, SECONDARY, TONAL, TEXT, DANGER }

@Composable
fun HesabiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HesabiButtonVariant = HesabiButtonVariant.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val content: @Composable () -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = if (variant == HesabiButtonVariant.PRIMARY) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
            }
            Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    when (variant) {
        HesabiButtonVariant.PRIMARY -> Button(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            enabled = enabled && !loading,
            shape = MaterialTheme.shapes.small,
            content = { content() },
        )
        HesabiButtonVariant.SECONDARY -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            enabled = enabled && !loading,
            shape = MaterialTheme.shapes.small,
            content = { content() },
        )
        HesabiButtonVariant.TONAL -> FilledTonalButton(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            enabled = enabled && !loading,
            shape = MaterialTheme.shapes.small,
            content = { content() },
        )
        HesabiButtonVariant.TEXT -> TextButton(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            enabled = enabled && !loading,
            content = { content() },
        )
        HesabiButtonVariant.DANGER -> Button(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            enabled = enabled && !loading,
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            content = { content() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HesabiTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت")
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}

@Composable
fun HesabiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(HesabiSpacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier.then(clickable),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Rounded.ChevronLeft, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {
        Icon(Icons.Rounded.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    },
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = HesabiSpacing.lg, vertical = HesabiSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
    ) {
        if (icon != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing()
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HesabiSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
    ) {
        if (icon != null) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(30.dp))
                }
            }
        }
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(HesabiSpacing.sm))
            HesabiButton(text = actionLabel, onClick = onAction, variant = HesabiButtonVariant.TONAL)
        }
    }
}

@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Info,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(HesabiSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel, color = contentColor, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun HesabiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = singleLine,
        shape = MaterialTheme.shapes.small,
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
    )
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "جست‌وجو",
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = if (value.isNotBlank()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Rounded.Clear, contentDescription = "پاک کردن")
                }
            }
        } else {
            null
        },
    )
}

@Composable
fun <T> FilterChipRow(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
        contentPadding = PaddingValues(horizontal = HesabiSpacing.xs),
    ) {
        items(options) { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                shape = MaterialTheme.shapes.extraLarge,
            )
        }
    }
}

@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.error,
    ) {
        Text(
            text = if (count > 99) "۹۹+" else com.braveboy.hesabi.core.money.Digits.toPersian(count.toString()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onError,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dangerous: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = if (dangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )
}

@Composable
fun LoadingSkeleton(modifier: Modifier = Modifier, rows: Int = 4) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md)) {
        repeat(rows) { index ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (index == 0) 88.dp else 64.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                content = {},
            )
        }
    }
}

@Composable
fun UndoBar(
    message: String,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String = "واگرد",
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.inverseSurface,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = HesabiSpacing.lg, vertical = HesabiSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onUndo) {
                Text(actionLabel, color = MaterialTheme.colorScheme.inversePrimary)
            }
        }
    }
}

@Preview(name = "دکمه‌ها")
@Composable
private fun ButtonsPreview() {
    HesabiPreview {
        HesabiButton("ثبت هزینه", onClick = {})
        HesabiButton("ثبت درآمد", onClick = {}, variant = HesabiButtonVariant.TONAL)
        HesabiButton("انصراف", onClick = {}, variant = HesabiButtonVariant.SECONDARY)
        HesabiButton("حذف", onClick = {}, variant = HesabiButtonVariant.DANGER)
        HesabiButton("در حال ذخیره", onClick = {}, loading = true)
    }
}

@Preview(name = "کارت و بنر", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CardPreview() {
    HesabiPreview(dark = true) {
        HesabiCard {
            SectionHeader("تراکنش‌های اخیر", actionLabel = "همه")
            Text("۱۲ تراکنش این ماه", style = MaterialTheme.typography.bodyMedium)
        }
        InfoBanner("مبالغ فقط روی همین گوشی ذخیره می‌شوند.", actionLabel = "بیشتر")
        EmptyState(
            title = "هنوز چیزی ثبت نشده",
            message = "با دکمهٔ + اولین هزینه را ثبت کن.",
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            actionLabel = "شروع",
            onAction = {},
        )
    }
}

@Preview(name = "نوار بالا و ردیف‌ها")
@Composable
private fun TopBarAndRowsPreview() {
    HesabiPreview {
        HesabiTopBar(
            title = "حساب‌ها",
            subtitle = "موجودی هر حساب",
            onBack = {},
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.Settings, contentDescription = "تنظیمات")
                }
            },
        )
        HesabiCard(contentPadding = PaddingValues(vertical = HesabiSpacing.xs)) {
            SettingsRow(
                title = "پشتیبان و خروجی",
                subtitle = "فایل رمزدار یا CSV",
                icon = Icons.Rounded.Backup,
                onClick = {},
            )
            SettingsRow(
                title = "پیامک‌های بانکی",
                subtitle = "دریافت اختیاری",
                icon = Icons.Rounded.Info,
                onClick = {},
                trailing = { CountBadge(count = 3) },
            )
            SettingsRow(
                title = "بدون آیکون",
                subtitle = "ردیف ساده",
                onClick = {},
            )
        }
    }
}

@Preview(name = "فیلدها و برچسب‌ها", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FieldsPreview() {
    HesabiPreview(dark = true) {
        HesabiTextField(
            value = "ناهار با تیم",
            onValueChange = {},
            label = "یادداشت",
            placeholder = "مثلاً ناهار با تیم",
        )
        HesabiTextField(
            value = "",
            onValueChange = {},
            label = "مبلغ",
            error = "مبلغ قابل خواندن نیست.",
        )
        SearchField(value = "کافه", onValueChange = {})
        Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
            CountBadge(count = 2)
            CountBadge(count = 150)
            CountBadge(count = 0)
        }
        FilterChipRow(
            options = listOf("همه", "هزینه", "درآمد"),
            selected = "هزینه",
            onSelect = {},
            label = { it },
        )
    }
}

@Preview(name = "اسکلت، واگرد و گفت‌وگو")
@Composable
private fun FeedbackPreview() {
    HesabiPreview {
        LoadingSkeleton(rows = 3)
        UndoBar(message = "تراکنش حذف شد.", onUndo = {})
        ConfirmDialog(
            title = "حذف تراکنش",
            message = "این تراکنش برای همیشه حذف می‌شود. مطمئنی؟",
            confirmLabel = "حذف",
            dangerous = true,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
