package org.telegram.messenger.astra.plugins;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class AstraPluginStore {
    private static final String DIR_NAME = "astra_plugins";
    private static final String PREFS = "astragram_plugins";

    public static final class InstalledPlugin {
        public final File file;
        public final AsplugManifest manifest;
        public final boolean enabled;

        InstalledPlugin(File file, AsplugManifest manifest, boolean enabled) {
            this.file = file;
            this.manifest = manifest;
            this.enabled = enabled;
        }
    }

    private AstraPluginStore() {}

    public static InstalledPlugin install(Context context, InputStream input) throws IOException, JSONException {
        File dir = pluginDir(context);
        File temp = File.createTempFile("install_", AsplugPackage.EXTENSION, dir);

        try {
            copy(input, new FileOutputStream(temp));
            AsplugManifest manifest = AsplugPackage.inspect(temp);
            File target = new File(dir, safeFileName(manifest.id) + AsplugPackage.EXTENSION);
            if (target.exists() && !target.delete()) {
                throw new IOException("Could not replace existing plugin");
            }
            if (!temp.renameTo(target)) {
                copy(new FileInputStream(temp), new FileOutputStream(target));
                if (!temp.delete()) {
                    temp.deleteOnExit();
                }
            }
            setEnabled(context, manifest.id, false);
            clearPermissions(context, manifest);
            return new InstalledPlugin(target, manifest, false);
        } catch (IOException | JSONException error) {
            if (temp.exists() && !temp.delete()) {
                temp.deleteOnExit();
            }
            throw error;
        }
    }

    public static List<InstalledPlugin> list(Context context) {
        File[] files = pluginDir(context).listFiles((dir, name) ->
                name != null && name.toLowerCase().endsWith(AsplugPackage.EXTENSION));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        ArrayList<InstalledPlugin> result = new ArrayList<>();
        for (File file : files) {
            try {
                AsplugManifest manifest = AsplugPackage.inspect(file);
                result.add(new InstalledPlugin(file, manifest, isEnabled(context, manifest.id)));
            } catch (Exception ignored) {
            }
        }
        result.sort(Comparator.comparing(plugin -> plugin.manifest.name.toLowerCase()));
        return result;
    }

    public static void setEnabled(Context context, String id, boolean enabled) {
        if (enabled) {
            InstalledPlugin plugin = find(context, id);
            if (plugin == null) {
                throw new IllegalArgumentException("Plugin is not installed");
            }
            if (!hasAllPermissions(context, plugin.manifest)) {
                throw new SecurityException("Grant the requested permissions first");
            }
        }
        prefs(context).edit().putBoolean("enabled:" + id, enabled).apply();
    }

    public static boolean isEnabled(Context context, String id) {
        return prefs(context).getBoolean("enabled:" + id, false);
    }

    public static void setPermissionGranted(
            Context context,
            String pluginId,
            AsplugPermission permission,
            boolean granted
    ) {
        prefs(context).edit()
                .putBoolean(permissionKey(pluginId, permission), granted)
                .apply();
        if (!granted) {
            prefs(context).edit().putBoolean("enabled:" + pluginId, false).apply();
        }
    }

    public static boolean isPermissionGranted(
            Context context,
            String pluginId,
            AsplugPermission permission
    ) {
        return prefs(context).getBoolean(permissionKey(pluginId, permission), false);
    }

    public static boolean hasAllPermissions(Context context, AsplugManifest manifest) {
        for (AsplugPermission permission : manifest.permissions) {
            if (!isPermissionGranted(context, manifest.id, permission)) {
                return false;
            }
        }
        return true;
    }

    public static InstalledPlugin find(Context context, String id) {
        for (InstalledPlugin plugin : list(context)) {
            if (plugin.manifest.id.equals(id)) {
                return plugin;
            }
        }
        return null;
    }

    public static boolean uninstall(Context context, InstalledPlugin plugin) {
        if (plugin == null) {
            return false;
        }
        SharedPreferences.Editor editor = prefs(context).edit().remove("enabled:" + plugin.manifest.id);
        for (AsplugPermission permission : plugin.manifest.permissions) {
            editor.remove(permissionKey(plugin.manifest.id, permission));
        }
        editor.apply();
        return !plugin.file.exists() || plugin.file.delete();
    }

    private static File pluginDir(Context context) {
        File dir = new File(context.getFilesDir(), DIR_NAME);
        if (!dir.exists() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IllegalStateException("Could not create plugin directory");
        }
        return dir;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static void clearPermissions(Context context, AsplugManifest manifest) {
        SharedPreferences.Editor editor = prefs(context).edit();
        for (AsplugPermission permission : manifest.permissions) {
            editor.remove(permissionKey(manifest.id, permission));
        }
        editor.apply();
    }

    private static String permissionKey(String pluginId, AsplugPermission permission) {
        return "permission:" + pluginId + ":" + permission.wireName();
    }

    private static String safeFileName(String id) {
        return id.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static void copy(InputStream input, FileOutputStream output) throws IOException {
        try (InputStream in = input; FileOutputStream out = output) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }
}
