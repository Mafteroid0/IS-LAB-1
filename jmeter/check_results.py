"""Return a nonzero exit code for an incomplete or failed JMeter CSV run."""

import argparse
import csv
import sys
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("results", type=Path)
    args = parser.parse_args()
    with args.results.open(encoding="utf-8-sig", newline="") as stream:
        reader = csv.DictReader(stream)
        if not {"label", "responseCode", "success"} <= set(reader.fieldnames or ()):
            parser.error("Expected a JMeter CSV with label, responseCode and success columns")
        rows = list(reader)

    failed = [row for row in rows if row["success"] != "true"]
    total = len(rows)
    print(f"{total} samples, {len(failed)} failures")
    for row in failed:
        print(f'{row["label"]}: HTTP {row["responseCode"]} {row.get("failureMessage", "")}', file=sys.stderr)

    completed = bool(rows) and rows[-1]["label"] == "Check: mixed race persisted exactly one outcome"
    if not completed:
        print("The last verification sample is missing", file=sys.stderr)
    return int(bool(failed) or not completed)


if __name__ == "__main__":
    sys.exit(main())
