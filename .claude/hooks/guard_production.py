#!/usr/bin/env python3
"""Stops an agent's Bash call from reaching production; the developer runs these.

A deny rule matches a command's prefix, so it misses a bare `git push` on main, `npx firebase deploy`
or `gh api .../dispatches`. This is a guard against mistakes, not a security boundary: branch
protection is the wall on the server's side.
"""
import json
import os
import re
import subprocess
import sys

FIREBASE_DENIED = {"deploy", "firestore:delete", "functions:delete", "hosting:disable"}
RUNNERS = {"npx", "bunx", "env", "exec", "command", "sudo"}
SHELLS = {"bash", "sh", "zsh"}
FORCE = re.compile(r"^(--force(-with-lease|-if-includes)?(=.*)?|--mirror|--delete|--all|-[a-zA-Z]*[fd][a-zA-Z]*)$")
MAIN = re.compile(r"(^|:)(refs/heads/)?main$")


def current_branch(cwd):
    result = subprocess.run(
        ["git", "-C", cwd, "branch", "--show-current"], capture_output=True, text=True
    )
    return result.stdout.strip()


def words(segment):
    tokens = [token.strip("\"'") for token in segment.split()]
    while tokens:
        first = os.path.basename(tokens[0])
        if "=" in tokens[0] and not tokens[0].startswith("-"):
            tokens.pop(0)
        elif first in RUNNERS or first in SHELLS or tokens[0].startswith("-"):
            tokens.pop(0)
        elif first in {"pnpm", "npm", "yarn"} and tokens[1:2] == ["exec"]:
            del tokens[:2]
        else:
            break
    return tokens


def refused_push(args, cwd):
    if any(FORCE.match(arg) or arg.startswith("+") for arg in args):
        return "a force or destructive push"

    positional = [arg for arg in args if not arg.startswith("-")]
    refspecs = positional[1:]
    if any(MAIN.search(refspec) for refspec in refspecs):
        return "a push to main"
    if (not refspecs or "HEAD" in refspecs) and current_branch(cwd) == "main":
        return "a push to main"
    return None


def refused(segment, cwd):
    tokens = words(segment)
    if not tokens:
        return None
    program, args = os.path.basename(tokens[0]), tokens[1:]

    if program == "firebase" and FIREBASE_DENIED.intersection(args):
        return "a Firebase deploy or delete"
    if program == "gh":
        if args[:2] == ["release", "create"] or (args[:1] == ["api"] and any("dispatches" in a for a in args)):
            return "starting a release"
        if args[:2] == ["workflow", "run"] and any("release" in a.lower() for a in args[2:]):
            return "starting a release"
    if program == "git":
        while args and args[0] in {"-C", "-c"}:
            args = args[2:]
        if args[:1] == ["push"]:
            return refused_push(args[1:], cwd)
    return None


def main():
    payload = json.load(sys.stdin)
    command = payload.get("tool_input", {}).get("command", "")
    cwd = payload.get("cwd") or os.getcwd()

    for segment in re.split(r"&&|\|\||[;|&\n()]", command):
        reason = refused(segment, cwd)
        if reason:
            json.dump(
                {
                    "hookSpecificOutput": {
                        "hookEventName": "PreToolUse",
                        "permissionDecision": "deny",
                        "permissionDecisionReason": f"Blocked: {reason} is the developer's to run.",
                    }
                },
                sys.stdout,
            )
            return


if __name__ == "__main__":
    main()
