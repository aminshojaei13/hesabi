package com.braveboy.hesabi.ui.debts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Currency
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.Cheque
import com.braveboy.hesabi.domain.model.ChequeDirection
import com.braveboy.hesabi.domain.model.Debt
import com.braveboy.hesabi.domain.model.DebtDirection
import com.braveboy.hesabi.domain.model.Installment
import com.braveboy.hesabi.domain.model.Loan
import com.braveboy.hesabi.domain.model.LoanWithInstallments
import com.braveboy.hesabi.domain.model.Person
import com.braveboy.hesabi.domain.model.PersonWithDebts
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.preview.HesabiPreview
import com.braveboy.hesabi.ui.theme.HesabiSpacing

@Composable
fun DebtsAndCommitmentsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: DebtsAndCommitmentsViewModel = hesabiViewModel(container)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DebtsAndCommitmentsContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsAndCommitmentsContent(
    uiState: DebtsUiState,
    onIntent: (DebtsIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("تعهدات مالی، چک و وام", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            val fabLabel = when (uiState.selectedTab) {
                DebtsTab.PEOPLE_AND_DEBTS -> "افزودن شخص"
                DebtsTab.CHEQUES -> "ثبت چک جدید"
                DebtsTab.LOANS -> "ثبت وام جدید"
            }
            ExtendedFloatingActionButton(
                onClick = {
                    when (uiState.selectedTab) {
                        DebtsTab.PEOPLE_AND_DEBTS -> onIntent(DebtsIntent.OpenAddPersonDialog)
                        DebtsTab.CHEQUES -> onIntent(DebtsIntent.OpenAddChequeDialog)
                        DebtsTab.LOANS -> onIntent(DebtsIntent.OpenAddLoanDialog)
                    }
                },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(fabLabel) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Tabs
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                DebtsTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { onIntent(DebtsIntent.SelectTab(tab)) },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = HesabiSpacing.lg,
                    end = HesabiSpacing.lg,
                    top = HesabiSpacing.md,
                    bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
            ) {
                // Header Summary Card
                item(key = "header_summary") {
                    CommitmentsSummaryHeader(
                        tab = uiState.selectedTab,
                        totalLentToman = uiState.totalLentToman,
                        totalBorrowedToman = uiState.totalBorrowedToman,
                        totalPendingChequesAmountToman = uiState.totalPendingChequesAmountToman,
                        totalRemainingLoansAmountToman = uiState.totalRemainingLoansAmountToman,
                        currency = uiState.currency,
                    )
                }

                // Tab Specific Content
                when (uiState.selectedTab) {
                    DebtsTab.PEOPLE_AND_DEBTS -> {
                        if (uiState.peopleWithDebts.isEmpty()) {
                            item(key = "empty_debts") {
                                EmptyCommitmentsMessage(
                                    title = "هنوز طرف حسابی ثبت نشده است",
                                    subtitle = "با دکمهٔ «افزودن شخص» افراد مورد نظر را اضافه و بدهی‌ها یا طلب‌های خود را مدیریت کنید.",
                                )
                            }
                        } else {
                            items(
                                items = uiState.peopleWithDebts,
                                key = { it.person.id },
                            ) { personWithDebts ->
                                PersonDebtCard(
                                    personWithDebts = personWithDebts,
                                    currency = uiState.currency,
                                    onAddDebt = { onIntent(DebtsIntent.OpenAddDebtDialog(personWithDebts.person)) },
                                    onSettleDebt = { debt -> onIntent(DebtsIntent.OpenSettleDebtDialog(debt)) },
                                    onDeleteDebt = { debt -> onIntent(DebtsIntent.DeleteDebt(debt.id)) },
                                    onDeletePerson = { onIntent(DebtsIntent.DeletePerson(personWithDebts.person.id)) },
                                )
                            }
                        }
                    }

                    DebtsTab.CHEQUES -> {
                        if (uiState.cheques.isEmpty()) {
                            item(key = "empty_cheques") {
                                EmptyCommitmentsMessage(
                                    title = "هیچ چکی ثبت نشده است",
                                    subtitle = "چک‌های صیادی صادره یا دریافتی خود را ثبت کنید تا تاریخ سررسید آن‌ها را فراموش نکنید.",
                                )
                            }
                        } else {
                            items(
                                items = uiState.cheques,
                                key = { it.id },
                            ) { cheque ->
                                ChequeCard(
                                    cheque = cheque,
                                    currency = uiState.currency,
                                    onStatusChange = { status ->
                                        onIntent(DebtsIntent.UpdateChequeStatus(cheque.id, status))
                                    },
                                    onDelete = { onIntent(DebtsIntent.DeleteCheque(cheque.id)) },
                                )
                            }
                        }
                    }

                    DebtsTab.LOANS -> {
                        if (uiState.loansWithInstallments.isEmpty()) {
                            item(key = "empty_loans") {
                                EmptyCommitmentsMessage(
                                    title = "هیچ وامی ثبت نشده است",
                                    subtitle = "وام‌های بانکی و قرض‌الحسنه را ثبت کنید تا برنامه و جدول سررسید اقساط خودکار ایجاد شود.",
                                )
                            }
                        } else {
                            items(
                                items = uiState.loansWithInstallments,
                                key = { it.loan.id },
                            ) { loanWithInstallments ->
                                LoanCard(
                                    loanWithInstallments = loanWithInstallments,
                                    currency = uiState.currency,
                                    onPayNextInstallment = { inst ->
                                        onIntent(DebtsIntent.PayInstallment(inst.id))
                                    },
                                    onRevertInstallment = { inst ->
                                        onIntent(DebtsIntent.RevertInstallment(inst.id))
                                    },
                                    onDeleteLoan = { onIntent(DebtsIntent.DeleteLoan(loanWithInstallments.loan.id)) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (uiState.isAddPersonDialogOpen) {
        AddPersonDialog(
            onDismiss = { onIntent(DebtsIntent.CloseAddPersonDialog) },
            onConfirm = { name, phone, notes ->
                onIntent(DebtsIntent.SavePerson(name, phone, notes))
            },
        )
    }

    uiState.addDebtForPerson?.let { person ->
        AddDebtDialog(
            person = person,
            onDismiss = { onIntent(DebtsIntent.CloseAddDebtDialog) },
            onConfirm = { amount, direction, dueDate, note ->
                onIntent(DebtsIntent.SaveDebt(person.id, amount, direction, dueDate, note))
            },
        )
    }

    uiState.settleDebtTarget?.let { debt ->
        SettleDebtDialog(
            debt = debt,
            onDismiss = { onIntent(DebtsIntent.CloseSettleDebtDialog) },
            onConfirm = { amount ->
                onIntent(DebtsIntent.SettleDebt(debt.id, amount))
            },
        )
    }

    if (uiState.isAddChequeDialogOpen) {
        AddChequeDialog(
            onDismiss = { onIntent(DebtsIntent.CloseAddChequeDialog) },
            onConfirm = { sayad, bank, amount, due, payee, direction, note ->
                onIntent(DebtsIntent.SaveCheque(sayad, bank, amount, due, payee, direction, note))
            },
        )
    }

    if (uiState.isAddLoanDialogOpen) {
        AddLoanDialog(
            onDismiss = { onIntent(DebtsIntent.CloseAddLoanDialog) },
            onConfirm = { title, total, count, instAmount, firstDue, note ->
                onIntent(DebtsIntent.SaveLoan(title, total, count, instAmount, firstDue, note))
            },
        )
    }
}

@Composable
private fun EmptyCommitmentsMessage(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = HesabiSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Debts Screen - Light")
@Composable
private fun DebtsScreenPreviewLight() {
    HesabiPreview(dark = false) {
        DebtsAndCommitmentsContent(
            uiState = DebtsUiState(
                peopleWithDebts = listOf(
                    PersonWithDebts(
                        person = Person(id = "1", name = "محمد کریمی", phoneNumber = "09123456789"),
                        debts = listOf(
                            Debt(
                                id = "d1",
                                personId = "1",
                                amountToman = 1_500_000L,
                                settledAmountToman = 0L,
                                direction = DebtDirection.LENT,
                                note = "هزینه بلیت سفر",
                            ),
                        ),
                    ),
                ),
                totalLentToman = 1_500_000L,
            ),
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Debts Screen - Dark")
@Composable
private fun DebtsScreenPreviewDark() {
    HesabiPreview(dark = true) {
        DebtsAndCommitmentsContent(
            uiState = DebtsUiState(
                selectedTab = DebtsTab.CHEQUES,
                cheques = listOf(
                    Cheque(
                        id = "c1",
                        sayadNumber = "9876543210987654",
                        bankName = "بانک پاسارگاد",
                        amountToman = 24_000_000L,
                        dueEpochDay = JalaliDate.today().plusMonths(2).toEpochDay(),
                        payeeOrDrawerName = "دفتر مهندسی پارس",
                        direction = ChequeDirection.RECEIVED,
                    ),
                ),
                totalPendingChequesAmountToman = 24_000_000L,
            ),
            onIntent = {},
            onBack = {},
        )
    }
}
