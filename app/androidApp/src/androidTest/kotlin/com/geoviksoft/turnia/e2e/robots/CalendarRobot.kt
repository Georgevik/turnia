package com.geoviksoft.turnia.e2e.robots

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventTypeId
import com.geoviksoft.turnia.e2e.infra.UI_TIMEOUT_MS
import com.geoviksoft.turnia.e2e.infra.awaitNoNode
import com.geoviksoft.turnia.e2e.infra.awaitNode
import com.geoviksoft.turnia.e2e.infra.scrollAndClick
import com.geoviksoft.turnia.ui.system.TestTags
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth
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
        compose.awaitNode(hasTestTag(TestTags.day(date))).scrollAndClick()
    }

    fun showMonthOf(date: LocalDate) {
        val target = date.yearMonth
        require(target >= shown) { "The robot only pages forward, from $shown to $target" }
        while (shown < target) {
            // Awaited: a test that pages first thing would otherwise tap while the splash is still up.
            compose.awaitNode(hasContentDescription("Next month") and hasClickAction())
                .scrollAndClick()
            shown = shown.plusMonth()
        }
        // The pager keeps the neighbouring months composed; only the shown one is on screen.
        compose.waitUntil(UI_TIMEOUT_MS) {
            runCatching {
                compose.onNode(hasTestTag(TestTags.day(target.firstDay))).assertIsDisplayed()
            }.isSuccess
        }
    }

    /** Pages back one month, the way a user checks the one they just left. */
    fun showPreviousMonth() {
        compose.awaitNode(hasContentDescription("Previous month") and hasClickAction()).scrollAndClick()
        shown = shown.minusMonth()
        compose.waitUntil(UI_TIMEOUT_MS) {
            runCatching {
                compose.onNode(hasTestTag(TestTags.day(shown.firstDay))).assertIsDisplayed()
            }.isSuccess
        }
    }

    // The day sheet. ==============================

    /**
     * Until the sheet has finished closing its veil still covers the grid, and a tap meant for the
     * next day lands on it instead.
     */
    fun awaitDayClosed() {
        compose.awaitNoNode(hasTestTag(TestTags.DAY_SHEET))
    }

    fun addEventOfType(typeId: String) {
        clickDescription("Add event")
        compose.awaitNode(hasTestTag(TestTags.eventTypeChip(EventTypeId(typeId)))).scrollAndClick()
    }

    /** A type created during the test, whose id only its acronym on the chip gives away. */
    fun addEventOfTypeLabelled(acronym: String) {
        clickDescription("Add event")
        compose.awaitNode(hasTestTagPrefix(EVENT_TYPE_CHIP_PREFIX) and hasText(acronym))
            .scrollAndClick()
    }

    /** A shift chip on an add pane that is already open, found by the acronym it shows. */
    fun pickShiftLabelled(acronym: String) {
        compose.awaitNode(hasTestTagPrefix(EVENT_TYPE_CHIP_PREFIX) and hasText(acronym)).scrollAndClick()
    }

    /** A one-off from the day sheet, which stays open after saving; closed so the day is done. */
    fun addOneOff(date: LocalDate, name: String) {
        openDay(date)
        clickDescription("Add event")
        openOtherEvent()
        type("Name", name)
        click("Save")
        awaitText(name)
        back()
        awaitDayClosed()
    }

    /** Below the shifts and the groups, so it may be off the pane until scrolled to. */
    fun openOtherEvent() {
        compose.awaitNode(hasTestTag(TestTags.ADD_PANE_OTHER_EVENT)).scrollAndClick()
    }

    /** The one-off's form, in edit mode, from its row in the day's sheet. */
    fun openOneOff(date: LocalDate, name: String) {
        openDay(date)
        // The row in the sheet, not the grid's label behind it: only the row is labelled "Edit".
        compose.awaitNode(hasText(name, substring = true) and hasClickLabel(EDIT)).scrollAndClick()
    }

    fun editOneOff(date: LocalDate, name: String, newName: String) {
        openOneOff(date, name)
        compose.awaitNode(hasSetTextAction() and hasText("Name")).apply {
            performTextClearance()
            performTextInput(newName)
        }
        click("Save")
        awaitText(newName)
        back()
        awaitDayClosed()
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
        compose.awaitNode(hasText(text) and hasClickAction() and hasAnyAncestor(row(eventId)))
            .scrollAndClick()
    }

    /** An action of the row's menu: "Delete event", "Give shift back", "Move to a group". */
    fun eventAction(eventId: String, action: String) {
        compose.awaitNode(hasContentDescription(MORE_OPTIONS) and hasAnyAncestor(row(eventId)))
            .scrollAndClick()
        click(action)
    }

    fun toggleSwap(eventId: String) {
        compose.awaitNode(hasTestTag(TestTags.SWAP_TOGGLE) and hasAnyAncestor(row(eventId)))
            .scrollAndClick()
    }

    fun writeNote(eventId: String, note: String) {
        clickInEvent(eventId, "Add note")
        compose.awaitNode(hasSetTextAction()).performTextInput(note)
        click("Save")
    }

    fun editNote(eventId: String, current: String, note: String) {
        clickInEvent(eventId, current)
        compose.awaitNode(hasSetTextAction()).apply {
            performTextClearance()
            if (note.isNotEmpty()) performTextInput(note)
        }
        click("Save")
    }

    private fun row(eventId: String) = hasTestTag(TestTags.dayEvent(EventId(eventId)))

    private fun hasTestTagPrefix(prefix: String) =
        SemanticsMatcher("TestTag starts with $prefix") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
        }

    private fun hasClickLabel(label: String) = SemanticsMatcher("click label $label") { node ->
        node.config.getOrNull(SemanticsActions.OnClick)?.label == label
    }

    private companion object {
        val EVENT_TYPE_CHIP_PREFIX = TestTags.eventTypeChip(EventTypeId(""))
        const val EDIT = "Edit"
        const val MORE_OPTIONS = "More options"
    }
}
