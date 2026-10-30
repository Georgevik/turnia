#!/usr/bin/env python3
"""Run with `python3 .claude/hooks/test_guard_production.py`: a guard that stops matching fails silently."""
import json
import os
import subprocess
import sys
import tempfile
import unittest

HOOK = os.path.join(os.path.dirname(os.path.abspath(__file__)), "guard_production.py")

DENIED = [
    "firebase deploy --only functions",
    "cd firebase && firebase deploy",
    "npx firebase deploy",
    "pnpm exec firebase deploy",
    "firebase --project turnia-23ebc deploy",
    "./node_modules/.bin/firebase deploy",
    "bash -c 'firebase deploy'",
    "firebase firestore:delete --all-collections",
    "firebase functions:delete takeEvent",
    "gh workflow run release.yml",
    "gh workflow run Release --ref main",
    "gh api repos/Georgevik/turnia/actions/workflows/release.yml/dispatches -f ref=main",
    "gh release create v2",
    "git push origin main",
    "git push origin HEAD:main",
    "git push origin feature:refs/heads/main",
    "git push --force origin feature",
    "git push -f",
    "git push -uf origin feature",
    "git push --force-with-lease",
    "git push origin +feature",
    "git -C /tmp push --force",
    "git push origin --delete main",
    "echo ok; git push origin main",
]
DENIED_ON_MAIN = ["git push", "git push origin", "git push origin HEAD"]
ALLOWED = [
    "ls",
    "git status",
    "git push -u origin features/tur-19",
    "git push origin features/main-screen",
    "git log --grep='firebase deploy'",
    "grep -rn 'firebase deploy' .github",
    "firebase emulators:start",
    "gh workflow run android-e2e.yml",
    "gh workflow list",
    "gh pr create --title 'Stop the deploy'",
]


def decision(command, cwd):
    payload = json.dumps({"tool_name": "Bash", "tool_input": {"command": command}, "cwd": cwd})
    result = subprocess.run([sys.executable, HOOK], input=payload, capture_output=True, text=True, check=True)
    return json.loads(result.stdout)["hookSpecificOutput"]["permissionDecision"] if result.stdout else "allow"


def repo_on(branch):
    path = tempfile.mkdtemp()
    subprocess.run(["git", "init", "-q", "-b", branch, path], check=True)
    return path


class GuardProductionTest(unittest.TestCase):
    def test_denied_on_any_branch(self):
        cwd = repo_on("features/x")
        for command in DENIED:
            with self.subTest(command):
                self.assertEqual("deny", decision(command, cwd))

    def test_a_bare_push_is_denied_on_main_only(self):
        for command in DENIED_ON_MAIN:
            with self.subTest(command):
                self.assertEqual("deny", decision(command, repo_on("main")))
                self.assertEqual("allow", decision(command, repo_on("features/x")))

    def test_allowed(self):
        cwd = repo_on("features/x")
        for command in ALLOWED:
            with self.subTest(command):
                self.assertEqual("allow", decision(command, cwd))


if __name__ == "__main__":
    unittest.main()
