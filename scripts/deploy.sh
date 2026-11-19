#!/bin/bash
# Usage: scripts/deploy.sh <domain>     (run on the server, needs jq and a certificate from get-cert.sh)
set -e
cd "$(dirname "$0")/.."
DOMAIN="${1:?usage: scripts/deploy.sh <domain>}"

[ -f .env ] || { echo "ERROR: .env missing"; exit 1; }
[ -d "certbot/conf/live/$DOMAIN" ] || { echo "ERROR: no certificate for $DOMAIN, run scripts/get-cert.sh first"; exit 1; }
command -v jq >/dev/null || { echo "ERROR: jq missing (sudo dnf install -y jq)"; exit 1; }

# domain settings used by docker-compose.prod.yml and the backend
sed -i '/^DOMAIN=/d;/^PUBLIC_URL=/d' .env
printf 'DOMAIN=%s\nPUBLIC_URL=https://%s\n' "$DOMAIN" "$DOMAIN" >> .env

# nginx config for this domain
sed "s/__DOMAIN__/$DOMAIN/g" nginx/nginx.prod.conf.template > nginx/nginx.prod.conf

# realm without the test users
mkdir -p keycloak/realm-prod
jq 'del(.users)' keycloak/realm/library-realm.json > keycloak/realm-prod/library-realm.json

C="docker compose -f docker-compose.yml -f docker-compose.app.yml -f docker-compose.prod.yml"
$C build backend     # one at a time: the server has only 2 GB of RAM
$C build nginx
$C up -d
$C ps
echo "Open https://$DOMAIN"
