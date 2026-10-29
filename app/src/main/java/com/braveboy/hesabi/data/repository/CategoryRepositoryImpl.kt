package com.braveboy.hesabi.data.repository

import com.braveboy.hesabi.data.local.dao.CategoryDao
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
) : CategoryRepository {

    override fun observeAll(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeByKind(kind: CategoryKind): Flow<List<Category>> =
        categoryDao.observeByKind(kind.name).map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<Category> = categoryDao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): Category? = categoryDao.findById(id)?.toDomain()

    override suspend fun save(category: Category) = categoryDao.upsert(category.toEntity())

    override suspend fun delete(id: String) = categoryDao.deleteById(id)
}
