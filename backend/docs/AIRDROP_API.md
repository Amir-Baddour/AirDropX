# Airdrop API

All endpoints need `Authorization: Bearer <JWT>` (from `/auth/login/oauth`).
Responses use the same shape as the rest of the API: `{ "success", "message", "data" }`.

## Company

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/companies` | `{"name": "Acme"}` | Creates a company; the caller becomes its owner. 409 if the caller already has one. |
| GET | `/companies/me` | | The caller's company. 404 if none. |

Every airdrop call is scoped to the caller's company. A user without a company gets **403**.

## Airdrops

| Method | Path | Body / query | Result |
|---|---|---|---|
| POST | `/airdrops` | `{"name", "description", "token_symbol"}` | New airdrop in **DRAFT** (201) |
| GET | `/airdrops` | `?limit=20&offset=0` | Company's airdrops, newest first |
| GET | `/airdrops/:id` | | Airdrop + `stats` (counts per status, total amount) |
| POST | `/airdrops/:id/recipients` | `{"recipients": [{"address": "0x…", "amount": "10.5"}]}` | Adds recipients (DRAFT only). Duplicates are skipped. |
| GET | `/airdrops/:id/recipients` | `?status=FAILED&limit=50&offset=0` | Recipients with payout status and tx reference |
| POST | `/airdrops/:id/validate` | | DRAFT → **VALIDATED** (needs ≥ 1 recipient) |
| POST | `/airdrops/:id/reopen` | | VALIDATED → **DRAFT** (to edit recipients again) |
| POST | `/airdrops/:id/launch` | | VALIDATED → **PROCESSING**; the worker starts the payouts |
| POST | `/airdrops/:id/cancel` | | Any active state → **CANCELLED**; unpaid recipients are cancelled |
| GET | `/airdrops/:id/events` | | Timeline (created, recipients added, validated, launched, completed…) |

### Lifecycle

```
DRAFT ──validate──▶ VALIDATED ──launch──▶ PROCESSING ──(worker)──▶ COMPLETED
  ▲                    │                        │
  └──────reopen────────┘                        │
  any active state ──────────cancel──────────▶ CANCELLED
```

### Recipient rules
- Address: EVM (`0x` + 40 hex) or Solana (base58, 32–44 chars). EVM duplicates are detected case-insensitively.
- Amount: greater than 0, at most 18 decimals. Send it as a string (`"10.5"`) to keep full precision.
- Up to 10,000 recipients per request. If any row is invalid, nothing is saved and you get **422** with a list of row errors.

### Error codes
| Code | Meaning |
|---|---|
| 400 | Bad input (invalid JSON, id, number) |
| 403 | Caller has no company (create one first) |
| 404 | Airdrop not found (or belongs to another company) |
| 409 | Action not allowed in the current status |
| 422 | Recipient list failed validation (`errors` array lists the rows) |

## Payout worker (mocked)

A background worker runs every few seconds. It claims pending recipients of PROCESSING airdrops with
`FOR UPDATE SKIP LOCKED` (safe with several workers), sends them through `PayoutProvider`
(currently `MockPayoutProvider`, which returns a fake tx hash or a simulated failure), and marks
the airdrop **COMPLETED** when nothing is left. Recipients left in PROCESSING by a crash are re-queued at startup.

Optional `.env` settings (defaults in brackets):
- `MOCK_PAYOUT_FAILURE_RATE` [0.05]: share of simulated failed payouts
- `WORKER_BATCH_SIZE` [20]: recipients per tick
- `WORKER_INTERVAL_SECONDS` [5]: seconds between ticks

## Example

```bash
TOKEN=...   # JWT from login
API=https://64-23-156-149.sslip.io

curl -X POST $API/companies -H "Authorization: Bearer $TOKEN" -d '{"name":"Acme"}'
ID=$(curl -s -X POST $API/airdrops -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Launch drop","token_symbol":"ACME"}' | jq -r .data.id)
curl -X POST $API/airdrops/$ID/recipients -H "Authorization: Bearer $TOKEN" \
  -d '{"recipients":[{"address":"0x1111111111111111111111111111111111111111","amount":"25"}]}'
curl -X POST $API/airdrops/$ID/validate -H "Authorization: Bearer $TOKEN"
curl -X POST $API/airdrops/$ID/launch   -H "Authorization: Bearer $TOKEN"
curl $API/airdrops/$ID -H "Authorization: Bearer $TOKEN"        # watch stats move to COMPLETED
```
