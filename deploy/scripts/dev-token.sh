#!/usr/bin/env bash
# Runs ON THE SERVER (as root). Prints a 24-hour sign-in token for an existing user,
# for the "Developer: sign in with an access token" option on the login page.
# Useful until Google sign-in is configured.
#   bash dev-token.sh               -> token for 'demo-user' (created by claim-demo.sh)
#   bash dev-token.sh "Some Name"   -> token for that username
set -euo pipefail
USERNAME="${1:-demo-user}"
cd /opt/airdropx/production
SECRET=$(grep '^JWT_SECRET=' app.env | cut -d= -f2- | tr -d '\r')
USER_ID=$(docker exec -i airdropx-production-db sh -c 'psql -qtA -U "$POSTGRES_USER" "$POSTGRES_DB" -v u="$1"' _ "$USERNAME" <<'SQL'
SELECT id FROM users WHERE username = :'u';
SQL
)
[ -n "$USER_ID" ] || { echo "No user named '$USERNAME'" >&2; exit 1; }
python3 - "$SECRET" "$USER_ID" <<'PY'
import sys, base64, hmac, hashlib, json, time
secret, user_id = sys.argv[1], sys.argv[2]
b64 = lambda x: base64.urlsafe_b64encode(x).rstrip(b"=").decode()
now = int(time.time())
h = b64(json.dumps({"alg": "HS256"}).encode())
p = b64(json.dumps({"sub": user_id, "iat": now, "exp": now + 86400}).encode())
s = b64(hmac.new(base64.b64decode(secret), f"{h}.{p}".encode(), hashlib.sha256).digest())
print(f"{h}.{p}.{s}")
PY
