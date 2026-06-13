#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_COMMAND="/usr/local/sbin/tutorspace-deploy"
ORIGINAL_COMMAND="${SSH_ORIGINAL_COMMAND:-}"

if [[ "$ORIGINAL_COMMAND" =~ ^sudo[[:space:]]+/usr/local/sbin/tutorspace-deploy[[:space:]]+([0-9a-fA-F]{40})$ ]]; then
  exec sudo "$DEPLOY_COMMAND" "${BASH_REMATCH[1]}"
fi

printf 'This SSH key is restricted to TutorSpace deployments.\n' >&2
exit 126
