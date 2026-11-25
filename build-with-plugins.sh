#!/bin/bash
# Builds fluxnova-bpm-platform together with the agentic subprocess engine plugins
# (fluxnova-engine-plugins-ai-agent-*) from the dogle-scottlogic/fluxnova-plugins fork,
# so the distribution never picks up a stale or foreign same-versioned jar from ~/.m2
# or a remote snapshot repository.
#
# The two repositories depend on each other:
#   - this repo's engine/ module depends on fluxnova-engine-plugins-ai-agentic-subprocess
#     (from fluxnova-plugins), and
#   - fluxnova-plugins depends on fluxnova-engine-plugin-otel (built only in this repo).
# The build breaks that cycle in three stages:
#   1. Install this repo's root/engine-plugins parent POMs and the OTEL engine plugin.
#      The OTEL plugin compiles against the released fluxnova-engine of the same version
#      (resolved from Maven Central), so the rest of the engine is not needed yet.
#   2. Build and install the agentic subprocess plugins (agentic-subprocess/pom.xml) from
#      the fluxnova-plugins checkout, including their tests unless skipped.
#   3. Build this repository *without* -U, so Maven reuses the freshly installed jars.
#
# Usage:
#   ./build-with-plugins.sh [maven-args-for-stage-3...]
#
# Examples:
#   ./build-with-plugins.sh clean install -DskipTests -DskipITs
#
# Environment variables:
#   FLUXNOVA_PLUGINS_REPO        Git URL of the plugins fork, used only when cloning
#                                (default: https://github.com/dogle-scottlogic/fluxnova-plugins)
#   FLUXNOVA_PLUGINS_BRANCH      Branch to clone when no checkout exists (default: upgrade-3.1.1)
#   FLUXNOVA_PLUGINS_DIR         Plugins checkout (default: ../fluxnova-plugins, a sibling of
#                                this repository). An existing checkout is built as-is, including
#                                local unpushed commits; it is never reset or switched.
#   FLUXNOVA_PLUGINS_SKIP_TESTS  Set to true to skip the plugin tests in stage 2 (default: false)

set -Eeuo pipefail

FLUXNOVA_PLUGINS_REPO="${FLUXNOVA_PLUGINS_REPO:-https://github.com/dogle-scottlogic/fluxnova-plugins}"
FLUXNOVA_PLUGINS_BRANCH="${FLUXNOVA_PLUGINS_BRANCH:-upgrade-3.1.1}"
FLUXNOVA_PLUGINS_SKIP_TESTS="${FLUXNOVA_PLUGINS_SKIP_TESTS:-false}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FLUXNOVA_PLUGINS_DIR="${FLUXNOVA_PLUGINS_DIR:-$SCRIPT_DIR/../fluxnova-plugins}"

if [ -d "$FLUXNOVA_PLUGINS_DIR/.git" ]; then
  echo "==> Using existing fluxnova-plugins checkout at ${FLUXNOVA_PLUGINS_DIR}" \
       "(branch $(git -C "$FLUXNOVA_PLUGINS_DIR" branch --show-current)," \
       "commit $(git -C "$FLUXNOVA_PLUGINS_DIR" rev-parse --short HEAD))"
else
  echo "==> Cloning fluxnova-plugins (${FLUXNOVA_PLUGINS_BRANCH}) into ${FLUXNOVA_PLUGINS_DIR}"
  git clone --branch "$FLUXNOVA_PLUGINS_BRANCH" "$FLUXNOVA_PLUGINS_REPO" "$FLUXNOVA_PLUGINS_DIR"
fi

cd "$SCRIPT_DIR"

echo "==> [1/3] Installing parent POMs and the OTEL engine plugin"
mvn -B -pl .,engine-plugins,engine-plugins/otel-plugin install -DskipTests -DskipITs

echo "==> [2/3] Installing the agentic subprocess plugins from ${FLUXNOVA_PLUGINS_DIR}"
mvn -B -f "$FLUXNOVA_PLUGINS_DIR/pom.xml" -N install -DskipTests -DskipITs
plugin_test_args=()
if [ "$FLUXNOVA_PLUGINS_SKIP_TESTS" = true ]; then
  plugin_test_args=(-DskipTests -DskipITs)
fi
mvn -B -f "$FLUXNOVA_PLUGINS_DIR/agentic-subprocess/pom.xml" clean install "${plugin_test_args[@]}"

echo "==> [3/3] Building fluxnova-bpm-platform (using the locally installed jars, no -U)"
mvn -B "$@"
