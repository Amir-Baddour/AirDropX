#!/usr/bin/env bash
# Runs ON THE SERVER: end-to-end demo of Claim + verification (company side + public side).
set -e
cd /opt/airdropx/production
SECRET=$(grep '^JWT_SECRET=' app.env | cut -d= -f2- | tr -d '\r')
USER_ID=$(docker exec -i airdropx-production-db sh -c 'psql -qtA -U "$POSTGRES_USER" "$POSTGRES_DB"' <<'SQL' | head -1
INSERT INTO users (username, provider, provider_id, role_id)
VALUES ('demo-user', 'TEST', 'demo', (SELECT id FROM roles WHERE name = 'USER'))
ON CONFLICT (username) DO UPDATE SET username = EXCLUDED.username
RETURNING id;
SQL
)
python3 - "$SECRET" "$USER_ID" <<'PY'
import sys, base64, hmac, hashlib, json, time, random, urllib.request, urllib.error
secret, user_id = sys.argv[1], sys.argv[2]
API = "https://64-23-156-149.sslip.io"

def b64(x): return base64.urlsafe_b64encode(x).rstrip(b"=").decode()
now = int(time.time())
header = b64(json.dumps({"alg": "HS256"}).encode())
payload = b64(json.dumps({"sub": user_id, "iat": now, "exp": now + 3600}).encode())
sig = b64(hmac.new(base64.b64decode(secret), f"{header}.{payload}".encode(), hashlib.sha256).digest())
token = f"{header}.{payload}.{sig}"

def call(method, path, body=None, auth=True):
    headers = {"Content-Type": "application/json"}
    if auth: headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(API + path, method=method,
        data=None if body is None else json.dumps(body).encode(), headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, json.loads(r.read() or b"{}")
    except urllib.error.HTTPError as e:
        raw = e.read()
        try: return e.code, json.loads(raw)
        except Exception: return e.code, {"message": raw.decode(errors="replace")[:200]}

def show(step, status, res):
    print(f"{step:<44} HTTP {status}  {res.get('message', '')}")

wallet = lambda: "0x" + "".join(random.choice("0123456789abcdef") for _ in range(40))

print("== Company side ==")
s, r = call("POST", "/companies", {"name": "Demo Company"})
s, r = call("POST", "/airdrops", {"name": "Quiz drop", "token_symbol": "DEMO", "description": "Answer and claim"}); show("1. Create airdrop", s, r)
aid = r["data"]["id"]
s, r = call("POST", f"/airdrops/{aid}/tasks", {"type": "QUIZ", "title": "Read our docs",
    "config": {"questions": [{"question": "Which chain do we launch on?", "options": ["Bitcoin", "Ethereum"], "answer": 1}]}}); show("2. Add QUIZ task", s, r)
quiz = r["data"]["id"]
s, r = call("POST", f"/airdrops/{aid}/tasks", {"type": "SECRET_CODE", "title": "Watch the launch video",
    "config": {"code": "MOON2026", "hint": "Shown at the end of the video"}}); show("3. Add SECRET_CODE task", s, r)
code = r["data"]["id"]
print("   stored config:", r["data"]["config"])
s, r = call("POST", f"/airdrops/{aid}/claims/open", {"claim_amount": "25", "max_claims": 100}); show("4. Open claims (25 DEMO each)", s, r)

print("\n== Public side (no login) ==")
s, r = call("GET", f"/public/airdrops/{aid}", auth=False); show("5. Public campaign page", s, r)
print("   tasks:", [(t["type"], t["config"]) for t in r["data"]["tasks"]])
me = wallet()
s, r = call("POST", f"/public/airdrops/{aid}/claims", {"address": me, "answers": {quiz: [0], code: "moon2026"}}, auth=False)
show("6. Claim with wrong quiz answer (expect 422)", s, r); print("   errors:", r.get("errors"))
s, r = call("POST", f"/public/airdrops/{aid}/claims", {"address": me, "answers": {quiz: [1], code: "moon2026"}}, auth=False)
show("7. Claim with correct answers", s, r)
claim_token = r["data"]["claim_token"]; print("   status:", r["data"]["status"])
s, r = call("POST", f"/public/airdrops/{aid}/claims", {"address": me, "answers": {quiz: [1], code: "moon2026"}}, auth=False)
show("8. Same wallet again (expect 409)", s, r)

print("\n== Manual review ==")
s, r = call("POST", "/airdrops", {"name": "Tweet drop", "token_symbol": "DEMO"}); aid2 = r["data"]["id"]
s, r = call("POST", f"/airdrops/{aid2}/tasks", {"type": "MANUAL_PROOF", "title": "Tweet about us",
    "config": {"instructions": "Paste the link to your tweet"}}); proof = r["data"]["id"]
call("POST", f"/airdrops/{aid2}/claims/open", {"claim_amount": "10"})
s, r = call("POST", f"/public/airdrops/{aid2}/claims", {"address": wallet(), "answers": {proof: "https://x.com/demo/status/1"}}, auth=False)
show("9. Claim with proof", s, r); print("   status:", r["data"]["status"])
s, r = call("GET", f"/airdrops/{aid2}/claims?status=NEEDS_REVIEW"); show("10. Review queue", s, r)
print("   counts:", r["data"]["counts"])
cid = r["data"]["items"][0]["id"]
s, r = call("POST", f"/airdrops/{aid2}/claims/{cid}/approve"); show("11. Approve claim", s, r)

print("\n== Rate limit ==")
codes = [call("POST", f"/public/airdrops/{aid}/claims", {"address": "bad"}, auth=False)[0] for _ in range(4)]
print(f"{'12. Hammer submit endpoint':<44} HTTP {codes}  (429 = rate limited)")

print("\n== Payout ==")
s, r = call("POST", f"/airdrops/{aid}/validate"); show("13. Validate (closes claims)", s, r)
s, r = call("POST", f"/airdrops/{aid}/launch"); show("14. Launch", s, r)
for i in range(12):
    time.sleep(5)
    s, r = call("GET", f"/public/claims/{claim_token}", auth=False)
    if r["data"]["payout_status"] == "COMPLETED": break
show("15. Claimant checks status by token", s, r)
d = r["data"]; print(f"   claim {d['status']}, payout {d['payout_status']}, tx {d['tx_ref'][:20]}…")
PY
