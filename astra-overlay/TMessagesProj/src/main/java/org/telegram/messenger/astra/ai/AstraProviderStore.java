package org.telegram.messenger.astra.ai;

import android.content.Context;
import android.content.SharedPreferences;

public final class AstraProviderStore {
    private static final String PREFS = "astragram_ai_provider";
    private static final String KEY_ENDPOINT = "endpoint";
    private static final String KEY_MODEL = "model";
    private static final String KEY_API_KEY = "api_key";

    private AstraProviderStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String endpoint(Context context) {
        return prefs(context).getString(KEY_ENDPOINT, "");
    }

    public static String model(Context context) {
        return prefs(context).getString(KEY_MODEL, "");
    }

    public static String apiKey(Context context) {
        return prefs(context).getString(KEY_API_KEY, "");
    }

    public static void save(Context context, String endpoint, String model, String apiKey) {
        prefs(context).edit()
                .putString(KEY_ENDPOINT, clean(endpoint))
                .putString(KEY_MODEL, clean(model))
                .putString(KEY_API_KEY, clean(apiKey))
                .apply();
    }

    public static boolean isConfigured(Context context) {
        return !endpoint(context).isEmpty() && !model(context).isEmpty();
    }

    public static AstraAiProvider create(Context context) {
        return new OpenAiCompatibleProvider(
                "openai-compatible",
                "OpenAI compatible",
                endpoint(context),
                model(context),
                apiKey(context),
                new HttpJsonTransport()
        );
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
