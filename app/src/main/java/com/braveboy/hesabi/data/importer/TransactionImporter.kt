package com.braveboy.hesabi.data.importer

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.date.MonthPeriod
import com.braveboy.hesabi.core.hash.Hashing
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.AccountKind
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionSource
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.domain.repository.AccountRepository
import com.braveboy.hesabi.domain.repository.CategoryRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import java.time.LocalDate
import java.util.UUID

/** One column index per logical field. `null` means "not in the file". */
data class ImportMapping(
    val date: Int? = null,
    val amount: Int? = null,
    val type: Int? = null,
    val category: Int? = null,
    val account: Int? = null,
    val note: Int? = null,
    val income: Int? = null,
    val expense: Int? = null,
) {
    val hasAmount: Boolean get() = amount != null || income != null || expense != null
}

data class ImportPreview(
    val table: CsvTableParser.Table,
    val mapping: ImportMapping,
    val sampleRows: List<List<String>>,
)

data class ImportIssue(val rowNumber: Int, val message: String)

data class ImportSummary(
    val imported: Int,
    val skippedDuplicates: Int,
    val failed: Int,
    val issues: List<ImportIssue>,
    val createdCategories: Int,
    val createdAccounts: Int,
    val fromDate: Long?,
    val toDate: Long?,
) {
    val totalRows: Int get() = imported + skippedDuplicates + failed
    val summaryText: String
        get() = "$imported تراکنش ثبت شد، $skippedDuplicates مورد تکراری رد شد و $failed سطر خطا داشت."
}

/**
 * CSV/XLSX migration wizard engine. The user sees a preview and can correct the
 * column mapping before anything is written; every row that fails produces an exact
 * row number instead of silently disappearing (acceptance criterion of scenario 6).
 */
class TransactionImporter(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
) {

    fun preview(table: CsvTableParser.Table): ImportPreview =
        ImportPreview(table = table, mapping = guessMapping(table.headers), sampleRows = table.rows.take(5))

    fun guessMapping(headers: List<String>): ImportMapping {
        fun find(vararg aliases: String): Int? = headers.indexOfFirst { header ->
            val normalized = normalizeHeader(header)
            aliases.any { alias -> normalized == alias || normalized.contains(alias) }
        }.takeIf { it >= 0 }

        return ImportMapping(
            date = find("تاریخ", "date", "زمان", "datetime"),
            amount = find("مبلغ", "amount", "value", "مقدار", "price"),
            type = find("نوع", "type", "kind"),
            category = find("دسته", "category", "گروه", "group"),
            account = find("حساب", "account", "کارت", "wallet", "منبع"),
            note = find("توضیح", "شرح", "یادداشت", "بابت", "note", "description", "memo", "details"),
            income = find("درآمد", "income", "credit", "واریز", "بستانکار"),
            expense = find("هزینه", "expense", "debit", "برداشت", "بدهکار"),
        )
    }

    suspend fun import(
        table: CsvTableParser.Table,
        mapping: ImportMapping,
        createMissingCategories: Boolean = true,
        createMissingAccounts: Boolean = false,
        defaultAccountId: String? = null,
        today: JalaliDate = JalaliDate.today(),
    ): ImportSummary {
        val issues = mutableListOf<ImportIssue>()
        if (!mapping.hasAmount) {
            return ImportSummary(0, 0, 0, listOf(ImportIssue(1, "ستون مبلغ در فایل پیدا نشد.")), 0, 0, null, null)
        }

        val accounts = accountRepository.getAll().toMutableList()
        val categories = categoryRepository.getAll().toMutableList()
        val defaultAccount = accounts.firstOrNull { it.id == defaultAccountId }
            ?: accounts.firstOrNull()
            ?: Account(
                id = UUID.randomUUID().toString(),
                name = "حساب اصلی",
                kind = AccountKind.CASH,
                colorArgb = 0xFF12AFC0,
                openingBalanceToman = 0,
            ).also {
                accountRepository.save(it)
                accounts += it
            }

        var imported = 0
        var skipped = 0
        var failed = 0
        var createdCategories = 0
        var createdAccounts = 0
        var minDate: Long? = null
        var maxDate: Long? = null

        table.rows.forEachIndexed { index, row ->
            val rowNumber = index + 2 // 1-based + header
            fun cell(column: Int?): String = column?.let { row.getOrNull(it)?.trim().orEmpty() }.orEmpty()

            val rawDate = cell(mapping.date)
            val date = parseDate(rawDate, today)
            if (rawDate.isNotBlank() && date == null) {
                failed++
                issues += ImportIssue(rowNumber, "تاریخ «$rawDate» قابل خواندن نیست.")
                return@forEachIndexed
            }
            val occurredOn = date ?: today

            val amountIncome = parseAmount(cell(mapping.income))
            val amountExpense = parseAmount(cell(mapping.expense))
            val rawAmount = cell(mapping.amount)
            val singleAmount = parseAmount(rawAmount)

            val typeFromColumn = parseType(cell(mapping.type))
            val (amountToman, type) = when {
                amountIncome != null && amountIncome > 0 -> amountIncome to TransactionType.INCOME
                amountExpense != null && amountExpense != 0L -> kotlin.math.abs(amountExpense) to TransactionType.EXPENSE
                singleAmount != null && singleAmount != 0L -> {
                    val signed = if (rawAmount.contains("-") || rawAmount.contains("−")) -kotlin.math.abs(singleAmount)
                    else singleAmount
                    val resolvedType = typeFromColumn ?: if (signed < 0) TransactionType.EXPENSE else TransactionType.EXPENSE
                    kotlin.math.abs(singleAmount) to resolvedType
                }
                else -> {
                    failed++
                    issues += ImportIssue(rowNumber, "مبلغ در این سطر خوانده نشد.")
                    return@forEachIndexed
                }
            }
            if (amountToman <= 0) {
                failed++
                issues += ImportIssue(rowNumber, "مبلغ باید بیشتر از صفر باشد.")
                return@forEachIndexed
            }
            if (type == TransactionType.TRANSFER) {
                failed++
                issues += ImportIssue(rowNumber, "انتقال نیاز به حساب مقصد دارد و از فایل خوانده نشد.")
                return@forEachIndexed
            }

            val rawAccount = cell(mapping.account)
            val account = resolveAccount(rawAccount, accounts) ?: run {
                if (!createMissingAccounts || rawAccount.isBlank()) {
                    defaultAccount
                } else {
                    val created = Account(
                        id = UUID.randomUUID().toString(),
                        name = rawAccount.take(40),
                        kind = AccountKind.BANK,
                        colorArgb = 0xFF6E7B80,
                        openingBalanceToman = 0,
                        sortOrder = accounts.size,
                    )
                    accountRepository.save(created)
                    accounts += created
                    createdAccounts++
                    created
                }
            }

            val rawCategory = cell(mapping.category)
            val categoryKind = if (type == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
            val category = resolveCategory(rawCategory, categories, categoryKind) ?: run {
                if (!createMissingCategories || rawCategory.isBlank()) {
                    null
                } else {
                    val created = Category(
                        id = UUID.randomUUID().toString(),
                        name = rawCategory.take(40),
                        kind = categoryKind,
                        iconKey = "more_horiz",
                        colorArgb = 0xFF8A9A9E,
                        sortOrder = categories.count { it.kind == categoryKind },
                    )
                    categoryRepository.save(created)
                    categories += created
                    createdCategories++
                    created
                }
            }
            if (type != TransactionType.TRANSFER && category == null) {
                failed++
                issues += ImportIssue(rowNumber, "دسته خالی است و ساخت دستهٔ جدید مجاز نیست.")
                return@forEachIndexed
            }

            val note = cell(mapping.note).ifBlank { rawCategory }
            val ref = "import:" + Hashing.shortSha256(
                listOf(
                    occurredOn.toStorage(),
                    amountToman.toString(),
                    type.name,
                    account.id,
                    category?.id.orEmpty(),
                    note,
                ).joinToString("|"),
            )
            if (transactionRepository.findByExternalRef(ref) != null) {
                skipped++
                return@forEachIndexed
            }

            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                type = type,
                amountToman = amountToman,
                accountId = account.id,
                categoryId = category?.id,
                occurredOnEpochDay = occurredOn.toEpochDay(),
                note = note.take(200),
                source = TransactionSource.IMPORT,
                externalRef = ref,
            )
            transactionRepository.save(transaction)
            imported++
            minDate = minDate?.let { minOf(it, occurredOn.toEpochDay()) } ?: occurredOn.toEpochDay()
            maxDate = maxDate?.let { maxOf(it, occurredOn.toEpochDay()) } ?: occurredOn.toEpochDay()
        }

        return ImportSummary(
            imported = imported,
            skippedDuplicates = skipped,
            failed = failed,
            issues = issues.take(50),
            createdCategories = createdCategories,
            createdAccounts = createdAccounts,
            fromDate = minDate,
            toDate = maxDate,
        )
    }

    /** True when every imported row falls inside the reported range. */
    fun importedPeriod(summary: ImportSummary): MonthPeriod? {
        val from = summary.fromDate?.let { JalaliDate.fromEpochDay(it) } ?: return null
        return MonthPeriod.of(from)
    }

    private fun resolveAccount(raw: String, accounts: List<Account>): Account? {
        if (raw.isBlank()) return null
        val normalized = normalizeHeader(raw)
        return accounts.firstOrNull { normalizeHeader(it.name) == normalized }
            ?: accounts.firstOrNull {
                normalizeHeader(it.name).contains(normalized) || normalized.contains(normalizeHeader(it.name))
            }
    }

    private fun resolveCategory(raw: String, categories: List<Category>, kind: CategoryKind): Category? {
        if (raw.isBlank()) return null
        val normalized = normalizeHeader(raw)
        return categories.filter { it.kind == kind }.let { candidates ->
            candidates.firstOrNull { normalizeHeader(it.name) == normalized }
                ?: candidates.firstOrNull {
                    normalizeHeader(it.name).contains(normalized) || normalized.contains(normalizeHeader(it.name))
                }
        }
    }

    private fun parseAmount(raw: String): Long? {
        if (raw.isBlank()) return null
        val negative = raw.contains("-") || raw.contains("−")
        val parsed = Money.parseToman(raw, allowNegative = true) ?: return null
        val value = kotlin.math.abs(parsed)
        return if (negative) -value else value
    }

    private fun parseType(raw: String): TransactionType? {
        if (raw.isBlank()) return null
        val normalized = Digits.toAscii(raw).lowercase().trim()
        return when {
            normalized.contains("درآمد") || normalized.contains("income") ||
                normalized.contains("credit") || normalized.contains("واریز") ||
                normalized.contains("بستانکار") -> TransactionType.INCOME
            normalized.contains("انتقال") || normalized.contains("transfer") -> TransactionType.TRANSFER
            normalized.contains("هزینه") || normalized.contains("expense") ||
                normalized.contains("debit") || normalized.contains("برداشت") ||
                normalized.contains("بدهکار") -> TransactionType.EXPENSE
            else -> null
        }
    }

    private fun parseDate(raw: String, today: JalaliDate): JalaliDate? {
        if (raw.isBlank()) return null
        val normalized = Digits.toAscii(raw).trim()
        val match = Regex("""(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})""").find(normalized)
        if (match != null) {
            val year = match.groupValues[1].toIntOrNull() ?: return null
            val month = match.groupValues[2].toIntOrNull() ?: return null
            val day = match.groupValues[3].toIntOrNull() ?: return null
            return runCatching {
                if (year in 1800..2200) {
                    JalaliDate.fromGregorian(LocalDate.of(year, month, day))
                } else {
                    JalaliDate(year, month, day)
                }
            }.getOrNull()
        }
        // ۱۲ آبان ۱۴۰۴ / 12 Aban 1404
        val monthNames = com.braveboy.hesabi.core.date.JalaliCalendar.MONTHS
        val named = Regex("""(\d{1,2})\s*([^\d\s]{3,})\s*(\d{2,4})?""").find(normalized) ?: return null
        val day = named.groupValues[1].toIntOrNull() ?: return null
        val monthIndex = monthNames.indexOfFirst { it == named.groupValues[2] }
        if (monthIndex < 0) return null
        val year = named.groupValues[3].toIntOrNull() ?: today.year
        return runCatching { JalaliDate(year, monthIndex + 1, day) }.getOrNull()
    }

    private fun normalizeHeader(raw: String): String = Digits.toAscii(raw)
        .lowercase()
        .replace("\u200c", " ")
        .replace("ي", "ی")
        .replace("ك", "ک")
        .replace("_", " ")
        .trim()
}
