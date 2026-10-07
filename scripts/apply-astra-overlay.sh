#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UPSTREAM_DIR="${ROOT}/vendor/telegram"

if [[ ! -d "${UPSTREAM_DIR}/.git" ]]; then
  echo "Telegram source is missing. Run scripts/bootstrap-telegram.sh first." >&2
  exit 1
fi

python3 - "${UPSTREAM_DIR}" <<'PY'
from pathlib import Path
import sys

root = Path(sys.argv[1])

def replace_once(path, old, new, label):
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match in {path}, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")

user_config = root / "TMessagesProj/src/main/java/org/telegram/messenger/UserConfig.java"
replace_once(
    user_config,
    "    public final static int MAX_ACCOUNT_DEFAULT_COUNT = 3;\n    public final static int MAX_ACCOUNT_COUNT = 4;",
    "    public final static int MAX_ACCOUNT_DEFAULT_COUNT = 32;\n    public final static int MAX_ACCOUNT_COUNT = 32;",
    "account capacity"
)

defines = root / "TMessagesProj/jni/tgnet/Defines.h"
replace_once(
    defines,
    "#define MAX_ACCOUNT_COUNT 5",
    "#define MAX_ACCOUNT_COUNT 32",
    "native account capacity"
)

settings = root / "TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java"
replace_once(
    settings,
    '        items.add(SettingCell.Factory.of(10, IconBackgroundColors.PURPLE.top, IconBackgroundColors.PURPLE.bottom, R.drawable.settings_language, getString(R.string.SettingsLanguage), LocaleController.getCurrentLanguageName()));\n',
    '        items.add(SettingCell.Factory.of(10, IconBackgroundColors.PURPLE.top, IconBackgroundColors.PURPLE.bottom, R.drawable.settings_language, getString(R.string.SettingsLanguage), LocaleController.getCurrentLanguageName()));\n'
    '        items.add(SettingCell.Factory.of(30, IconBackgroundColors.BLUE.top, IconBackgroundColors.BLUE.bottom, R.drawable.settings_chat, "AstraGram", "Automation, plugins and appearance"));\n',
    "Astra settings row"
)
replace_once(
    settings,
    '            case 10:\n                presentSettingFragment(new LanguageSelectActivity());\n                break;\n',
    '            case 10:\n                presentSettingFragment(new LanguageSelectActivity());\n                break;\n'
    '            case 30:\n                presentSettingFragment(new AstraSettingsActivity());\n                break;\n',
    "Astra settings navigation"
)
PY

cp -a "${ROOT}/astra-overlay/." "${UPSTREAM_DIR}/"
"${ROOT}/scripts/brand-astra.sh"
echo "AstraGram overlay applied."
