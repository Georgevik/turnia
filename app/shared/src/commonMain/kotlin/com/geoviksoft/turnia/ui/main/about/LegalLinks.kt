package com.geoviksoft.turnia.ui.main.about

import androidx.compose.runtime.Composable
import com.geoviksoft.turnia.core.domain.model.InvitationLinkConfig
import org.jetbrains.compose.resources.stringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.legal_privacy_path
import turnia.app.shared.generated.resources.legal_terms_path

/**
 * Pages served by `firebase/hosting`, on the same domain as the invitation links, one per app
 * language: each language's strings name its own page (`terms`, `terminos`, `conditions`…), and
 * `cleanUrls` in `firebase.json` is what drops the `.html`.
 */
object LegalLinks {
    val terms: String
        @Composable get() = url(stringResource(Res.string.legal_terms_path))

    val privacy: String
        @Composable get() = url(stringResource(Res.string.legal_privacy_path))

    private fun url(path: String) = "https://${InvitationLinkConfig.HOST}/$path"
}
