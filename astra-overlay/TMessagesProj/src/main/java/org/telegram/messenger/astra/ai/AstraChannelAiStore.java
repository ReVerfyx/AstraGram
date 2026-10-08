package org.telegram.messenger.astra.ai;

import android.content.Context;
import android.content.SharedPreferences;

public final class AstraChannelAiStore {
    private static final String PREFS = "astragram_channel_ai";

    private AstraChannelAiStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(int account, long chatId, String name) {
        return account + "_" + chatId + "_" + name;
    }

    public static boolean enabled(Context context, int account, long chatId) {
        return prefs(context).getBoolean(key(account, chatId, "enabled"), false);
    }

    public static String instruction(Context context, int account, long chatId) {
        return prefs(context).getString(key(account, chatId, "instruction"), "");
    }

    public static int hour(Context context, int account, long chatId) {
        return prefs(context).getInt(key(account, chatId, "hour"), 18);
    }

    public static int minute(Context context, int account, long chatId) {
        return prefs(context).getInt(key(account, chatId, "minute"), 0);
    }

    public static void save(Context context, int account, long chatId, boolean enabled, String instruction, int hour, int minute) {
        prefs(context).edit()
                .putBoolean(key(account, chatId, "enabled"), enabled)
                .putString(key(account, chatId, "instruction"), instruction == null ? "" : instruction.trim())
                .putInt(key(account, chatId, "hour"), hour)
                .putInt(key(account, chatId, "minute"), minute)
                .apply();
    }
}
