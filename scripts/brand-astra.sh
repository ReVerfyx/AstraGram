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

echo "AstraGram branding applied."
