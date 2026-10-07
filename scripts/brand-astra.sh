#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UPSTREAM_DIR="${ROOT}/vendor/telegram"

if [[ ! -d "${UPSTREAM_DIR}" ]]; then
  echo "Telegram source is missing." >&2
  exit 1
fi

if [[ -f "${UPSTREAM_DIR}/gradle.properties" ]]; then
  sed -i 's/^APP_PACKAGE=.*/APP_PACKAGE=com.reverfyx.astragram/' "${UPSTREAM_DIR}/gradle.properties"
fi

while IFS= read -r -d '' strings_file; do
  sed -i     -e 's#<string name="AppName">[^<]*</string>#<string name="AppName">AstraGram</string>#'     -e 's#<string name="AppNameBeta">[^<]*</string>#<string name="AppNameBeta">AstraGram Beta</string>#'     "${strings_file}"
done < <(find "${UPSTREAM_DIR}/TMessagesProj/src/main/res" -path '*/values*/strings.xml' -print0)

GOOGLE_SERVICES="${UPSTREAM_DIR}/TMessagesProj/google-services.json"
if [[ -f "${GOOGLE_SERVICES}" ]]; then
  python3 - "${GOOGLE_SERVICES}" <<'PY'
import json, sys
path = sys.argv[1]
with open(path, "r", encoding="utf-8") as f:
    data = json.load(f)
clients = data.get("client", [])
if clients:
    clients[0].setdefault("client_info", {}).setdefault("android_client_info", {})["package_name"] = "com.reverfyx.astragram"
with open(path, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
PY
fi

echo "AstraGram branding applied."
