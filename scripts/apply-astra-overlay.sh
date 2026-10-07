#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UPSTREAM_DIR="${ROOT}/vendor/telegram"

if [[ ! -d "${UPSTREAM_DIR}/.git" ]]; then
  echo "Telegram source is missing. Run scripts/bootstrap-telegram.sh first." >&2
  exit 1
fi

for patch in "${ROOT}"/patches/*.patch; do
  [[ -e "${patch}" ]] || continue
  if git -C "${UPSTREAM_DIR}" apply --check "${patch}"; then
    git -C "${UPSTREAM_DIR}" apply "${patch}"
  fi
done

cp -a "${ROOT}/astra-overlay/." "${UPSTREAM_DIR}/"
echo "AstraGram overlay applied."
