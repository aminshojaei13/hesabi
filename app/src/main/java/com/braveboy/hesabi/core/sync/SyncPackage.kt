package com.braveboy.hesabi.core.sync

import kotlinx.serialization.Serializable

/**
 * Wire format for phase 6. One package is a snapshot delta: every record carries the
 * timestamp and device that produced it, so the receiver can resolve conflicts
 * without the server ever seeing plaintext.
 *
 * The package is encrypted with a passphrase only the devices know (see
 * [SyncPackageCodec]); a sync server would store it as an opaque blob and could not
 * read an amount, a note or a merchant name.
 */
@Serializable
data class SyncPackage(
    val magic: String = MAGIC,
    val schema: Int = SCHEMA,
    val deviceId: String,
    val deviceName: String,
    /** Revision of the sending device; the server uses it for optimistic concurrency. */
    val baseRevision: Long = 0L,
    val createdAtMillis: Long,
    val records: List<SyncRecord> = emptyList(),
) {
    fun toVersions(): List<SyncVersion> = records.map { it.toVersion() }

    companion object {
        const val MAGIC = "hesabi-sync"
        const val SCHEMA = 1
        const val MAX_RECORDS = 200_000
    }
}

/** A single entity version. `payload` stays an opaque string so the sync layer does not
 *  need to know every DTO; the receiver decodes it with the same serializer that wrote it. */
@Serializable
data class SyncRecord(
    val entity: String,
    val id: String,
    val updatedAtMillis: Long,
    val deviceId: String,
    val deleted: Boolean = false,
    val payload: String = "",
) {
    fun toVersion(): SyncVersion = SyncVersion(
        id = "$entity:$id",
        updatedAtMillis = updatedAtMillis,
        deviceId = deviceId,
        deleted = deleted,
    )
}
