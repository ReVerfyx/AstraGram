#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UPSTREAM_DIR="${ROOT}/vendor/telegram"
UPSTREAM_URL="https://github.com/DrKLO/Telegram.git"
UPSTREAM_REV="${TELEGRAM_REV:-f2908b14133bbffbf7ab04f641ecb5bfaf533242}"

mkdir -p "${ROOT}/vendor"

if [[ ! -d "${UPSTREAM_DIR}/.git" ]]; then
  git clone --filter=blob:none --no-checkout "${UPSTREAM_URL}" "${UPSTREAM_DIR}"
fi

git -C "${UPSTREAM_DIR}" fetch --depth 1 origin "${UPSTREAM_REV}"
git -C "${UPSTREAM_DIR}" reset --hard "${UPSTREAM_REV}"
git -C "${UPSTREAM_DIR}" clean -fdx

"${ROOT}/scripts/apply-astra-overlay.sh"

echo
echo "AstraGram source is ready in:"
echo "  ${UPSTREAM_DIR}"
