# Bootstrap Guide

On first launch, `BootstrapManager` runs `assets/bootstrap/bootstrap.sh`
inside the app's private storage to lay down a minimal userland:

1. Extract the base rootfs archive (busybox + bash + core coreutils).
2. Create `$PREFIX/{bin,lib,etc,tmp,var}`.
3. Run `setup_env.sh` to write default `.bashrc`, `PATH`, and symlink
   applets.
4. Print `welcome.txt` to the first session.

After that, individual tools are installed on demand via `ToolInstaller`,
which fetches a tool's package, verifies its checksum, and extracts it
into `$PREFIX`.

**Rootfs source:** the actual busybox/bash/coreutils userland ships as
`assets/bootstrap/bootstrap-<arch>.zip`, downloaded automatically at BUILD
time (see the `downloadBootstraps` Gradle task in `app/build.gradle`) from
Termux's own official GitHub releases (termux/termux-packages) -- the same
mechanism termux-app itself uses internally. This means the first CI build
after cloning will fetch real, working binaries with no manual steps.

The scripts and glue code around that rootfs (`bootstrap.sh`,
`setup_env.sh`, `BootstrapManager.kt`, `EnvironmentSetup.kt`) are original
-- written for this app, not copied from Termux.
