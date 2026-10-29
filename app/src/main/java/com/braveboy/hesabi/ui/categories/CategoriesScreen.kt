package com.braveboy.hesabi.ui.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.ui.components.CategoryColorChoices
import com.braveboy.hesabi.ui.components.CategoryIconChoices
import com.braveboy.hesabi.ui.components.ConfirmDialog
import com.braveboy.hesabi.ui.components.EmptyState
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiButtonVariant
import com.braveboy.hesabi.ui.components.HesabiTextField
import com.braveboy.hesabi.ui.components.HesabiTopBar
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.components.categoryIcon
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.preview.PreviewData
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme

@Composable
fun CategoriesScreen(
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CategoriesViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Category?>(null) }

    CategoriesContent(
        state = state,
        onBack = onBack,
        onKindChange = viewModel::setKind,
        onAdd = { creating = true },
        onEdit = { editing = it },
        onDismissNotice = viewModel::dismissNotice,
        modifier = modifier,
    )

    if (creating || editing != null) {
        val category = editing
        CategoryEditorDialog(
            initial = category,
            kind = state.kind,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { name, iconKey, color ->
                viewModel.save(category?.id, name, state.kind, iconKey, color)
                creating = false
                editing = null
            },
            onDelete = category?.let {
                {
                    deleting = it
                    editing = null
                }
            },
        )
    }

    deleting?.let { category ->
        ConfirmDialog(
            title = "حذف دسته",
            message = "دسته‌ای که تراکنش دارد حذف نمی‌شود. برای بقیه، حذف قطعی است.",
            confirmLabel = "حذف",
            dangerous = true,
            onConfirm = {
                viewModel.delete(category)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
fun CategoriesContent(
    state: CategoriesUiState,
    onBack: () -> Unit,
    onKindChange: (CategoryKind) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Category) -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        HesabiTopBar(
            title = "دسته‌ها",
            subtitle = "رنگ و آیکون را خودت انتخاب کن",
            onBack = onBack,
            actions = {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Rounded.Add, contentDescription = "دسته جدید")
                }
            },
        )
        Column(modifier = Modifier.padding(horizontal = HesabiSpacing.lg)) {
            Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                CategoryKind.entries.forEach { kind ->
                    FilterChip(
                        selected = state.kind == kind,
                        onClick = { onKindChange(kind) },
                        label = { Text(if (kind == CategoryKind.EXPENSE) "هزینه‌ها" else "درآمدها") },
                    )
                }
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(HesabiSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
        ) {
            if (state.error != null || state.message != null) {
                item {
                    InfoBanner(
                        text = state.error ?: state.message.orEmpty(),
                        containerColor = if (state.error != null) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = if (state.error != null) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        actionLabel = "باشه",
                        onAction = onDismissNotice,
                    )
                }
            }
            if (state.categories.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        title = "دسته‌ای نیست",
                        message = "یک دستهٔ جدید برای ${if (state.kind == CategoryKind.EXPENSE) "هزینه‌ها" else "درآمدها"} بساز.",
                        actionLabel = "دسته جدید",
                        onAction = onAdd,
                    )
                }
            }
            items(state.categories, key = { it.id }) { category ->
                CategoryCard(category = category, onClick = { onEdit(category) })
            }
            item {
                HesabiButton(
                    text = "دسته جدید",
                    onClick = onAdd,
                    variant = HesabiButtonVariant.TONAL,
                    icon = Icons.Rounded.Add,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(category: Category, onClick: () -> Unit) {
    val color = Color(category.colorArgb)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(HesabiSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
        ) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.16f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(categoryIcon(category.iconKey), contentDescription = null, tint = color)
                }
            }
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Surface(shape = CircleShape, color = color, modifier = Modifier.size(10.dp), content = {})
        }
    }
}

@Composable
private fun CategoryEditorDialog(
    initial: Category?,
    kind: CategoryKind,
    onDismiss: () -> Unit,
    onSave: (String, String, Long) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var iconKey by remember { mutableStateOf(initial?.iconKey ?: CategoryIconChoices.first().first) }
    var color by remember { mutableStateOf(initial?.colorArgb ?: CategoryColorChoices.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "دستهٔ جدید" else "ویرایش دسته") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                HesabiTextField(value = name, onValueChange = { name = it }, label = "نام دسته")
                Text("آیکون", style = MaterialTheme.typography.labelMedium)
                CategoryIconChoices.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs)) {
                        row.forEach { (key, label) ->
                            val selected = iconKey == key
                            Surface(
                                onClick = { iconKey = key },
                                shape = MaterialTheme.shapes.small,
                                color = if (selected) {
                                    Color(color).copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                border = if (selected) BorderStroke(1.5.dp, Color(color)) else null,
                                modifier = Modifier.size(44.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        categoryIcon(key),
                                        contentDescription = label,
                                        tint = if (selected) Color(color) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                Text("رنگ", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs)) {
                    CategoryColorChoices.take(8).forEach { option ->
                        Surface(
                            onClick = { color = option },
                            shape = CircleShape,
                            color = Color(option),
                            border = if (color == option) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null,
                            modifier = Modifier.size(28.dp),
                            content = {},
                        )
                    }
                }
                Text(
                    text = if (kind == CategoryKind.EXPENSE) "برای دستهٔ هزینه" else "برای دستهٔ درآمد",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, iconKey, color) },
                enabled = name.isNotBlank(),
            ) { Text("ذخیره") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = onDismiss) { Text("انصراف") }
            }
        },
    )
}

@Preview(name = "دسته‌ها", heightDp = 750, showBackground = true)
@Composable
private fun CategoriesPreview() {
    val state = CategoriesUiState(
        kind = CategoryKind.EXPENSE,
        categories = PreviewData.categories.filter { it.kind == CategoryKind.EXPENSE },
        loading = false,
    )
    HesabiTheme {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                CategoriesContent(state, {}, {}, {}, {}, {})
            }
        }
    }
}

@Preview(
    name = "دسته‌ها - تیره",
    heightDp = 750,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun CategoriesDarkPreview() {
    val state = CategoriesUiState(
        kind = CategoryKind.INCOME,
        categories = PreviewData.categories.filter { it.kind == CategoryKind.INCOME },
        loading = false,
    )
    HesabiTheme(themeMode = com.braveboy.hesabi.domain.model.ThemeMode.DARK) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                CategoriesContent(state, {}, {}, {}, {}, {})
            }
        }
    }
}
