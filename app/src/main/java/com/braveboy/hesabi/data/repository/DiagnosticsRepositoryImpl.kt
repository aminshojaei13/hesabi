package com.braveboy.hesabi.data.repository

import com.braveboy.hesabi.data.local.dao.AppEventDao
import com.braveboy.hesabi.data.local.toDomain
import com.braveboy.hesabi.data.local.toEntity
import com.braveboy.hesabi.domain.model.AppEvent
import com.braveboy.hesabi.domain.repository.DiagnosticsRepository
import java.util.UUID

/**
 * Writes anonymous, local-only product events. Callers must never pass amounts,
 * notes, phone numbers or SMS bodies; [sanitize] also strips long digit runs.
 */
class DiagnosticsRepositoryImpl(
    private val dao: AppEventDao,
) : DiagnosticsRepository {

    override suspend fun record(type: String, name: String, metadata: String) {
        dao.insert(
            AppEvent(
                id = UUID.randomUUID().toString(),
                type = type.take(32),
                name = name.take(64),
                metadata = sanitize(metadata),
            ).toEntity(),
        )
        dao.purgeOlderThan(System.currentTimeMillis() - RETENTION_MILLIS)
    }

    override suspend fun recent(): List<AppEvent> = dao.recent().map { it.toDomain() }

    override suspend fun clear() = dao.deleteAll()

    private fun sanitize(metadata: String): String {
        val withoutDigits = metadata.replace(Regex("""\d{3,}"""), "…")
        return withoutDigits.take(200)
    }

    private companion object {
        const val RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
