package com.geoviksoft.turnia.ui.main.about

import com.geoviksoft.turnia.core.domain.model.InvitationLinkConfig

/**
 * Pages served by `firebase/hosting` (`terms.html`, `privacy.html`), on the same domain as the
 * invitation links. `cleanUrls` in `firebase.json` is what drops the `.html`. Each page links to its
 * Spanish version (`terminos`, `privacidad`).
 */
object LegalLinks {
    const val TERMS = "https://${InvitationLinkConfig.HOST}/terms"
    const val PRIVACY = "https://${InvitationLinkConfig.HOST}/privacy"
}
