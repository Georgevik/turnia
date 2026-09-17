"""Writes the connected-test JUnit results as a Markdown table to the job summary."""
import glob
import os
import sys
import xml.etree.ElementTree as ET

results_dir, report_url = sys.argv[1], (sys.argv[2] if len(sys.argv) > 2 else "")

rows, passed, failed, skipped = [], 0, 0, 0
for path in sorted(glob.glob(os.path.join(results_dir, "**", "TEST-*.xml"), recursive=True)):
    for case in ET.parse(path).getroot().iter("testcase"):
        name = f"{case.get('classname', '').rsplit('.', 1)[-1]}.{case.get('name')}"
        problem = case.find("failure")
        if problem is None:
            problem = case.find("error")
        if problem is not None:
            failed += 1
            reason = (problem.text or problem.get("message") or "").strip().splitlines()
            rows.append(f"| ❌ | `{name}` | {case.get('time')}s | {reason[0] if reason else ''} |")
        elif case.find("skipped") is not None:
            skipped += 1
            rows.append(f"| ⏭️ | `{name}` | | |")
        else:
            passed += 1
            rows.append(f"| ✅ | `{name}` | {case.get('time')}s | |")

lines = ["## Android E2E"]
if not rows:
    lines.append("⚠️ No test results: the suite did not run (the build or the emulator failed first).")
else:
    verdict = "✅ All tests passed" if failed == 0 else f"❌ {failed} failed"
    lines += [
        f"**{verdict}** — {passed} passed, {failed} failed, {skipped} skipped",
        "",
        "| | Test | Time | Failure |",
        "|---|---|---|---|",
        *sorted(rows, key=lambda row: not row.startswith("| ❌")),
    ]
if report_url:
    lines += ["", f"[Full report (HTML, logcat, emulator logs)]({report_url})"]

with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as summary:
    summary.write("\n".join(lines) + "\n")
