#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPT="$ROOT_DIR/scripts/bootstrap-server.sh"

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  exit 1
}

assert_contains() {
  local pattern="$1"
  grep -Eq -- "$pattern" "$SCRIPT" || fail "missing pattern: $pattern"
}

[[ -f "$SCRIPT" ]] || fail "scripts/bootstrap-server.sh does not exist"

assert_contains 'EUID'
assert_contains '/opt/tutorspace'
assert_contains 'authorized_keys'
assert_contains 'install -d -m 700'
assert_contains 'chmod 600'
assert_contains 'visudo -cf'
assert_contains 'tutorspace-deploy'
assert_contains 'tutorspace-ci-command'
assert_contains 'restrict,command='
assert_contains 'git clone'

if grep -Eq 'JWT_SECRET=.*MDEy|admin12345|change_me_' "$SCRIPT"; then
  fail "bootstrap script embeds default application secrets"
fi

printf 'PASS: bootstrap script contract\n'
