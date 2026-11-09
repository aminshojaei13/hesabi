package com.braveboy.hesabi.data.repository

import com.braveboy.hesabi.data.local.dao.AccountDao
import com.braveboy.hesabi.data.local.dao.TransactionDao
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.AccountWithBalance
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class AccountRepositoryImpl(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
) : AccountRepository {

    override fun observeAll(): Flow<List<Account>> =
        accountDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeWithBalances(): Flow<List<AccountWithBalance>> =
        combine(accountDao.observeAll(), transactionDao.observeAll()) { accounts, transactions ->
            // One pass over transactions, then one lookup per account: O(n + m) instead
            // of scanning every transaction once per account (M9).
            val deltas = HashMap<String, Long>(accounts.size * 2)
            for (tx in transactions) {
                when (tx.type) {
                    TransactionType.INCOME.name ->
                        deltas[tx.accountId] = (deltas[tx.accountId] ?: 0L) + tx.amountToman
                    TransactionType.EXPENSE.name ->
                        deltas[tx.accountId] = (deltas[tx.accountId] ?: 0L) - tx.amountToman
                    TransactionType.TRANSFER.name -> {
                        deltas[tx.accountId] = (deltas[tx.accountId] ?: 0L) - tx.amountToman
                        val destination = tx.toAccountId
                        if (destination != null) {
                            deltas[destination] = (deltas[destination] ?: 0L) + tx.amountToman
                        }
                    }
                }
            }
            accounts.map { entity ->
                val account = entity.toDomain()
                AccountWithBalance(
                    account = account,
                    balanceToman = account.openingBalanceToman + (deltas[account.id] ?: 0L),
                )
            }
        }

    override suspend fun getAll(): List<Account> = accountDao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): Account? = accountDao.findById(id)?.toDomain()

    override suspend fun save(account: Account) = accountDao.upsert(account.toEntity())

    override suspend fun delete(id: String) = accountDao.deleteById(id)
}
