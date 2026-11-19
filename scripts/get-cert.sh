#!/bin/bash
# Usage: scripts/get-cert.sh <domain> <email>
set -e
cd "$(dirname "$0")/.."
DOMAIN="${1:?usage: scripts/get-cert.sh <domain> <email>}"
EMAIL="${2:?usage: scripts/get-cert.sh <domain> <email>}"
mkdir -p certbot/conf certbot/www
docker run --rm -p 80:80 \
  -v "$PWD/certbot/conf:/etc/letsencrypt" \
  -v "$PWD/certbot/www:/var/www/certbot" \
  certbot/certbot certonly --standalone -d "$DOMAIN" -m "$EMAIL" --agree-tos --no-eff-email -n
echo "Certificate saved in certbot/conf/live/$DOMAIN (valid 90 days)."
