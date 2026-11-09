package com.braveboy.hesabi.domain.export

import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.core.export.PdfReportWriter
import com.braveboy.hesabi.core.export.XlsxWriter
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.logic.AnnualReport
import com.braveboy.hesabi.domain.logic.PortfolioSummary
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.Budget
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.MonthReport
import com.braveboy.hesabi.domain.model.Transaction

data class ExportData(
    val period: MonthPeriod,
    val report: MonthReport,
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val accounts: List<Account>,
    val budgets: List<Budget> = emptyList(),
    val annual: AnnualReport? = null,
    val portfolio: PortfolioSummary? = null,
)

/**
 * Turns one month of data (plus optional annual/portfolio sections) into a printable
 * PDF or a multi-sheet XLSX. Both formats share the same numbers as the report screen.
 */
object ReportExportBuilder {

    fun pdf(data: ExportData, includeTransactions: Boolean = true): PdfReportWriter.Spec {
        val categoryNameById = data.categories.associateBy { it.id }
        val accountNameById = data.accounts.associateBy { it.id }
        val sections = mutableListOf<PdfReportWriter.Section>()

        sections += PdfReportWriter.Section(
            title = "خلاصهٔ ماه",
            rows = listOf(
                PdfReportWriter.Row(listOf("شرح", "مبلغ"), bold = true),
                PdfReportWriter.Row(
                    listOf("هزینه", visibleMoney(data.report.totalExpenseToman, withUnit = false)),
                ),
                PdfReportWriter.Row(
                    listOf("درآمد", visibleMoney(data.report.totalIncomeToman, withUnit = false)),
                ),
                PdfReportWriter.Row(
                    listOf("مانده", visibleMoney(data.report.netToman, signed = true, withUnit = false)),
                ),
                PdfReportWriter.Row(listOf("تعداد تراکنش", data.report.transactionCount.toString())),
            ),
        )

        if (data.report.slices.isNotEmpty()) {
            sections += PdfReportWriter.Section(
                title = "هزینه بر اساس دسته",
                rows = listOf(PdfReportWriter.Row(listOf("دسته", "مبلغ", "سهم"), bold = true)) +
                    data.report.slices.map { slice ->
                        PdfReportWriter.Row(
                            listOf(
                                slice.name,
                                visibleMoney(slice.amountToman, withUnit = false),
                                "${(slice.share * 100).toInt()}٪",
                            ),
                        )
                    },
            )
        }

        if (includeTransactions && data.transactions.isNotEmpty()) {
            sections += PdfReportWriter.Section(
                title = "تراکنش‌ها",
                rows = listOf(
                    PdfReportWriter.Row(listOf("تاریخ", "دسته", "حساب", "مبلغ"), bold = true),
                ) + data.transactions.sortedBy { it.occurredOnEpochDay }.map { transaction ->
                    PdfReportWriter.Row(
                        listOf(
                            transaction.occurredOn.formatShort(),
                            transaction.categoryId?.let { categoryNameById[it]?.name } ?: "—",
                            accountNameById[transaction.accountId]?.name ?: "—",
                            visibleMoney(transaction.amountToman, withUnit = false),
                        ),
                    )
                },
            )
        }

        data.annual?.let { annual ->
            sections += PdfReportWriter.Section(
                title = "حساب سال ${annual.year}",
                paragraphs = annual.insights,
                rows = listOf(PdfReportWriter.Row(listOf("ماه", "درآمد", "هزینه"), bold = true)) +
                    annual.months.map { month ->
                        PdfReportWriter.Row(
                            listOf(
                                month.label,
                                visibleMoney(month.incomeToman, withUnit = false),
                                visibleMoney(month.expenseToman, withUnit = false),
                            ),
                        )
                    },
            )
        }

        data.portfolio?.let { portfolio ->
            sections += PdfReportWriter.Section(
                title = "دارایی غیرنقدی",
                paragraphs = listOf(
                    "ارزش فعلی: " + visibleMoney(portfolio.totalValueToman),
                    "سود/زیان: " + visibleMoney(portfolio.profitToman, signed = true),
                ),
                rows = listOf(PdfReportWriter.Row(listOf("نوع دارایی", "ارزش"), bold = true)) +
                    portfolio.allocations.map { allocation ->
                        PdfReportWriter.Row(
                            listOf(
                                allocation.kind.labelFa,
                                visibleMoney(allocation.valueToman, withUnit = false),
                            ),
                        )
                    },
            )
        }

        return PdfReportWriter.Spec(
            title = "گزارش مالی ${data.period.label}",
            subtitle = "ساخته‌شده از دادهٔ روی همین دستگاه",
            metaLines = listOf("حسابی — نسخهٔ محلی", "تعداد تراکنش: ${data.transactions.size}"),
            sections = sections,
        )
    }

    fun xlsx(data: ExportData): List<XlsxWriter.Sheet> {
        val categoryNameById = data.categories.associateBy { it.id }
        val accountNameById = data.accounts.associateBy { it.id }

        val summary = listOf(
            listOf("شرح", "مقدار"),
            listOf("دوره", data.period.label),
            listOf("هزینه (تومان)", data.report.totalExpenseToman),
            listOf("درآمد (تومان)", data.report.totalIncomeToman),
            listOf("مانده (تومان)", data.report.netToman),
            listOf("تعداد تراکنش", data.report.transactionCount),
        )

        val transactions = listOf(
            listOf(
                "تاریخ", "نوع", "دسته", "حساب", "مبلغ (تومان)", "توضیح", "منبع",
            ),
        ) + data.transactions.sortedBy { it.occurredOnEpochDay }.map { transaction ->
            listOf(
                transaction.occurredOn.formatShort(),
                transaction.type.label,
                transaction.categoryId?.let { categoryNameById[it]?.name } ?: "—",
                accountNameById[transaction.accountId]?.name ?: "—",
                transaction.amountToman,
                transaction.note,
                transaction.source.name,
            )
        }

        val categories = listOf(listOf("دسته", "مبلغ (تومان)", "سهم درصد")) +
            data.report.slices.map {
                listOf(it.name, it.amountToman, (it.share * 100).toInt())
            }

        val budgets = listOf(listOf("نوع بودجه", "سقف (تومان)", "مصرف (تومان)")) +
            data.budgets.map { budget ->
                listOf(
                    if (budget.categoryId.isBlank()) "کل ماه"
                    else categoryNameById[budget.categoryId]?.name ?: budget.categoryId,
                    budget.limitToman,
                    0L,
                )
            }

        val annual = data.annual?.let { report ->
            listOf(listOf("ماه", "درآمد (تومان)", "هزینه (تومان)", "مانده (تومان)")) +
                report.months.map {
                    listOf(it.label, it.incomeToman, it.expenseToman, it.netToman)
                }
        }

        val sheets = mutableListOf(
            XlsxWriter.Sheet("خلاصه", summary),
            XlsxWriter.Sheet("تراکنش‌ها", transactions),
            XlsxWriter.Sheet("دسته‌ها", categories),
            XlsxWriter.Sheet("بودجه", budgets),
        )
        if (annual != null) sheets += XlsxWriter.Sheet("سال", annual)
        data.portfolio?.let { portfolio ->
            sheets += XlsxWriter.Sheet(
                "دارایی",
                listOf(listOf("نوع", "ارزش (تومان)")) +
                    portfolio.allocations.map { listOf(it.kind.labelFa, it.valueToman) },
            )
        }
        return sheets
    }
}


/** Annual-specific exports ("حساب سال من"). */
object AnnualExportBuilder {

    fun pdf(
        annual: AnnualReport,
        yearTransactions: List<Transaction>,
        categories: List<Category>,
        portfolio: PortfolioSummary? = null,
    ): PdfReportWriter.Spec {
        val slices = com.braveboy.hesabi.domain.logic.ReportCalculator
            .totalExpenseByCategory(yearTransactions, categories)
        val sections = mutableListOf<PdfReportWriter.Section>()
        sections += PdfReportWriter.Section(
            title = "خلاصهٔ سال",
            rows = listOf(
                PdfReportWriter.Row(listOf("شرح", "مبلغ"), bold = true),
                PdfReportWriter.Row(listOf("درآمد", visibleMoney(annual.totalIncomeToman, withUnit = false))),
                PdfReportWriter.Row(listOf("هزینه", visibleMoney(annual.totalExpenseToman, withUnit = false))),
                PdfReportWriter.Row(
                    listOf("مانده", visibleMoney(annual.totalNetToman, signed = true, withUnit = false)),
                ),
                PdfReportWriter.Row(listOf("نرخ پس‌انداز", "${annual.savingsRatePercent}٪")),
                PdfReportWriter.Row(listOf("تعداد تراکنش", annual.transactionCount.toString())),
            ),
        )
        sections += PdfReportWriter.Section(
            title = "ماه‌به‌ماه",
            rows = listOf(PdfReportWriter.Row(listOf("ماه", "درآمد", "هزینه", "مانده"), bold = true)) +
                annual.months.map { month ->
                    PdfReportWriter.Row(
                        listOf(
                            month.label,
                            visibleMoney(month.incomeToman, withUnit = false),
                            visibleMoney(month.expenseToman, withUnit = false),
                            visibleMoney(month.netToman, signed = true, withUnit = false),
                        ),
                    )
                },
        )
        if (slices.isNotEmpty()) {
            sections += PdfReportWriter.Section(
                title = "هزینه بر اساس دسته",
                rows = listOf(PdfReportWriter.Row(listOf("دسته", "مبلغ", "سهم"), bold = true)) +
                    slices.map {
                        PdfReportWriter.Row(
                            listOf(
                                it.name,
                                visibleMoney(it.amountToman, withUnit = false),
                                "${(it.share * 100).toInt()}٪",
                            ),
                        )
                    },
            )
        }
        sections += PdfReportWriter.Section(title = "نکته‌های سال", paragraphs = annual.insights)
        portfolio?.let {
            sections += PdfReportWriter.Section(
                title = "دارایی غیرنقدی",
                rows = listOf(PdfReportWriter.Row(listOf("نوع", "ارزش"), bold = true)) +
                    it.allocations.map { allocation ->
                        PdfReportWriter.Row(
                            listOf(allocation.kind.labelFa, visibleMoney(allocation.valueToman, withUnit = false)),
                        )
                    },
            )
        }
        return PdfReportWriter.Spec(
            title = "حساب سال ${annual.year}",
            subtitle = "گزارش سالانهٔ حسابی",
            metaLines = listOf("ساخته‌شده از دادهٔ روی همین دستگاه"),
            sections = sections,
        )
    }

    fun xlsx(
        annual: AnnualReport,
        yearTransactions: List<Transaction>,
        categories: List<Category>,
        portfolio: PortfolioSummary? = null,
    ): List<XlsxWriter.Sheet> {
        val slices = com.braveboy.hesabi.domain.logic.ReportCalculator
            .totalExpenseByCategory(yearTransactions, categories)
        val summary = listOf(
            listOf("شرح", "مقدار"),
            listOf("سال", annual.year),
            listOf("درآمد (تومان)", annual.totalIncomeToman),
            listOf("هزینه (تومان)", annual.totalExpenseToman),
            listOf("مانده (تومان)", annual.totalNetToman),
            listOf("نرخ پس‌انداز (درصد)", annual.savingsRatePercent),
        )
        val months = listOf(listOf("ماه", "درآمد (تومان)", "هزینه (تومان)", "مانده (تومان)")) +
            annual.months.map { listOf(it.label, it.incomeToman, it.expenseToman, it.netToman) }
        val categoryRows = listOf(listOf("دسته", "مبلغ (تومان)", "سهم درصد")) +
            slices.map { listOf(it.name, it.amountToman, (it.share * 100).toInt()) }
        val transactionRows = listOf(listOf("تاریخ", "نوع", "دسته", "مبلغ (تومان)", "توضیح")) +
            yearTransactions.sortedBy { it.occurredOnEpochDay }.map {
                val categoryName = categories.firstOrNull { category -> category.id == it.categoryId }?.name ?: "—"
                listOf(it.occurredOn.formatShort(), it.type.label, categoryName, it.amountToman, it.note)
            }
        val sheets = mutableListOf(
            XlsxWriter.Sheet("خلاصه", summary),
            XlsxWriter.Sheet("ماه‌ها", months),
            XlsxWriter.Sheet("دسته‌ها", categoryRows),
            XlsxWriter.Sheet("تراکنش‌ها", transactionRows),
        )
        portfolio?.let {
            sheets += XlsxWriter.Sheet(
                "دارایی",
                listOf(listOf("نوع", "ارزش (تومان)")) +
                    it.allocations.map { allocation -> listOf(allocation.kind.labelFa, allocation.valueToman) },
            )
        }
        return sheets
    }
}


/** Export must show real numbers even when the UI masks them. */
private fun visibleMoney(
    amountToman: Long,
    withUnit: Boolean = true,
    signed: Boolean = false,
): String = Money.format(amountToman, withUnit = withUnit, signed = signed, respectPrivacy = false)

private fun visibleCompact(amountToman: Long): String =
    Money.compact(amountToman, respectPrivacy = false)
