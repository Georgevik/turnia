package com.geoviksoft.turnia.ui.main.about

import org.jetbrains.compose.resources.StringResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.about_feedback_report_body
import turnia.app.shared.generated.resources.about_feedback_report_subject
import turnia.app.shared.generated.resources.about_feedback_suggest_body
import turnia.app.shared.generated.resources.about_feedback_suggest_subject

data class AboutUi(
    val userId: String = "",
    val supportEmail: String = "",
    /** A mail the user asked to write, until the UI has handed it to the mail app. */
    val feedbackMail: FeedbackMailUi? = null,
    val userMessage: AboutMessage? = null,
)

data class FeedbackMailUi(
    val address: String,
    val subject: StringResource,
    val prompt: StringResource,
    val versionName: String,
    val versionBuild: String,
    val system: String,
    val userId: String,
)

enum class FeedbackKind(val subject: StringResource, val prompt: StringResource) {
    REPORT(Res.string.about_feedback_report_subject, Res.string.about_feedback_report_body),
    SUGGESTION(Res.string.about_feedback_suggest_subject, Res.string.about_feedback_suggest_body),
}

enum class AboutMessage {
    NoEmailApp,
}
