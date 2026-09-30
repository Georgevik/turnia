"""Given a comma-separated "Class#method,..." list of still-failing tests and the known-flaky file
(one "Class#method" per line), prints only the failures that are NOT on that list. Empty output
means every remaining failure is already known and tracked, so the run should not be blocked."""
import sys

failed = [t for t in sys.argv[1].split(",") if t]
known_flaky_path = sys.argv[2]

try:
    with open(known_flaky_path) as f:
        known = {line.strip() for line in f if line.strip()}
except FileNotFoundError:
    known = set()

unknown = [t for t in failed if t not in known]
print(",".join(unknown))
