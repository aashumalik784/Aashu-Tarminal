#!/system/bin/sh
# Aashu Bootstrap — writes default shell config and symlinks applets.

PREFIX="$1"
HOME_DIR="$2"

cat > "$HOME_DIR/.bashrc" << 'RC'
export PS1='\[\e[36m\]\w\[\e[0m\] $ '
export EDITOR=nano
alias ll='ls -la'
alias update='aashu-pkg update'
RC

cat > "$PREFIX/etc/motd" << 'MOTD'
Welcome to Aashu Tarminal.
Type 'aashu-pkg list' to browse 500+ available tools.
MOTD

# Symlink busybox applets if busybox is present in the extracted rootfs.
if [ -x "$PREFIX/bin/busybox" ]; then
    for applet in ls cat grep sed awk mkdir rm cp mv chmod tar gzip; do
        [ -e "$PREFIX/bin/$applet" ] || ln -s busybox "$PREFIX/bin/$applet"
    done
fi

echo "[aashu-bootstrap] environment configured."
