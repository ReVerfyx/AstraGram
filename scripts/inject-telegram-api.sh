#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="${ROOT}/vendor/telegram/TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java"

if [[ ! -f "${TARGET}" ]]; then
  echo "BuildVars.java not found. Run bootstrap first." >&2
  exit 1
fi

if [[ -z "${TELEGRAM_API_ID:-}" || -z "${TELEGRAM_API_HASH:-}" ]]; then
  echo "TELEGRAM_API_ID and TELEGRAM_API_HASH are required for AstraGram release builds." >&2
  exit 1
fi

if [[ ! "${TELEGRAM_API_ID}" =~ ^[0-9]+$ ]]; then
  echo "TELEGRAM_API_ID must be numeric." >&2
  exit 1
fi

if [[ ! "${TELEGRAM_API_HASH}" =~ ^[0-9a-fA-F]{32}$ ]]; then
  echo "TELEGRAM_API_HASH must be a 32-character hexadecimal string." >&2
  exit 1
fi

python3 - "${TARGET}" <<'PY'
import os, re, sys
path = sys.argv[1]
api_id = os.environ["TELEGRAM_API_ID"]
api_hash = os.environ["TELEGRAM_API_HASH"]
with open(path, "r", encoding="utf-8") as f:
    text = f.read()
text, n1 = re.subn(r'public static int APP_ID = \d+;', f'public static int APP_ID = {api_id};', text, count=1)
text, n2 = re.subn(r'public static String APP_HASH = "[^"]*";', f'public static String APP_HASH = "{api_hash}";', text, count=1)
text = text.replace("public static boolean SUPPORTS_PASSKEYS = true;", "public static boolean SUPPORTS_PASSKEYS = false;")
if n1 != 1 or n2 != 1:
    raise SystemExit("Could not patch Telegram API credentials")
with open(path, "w", encoding="utf-8") as f:
    f.write(text)
PY

echo "Telegram API credentials injected from environment."
