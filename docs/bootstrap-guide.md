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

This is **Aashu Bootstrap** — a from-scratch bootstrap implementation
(not copied from any other project), designed to be simple to audit and
easy to extend with new tool categories.
