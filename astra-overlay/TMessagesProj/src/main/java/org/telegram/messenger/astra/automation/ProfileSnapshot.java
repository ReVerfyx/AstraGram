package org.telegram.messenger.astra.automation;

public final class ProfileSnapshot {
    public final String displayName;
    public final String bio;
    public final Object avatarToken;

    public ProfileSnapshot(String displayName, String bio, Object avatarToken) {
        this.displayName = displayName;
        this.bio = bio;
        this.avatarToken = avatarToken;
    }
}
