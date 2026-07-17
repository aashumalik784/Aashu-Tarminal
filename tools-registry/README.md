# tools-registry

Source of truth for Aashu Tarminal's tool catalog. Each JSON file under
`tools/` is one category; `generate_registry.py` merges them into
`app/src/main/assets/tools/{tools,categories,aliases}.json` which ships in
the APK.

To add a tool: edit the relevant category file, then run:
```
python generate_registry.py
```
