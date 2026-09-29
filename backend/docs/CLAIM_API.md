# Claim + verification API

Companies attach **tasks** to a DRAFT airdrop and open a **public claim page**. Anyone can claim with a wallet address by completing the tasks. No login is needed for the claim itself.

- **Auto-verified tasks** (quiz, secret code) are checked instantly. If one fails, the claim is refused with `422` and nothing is stored, so the claimant can try again.
- **Manual tasks** (proof link or text) put the claim in a **review queue**.
- An **approved claim becomes a recipient** of the airdrop with the fixed `claim_amount`. From there, the normal flow applies: validate → launch → worker pays → the claimant sees the tx ref.

All responses use the usual shape `{ "success", "message", "data" }`. Company endpoints need `Authorization: Bearer <jwt>`.

## Task types

| Type | Config (company sends) | Answer (claimant sends) | Verified |
|---|---|---|---|
| `QUIZ` | `{"questions":[{"question":"...","options":["a","b"],"answer":1}]}` (1–10 questions, 2–6 options) | `[1, 0, ...]`: option index per question | Auto. The response never says which question was wrong. |
| `SECRET_CODE` | `{"code":"MOON2026","hint":"optional"}` (4–64 chars) | `"moon2026"`: case and spaces ignored | Auto. Only a SHA-256 hash is stored; the comparison is constant-time. |
| `MANUAL_PROOF` | `{"instructions":"Paste your tweet link"}` | `"https://x.com/..."` (1–500 chars) | Manual review. |

The claimant view of a task never includes quiz answers or code hashes.

## Company endpoints

| Method | Path | Notes |
|---|---|---|
| POST | `/airdrops/:id/tasks` | `{type, title, description?, config}`. Only while DRAFT with claims closed. Max 10 tasks. |
| GET | `/airdrops/:id/tasks` | Full config (quiz answers included). |
| DELETE | `/airdrops/:id/tasks/:taskId` | Only while DRAFT with claims closed. |
| POST | `/airdrops/:id/claims/open` | `{claim_amount, max_claims?}`. Returns `public_path`. |
| POST | `/airdrops/:id/claims/close` | |
| GET | `/airdrops/:id/claims?status=NEEDS_REVIEW&limit=&offset=` | `{counts, items}`. Each item includes its task results and proof. |
| POST | `/airdrops/:id/claims/:claimId/approve` | NEEDS_REVIEW → APPROVED, and the claimant is added as a recipient. |
| POST | `/airdrops/:id/claims/:claimId/reject` | `{reason}`. NEEDS_REVIEW → REJECTED. The claimant sees the reason. |

Validating the airdrop **closes claims automatically**, so the recipient list is frozen before launch.

## Public endpoints (no login)

| Method | Path | Notes |
|---|---|---|
| GET | `/public/airdrops/:id` | Campaign and tasks. `404` unless claims are open. |
| POST | `/public/airdrops/:id/claims` | `{address, answers: {"<taskId>": <answer>}}` → `201 {claim_id, status, claim_token}` |
| GET | `/public/claims/:token` | Claim status, payout status and tx ref. |

`claim_token` is shown **once**. Only its SHA-256 hash is stored, so the token works like a password for the status page.

## Anti-abuse

| Rule | Where | Response |
|---|---|---|
| One claim per wallet per airdrop (EVM addresses case-insensitive) | `UNIQUE(airdrop_id, address_key)` | 409 |
| Claim cap `max_claims` | Checked with the airdrop row locked (`SELECT … FOR UPDATE`), so parallel submits can't overshoot | 409 |
| Max 3 claims per network per airdrop | `ip_hash` (SHA-256 of the IP; raw IPs are never stored) | 429 |
| Rate limit: 5 submits/min and 60 reads/min per IP | `Middleware/RateLimiter` (in-memory sliding window) | 429 + `Retry-After` |

## Status codes

`400` bad input · `401/403` auth · `404` not found / claims closed · `409` wrong state, duplicate or cap reached · `422` tasks not completed (`errors` lists which) · `429` too many requests

## Data model

```
airdrops            + claims_open, claim_amount, max_claims
airdrop_tasks       id, airdrop_id → airdrops, type, title, description, config jsonb, position
claims              id, airdrop_id → airdrops, address, address_key, status, token_hash UNIQUE,
                    ip_hash, reject_reason, reviewed_by, reviewed_at   UNIQUE(airdrop_id, address_key)
claim_task_results  claim_id → claims, task_id → airdrop_tasks, passed, needs_review, proof, detail
```

## Adding a task type

Implement `Core/Task/Verifier/TaskVerifier` (`prepareConfig`, `verify`, `publicConfig`), add the enum value to `TaskType` and the migration CHECK, and register the class in `TaskVerifierRegistry`. Nothing else changes.
