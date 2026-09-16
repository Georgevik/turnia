package com.geoviksoft.turnia.e2e.robots

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.e2e.infra.UI_TIMEOUT_MS
import com.geoviksoft.turnia.e2e.infra.awaitNoNode
import com.geoviksoft.turnia.e2e.infra.awaitNode
import com.geoviksoft.turnia.ui.system.TestTags
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth

/**
 * A month grid — the user's own, a group's or a colleague's — and the day sheet it opens. Every
 * grid starts on the current month; the robot pages forward to reach a later one.
 */
internal class CalendarRobot(compose: ComposeTestRule, today: LocalDate) : AppRobot(compose) {

    private var shown: YearMonth = today.yearMonth

    fun awaitDayShows(date: LocalDate, label: String) {
        showMonthOf(date)
        compose.awaitNode(hasTestTag(TestTags.day(date)) and hasText(label))
    }

    fun awaitDayDoesNotShow(date: LocalDate, label: String) {
        showMonthOf(date)
        compose.awaitNoNode(hasTestTag(TestTags.day(date)) and hasText(label))
    }

    fun openDay(date: LocalDate) {
        showMonthOf(date)
        compose.onNode(hasTestTag(TestTags.day(date))).performClick()
    }

    fun showMonthOf(date: LocalDate) {
        val target = date.yearMonth
        require(target >= shown) { "The robot only pages forward, from $shown to $target" }
        while (shown < target) {
            compose.onAllNodes(hasContentDescription("Next month") and hasClickAction()).onFirst().performClick()
            shown = YearMonth(shown.year + shown.month.ordinal / 11, shown.month.ordinal % 11 + 1 + if (shown.month.ordinal == 11) 0 else 1)
                .let { if (shown.month.ordinal == 11) YearMonth(shown.year + 1, 1) else YearMonth(shown.year, shown.month.ordinal + 2) }
        }
        // The pager keeps the neighbouring months composed; only the shown one is on screen.
        val firstDay = LocalDate(target.year, target.month, 1)
        compose.waitUntil(UI_TIMEOUT_MS) {
            runCatching { compose.onNode(hasTestTag(TestTags.day(firstDay))).assertIsDisplayed() }.isSuccess
        }
    }

    // The day sheet. ==============================

    fun addEventOfType(typeId: String) {
        clickDescription("Add event")
        compose.awaitNode(hasTestTag(TestTags.eventTypeChip(EventTypeId(typeId)))).performClick()
    }

    /** A type created during the test, whose id only its acronym on the chip gives away. */
    fun addEventOfTypeLabelled(acronym: String) {
        clickDescription("Add event")
        compose.awaitNode(hasTestTagPrefix(EVENT_TYPE_CHIP_PREFIX) and hasText(acronym)).performClick()
    }

    fun awaitEvent(eventId: String) {
        compose.awaitNode(row(eventId))
    }

    fun awaitNoEvent(eventId: String) {
        compose.awaitNoNode(row(eventId))
    }

    fun awaitEventShows(eventId: String, text: String) {
        compose.awaitNode(hasText(text) and hasAnyAncestor(row(eventId)), useUnmergedTree = true)
    }

    fun clickInEvent(eventId: String, text: String) {
        compose.awaitNode(hasText(text) and hasClickAction() and hasAnyAncestor(row(eventId))).performClick()
    }

    fun clickDescriptionInEvent(eventId: String, description: String) {
        compose.awaitNode(
            hasContentDescription(description) and hasClickAction() and hasAnyAncestor(row(eventId))
        ).performClick()
    }

    fun toggleSwap(eventId: String) {
        compose.awaitNode(hasTestTag(TestTags.SWAP_TOGGLE) and hasAnyAncestor(row(eventId))).performClick()
    }

    fun writeNote(eventId: String, note: String) {
        clickInEvent(eventId, "Add note")
        compose.awaitNode(hasSetTextAction()).performTextInput(note)
        click("Save")
    }

    private fun row(eventId: String) = hasTestTag(TestTags.dayEvent(EventId(eventId)))

    private fun hasTestTagPrefix(prefix: String) = SemanticsMatcher("TestTag starts with $prefix") { node ->
        node.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { null }
            ?.startsWith(prefix) == true
    }

    private companion object {
        val EVENT_TYPE_CHIP_PREFIX = TestTags.eventTypeChip(EventTypeId("")).removeSuffix("")
    }
}
