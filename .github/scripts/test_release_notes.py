"""Tests for the decisions release-notes.py takes. Run: python3 .github/scripts/test_release_notes.py"""

import importlib.util
import sys
import unittest
from pathlib import Path

_spec = importlib.util.spec_from_file_location("release_notes", Path(__file__).with_name("release-notes.py"))
rn = importlib.util.module_from_spec(_spec)
# dataclasses look their module up here while the file is still being executed.
sys.modules[_spec.name] = rn
_spec.loader.exec_module(rn)

V = rn.AppStoreVersion


class PlanVersionTest(unittest.TestCase):
    def test_creates_when_only_released_versions_exist(self):
        plan = rn.plan_version([V("a", "1.11.2", "READY_FOR_DISTRIBUTION")], "1.12.0")
        self.assertEqual(plan.action, "create")

    def test_creates_for_the_first_version(self):
        self.assertEqual(rn.plan_version([], "1.0.0").action, "create")

    def test_uses_the_editable_version_with_the_same_name(self):
        plan = rn.plan_version(
            [V("a", "1.11.2", "READY_FOR_DISTRIBUTION"), V("b", "1.12.0", "PREPARE_FOR_SUBMISSION")], "1.12.0"
        )
        self.assertEqual((plan.action, plan.version.id), ("use", "b"))

    def test_uses_a_rejected_version_with_the_same_name(self):
        plan = rn.plan_version([V("b", "1.12.0", "REJECTED")], "1.12.0")
        self.assertEqual(plan.action, "use")

    def test_renames_an_older_unsubmitted_version(self):
        plan = rn.plan_version(
            [V("a", "1.11.2", "READY_FOR_DISTRIBUTION"), V("b", "1.11.3", "PREPARE_FOR_SUBMISSION")], "1.12.0"
        )
        self.assertEqual((plan.action, plan.version.id), ("rename", "b"))

    def test_skips_while_a_version_is_in_flight(self):
        for state in ("WAITING_FOR_REVIEW", "IN_REVIEW", "READY_FOR_REVIEW", "PENDING_DEVELOPER_RELEASE",
                      "PENDING_APPLE_RELEASE", "PROCESSING_FOR_DISTRIBUTION", "ACCEPTED",
                      "WAITING_FOR_EXPORT_COMPLIANCE"):
            with self.subTest(state=state):
                plan = rn.plan_version([V("a", "1.11.3", state)], "1.12.0")
                self.assertEqual(plan.action, "skip")
                self.assertIn(state, plan.reason)

    def test_skips_a_version_already_released(self):
        plan = rn.plan_version([V("a", "1.11.2", "READY_FOR_DISTRIBUTION")], "1.11.2")
        self.assertEqual(plan.action, "skip")

    def test_skips_a_build_older_than_the_editable_version(self):
        plan = rn.plan_version([V("a", "1.12.0", "PREPARE_FOR_SUBMISSION")], "1.11.2")
        self.assertEqual(plan.action, "skip")
        self.assertIn("older", plan.reason)

    def test_skips_a_build_older_than_a_released_version(self):
        plan = rn.plan_version([V("a", "1.11.2", "READY_FOR_DISTRIBUTION")], "1.10.0")
        self.assertEqual(plan.action, "skip")

    def test_an_unknown_state_counts_as_in_flight(self):
        plan = rn.plan_version([V("a", "1.11.3", "SOMETHING_NEW")], "1.12.0")
        self.assertEqual(plan.action, "skip")

    def test_orders_versions_numerically(self):
        self.assertGreater(rn.version_key("1.10.0"), rn.version_key("1.9.3"))


class PlanNotesTest(unittest.TestCase):
    TEXTS = {"en-GB": "en", "es-ES": "es", "fr-FR": "fr", "de-DE": "de", "it": "it"}

    def test_each_localization_gets_its_own_notes(self):
        plan = rn.plan_notes(["en-GB", "es-ES", "fr-FR", "de-DE", "it"], self.TEXTS)
        self.assertEqual(plan.texts, self.TEXTS)
        self.assertEqual((plan.fallbacks, plan.unused), ([], []))

    def test_a_localization_without_notes_gets_english(self):
        plan = rn.plan_notes(["en-GB", "en-US"], self.TEXTS)
        self.assertEqual(plan.texts["en-US"], "en")
        self.assertEqual(plan.fallbacks, ["en-US"])

    def test_notes_without_a_localization_are_reported_not_added(self):
        plan = rn.plan_notes(["en-GB"], self.TEXTS)
        self.assertEqual(list(plan.texts), ["en-GB"])
        self.assertEqual(sorted(plan.unused), ["de-DE", "es-ES", "fr-FR", "it"])


if __name__ == "__main__":
    unittest.main()
