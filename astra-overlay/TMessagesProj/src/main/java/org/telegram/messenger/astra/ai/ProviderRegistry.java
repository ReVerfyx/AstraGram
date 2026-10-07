package org.telegram.messenger.astra.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProviderRegistry {
    private final Map<String, AstraAiProvider> providers = new LinkedHashMap<>();

    public synchronized void register(AstraAiProvider provider) {
        if (provider == null || provider.id() == null || provider.id().trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid AI provider");
        }
        providers.put(provider.id(), provider);
    }

    public synchronized void unregister(String id) {
        providers.remove(id);
    }

    public synchronized AstraAiProvider get(String id) {
        return providers.get(id);
    }

    public synchronized List<AstraAiProvider> all() {
        return Collections.unmodifiableList(new ArrayList<>(providers.values()));
    }
}
