#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPT="$ROOT_DIR/scripts/deploy.sh"

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  exit 1
}

assert_contains() {
  local pattern="$1"
  grep -Eq -- "$pattern" "$SCRIPT" || fail "missing pattern: $pattern"
}

[[ -f "$SCRIPT" ]] || fail "scripts/deploy.sh does not exist"

assert_contains '\^\[0-9a-f\]\{40\}\$|\^\[0-9a-fA-F\]\{40\}\$'
assert_contains 'flock'
assert_contains 'git fetch .*origin .*main'
assert_contains 'merge-base --is-ancestor'
assert_contains 'git checkout --detach'
assert_contains 'docker compose'
assert_contains 'docker-compose\.prod\.yml'
assert_contains 'config --quiet'
assert_contains 'up -d --build --remove-orphans'
assert_contains '127\.0\.0\.1/health'

if grep -Eq 'down[[:space:]].*(-v|--volumes)|volume[[:space:]]+rm|system[[:space:]]+prune' "$SCRIPT"; then
  fail "deploy script contains a data-destructive Docker command"
fi

printf 'PASS: deploy script contract\n'
