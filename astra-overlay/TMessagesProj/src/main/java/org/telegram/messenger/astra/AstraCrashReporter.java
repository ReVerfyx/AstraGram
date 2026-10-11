package org.telegram.messenger.astra;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public final class AstraCrashReporter {
    private static final String PREFS = "astragram_crash_reporter";
    private static final String KEY_LAST_CRASH = "last_crash";
    private static final String KEY_LAST_EXIT_SHOWN = "last_exit_shown";
    private static volatile boolean installed;

    private AstraCrashReporter() {}

    public static synchronized void install(Context context) {
        if (installed || context == null) {
            return;
        }
        installed = true;

        final Context appContext = context.getApplicationContext();
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                StringWriter writer = new StringWriter();
                PrintWriter printer = new PrintWriter(writer);
                printer.println("AstraGram crash report");
                printer.println("Android " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");
                printer.println("Device: " + Build.MANUFACTURER + " " + Build.MODEL);
                printer.println("Thread: " + (thread == null ? "unknown" : thread.getName()));
                printer.println();
                if (throwable != null) {
                    throwable.printStackTrace(printer);
                }
                printer.flush();

                String crash = writer.toString();
                if (crash.length() > 30000) {
                    crash = crash.substring(0, 30000);
                }

                appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putString(KEY_LAST_CRASH, crash)
                        .commit();
            } catch (Throwable ignored) {
            }

            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            } else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        });
    }

    public static String consumeLastCrash(Context context) {
        if (context == null) {
            return null;
        }

        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String javaCrash = preferences.getString(KEY_LAST_CRASH, null);
        if (javaCrash != null) {
            preferences.edit().remove(KEY_LAST_CRASH).apply();
            return javaCrash;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return null;
        }

        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager == null) {
                return null;
            }
            List<ApplicationExitInfo> exits = manager.getHistoricalProcessExitReasons(context.getPackageName(), 0, 5);
            if (exits == null || exits.isEmpty()) {
                return null;
            }

            long alreadyShown = preferences.getLong(KEY_LAST_EXIT_SHOWN, 0L);
            long now = System.currentTimeMillis();
            for (ApplicationExitInfo info : exits) {
                if (info == null || info.getTimestamp() <= alreadyShown || now - info.getTimestamp() > 15 * 60_000L) {
                    continue;
                }
                int reason = info.getReason();
                if (reason != ApplicationExitInfo.REASON_CRASH
                        && reason != ApplicationExitInfo.REASON_CRASH_NATIVE
                        && reason != ApplicationExitInfo.REASON_ANR) {
                    continue;
                }

                preferences.edit().putLong(KEY_LAST_EXIT_SHOWN, info.getTimestamp()).apply();
                return "AstraGram process exit\n"
                        + "reason=" + reasonName(reason) + " (" + reason + ")\n"
                        + "status=" + info.getStatus() + "\n"
                        + "description=" + String.valueOf(info.getDescription()) + "\n"
                        + "timestamp=" + info.getTimestamp();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String reasonName(int reason) {
        if (reason == ApplicationExitInfo.REASON_CRASH_NATIVE) return "CRASH_NATIVE";
        if (reason == ApplicationExitInfo.REASON_CRASH) return "CRASH";
        if (reason == ApplicationExitInfo.REASON_ANR) return "ANR";
        return "UNKNOWN";
    }
}
