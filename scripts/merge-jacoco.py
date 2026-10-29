#!/usr/bin/env python3
"""Merge JaCoCo XML reports into one jacoco.xml."""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def add_counters(dst, src):
    existing = {c.get("type"): c for c in dst.findall("counter")}
    for counter in src.findall("counter"):
        kind = counter.get("type")
        if kind in existing:
            current = existing[kind]
            current.set("missed", str(int(current.get("missed") or 0) + int(counter.get("missed") or 0)))
            current.set("covered", str(int(current.get("covered") or 0) + int(counter.get("covered") or 0)))
        else:
            dst.append(counter)


def main():
    if len(sys.argv) < 3:
        sys.exit("usage: merge-jacoco.py <report.xml> ... <out.xml>")
    inputs = [Path(path) for path in sys.argv[1:-1]]
    output = Path(sys.argv[-1])
    root = ET.Element("report", {"name": "jacoco"})
    packages = {}
    used = 0
    for path in inputs:
        if not path.is_file() or path.resolve() == output.resolve():
            continue
        used += 1
        report = ET.parse(path).getroot()
        for package in report.findall("package"):
            name = package.get("name")
            dest = packages.get(name)
            if dest is None:
                dest = ET.Element("package", {"name": name})
                root.append(dest)
                packages[name] = dest
            classes = {item.get("name") for item in dest.findall("class")}
            sourcefiles = {item.get("name") for item in dest.findall("sourcefile")}
            for child in list(package):
                if child.tag == "counter":
                    continue
                if child.tag == "class" and child.get("name") in classes:
                    continue
                if child.tag == "sourcefile" and child.get("name") in sourcefiles:
                    continue
                dest.append(child)
            add_counters(dest, package)
        add_counters(root, report)
    if used == 0:
        sys.exit("no JaCoCo XML reports found")
    output.parent.mkdir(parents=True, exist_ok=True)
    ET.ElementTree(root).write(output, encoding="utf-8", xml_declaration=True)
    print("wrote %s from %s reports" % (output, used))


if __name__ == "__main__":
    main()
