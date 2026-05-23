#!/usr/bin/env bash
# Migrate a slides project from Gradle+SDKMAN setup to mise+Ruby (asciidoctor-revealjs).
# Run from the root of your slides project after unpacking the bootstrap zip there.
# Safe to run multiple times (idempotent).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_DIR="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR=".backup-$(date +%Y%m%d-%H%M%S)"

# ── Guard: already migrated? ──────────────────────────────────────────────────
if [ -f ".mise.toml" ]; then
  echo "ERROR: .mise.toml already exists — this project looks already migrated."
  echo "       To upgrade tooling instead, run: bash bin/upgrade.sh"
  exit 1
fi

# ── Guard: must look like an old slides project ───────────────────────────────
if [ ! -f "slides/build.gradle.kts" ] && [ ! -f ".envrc" ]; then
  echo "ERROR: This does not look like a slides project with the old Gradle setup."
  echo "       Expected to find slides/build.gradle.kts and/or .envrc."
  exit 1
fi

echo "==> Detected old Gradle+SDKMAN setup."
echo "==> Creating backup at $BACKUP_DIR/"
mkdir -p "$BACKUP_DIR"

# ── Back up files that will be changed/deleted ────────────────────────────────
[ -f ".envrc"                              ] && cp .envrc                              "$BACKUP_DIR/.envrc.bak"
[ -f "settings.gradle.kts"                ] && cp settings.gradle.kts                "$BACKUP_DIR/settings.gradle.kts.bak"
[ -f ".gitignore"                          ] && cp .gitignore                          "$BACKUP_DIR/.gitignore.bak"
[ -f ".github/workflows/gradle-build.yml" ] && cp .github/workflows/gradle-build.yml "$BACKUP_DIR/gradle-build.yml.bak"
[ -f "slides/build.gradle.kts"            ] && cp slides/build.gradle.kts            "$BACKUP_DIR/slides-build.gradle.kts.bak"

# ── Remove old Gradle slides config ──────────────────────────────────────────
if [ -f "slides/build.gradle.kts" ]; then
  echo "==> Removing slides/build.gradle.kts"
  rm slides/build.gradle.kts
fi

# ── Remove old GitHub Actions workflow ───────────────────────────────────────
if [ -f ".github/workflows/gradle-build.yml" ]; then
  echo "==> Removing .github/workflows/gradle-build.yml"
  rm .github/workflows/gradle-build.yml
fi

# ── Update settings.gradle.kts ───────────────────────────────────────────────
if [ -f "settings.gradle.kts" ]; then
  echo "==> Updating settings.gradle.kts (removing slides include)"
  sed -i '/include("slides")/d' settings.gradle.kts
  if ! grep -q "slides project now built via mise" settings.gradle.kts; then
    echo '// slides project now built via mise (asciidoctor-revealjs Ruby gem)' >> settings.gradle.kts
  fi
fi

# ── Replace .envrc ────────────────────────────────────────────────────────────
echo "==> Writing .envrc"
echo "use mise" > .envrc

# ── Add Ruby/mise entries to .gitignore (idempotent) ─────────────────────────
if ! grep -q "vendor/" .gitignore 2>/dev/null; then
  echo "==> Updating .gitignore"
  cat >> .gitignore << 'EOF'

# Ruby / Bundler
vendor/
.bundle/
*.gem

# mise
.mise.local.toml
EOF
fi

# ── Copy new tooling files from the package ───────────────────────────────────
echo "==> Copying tooling files from package"

cp "$PACKAGE_DIR/.mise.toml"    .mise.toml
cp "$PACKAGE_DIR/Gemfile"       Gemfile
cp "$PACKAGE_DIR/Gemfile.lock"  Gemfile.lock
cp "$PACKAGE_DIR/Guardfile"     Guardfile
cp "$PACKAGE_DIR/README.md"     README.md
cp "$PACKAGE_DIR/VERSION"       VERSION

mkdir -p .github/workflows
cp "$PACKAGE_DIR/.github/workflows/slides-build.yml" .github/workflows/slides-build.yml

# ── Run mise install if mise is available ─────────────────────────────────────
if command -v mise &>/dev/null; then
  echo "==> Running mise install (Ruby, Node, Java, mmdc)..."
  mise install
  echo "==> Running mise run install (Ruby gems)..."
  mise run install
else
  echo "WARNING: mise not found — install it first: https://mise.jdx.dev/getting-started.html"
fi

# ── Done ──────────────────────────────────────────────────────────────────────
echo ""
echo "==> Migration complete! Backup saved to: $BACKUP_DIR/"
echo ""
if command -v mise &>/dev/null; then
  echo "Next step:"
  echo "  mise run build    # verify slides build"
else
  echo "Next steps:"
  echo "  1. Install mise: https://mise.jdx.dev/getting-started.html"
  echo "  2. mise install"
  echo "  3. mise run install"
  echo "  4. mise run build"
fi
echo ""
echo "Manual step (if needed):"
echo "  If slides/src/main/slides/index.adoc contains:"
echo "    :revealjs_plugins: src/main/slides/revealjs-plugins.js"
echo "  Change it to:"
echo "    :revealjs_plugins: revealjs-plugins.js"
