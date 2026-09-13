package com.geoviksoft.turnia.ui.main.about

import com.geoviksoft.turnia.core.domain.model.InvitationLinkConfig

/**
 * The privacy policy served by `firebase/hosting`, on the same domain as the invitation links:
 * `privacy.html` in English and `privacidad.html` in Spanish. `cleanUrls` in `firebase.json` is what
 * drops the `.html`. The path comes from `about_privacy_path`, so the page follows the app language.
 */
object LegalLinks {
    fun privacy(path: String) = "https://${InvitationLinkConfig.HOST}/$path"
}
