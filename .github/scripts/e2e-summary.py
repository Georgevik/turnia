"""Writes the connected-test JUnit results as a Markdown table to the job summary.

A test retried by run-e2e-with-retry.sh leaves its first attempt under
"<results_dir>-attempt1" and its second (final) attempt under "<results_dir>". The final attempt
always wins for a retried test; a test that was never retried only exists in one of the two dirs.
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET

results_dir, report_url = sys.argv[1], (sys.argv[2] if len(sys.argv) > 2 else "")

try:
    with open(".github/known-flaky-e2e-tests.txt") as f:
        known_flaky = {line.strip() for line in f if line.strip()}
except FileNotFoundError:
    known_flaky = set()


def read_cases(dir_path):
    cases = {}
    for path in sorted(glob.glob(os.path.join(dir_path, "**", "TEST-*.xml"), recursive=True)):
        for case in ET.parse(path).getroot().iter("testcase"):
            cases[(case.get("classname", ""), case.get("name"))] = case
    return cases


final_cases = read_cases(results_dir)
attempt1_dir = results_dir + "-attempt1"
retried_keys = set()
if os.path.isdir(attempt1_dir):
    attempt1_cases = read_cases(attempt1_dir)
    retried_keys = set(attempt1_cases) & set(final_cases)
    cases = {**attempt1_cases, **final_cases}  # the final (retried) attempt wins
else:
    cases = final_cases

rows, passed, failed, skipped, retried_ok, flaky_allowed = [], 0, 0, 0, 0, 0
for key in sorted(cases):
    classname, name = key
    case = cases[key]
    display_name = f"{classname.rsplit('.', 1)[-1]}.{name}"
    was_retried = key in retried_keys
    problem = case.find("failure")
    if problem is None:
        problem = case.find("error")
    if problem is not None:
        reason = (problem.text or problem.get("message") or "").strip().splitlines()
        key_id = f"{classname}#{name}"
        if was_retried and key_id in known_flaky:
            flaky_allowed += 1
            tag = "🟡 (known flaky, not blocking)"
        else:
            failed += 1
            tag = "❌ (after retry)" if was_retried else "❌"
        rows.append(f"| {tag} | `{display_name}` | {case.get('time')}s | {reason[0] if reason else ''} |")
    elif case.find("skipped") is not None:
        skipped += 1
        rows.append(f"| ⏭️ | `{display_name}` | | |")
    elif was_retried:
        retried_ok += 1
        rows.append(f"| 🔁 | `{display_name}` | {case.get('time')}s | passed on retry |")
    else:
        passed += 1
        rows.append(f"| ✅ | `{display_name}` | {case.get('time')}s | |")

lines = ["## Android E2E"]
if not rows:
    lines.append("⚠️ No test results: the suite did not run (the build or the emulator failed first).")
else:
    verdict = "✅ All tests passed" if failed == 0 else f"❌ {failed} failed"
    summary = f"**{verdict}** — {passed + retried_ok} passed, {failed} failed, {skipped} skipped"
    if retried_ok:
        summary += f" ({retried_ok} needed a retry)"
    if flaky_allowed:
        summary += f", {flaky_allowed} known flaky (not blocking)"
    lines += [
        summary,
        "",
        "| | Test | Time | Failure |",
        "|---|---|---|---|",
        *sorted(rows, key=lambda row: not row.startswith("| ❌")),
    ]
if report_url:
    lines += ["", f"[Full report (HTML, logcat, emulator logs)]({report_url})"]

with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as summary:
    summary.write("\n".join(lines) + "\n")
