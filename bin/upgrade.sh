#!/usr/bin/env bash
# Upgrade tooling files in a slides project already on the mise+Ruby setup.
# Run from the root of your slides project after unpacking the new bootstrap zip there.
# Slides and code content (slides/src/, code/src/) are never touched.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_DIR="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR=".backup-$(date +%Y%m%d-%H%M%S)"

# ── Guard: must already be migrated ──────────────────────────────────────────
if [ ! -f ".mise.toml" ]; then
  echo "ERROR: No .mise.toml found — this project has not been migrated yet."
  echo "       Run: bash bin/migrate.sh"
  exit 1
fi

# ── Version check ─────────────────────────────────────────────────────────────
PKG_VERSION=$(cat "$PACKAGE_DIR/VERSION" 2>/dev/null || echo "unknown")
CUR_VERSION=$(cat VERSION 2>/dev/null || echo "unknown")

if [ "$PKG_VERSION" = "unknown" ] || [ "$CUR_VERSION" = "unknown" ]; then
  echo "WARNING: Could not determine versions (package=$PKG_VERSION, current=$CUR_VERSION)."
elif [ "$PKG_VERSION" = "$CUR_VERSION" ]; then
  echo "INFO: Already at version $CUR_VERSION. Reapplying tooling files."
fi

echo "==> Upgrading from $CUR_VERSION to $PKG_VERSION"
echo "==> Creating backup at $BACKUP_DIR/"
mkdir -p "$BACKUP_DIR"

# ── Back up and overwrite tooling files ───────────────────────────────────────
overwrite() {
  local src="$PACKAGE_DIR/$1" dst="$1"
  if [ -f "$dst" ]; then
    mkdir -p "$BACKUP_DIR/$(dirname "$1")"
    cp "$dst" "$BACKUP_DIR/$1.bak"
  fi
  mkdir -p "$(dirname "$dst")"
  cp "$src" "$dst"
  echo "  Updated: $dst"
}

overwrite ".mise.toml"
overwrite "Gemfile"
overwrite "Gemfile.lock"
overwrite "Guardfile"
overwrite ".envrc"
overwrite ".gitignore"
overwrite "settings.gradle.kts"
overwrite "build.gradle.kts"
overwrite "gradlew"
overwrite "gradlew.bat"
overwrite "README.md"
overwrite "VERSION"
overwrite ".github/workflows/slides-build.yml"
overwrite "bin/migrate.sh"
overwrite "bin/upgrade.sh"
overwrite "bin/package.sh"

# gradle wrapper dir
if [ -d "$PACKAGE_DIR/gradle" ]; then
  cp -r "$PACKAGE_DIR/gradle" .
  echo "  Updated: gradle/"
fi

# ── Make scripts executable ───────────────────────────────────────────────────
chmod +x gradlew bin/migrate.sh bin/upgrade.sh bin/package.sh 2>/dev/null || true

# ── Run mise install to pick up any new tools (e.g. mmdc) ────────────────────
echo "==> Running mise install (tools may have been added or updated)..."
mise install
echo "==> Running mise run install (Ruby gems)..."
mise run install

# ── Done ──────────────────────────────────────────────────────────────────────
echo ""
echo "==> Upgrade to v$PKG_VERSION complete! Backup saved to: $BACKUP_DIR/"
echo ""
echo "Next step:"
echo "  mise run build    # verify slides still build"
