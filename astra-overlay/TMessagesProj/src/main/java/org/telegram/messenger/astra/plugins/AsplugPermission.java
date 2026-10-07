package org.telegram.messenger.astra.plugins;

import java.util.Locale;

public enum AsplugPermission {
    UI("ui"),
    NETWORK("network"),
    FILES_READ("files.read"),
    FILES_WRITE("files.write"),
    MESSAGES_READ("messages.read"),
    MESSAGES_SEND("messages.send"),
    PROFILE_READ("profile.read"),
    PROFILE_WRITE("profile.write"),
    AUTOMATION("automation");

    private final String wireName;

    AsplugPermission(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static AsplugPermission fromWireName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Permission is null");
        }
        String normalized = value.trim().toLowerCase(Locale.US);
        for (AsplugPermission permission : values()) {
            if (permission.wireName.equals(normalized)) {
                return permission;
            }
        }
        throw new IllegalArgumentException("Unknown .asplug permission: " + value);
    }
}
