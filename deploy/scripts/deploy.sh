#!/usr/bin/env bash
# Runs ON THE DROPLET. Called by the GitHub Actions CD workflow.
# Usage: deploy.sh production <image> <tag>
# Deploys the API and the frontend for this tag, waits for both healthchecks,
# and rolls back automatically if either is unhealthy.
set -euo pipefail

APP_ENV="$1"; IMAGE="$2"; NEW_TAG="$3"
DIR="/opt/airdropx/${APP_ENV}"
API="airdropx-${APP_ENV}-api"
WEB="airdropx-${APP_ENV}-web"
cd "$DIR"

PREV_TAG="$(cat .current_tag 2>/dev/null || true)"
echo "==> [$APP_ENV] deploying $IMAGE:$NEW_TAG (previous: ${PREV_TAG:-none})"

compose() { APP_UID="$(id -u)" APP_GID="$(id -g)" APP_ENV="$APP_ENV" IMAGE="$IMAGE" IMAGE_TAG="$1" docker compose -f docker-compose.yml "${@:2}"; }

wait_healthy() { # <container>
  for i in $(seq 1 40); do
    status="$(docker inspect -f '{{.State.Health.Status}}' "$1" 2>/dev/null || echo missing)"
    echo "    $1 health: $status ($i/40)"
    [ "$status" = "healthy" ] && return 0
    [ "$status" = "unhealthy" ] && return 1
    sleep 5
  done
  return 1
}

# Older tags were built before the frontend existed: deploy only the API for those.
SERVICES="api"
if docker manifest inspect "$IMAGE-web:$NEW_TAG" >/dev/null 2>&1 || docker image inspect "$IMAGE-web:$NEW_TAG" >/dev/null 2>&1; then
  SERVICES="api web"
fi

compose "$NEW_TAG" pull $SERVICES
compose "$NEW_TAG" up -d --remove-orphans --force-recreate $SERVICES   # recreate so new secrets/config are always loaded

ok=true
wait_healthy "$API" || ok=false
if [ "$ok" = true ] && [[ "$SERVICES" == *web* ]]; then wait_healthy "$WEB" || ok=false; fi

if [ "$ok" = true ]; then
  echo "$NEW_TAG" > .current_tag
  echo "$(date -u +%FT%TZ) $NEW_TAG OK ($SERVICES)" >> deploy-history.log
  docker image prune -f >/dev/null
  echo "==> [$APP_ENV] deploy OK"
  exit 0
fi

echo "!!> [$APP_ENV] new version is unhealthy. Last logs:"
docker logs --tail 50 "$API" || true
[[ "$SERVICES" == *web* ]] && docker logs --tail 20 "$WEB" || true
echo "$(date -u +%FT%TZ) $NEW_TAG FAILED" >> deploy-history.log

if [ -n "$PREV_TAG" ]; then
  echo "==> rolling back to $PREV_TAG"
  compose "$PREV_TAG" up -d --force-recreate api
  compose "$PREV_TAG" up -d --force-recreate web 2>/dev/null || echo "    (no frontend image for $PREV_TAG; frontend left as is)"
  wait_healthy "$API" && echo "==> rollback OK" || echo "!!> rollback also unhealthy, check the server"
fi
exit 1
