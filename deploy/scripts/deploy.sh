#!/usr/bin/env bash
# Runs ON THE DROPLET. Called by the GitHub Actions CD workflow.
# Usage: deploy.sh production <image> <tag>
# Deploys the tag, waits for the healthcheck, and rolls back automatically if it fails.
set -euo pipefail

APP_ENV="$1"; IMAGE="$2"; NEW_TAG="$3"
DIR="/opt/airdropx/${APP_ENV}"
CONTAINER="airdropx-${APP_ENV}-api"
cd "$DIR"

PREV_TAG="$(cat .current_tag 2>/dev/null || true)"
echo "==> [$APP_ENV] deploying $IMAGE:$NEW_TAG (previous: ${PREV_TAG:-none})"

compose() { APP_UID="$(id -u)" APP_GID="$(id -g)" APP_ENV="$APP_ENV" IMAGE="$IMAGE" IMAGE_TAG="$1" docker compose -f docker-compose.yml "${@:2}"; }

wait_healthy() {
  for i in $(seq 1 40); do
    status="$(docker inspect -f '{{.State.Health.Status}}' "$CONTAINER" 2>/dev/null || echo missing)"
    echo "    health: $status ($i/40)"
    [ "$status" = "healthy" ] && return 0
    [ "$status" = "unhealthy" ] && return 1
    sleep 5
  done
  return 1
}

compose "$NEW_TAG" pull api
compose "$NEW_TAG" up -d --remove-orphans --force-recreate api   # recreate so new secrets/config are always loaded

if wait_healthy; then
  echo "$NEW_TAG" > .current_tag
  echo "$(date -u +%FT%TZ) $NEW_TAG OK" >> deploy-history.log
  docker image prune -f >/dev/null
  echo "==> [$APP_ENV] deploy OK"
  exit 0
fi

echo "!!> [$APP_ENV] new version is unhealthy. Last logs:"
docker logs --tail 50 "$CONTAINER" || true
echo "$(date -u +%FT%TZ) $NEW_TAG FAILED" >> deploy-history.log

if [ -n "$PREV_TAG" ]; then
  echo "==> rolling back to $PREV_TAG"
  compose "$PREV_TAG" up -d --force-recreate api
  wait_healthy && echo "==> rollback OK" || echo "!!> rollback also unhealthy, check the server"
fi
exit 1
