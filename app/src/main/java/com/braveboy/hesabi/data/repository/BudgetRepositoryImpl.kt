package com.braveboy.hesabi.data.repository

import com.braveboy.hesabi.data.local.dao.BudgetDao
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.model.Budget
import com.braveboy.hesabi.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao,
) : BudgetRepository {

    override fun observeForPeriod(periodKey: String): Flow<List<Budget>> =
        budgetDao.observeForPeriod(periodKey).map { list -> list.map { it.toDomain() } }

    override suspend fun getForPeriod(periodKey: String): List<Budget> =
        budgetDao.getForPeriod(periodKey).map { it.toDomain() }

    override suspend fun save(budget: Budget) = budgetDao.upsert(budget.toEntity())

    override suspend fun delete(id: String) = budgetDao.deleteById(id)
}
