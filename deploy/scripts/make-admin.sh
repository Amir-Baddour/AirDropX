#!/usr/bin/env bash
# Runs ON THE SERVER (as root). Gives a user the platform SUPERADMIN role (or takes it away).
#   bash make-admin.sh demo-user            -> make 'demo-user' a platform admin
#   bash make-admin.sh "Amir Baddour"       -> by username (Google sign-in uses your Google name)
#   bash make-admin.sh demo-user --revoke   -> back to a normal USER
#   bash make-admin.sh --list               -> show all users and roles
set -euo pipefail
cd /opt/airdropx/production
psql_run() { docker exec -i airdropx-production-db sh -c 'psql -qtA -F " | " -U "$POSTGRES_USER" "$POSTGRES_DB" -v u="$1" -v r="$2"' _ "$@"; }

if [ "${1:-}" = "--list" ]; then
  psql_run "" "" <<'SQL'
SELECT u.username, r.name FROM users u JOIN roles r ON r.id = u.role_id ORDER BY u.created_at;
SQL
  exit 0
fi

USERNAME="${1:?usage: make-admin.sh <username> [--revoke] | --list}"
ROLE="SUPERADMIN"; [ "${2:-}" = "--revoke" ] && ROLE="USER"
UPDATED=$(psql_run "$USERNAME" "$ROLE" <<'SQL'
UPDATE users SET role_id = (SELECT id FROM roles WHERE name = :'r') WHERE username = :'u' RETURNING username;
SQL
)
[ -n "$UPDATED" ] || { echo "No user named '$USERNAME'. Run: bash make-admin.sh --list" >&2; exit 1; }
echo "'$USERNAME' is now $ROLE. Sign out and in again (or refresh) to see the admin area."
