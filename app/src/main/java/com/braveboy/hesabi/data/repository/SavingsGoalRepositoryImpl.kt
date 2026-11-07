package com.braveboy.hesabi.data.repository

import com.braveboy.hesabi.data.local.dao.SavingsGoalDao
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.model.SavingsGoal
import com.braveboy.hesabi.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavingsGoalRepositoryImpl(
    private val savingsGoalDao: SavingsGoalDao,
) : SavingsGoalRepository {

    override fun observeAll(): Flow<List<SavingsGoal>> =
        savingsGoalDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeActive(): Flow<List<SavingsGoal>> =
        savingsGoalDao.observeActive().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<SavingsGoal> =
        savingsGoalDao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): SavingsGoal? =
        savingsGoalDao.findById(id)?.toDomain()

    override suspend fun save(goal: SavingsGoal) {
        val completed = goal.targetAmountToman > 0 && goal.savedAmountToman >= goal.targetAmountToman
        savingsGoalDao.upsert(goal.copy(isCompleted = completed).toEntity())
    }

    override suspend fun delete(id: String) {
        savingsGoalDao.deleteById(id)
    }

    override suspend fun deposit(goalId: String, amountToman: Long) {
        if (amountToman <= 0) return
        val current = savingsGoalDao.findById(goalId) ?: return
        val newAmount = current.savedAmountToman + amountToman
        val completed = current.targetAmountToman > 0 && newAmount >= current.targetAmountToman
        savingsGoalDao.updateAmount(goalId, newAmount, completed)
    }

    override suspend fun withdraw(goalId: String, amountToman: Long) {
        if (amountToman <= 0) return
        val current = savingsGoalDao.findById(goalId) ?: return
        val newAmount = (current.savedAmountToman - amountToman).coerceAtLeast(0L)
        val completed = current.targetAmountToman > 0 && newAmount >= current.targetAmountToman
        savingsGoalDao.updateAmount(goalId, newAmount, completed)
    }
}
