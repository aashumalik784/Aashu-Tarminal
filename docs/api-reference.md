# API Reference (internal)

## TerminalSession
- `open(command: String, args: Array<String>, cwd: String, env: Array<String>): Int`
- `write(data: ByteArray)`
- `resize(cols: Int, rows: Int)`
- `close()`

## ToolRegistry
- `loadAll(): List<Tool>`
- `findByName(name: String): Tool?`
- `byCategory(category: String): List<Tool>`

## BootstrapManager
- `isBootstrapped(): Boolean`
- `runBootstrap(progress: (Float) -> Unit)`
- `reset()`

See KDoc in each source file for full parameter descriptions.
