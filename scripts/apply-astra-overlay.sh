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
        logoDrawable = context.getResources().getDrawable(R.drawable.telegram_logo).mutate();
        logoDrawable.setBounds(0, dp(8.666f), dp(115), dp(35));
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


# Keep the 32-account capacity without eagerly starting 32 Telegram stacks at app launch.
# Upstream initializes every slot, which becomes extremely expensive once MAX_ACCOUNT_COUNT is raised.
application_loader = root / "TMessagesProj/src/main/java/org/telegram/messenger/ApplicationLoader.java"
replace_once(
    application_loader,
    '''                    boolean isSlow = isConnectionSlow();
                    for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                        ConnectionsManager.getInstance(a).checkConnection();
                        FileLoader.getInstance(a).onNetworkChanged(isSlow);
                    }
''',
    '''                    boolean isSlow = isConnectionSlow();
                    for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                        UserConfig config = UserConfig.getInstance(a);
                        if (a != UserConfig.selectedAccount && !config.isClientActivated()) {
                            continue;
                        }
                        ConnectionsManager.getInstance(a).checkConnection();
                        FileLoader.getInstance(a).onNetworkChanged(isSlow);
                    }
''',
    "lazy account startup network loop"
)

replace_once(
    application_loader,
    '''        SharedConfig.loadConfig();
        SharedPrefsHelper.init(applicationContext);
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) { //TODO improve account
            UserConfig.getInstance(a).loadConfig();
            MessagesController.getInstance(a);
            if (a == 0) {
                SharedConfig.pushStringStatus = "__FIREBASE_GENERATING_SINCE_" + ConnectionsManager.getInstance(a).getCurrentTime() + "__";
            } else {
                ConnectionsManager.getInstance(a);
            }
            TLRPC.User user = UserConfig.getInstance(a).getCurrentUser();
            if (user != null) {
                MessagesController.getInstance(a).putUser(user, true);
                SendMessagesHelper.getInstance(a).checkUnsentMessages();
            }
        }
''',
    '''        SharedConfig.loadConfig();
        SharedPrefsHelper.init(applicationContext);

        // Loading the small UserConfig objects is cheap and is needed to discover activated slots.
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            UserConfig.getInstance(a).loadConfig();
        }

        // Heavy Telegram controllers/connections are created only for the selected or activated accounts.
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            UserConfig config = UserConfig.getInstance(a);
            if (a != UserConfig.selectedAccount && !config.isClientActivated()) {
                continue;
            }
            MessagesController.getInstance(a);
            if (a == 0) {
                SharedConfig.pushStringStatus = "__FIREBASE_GENERATING_SINCE_" + ConnectionsManager.getInstance(a).getCurrentTime() + "__";
            } else {
                ConnectionsManager.getInstance(a);
            }
            TLRPC.User user = config.getCurrentUser();
            if (user != null) {
                MessagesController.getInstance(a).putUser(user, true);
                SendMessagesHelper.getInstance(a).checkUnsentMessages();
            }
        }
''',
    "lazy account startup controller loop"
)

replace_once(
    application_loader,
    '''        MediaController.getInstance();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) { //TODO improve account
            ContactsController.getInstance(a).checkAppAccount();
            DownloadController.getInstance(a);
        }
''',
    '''        MediaController.getInstance();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            UserConfig config = UserConfig.getInstance(a);
            if (a != UserConfig.selectedAccount && !config.isClientActivated()) {
                continue;
            }
            ContactsController.getInstance(a).checkAppAccount();
            DownloadController.getInstance(a);
        }
''',
    "lazy account startup contacts loop"
)

connections_java = root / "TMessagesProj/src/main/java/org/telegram/tgnet/ConnectionsManager.java"
replace_once(
    connections_java,
    '''        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            native_setLangCode(a, langCode);
        }
''',
    '''        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            UserConfig config = UserConfig.getInstance(a);
            if (a == UserConfig.selectedAccount || config.isClientActivated()) {
                native_setLangCode(a, langCode);
            }
        }
''',
    "lazy account language loop"
)

replace_once(
    connections_java,
    '''        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            native_setRegId(a, pushString);
        }
''',
    '''        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            UserConfig config = UserConfig.getInstance(a);
            if (a == UserConfig.selectedAccount || config.isClientActivated()) {
                native_setRegId(a, pushString);
            }
        }
''',
    "lazy account push loop"
)

replace_once(
    connections_java,
    '''        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            native_setSystemLangCode(a, langCode);
        }
''',
    '''        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            UserConfig config = UserConfig.getInstance(a);
            if (a == UserConfig.selectedAccount || config.isClientActivated()) {
                native_setSystemLangCode(a, langCode);
            }
        }
''',
    "lazy account system language loop"
)

tgnet_wrapper = root / "TMessagesProj/jni/TgNetWrapper.cpp"
replace_once(
    tgnet_wrapper,
    '''    ConnectionsManager::getInstance(instanceNum).init((uint32_t) version, layer, apiId, std::string(deviceModelStr), std::string(systemVersionStr), std::string(appVersionStr), std::string(langCodeStr), std::string(systemLangCodeStr), std::string(configPathStr), std::string(logPathStr), std::string(regIdStr), std::string(cFingerprintStr), std::string(installerIdStr), std::string(packageIdStr), timezoneOffset, userId, userPremium, true, enablePushConnection, hasNetwork, networkType, performanceClass);
''',
    '''    auto &manager = ConnectionsManager::getInstance(instanceNum);
    manager.setDelegate(new Delegate());
    manager.init((uint32_t) version, layer, apiId, std::string(deviceModelStr), std::string(systemVersionStr), std::string(appVersionStr), std::string(langCodeStr), std::string(systemLangCodeStr), std::string(configPathStr), std::string(logPathStr), std::string(regIdStr), std::string(cFingerprintStr), std::string(installerIdStr), std::string(packageIdStr), timezoneOffset, userId, userPremium, true, enablePushConnection, hasNetwork, networkType, performanceClass);
''',
    "lazy native delegate init"
)

replace_once(
    tgnet_wrapper,
    '''void setJava(JNIEnv *env, jclass c, jboolean useJavaByteBuffers) {
    ConnectionsManager::useJavaVM(java, useJavaByteBuffers);
    for (int a = 0; a < MAX_ACCOUNT_COUNT; a++) {
        ConnectionsManager::getInstance(a).setDelegate(new Delegate());
    }
}
''',
    '''void setJava(JNIEnv *env, jclass c, jboolean useJavaByteBuffers) {
    // Do not instantiate every possible account here. A delegate is attached lazily in native_init.
    ConnectionsManager::useJavaVM(java, useJavaByteBuffers);
}
''',
    "lazy native account initialization"
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
