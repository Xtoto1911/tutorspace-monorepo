#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPT="$ROOT_DIR/scripts/ci-ssh-command.sh"

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  exit 1
}

[[ -f "$SCRIPT" ]] || fail "scripts/ci-ssh-command.sh does not exist"

grep -Eq 'SSH_ORIGINAL_COMMAND' "$SCRIPT" || fail "SSH_ORIGINAL_COMMAND is not validated"
grep -Eq '\[0-9a-fA-F\].*40' "$SCRIPT" || fail "full commit SHA is not required"
grep -Eq 'DEPLOY_COMMAND=.*/usr/local/sbin/tutorspace-deploy' "$SCRIPT" || fail "deploy command path is missing"
grep -Eq 'exec sudo .*DEPLOY_COMMAND' "$SCRIPT" || fail "validated command is not executed"

if grep -Eq 'eval|bash -c|sh -c' "$SCRIPT"; then
  fail "forced command must not evaluate user-controlled shell input"
fi

printf 'PASS: CI SSH forced-command contract\n'
