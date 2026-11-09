package com.braveboy.hesabi.data.repository

import com.braveboy.hesabi.data.local.dao.TransactionDao
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.model.Transaction
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
) : TransactionRepository {

    override fun observeAll(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeBetween(start: Long, endExclusive: Long): Flow<List<Transaction>> =
        transactionDao.observeBetween(start, endExclusive).map { list -> list.map { it.toDomain() } }

    override fun observeRecent(limit: Int): Flow<List<Transaction>> =
        transactionDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override fun observeTrash(): Flow<List<Transaction>> =
        transactionDao.observeTrash().map { list -> list.map { it.toDomain() } }

    override suspend fun getBetween(start: Long, endExclusive: Long): List<Transaction> =
        transactionDao.getBetween(start, endExclusive).map { it.toDomain() }

    override suspend fun getAll(): List<Transaction> = transactionDao.getAll().map { it.toDomain() }

    override suspend fun getAllIncludingDeleted(): List<Transaction> =
        transactionDao.getAllIncludingDeleted().map { it.toDomain() }

    override suspend fun getById(id: String): Transaction? = transactionDao.findById(id)?.toDomain()

    override suspend fun findByExternalRef(ref: String): Transaction? =
        transactionDao.findByExternalRef(ref)?.toDomain()

    override suspend fun findByLinkedEntity(type: String, id: String): List<Transaction> =
        transactionDao.findByLinkedEntity(type, id).map { it.toDomain() }

    override suspend fun save(transaction: Transaction) = transactionDao.upsert(transaction.toEntity())

    override suspend fun saveAll(transactions: List<Transaction>) =
        transactionDao.upsertAll(transactions.map { it.toEntity() })

    override suspend fun delete(id: String) =
        transactionDao.softDelete(id, System.currentTimeMillis())

    override suspend fun restore(id: String) = transactionDao.restore(id)

    override suspend fun deletePermanently(id: String) = transactionDao.deleteById(id)

    override suspend fun purgeTrash(olderThanMillis: Long): Int =
        transactionDao.purgeExpired(olderThanMillis)

    override suspend fun countForCategory(categoryId: String): Int = transactionDao.countForCategory(categoryId)

    override suspend fun countForAccount(accountId: String): Int = transactionDao.countForAccount(accountId)
}
