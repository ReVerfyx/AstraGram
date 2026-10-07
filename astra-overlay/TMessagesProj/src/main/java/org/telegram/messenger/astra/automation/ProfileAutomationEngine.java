package org.telegram.messenger.astra.automation;

import org.telegram.messenger.astra.ai.AstraAiProvider;
import org.telegram.messenger.astra.ai.ProfilePlan;
import org.telegram.messenger.astra.ai.ProfileRequest;

public final class ProfileAutomationEngine {
    public ProfilePlan runAndApply(AstraAiProvider provider, ProfileRequest request, ProfileGateway gateway) throws Exception {
        if (provider == null || !provider.isConfigured()) {
            throw new IllegalStateException("AI provider is not configured");
        }
        if (gateway == null) {
            throw new IllegalArgumentException("Profile gateway is required");
        }

        ProfileSnapshot before = gateway.snapshot();
        ProfilePlan plan = provider.buildProfile(request);

        try {
            if (plan.displayName != null) {
                gateway.setDisplayName(plan.displayName);
            }
            if (plan.bio != null) {
                gateway.setBio(plan.bio);
            }
            String avatar = plan.resolvedAvatarUrl();
            if (avatar != null) {
                gateway.setAvatarFromUrl(avatar);
            }
            return plan;
        } catch (Exception applyError) {
            try {
                gateway.restore(before);
            } catch (Exception rollbackError) {
                applyError.addSuppressed(rollbackError);
            }
            throw applyError;
        }
    }
}
