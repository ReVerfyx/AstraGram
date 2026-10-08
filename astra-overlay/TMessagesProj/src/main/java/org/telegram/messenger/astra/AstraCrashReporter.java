package org.telegram.messenger.astra;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public final class AstraCrashReporter {
    private static volatile boolean installed;

    private AstraCrashReporter() {
    }

    public static synchronized void install(Context context) {
        if (installed || context == null) {
            return;
        }
        installed = true;

        Context appContext = context.getApplicationContext();
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                String trace = stackTrace(thread, error);
                appContext.getSharedPreferences("astragram_diagnostics", Context.MODE_PRIVATE)
                        .edit()
                        .putString("last_crash", trace)
                        .commit();
                writeDownloadCopy(appContext, trace);
            } catch (Throwable ignored) {
            }

            if (previous != null) {
                previous.uncaughtException(thread, error);
            }
        });
    }

    private static String stackTrace(Thread thread, Throwable error) {
        StringWriter writer = new StringWriter();
        PrintWriter printer = new PrintWriter(writer);
        printer.println("AstraGram crash report");
        printer.println("Android " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");
        printer.println("Device: " + Build.MANUFACTURER + " " + Build.MODEL);
        printer.println("Thread: " + (thread == null ? "unknown" : thread.getName()));
        printer.println();
        if (error != null) {
            error.printStackTrace(printer);
        }
        printer.flush();
        return writer.toString();
    }

    private static void writeDownloadCopy(Context context, String trace) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }

        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, "AstraGram-last-crash.txt");
        values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AstraGram");
        values.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            return;
        }

        try (OutputStream stream = resolver.openOutputStream(uri, "w")) {
            if (stream == null) {
                resolver.delete(uri, null, null);
                return;
            }
            stream.write(trace.getBytes(StandardCharsets.UTF_8));
            stream.flush();
        } catch (Throwable error) {
            resolver.delete(uri, null, null);
            return;
        }

        ContentValues complete = new ContentValues();
        complete.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(uri, complete, null, null);
    }
}
