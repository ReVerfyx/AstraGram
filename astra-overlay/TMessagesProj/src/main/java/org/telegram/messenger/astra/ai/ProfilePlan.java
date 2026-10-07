package org.telegram.messenger.astra.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProfilePlan {
    public final String displayName;
    public final String bio;
    public final String avatarUrl;
    public final List<String> avatarOptions;

    public ProfilePlan(String displayName, String bio, String avatarUrl, List<String> avatarOptions) {
        this.displayName = displayName;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.avatarOptions = avatarOptions == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(avatarOptions));
    }

    public String resolvedAvatarUrl() {
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            return avatarUrl;
        }
        return avatarOptions.isEmpty() ? null : avatarOptions.get(0);
    }
}
