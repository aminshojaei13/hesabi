package com.braveboy.hesabi.data.repository

import com.braveboy.hesabi.data.local.dao.RecurringTransactionDao
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.model.RecurringTransaction
import com.braveboy.hesabi.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecurringTransactionRepositoryImpl(
    private val dao: RecurringTransactionDao,
) : RecurringTransactionRepository {

    override fun observeAll(): Flow<List<RecurringTransaction>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeActive(): Flow<List<RecurringTransaction>> =
        dao.observeActive().map { list -> list.map { it.toDomain() } }

    override fun observeDue(todayEpochDay: Long): Flow<List<RecurringTransaction>> =
        dao.observeActive().map { list ->
            list.filter { it.nextOccurrenceEpochDay <= todayEpochDay }.map { it.toDomain() }
        }

    override suspend fun getAll(): List<RecurringTransaction> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getDue(todayEpochDay: Long): List<RecurringTransaction> =
        dao.getDue(todayEpochDay).map { it.toDomain() }

    override suspend fun getById(id: String): RecurringTransaction? =
        dao.findById(id)?.toDomain()

    override suspend fun save(recurring: RecurringTransaction) {
        dao.upsert(recurring.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun updateOccurrence(id: String, appliedEpochDay: Long, nextOccurrenceEpochDay: Long) {
        dao.updateOccurrence(id, appliedEpochDay, nextOccurrenceEpochDay)
    }

    override suspend fun toggleActive(id: String, isActive: Boolean) {
        dao.updateActive(id, isActive)
    }
}
