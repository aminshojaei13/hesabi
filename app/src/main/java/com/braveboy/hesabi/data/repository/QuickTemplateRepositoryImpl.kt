package com.braveboy.hesabi.data.repository

import com.braveboy.hesabi.data.local.dao.QuickTemplateDao
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.model.QuickTemplate
import com.braveboy.hesabi.domain.repository.QuickTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class QuickTemplateRepositoryImpl(
    private val dao: QuickTemplateDao,
) : QuickTemplateRepository {

    override fun observeAll(): Flow<List<QuickTemplate>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<QuickTemplate> = dao.getAll().map { it.toDomain() }

    override suspend fun save(template: QuickTemplate) = dao.upsert(template.toEntity())

    override suspend fun delete(id: String) = dao.deleteById(id)

    override suspend fun markUsed(id: String) = dao.incrementUsage(id)
}
