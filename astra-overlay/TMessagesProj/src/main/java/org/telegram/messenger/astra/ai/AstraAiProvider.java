package org.telegram.messenger.astra.ai;

public interface AstraAiProvider {
    String id();
    String displayName();
    boolean isConfigured();
    ProfilePlan buildProfile(ProfileRequest request) throws Exception;
}
