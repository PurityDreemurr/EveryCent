#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONTAINER_NAME="${EVERYCENT_DEV_CONTAINER:-everycent-dev-dev-1}"

is_inside_dev_container() {
  [[ -f /.dockerenv ]] && [[ "$(pwd -P)" == /workspace* ]]
}

if is_inside_dev_container || [[ "${EVERYCENT_CLI_FORCE_LOCAL:-false}" == "true" ]]; then
  cd "$ROOT_DIR"
  exec scripts/assistant-chat-cli.sh "$@"
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "docker command not found. Please run this script inside the dev container or install Docker CLI." >&2
  exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -Fxq "$CONTAINER_NAME"; then
  echo "Dev container is not running: $CONTAINER_NAME" >&2
  echo "Start it first, then rerun this script." >&2
  exit 1
fi

DOCKER_FLAGS=(-i)
if [[ -t 0 && -t 1 ]]; then
  DOCKER_FLAGS=(-it)
fi

exec docker exec "${DOCKER_FLAGS[@]}" "$CONTAINER_NAME" bash -lc 'cd /workspace && exec scripts/assistant-chat-cli.sh "$@"' -- "$@"
