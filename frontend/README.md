# AirdropX frontend

React 19 · TypeScript · Vite · Tailwind CSS 4 · shadcn/ui-style components (Radix) · TanStack Query & Table · react-hook-form + zod · Motion · Sonner · Lucide

## Run locally

```bash
npm install
API_URL=http://localhost:8080 npm run dev      # /api is proxied to the Spark backend
```

Open http://localhost:5173. Without Google keys, use **Developer: sign in with an access token** on the login page.

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Dev server with hot reload |
| `npm run build` | Type-check and build to `dist/` |
| `npm run lint` | ESLint |
| `npm run typecheck` | TypeScript only |
| `npm test` | Unit tests (Vitest) |

## Structure

```
src/
  lib/            API client, auth/session, React Query hooks, types, helpers
  components/ui/  shadcn-style primitives (button, card, dialog, tabs, table…)
  components/     layout + shared pieces (status badges, glow background…)
  features/
    landing/      landing, how-it-works, 404
    auth/         Google sign-in, callback, route guard
    dashboard/    company onboarding, airdrop list, create dialog
    airdrop/      airdrop detail: overview, recipients, tasks, claims review, timeline
    public/       public claim flow + claim status page (glass design)
```

## How it's served

One domain for everything. The edge Caddy sends `/api/*` to the Spark API (without the `/api` prefix) and everything else to this app's container, so there is no CORS and no extra domain.

`VITE_GOOGLE_CLIENT_ID` is read at build time (GitHub repository variable `GOOGLE_CLIENT_ID` in CD).
