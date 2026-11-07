package com.braveboy.hesabi.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Currency
import com.braveboy.hesabi.domain.model.Cheque
import com.braveboy.hesabi.domain.model.ChequeDirection
import com.braveboy.hesabi.domain.model.ChequeStatus
import com.braveboy.hesabi.domain.model.Debt
import com.braveboy.hesabi.domain.model.DebtDirection
import com.braveboy.hesabi.domain.model.Installment
import com.braveboy.hesabi.domain.model.Loan
import com.braveboy.hesabi.domain.model.LoanWithInstallments
import com.braveboy.hesabi.domain.model.Person
import com.braveboy.hesabi.domain.model.PersonWithDebts
import com.braveboy.hesabi.domain.repository.ChequeRepository
import com.braveboy.hesabi.domain.repository.DebtRepository
import com.braveboy.hesabi.domain.repository.LoanRepository
import com.braveboy.hesabi.domain.repository.PersonRepository
import com.braveboy.hesabi.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class DebtsTab(val title: String) {
    PEOPLE_AND_DEBTS("اشخاص و بدهی/طلب"),
    CHEQUES("چک‌های صیادی"),
    LOANS("وام و اقساط"),
}

data class DebtsUiState(
    val selectedTab: DebtsTab = DebtsTab.PEOPLE_AND_DEBTS,
    val peopleWithDebts: List<PersonWithDebts> = emptyList(),
    val cheques: List<Cheque> = emptyList(),
    val loansWithInstallments: List<LoanWithInstallments> = emptyList(),
    val currency: Currency = Currency.TOMAN,
    val totalLentToman: Long = 0L,
    val totalBorrowedToman: Long = 0L,
    val totalPendingChequesAmountToman: Long = 0L,
    val totalRemainingLoansAmountToman: Long = 0L,
    // Dialog / Sheet states
    val isAddPersonDialogOpen: Boolean = false,
    val addDebtForPerson: Person? = null,
    val settleDebtTarget: Debt? = null,
    val isAddChequeDialogOpen: Boolean = false,
    val isAddLoanDialogOpen: Boolean = false,
)

sealed interface DebtsIntent {
    data class SelectTab(val tab: DebtsTab) : DebtsIntent

    // Person & Debts
    data object OpenAddPersonDialog : DebtsIntent
    data object CloseAddPersonDialog : DebtsIntent
    data class SavePerson(val name: String, val phoneNumber: String, val notes: String) : DebtsIntent
    data class DeletePerson(val personId: String) : DebtsIntent

    data class OpenAddDebtDialog(val person: Person) : DebtsIntent
    data object CloseAddDebtDialog : DebtsIntent
    data class SaveDebt(
        val personId: String,
        val amountToman: Long,
        val direction: DebtDirection,
        val dueEpochDay: Long?,
        val note: String,
    ) : DebtsIntent
    data class DeleteDebt(val debtId: String) : DebtsIntent

    data class OpenSettleDebtDialog(val debt: Debt) : DebtsIntent
    data object CloseSettleDebtDialog : DebtsIntent
    data class SettleDebt(val debtId: String, val amountToman: Long) : DebtsIntent

    // Cheques
    data object OpenAddChequeDialog : DebtsIntent
    data object CloseAddChequeDialog : DebtsIntent
    data class SaveCheque(
        val sayadNumber: String,
        val bankName: String,
        val amountToman: Long,
        val dueEpochDay: Long,
        val payeeOrDrawerName: String,
        val direction: ChequeDirection,
        val note: String,
    ) : DebtsIntent
    data class UpdateChequeStatus(val chequeId: String, val status: ChequeStatus) : DebtsIntent
    data class DeleteCheque(val chequeId: String) : DebtsIntent

    // Loans
    data object OpenAddLoanDialog : DebtsIntent
    data object CloseAddLoanDialog : DebtsIntent
    data class SaveLoan(
        val title: String,
        val totalAmountToman: Long,
        val installmentCount: Int,
        val installmentAmountToman: Long,
        val firstDueEpochDay: Long,
        val note: String,
    ) : DebtsIntent
    data class PayInstallment(val installmentId: String) : DebtsIntent
    data class RevertInstallment(val installmentId: String) : DebtsIntent
    data class DeleteLoan(val loanId: String) : DebtsIntent
}

class DebtsAndCommitmentsViewModel(
    private val personRepository: PersonRepository,
    private val debtRepository: DebtRepository,
    private val chequeRepository: ChequeRepository,
    private val loanRepository: LoanRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebtsUiState())

    val uiState: StateFlow<DebtsUiState> = combine(
        _uiState,
        personRepository.observeWithDebts(),
        chequeRepository.observeAll(),
        loanRepository.observeAllWithInstallments(),
        settingsRepository.settings,
    ) { currentUi, people, cheques, loans, settings ->
        val totalLent = people.flatMap { it.debts }
            .filter { !it.isSettled && it.direction == DebtDirection.LENT }
            .sumOf { it.remainingAmountToman }

        val totalBorrowed = people.flatMap { it.debts }
            .filter { !it.isSettled && it.direction == DebtDirection.BORROWED }
            .sumOf { it.remainingAmountToman }

        val totalPendingCheques = cheques
            .filter { it.status == ChequeStatus.PENDING }
            .sumOf { it.amountToman }

        val totalRemainingLoans = loans.sumOf { it.remainingAmountToman }

        currentUi.copy(
            peopleWithDebts = people,
            cheques = cheques,
            loansWithInstallments = loans,
            currency = settings.currency,
            totalLentToman = totalLent,
            totalBorrowedToman = totalBorrowed,
            totalPendingChequesAmountToman = totalPendingCheques,
            totalRemainingLoansAmountToman = totalRemainingLoans,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DebtsUiState(),
    )

    fun onIntent(intent: DebtsIntent) {
        when (intent) {
            is DebtsIntent.SelectTab -> _uiState.update { it.copy(selectedTab = intent.tab) }

            DebtsIntent.OpenAddPersonDialog -> _uiState.update { it.copy(isAddPersonDialogOpen = true) }
            DebtsIntent.CloseAddPersonDialog -> _uiState.update { it.copy(isAddPersonDialogOpen = false) }
            is DebtsIntent.SavePerson -> savePerson(intent.name, intent.phoneNumber, intent.notes)
            is DebtsIntent.DeletePerson -> deletePerson(intent.personId)

            is DebtsIntent.OpenAddDebtDialog -> _uiState.update { it.copy(addDebtForPerson = intent.person) }
            DebtsIntent.CloseAddDebtDialog -> _uiState.update { it.copy(addDebtForPerson = null) }
            is DebtsIntent.SaveDebt -> saveDebt(
                intent.personId,
                intent.amountToman,
                intent.direction,
                intent.dueEpochDay,
                intent.note,
            )
            is DebtsIntent.DeleteDebt -> deleteDebt(intent.debtId)

            is DebtsIntent.OpenSettleDebtDialog -> _uiState.update { it.copy(settleDebtTarget = intent.debt) }
            DebtsIntent.CloseSettleDebtDialog -> _uiState.update { it.copy(settleDebtTarget = null) }
            is DebtsIntent.SettleDebt -> settleDebt(intent.debtId, intent.amountToman)

            DebtsIntent.OpenAddChequeDialog -> _uiState.update { it.copy(isAddChequeDialogOpen = true) }
            DebtsIntent.CloseAddChequeDialog -> _uiState.update { it.copy(isAddChequeDialogOpen = false) }
            is DebtsIntent.SaveCheque -> saveCheque(
                intent.sayadNumber,
                intent.bankName,
                intent.amountToman,
                intent.dueEpochDay,
                intent.payeeOrDrawerName,
                intent.direction,
                intent.note,
            )
            is DebtsIntent.UpdateChequeStatus -> updateChequeStatus(intent.chequeId, intent.status)
            is DebtsIntent.DeleteCheque -> deleteCheque(intent.chequeId)

            DebtsIntent.OpenAddLoanDialog -> _uiState.update { it.copy(isAddLoanDialogOpen = true) }
            DebtsIntent.CloseAddLoanDialog -> _uiState.update { it.copy(isAddLoanDialogOpen = false) }
            is DebtsIntent.SaveLoan -> saveLoan(
                intent.title,
                intent.totalAmountToman,
                intent.installmentCount,
                intent.installmentAmountToman,
                intent.firstDueEpochDay,
                intent.note,
            )
            is DebtsIntent.PayInstallment -> payInstallment(intent.installmentId)
            is DebtsIntent.RevertInstallment -> revertInstallment(intent.installmentId)
            is DebtsIntent.DeleteLoan -> deleteLoan(intent.loanId)
        }
    }

    private fun savePerson(name: String, phone: String, notes: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            personRepository.save(
                Person(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    phoneNumber = phone.trim(),
                    notes = notes.trim(),
                ),
            )
            _uiState.update { it.copy(isAddPersonDialogOpen = false) }
        }
    }

    private fun deletePerson(personId: String) {
        viewModelScope.launch {
            personRepository.delete(personId)
        }
    }

    private fun saveDebt(
        personId: String,
        amountToman: Long,
        direction: DebtDirection,
        dueEpochDay: Long?,
        note: String,
    ) {
        if (amountToman <= 0) return
        viewModelScope.launch {
            debtRepository.save(
                Debt(
                    id = UUID.randomUUID().toString(),
                    personId = personId,
                    amountToman = amountToman,
                    settledAmountToman = 0L,
                    direction = direction,
                    dueEpochDay = dueEpochDay,
                    note = note.trim(),
                    isSettled = false,
                ),
            )
            _uiState.update { it.copy(addDebtForPerson = null) }
        }
    }

    private fun deleteDebt(debtId: String) {
        viewModelScope.launch {
            debtRepository.delete(debtId)
        }
    }

    private fun settleDebt(debtId: String, amountToman: Long) {
        if (amountToman <= 0) return
        viewModelScope.launch {
            debtRepository.settle(debtId, amountToman)
            _uiState.update { it.copy(settleDebtTarget = null) }
        }
    }

    private fun saveCheque(
        sayadNumber: String,
        bankName: String,
        amountToman: Long,
        dueEpochDay: Long,
        payeeOrDrawerName: String,
        direction: ChequeDirection,
        note: String,
    ) {
        if (bankName.isBlank() || amountToman <= 0) return
        viewModelScope.launch {
            chequeRepository.save(
                Cheque(
                    id = UUID.randomUUID().toString(),
                    sayadNumber = sayadNumber.trim(),
                    bankName = bankName.trim(),
                    amountToman = amountToman,
                    dueEpochDay = dueEpochDay,
                    payeeOrDrawerName = payeeOrDrawerName.trim(),
                    direction = direction,
                    status = ChequeStatus.PENDING,
                    note = note.trim(),
                ),
            )
            _uiState.update { it.copy(isAddChequeDialogOpen = false) }
        }
    }

    private fun updateChequeStatus(chequeId: String, status: ChequeStatus) {
        viewModelScope.launch {
            chequeRepository.updateStatus(chequeId, status)
        }
    }

    private fun deleteCheque(chequeId: String) {
        viewModelScope.launch {
            chequeRepository.delete(chequeId)
        }
    }

    private fun saveLoan(
        title: String,
        totalAmountToman: Long,
        installmentCount: Int,
        installmentAmountToman: Long,
        firstDueEpochDay: Long,
        note: String,
    ) {
        if (title.isBlank() || installmentCount <= 0 || installmentAmountToman <= 0) return
        viewModelScope.launch {
            val loanId = UUID.randomUUID().toString()
            val loan = Loan(
                id = loanId,
                title = title.trim(),
                totalAmountToman = totalAmountToman,
                installmentCount = installmentCount,
                installmentAmountToman = installmentAmountToman,
                firstDueEpochDay = firstDueEpochDay,
                note = note.trim(),
            )
            val firstJalali = JalaliDate.fromEpochDay(firstDueEpochDay)
            val installments = (1..installmentCount).map { index ->
                val dueJalali = firstJalali.plusMonths(index - 1)
                Installment(
                    id = UUID.randomUUID().toString(),
                    loanId = loanId,
                    installmentNumber = index,
                    amountToman = installmentAmountToman,
                    dueEpochDay = dueJalali.toEpochDay(),
                    isPaid = false,
                )
            }
            loanRepository.saveLoan(loan, installments)
            _uiState.update { it.copy(isAddLoanDialogOpen = false) }
        }
    }

    private fun payInstallment(installmentId: String) {
        viewModelScope.launch {
            loanRepository.payInstallment(installmentId, JalaliDate.today().toEpochDay())
        }
    }

    private fun revertInstallment(installmentId: String) {
        viewModelScope.launch {
            loanRepository.revertInstallmentPayment(installmentId)
        }
    }

    private fun deleteLoan(loanId: String) {
        viewModelScope.launch {
            loanRepository.deleteLoan(loanId)
        }
    }
}
