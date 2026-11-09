package com.braveboy.hesabi.data.repository

import com.braveboy.hesabi.data.local.dao.AssetDao
import com.braveboy.hesabi.data.local.dao.AssetPriceSnapshotDao
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.model.Asset
import com.braveboy.hesabi.domain.model.AssetPriceSnapshot
import com.braveboy.hesabi.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class AssetRepositoryImpl(
    private val assetDao: AssetDao,
    private val snapshotDao: AssetPriceSnapshotDao,
) : AssetRepository {

    override fun observeAll(): Flow<List<Asset>> =
        assetDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSnapshots(): Flow<List<AssetPriceSnapshot>> =
        snapshotDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<Asset> = assetDao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): Asset? = assetDao.findById(id)?.toDomain()

    override suspend fun save(asset: Asset, recordSnapshot: Boolean) {
        assetDao.upsert(asset.toEntity())
        if (recordSnapshot && asset.currentPriceTomanPerUnit > 0) {
            recordSnapshot(asset.id, asset.currentPriceTomanPerUnit, System.currentTimeMillis())
        }
    }

    override suspend fun updatePrice(
        assetId: String,
        priceTomanPerUnit: Long,
        source: String,
        atMillis: Long,
    ) {
        val current = assetDao.findById(assetId) ?: return
        val updated = current.copy(
            currentPriceTomanPerUnit = priceTomanPerUnit.coerceAtLeast(0L),
            priceUpdatedAtMillis = atMillis,
            priceSourceLabel = source,
        )
        assetDao.upsert(updated)
        recordSnapshot(assetId, updated.currentPriceTomanPerUnit, atMillis)
    }

    override suspend fun delete(id: String) {
        snapshotDao.deleteForAsset(id)
        assetDao.deleteById(id)
    }

    private suspend fun recordSnapshot(assetId: String, price: Long, atMillis: Long) {
        snapshotDao.upsertAll(
            listOf(
                AssetPriceSnapshot(
                    id = UUID.randomUUID().toString(),
                    assetId = assetId,
                    priceTomanPerUnit = price,
                    recordedAtMillis = atMillis,
                ).toEntity(),
            ),
        )
    }
}
