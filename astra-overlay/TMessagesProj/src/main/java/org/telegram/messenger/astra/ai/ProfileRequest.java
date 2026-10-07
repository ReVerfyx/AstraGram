package org.telegram.messenger.astra.ai;

public final class ProfileRequest {
    public final String instruction;
    public final String currentName;
    public final String currentBio;
    public final String language;

    public ProfileRequest(String instruction, String currentName, String currentBio, String language) {
        this.instruction = instruction == null ? "" : instruction;
        this.currentName = currentName == null ? "" : currentName;
        this.currentBio = currentBio == null ? "" : currentBio;
        this.language = language == null ? "en" : language;
    }
}
