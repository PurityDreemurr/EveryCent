#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

CP_FILE="target/assistant-chat-classpath.txt"

./mvnw -q -Dskip.installnodenpm=true -Dskip.npm=true -DskipTests compile dependency:build-classpath -Dmdep.outputFile="$CP_FILE"

JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8" \
java -cp "target/classes:$(cat "$CP_FILE")" com.everycent.assistant.cli.AssistantChatCli "$@"
