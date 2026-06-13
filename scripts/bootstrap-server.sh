#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="/opt/tutorspace"
DEPLOY_USER="kirill"
DEPLOY_WRAPPER="/usr/local/sbin/tutorspace-deploy"
CI_COMMAND="/usr/local/sbin/tutorspace-ci-command"
SUDOERS_FILE="/etc/sudoers.d/tutorspace-deploy"
REPOSITORY_URL="${1:-}"
CI_PUBLIC_KEY="${2:-}"

if [[ "$EUID" -ne 0 ]]; then
  printf 'Run this script as root.\n' >&2
  exit 2
fi

if [[ -z "$REPOSITORY_URL" || -z "$CI_PUBLIC_KEY" ]]; then
  printf 'Usage: %s <git-ssh-repository-url> <ci-public-key>\n' "$0" >&2
  exit 3
fi

for command in git docker curl flock visudo; do
  command -v "$command" >/dev/null || {
    printf 'Required command is missing: %s\n' "$command" >&2
    exit 4
  }
done

id "$DEPLOY_USER" >/dev/null 2>&1 || {
  printf 'Required deployment user does not exist: %s\n' "$DEPLOY_USER" >&2
  exit 5
}

DEPLOY_HOME="$(getent passwd "$DEPLOY_USER" | cut -d: -f6)"
INSTALL_KEY="$DEPLOY_HOME/.ssh/authorized_keys"
AUTHORIZED_KEY_ENTRY="restrict,command=\"$CI_COMMAND\" $CI_PUBLIC_KEY"

install -d -m 700 -o "$DEPLOY_USER" -g "$DEPLOY_USER" "$DEPLOY_HOME/.ssh"
touch "$INSTALL_KEY"
chown "$DEPLOY_USER:$DEPLOY_USER" "$INSTALL_KEY"
chmod 600 "$INSTALL_KEY"

if ! grep -Fqx "$AUTHORIZED_KEY_ENTRY" "$INSTALL_KEY"; then
  KEY_FILE="$(mktemp)"
  grep -Fvx "$CI_PUBLIC_KEY" "$INSTALL_KEY" >"$KEY_FILE" || true
  printf '%s\n' "$AUTHORIZED_KEY_ENTRY" >>"$KEY_FILE"
  install -m 600 -o "$DEPLOY_USER" -g "$DEPLOY_USER" "$KEY_FILE" "$INSTALL_KEY"
  rm -f "$KEY_FILE"
fi

if [[ -d "$APP_DIR/.git" ]]; then
  git -C "$APP_DIR" remote set-url origin "$REPOSITORY_URL"
  git -C "$APP_DIR" fetch --prune origin main
else
  [[ ! -e "$APP_DIR" ]] || {
    printf '%s exists but is not a Git repository.\n' "$APP_DIR" >&2
    exit 6
  }
  git clone "$REPOSITORY_URL" "$APP_DIR"
fi

git config --global --add safe.directory "$APP_DIR"
install -m 0755 -o root -g root "$APP_DIR/scripts/deploy.sh" "$DEPLOY_WRAPPER"
install -m 0755 -o root -g root "$APP_DIR/scripts/ci-ssh-command.sh" "$CI_COMMAND"

cat >"$SUDOERS_FILE" <<EOF
$DEPLOY_USER ALL=(root) NOPASSWD: $DEPLOY_WRAPPER *
EOF
chmod 440 "$SUDOERS_FILE"
visudo -cf "$SUDOERS_FILE"

if [[ ! -f "$APP_DIR/.env" ]]; then
  printf 'Create %s/.env before the first deployment.\n' "$APP_DIR"
fi

printf 'Server bootstrap complete. Deployment wrapper: %s\n' "$DEPLOY_WRAPPER"
