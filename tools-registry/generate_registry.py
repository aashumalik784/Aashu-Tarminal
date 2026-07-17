#!/usr/bin/env python3
"""
Aashu Tarminal — tool registry generator.

Merges tools-registry/tools/*.json (one file per category) into a single
app/src/main/assets/tools/tools.json, plus derives categories.json and
aliases.json. Run this after editing/adding tools.

Usage: python tools-registry/generate_registry.py
"""
import json
import os

ROOT = os.path.dirname(os.path.abspath(__file__))
TOOLS_DIR = os.path.join(ROOT, "tools")
OUT_DIR = os.path.join(ROOT, "..", "app", "src", "main", "assets", "tools")


def main():
    merged = []
    categories = {}

    for filename in sorted(os.listdir(TOOLS_DIR)):
        if not filename.endswith(".json"):
            continue
        category = filename.replace(".json", "")
        with open(os.path.join(TOOLS_DIR, filename), encoding="utf-8") as f:
            entries = json.load(f)
        categories[category] = len(entries)
        for entry in entries:
            entry.setdefault("category", category)
            merged.append(entry)

    os.makedirs(OUT_DIR, exist_ok=True)

    with open(os.path.join(OUT_DIR, "tools.json"), "w", encoding="utf-8") as f:
        json.dump(merged, f, indent=2, ensure_ascii=False)

    with open(os.path.join(OUT_DIR, "categories.json"), "w", encoding="utf-8") as f:
        json.dump(categories, f, indent=2, ensure_ascii=False)

    aliases = {}
    for entry in merged:
        for alias in entry.get("aliases", []):
            aliases[alias] = entry["name"]
    with open(os.path.join(OUT_DIR, "aliases.json"), "w", encoding="utf-8") as f:
        json.dump(aliases, f, indent=2, ensure_ascii=False)

    print(f"Merged {len(merged)} tools across {len(categories)} categories.")
    print(f"Written to {OUT_DIR}")


if __name__ == "__main__":
    main()
