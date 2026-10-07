package org.telegram.messenger.astra;

import android.content.Context;
import android.content.SharedPreferences;

public final class AstraSettings {
    private static final String PREFS = "astragram_settings";
    private static final String KEY_ANIMATIONS = "animations_enabled";
    private static final String KEY_MOTION_SCALE = "motion_scale";
    private static final String KEY_AI_PROVIDER = "ai_provider";
    private static final String KEY_PROFILE_AUTOMATION = "profile_automation_enabled";

    public static final boolean DEFAULT_ANIMATIONS = true;
    public static final float DEFAULT_MOTION_SCALE = 1.0f;

    private AstraSettings() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean animationsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ANIMATIONS, DEFAULT_ANIMATIONS);
    }

    public static void setAnimationsEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ANIMATIONS, enabled).apply();
    }

    public static float motionScale(Context context) {
        return Math.max(0f, Math.min(1.5f, prefs(context).getFloat(KEY_MOTION_SCALE, DEFAULT_MOTION_SCALE)));
    }

    public static void setMotionScale(Context context, float scale) {
        prefs(context).edit().putFloat(KEY_MOTION_SCALE, Math.max(0f, Math.min(1.5f, scale))).apply();
    }

    public static String aiProvider(Context context) {
        return prefs(context).getString(KEY_AI_PROVIDER, "openai-compatible");
    }

    public static void setAiProvider(Context context, String id) {
        prefs(context).edit().putString(KEY_AI_PROVIDER, id).apply();
    }

    public static boolean profileAutomationEnabled(Context context) {
        return prefs(context).getBoolean(KEY_PROFILE_AUTOMATION, false);
    }

    public static void setProfileAutomationEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_PROFILE_AUTOMATION, enabled).apply();
    }

    public static long animationDuration(Context context, long normalDurationMs) {
        if (!animationsEnabled(context)) {
            return 0L;
        }
        return Math.round(normalDurationMs * motionScale(context));
    }
}
