# Platform admin API

Everything under `/admin/*` requires the **SUPERADMIN** role, checked with the existing
`AuthorizationMiddleware.requireSuperAdmin`. Everyone else gets `403`.

| Method | Path | Notes |
|---|---|---|
| GET | `/admin/access` | `200 {admin: true}` for admins. The frontend uses it to show the admin area. |
| GET | `/admin/stats` | Users, and companies, airdrops, claims and recipients broken down by status. Also claims in the last 24h. |
| GET | `/admin/companies?q=&status=ACTIVE\|SUSPENDED&limit=&offset=` | Search by company name or owner username (case-insensitive; `%` and `_` are escaped). Includes counts of members, airdrops and claims. |
| POST | `/admin/companies/:id/suspend` | `{reason}` is required. `409` if the company is already suspended. |
| POST | `/admin/companies/:id/restore` | `{note?}`. `409` if the company is not suspended. |
| GET | `/admin/audit?limit=` | Admin actions, newest first: who, what, target, reason. |
| GET | `/admin/activity?limit=` | Latest airdrop events across all companies. |

## What suspension does

Suspending a company freezes it; restoring it resumes everything as it was. Nothing is deleted.

- **Members:** every company endpoint returns `403`. `GET /companies/me` returns `code: COMPANY_SUSPENDED` with the reason, and the dashboard shows it.
- **Public claim pages:** `404`. New claims are refused.
- **Payout worker:** skips the company's airdrops (pending payouts wait).
- **Audit:** the status change and the audit row are written in one transaction. A failed action writes nothing.

## Making someone an admin

Run this on the server: `bash make-admin.sh <username>`. It also supports `--revoke` and `--list`.
