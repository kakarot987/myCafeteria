#!/usr/bin/env sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ENV_FILE="$ROOT/myCafeteriaD/.env"
COMPOSE_FILE="$ROOT/myCafeteriaD/docker-compose.yml"

if [ ! -f "$ENV_FILE" ]; then
  cp "$ROOT/myCafeteriaD/.env.example" "$ENV_FILE"
  printf '%s\n' "Created myCafeteriaD/.env from the example." >&2
  printf '%s\n' "Replace the placeholder passwords and JWT secret in that file, then rerun ./start.sh" >&2
  exit 1
fi

if grep -Eq '^(MYSQLDB_ROOT_PASSWORD|MYSQLDB_PASSWORD|APP_JWT_SECRET|APP_BOOTSTRAP_ADMIN_PASSWORD)=replace-' "$ENV_FILE"; then
  printf '%s\n' "Update placeholder secrets in myCafeteriaD/.env before starting." >&2
  exit 1
fi

case "${1:-up}" in
  up)
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build
    printf '\n%s\n' "Frontend: http://localhost:${FRONTEND_LOCAL_PORT:-4200}" \
      "Backend:  http://localhost:${SPRING_LOCAL_PORT:-8080}" \
      "View logs: ./start.sh logs" "Stop: ./start.sh down"
    ;;
  down)
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" down
    ;;
  logs)
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs -f
    ;;
  *)
    printf '%s\n' "Usage: ./start.sh [up|down|logs]" >&2
    exit 2
    ;;
esac
