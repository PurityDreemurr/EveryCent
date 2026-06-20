#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:-stage2}"

case "$MODE" in
  stage2)
    TEST_PATTERN="ActionPolicyServiceTest,ActionSchemaValidatorTest,SkillRouterTest"
    ;;
  --all | all)
    TEST_PATTERN="com.everycent.assistant.skill.*Test"
    ;;
  -h | --help)
    cat <<'USAGE'
Usage:
  scripts/verify-skill-stage2-gates.sh        Run stage 2 gate tests only.
  scripts/verify-skill-stage2-gates.sh --all  Run all Skill protocol/gate tests.

Recommended in dev container:
  docker exec -it everycent-dev-dev-1 bash -lc 'cd /workspace && scripts/verify-skill-stage2-gates.sh'
USAGE
    exit 0
    ;;
  *)
    echo "Unknown option: $MODE" >&2
    echo "Use --help for usage." >&2
    exit 2
    ;;
esac

echo "== EveryCent Skill Stage 2 Gate Verification =="
echo "Project: $ROOT_DIR"
echo "Tests:   $TEST_PATTERN"
echo
echo "Gate checklist covered by this run:"
echo "  [1] URL / SQL / Repository / Java-class-like action names are rejected."
echo "  [2] Missing required action arguments return structured INVALID_ACTION errors."
echo "  [3] Delete/remove actions stay blocked even with requiresConfirmation=true."
echo "  [4] Account/authenticate actions stay blocked."
echo "  [5] Confirmation-required non-forbidden actions do not execute before confirmation."
echo "  [6] Router executes matching Skill only after schema + policy pass."
echo

./mvnw \
  -Dskip.installnodenpm=true \
  -Dskip.npm=true \
  -Dtest="$TEST_PATTERN" \
  test

echo
echo "== Stage 2 gate verification passed =="
