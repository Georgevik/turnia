package com.geoviksoft.turnia.core.data.datasource.firestore

import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.FirestoreAudit
import com.geoviksoft.turnia.core.data.datasource.firestore.analytics.FirestoreUsage
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/** Through [FirestoreAudit.add], as `trackStorage` does: its log line needs Android's `Log`. */
class FirestoreAuditStorageTest {

    @Test
    fun storageIsItsOwnBlock() {
        FirestoreAudit.add("PhotoStorageAuditTest", "upload", FirestoreUsage(uploads = 1, storageBytes = 20 * 1024))
        FirestoreAudit.add("PhotoStorageAuditTest", "download", FirestoreUsage(downloads = 1, storageBytes = 30 * 1024))

        val storage = FirestoreAudit.summary().substringAfter("\nStorage  ")

        assertContains(storage, "PhotoStorageAuditTest -> Uploads: 1 Downloads: 1 KB: 50")
    }

    @Test
    fun storageSpendsNothingUnderFirestore() {
        FirestoreAudit.add("PhotoStorageOnlyTest", "upload", FirestoreUsage(uploads = 1, storageBytes = 1024))

        val firestore = FirestoreAudit.summary().substringBefore("\nFunctions  ")

        assertFalse("PhotoStorageOnlyTest" in firestore)
    }
}
