package com.braveboy.hesabi.ui.debts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.money.Currency
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.money.Money
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
import com.braveboy.hesabi.ui.components.GlassCard
import com.braveboy.hesabi.ui.preview.HesabiPreview
import com.braveboy.hesabi.ui.theme.HesabiRadius
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.LocalHesabiLumina
import com.braveboy.hesabi.ui.theme.LocalHesabiSemantic

@Composable
fun CommitmentsSummaryHeader(
    tab: DebtsTab,
    totalLentToman: Long,
    totalBorrowedToman: Long,
    totalPendingChequesAmountToman: Long,
    totalRemainingLoansAmountToman: Long,
    currency: Currency,
    modifier: Modifier = Modifier,
) {
    val semantic = LocalHesabiSemantic.current

    GlassCard(modifier = modifier.fillMaxWidth()) {
        when (tab) {
            DebtsTab.PEOPLE_AND_DEBTS -> {
                val netBalance = totalLentToman - totalBorrowedToman
                val netColor = if (netBalance >= 0) semantic.income else semantic.expense
                val netLabel = if (netBalance >= 0) "تراز مثبت (طلبکار)" else "تراز منفی (بدهکار)"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "مجموع طلب‌ها",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = Money.format(totalLentToman, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = semantic.income,
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "مجموع بدهی‌ها",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = Money.format(totalBorrowedToman, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = semantic.expense,
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = HesabiSpacing.md),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "وضعیت کل: $netLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = Money.format(kotlin.math.abs(netBalance), currency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = netColor,
                    )
                }
            }

            DebtsTab.CHEQUES -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "مجموع تعهدات چک‌های در جریان",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = Money.format(totalPendingChequesAmountToman, currency),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = LocalHesabiLumina.current.glowPrimary,
                    )
                }
            }

            DebtsTab.LOANS -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "کل باقیمانده اقساط و وام‌ها",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = Money.format(totalRemainingLoansAmountToman, currency),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = semantic.expense,
                    )
                }
            }
        }
    }
}

@Composable
fun PersonDebtCard(
    personWithDebts: PersonWithDebts,
    currency: Currency,
    onAddDebt: () -> Unit,
    onSettleDebt: (Debt) -> Unit,
    onDeleteDebt: (Debt) -> Unit,
    onDeletePerson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val person = personWithDebts.person
    val debts = personWithDebts.debts
    val semantic = LocalHesabiSemantic.current
    var expanded by remember { mutableStateOf(false) }

    val netBalance = personWithDebts.netBalanceToman
    val netColor = if (netBalance >= 0) semantic.income else semantic.expense
    val netStatusText = when {
        netBalance > 0 -> "طلبکاریم: ${Money.format(netBalance, currency)}"
        netBalance < 0 -> "بدهکاریم: ${Money.format(kotlin.math.abs(netBalance), currency)}"
        else -> "بی‌حساب"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HesabiRadius.card),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(HesabiSpacing.md)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Column {
                        Text(
                            text = person.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (person.phoneNumber.isNotBlank()) {
                            Text(
                                text = Digits.toPersian(person.phoneNumber),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = netColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = netStatusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = netColor,
                    )
                }
            }

            Spacer(Modifier.height(HesabiSpacing.sm))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.xs)) {
                    TextButton(onClick = onAddDebt) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ثبت بدهی/طلب")
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDeletePerson) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "حذف شخص",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    if (debts.isNotEmpty()) {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = if (expanded) "بستن" else "باز کردن",
                            )
                        }
                    }
                }
            }

            // Debts List
            AnimatedVisibility(visible = expanded || debts.size == 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = HesabiSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                ) {
                    debts.forEach { debt ->
                        DebtItemRow(
                            debt = debt,
                            currency = currency,
                            onSettle = { onSettleDebt(debt) },
                            onDelete = { onDeleteDebt(debt) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DebtItemRow(
    debt: Debt,
    currency: Currency,
    onSettle: () -> Unit,
    onDelete: () -> Unit,
) {
    val semantic = LocalHesabiSemantic.current
    val isLent = debt.direction == DebtDirection.LENT
    val directionColor = if (isLent) semantic.income else semantic.expense
    val directionLabel = if (isLent) "طلبکارم" else "بدهکارم"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HesabiRadius.input),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HesabiSpacing.sm, vertical = HesabiSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = directionColor.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = directionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = directionColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    Text(
                        text = Money.format(debt.remainingAmountToman, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    if (debt.isSettled) {
                        Text(
                            text = "(تسویه شد)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (debt.note.isNotBlank() || debt.dueEpochDay != null) {
                    Spacer(Modifier.height(2.dp))
                    val dateText = debt.dueEpochDay?.let { "موعد: ${JalaliDate.fromEpochDay(it).formatShort()}" } ?: ""
                    val noteText = debt.note
                    val subText = listOf(dateText, noteText).filter { it.isNotBlank() }.joinToString(" • ")
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!debt.isSettled) {
                    TextButton(onClick = onSettle) {
                        Text("تسویه", style = MaterialTheme.typography.labelMedium)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ChequeCard(
    cheque: Cheque,
    currency: Currency,
    onStatusChange: (ChequeStatus) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = LocalHesabiSemantic.current
    var showMenu by remember { mutableStateOf(false) }

    val isIssued = cheque.direction == ChequeDirection.ISSUED
    val directionLabel = if (isIssued) "چک صادره (پرداختی)" else "چک دریافتی"
    val directionColor = if (isIssued) semantic.expense else semantic.income

    val statusColor = when (cheque.status) {
        ChequeStatus.PENDING -> MaterialTheme.colorScheme.primary
        ChequeStatus.CLEARED -> semantic.income
        ChequeStatus.BOUNCED -> semantic.expense
    }

    val dueJalali = JalaliDate.fromEpochDay(cheque.dueEpochDay)
    val today = JalaliDate.today()
    val isOverdue = cheque.status == ChequeStatus.PENDING && dueJalali.toEpochDay() < today.toEpochDay()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HesabiRadius.card),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isOverdue) semantic.expense.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(HesabiSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = directionColor.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = directionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = directionColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    Text(
                        text = cheque.bankName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Box {
                    Surface(
                        modifier = Modifier.clickable { showMenu = true },
                        shape = RoundedCornerShape(12.dp),
                        color = statusColor.copy(alpha = 0.12f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = cheque.status.labelFa,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                            )
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        ChequeStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.labelFa) },
                                onClick = {
                                    onStatusChange(status)
                                    showMenu = false
                                },
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("حذف چک", color = semantic.expense) },
                            onClick = {
                                onDelete()
                                showMenu = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(HesabiSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = if (isIssued) "در وجه: ${cheque.payeeOrDrawerName}" else "از: ${cheque.payeeOrDrawerName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (cheque.sayadNumber.isNotBlank()) {
                        Text(
                            text = "شناسه صیادی: ${Digits.toPersian(cheque.sayadNumber)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                    }
                }

                Text(
                    text = Money.format(cheque.amountToman, currency),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(HesabiSpacing.sm))

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
                        Icons.Rounded.Event,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isOverdue) semantic.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "سررسید: ${dueJalali.formatShort()}" + if (isOverdue) " (سررسید گذشته)" else "",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                        color = if (isOverdue) semantic.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (cheque.note.isNotBlank()) {
                    Text(
                        text = cheque.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun LoanCard(
    loanWithInstallments: LoanWithInstallments,
    currency: Currency,
    onPayNextInstallment: (Installment) -> Unit,
    onRevertInstallment: (Installment) -> Unit,
    onDeleteLoan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loan = loanWithInstallments.loan
    val installments = loanWithInstallments.installments
    val semantic = LocalHesabiSemantic.current
    var expanded by remember { mutableStateOf(false) }

    val progress = if (loan.installmentCount > 0) {
        loanWithInstallments.paidCount.toFloat() / loan.installmentCount
    } else 0f

    val nextPending = loanWithInstallments.nextPendingInstallment

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HesabiRadius.card),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(HesabiSpacing.md)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Column {
                        Text(
                            text = loan.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "کل وام: ${Money.format(loan.totalAmountToman, currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconButton(onClick = onDeleteLoan) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "حذف وام",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(HesabiSpacing.sm))

            // Progress Bar & Labels
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "پیشرفت پرداخت (${Digits.toPersian(loanWithInstallments.paidCount.toString())} از ${Digits.toPersian(loan.installmentCount.toString())})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${Digits.toPersian((progress * 100).toInt().toString())}٪",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = semantic.income,
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = semantic.income,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )
            }

            Spacer(Modifier.height(HesabiSpacing.md))

            // Next Installment Highlight
            if (nextPending != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HesabiRadius.input),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(HesabiSpacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "قسط بعدی (#${Digits.toPersian(nextPending.installmentNumber.toString())})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "موعد: ${JalaliDate.fromEpochDay(nextPending.dueEpochDay).formatShort()}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        Button(
                            onClick = { onPayNextInstallment(nextPending) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                        ) {
                            Text(
                                text = "پرداخت (${Money.format(nextPending.amountToman, currency)})",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(HesabiSpacing.xs))

            // Expand Schedule Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (expanded) "بستن جدول اقساط" else "مشاهده جدول اقساط",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }

            // Installment Schedule
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = HesabiSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    installments.forEach { installment ->
                        InstallmentScheduleRow(
                            installment = installment,
                            currency = currency,
                            onTogglePaid = {
                                if (installment.isPaid) onRevertInstallment(installment)
                                else onPayNextInstallment(installment)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InstallmentScheduleRow(
    installment: Installment,
    currency: Currency,
    onTogglePaid: () -> Unit,
) {
    val semantic = LocalHesabiSemantic.current
    val dueJalali = JalaliDate.fromEpochDay(installment.dueEpochDay)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = if (installment.isPaid) semantic.income.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HesabiSpacing.sm, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "قسط ${Digits.toPersian(installment.installmentNumber.toString())}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = dueJalali.formatShort(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = Money.format(installment.amountToman, currency),
                    style = MaterialTheme.typography.labelMedium,
                )

                IconButton(
                    onClick = onTogglePaid,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = if (installment.isPaid) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = if (installment.isPaid) "پرداخت شده" else "لغو پرداخت",
                        tint = if (installment.isPaid) semantic.income else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Preview(name = "Cheque Card - Dark")
@Composable
private fun ChequeCardPreviewDark() {
    HesabiPreview(dark = true) {
        ChequeCard(
            cheque = Cheque(
                id = "1",
                sayadNumber = "1234567890123456",
                bankName = "بانک ملت",
                amountToman = 15_000_000L,
                dueEpochDay = JalaliDate.today().plusMonths(1).toEpochDay(),
                payeeOrDrawerName = "فروشگاه لوازم خانگی",
                direction = ChequeDirection.ISSUED,
                status = ChequeStatus.PENDING,
            ),
            currency = Currency.TOMAN,
            onStatusChange = {},
            onDelete = {},
        )
    }
}

@Preview(name = "Person Debt Card - Light")
@Composable
private fun PersonDebtCardPreviewLight() {
    HesabiPreview(dark = false) {
        PersonDebtCard(
            personWithDebts = PersonWithDebts(
                person = Person(id = "1", name = "علی احمدی", phoneNumber = "09121112233"),
                debts = listOf(
                    Debt(
                        id = "d1",
                        personId = "1",
                        amountToman = 2_500_000L,
                        settledAmountToman = 500_000L,
                        direction = DebtDirection.LENT,
                        note = "قرض برای خرید گوشی",
                    ),
                ),
            ),
            currency = Currency.TOMAN,
            onAddDebt = {},
            onSettleDebt = {},
            onDeleteDebt = {},
            onDeletePerson = {},
        )
    }
}
