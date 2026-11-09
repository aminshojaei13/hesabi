package com.braveboy.hesabi.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncConflictResolverTest {

    private fun version(
        id: String,
        at: Long,
        device: String = "a",
        deleted: Boolean = false,
    ) = SyncVersion(id = id, updatedAtMillis = at, deviceId = device, deleted = deleted)

    @Test
    fun newerRemoteWins() {
        val outcome = SyncConflictResolver.resolve(version("t:1", 100), version("t:1", 200, "b"))

        assertEquals(MergeOutcome.TAKE_REMOTE, outcome)
    }

    @Test
    fun newerLocalWins() {
        val outcome = SyncConflictResolver.resolve(version("t:1", 300), version("t:1", 200, "b"))

        assertEquals(MergeOutcome.KEEP_LOCAL, outcome)
    }

    @Test
    fun tombstoneBeatsLiveOnTheSameTimestamp() {
        val live = version("t:1", 500, "a", deleted = false)
        val tombstone = version("t:1", 500, "b", deleted = true)

        assertEquals(MergeOutcome.TAKE_REMOTE, SyncConflictResolver.resolve(live, tombstone))
        assertEquals(MergeOutcome.KEEP_LOCAL, SyncConflictResolver.resolve(tombstone, live))
    }

    @Test
    fun aNewerEditResurrectsADeletedRecord() {
        val tombstone = version("t:1", 100, "a", deleted = true)
        val newerEdit = version("t:1", 200, "b", deleted = false)

        assertEquals(MergeOutcome.TAKE_REMOTE, SyncConflictResolver.resolve(tombstone, newerEdit))
    }

    @Test
    fun timestampTieIsResolvedDeterministicallyByDeviceId() {
        val a = version("t:1", 100, "device-a")
        val b = version("t:1", 100, "device-b")

        assertEquals(MergeOutcome.KEEP_LOCAL, SyncConflictResolver.resolve(a, b))
        assertEquals(MergeOutcome.TAKE_REMOTE, SyncConflictResolver.resolve(b, a))
    }

    @Test
    fun mergeIsSymmetricAndConverges() {
        val local = listOf(version("t:1", 100), version("t:2", 300))
        val remote = listOf(version("t:1", 200, "b"), version("t:3", 50, "b"))

        val forward = SyncConflictResolver.merge(local, remote)
        val backward = SyncConflictResolver.merge(remote, local)

        assertEquals(setOf("t:1", "t:3"), forward.takeRemote.map { it.id }.toSet())
        assertEquals(listOf("t:2"), forward.keepLocal.map { it.id })
        assertEquals(forward.takeRemote.map { it.id }.toSet(), backward.keepLocal.map { it.id }.toSet())
    }

    @Test
    fun remoteOnlyRecordsAreAddedAndLocalOnlyKept() {
        val report = SyncConflictResolver.merge(
            local = listOf(version("t:local", 10)),
            remote = listOf(version("t:remote", 10, "b")),
        )

        assertEquals(listOf("t:remote"), report.takeRemote.map { it.id })
        assertEquals(listOf("t:local"), report.keepLocal.map { it.id })
    }

    @Test
    fun identicalSidesAreInSync() {
        val versions = listOf(version("t:1", 100), version("t:2", 200))

        assertTrue(SyncConflictResolver.isInSync(versions, versions))
    }

    @Test
    fun differentSidesAreNotInSync() {
        val local = listOf(version("t:1", 100))
        val remote = listOf(version("t:1", 100, "z"))

        assertTrue(!SyncConflictResolver.isInSync(local, remote))
    }

    @Test
    fun resolvingDifferentIdsIsRejected() {
        val failure = runCatching { SyncConflictResolver.resolve(version("t:1", 1), version("t:2", 1)) }

        assertTrue(failure.isFailure)
    }
}
