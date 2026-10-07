package org.telegram.messenger.astra.ai;

import java.util.Map;

public interface AstraAiTransport {
    String postJson(String url, String body, Map<String, String> headers) throws Exception;
}
