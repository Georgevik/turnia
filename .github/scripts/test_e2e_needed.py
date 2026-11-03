"""Tests for e2e-needed.py against the real ignore list. Run: python3 .github/scripts/test_e2e_needed.py"""
import importlib.util
import os
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
SPEC = importlib.util.spec_from_file_location("e2e_needed", os.path.join(HERE, "e2e-needed.py"))
e2e_needed = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(e2e_needed)

PATTERNS = e2e_needed.read_patterns(os.path.join(HERE, "..", "e2e-ignored-paths.txt"))


def needs(*paths):
    return e2e_needed.needs_e2e(list(paths), PATTERNS)


class E2eNeededTest(unittest.TestCase):

    def test_app_code_runs(self):
        self.assertTrue(needs("core/src/commonMain/kotlin/com/geoviksoft/turnia/core/Foo.kt"))

    def test_markdown_and_openspec_skip(self):
        self.assertFalse(needs("CLAUDE.md", "openspec/changes/x/tasks.md", "openspec/config.yaml"))

    def test_nested_markdown_skips(self):
        self.assertFalse(needs("firebase/test/README.md", ".github/RELEASE.md"))

    def test_ios_app_skips(self):
        self.assertFalse(needs("app/iosApp/iosApp/iOSApp.swift", "app/iosApp/iosApp.xcodeproj/project.pbxproj"))

    def test_other_ignored_paths_skip(self):
        self.assertFalse(needs("docs/analytics-dashboard.md", "store/listing/google-play.txt",
                               ".claude/hooks/guard_production.py", "firebase/hosting/index.html",
                               "LICENSE", "pendingTests.md"))

    def test_documentation_plus_workflow_runs(self):
        self.assertTrue(needs("README.md", ".github/workflows/android-e2e.yml"))

    def test_firebase_outside_hosting_runs(self):
        self.assertTrue(needs("firebase/firestore.rules"))

    def test_shared_app_runs(self):
        self.assertTrue(needs("app/shared/src/commonMain/kotlin/Foo.kt"))

    def test_build_logic_runs(self):
        self.assertTrue(needs("buildSrc/src/main/kotlin/FirebaseEmulators.kt"))
        self.assertTrue(needs("gradle/libs.versions.toml"))

    def test_lookalike_prefix_runs(self):
        self.assertTrue(needs("app/iosAppTests/Foo.kt"))
        self.assertTrue(needs("LICENSE.txt"))

    def test_empty_list_runs(self):
        self.assertTrue(needs())


if __name__ == "__main__":
    unittest.main()
