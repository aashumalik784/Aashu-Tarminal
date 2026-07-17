# Tool Registry

Tools live under `tools-registry/tools/*.json`, split by category
(languages, databases, devops, networking, editors, build-tools,
package-managers, cloud-tools, security, misc). Each entry:

```json
{
  "name": "python",
  "category": "languages",
  "version": "3.12.3",
  "description": "Python programming language",
  "binary": "python3",
  "size_mb": 42,
  "depends": ["libffi", "openssl"]
}
```

Run `python tools-registry/generate_registry.py` to merge all category
files into `app/src/main/assets/tools/tools.json`, which ships in the APK
and is read by `ToolRegistry.kt` at runtime.
