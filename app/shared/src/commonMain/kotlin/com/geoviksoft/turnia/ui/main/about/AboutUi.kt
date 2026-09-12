package com.geoviksoft.turnia.ui.main.about

import com.geoviksoft.turnia.core.domain.model.DeleteAccountError

data class AboutUi(
    val userId: String = "",
    val deletingAccount: Boolean = false,
    val userMessage: DeleteAccountError? = null,
)
