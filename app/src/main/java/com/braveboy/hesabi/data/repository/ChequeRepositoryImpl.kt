package com.braveboy.hesabi.data.repository

import com.braveboy.hesabi.data.local.dao.ChequeDao
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.model.Cheque
import com.braveboy.hesabi.domain.model.ChequeStatus
import com.braveboy.hesabi.domain.repository.ChequeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChequeRepositoryImpl(
    private val chequeDao: ChequeDao,
) : ChequeRepository {

    override fun observeAll(): Flow<List<Cheque>> =
        chequeDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observePending(): Flow<List<Cheque>> =
        chequeDao.observePending().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<Cheque> =
        chequeDao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): Cheque? =
        chequeDao.findById(id)?.toDomain()

    override suspend fun save(cheque: Cheque) =
        chequeDao.upsert(cheque.toEntity())

    override suspend fun delete(id: String) =
        chequeDao.deleteById(id)

    override suspend fun updateStatus(id: String, status: ChequeStatus) =
        chequeDao.updateStatus(id, status.name)
}
