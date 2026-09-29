"""Prints every failed/errored testcase under a connected-test results dir as "Class#method,...",
ready for -Pandroid.testInstrumentationRunnerArguments.class. Empty output means nothing to retry."""
import glob
import sys
import xml.etree.ElementTree as ET

results_dir = sys.argv[1]

names = []
for path in glob.glob(f"{results_dir}/**/TEST-*.xml", recursive=True):
    for case in ET.parse(path).getroot().iter("testcase"):
        problem = case.find("failure")
        if problem is None:
            problem = case.find("error")
        if problem is not None:
            names.append(f"{case.get('classname')}#{case.get('name')}")

print(",".join(names))
