#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UPSTREAM_DIR="${ROOT}/vendor/telegram"

python3 - "${UPSTREAM_DIR}" <<'PY'
from pathlib import Path
import re
import subprocess
import sys

root = Path(sys.argv[1])

gradle = (root / "gradle.properties").read_text(encoding="utf-8")
m = re.search(r"^APP_PACKAGE=(.+)$", gradle, re.M)
if not m or m.group(1).strip() != "com.reverfyx.astragram":
    raise SystemExit("Astra validation: APP_PACKAGE is not com.reverfyx.astragram")

user_config = (root / "TMessagesProj/src/main/java/org/telegram/messenger/UserConfig.java").read_text(encoding="utf-8")
if "MAX_ACCOUNT_DEFAULT_COUNT = 32;" not in user_config or "MAX_ACCOUNT_COUNT = 32;" not in user_config:
    raise SystemExit("Astra validation: account capacity must be 32 default / 32 max")

defines = (root / "TMessagesProj/jni/tgnet/Defines.h").read_text(encoding="utf-8")
if "#define MAX_ACCOUNT_COUNT 32" not in defines:
    raise SystemExit("Astra validation: native account capacity must be 32")

intro = (root / "TMessagesProj/src/main/java/org/telegram/ui/IntroActivity.java").read_text(encoding="utf-8")
if 'titles[0] = LocaleController.getString(R.string.AppName);' not in intro:
    raise SystemExit("Astra validation: AstraGram AppName is not used on the intro screen")
if "new ImageSpan(logoDrawable)" in intro or "titles[0] = ssb;" in intro:
    raise SystemExit("Astra validation: Telegram wordmark ImageSpan is still used on the intro screen")
if "logoDrawable = context.getResources().getDrawable(R.drawable.telegram_logo).mutate();" not in intro:
    raise SystemExit("Astra validation: intro theme drawable must stay initialized to avoid a startup NPE")

build_vars_path = root / "TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java"
build_vars = build_vars_path.read_text(encoding="utf-8")
m_id = re.search(r"public static int APP_ID = (\d+);", build_vars)
m_hash = re.search(r'public static String APP_HASH = "([^"]+)";', build_vars)
if not m_id or not m_hash:
    raise SystemExit("Astra validation: Telegram API credentials were not found in BuildVars.java")
if m_id.group(1) == "4" or m_hash.group(1) == "014b35b6184100b085b0d0572f9b5103":
    raise SystemExit("Astra validation: upstream dummy Telegram API credentials are still present")
if not re.fullmatch(r"[0-9a-fA-F]{32}", m_hash.group(1)):
    raise SystemExit("Astra validation: APP_HASH format is invalid")

decl = re.compile(r'<(?:string|plurals|string-array|integer-array|array)\b[^>]*\bname="([^"]+)"')
bad_astra_names = []

for path in sorted((root / "TMessagesProj/src/main/res").glob("values*/strings.xml")):
    rel = path.relative_to(root).as_posix()
    current = path.read_text(encoding="utf-8")
    try:
        original = subprocess.check_output(
            ["git", "-C", str(root), "show", f"HEAD:{rel}"],
            text=True,
            stderr=subprocess.DEVNULL,
        )
    except subprocess.CalledProcessError:
        continue

    original_names = decl.findall(original)
    current_names = decl.findall(current)
    if original_names != current_names:
        missing = [x for x in original_names if x not in current_names][:10]
        added = [x for x in current_names if x not in original_names][:10]
        raise SystemExit(
            f"Astra validation: resource IDs changed in {rel}; missing={missing}, added={added}"
        )

    bad_astra_names.extend(
        name for name in current_names if "AstraGram" in name
    )

    app = re.search(r'<string\s+name="AppName"[^>]*>(.*?)</string>', current, re.S)
    if app and app.group(1).strip() != "AstraGram":
        raise SystemExit(f"Astra validation: AppName was not branded in {rel}")

if bad_astra_names:
    raise SystemExit(
        "Astra validation: AstraGram leaked into Android resource identifiers: "
        + ", ".join(bad_astra_names[:10])
    )

print("Astra validation OK: package, API credentials, branding and resource IDs are consistent.")
PY


# lazy account startup validation
python3 - "$UPSTREAM_DIR" <<'PY'
from pathlib import Path
import sys
root = Path(sys.argv[1])
app = (root / "TMessagesProj/src/main/java/org/telegram/messenger/ApplicationLoader.java").read_text(encoding="utf-8")
cm = (root / "TMessagesProj/src/main/java/org/telegram/tgnet/ConnectionsManager.java").read_text(encoding="utf-8")
jni = (root / "TMessagesProj/jni/TgNetWrapper.cpp").read_text(encoding="utf-8")
if "Heavy Telegram controllers/connections are created only for the selected or activated accounts." not in app:
    raise SystemExit("Astra validation: lazy ApplicationLoader account initialization is missing")
if "a == UserConfig.selectedAccount || config.isClientActivated()" not in cm:
    raise SystemExit("Astra validation: lazy ConnectionsManager broadcast loops are missing")
if "manager.setDelegate(new Delegate());" not in jni or "Do not instantiate every possible account here" not in jni:
    raise SystemExit("Astra validation: lazy native account initialization is missing")
print("Astra lazy-account validation OK.")
PY
