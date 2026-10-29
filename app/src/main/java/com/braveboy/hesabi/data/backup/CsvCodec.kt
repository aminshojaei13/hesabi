package com.braveboy.hesabi.data.backup

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionType

/**
 * Spreadsheet-friendly export. The header is English so Google Sheets / Excel
 * auto-detect columns, while values stay human-readable Persian.
 */
object CsvCodec {

    const val BOM = "\uFEFF"

    fun export(
        transactions: List<Transaction>,
        accounts: List<Account>,
        categories: List<Category>,
    ): String {
        val accountNames = accounts.associate { it.id to it.name }
        val categoryNames = categories.associate { it.id to it.name }
        val builder = StringBuilder()
        builder.append(BOM)
        builder.append("date,date_epoch,type,amount_toman,account,to_account,category,note,source\n")
        transactions
            .sortedWith(compareByDescending<Transaction> { it.occurredOnEpochDay }.thenByDescending { it.createdAtMillis })
            .forEach { tx ->
                builder.append(
                    listOf(
                        JalaliDate.fromEpochDay(tx.occurredOnEpochDay).formatShort(),
                        tx.occurredOnEpochDay.toString(),
                        typeLabel(tx.type),
                        tx.amountToman.toString(),
                        accountNames[tx.accountId] ?: tx.accountId,
                        tx.toAccountId?.let { accountNames[it] ?: it } ?: "",
                        tx.categoryId?.let { categoryNames[it] ?: it } ?: "",
                        tx.note,
                        tx.source.name,
                    ).joinToString(",") { escape(it) },
                ).append('\n')
            }
        return builder.toString()
    }

    fun typeLabel(type: TransactionType): String = when (type) {
        TransactionType.EXPENSE -> "expense"
        TransactionType.INCOME -> "income"
        TransactionType.TRANSFER -> "transfer"
    }

    fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
