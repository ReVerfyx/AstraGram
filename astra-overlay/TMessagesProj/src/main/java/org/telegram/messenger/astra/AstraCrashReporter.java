package org.telegram.messenger.astra;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class AstraCrashReporter {
    private static final String PREFS = "astragram_crash_reporter";
    private static final String KEY_LAST_CRASH = "last_crash";
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
                printer.println("Thread: " + (thread == null ? "unknown" : thread.getName()));
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
        String value = preferences.getString(KEY_LAST_CRASH, null);
        if (value != null) {
            preferences.edit().remove(KEY_LAST_CRASH).apply();
        }
        return value;
    }
}
