package com.braveboy.hesabi.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionDraft
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.ui.components.AccountChips
import com.braveboy.hesabi.ui.components.AmountEntryDisplay
import com.braveboy.hesabi.ui.components.AmountKeypad
import com.braveboy.hesabi.ui.components.CategoryGrid
import com.braveboy.hesabi.ui.components.ConfirmDialog
import com.braveboy.hesabi.ui.components.DateField
import com.braveboy.hesabi.ui.components.FormSection
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.HesabiTextField
import com.braveboy.hesabi.ui.components.HesabiTopBar
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.components.JalaliDatePickerDialog
import com.braveboy.hesabi.ui.components.TransactionTypeToggle
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.preview.PreviewData
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme

@Composable
fun AddEditScreen(
    container: AppContainer,
    transactionId: String?,
    initialType: TransactionType?,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AddEditViewModel = hesabiViewModel(
        container = container,
        transactionId = transactionId,
        initialType = initialType,
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.finished) {
        if (state.finished) onDone()
    }

    AddEditContent(
        state = state,
        onBack = onBack,
        onTypeChange = viewModel::setType,
        onDigit = viewModel::appendDigit,
        onTripleZero = viewModel::appendTripleZero,
        onBackspace = viewModel::backspace,
        onAccount = viewModel::setAccount,
        onToAccount = viewModel::setToAccount,
        onCategory = viewModel::setCategory,
        onDate = viewModel::setDate,
        onNote = viewModel::setNote,
        onSave = { viewModel.save() },
        onConfirmDuplicate = { viewModel.save(force = true) },
        onDismissDuplicate = viewModel::dismissDuplicate,
        onDelete = viewModel::delete,
        modifier = modifier,
    )
}

@Composable
fun AddEditContent(
    state: AddEditUiState,
    onBack: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onDigit: (Int) -> Unit,
    onTripleZero: () -> Unit,
    onBackspace: () -> Unit,
    onAccount: (com.braveboy.hesabi.domain.model.Account) -> Unit,
    onToAccount: (com.braveboy.hesabi.domain.model.Account) -> Unit,
    onCategory: (com.braveboy.hesabi.domain.model.Category) -> Unit,
    onDate: (JalaliDate) -> Unit,
    onNote: (String) -> Unit,
    onSave: () -> Unit,
    onConfirmDuplicate: () -> Unit,
    onDismissDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val draft = state.draft
    val activeAccounts = state.accounts.filterNot { it.archived }
    val destinationAccounts = activeAccounts.filterNot { it.id == draft.accountId }

    Column(modifier = modifier.fillMaxSize()) {
        HesabiTopBar(
            title = if (state.isEditing) "ویرایش تراکنش" else "تراکنش جدید",
            subtitle = when (draft.type) {
                TransactionType.EXPENSE -> "هزینه"
                TransactionType.INCOME -> "درآمد"
                TransactionType.TRANSFER -> "انتقال بین حساب‌ها"
            },
            onBack = onBack,
            actions = {
                if (state.isEditing) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "حذف تراکنش",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(HesabiSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HesabiSpacing.lg),
        ) {
            item {
                TransactionTypeToggle(type = draft.type, onTypeChange = onTypeChange)
            }
            item {
                AmountEntryDisplay(amountToman = draft.amountToman, type = draft.type)
            }
            item {
                AmountKeypad(
                    onDigit = onDigit,
                    onTripleZero = onTripleZero,
                    onBackspace = onBackspace,
                )
            }
            if (state.errors.isNotEmpty()) {
                item {
                    InfoBanner(
                        text = state.errors.joinToString(separator = "\n"),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        icon = Icons.Rounded.ErrorOutline,
                    )
                }
            }
            if (draft.type == TransactionType.TRANSFER) {
                item {
                    FormSection(
                        title = "از حساب",
                        subtitle = "پول از این حساب کم می‌شود.",
                    ) {
                        AccountChips(
                            accounts = activeAccounts,
                            selectedId = draft.accountId,
                            onSelect = onAccount,
                        )
                    }
                }
                item {
                    FormSection(
                        title = "به حساب",
                        subtitle = "پول به این حساب اضافه می‌شود.",
                    ) {
                        AccountChips(
                            accounts = destinationAccounts,
                            selectedId = draft.toAccountId,
                            onSelect = onToAccount,
                        )
                    }
                }
            } else {
                item {
                    FormSection(title = "حساب") {
                        AccountChips(
                            accounts = activeAccounts,
                            selectedId = draft.accountId,
                            onSelect = onAccount,
                        )
                    }
                }
                item {
                    FormSection(
                        title = "دسته",
                        subtitle = if (draft.type == TransactionType.EXPENSE) {
                            "هزینه‌ها بر اساس دسته گزارش می‌شوند."
                        } else {
                            "درآمدها هم دسته‌بندی می‌شوند."
                        },
                    ) {
                        CategoryGrid(
                            categories = state.filteredCategories,
                            selectedId = draft.categoryId,
                            onSelect = onCategory,
                        )
                    }
                }
            }
            item {
                FormSection(title = "تاریخ") {
                    DateField(
                        date = JalaliDate.fromEpochDay(draft.occurredOnEpochDay),
                        onClick = { showDatePicker = true },
                    )
                }
            }
            item {
                FormSection(title = "توضیح", subtitle = "اختیاری") {
                    HesabiTextField(
                        value = draft.note,
                        onValueChange = onNote,
                        label = "یادداشت",
                        placeholder = "مثلاً ناهار با تیم",
                        singleLine = false,
                    )
                }
            }
            item {
                InfoBanner(
                    text = "پس از ذخیره هم می‌توانی همین تراکنش را ویرایش یا حذف کنی.",
                )
            }
        }
        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            HesabiButton(
                text = if (state.isEditing) "ذخیرهٔ تغییرات" else "ثبت ${draft.type.label}",
                onClick = onSave,
                enabled = state.canSave,
                loading = state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HesabiSpacing.lg, vertical = HesabiSpacing.md),
            )
        }
    }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initial = JalaliDate.fromEpochDay(draft.occurredOnEpochDay),
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                onDate(date)
                showDatePicker = false
            },
        )
    }

    state.duplicateOf?.let { duplicate ->
        ConfirmDialog(
            title = "تراکنش تکراری؟",
            message = duplicateWarning(duplicate),
            confirmLabel = "باز هم ثبت کن",
            onConfirm = {
                onDismissDuplicate()
                onConfirmDuplicate()
            },
            onDismiss = onDismissDuplicate,
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "حذف تراکنش",
            message = "این تراکنش برای همیشه حذف می‌شود. مطمئنی؟",
            confirmLabel = "حذف",
            dangerous = true,
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

private fun duplicateWarning(duplicate: Transaction): String =
    "یک تراکنش با همین مبلغ، نوع و حساب در ${duplicate.occurredOn.formatShort()} ثبت شده است. " +
        "اگر این یکی جداست، باز هم ثبت کن."

@Preview(name = "ثبت هزینه", heightDp = 1000, showBackground = true)
@Composable
private fun AddExpensePreview() {
    val state = AddEditUiState(
        draft = TransactionDraft(
            type = TransactionType.EXPENSE,
            amountToman = 185_000,
            accountId = "acc_card",
            categoryId = "cat_food",
            occurredOnEpochDay = PreviewData.today.toEpochDay(),
            note = "ناهار با همکارها",
        ),
        amountDigits = "185000",
        accounts = PreviewData.accounts,
        categories = PreviewData.categories,
        loading = false,
    )
    HesabiTheme {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AddEditContent(
                    state = state,
                    onBack = {},
                    onTypeChange = {},
                    onDigit = {},
                    onTripleZero = {},
                    onBackspace = {},
                    onAccount = {},
                    onToAccount = {},
                    onCategory = {},
                    onDate = {},
                    onNote = {},
                    onSave = {},
                    onConfirmDuplicate = {},
                    onDismissDuplicate = {},
                    onDelete = {},
                )
            }
        }
    }
}

@Preview(
    name = "انتقال - تیره",
    heightDp = 900,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun AddTransferPreview() {
    val state = AddEditUiState(
        draft = TransactionDraft(
            type = TransactionType.TRANSFER,
            amountToman = 1_500_000,
            accountId = "acc_card",
            toAccountId = "acc_cash",
            occurredOnEpochDay = PreviewData.today.toEpochDay(),
        ),
        amountDigits = "1500000",
        accounts = PreviewData.accounts,
        categories = PreviewData.categories.filter { it.kind == CategoryKind.EXPENSE },
        loading = false,
    )
    HesabiTheme(themeMode = com.braveboy.hesabi.domain.model.ThemeMode.DARK) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AddEditContent(
                    state = state,
                    onBack = {},
                    onTypeChange = {},
                    onDigit = {},
                    onTripleZero = {},
                    onBackspace = {},
                    onAccount = {},
                    onToAccount = {},
                    onCategory = {},
                    onDate = {},
                    onNote = {},
                    onSave = {},
                    onConfirmDuplicate = {},
                    onDismissDuplicate = {},
                    onDelete = {},
                )
            }
        }
    }
}
