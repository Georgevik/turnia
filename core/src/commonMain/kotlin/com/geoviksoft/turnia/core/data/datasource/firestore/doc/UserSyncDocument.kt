package com.geoviksoft.turnia.core.data.datasource.firestore.doc

import dev.gitlive.firebase.firestore.BaseTimestamp
import kotlinx.datetime.YearMonth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * When each of the user's own things last changed, so a reader can tell whether its cache is behind
 * without reading the thing itself.
 */
@Serializable
data class UserSyncDocument(
    @SerialName("personalEvents") val personalEventsUpdatedAt: Map<YearMonth, EventSyncUpdateAt> = emptyMap(),
    @SerialName("personalEventTypesUpdatedAt") val personalEventTypesUpdatedAt: BaseTimestamp? = null,
    @SerialName("revokedGroups") val revokedGroupsUpdatedAt: BaseTimestamp? = null,
    @SerialName(FIELD_ACCOUNT) val accountUpdatedAt: BaseTimestamp? = null,
    /** Last write to the user's own `users/{uid}`, from any device or from `updateProfile`. */
    @SerialName(FIELD_PROFILE) val profileUpdatedAt: BaseTimestamp? = null,
    @SerialName(FIELD_JOIN_REQUESTS) val joinRequestsUpdatedAt: BaseTimestamp? = null,
    @SerialName(FIELD_PREFERENCES) val preferencesUpdatedAt: BaseTimestamp? = null,
    /** Moved only by the receipt-verification function, in the commit that writes the entitlement. */
    @SerialName(FIELD_SUBSCRIPTION) val subscriptionUpdatedAt: BaseTimestamp? = null,
    @Deprecated("Remove at some point")
    @SerialName("private") val legacyPrivateUpdatedAt: BaseTimestamp? = null,
) {
    val accountMarker: BaseTimestamp? get() = accountUpdatedAt ?: legacyPrivateUpdatedAt
    val joinRequestsMarker: BaseTimestamp? get() = joinRequestsUpdatedAt ?: legacyPrivateUpdatedAt

    companion object {
        const val FIELD_ACCOUNT = "account"
        const val FIELD_PROFILE = "profile"
        const val FIELD_JOIN_REQUESTS = "joinRequests"
        const val FIELD_PREFERENCES = "preferences"
        const val FIELD_SUBSCRIPTION = "subscription"
    }
}
