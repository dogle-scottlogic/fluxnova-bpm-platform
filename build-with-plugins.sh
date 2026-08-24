#!/bin/bash
# Builds fluxnova-bpm-platform, ensuring the agentic subprocess engine plugins
# (fluxnova-engine-plugins-ai-agent-*) are always sourced from the
# dogle-scottlogic/fluxnova-plugins fork rather than any same-versioned SNAPSHOT
# that may be published to the public Sonatype OSSRH snapshots repository.
#
# It does this by:
#   1. Cloning (or updating) the fluxnova-plugins fork at the configured branch.
#   2. Installing it into the local Maven repository (~/.m2).
#   3. Building this repository *without* -U, so Maven reuses the freshly
#      installed local jars instead of checking the network for a
#      same-versioned artifact from elsewhere.
#
# Usage:
#   ./build-with-plugins.sh [maven-args-for-this-repo...]
#
# Examples:
#   ./build-with-plugins.sh -DskipTests -DskipITs
#   ./build-with-plugins.sh clean install
#
# Environment variables:
#   FLUXNOVA_PLUGINS_REPO    Git URL of the plugins fork
#                            (default: https://github.com/dogle-scottlogic/fluxnova-plugins)
#   FLUXNOVA_PLUGINS_BRANCH  Branch/ref to build (default: eval-upates)
#   FLUXNOVA_PLUGINS_DIR     Local checkout directory (default: ../fluxnova-plugins,
#                            i.e. a sibling of this repository)

set -Eeu

FLUXNOVA_PLUGINS_REPO="${FLUXNOVA_PLUGINS_REPO:-https://github.com/dogle-scottlogic/fluxnova-plugins}"
FLUXNOVA_PLUGINS_BRANCH="${FLUXNOVA_PLUGINS_BRANCH:-eval-upates}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FLUXNOVA_PLUGINS_DIR="${FLUXNOVA_PLUGINS_DIR:-$SCRIPT_DIR/../fluxnova-plugins}"

echo "==> Ensuring fluxnova-plugins (${FLUXNOVA_PLUGINS_BRANCH}) is checked out at ${FLUXNOVA_PLUGINS_DIR}"
if [ -d "$FLUXNOVA_PLUGINS_DIR/.git" ]; then
  git -C "$FLUXNOVA_PLUGINS_DIR" fetch origin "$FLUXNOVA_PLUGINS_BRANCH"
  git -C "$FLUXNOVA_PLUGINS_DIR" checkout "$FLUXNOVA_PLUGINS_BRANCH"
  git -C "$FLUXNOVA_PLUGINS_DIR" reset --hard "origin/$FLUXNOVA_PLUGINS_BRANCH"
else
  git clone --branch "$FLUXNOVA_PLUGINS_BRANCH" "$FLUXNOVA_PLUGINS_REPO" "$FLUXNOVA_PLUGINS_DIR"
fi

echo "==> Installing fluxnova-plugins into the local Maven repository"
mvn -f "$FLUXNOVA_PLUGINS_DIR/pom.xml" -B clean install -DskipTests -DskipITs

echo "==> Building fluxnova-bpm-platform (using the locally installed plugin jars, no -U)"
cd "$SCRIPT_DIR"
mvn "$@"
