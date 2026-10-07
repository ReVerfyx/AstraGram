package org.telegram.messenger.astra.automation;

public interface ProfileGateway {
    ProfileSnapshot snapshot() throws Exception;
    void setDisplayName(String name) throws Exception;
    void setBio(String bio) throws Exception;
    void setAvatarFromUrl(String url) throws Exception;
    void restore(ProfileSnapshot snapshot) throws Exception;
}
