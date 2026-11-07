package com.braveboy.hesabi.data.repository

import com.braveboy.hesabi.data.local.dao.DebtDao
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.model.Debt
import com.braveboy.hesabi.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DebtRepositoryImpl(
    private val debtDao: DebtDao,
) : DebtRepository {

    override fun observeForPerson(personId: String): Flow<List<Debt>> =
        debtDao.observeForPerson(personId).map { list -> list.map { it.toDomain() } }

    override fun observeAllUnsettled(): Flow<List<Debt>> =
        debtDao.observeAllUnsettled().map { list -> list.map { it.toDomain() } }

    override fun observeAll(): Flow<List<Debt>> =
        debtDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Debt? =
        debtDao.findById(id)?.toDomain()

    override suspend fun save(debt: Debt) =
        debtDao.upsert(debt.toEntity())

    override suspend fun delete(id: String) =
        debtDao.deleteById(id)

    override suspend fun settle(debtId: String, amountToman: Long) {
        val existing = debtDao.findById(debtId) ?: return
        val newSettled = (existing.settledAmountToman + amountToman).coerceAtMost(existing.amountToman)
        val isSettled = newSettled >= existing.amountToman
        debtDao.updateSettlement(debtId, newSettled, isSettled)
    }
}
