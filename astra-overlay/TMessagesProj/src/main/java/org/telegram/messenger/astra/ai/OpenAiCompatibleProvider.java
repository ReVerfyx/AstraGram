package org.telegram.messenger.astra.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OpenAiCompatibleProvider implements AstraAiProvider {
    private static final String SYSTEM_PROMPT =
            "You are the profile engine inside AstraGram. Return only valid JSON with keys " +
            "display_name, bio, avatar_url and avatar_options. Keep Telegram profile limits in mind. " +
            "Do not explain the result. avatar_options must be an array of direct image URLs when available.";

    private final String id;
    private final String name;
    private final String endpoint;
    private final String model;
    private final String apiKey;
    private final AstraAiTransport transport;

    public OpenAiCompatibleProvider(String id, String name, String endpoint, String model, String apiKey, AstraAiTransport transport) {
        this.id = id;
        this.name = name;
        this.endpoint = endpoint;
        this.model = model;
        this.apiKey = apiKey;
        this.transport = transport;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return name;
    }

    @Override
    public boolean isConfigured() {
        return endpoint != null && !endpoint.trim().isEmpty()
                && model != null && !model.trim().isEmpty()
                && transport != null;
    }

    @Override
    public ProfilePlan buildProfile(ProfileRequest request) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("AI provider is not configured");
        }

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("temperature", 0.8);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", SYSTEM_PROMPT));
        messages.put(new JSONObject().put("role", "user").put("content", userPrompt(request)));
        body.put("messages", messages);

        Map<String, String> headers = new HashMap<>();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            headers.put("Authorization", "Bearer " + apiKey.trim());
        }

        String response = transport.postJson(endpoint, body.toString(), headers);
        JSONObject root = new JSONObject(response);
        String content = root.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();

        if (content.startsWith("\u0060\u0060\u0060")) {
            int firstNewline = content.indexOf('\n');
            int lastFence = content.lastIndexOf("\u0060\u0060\u0060");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                content = content.substring(firstNewline + 1, lastFence).trim();
            }
        }

        JSONObject plan = new JSONObject(content);
        List<String> avatars = new ArrayList<>();
        JSONArray avatarArray = plan.optJSONArray("avatar_options");
        if (avatarArray != null) {
            for (int i = 0; i < avatarArray.length(); i++) {
                String value = avatarArray.optString(i, "").trim();
                if (!value.isEmpty()) {
                    avatars.add(value);
                }
            }
        }

        return new ProfilePlan(
                clean(plan.optString("display_name", null)),
                clean(plan.optString("bio", null)),
                clean(plan.optString("avatar_url", null)),
                avatars
        );
    }

    private static String userPrompt(ProfileRequest request) {
        return "Instruction: " + request.instruction + "\n"
                + "Current name: " + request.currentName + "\n"
                + "Current bio: " + request.currentBio + "\n"
                + "Language: " + request.language;
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? null : trimmed;
    }
}
