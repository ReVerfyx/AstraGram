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

python3 - "${UPSTREAM_DIR}/TMessagesProj/src/main/res" <<'PY'
from pathlib import Path
import re
import sys

res = Path(sys.argv[1])
for path in res.glob("values*/strings.xml"):
    text = path.read_text(encoding="utf-8")

    # App label in every bundled locale.
    text = re.sub(
        r'<string name="AppName">.*?</string>',
        '<string name="AppName">AstraGram</string>',
        text,
        count=1,
        flags=re.S,
    )
    text = re.sub(
        r'<string name="AppNameBeta">.*?</string>',
        '<string name="AppNameBeta">AstraGram Beta</string>',
        text,
        count=1,
        flags=re.S,
    )

    # Rebrand user-visible Telegram wording without touching Java package names,
    # deep-link schemes or lower-case telegram.org URLs.
    text = text.replace("Telegram", "AstraGram")

    path.write_text(text, encoding="utf-8")
PY

echo "AstraGram branding applied."
