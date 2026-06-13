#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="${TUTORSPACE_APP_DIR:-/opt/tutorspace}"
DEPLOY_SHA="${1:-}"
COMPOSE_FILES=(-f docker-compose.yml -f docker-compose.prod.yml)

if [[ ! "$DEPLOY_SHA" =~ ^[0-9a-fA-F]{40}$ ]]; then
  printf 'Usage: %s <40-character-git-sha>\n' "$0" >&2
  exit 2
fi

exec 9>/var/lock/tutorspace-deploy.lock
if ! flock -n 9; then
  printf 'Another TutorSpace deployment is already running.\n' >&2
  exit 3
fi

cd "$APP_DIR"

[[ -d .git ]] || {
  printf '%s is not a Git repository.\n' "$APP_DIR" >&2
  exit 4
}

[[ -f .env ]] || {
  printf '%s/.env is missing.\n' "$APP_DIR" >&2
  exit 5
}

git fetch --prune origin main
git cat-file -e "${DEPLOY_SHA}^{commit}"
git merge-base --is-ancestor "$DEPLOY_SHA" origin/main || {
  printf 'Commit %s is not reachable from origin/main.\n' "$DEPLOY_SHA" >&2
  exit 6
}
git checkout --detach "$DEPLOY_SHA"
install -m 0755 -o root -g root scripts/deploy.sh /usr/local/sbin/tutorspace-deploy

docker compose --env-file .env "${COMPOSE_FILES[@]}" config --quiet
docker compose --env-file .env "${COMPOSE_FILES[@]}" up -d --build --remove-orphans

for attempt in {1..30}; do
  if curl --fail --silent --show-error http://127.0.0.1/health >/dev/null; then
    printf 'TutorSpace deployed successfully at %s.\n' "$DEPLOY_SHA"
    exit 0
  fi

  if [[ "$attempt" -eq 30 ]]; then
    break
  fi

  sleep 5
done

docker compose --env-file .env "${COMPOSE_FILES[@]}" ps >&2
docker compose --env-file .env "${COMPOSE_FILES[@]}" logs --tail=100 gateway >&2
printf 'Health check failed after deployment of %s.\n' "$DEPLOY_SHA" >&2
exit 7
