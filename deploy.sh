#!/usr/bin/env bash
# ============================================================
#  EnglishEdu – One-command deployment script
#  Run on the production Linux server
#  Usage: bash deploy.sh
# ============================================================
set -e

BLUE='\033[0;34m'; GREEN='\033[0;32m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${BLUE}[INFO]${NC} $1"; }
success() { echo -e "${GREEN}[OK]${NC}   $1"; }
error()   { echo -e "${RED}[ERR]${NC}  $1"; exit 1; }

# ── 0. Pre-flight checks ──────────────────────────────────────
info "Checking requirements..."
command -v docker  >/dev/null || error "Docker not found. Install: https://docs.docker.com/engine/install/"
command -v node    >/dev/null || error "Node.js not found. Install via nvm or apt."
[ -f ".env.prod" ] || error ".env.prod not found. Copy .env.prod.example and fill in values."
[ -d "nginx/ssl" ] || error "SSL certs missing. Create nginx/ssl/ with cert.pem and key.pem."
[ -f "nginx/ssl/cert.pem" ] || error "nginx/ssl/cert.pem not found."
[ -f "nginx/ssl/key.pem"  ] || error "nginx/ssl/key.pem not found."

# ── 1. Build frontend ─────────────────────────────────────────
info "Building frontend..."
cd sunshine-rebuild
[ -f ".env.production" ] || cp .env.production.example .env.production
npm ci --silent
npm run build
cd ..
success "Frontend built → sunshine-rebuild/dist/"

# ── 2. Build & start all services ────────────────────────────
info "Starting Docker services..."
docker compose -f docker-compose.prod.yml --env-file .env.prod pull --quiet
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build

success "All services started."

# ── 3. Wait for backend to be ready ──────────────────────────
info "Waiting for backend health check..."
for i in $(seq 1 30); do
  if docker compose -f docker-compose.prod.yml exec -T backend \
       wget -qO- http://localhost:4000/api/v1/health >/dev/null 2>&1; then
    success "Backend is up."
    break
  fi
  echo -n "."
  sleep 5
done

# ── 4. Fix Moodle wwwroot after DB import ────────────────────
info "Updating Moodle wwwroot to production domain..."
LMS_DOMAIN=$(grep ^LMS_DOMAIN .env.prod | cut -d= -f2)
docker compose -f docker-compose.prod.yml exec -T mariadb \
  /opt/bitnami/mariadb/bin/mariadb -u bn_moodle bitnami_moodle \
  -e "UPDATE mdl_config SET value='https://${LMS_DOMAIN}' WHERE name='wwwroot';" \
  2>/dev/null || true
success "Moodle wwwroot updated."

# ── 4. Print service status ───────────────────────────────────
echo ""
docker compose -f docker-compose.prod.yml --env-file .env.prod ps

echo ""
success "Deployment complete!"
echo ""
echo "  Frontend : https://\$(grep ^DOMAIN .env.prod | cut -d= -f2)"
echo "  Moodle   : https://\$(grep ^LMS_DOMAIN .env.prod | cut -d= -f2)"
echo "  MinIO    : http://YOUR_SERVER_IP:9001  (restrict firewall in production)"
