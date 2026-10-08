#!/usr/bin/env bash
set -euo pipefail

SERVER_IP="${SERVER_IP:-2.26.85.86}"
MODEL="${ASTRAGRAM_MODEL:-qwen2.5:3b}"

if [ "$(id -u)" -ne 0 ]; then
  echo "Run as root: sudo bash install.sh" >&2
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y curl ca-certificates nginx python3 python3-venv

if ! command -v ollama >/dev/null 2>&1; then
  curl -fsSL https://ollama.com/install.sh | sh
fi

mkdir -p /etc/systemd/system/ollama.service.d
cat >/etc/systemd/system/ollama.service.d/astragram.conf <<'EOF'
[Service]
Environment="OLLAMA_HOST=127.0.0.1:11434"
Environment="OLLAMA_NUM_PARALLEL=1"
Environment="OLLAMA_MAX_LOADED_MODELS=1"
Environment="OLLAMA_KEEP_ALIVE=5m"
EOF

systemctl daemon-reload
systemctl enable --now ollama
ollama pull "$MODEL"

if ! swapon --show | grep -q .; then
  fallocate -l 4G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

python3 -m venv /opt/astragram-certbot
/opt/astragram-certbot/bin/pip install --upgrade pip certbot

mkdir -p /var/www/letsencrypt
rm -f /etc/nginx/sites-enabled/default
cat >/etc/nginx/sites-available/astragram-ai <<EOF
server {
    listen 80 default_server;
    server_name _;
    location /.well-known/acme-challenge/ {
        root /var/www/letsencrypt;
    }
    location / {
        return 404;
    }
}
EOF
ln -sf /etc/nginx/sites-available/astragram-ai /etc/nginx/sites-enabled/astragram-ai
nginx -t
systemctl restart nginx

if [ ! -s "/etc/letsencrypt/live/$SERVER_IP/fullchain.pem" ] || [ ! -s "/etc/letsencrypt/live/$SERVER_IP/privkey.pem" ]; then
  /opt/astragram-certbot/bin/certbot certonly \
    --preferred-profile shortlived \
    --webroot \
    --webroot-path /var/www/letsencrypt \
    --ip-address "$SERVER_IP" \
    --non-interactive \
    --agree-tos \
    --register-unsafely-without-email
else
  echo "TLS certificate for $SERVER_IP already exists; reusing it."
fi

cat >/etc/nginx/conf.d/astragram-rate.conf <<'EOF'
limit_req_zone $binary_remote_addr zone=astragram_ai:10m rate=2r/s;
EOF

cat >/etc/nginx/sites-available/astragram-ai <<'EOF'
server {
    listen 80 default_server;
    server_name _;
    location /.well-known/acme-challenge/ {
        root /var/www/letsencrypt;
    }
    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl default_server;
    server_name _;

    ssl_certificate /etc/letsencrypt/live/__SERVER_IP__/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/__SERVER_IP__/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    client_max_body_size 1m;

    location = /ai/health {
        default_type application/json;
        return 200 '{"ok":true,"model":"__MODEL__"}';
    }

    location = /ai/v1/chat/completions {
        limit_req zone=astragram_ai burst=10 nodelay;
        proxy_http_version 1.1;
        proxy_set_header Host 127.0.0.1;
        proxy_set_header Connection "";
        proxy_read_timeout 180s;
        proxy_send_timeout 180s;
        proxy_pass http://127.0.0.1:11434/v1/chat/completions;
    }

    location / {
        return 404;
    }
}
EOF

sed -i \
  -e "s|__SERVER_IP__|$SERVER_IP|g" \
  -e "s|__MODEL__|$MODEL|g" \
  /etc/nginx/sites-available/astragram-ai

nginx -t
systemctl restart nginx

cat >/etc/systemd/system/astragram-cert-renew.service <<'EOF'
[Unit]
Description=Renew AstraGram short-lived IP TLS certificate
After=network-online.target

[Service]
Type=oneshot
ExecStart=/opt/astragram-certbot/bin/certbot renew --quiet --deploy-hook "systemctl reload nginx"
EOF

cat >/etc/systemd/system/astragram-cert-renew.timer <<'EOF'
[Unit]
Description=Renew AstraGram TLS certificate daily

[Timer]
OnCalendar=daily
Persistent=true
RandomizedDelaySec=1800

[Install]
WantedBy=timers.target
EOF

systemctl daemon-reload
systemctl enable --now astragram-cert-renew.timer

echo
echo "Astra AI is ready:"
echo "  https://$SERVER_IP/ai/health"
echo "  https://$SERVER_IP/ai/v1/chat/completions"
echo "Model: $MODEL"
