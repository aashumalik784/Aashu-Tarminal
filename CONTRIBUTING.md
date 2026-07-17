# Contributing to Aashu Tarminal

Thanks for wanting to contribute!

1. Fork the repo and create a branch from `main`.
2. Follow the existing Kotlin style (ktlint) and C++ style (clang-format).
3. Add/adjust tests under `app/src/test` or `app/src/androidTest`.
4. Run `./scripts/test.sh` before opening a PR.
5. Open a PR against `main` describing the change and motivation.

## Adding a new tool to the registry
Add an entry to the relevant file under `tools-registry/tools/<category>.json`
following the existing schema, then run:
```
python tools-registry/generate_registry.py
```
This regenerates `app/src/main/assets/tools/tools.json`.
