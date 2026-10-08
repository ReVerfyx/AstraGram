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
    "    public final static int MAX_ACCOUNT_DEFAULT_COUNT = 8;\n    public final static int MAX_ACCOUNT_COUNT = 16;",
    "account capacity"
)

defines = root / "TMessagesProj/jni/tgnet/Defines.h"
replace_once(
    defines,
    "#define MAX_ACCOUNT_COUNT 5",
    "#define MAX_ACCOUNT_COUNT 16",
    "native account capacity"
)

intro = root / "TMessagesProj/src/main/java/org/telegram/ui/IntroActivity.java"
replace_once(
    intro,
    '''    @Override
    public View createView(Context context) {
        logoDrawable = context.getResources().getDrawable(R.drawable.telegram_logo).mutate();
        logoDrawable.setBounds(0, dp(8.666f), dp(115), dp(35));
        SpannableStringBuilder ssb = new SpannableStringBuilder(LocaleController.getString(R.string.Page1Title));
        ssb.setSpan(new ImageSpan(logoDrawable), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        titles[0] = ssb;


        actionBar.setAddToContainer(false);
''',
    '''    @Override
    public View createView(Context context) {
        titles[0] = LocaleController.getString(R.string.AppName);

        actionBar.setAddToContainer(false);
''',
    "intro branding"
)

application_loader = root / "TMessagesProj/src/main/java/org/telegram/messenger/ApplicationLoader.java"
replace_once(
    application_loader,
    '''        try {
            applicationContext = getApplicationContext();
        } catch (Throwable ignore) {

        }

        super.onCreate();
''',
    '''        try {
            applicationContext = getApplicationContext();
        } catch (Throwable ignore) {

        }

        org.telegram.messenger.astra.AstraCrashReporter.install(applicationContext);
        super.onCreate();
''',
    "early crash reporter"
)

intro = root / "TMessagesProj/src/main/java/org/telegram/ui/IntroActivity.java"
replace_once(
    intro,
    '''        logoDrawable = context.getResources().getDrawable(R.drawable.telegram_logo).mutate();
        logoDrawable.setBounds(0, dp(8.666f), dp(115), dp(35));
        SpannableStringBuilder ssb = new SpannableStringBuilder(LocaleController.getString(R.string.Page1Title));
        ssb.setSpan(new ImageSpan(logoDrawable), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        titles[0] = ssb;
''',
    '''        logoDrawable = context.getResources().getDrawable(R.drawable.telegram_logo).mutate();
        logoDrawable.setBounds(0, dp(8.666f), dp(115), dp(35));
        titles[0] = LocaleController.getString(R.string.Page1Title);
''',
    "AstraGram intro title"
)

replace_once(
    intro,
    '''        fragmentView = scrollView;

        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.suggestedLangpack);
''',
    '''        fragmentView = scrollView;

        String astraLastCrash = org.telegram.messenger.astra.AstraCrashReporter.consumeLastCrash(context);
        if (astraLastCrash != null && !astraLastCrash.isEmpty()) {
            final String crashText = astraLastCrash.length() > 7000 ? astraLastCrash.substring(0, 7000) : astraLastCrash;
            AndroidUtilities.runOnUIThread(() -> {
                if (getParentActivity() == null) {
                    return;
                }
                new AlertDialog.Builder(getParentActivity())
                        .setTitle("AstraGram crash report")
                        .setMessage(crashText)
                        .setPositiveButton("OK", null)
                        .show();
            }, 350);
        }

        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.suggestedLangpack);
''',
    "Astra crash report on intro"
)

login = root / "TMessagesProj/src/main/java/org/telegram/ui/LoginActivity.java"
replace_once(
    login,
    '''    private boolean checkPermissions = true;
    private boolean checkShowPermissions = true;
''',
    '''    // AstraGram does not need phone/call-log access for manual login.
    // Keeping these off also avoids fragile permission flows on newer Android versions.
    private boolean checkPermissions = false;
    private boolean checkShowPermissions = false;
''',
    "first-run permissions defaults"
)
replace_once(
    login,
    '''            if (page == VIEW_PHONE_INPUT) {
                checkPermissions = true;
                checkShowPermissions = true;
            }
''',
    '''            if (page == VIEW_PHONE_INPUT) {
                checkPermissions = false;
                checkShowPermissions = false;
            }
''',
    "first-run permissions reset"
)

chat_edit = root / "TMessagesProj/src/main/java/org/telegram/ui/ChatEditActivity.java"
replace_once(
    chat_edit,
    '''        settingsContainer = new LinearLayout(context);
        settingsContainer.setOrientation(LinearLayout.VERTICAL);
        linearLayout1.addView(settingsContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (currentUser != null || ChatObject.canChangeChatInfo(currentChat)) {
''',
    '''        settingsContainer = new LinearLayout(context);
        settingsContainer.setOrientation(LinearLayout.VERTICAL);
        linearLayout1.addView(settingsContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (currentChat != null && ChatObject.isChannelAndNotMegaGroup(currentChat) && ChatObject.canChangeChatInfo(currentChat)) {
            TextCell astraAiCell = new TextCell(context);
            astraAiCell.setBackgroundDrawable(Theme.getSelectorDrawable(false));
            astraAiCell.setColors(Theme.key_windowBackgroundWhiteBlueIcon, Theme.key_windowBackgroundWhiteBlueButton);
            astraAiCell.setTextAndIcon("AI settings", R.drawable.msg_bot, true);
            astraAiCell.setOnClickListener(v -> presentFragment(new AstraChannelAiSettingsActivity(chatId)));
            settingsContainer.addView(astraAiCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        if (currentUser != null || ChatObject.canChangeChatInfo(currentChat)) {
''',
    "channel AI settings row"
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
