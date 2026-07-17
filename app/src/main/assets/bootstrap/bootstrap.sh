#!/system/bin/sh
# Aashu Bootstrap — original first-run setup script for Aashu Tarminal.
# Runs inside app-private storage ($PREFIX). Not derived from any other
# project's bootstrap; written from scratch for this app.

set -e

PREFIX="$1"
HOME_DIR="$2"

echo "[aashu-bootstrap] setting up environment at $PREFIX"

mkdir -p "$PREFIX/bin" "$PREFIX/lib" "$PREFIX/etc" "$PREFIX/tmp" "$PREFIX/var" "$PREFIX/share"
mkdir -p "$HOME_DIR"

# Base rootfs (busybox + bash + coreutils) is extracted by
# EnvironmentSetup.kt before this script runs; this script only wires up
# the environment on top of it.

sh "$(dirname "$0")/setup_env.sh" "$PREFIX" "$HOME_DIR"

echo "[aashu-bootstrap] done."
