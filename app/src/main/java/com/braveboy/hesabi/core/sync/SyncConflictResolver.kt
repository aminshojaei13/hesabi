package com.braveboy.hesabi.core.sync

/**
 * The comparison identity of one record version. `deviceId` is only used to break an
 * exact timestamp tie, and it is compared lexicographically so every device reaches
 * the same answer without needing a clock authority (6.3).
 */
data class SyncVersion(
    val id: String,
    val updatedAtMillis: Long,
    val deviceId: String,
    val deleted: Boolean = false,
)

enum class MergeOutcome { KEEP_LOCAL, TAKE_REMOTE }

data class MergeReport(
    /** Remote versions the receiver must write (new, newer, or winning tombstones). */
    val takeRemote: List<SyncVersion>,
    /** Local versions that stay untouched and can be pushed to the peer later. */
    val keepLocal: List<SyncVersion>,
)

/**
 * Deterministic last-writer-wins merge with tombstones.
 *
 * Rules, in order:
 * 1. A newer `updatedAtMillis` wins. A deletion is an update and can win like any other.
 * 2. On the same timestamp, a tombstone beats a live record (never resurrect by accident).
 * 3. On the same timestamp and the same deletion state, the lexicographically smaller
 *    `deviceId` wins. The rule is symmetric: whatever each side keeps, the union agrees.
 *
 * The result is a total order on versions, so running the merge on device A or B and
 * then re-merging converges — the core property a sync loop needs.
 */
object SyncConflictResolver {

    fun resolve(local: SyncVersion, remote: SyncVersion): MergeOutcome {
        require(local.id == remote.id) { "cannot resolve different records: ${local.id} vs ${remote.id}" }
        if (remote.updatedAtMillis != local.updatedAtMillis) {
            return if (remote.updatedAtMillis > local.updatedAtMillis) MergeOutcome.TAKE_REMOTE else MergeOutcome.KEEP_LOCAL
        }
        if (remote.deleted != local.deleted) {
            return if (remote.deleted) MergeOutcome.TAKE_REMOTE else MergeOutcome.KEEP_LOCAL
        }
        return if (remote.deviceId < local.deviceId) MergeOutcome.TAKE_REMOTE else MergeOutcome.KEEP_LOCAL
    }

    fun merge(local: List<SyncVersion>, remote: List<SyncVersion>): MergeReport {
        val localById = local.associateBy { it.id }
        val remoteById = remote.associateBy { it.id }
        val takeRemote = mutableListOf<SyncVersion>()
        val keepLocal = mutableListOf<SyncVersion>()

        for (remoteVersion in remoteById.values) {
            val localVersion = localById[remoteVersion.id]
            when {
                localVersion == null -> takeRemote += remoteVersion
                resolve(localVersion, remoteVersion) == MergeOutcome.TAKE_REMOTE -> takeRemote += remoteVersion
                else -> keepLocal += localVersion
            }
        }
        for (localVersion in localById.values) {
            if (remoteById[localVersion.id] == null) keepLocal += localVersion
        }
        return MergeReport(takeRemote = takeRemote, keepLocal = keepLocal)
    }

    /**
     * True only when both sides hold exactly the same versions, so a sync round can
     * be skipped. A same-timestamp edit from another device is a real conflict even
     * when the tie-break already picked a winner, so it is reported as out of sync.
     */
    fun isInSync(local: List<SyncVersion>, remote: List<SyncVersion>): Boolean =
        local.associateBy { it.id } == remote.associateBy { it.id }
}
