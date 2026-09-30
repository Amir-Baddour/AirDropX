# AirdropX: CI/CD setup (DigitalOcean droplet 64.23.156.149)

Repo: `github.com/Amir-Baddour/AirDropX`. Image: `ghcr.io/amir-baddour/airdropx`.
Live URL once deployed: **https://64-23-156-149.sslip.io/health**

None of these files change the backend code. The backend lives in `backend/`, and everything else sits beside it:

```
AirDropX/
├── .gitignore                      keeps .env, target/, .idea/ out of Git
├── README.md
├── backend/                        ← your backend, unchanged (pom.xml, src/)
│   ├── Dockerfile                  packages the existing Maven build
│   └── .dockerignore
├── deploy/
│   ├── proxy/                      Caddy: automatic HTTPS
│   ├── app/docker-compose.yml      API + its own Postgres (memory-limited for a 1 GB droplet)
│   └── scripts/
│       ├── deploy.sh               runs on the server: deploy, wait for healthy, auto-rollback
│       └── server-setup.sh         one-time server preparation
└── .github/workflows/
    ├── ci.yml                      build, secret scan, dependency scan, Docker build (every PR and push)
    ├── cd.yml                      image → production → health check (auto-rollback on failure)
    ├── _deploy.yml                 shared deploy steps
    └── rollback.yml                manual "redeploy an older version" button
```

> **Do the steps in this order.** The first push to `main` starts a deploy immediately, so the server (steps 1–2) and GitHub secrets (step 3) must be ready first.

---

## 1. Prepare the droplet (once): DigitalOcean web console
Open **Droplets → your droplet → Console**. You're logged in as `root`, no password needed.

Create the setup script on the server:
```bash
nano server-setup.sh
```
Paste the whole contents of `deploy/scripts/server-setup.sh` (right-click → Paste), then save with **Ctrl+O**, **Enter**, **Ctrl+X**. Run it:
```bash
bash server-setup.sh
```
What it does:
- **Stops** (doesn't delete) your old "healthcheck" project, to free ports 80/443 and memory. It also stops the database, Grafana and Loki ports that were open to the internet. To bring the old project back later: `docker start nginx_proxy healthcheck-backend frontend-container postgres_db grafana loki promtail`
- Adds 1 GB of swap (a safety net for 1 GB RAM)
- Creates a `deploy` user for GitHub Actions
- Creates `/opt/airdropx/{proxy,production}` and the `edge` Docker network
- Turns on the firewall: only 22, 80 and 443

Then install the pending security updates and restart:
```bash
apt update && apt upgrade -y && reboot
```
Wait a minute, then open the console again.

## 2. Create the deploy SSH key
**On your computer (Git Bash):**
```bash
ssh-keygen -t ed25519 -f ~/airdropx_deploy -N "" -C "github-actions"
cat ~/airdropx_deploy.pub
```
Copy the line it prints (it starts with `ssh-ed25519`).

**In the DigitalOcean console**, paste it into the deploy user's keys, replacing the text in quotes:
```bash
echo "ssh-ed25519 AAAA...your-line... github-actions" >> /home/deploy/.ssh/authorized_keys
```
**Test from Git Bash:**
```bash
ssh -i ~/airdropx_deploy deploy@64.23.156.149 docker ps
```
Type `yes` the first time. It should list the containers without asking for a password.

## 3. GitHub settings
**Settings → Secrets and variables → Actions → Repository secrets**

| Name | Value |
|---|---|
| `SSH_HOST` | `64.23.156.149` |
| `SSH_PRIVATE_KEY` | full contents of `~/airdropx_deploy` (the private file: `cat ~/airdropx_deploy`, copy everything including the BEGIN/END lines) |

**Settings → Environments → New environment →** `production`.
Optional: tick **Required reviewers** and add yourself. Every deploy then waits for your click, which is a nice thing to show.

Add these **two secrets to the `production` environment**:

`APP_ENV_FILE`: your whole backend `.env`, with `DB_URL` pointing at the Postgres container named `db`:
```
JWT_SECRET=...
COOKIE_NAME=...
DB_URL=jdbc:postgresql://db:5432/airdropx?user=airdropx&password=CHANGE_ME
CLIENT_ID=...
... (all the other keys exactly as today)
```
`DB_ENV_FILE`: must match the user and password in `DB_URL`:
```
POSTGRES_DB=airdropx
POSTGRES_USER=airdropx
POSTGRES_PASSWORD=CHANGE_ME
```
Use a long random password; this database is brand new and separate from your old project's.

## 4. First push
```bash
git clone https://github.com/Amir-Baddour/AirDropX.git
cd AirDropX
unzip ~/Downloads/airdropx-cicd.zip          # adds .github/, deploy/, backend/Dockerfile, .gitignore
cp -r /path/to/your/backend/. backend/        # pom.xml + src/ end up in backend/

git status                                    # CHECK: no .env, no .idea, no target/ in the list
git ls-files --others --exclude-standard | grep -i "\.env" # must print nothing
git add .
git commit -m "Add backend and CI/CD pipeline"
git push origin main
```

## 5. Watch it
Open the repo's **Actions** tab:
1. **CI**: build, secret scan, dependency scan, Docker build
2. **CD**: builds `ghcr.io/amir-baddour/airdropx:<sha>` → deploys to the droplet → waits for healthy → HTTPS smoke test

Then open **https://64-23-156-149.sslip.io/health**. It should say `OK`. The first HTTPS request can take a few seconds while Caddy gets the certificate.

## Rollback
- **Automatic:** if the new container doesn't become healthy, `deploy.sh` restarts the previous version and the job fails.
- **Manual:** Actions → **Rollback** → *Run workflow* → enter an older tag. Tags are listed under the repo's *Packages*, or in `/opt/airdropx/production/deploy-history.log`.

## Useful server commands
```bash
docker ps                                    # what's running
docker logs -f airdropx-production-api       # live logs
cat /opt/airdropx/production/.current_tag    # deployed version
free -h                                      # memory
```

## 6. Frontend + Google sign-in
The React app (`frontend/`) is built by CD into `ghcr.io/amir-baddour/airdropx-web:<sha>` and served on the same domain:

| URL | Goes to |
|---|---|
| `https://64-23-156-149.sslip.io/api/...` | Spark API (`/api` is removed) |
| `https://64-23-156-149.sslip.io/health` | Spark API |
| everything else | the frontend container |

Same domain means no CORS setup is needed.

**Sign in before Google is set up:** on the server run `bash dev-token.sh` (prints a 24-hour token for `demo-user`), then on the login page open *Developer: sign in with an access token* and paste it.

**Google sign-in (once):**
1. Google Cloud console → APIs & Services → Credentials → *Create OAuth client ID* → Web application.
2. Authorized JavaScript origin: `https://64-23-156-149.sslip.io`
   Authorized redirect URI: `https://64-23-156-149.sslip.io/auth/callback`
3. GitHub → Settings → Secrets and variables → Actions → **Variables** → `GOOGLE_CLIENT_ID` = the client ID (public, not a secret).
4. Edit the `APP_ENV_FILE` environment secret (production): set `CLIENT_ID`, `CLIENT_SECRET`, and `OAUTH_REDIRECT_URI=https://64-23-156-149.sslip.io/auth/callback`.
5. Re-run CD (or push any commit).

The edge Caddy removes the `Origin` header on `/api/auth/*`, so the backend uses `OAUTH_REDIRECT_URI` without any backend change.

## Note for the backend owner (no change made)
The backend's CORS allow-list in `backend/src/main/java/org/example/Main.java` only contains `localhost:5173` and the `l1-portal` Vercel URLs. The AirdropX frontend is served from the same domain as the API, so it doesn't need CORS. The list only matters if the frontend is ever hosted on another domain.
