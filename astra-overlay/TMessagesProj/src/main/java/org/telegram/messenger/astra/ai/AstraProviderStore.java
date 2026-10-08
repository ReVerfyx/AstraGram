package org.telegram.messenger.astra.ai;

import android.content.Context;
import android.content.SharedPreferences;

public final class AstraProviderStore {
    private static final String PREFS = "astragram_ai_provider";
    private static final String KEY_ENDPOINT = "endpoint";
    private static final String KEY_MODEL = "model";
    private static final String KEY_API_KEY = "api_key";

    public static final String BUILTIN_ENDPOINT = "https://2.26.85.86/ai/v1/chat/completions";
    public static final String BUILTIN_MODEL = "qwen2.5:3b";

    private AstraProviderStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String endpoint(Context context) {
        String value = clean(prefs(context).getString(KEY_ENDPOINT, ""));
        return value.isEmpty() ? BUILTIN_ENDPOINT : value;
    }

    public static String model(Context context) {
        String value = clean(prefs(context).getString(KEY_MODEL, ""));
        return value.isEmpty() ? BUILTIN_MODEL : value;
    }

    public static String apiKey(Context context) {
        return clean(prefs(context).getString(KEY_API_KEY, ""));
    }

    public static void save(Context context, String endpoint, String model, String apiKey) {
        String cleanEndpoint = clean(endpoint);
        String cleanModel = clean(model);
        SharedPreferences.Editor editor = prefs(context).edit();
        if (cleanEndpoint.isEmpty() && cleanModel.isEmpty()) {
            editor.remove(KEY_ENDPOINT).remove(KEY_MODEL).remove(KEY_API_KEY).apply();
            return;
        }
        editor.putString(KEY_ENDPOINT, cleanEndpoint)
                .putString(KEY_MODEL, cleanModel)
                .putString(KEY_API_KEY, clean(apiKey))
                .apply();
    }

    public static void useBuiltIn(Context context) {
        prefs(context).edit()
                .remove(KEY_ENDPOINT)
                .remove(KEY_MODEL)
                .remove(KEY_API_KEY)
                .apply();
    }

    public static boolean isBuiltIn(Context context) {
        return BUILTIN_ENDPOINT.equals(endpoint(context))
                && BUILTIN_MODEL.equals(model(context))
                && apiKey(context).isEmpty();
    }

    public static boolean isConfigured(Context context) {
        return !endpoint(context).isEmpty() && !model(context).isEmpty();
    }

    public static AstraAiProvider create(Context context) {
        boolean builtIn = isBuiltIn(context);
        return new OpenAiCompatibleProvider(
                builtIn ? "astragram-local" : "openai-compatible",
                builtIn ? "Astra AI" : "OpenAI compatible",
                endpoint(context),
                model(context),
                apiKey(context),
                new HttpJsonTransport(15000, 120000)
        );
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
