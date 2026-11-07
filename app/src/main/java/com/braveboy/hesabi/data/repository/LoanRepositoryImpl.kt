package com.braveboy.hesabi.data.repository

import androidx.room.withTransaction
import com.braveboy.hesabi.data.local.HesabiDatabase
import com.braveboy.hesabi.data.local.dao.InstallmentDao
import com.braveboy.hesabi.data.local.dao.LoanDao
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.model.Installment
import com.braveboy.hesabi.domain.model.Loan
import com.braveboy.hesabi.domain.model.LoanWithInstallments
import com.braveboy.hesabi.domain.repository.LoanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class LoanRepositoryImpl(
    private val db: HesabiDatabase,
    private val loanDao: LoanDao,
    private val installmentDao: InstallmentDao,
) : LoanRepository {

    override fun observeAllWithInstallments(): Flow<List<LoanWithInstallments>> =
        combine(loanDao.observeAll(), installmentDao.observeAll()) { loans, installments ->
            val byLoan = installments.groupBy { it.loanId }
            loans.map { loanEntity ->
                val loan = loanEntity.toDomain()
                val loanInstallments = byLoan[loan.id].orEmpty().map { it.toDomain() }
                LoanWithInstallments(loan = loan, installments = loanInstallments)
            }
        }

    override suspend fun getAllWithInstallments(): List<LoanWithInstallments> {
        val loans = loanDao.getAll()
        val installments = installmentDao.getAll()
        val byLoan = installments.groupBy { it.loanId }
        return loans.map { loanEntity ->
            val loan = loanEntity.toDomain()
            val loanInstallments = byLoan[loan.id].orEmpty().map { it.toDomain() }
            LoanWithInstallments(loan = loan, installments = loanInstallments)
        }
    }

    override suspend fun getById(id: String): LoanWithInstallments? {
        val loanEntity = loanDao.findById(id) ?: return null
        val installments = installmentDao.getForLoan(id).map { it.toDomain() }
        return LoanWithInstallments(loan = loanEntity.toDomain(), installments = installments)
    }

    override suspend fun saveLoan(loan: Loan, installments: List<Installment>) {
        db.withTransaction {
            loanDao.upsert(loan.toEntity())
            if (installments.isNotEmpty()) {
                installmentDao.upsertAll(installments.map { it.toEntity() })
            }
        }
    }

    override suspend fun deleteLoan(id: String) {
        db.withTransaction {
            installmentDao.deleteByLoanId(id)
            loanDao.deleteById(id)
        }
    }

    override suspend fun payInstallment(installmentId: String, paidAtEpochDay: Long) {
        db.withTransaction {
            val inst = installmentDao.findById(installmentId) ?: return@withTransaction
            installmentDao.updatePaid(installmentId, isPaid = true, paidAtEpochDay = paidAtEpochDay)

            val allForLoan = installmentDao.getForLoan(inst.loanId)
            val allPaid = allForLoan.all { it.id == installmentId || it.isPaid }
            if (allPaid) {
                loanDao.updateCompleted(inst.loanId, true)
            }
        }
    }

    override suspend fun revertInstallmentPayment(installmentId: String) {
        db.withTransaction {
            val inst = installmentDao.findById(installmentId) ?: return@withTransaction
            installmentDao.updatePaid(installmentId, isPaid = false, paidAtEpochDay = null)
            loanDao.updateCompleted(inst.loanId, false)
        }
    }
}
