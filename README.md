# Aashu Tarminal

Aashu Tarminal is an open-source Android terminal emulator with a built-in
bootstrap system that installs a Linux-style environment (138 curated CLI
tools across languages, databases, devops, networking, editors, build
tools, package managers, cloud tools, and security -- registry format is
built to scale well past 500) directly on-device, no root required.

## Features
- Native terminal emulator (PTY + custom renderer, written in C++ via JNI)
- Custom Aashu Bootstrap — downloads and sets up a minimal Linux userland
- tool registry (138 tools shipped, easily extendable), organized by category, installable on demand
- Multiple sessions, customizable themes and fonts
- Material 3 UI built with Jetpack Compose

## Project layout
See `docs/architecture.md` for a full breakdown of the codebase.

## Building
```
./scripts/build.sh
```

## License
GPLv3 — see [LICENSE](LICENSE).
