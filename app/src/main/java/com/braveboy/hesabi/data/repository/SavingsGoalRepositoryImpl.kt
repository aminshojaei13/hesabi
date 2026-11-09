package com.braveboy.hesabi.data.repository

import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.data.local.dao.SavingsDepositDao
import com.braveboy.hesabi.data.local.dao.SavingsGoalDao
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.model.SavingsDeposit
import com.braveboy.hesabi.domain.model.SavingsGoal
import com.braveboy.hesabi.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SavingsGoalRepositoryImpl(
    private val savingsGoalDao: SavingsGoalDao,
    private val savingsDepositDao: SavingsDepositDao,
) : SavingsGoalRepository {

    override fun observeAll(): Flow<List<SavingsGoal>> =
        savingsGoalDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeActive(): Flow<List<SavingsGoal>> =
        savingsGoalDao.observeActive().map { list -> list.map { it.toDomain() } }

    override fun observeDeposits(goalId: String): Flow<List<SavingsDeposit>> =
        savingsDepositDao.observeForGoal(goalId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<SavingsGoal> =
        savingsGoalDao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): SavingsGoal? =
        savingsGoalDao.findById(id)?.toDomain()

    override suspend fun save(goal: SavingsGoal) {
        val completed = goal.targetAmountToman > 0 && goal.savedAmountToman >= goal.targetAmountToman
        savingsGoalDao.upsert(goal.copy(isCompleted = completed).toEntity())
    }

    override suspend fun delete(id: String) {
        savingsDepositDao.deleteForGoal(id)
        savingsGoalDao.deleteById(id)
    }

    override suspend fun deposit(goalId: String, amountToman: Long, note: String) =
        applyDelta(goalId, amountToman, withdrawal = false, note = note)

    override suspend fun withdraw(goalId: String, amountToman: Long, note: String) =
        applyDelta(goalId, amountToman, withdrawal = true, note = note)

    private suspend fun applyDelta(goalId: String, amountToman: Long, withdrawal: Boolean, note: String) {
        if (amountToman <= 0) return
        val current = savingsGoalDao.findById(goalId) ?: return
        val delta = if (withdrawal) -amountToman else amountToman
        val newAmount = (current.savedAmountToman + delta).coerceAtLeast(0L)
        val completed = current.targetAmountToman > 0 && newAmount >= current.targetAmountToman
        savingsGoalDao.updateAmount(goalId, newAmount, completed)
        savingsDepositDao.insert(
            SavingsDeposit(
                id = UUID.randomUUID().toString(),
                goalId = goalId,
                amountToman = amountToman,
                isWithdrawal = withdrawal,
                occurredOnEpochDay = JalaliDate.today().toEpochDay(),
                note = note.take(200),
            ).toEntity(),
        )
    }
}
