package org.telegram.messenger.astra.ai;

import android.content.Context;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public final class AstraTextGenerator {
    private AstraTextGenerator() {}

    public static String generate(Context context, String systemPrompt, String userPrompt) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", AstraProviderStore.model(context));
        body.put("temperature", 0.75);
        body.put("stream", false);

        JSONArray messages = new JSONArray();
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", systemPrompt.trim()));
        }
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", userPrompt == null ? "" : userPrompt));
        body.put("messages", messages);

        Map<String, String> headers = new HashMap<>();
        String apiKey = AstraProviderStore.apiKey(context);
        if (!apiKey.isEmpty()) {
            headers.put("Authorization", "Bearer " + apiKey);
        }

        String response;
        try {
            response = new HttpJsonTransport(15000, 180000)
                    .postJson(AstraProviderStore.endpoint(context), body.toString(), headers);
        } catch (Exception error) {
            if (AstraProviderStore.isBuiltIn(context)) {
                throw new IllegalStateException("Astra AI: " + LocaleController.getString(R.string.ErrorOccurred));
            }
            throw error;
        }

        JSONObject root = new JSONObject(response);
        return root.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
    }
}
