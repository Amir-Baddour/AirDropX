#!/usr/bin/env bash
# Run ONCE on the droplet (64.23.156.149), as root, from the DigitalOcean console:
#   bash server-setup.sh
# Safe to run again. It does NOT delete your old project; it only stops it (see step 1).
set -euo pipefail

# 1) Free ports 80/443 and memory: STOP (not delete) the old "healthcheck" project.
#    Bring it back later with:  docker start nginx_proxy healthcheck-backend frontend-container postgres_db grafana loki promtail
for c in nginx_proxy healthcheck-backend frontend-container postgres_db grafana loki promtail; do
  if command -v docker >/dev/null && docker ps -q -f name="^${c}$" | grep -q .; then
    docker update --restart=no "$c" >/dev/null   # don't auto-start after reboot
    docker stop "$c"
  fi
done

# 2) Docker (skip if already installed)
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sh
fi

# 3) Docker log rotation so logs never fill the disk
cat >/etc/docker/daemon.json <<'EOF'
{ "log-driver": "json-file", "log-opts": { "max-size": "10m", "max-file": "5" } }
EOF
systemctl restart docker

# 4) 1 GB swap file: a safety net for a 1 GB RAM droplet
if ! swapon --show | grep -q /swapfile; then
  fallocate -l 1G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# 5) A dedicated 'deploy' user that GitHub Actions logs in as (SSH key only)
if ! id deploy >/dev/null 2>&1; then
  adduser --disabled-password --gecos "" deploy
fi
usermod -aG docker deploy
install -d -m 700 -o deploy -g deploy /home/deploy/.ssh
touch /home/deploy/.ssh/authorized_keys
chown deploy:deploy /home/deploy/.ssh/authorized_keys
chmod 600 /home/deploy/.ssh/authorized_keys

# 6) Folders for the proxy and production
install -d -o deploy -g deploy /opt/airdropx/proxy /opt/airdropx/production

# 7) Shared network between Caddy and the API
docker network inspect edge >/dev/null 2>&1 || docker network create edge

# 8) Firewall: only SSH + HTTP + HTTPS
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

echo
echo "Done. Next: paste the PUBLIC deploy key into /home/deploy/.ssh/authorized_keys"
echo "Also recommended now: apt update && apt upgrade -y && reboot"
