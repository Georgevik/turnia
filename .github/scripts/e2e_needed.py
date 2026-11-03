"""Decides whether a pull request has to run the Android E2E suite.

Reads the changed paths on stdin, one per line, and the ignore list from the file given as the only
argument. Writes run=false only when every path matches an ignored pattern, and run=true otherwise,
including when no path is given: a change whose files could not be listed is not proof it is safe.
The answer goes to stdout, and to $GITHUB_OUTPUT when it is set."""
import fnmatch
import os
import sys


def read_patterns(path):
    with open(path) as f:
        lines = [line.strip() for line in f]
    return [line for line in lines if line and not line.startswith("#")]


def needs_e2e(paths, patterns):
    if not paths:
        return True
    return any(not any(fnmatch.fnmatchcase(p, pattern) for pattern in patterns) for p in paths)


def main():
    patterns = read_patterns(sys.argv[1])
    paths = [line.strip() for line in sys.stdin if line.strip()]
    line = "run=%s" % str(needs_e2e(paths, patterns)).lower()
    output = os.environ.get("GITHUB_OUTPUT")
    if output:
        with open(output, "a") as f:
            f.write(line + "\n")
    print(line)


if __name__ == "__main__":
    main()
