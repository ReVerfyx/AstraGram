package org.telegram.messenger.astra.plugins;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class AsplugManifest {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9][a-z0-9._-]{2,95}");
    private static final Pattern VERSION_PATTERN = Pattern.compile("[0-9]+(?:\\.[0-9A-Za-z_-]+){0,4}");

    public final String id;
    public final String name;
    public final String version;
    public final String entrypoint;
    public final Set<AsplugPermission> permissions;

    private AsplugManifest(String id, String name, String version, String entrypoint, Set<AsplugPermission> permissions) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.entrypoint = entrypoint;
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public static AsplugManifest parse(String json) throws JSONException {
        JSONObject object = new JSONObject(json);
        String id = object.getString("id").trim();
        String name = object.getString("name").trim();
        String version = object.getString("version").trim();
        String entrypoint = object.optString("entrypoint", "main.js").trim();

        if (!ID_PATTERN.matcher(id).matches()) {
            throw new JSONException("Invalid plugin id");
        }
        if (name.isEmpty() || name.length() > 80) {
            throw new JSONException("Invalid plugin name");
        }
        if (!VERSION_PATTERN.matcher(version).matches()) {
            throw new JSONException("Invalid plugin version");
        }
        if (entrypoint.isEmpty() || entrypoint.startsWith("/") || entrypoint.contains("..")) {
            throw new JSONException("Invalid plugin entrypoint");
        }

        EnumSet<AsplugPermission> permissions = EnumSet.noneOf(AsplugPermission.class);
        JSONArray array = object.optJSONArray("permissions");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                try {
                    permissions.add(AsplugPermission.fromWireName(array.getString(i)));
                } catch (IllegalArgumentException e) {
                    throw new JSONException(e.getMessage());
                }
            }
        }
        return new AsplugManifest(id, name, version, entrypoint, permissions);
    }
}
