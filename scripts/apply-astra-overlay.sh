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

def replace_between(path, start, end, replacement, label):
    text = path.read_text(encoding="utf-8")
    i = text.find(start)
    if i < 0:
        raise SystemExit(f"{label}: start marker not found in {path}")
    j = text.find(end, i + len(start))
    if j < 0:
        raise SystemExit(f"{label}: end marker not found in {path}")
    path.write_text(text[:i] + replacement + text[j:], encoding="utf-8")

user_config = root / "TMessagesProj/src/main/java/org/telegram/messenger/UserConfig.java"
replace_once(
    user_config,
    "    public final static int MAX_ACCOUNT_DEFAULT_COUNT = 3;\n    public final static int MAX_ACCOUNT_COUNT = 4;",
    "    public final static int MAX_ACCOUNT_DEFAULT_COUNT = 3;\n    public final static int MAX_ACCOUNT_COUNT = 4;",
    "account capacity"
)

defines = root / "TMessagesProj/jni/tgnet/Defines.h"
replace_once(
    defines,
    "#define MAX_ACCOUNT_COUNT 5",
    "#define MAX_ACCOUNT_COUNT 5",
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

# AstraGram startup should not depend on Telegram's native EGL intro renderer.
# Keep the pager/text UI, but remove the TextureView + JNI animation path until
# startup is proven stable on real devices.
replace_between(
    intro,
    '''        TextureView textureView = new TextureView(context);
''',
    '''        viewPager = new ViewPager(context);
''',
    '''        // AstraGram: native EGL intro animation disabled for stable startup.
''',
    "disable native EGL intro"
)
replace_once(
    intro,
    '''                Intro.setScrollOffset(offset);
''',
    '''                // AstraGram: native intro renderer disabled.
''',
    "disable native intro scroll"
)
replace_once(
    intro,
    '''        } else Intro.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
''',
    '''        }
''',
    "disable native intro background update"
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

# Astra automatic proxy discovery UI.
proxy_list = root / "TMessagesProj/src/main/java/org/telegram/ui/ProxyListActivity.java"
replace_once(
    proxy_list,
    '''    private int rotationRow;
    private int rotationTimeoutRow;
''',
    '''    private int rotationRow;
    private int autoFindProxyRow;
    private int rotationTimeoutRow;
''',
    "proxy auto-find row field"
)
replace_once(
    proxy_list,
    '''            } else if (position == rotationRow) {
                SharedConfig.proxyRotationEnabled = !SharedConfig.proxyRotationEnabled;
''',
    '''            } else if (position == autoFindProxyRow) {
                boolean enabled = !org.telegram.messenger.astra.AstraProxyDiscovery.isEnabled();
                org.telegram.messenger.astra.AstraProxyDiscovery.setEnabled(enabled);
                ((TextCheckCell) view).setChecked(enabled);
                if (enabled) {
                    org.telegram.messenger.astra.AstraProxyDiscovery.refresh(() -> updateRows(true));
                }
            } else if (position == rotationRow) {
                SharedConfig.proxyRotationEnabled = !SharedConfig.proxyRotationEnabled;
''',
    "proxy auto-find click"
)
replace_once(
    proxy_list,
    '''        rowCount = 0;
        useProxyRow = rowCount++;
''',
    '''        rowCount = 0;
        useProxyRow = rowCount++;
        autoFindProxyRow = rowCount++;
''',
    "proxy auto-find row placement"
)
replace_once(
    proxy_list,
    '''                    if (position == proxyAddRow) {
                        textCell.setText(getString(R.string.AddProxy), deleteAllRow != -1);
''',
    '''                    if (position == proxyAddRow) {
                        textCell.setText(getString(R.string.AddProxy), deleteAllRow != -1);
''',
    "proxy text settings anchor"
)
replace_once(
    proxy_list,
    '''                    if (position == useProxyRow) {
                        checkCell.setTextAndCheck(getString(R.string.UseProxySettings), useProxySettings, rotationRow != -1);
                    } else if (position == rotationRow) {
''',
    '''                    if (position == useProxyRow) {
                        checkCell.setTextAndCheck(getString(R.string.UseProxySettings), useProxySettings, true);
                    } else if (position == autoFindProxyRow) {
                        checkCell.setTextAndCheck("Automatic proxy search", org.telegram.messenger.astra.AstraProxyDiscovery.isEnabled(), rotationRow != -1);
                    } else if (position == rotationRow) {
''',
    "proxy auto-find bind"
)
replace_once(
    proxy_list,
    '''                if (position == useProxyRow) {
                    checkCell.setChecked(useProxySettings);
                } else if (position == rotationRow) {
''',
    '''                if (position == useProxyRow) {
                    checkCell.setChecked(useProxySettings);
                } else if (position == autoFindProxyRow) {
                    checkCell.setChecked(org.telegram.messenger.astra.AstraProxyDiscovery.isEnabled());
                } else if (position == rotationRow) {
''',
    "proxy auto-find payload"
)
replace_once(
    proxy_list,
    '''                if (position == useProxyRow) {
                    checkCell.setChecked(useProxySettings);
                } else if (position == rotationRow) {
                    checkCell.setChecked(SharedConfig.proxyRotationEnabled);
''',
    '''                if (position == useProxyRow) {
                    checkCell.setChecked(useProxySettings);
                } else if (position == autoFindProxyRow) {
                    checkCell.setChecked(org.telegram.messenger.astra.AstraProxyDiscovery.isEnabled());
                } else if (position == rotationRow) {
                    checkCell.setChecked(SharedConfig.proxyRotationEnabled);
''',
    "proxy auto-find attached"
)
replace_once(
    proxy_list,
    '''            return position == useProxyRow || position == rotationRow || position == proxyAddRow || position == deleteAllRow || position >= proxyStartRow && position < proxyEndRow;
''',
    '''            return position == useProxyRow || position == autoFindProxyRow || position == rotationRow || position == proxyAddRow || position == deleteAllRow || position >= proxyStartRow && position < proxyEndRow;
''',
    "proxy auto-find enabled"
)
replace_once(
    proxy_list,
    '''            } else if (position == rotationRow) {
                return -9;
''',
    '''            } else if (position == autoFindProxyRow) {
                return -12;
            } else if (position == rotationRow) {
                return -9;
''',
    "proxy auto-find id"
)
replace_once(
    proxy_list,
    '''            } else if (position == useProxyRow || position == rotationRow) {
                return VIEW_TYPE_TEXT_CHECK;
''',
    '''            } else if (position == useProxyRow || position == autoFindProxyRow || position == rotationRow) {
                return VIEW_TYPE_TEXT_CHECK;
''',
    "proxy auto-find type"
)

proxy_rotation = root / "TMessagesProj/src/main/java/org/telegram/messenger/ProxyRotationController.java"
replace_once(
    proxy_rotation,
    '''        } else if (id == NotificationCenter.didUpdateConnectionState && account == UserConfig.selectedAccount) {
            if (!SharedConfig.isProxyEnabled() && !SharedConfig.proxyRotationEnabled || SharedConfig.proxyList.size() <= 1) {
                return;
            }

            int state = ConnectionsManager.getInstance(account).getConnectionState();

            if (state == ConnectionsManager.ConnectionStateConnectingToProxy) {
''',
    '''        } else if (id == NotificationCenter.didUpdateConnectionState && account == UserConfig.selectedAccount) {
            boolean autoFind = org.telegram.messenger.astra.AstraProxyDiscovery.isEnabled();
            if ((!SharedConfig.isProxyEnabled() && !SharedConfig.proxyRotationEnabled) || (SharedConfig.proxyList.size() <= 1 && !autoFind)) {
                return;
            }

            int state = ConnectionsManager.getInstance(account).getConnectionState();

            if (state == ConnectionsManager.ConnectionStateConnectingToProxy) {
                if (autoFind) {
                    org.telegram.messenger.astra.AstraProxyDiscovery.refresh(null);
                }
''',
    "proxy discovery on connection failure"
)

# AstraGram does not self-install APK updates; remove this sensitive permission.
main_manifest = root / "TMessagesProj/src/main/AndroidManifest.xml"
replace_once(
    main_manifest,
    '''    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
''',
    '''    <!-- AstraGram updates are installed explicitly by the user; no package-installer permission. -->
''',
    "remove package installer permission"
)

# Astra MTProto auto-search UI + failover hook.
proxy_list = root / "TMessagesProj/src/main/java/org/telegram/ui/ProxyListActivity.java"
replace_once(
    proxy_list,
    '''import org.telegram.messenger.SharedConfig;
''',
    '''import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.astra.AstraProxyDirectory;
''',
    "Astra proxy directory import"
)
replace_once(
    proxy_list,
    '''    private int rotationRow;
    private int rotationTimeoutRow;
    private int rotationTimeoutInfoRow;
    private int deleteAllRow;
''',
    '''    private int rotationRow;
    private int rotationTimeoutRow;
    private int rotationTimeoutInfoRow;
    private int astraAutoSearchRow;
    private int astraAutoSearchTimeoutRow;
    private int astraAutoSearchInfoRow;
    private int deleteAllRow;
''',
    "Astra proxy rows"
)
replace_once(
    proxy_list,
    '''            } else if (position == rotationRow) {
                SharedConfig.proxyRotationEnabled = !SharedConfig.proxyRotationEnabled;
''',
    '''            } else if (position == astraAutoSearchRow) {
                boolean enabled = !AstraProxyDirectory.isEnabled();
                AstraProxyDirectory.setEnabled(enabled);
                if (enabled) {
                    AstraProxyDirectory.refreshNow(currentAccount, null);
                } else {
                    AstraProxyDirectory.cancelPendingRefresh();
                }
                updateRows(true);
            } else if (position == rotationRow) {
                SharedConfig.proxyRotationEnabled = !SharedConfig.proxyRotationEnabled;
''',
    "Astra proxy auto-search click"
)
replace_once(
    proxy_list,
    '''        useProxyRow = rowCount++;
        if (useProxySettings && SharedConfig.currentProxy != null && SharedConfig.proxyList.size() > 1 && IS_PROXY_ROTATION_AVAILABLE) {
''',
    '''        useProxyRow = rowCount++;
        astraAutoSearchRow = rowCount++;
        if (AstraProxyDirectory.isEnabled()) {
            astraAutoSearchTimeoutRow = rowCount++;
            astraAutoSearchInfoRow = rowCount++;
        } else {
            astraAutoSearchTimeoutRow = -1;
            astraAutoSearchInfoRow = -1;
        }
        if (useProxySettings && SharedConfig.currentProxy != null && SharedConfig.proxyList.size() > 1 && IS_PROXY_ROTATION_AVAILABLE) {
''',
    "Astra proxy auto-search rows layout"
)
replace_once(
    proxy_list,
    '''                    if (position == proxyAddRow) {
                        textCell.setText(getString(R.string.AddProxy), deleteAllRow != -1);
''',
    '''                    if (position == proxyAddRow) {
                        textCell.setText(getString(R.string.AddProxy), deleteAllRow != -1);
''',
    "Astra proxy text setting anchor"
)
replace_once(
    proxy_list,
    '''                    if (position == useProxyRow) {
                        checkCell.setTextAndCheck(getString(R.string.UseProxySettings), useProxySettings, rotationRow != -1);
                    } else if (position == rotationRow) {
                        checkCell.setTextAndCheck(getString(R.string.UseProxyRotation), SharedConfig.proxyRotationEnabled, true);
                    }
''',
    '''                    if (position == useProxyRow) {
                        checkCell.setTextAndCheck(getString(R.string.UseProxySettings), useProxySettings, true);
                    } else if (position == astraAutoSearchRow) {
                        checkCell.setTextAndCheck("Automatic MTProto proxy search", AstraProxyDirectory.isEnabled(), astraAutoSearchTimeoutRow != -1);
                    } else if (position == rotationRow) {
                        checkCell.setTextAndCheck(getString(R.string.UseProxyRotation), SharedConfig.proxyRotationEnabled, true);
                    }
''',
    "Astra proxy auto-search toggle bind"
)
replace_once(
    proxy_list,
    '''                    if (position == rotationTimeoutInfoRow) {
                        cell.setText(getString(R.string.ProxyRotationTimeoutInfo));
                    }
''',
    '''                    if (position == rotationTimeoutInfoRow) {
                        cell.setText(getString(R.string.ProxyRotationTimeoutInfo));
                    } else if (position == astraAutoSearchInfoRow) {
                        cell.setText("If the current MTProto proxy cannot connect, AstraGram downloads fresh public candidates, checks them with Telegram, and switches to a working proxy automatically.");
                    }
''',
    "Astra proxy auto-search info"
)
replace_once(
    proxy_list,
    '''                    if (position == rotationTimeoutRow) {
                        SlideChooseView chooseView = (SlideChooseView) holder.itemView;
                        ArrayList<Integer> options = new ArrayList<>(ProxyRotationController.ROTATION_TIMEOUTS);
                        String[] values = new String[options.size()];
                        for (int i = 0; i < options.size(); i++) {
                            values[i] = LocaleController.formatString(R.string.ProxyRotationTimeoutSeconds, options.get(i));
                        }
                        chooseView.setCallback(i -> {
                            SharedConfig.proxyRotationTimeout = i;
                            SharedConfig.saveConfig();
                        });
                        chooseView.setOptions(SharedConfig.proxyRotationTimeout, values);
                    }
''',
    '''                    if (position == rotationTimeoutRow || position == astraAutoSearchTimeoutRow) {
                        SlideChooseView chooseView = (SlideChooseView) holder.itemView;
                        ArrayList<Integer> options = new ArrayList<>(ProxyRotationController.ROTATION_TIMEOUTS);
                        String[] values = new String[options.size()];
                        for (int i = 0; i < options.size(); i++) {
                            values[i] = LocaleController.formatString(R.string.ProxyRotationTimeoutSeconds, options.get(i));
                        }
                        if (position == rotationTimeoutRow) {
                            chooseView.setCallback(i -> {
                                SharedConfig.proxyRotationTimeout = i;
                                SharedConfig.saveConfig();
                            });
                            chooseView.setOptions(SharedConfig.proxyRotationTimeout, values);
                        } else {
                            chooseView.setCallback(AstraProxyDirectory::setTimeoutIndex);
                            chooseView.setOptions(AstraProxyDirectory.getTimeoutIndex(), values);
                        }
                    }
''',
    "Astra proxy auto-search timeout bind"
)
replace_once(
    proxy_list,
    '''                if (position == useProxyRow) {
                    checkCell.setChecked(useProxySettings);
                } else if (position == rotationRow) {
                    checkCell.setChecked(SharedConfig.proxyRotationEnabled);
                }
''',
    '''                if (position == useProxyRow) {
                    checkCell.setChecked(useProxySettings);
                } else if (position == astraAutoSearchRow) {
                    checkCell.setChecked(AstraProxyDirectory.isEnabled());
                } else if (position == rotationRow) {
                    checkCell.setChecked(SharedConfig.proxyRotationEnabled);
                }
''',
    "Astra proxy payload checked"
)
replace_once(
    proxy_list,
    '''                if (position == useProxyRow) {
                    checkCell.setChecked(useProxySettings);
                } else if (position == rotationRow) {
                    checkCell.setChecked(SharedConfig.proxyRotationEnabled);
                }
''',
    '''                if (position == useProxyRow) {
                    checkCell.setChecked(useProxySettings);
                } else if (position == astraAutoSearchRow) {
                    checkCell.setChecked(AstraProxyDirectory.isEnabled());
                } else if (position == rotationRow) {
                    checkCell.setChecked(SharedConfig.proxyRotationEnabled);
                }
''',
    "Astra proxy attached checked"
)
replace_once(
    proxy_list,
    '''            return position == useProxyRow || position == rotationRow || position == proxyAddRow || position == deleteAllRow || position >= proxyStartRow && position < proxyEndRow;
''',
    '''            return position == useProxyRow || position == astraAutoSearchRow || position == rotationRow || position == proxyAddRow || position == deleteAllRow || position >= proxyStartRow && position < proxyEndRow;
''',
    "Astra proxy enabled rows"
)
replace_once(
    proxy_list,
    '''            } else if (position == rotationTimeoutInfoRow) {
                return -11;
            } else if (position >= proxyStartRow && position < proxyEndRow) {
''',
    '''            } else if (position == rotationTimeoutInfoRow) {
                return -11;
            } else if (position == astraAutoSearchRow) {
                return -12;
            } else if (position == astraAutoSearchTimeoutRow) {
                return -13;
            } else if (position == astraAutoSearchInfoRow) {
                return -14;
            } else if (position >= proxyStartRow && position < proxyEndRow) {
''',
    "Astra proxy stable ids"
)
replace_once(
    proxy_list,
    '''            } else if (position == useProxyRow || position == rotationRow) {
                return VIEW_TYPE_TEXT_CHECK;
            } else if (position == connectionsHeaderRow) {
                return VIEW_TYPE_HEADER;
            } else if (position == rotationTimeoutRow) {
                return VIEW_TYPE_SLIDE_CHOOSER;
''',
    '''            } else if (position == useProxyRow || position == astraAutoSearchRow || position == rotationRow) {
                return VIEW_TYPE_TEXT_CHECK;
            } else if (position == connectionsHeaderRow) {
                return VIEW_TYPE_HEADER;
            } else if (position == rotationTimeoutRow || position == astraAutoSearchTimeoutRow) {
                return VIEW_TYPE_SLIDE_CHOOSER;
''',
    "Astra proxy view types"
)

proxy_rotation = root / "TMessagesProj/src/main/java/org/telegram/messenger/ProxyRotationController.java"
replace_once(
    proxy_rotation,
    '''        } else if (id == NotificationCenter.didUpdateConnectionState && account == UserConfig.selectedAccount) {
            if (!SharedConfig.isProxyEnabled() && !SharedConfig.proxyRotationEnabled || SharedConfig.proxyList.size() <= 1) {
''',
    '''        } else if (id == NotificationCenter.didUpdateConnectionState && account == UserConfig.selectedAccount) {
            org.telegram.messenger.astra.AstraProxyDirectory.onConnectionState(account, ConnectionsManager.getInstance(account).getConnectionState());
            if (!SharedConfig.isProxyEnabled() && !SharedConfig.proxyRotationEnabled || SharedConfig.proxyList.size() <= 1) {
''',
    "Astra proxy failover hook"
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
