package com.geoviksoft.turnia.ui.signin.createaccount

import com.geoviksoft.turnia.core.domain.model.PasswordRule
import com.geoviksoft.turnia.core.domain.model.meetsPasswordPolicy

data class CreateAccountUi(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val termsAccepted: Boolean = false,
    val privacyAccepted: Boolean = false,
    val submitting: Boolean = false,
    val error: CreateAccountError? = null,
) {
    val metRules: Set<PasswordRule> get() = PasswordRule.entries.filterTo(mutableSetOf()) { it.isMetBy(password) }
    val canSubmit: Boolean
        get() = !submitting && name.isNotBlank() && email.isNotBlank() && password.meetsPasswordPolicy() &&
            termsAccepted && privacyAccepted
}
