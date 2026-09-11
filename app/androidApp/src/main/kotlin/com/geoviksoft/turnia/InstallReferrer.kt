package com.geoviksoft.turnia

import android.content.Context
import android.os.RemoteException
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import com.geoviksoft.turnia.core.data.logger.Logger
import com.geoviksoft.turnia.core.domain.repository.InvitationLinkRepository

/**
 * Hands over the invitation code of an install that started from an invitation link.
 *
 * The invitation page sends the Play Store `referrer=code=CODE`, and Play keeps it for the app to
 * read once installed: the only way a code survives the install. It is read once per install —
 * Play answers with the same referrer for 90 days, and opening the join sheet on every launch
 * would be a bug.
 */
class InstallReferrer(
    private val context: Context,
    private val invitations: InvitationLinkRepository,
) {

    fun readOnce() {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_READ, false)) return

        val client = InstallReferrerClient.newBuilder(context).build()
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerResponse.OK -> {
                        codeOf(client)?.let(invitations::referred)
                        prefs.edit().putBoolean(KEY_READ, true).apply()
                    }

                    // Not installed from Play, or a Play too old to say: asking again changes
                    // nothing.
                    InstallReferrerResponse.FEATURE_NOT_SUPPORTED,
                    InstallReferrerResponse.DEVELOPER_ERROR ->
                        prefs.edit().putBoolean(KEY_READ, true).apply()

                    // Play is busy or updating: the next launch tries again.
                    else -> Unit
                }
                client.endConnection()
            }

            override fun onInstallReferrerServiceDisconnected() = Unit
        })
    }

    /** `code=CODE`, possibly among the `utm_*` pairs Play adds; an organic install has none. */
    private fun codeOf(client: InstallReferrerClient): String? {
        val referrer = try {
            client.installReferrer.installReferrer
        } catch (e: RemoteException) {
            Logger.e(TAG, "Install referrer unavailable", e)
            return null
        }

        return referrer.split('&')
            .firstOrNull { it.startsWith(CODE_PREFIX) }
            ?.removePrefix(CODE_PREFIX)
    }

    private companion object {
        const val TAG = "InstallReferrer"
        const val PREFS = "install_referrer"
        const val KEY_READ = "read"

        // The contract with firebase/hosting/join.html, which builds the store link.
        const val CODE_PREFIX = "code="
    }
}
