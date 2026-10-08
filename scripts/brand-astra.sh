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

def replace_visible_text(xml: str) -> str:
    # Only touch element bodies. Never replace inside resource names/attributes:
    # Java references such as R.string.TelegramVersion must keep their original IDs.
    pattern = re.compile(r'(<(?:string|item)\b[^>]*>)(.*?)(</(?:string|item)>)', re.S)

    def repl(match):
        body = match.group(2).replace("Telegram", "AstraGram")
        return match.group(1) + body + match.group(3)

    return pattern.sub(repl, xml)

for path in res.glob("values*/strings.xml"):
    text = path.read_text(encoding="utf-8")

    text = re.sub(
        r'(<string\s+name="AppName"[^>]*>).*?(</string>)',
        r'\1AstraGram\2',
        text,
        count=1,
        flags=re.S,
    )
    text = re.sub(
        r'(<string\s+name="AppNameBeta"[^>]*>).*?(</string>)',
        r'\1AstraGram Beta\2',
        text,
        count=1,
        flags=re.S,
    )

    text = replace_visible_text(text)
    path.write_text(text, encoding="utf-8")
PY

echo "AstraGram branding applied."
