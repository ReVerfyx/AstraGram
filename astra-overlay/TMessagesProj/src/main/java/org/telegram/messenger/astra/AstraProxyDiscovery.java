package org.telegram.messenger.astra;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AstraProxyDiscovery {
    private static final String PREFS = "astragram_proxy_discovery";
    private static final String KEY_ENABLED = "enabled";
    private static final long MIN_REFRESH_MS = 120_000L;
    private static final int MAX_CANDIDATES = 60;

    private static final String[] SOURCES = new String[] {
            "https://raw.githubusercontent.com/tgmtproxy/mtproxy/main/proxies.txt",
            "https://zakky8.github.io/mtproto-proxy-pro/censorship_resistant.txt"
    };

    private static volatile boolean refreshing;
    private static volatile long lastRefresh;

    private AstraProxyDiscovery() {}

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled() {
        return ApplicationLoader.applicationContext != null && prefs().getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(boolean enabled) {
        if (ApplicationLoader.applicationContext == null) {
            return;
        }
        prefs().edit().putBoolean(KEY_ENABLED, enabled).apply();
        if (enabled) {
            SharedConfig.proxyRotationEnabled = true;
            SharedConfig.saveConfig();
        }
    }

    public static void refresh(Runnable done) {
        if (!isEnabled()) {
            if (done != null) {
                AndroidUtilities.runOnUIThread(done);
            }
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (refreshing || now - lastRefresh < MIN_REFRESH_MS) {
            if (done != null) {
                AndroidUtilities.runOnUIThread(done);
            }
            return;
        }

        refreshing = true;
        lastRefresh = now;

        Utilities.globalQueue.postRunnable(() -> {
            final Set<String> links = new LinkedHashSet<>();
            for (String source : SOURCES) {
                if (links.size() >= MAX_CANDIDATES) {
                    break;
                }
                fetchLinks(source, links);
            }

            final List<ProxySettings> candidates = new ArrayList<>();
            for (String link : links) {
                if (candidates.size() >= MAX_CANDIDATES) {
                    break;
                }
                ProxySettings settings = ProxySettings.fromUri(Uri.parse(link));
                if (settings == null
                        || settings.getType() != ProxySettings.Type.MTPROTO
                        || !settings.isValid()
                        || TextUtils.isEmpty(settings.getSecret())
                        || settings.getAddress().length() > 253
                        || settings.getSecret().length() > 512
                        || settings.getPort() < 1
                        || settings.getPort() > 65535) {
                    continue;
                }
                candidates.add(settings);
            }

            AndroidUtilities.runOnUIThread(() -> {
                try {
                    SharedConfig.loadProxyList();
                    SharedConfig.ProxyInfo firstAdded = null;
                    for (ProxySettings settings : candidates) {
                        SharedConfig.ProxyInfo before = find(settings);
                        SharedConfig.ProxyInfo info = SharedConfig.addProxy(new SharedConfig.ProxyInfo(settings));
                        if (before == null && firstAdded == null) {
                            firstAdded = info;
                        }
                    }

                    if (SharedConfig.currentProxy == null && !SharedConfig.proxyList.isEmpty()) {
                        SharedConfig.currentProxy = firstAdded != null ? firstAdded : SharedConfig.proxyList.get(0);
                    }

                    if (SharedConfig.currentProxy != null) {
                        SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
                        SharedConfig.currentProxy.settings.toSharedPreferences(editor);
                        editor.putBoolean("proxy_enabled", true);
                        editor.apply();

                        SharedConfig.proxyRotationEnabled = true;
                        SharedConfig.saveConfig();
                        ConnectionsManager.setProxySettings(true, SharedConfig.currentProxy.settings);
                    }

                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
                } finally {
                    refreshing = false;
                    if (done != null) {
                        done.run();
                    }
                }
            });
        });
    }

    private static SharedConfig.ProxyInfo find(ProxySettings settings) {
        for (SharedConfig.ProxyInfo info : SharedConfig.proxyList) {
            if (settings.equals(info.settings)) {
                return info;
            }
        }
        return null;
    }

    private static void fetchLinks(String source, Set<String> out) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(source).openConnection();
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "AstraGram/1 proxy-discovery");
            connection.connect();

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null && out.size() < MAX_CANDIDATES) {
                    String link = extractProxyLink(line);
                    if (link != null) {
                        out.add(link);
                    }
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String extractProxyLink(String line) {
        if (line == null) {
            return null;
        }
        line = line.trim();
        int i = line.indexOf("https://t.me/proxy?");
        if (i < 0) {
            i = line.indexOf("tg://proxy?");
        }
        if (i < 0) {
            return null;
        }
        String link = line.substring(i).trim();
        int whitespace = -1;
        for (int p = 0; p < link.length(); p++) {
            if (Character.isWhitespace(link.charAt(p))) {
                whitespace = p;
                break;
            }
        }
        if (whitespace >= 0) {
            link = link.substring(0, whitespace);
        }
        return link;
    }
}
