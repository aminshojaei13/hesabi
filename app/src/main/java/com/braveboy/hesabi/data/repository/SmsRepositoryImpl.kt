package com.braveboy.hesabi.data.repository

import androidx.room.withTransaction
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.core.hash.Hashing
import com.braveboy.hesabi.data.local.HesabiDatabase
import com.braveboy.hesabi.data.local.dao.SenderRuleDao
import com.braveboy.hesabi.data.local.dao.SmsDao
import com.braveboy.hesabi.data.local.dao.TransactionDao
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.data.sms.BankSmsParser
import com.braveboy.hesabi.data.sms.SmsParseResult
import com.braveboy.hesabi.data.sms.SmsTextMasker
import com.braveboy.hesabi.domain.model.SenderRule
import com.braveboy.hesabi.domain.model.SmsEvent
import com.braveboy.hesabi.domain.model.SmsStatus
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.model.TransactionSource
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.domain.repository.IngestOutcome
import com.braveboy.hesabi.domain.repository.SettingsRepository
import com.braveboy.hesabi.domain.repository.SmsAcceptInput
import com.braveboy.hesabi.domain.repository.SmsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SmsRepositoryImpl(
    private val db: HesabiDatabase,
    private val smsDao: SmsDao,
    private val senderRuleDao: SenderRuleDao,
    private val transactionDao: TransactionDao,
    private val settingsRepository: SettingsRepository,
) : SmsRepository {

    override fun observePending(): Flow<List<SmsEvent>> =
        smsDao.observePending().map { list -> list.map { it.toDomain() } }

    override fun observeRecent(): Flow<List<SmsEvent>> =
        smsDao.observeRecent().map { list -> list.map { it.toDomain() } }

    override fun observePendingCount(): Flow<Int> = smsDao.observePendingCount()

    override fun observeSenderRules(): Flow<List<SenderRule>> =
        senderRuleDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun ingest(
        sender: String,
        body: String,
        receivedAtMillis: Long,
        todayEpochDay: Long,
    ): IngestOutcome {
        if (body.isBlank()) return IngestOutcome.Failed
        val settings = settingsRepository.current()
        val ignoredSenders = senderRuleDao.getAll().filter { it.ignored }.map { it.sender }.toSet()
        val parsed = BankSmsParser.parse(
            sender = sender,
            body = body,
            receivedAtMillis = receivedAtMillis,
            todayEpochDay = todayEpochDay,
            amountsAreRial = settings.amountsAreRial,
            ignoredSenders = ignoredSenders,
        )
        return when (parsed) {
            is SmsParseResult.Ignored -> IngestOutcome.Ignored(parsed.reason.name)
            is SmsParseResult.Accepted -> {
                // Same sender + same normalized content inside one 24-hour bucket is a
                // duplicate. The old 60-second bucket both missed retries and merged
                // unrelated duplicates (M19).
                val contentKey = BankSmsParser.normalizeSender(sender) + "|" +
                    SmsTextMasker.normalizeForHash(body) + "|" + (receivedAtMillis / DAY_MILLIS)
                val hash = Hashing.shortSha256(contentKey)
                if (smsDao.findByHash(hash) != null) {
                    return IngestOutcome.Duplicate
                }
                val event = SmsEvent(
                    id = UUID.randomUUID().toString(),
                    sender = sender,
                    // Never persist full card/account numbers (M20).
                    body = SmsTextMasker.mask(body),
                    receivedAtMillis = receivedAtMillis,
                    rawHash = hash,
                    amountToman = parsed.parsed.amountToman,
                    type = parsed.parsed.type,
                    last4 = parsed.parsed.last4,
                    occurredOnEpochDay = parsed.parsed.occurredOnEpochDay,
                    merchant = parsed.parsed.merchant,
                    confidence = parsed.parsed.confidence,
                    status = SmsStatus.PENDING,
                )
                val row = smsDao.insertIgnore(event.toEntity())
                if (row == -1L) IngestOutcome.Duplicate else IngestOutcome.Added(event.id)
            }
        }
    }

    override suspend fun importText(text: String, todayEpochDay: Long): List<IngestOutcome> {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return listOf(IngestOutcome.Failed)
        val receivedAt = System.currentTimeMillis()
        val chunks = cleaned.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }
        val effective = if (chunks.size > 1) chunks else listOf(cleaned)
        return effective.map { chunk ->
            // A shared SMS often arrives as "Sender: body"; keep the sender hint.
            val delimiter = chunk.indexOf(':')
            val sender = if (delimiter in 2..20) chunk.substring(0, delimiter).trim() else "shared"
            val body = if (delimiter in 2..20) chunk.substring(delimiter + 1).trim() else chunk
            ingest(sender, body, receivedAt, todayEpochDay)
        }
    }

    override suspend fun acceptAll(entries: List<SmsAcceptInput>): Int {
        if (entries.isEmpty()) return 0
        return db.withTransaction {
            var accepted = 0
            for (entry in entries) {
                if (entry.amountToman <= 0L) continue
                if (entry.type == TransactionType.TRANSFER) {
                    // A transfer without a distinct destination would make money disappear.
                    if (entry.toAccountId.isNullOrBlank() || entry.toAccountId == entry.accountId) continue
                } else if (entry.categoryId.isNullOrBlank()) {
                    continue
                }
                val ref = "sms:${entry.eventId}"
                if (transactionDao.findByExternalRef(ref) != null) {
                    smsDao.markImported(entry.eventId, ref)
                    continue
                }
                val tx = Transaction(
                    id = UUID.randomUUID().toString(),
                    type = entry.type,
                    amountToman = entry.amountToman,
                    accountId = entry.accountId,
                    toAccountId = entry.toAccountId.takeIf { entry.type == TransactionType.TRANSFER },
                    categoryId = entry.categoryId,
                    occurredOnEpochDay = entry.occurredOnEpochDay,
                    note = entry.note,
                    source = TransactionSource.SMS,
                    externalRef = ref,
                )
                transactionDao.upsert(tx.toEntity())
                smsDao.markImported(entry.eventId, tx.id)
                accepted++
            }
            accepted
        }
    }

    override suspend fun ignore(eventId: String) = smsDao.updateStatus(eventId, SmsStatus.IGNORED.name)

    override suspend fun ignoreSender(sender: String) {
        senderRuleDao.upsert(com.braveboy.hesabi.data.local.entity.SenderRuleEntity(sender, ignored = true))
        smsDao.ignoreAllFromSender(sender)
    }

    override suspend fun allowSender(sender: String) {
        senderRuleDao.deleteBySender(sender)
    }

    override suspend fun delete(eventId: String) = smsDao.deleteById(eventId)

    override suspend fun clearAll() = smsDao.deleteAll()

    fun today(): Long = JalaliDate.today().toEpochDay()

    private companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
