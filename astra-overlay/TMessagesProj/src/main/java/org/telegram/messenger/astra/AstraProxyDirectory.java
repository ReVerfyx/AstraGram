package org.telegram.messenger.astra;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.ProxyRotationController;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.utils.proxy.ProxySettings;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class AstraProxyDirectory {
    private static final String PREFS = "astragram_proxy_directory";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_TIMEOUT = "timeout_index";

    private static final int MAX_IMPORTED = 24;
    private static final int MAX_CHECKED = 16;
    private static final long REFRESH_COOLDOWN_MS = 30_000L;

    private static final String[] SOURCES = new String[]{
            "https://tgmtproxy.github.io/mtproxy/proxies.txt",
            "https://zakky8.github.io/mtproto-proxy-pro/censorship_resistant.txt",
            "https://raw.githubusercontent.com/tgmtproxy/mtproxy/main/proxies.txt"
    };

    private static volatile boolean refreshing;
    private static volatile long lastRefreshAt;
    private static Runnable pendingRefresh;

    private AstraProxyDirectory() {}

    private static SharedPreferences prefs() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            throw new IllegalStateException("Application context is not ready");
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled() {
        try {
            return prefs().getBoolean(KEY_ENABLED, false);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void setEnabled(boolean enabled) {
        try {
            prefs().edit().putBoolean(KEY_ENABLED, enabled).apply();
        } catch (Throwable ignored) {
        }
        if (!enabled) {
            cancelPendingRefresh();
        }
    }

    public static int getTimeoutIndex() {
        try {
            int index = prefs().getInt(KEY_TIMEOUT, ProxyRotationController.DEFAULT_TIMEOUT_INDEX);
            return Math.max(0, Math.min(index, ProxyRotationController.ROTATION_TIMEOUTS.size() - 1));
        } catch (Throwable ignored) {
            return ProxyRotationController.DEFAULT_TIMEOUT_INDEX;
        }
    }

    public static void setTimeoutIndex(int index) {
        int safe = Math.max(0, Math.min(index, ProxyRotationController.ROTATION_TIMEOUTS.size() - 1));
        try {
            prefs().edit().putInt(KEY_TIMEOUT, safe).apply();
        } catch (Throwable ignored) {
        }
    }

    public static void cancelPendingRefresh() {
        Runnable task = pendingRefresh;
        if (task != null) {
            AndroidUtilities.cancelRunOnUIThread(task);
        }
        pendingRefresh = null;
    }

    public static void onConnectionState(int account, int state) {
        if (!isEnabled()) {
            cancelPendingRefresh();
            return;
        }

        if (state == ConnectionsManager.ConnectionStateConnectingToProxy) {
            if (pendingRefresh == null) {
                pendingRefresh = () -> {
                    pendingRefresh = null;
                    refreshNow(account, null);
                };
                int seconds = ProxyRotationController.ROTATION_TIMEOUTS.get(getTimeoutIndex());
                AndroidUtilities.runOnUIThread(pendingRefresh, seconds * 1000L);
            }
        } else if (state == ConnectionsManager.ConnectionStateConnected
                || state == ConnectionsManager.ConnectionStateUpdating) {
            cancelPendingRefresh();
        }
    }

    public static void refreshNow(int account, Runnable completion) {
        if (!isEnabled() || refreshing) {
            if (completion != null) {
                AndroidUtilities.runOnUIThread(completion);
            }
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastRefreshAt < REFRESH_COOLDOWN_MS) {
            if (completion != null) {
                AndroidUtilities.runOnUIThread(completion);
            }
            return;
        }

        refreshing = true;
        lastRefreshAt = now;
        Utilities.globalQueue.postRunnable(() -> {
            ArrayList<ProxySettings> settings = downloadCandidates();
            AndroidUtilities.runOnUIThread(() -> importAndCheck(account, settings, completion));
        });
    }

    private static ArrayList<ProxySettings> downloadCandidates() {
        ArrayList<ProxySettings> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String source : SOURCES) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(source).openConnection();
                connection.setConnectTimeout(6000);
                connection.setReadTimeout(7000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "AstraGram/12.10.6");
                connection.setRequestProperty("Accept", "text/plain,*/*");

                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) {
                    continue;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null && result.size() < MAX_IMPORTED) {
                        ProxySettings parsed = parseLine(line);
                        if (parsed == null) {
                            continue;
                        }

                        String key = parsed.getAddress().toLowerCase(Locale.US) + ":" + parsed.getPort() + ":" + parsed.getSecret();
                        if (seen.add(key)) {
                            result.add(parsed);
                        }
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            if (result.size() >= MAX_IMPORTED) {
                break;
            }
        }
        return result;
    }

    private static ProxySettings parseLine(String raw) {
        if (raw == null) {
            return null;
        }

        String line = raw.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return null;
        }

        int separator = line.indexOf('|');
        if (separator > 0) {
            line = line.substring(0, separator).trim();
        }

        int space = line.indexOf(' ');
        if (space > 0) {
            line = line.substring(0, space).trim();
        }

        if (!(line.startsWith("tg://proxy?") || line.startsWith("tg:proxy?")
                || line.startsWith("https://t.me/proxy?")
                || line.startsWith("https://telegram.me/proxy?")
                || line.startsWith("https://telegram.dog/proxy?"))) {
            return null;
        }

        ProxySettings settings = ProxySettings.fromUri(Uri.parse(line));
        if (settings == null
                || settings.getType() != ProxySettings.Type.MTPROTO
                || !settings.isValid()
                || TextUtils.isEmpty(settings.getSecret())
                || settings.getPort() < 1
                || settings.getPort() > 65535
                || isLocalAddress(settings.getAddress())) {
            return null;
        }
        return settings;
    }

    private static boolean isLocalAddress(String address) {
        if (TextUtils.isEmpty(address)) {
            return true;
        }
        String value = address.toLowerCase(Locale.US).trim();
        if ("localhost".equals(value) || "::1".equals(value) || value.startsWith("127.")
                || value.startsWith("10.") || value.startsWith("192.168.")
                || value.startsWith("169.254.") || value.startsWith("fc")
                || value.startsWith("fd") || value.startsWith("fe80:")) {
            return true;
        }
        if (value.startsWith("172.")) {
            String[] parts = value.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return false;
    }

    private static void importAndCheck(int account, List<ProxySettings> candidates, Runnable completion) {
        try {
            SharedConfig.loadProxyList();

            ArrayList<SharedConfig.ProxyInfo> toCheck = new ArrayList<>();
            for (ProxySettings settings : candidates) {
                SharedConfig.ProxyInfo requested = new SharedConfig.ProxyInfo(settings);
                SharedConfig.ProxyInfo actual = SharedConfig.addProxy(requested);
                if (toCheck.size() < MAX_CHECKED && !toCheck.contains(actual)) {
                    actual.availableCheckTime = 0;
                    toCheck.add(actual);
                }
            }

            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);

            if (toCheck.isEmpty()) {
                refreshing = false;
                if (completion != null) {
                    completion.run();
                }
                return;
            }

            AtomicInteger remaining = new AtomicInteger(toCheck.size());
            for (SharedConfig.ProxyInfo info : toCheck) {
                info.checking = true;
                ConnectionsManager.getInstance(account).checkProxy(info.settings, time ->
                        AndroidUtilities.runOnUIThread(() -> {
                            info.checking = false;
                            info.availableCheckTime = android.os.SystemClock.elapsedRealtime();
                            if (time == -1) {
                                info.available = false;
                                info.ping = 0;
                            } else {
                                info.available = true;
                                info.ping = time;
                            }
                            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxyCheckDone, info);

                            if (remaining.decrementAndGet() == 0) {
                                chooseBestWorkingProxy();
                                refreshing = false;
                                if (completion != null) {
                                    completion.run();
                                }
                            }
                        }));
            }
        } catch (Throwable ignored) {
            refreshing = false;
            if (completion != null) {
                completion.run();
            }
        }
    }

    private static void chooseBestWorkingProxy() {
        if (!isEnabled()) {
            return;
        }

        SharedConfig.ProxyInfo best = null;
        for (SharedConfig.ProxyInfo info : SharedConfig.proxyList) {
            if (info.settings.getType() != ProxySettings.Type.MTPROTO || !info.available || info.ping <= 0) {
                continue;
            }
            if (best == null || info.ping < best.ping) {
                best = info;
            }
        }

        if (best == null) {
            return;
        }

        SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        editor.putBoolean("proxy_enabled", true);
        best.settings.toSharedPreferences(editor);
        editor.apply();

        SharedConfig.currentProxy = best;
        SharedConfig.proxyRotationEnabled = SharedConfig.proxyList.size() > 1;
        SharedConfig.saveConfig();

        ConnectionsManager.setProxySettings(true, best.settings);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxyChangedByRotation);
    }
}
