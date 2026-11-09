package com.braveboy.hesabi.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.date.JalaliCalendar
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.domain.model.AppSettings
import com.braveboy.hesabi.domain.model.Cheque
import com.braveboy.hesabi.domain.model.ChequeStatus
import com.braveboy.hesabi.domain.model.Debt
import com.braveboy.hesabi.domain.model.DebtDirection
import com.braveboy.hesabi.domain.model.LoanWithInstallments
import com.braveboy.hesabi.domain.model.Person
import com.braveboy.hesabi.domain.model.RecurringTransaction
import com.braveboy.hesabi.domain.model.SavingsGoal
import com.braveboy.hesabi.domain.repository.ChequeRepository
import com.braveboy.hesabi.domain.repository.DebtRepository
import com.braveboy.hesabi.domain.repository.LoanRepository
import com.braveboy.hesabi.domain.repository.PersonRepository
import com.braveboy.hesabi.domain.repository.RecurringTransactionRepository
import com.braveboy.hesabi.domain.repository.SavingsGoalRepository
import com.braveboy.hesabi.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class DueKind { CHEQUE, INSTALLMENT, DEBT, RECURRING, GOAL, SALARY }

data class DueItem(
    val id: String,
    val kind: DueKind,
    val title: String,
    val subtitle: String,
    val amountToman: Long,
    val dueEpochDay: Long,
)

data class CalendarCell(
    val date: JalaliDate?,
    val itemCount: Int,
)

data class DueCalendarUiState(
    val period: MonthPeriod = MonthPeriod.current(),
    val cells: List<CalendarCell> = emptyList(),
    val items: List<DueItem> = emptyList(),
    val selectedDay: JalaliDate = JalaliDate.today(),
    val loading: Boolean = true,
) {
    val selectedItems: List<DueItem>
        get() = items.filter { it.dueEpochDay == selectedDay.toEpochDay() }
    val totalDueToman: Long get() = items.sumOf { it.amountToman }
}

/**
 * Combines every commitment source into one monthly calendar: cheques, installments,
 * debts, recurring entries, savings-goal dates and the configured payday.
 */
class DueCalendarViewModel(
    chequeRepository: ChequeRepository,
    loanRepository: LoanRepository,
    debtRepository: DebtRepository,
    personRepository: PersonRepository,
    recurringRepository: RecurringTransactionRepository,
    savingsGoalRepository: SavingsGoalRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val period = MutableStateFlow(MonthPeriod.current())
    private val selectedDay = MutableStateFlow(JalaliDate.today())

    private val cheques = chequeRepository.observeAll()
    private val loans = loanRepository.observeAllWithInstallments()
    private val debts = debtRepository.observeAllUnsettled()
    private val people = personRepository.observeAll()
    private val recurring = recurringRepository.observeAll()
    private val goals = savingsGoalRepository.observeActive()

    val state: StateFlow<DueCalendarUiState> = combine(
        cheques,
        loans,
        debts,
        combine(people, recurring, goals) { p, r, g -> Triple(p, r, g) },
        combine(period, selectedDay, settingsRepository.settings) { month, day, settings ->
            Triple(month, day, settings)
        },
    ) { chequeList, loanList, debtList, rest, monthState ->
        val (peopleList, recurringList, goalList) = rest
        val (currentPeriod, selected, settings) = monthState
        val items = buildItems(
            period = currentPeriod,
            cheques = chequeList,
            loans = loanList,
            debts = debtList,
            people = peopleList,
            recurring = recurringList,
            goals = goalList,
            settings = settings,
        )
        DueCalendarUiState(
            period = currentPeriod,
            cells = buildGrid(currentPeriod, items),
            items = items,
            selectedDay = selected,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DueCalendarUiState())

    fun previousMonth() {
        period.value = period.value.previous()
        selectedDay.value = JalaliDate(period.value.year, period.value.month, 1)
    }

    fun nextMonth() {
        period.value = period.value.next()
        selectedDay.value = JalaliDate(period.value.year, period.value.month, 1)
    }

    fun goToCurrentMonth() {
        period.value = MonthPeriod.current()
        selectedDay.value = JalaliDate.today()
    }

    fun selectDay(date: JalaliDate) {
        selectedDay.value = date
    }

    private fun buildItems(
        period: MonthPeriod,
        cheques: List<Cheque>,
        loans: List<LoanWithInstallments>,
        debts: List<Debt>,
        people: List<Person>,
        recurring: List<RecurringTransaction>,
        goals: List<SavingsGoal>,
        settings: AppSettings,
    ): List<DueItem> {
        val personById = people.associateBy { it.id }
        val result = mutableListOf<DueItem>()
        cheques.filter { it.status == ChequeStatus.PENDING }.forEach { cheque ->
            if (period.contains(cheque.dueEpochDay)) {
                result += DueItem(
                    id = "cheque:${cheque.id}",
                    kind = DueKind.CHEQUE,
                    title = "چک ${cheque.bankName}",
                    subtitle = "در وجه ${cheque.payeeOrDrawerName}",
                    amountToman = cheque.amountToman,
                    dueEpochDay = cheque.dueEpochDay,
                )
            }
        }
        loans.forEach { loanWithInstallments ->
            loanWithInstallments.installments.filter { !it.isPaid }.forEach { installment ->
                if (period.contains(installment.dueEpochDay)) {
                    result += DueItem(
                        id = "installment:${installment.id}",
                        kind = DueKind.INSTALLMENT,
                        title = "قسط ${installment.installmentNumber} ${loanWithInstallments.loan.title}",
                        subtitle = "وام",
                        amountToman = installment.amountToman,
                        dueEpochDay = installment.dueEpochDay,
                    )
                }
            }
        }
        debts.filter { !it.isSettled && it.dueEpochDay != null }.forEach { debt ->
            val due = debt.dueEpochDay ?: return@forEach
            if (period.contains(due)) {
                val person = personById[debt.personId]?.name ?: "شخص"
                result += DueItem(
                    id = "debt:${debt.id}",
                    kind = DueKind.DEBT,
                    title = if (debt.direction == DebtDirection.LENT) "طلب از $person" else "بدهی به $person",
                    subtitle = debt.note.ifBlank { "تسویهٔ بدهی" },
                    amountToman = debt.remainingAmountToman,
                    dueEpochDay = due,
                )
            }
        }
        recurring.filter { it.isActive }.forEach { item ->
            if (period.contains(item.nextOccurrenceEpochDay)) {
                result += DueItem(
                    id = "recurring:${item.id}",
                    kind = DueKind.RECURRING,
                    title = item.title,
                    subtitle = "تراکنش دوره‌ای ${item.frequency.labelFa}",
                    amountToman = item.amountToman,
                    dueEpochDay = item.nextOccurrenceEpochDay,
                )
            }
        }
        goals.forEach { goal ->
            val target = goal.targetDateEpochDay ?: return@forEach
            if (period.contains(target)) {
                result += DueItem(
                    id = "goal:${goal.id}",
                    kind = DueKind.GOAL,
                    title = "هدف ${goal.title}",
                    subtitle = "مهلت رسیدن به هدف",
                    amountToman = goal.remainingToman,
                    dueEpochDay = target,
                )
            }
        }
        if (settings.monthlySalaryToman > 0) {
            val payday = settings.paydayDayOfMonth.coerceIn(1, period.days)
            result += DueItem(
                id = "salary:${period.key}",
                kind = DueKind.SALARY,
                title = "روز حقوق",
                subtitle = "واریز حقوق ماهانه",
                amountToman = settings.monthlySalaryToman,
                dueEpochDay = JalaliDate(period.year, period.month, payday).toEpochDay(),
            )
        }
        return result.sortedBy { it.dueEpochDay }
    }

    private fun buildGrid(period: MonthPeriod, items: List<DueItem>): List<CalendarCell> {
        val leading = JalaliCalendar.monthGridOffset(period)
        val countByDay = items.groupingBy { it.dueEpochDay }.eachCount()
        val cells = mutableListOf<CalendarCell>()
        repeat(leading) { cells += CalendarCell(null, 0) }
        for (day in 1..period.days) {
            val date = JalaliDate(period.year, period.month, day)
            cells += CalendarCell(date, countByDay[date.toEpochDay()] ?: 0)
        }
        while (cells.size % 7 != 0) cells += CalendarCell(null, 0)
        return cells
    }
}
