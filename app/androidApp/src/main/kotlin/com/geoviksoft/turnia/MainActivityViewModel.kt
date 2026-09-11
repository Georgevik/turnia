package com.geoviksoft.turnia

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.geoviksoft.turnia.core.domain.repository.InvitationLinkRepository
import com.geoviksoft.turnia.core.domain.repository.NotificationRepository

class MainActivityViewModel(
    private val notifications: NotificationRepository,
    private val invitations: InvitationLinkRepository,
) : ViewModel() {

    fun readInstallReferrer(context: Context) {
        InstallReferrer(context, invitations).readOnce()

    }

    /** An invitation link, https or the custom scheme; the repository ignores any other. */
    fun openLink(intent: Intent?) {
        val link = intent?.dataString ?: return
        invitations.opened(link)
    }

    fun openNotification(intent: Intent?) {
        val extras = intent?.extras ?: return

        notifications.opened(
            extras.keySet().mapNotNull { key ->
                extras.getString(key)?.let { value -> key to value }
            }.toMap()
        )
    }

}
