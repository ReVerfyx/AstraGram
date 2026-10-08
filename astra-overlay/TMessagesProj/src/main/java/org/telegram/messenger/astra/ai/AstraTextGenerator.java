package org.telegram.messenger.astra.ai;

import android.content.Context;

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

        String response = new HttpJsonTransport(15000, 180000)
                .postJson(AstraProviderStore.endpoint(context), body.toString(), headers);

        JSONObject root = new JSONObject(response);
        return root.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
    }
}
