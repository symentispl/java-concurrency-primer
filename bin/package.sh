#!/usr/bin/env bash
# Create a distributable upgrade zip containing only tooling files.
# slides/src/ and code/src/ are excluded — they are project-specific content.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

VERSION=$(cat VERSION)
OUT="slides-bootstrap-v${VERSION}.zip"

rm -f "$OUT"

zip -r "$OUT" \
  VERSION \
  .mise.toml \
  Gemfile \
  Gemfile.lock \
  Guardfile \
  .envrc \
  .gitignore \
  settings.gradle.kts \
  build.gradle.kts \
  gradlew \
  gradlew.bat \
  gradle/ \
  .github/ \
  README.md \
  bin/ \
  -x "*.DS_Store" \
  -x ".git/*" \
  -x "bin/*.sh~"

echo "Created: $OUT"
echo "Contents:"
unzip -l "$OUT"
