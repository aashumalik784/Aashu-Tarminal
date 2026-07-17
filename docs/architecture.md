# Architecture

## Layers

1. **UI layer** (`app/.../ui`) — Jetpack Compose screens: terminal view,
   settings, tool browser.
2. **Terminal layer** (`app/.../terminal`) — `TerminalSession` owns a PTY,
   `TerminalEmulator` parses the VT100/xterm escape sequence stream and
   maintains screen buffer state, `TerminalView` renders it, `TerminalService`
   keeps sessions alive in the background.
3. **Native layer** (`app/src/main/cpp`) — JNI bridge to a real POSIX PTY
   (`pty_manager.cpp`), plus a minimal software renderer/glyph cache
   (`renderer.cpp`, `glyph_atlas.cpp`) that the Compose `TerminalView` can
   blit from a `Bitmap`/`SurfaceTexture`.
4. **Bootstrap layer** (`app/.../bootstrap`) — `BootstrapManager` downloads
   and extracts the base userland into app-private storage
   (`/data/data/com.aashutarminal/files/usr`), `EnvironmentSetup` writes
   `PATH`, `PREFIX`, `LD_LIBRARY_PATH` etc. into the session environment,
   `ToolInstaller` installs individual tools from the registry.
5. **Tools layer** (`app/.../tools`) — `ToolRegistry` loads
   `assets/tools/tools.json`, `ToolManager` exposes install/remove/search,
   `VersionManager` tracks installed versions for update checks.

## Data flow
`TerminalActivity` → `TerminalService` → `TerminalSession` → JNI
(`pty_manager.cpp`) → real Linux PTY (`/dev/ptmx`) → shell process
(`bash`/`sh` from the bootstrap userland) → output parsed by
`TerminalEmulator` → drawn by `TerminalView` via `renderer.cpp`.
