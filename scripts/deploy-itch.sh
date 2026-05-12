#!/usr/bin/env bash
# scripts/deploy-itch.sh — push a built JAR to itch.io via butler.
#
# Called by .github/workflows/itch-deploy.yml, but also runnable locally
# once you have butler installed and BUTLER_API_KEY exported.
#
# Required environment variables:
#   BUTLER_API_KEY     itch.io API key (https://itch.io/user/settings/api-keys)
#   ITCH_JAR           path to the JAR to upload
#   ITCH_CHANNEL       itch.io channel (e.g. desktop, windows, linux, mac)
#   ITCH_VERSION_TAG   version label for the upload (short SHA, semver tag, etc.)
#
# Optional:
#   ITCH_TARGET        full itch.io target slug; defaults to sohailshahm/cloudy-ninja

set -euo pipefail

: "${BUTLER_API_KEY:?BUTLER_API_KEY must be set (itch.io API key)}"
: "${ITCH_JAR:?ITCH_JAR must be set (path to JAR file)}"
: "${ITCH_CHANNEL:?ITCH_CHANNEL must be set (e.g. desktop)}"
: "${ITCH_VERSION_TAG:?ITCH_VERSION_TAG must be set}"

ITCH_TARGET="${ITCH_TARGET:-sohailshahm/cloudy-ninja}"

if [ ! -f "$ITCH_JAR" ]; then
  echo "ERROR: JAR not found at $ITCH_JAR" >&2
  exit 1
fi

echo "Deploy plan:"
echo "  JAR:     $ITCH_JAR"
echo "  Target:  $ITCH_TARGET:$ITCH_CHANNEL"
echo "  Version: $ITCH_VERSION_TAG"
echo ""

# Install butler into a workspace-local dir so this script is self-contained
# and doesn't pollute the user's PATH on local runs.
BUTLER_DIR="${BUTLER_DIR:-$PWD/.butler}"
mkdir -p "$BUTLER_DIR"

# Pick the right butler binary for the host. Linux runners are the CI path;
# the other branches let you run this locally on macOS / Windows-via-git-bash.
OS="$(uname -s)"
case "$OS" in
  Linux*)   BUTLER_PLATFORM="linux-amd64"  ; BUTLER_BIN="butler"     ;;
  Darwin*)  BUTLER_PLATFORM="darwin-amd64" ; BUTLER_BIN="butler"     ;;
  MINGW*|MSYS*|CYGWIN*) BUTLER_PLATFORM="windows-amd64" ; BUTLER_BIN="butler.exe" ;;
  *)        echo "ERROR: unsupported host OS: $OS" >&2 ; exit 1 ;;
esac

BUTLER_PATH="$BUTLER_DIR/$BUTLER_BIN"

if [ ! -x "$BUTLER_PATH" ]; then
  echo "Installing butler ($BUTLER_PLATFORM) into $BUTLER_DIR ..."
  ZIP="$BUTLER_DIR/butler.zip"
  curl -fLsS -o "$ZIP" "https://broth.itch.zone/butler/$BUTLER_PLATFORM/LATEST/archive/default"
  # `unzip` is present on github-hosted ubuntu-latest; on minimal containers
  # you may need `apt-get install -y unzip` first.
  unzip -o -q "$ZIP" -d "$BUTLER_DIR"
  chmod +x "$BUTLER_PATH"
  rm -f "$ZIP"
fi

echo "butler version:"
"$BUTLER_PATH" -V

# butler picks up the API key from $BUTLER_API_KEY. No file or login step
# needed — see https://itch.io/docs/butler/login.html.
echo ""
echo "Pushing to itch.io ..."
"$BUTLER_PATH" push \
  "$ITCH_JAR" \
  "$ITCH_TARGET:$ITCH_CHANNEL" \
  --userversion "$ITCH_VERSION_TAG"

echo ""
echo "Done. View status: $BUTLER_PATH status $ITCH_TARGET:$ITCH_CHANNEL"
