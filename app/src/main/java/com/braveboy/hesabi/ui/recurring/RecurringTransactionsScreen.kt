package com.braveboy.hesabi.ui.recurring

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.AccountKind
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.RecurringFrequency
import com.braveboy.hesabi.domain.model.RecurringTransaction
import com.braveboy.hesabi.domain.model.ThemeMode
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.ui.components.EmptyState
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiButtonVariant
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.HesabiTopBar
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.theme.HesabiRadius
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme

@Composable
fun RecurringTransactionsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: RecurringTransactionsViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RecurringTransaction?>(null) }

    RecurringContent(
        state = state,
        onBack = onBack,
        onAdd = { creating = true },
        onEdit = { editing = it },
        onToggleActive = { id, active -> viewModel.onIntent(RecurringIntent.ToggleActive(id, active)) },
        onApplyNow = { item -> viewModel.onIntent(RecurringIntent.ApplyNow(item)) },
        onProcessDue = { viewModel.onIntent(RecurringIntent.ProcessDue) },
        onDismissNotice = { viewModel.onIntent(RecurringIntent.DismissNotice) },
        modifier = modifier,
    )

    if (creating || editing != null) {
        val current = editing
        AddEditRecurringDialog(
            initial = current,
            accounts = state.accounts,
            categories = state.categories,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { title, type, amount, accountId, toAccountId, categoryId, freq, interval, dayOfMonth, startEpoch, autoApply, note ->
                viewModel.onIntent(
                    RecurringIntent.Save(
                        id = current?.id,
                        title = title,
                        type = type,
                        amountToman = amount,
                        accountId = accountId,
                        toAccountId = toAccountId,
                        categoryId = categoryId,
                        frequency = freq,
                        interval = interval,
                        dayOfMonth = dayOfMonth,
                        startEpochDay = startEpoch,
                        autoApply = autoApply,
                        note = note,
                    )
                )
                creating = false
                editing = null
            },
            onDelete = current?.let {
                {
                    viewModel.onIntent(RecurringIntent.Delete(it.id))
                    creating = false
                    editing = null
                }
            },
        )
    }
}

@Composable
fun RecurringContent(
    state: RecurringUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (RecurringTransaction) -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    onApplyNow: (RecurringTransaction) -> Unit,
    onProcessDue: () -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                HesabiTopBar(
                    title = "تراکنش‌های دوره‌ای و ثابت",
                    subtitle = "مدیریت اجاره، قبوض، اشتراک‌ها و حقوق",
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = onAdd) {
                            Icon(Icons.Rounded.Add, contentDescription = "افزودن")
                        }
                    },
                )

                LazyColumn(
                    contentPadding = PaddingValues(
                        start = HesabiSpacing.lg,
                        top = HesabiSpacing.sm,
                        end = HesabiSpacing.lg,
                        bottom = 88.dp,
                    ),
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

                    // Summary Lumina Card
                    item {
                        RecurringSummaryHeader(
                            activeCount = state.activeItems.size,
                            monthlyCommitment = state.monthlyCommitmentToman,
                        )
                    }

                    // Due Transactions Alert Card
                    if (state.dueItems.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(HesabiRadius.card),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(HesabiSpacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            Icons.Rounded.NotificationsActive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                        )
                                        Column {
                                            Text(
                                                text = "${state.dueItems.size} تراکنش موعد رسیده",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Text(
                                                text = "موعد پرداخت یا دریافت امروز است",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            )
                                        }
                                    }
                                    HesabiButton(
                                        text = "ثبت موعدها",
                                        onClick = onProcessDue,
                                        variant = HesabiButtonVariant.PRIMARY,
                                    )
                                }
                            }
                        }
                    }

                    // Items
                    if (state.items.isEmpty() && !state.loading) {
                        item {
                            EmptyState(
                                title = "هنوز تراکنش دوره‌ای ثبت نکردی",
                                message = "اجاره، اشتراک ماهانه یا حقوق را اضافه کن تا سر موعد یادآوری یا ثبت شود.",
                                actionLabel = "تراکنش دوره‌ای جدید",
                                onAction = onAdd,
                            )
                        }
                    }

                    items(state.items, key = { it.id }) { item ->
                        val account = state.accounts.firstOrNull { it.id == item.accountId }
                        val category = state.categories.firstOrNull { it.id == item.categoryId }
                        RecurringTransactionCard(
                            item = item,
                            accountName = account?.name ?: "حساب پیش‌فرض",
                            categoryName = category?.name,
                            onClick = { onEdit(item) },
                            onToggleActive = { onToggleActive(item.id, it) },
                            onApplyNow = { onApplyNow(item) },
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "تراکنش جدید")
            }
        }
    }
}

@Composable
private fun RecurringSummaryHeader(
    activeCount: Int,
    monthlyCommitment: Long,
) {
    Surface(
        shape = RoundedCornerShape(HesabiRadius.card),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(HesabiSpacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "تعهدات ماهانه",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = Money.format(monthlyCommitment, withUnit = true),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Surface(
                shape = RoundedCornerShape(HesabiRadius.input),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
                ) {
                    Icon(
                        Icons.Rounded.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "$activeCount مورد فعال",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringTransactionCard(
    item: RecurringTransaction,
    accountName: String,
    categoryName: String?,
    onClick: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onApplyNow: () -> Unit,
) {
    val amountColor = when (item.type) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.INCOME -> Color(0xFF10B981)
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
    }
    val jalaliNext = JalaliDate.fromEpochDay(item.nextOccurrenceEpochDay)

    HesabiCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(HesabiSpacing.md),
            verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
        ) {
            // Header: Title + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        // Frequency pill
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = item.frequency.labelFa,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                        if (item.autoApply) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                            ) {
                                Text(
                                    text = "خودکار",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = buildString {
                            append(accountName)
                            if (categoryName != null) {
                                append(" • ")
                                append(categoryName)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = Money.format(item.amountToman, withUnit = true),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                )
            }

            // Footer: Next occurrence + Action switch & Apply button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "موعد بعدی: ${jalaliNext.formatDayMonth()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs),
                ) {
                    IconButton(
                        onClick = onApplyNow,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "ثبت برای امروز",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Switch(
                        checked = item.isActive,
                        onCheckedChange = onToggleActive,
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode")
@Composable
private fun RecurringTransactionsScreenPreviewLight() {
    HesabiTheme(themeMode = ThemeMode.LIGHT) {
        RecurringContent(
            state = RecurringUiState(
                items = listOf(
                    RecurringTransaction(
                        id = "1",
                        title = "اجاره آپارتمان",
                        type = TransactionType.EXPENSE,
                        amountToman = 15_000_000L,
                        accountId = "1",
                        frequency = RecurringFrequency.MONTHLY,
                        dayOfMonth = 1,
                        nextOccurrenceEpochDay = JalaliDate.today().toEpochDay(),
                        autoApply = true,
                        isActive = true,
                    ),
                    RecurringTransaction(
                        id = "2",
                        title = "حقوق ماهانه",
                        type = TransactionType.INCOME,
                        amountToman = 35_000_000L,
                        accountId = "1",
                        frequency = RecurringFrequency.MONTHLY,
                        dayOfMonth = 25,
                        nextOccurrenceEpochDay = JalaliDate.today().plusDays(10).toEpochDay(),
                        autoApply = false,
                        isActive = true,
                    ),
                ),
                accounts = listOf(Account("1", "بلوبانک", AccountKind.BANK, 0xFF2563EB, 5000000)),
                categories = listOf(Category("1", "مسکن", CategoryKind.EXPENSE, "home", 0xFF6366F1)),
                loading = false,
            ),
            onBack = {},
            onAdd = {},
            onEdit = {},
            onToggleActive = { _, _ -> },
            onApplyNow = {},
            onProcessDue = {},
            onDismissNotice = {},
        )
    }
}

@Preview(name = "Dark Mode")
@Composable
private fun RecurringTransactionsScreenPreviewDark() {
    HesabiTheme(themeMode = ThemeMode.DARK) {
        RecurringContent(
            state = RecurringUiState(
                items = listOf(
                    RecurringTransaction(
                        id = "1",
                        title = "اشتراک فیلیمو",
                        type = TransactionType.EXPENSE,
                        amountToman = 180_000L,
                        accountId = "1",
                        frequency = RecurringFrequency.MONTHLY,
                        nextOccurrenceEpochDay = JalaliDate.today().toEpochDay(),
                        autoApply = true,
                        isActive = true,
                    ),
                ),
                accounts = listOf(Account("1", "بلوبانک", AccountKind.BANK, 0xFF2563EB, 5000000)),
                loading = false,
            ),
            onBack = {},
            onAdd = {},
            onEdit = {},
            onToggleActive = { _, _ -> },
            onApplyNow = {},
            onProcessDue = {},
            onDismissNotice = {},
        )
    }
}
