#!/bin/sh
# Maven Wrapper script — downloads the pinned Maven version on first run
# then delegates all arguments to the real mvn binary.

set -e

SCRIPT_DIR="$(dirname "$0")"
WRAPPER_PROPS="$SCRIPT_DIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$WRAPPER_PROPS" ]; then
    echo "[mvnw] ERROR: $WRAPPER_PROPS not found" >&2
    exit 1
fi

DIST_URL=$(grep -m1 '^distributionUrl=' "$WRAPPER_PROPS" | cut -d= -f2-)
if [ -z "$DIST_URL" ]; then
    echo "[mvnw] ERROR: distributionUrl not found in $WRAPPER_PROPS" >&2
    exit 1
fi

DIST_FILENAME=$(basename "$DIST_URL")
DIST_NAME="${DIST_FILENAME%-bin.zip}"

DIST_CACHE="$HOME/.m2/wrapper/dists/$DIST_NAME"
MVN_CMD="$DIST_CACHE/$DIST_NAME/bin/mvn"

if [ ! -x "$MVN_CMD" ]; then
    echo "[mvnw] Maven $DIST_NAME not found in cache. Downloading..."
    mkdir -p "$DIST_CACHE"
    if command -v curl >/dev/null 2>&1; then
        curl -fL "$DIST_URL" -o "$DIST_CACHE/$DIST_FILENAME"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "$DIST_URL" -O "$DIST_CACHE/$DIST_FILENAME"
    else
        echo "[mvnw] ERROR: neither curl nor wget available" >&2
        exit 1
    fi
    unzip -q "$DIST_CACHE/$DIST_FILENAME" -d "$DIST_CACHE"
    rm -f "$DIST_CACHE/$DIST_FILENAME"
    chmod +x "$MVN_CMD"
    echo "[mvnw] Maven downloaded to $DIST_CACHE"
fi

exec "$MVN_CMD" "$@"
