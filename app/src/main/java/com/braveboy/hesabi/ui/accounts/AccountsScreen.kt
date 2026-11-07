package com.braveboy.hesabi.ui.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.AccountKind
import com.braveboy.hesabi.domain.model.AccountWithBalance
import com.braveboy.hesabi.ui.components.ConfirmDialog
import com.braveboy.hesabi.ui.components.EmptyState
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.HesabiTextField
import com.braveboy.hesabi.ui.components.HesabiTopBar
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.components.accountIcon
import com.braveboy.hesabi.ui.components.CategoryColorChoices
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.preview.PreviewData
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme

@Composable
fun AccountsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AccountsViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Account?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Account?>(null) }
    var reconciling by remember { mutableStateOf<AccountWithBalance?>(null) }

    AccountsContent(
        state = state,
        onBack = onBack,
        onAdd = { creating = true },
        onEdit = { editing = it },
        onReconcile = { reconciling = it },
        onDismissNotice = viewModel::dismissNotice,
        modifier = modifier,
    )

    reconciling?.let { item ->
        ReconciliationDialog(
            item = item,
            onDismiss = { reconciling = null },
            onConfirmReconcile = { actual ->
                viewModel.reconcileAccount(item.account.id, actual, item.balanceToman)
                reconciling = null
            },
        )
    }

    if (creating || editing != null) {
        val account = editing
        AccountEditorDialog(
            initial = account,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { name, kind, color, opening, archived ->
                viewModel.save(account?.id, name, kind, color, opening, archived)
                creating = false
                editing = null
            },
            onDelete = account?.let {
                {
                    deleting = it
                    editing = null
                }
            },
        )
    }

    deleting?.let { account ->
        ConfirmDialog(
            title = "حذف حساب",
            message = "اگر این حساب تراکنش داشته باشد حذف نمی‌شود؛ در عوض آرشیوش کن.",
            confirmLabel = "حذف",
            dangerous = true,
            onConfirm = {
                viewModel.delete(account)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
fun AccountsContent(
    state: AccountsUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Account) -> Unit,
    onReconcile: (AccountWithBalance) -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        HesabiTopBar(
            title = "حساب‌ها",
            subtitle = "مبلغ شروع و موجودی هر حساب",
            onBack = onBack,
            actions = {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Rounded.Add, contentDescription = "حساب جدید")
                }
            },
        )
        LazyColumn(
            contentPadding = PaddingValues(HesabiSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
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
            if (state.accounts.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        title = "حسابی نداری",
                        message = "برای شروع یک حساب نقدی بساز.",
                        actionLabel = "حساب جدید",
                        onAction = onAdd,
                    )
                }
            }
            items(state.accounts, key = { it.account.id }) { item ->
                AccountCard(
                    item = item,
                    onClick = { onEdit(item.account) },
                    onReconcile = { onReconcile(item) },
                )
            }
            item {
                HesabiButton(
                    text = "حساب جدید",
                    onClick = onAdd,
                    variant = com.braveboy.hesabi.ui.components.HesabiButtonVariant.TONAL,
                    icon = Icons.Rounded.Add,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AccountCard(
    item: AccountWithBalance,
    onClick: () -> Unit,
    onReconcile: () -> Unit,
) {
    val account = item.account
    val color = Color(account.colorArgb)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(HesabiSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
        ) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.15f), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(accountIcon(account.kind), contentDescription = null, tint = color)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs)) {
                    Text(account.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    if (account.archived) {
                        Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(
                                text = "آرشیو",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Text(
                    account.kind.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Money.format(item.balanceToman, withUnit = false),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.balanceToman < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = onReconcile,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Rounded.AccountBalance,
                        contentDescription = "مغایرت‌گیری با بانک",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountEditorDialog(
    initial: Account?,
    onDismiss: () -> Unit,
    onSave: (String, AccountKind, Long, Long, Boolean) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var kind by remember { mutableStateOf(initial?.kind ?: AccountKind.CASH) }
    var color by remember { mutableStateOf(initial?.colorArgb ?: CategoryColorChoices.first()) }
    var openingText by remember {
        mutableStateOf(
            initial?.openingBalanceToman
                ?.takeIf { it != 0L }
                ?.let { Money.format(it, withUnit = false) }
                .orEmpty(),
        )
    }
    var archived by remember { mutableStateOf(initial?.archived ?: false) }
    val opening = Money.parseToman(openingText) ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "حساب جدید" else "ویرایش حساب") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                HesabiTextField(value = name, onValueChange = { name = it }, label = "نام حساب")
                Text("نوع حساب", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs)) {
                    AccountKind.entries.forEach { option ->
                        FilterChip(
                            selected = kind == option,
                            onClick = { kind = option },
                            label = { Text(option.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                HesabiTextField(
                    value = openingText,
                    onValueChange = { openingText = it },
                    label = "موجودی اولیه (تومان)",
                    placeholder = "۰",
                )
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
                if (initial != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("آرشیو شده", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = archived, onCheckedChange = { archived = it })
                    }
                }
                if (openingText.isNotBlank() && Money.parseToman(openingText) == null) {
                    Text(
                        "مبلغ قابل خواندن نیست.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, kind, color, opening, archived) },
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

@Preview(name = "حساب‌ها", heightDp = 700, showBackground = true)
@Composable
private fun AccountsPreview() {
    val state = AccountsUiState(
        accounts = PreviewData.balances,
        loading = false,
    )
    HesabiTheme {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AccountsContent(state, {}, {}, {}, {}, {})
            }
        }
    }
}

@Preview(
    name = "حساب‌ها - خالی",
    heightDp = 600,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun AccountsEmptyPreview() {
    HesabiTheme(themeMode = com.braveboy.hesabi.domain.model.ThemeMode.DARK) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AccountsContent(AccountsUiState(loading = false), {}, {}, {}, {}, {})
            }
        }
    }
}
