package org.telegram.messenger.astra.plugins;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class AsplugPackage {
    public static final String EXTENSION = ".asplug";
    private static final String MANIFEST = "manifest.json";
    private static final int MAX_ENTRIES = 2048;
    private static final int MAX_MANIFEST_BYTES = 256 * 1024;

    private AsplugPackage() {}

    public static AsplugManifest inspect(File file) throws IOException, JSONException {
        if (file == null || !file.isFile() || !file.getName().toLowerCase().endsWith(EXTENSION)) {
            throw new IOException("Not an .asplug file");
        }

        try (ZipFile zip = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            int count = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                count++;
                if (count > MAX_ENTRIES) {
                    throw new IOException("Plugin contains too many files");
                }
                validateEntry(entry.getName());
            }

            ZipEntry manifest = zip.getEntry(MANIFEST);
            if (manifest == null || manifest.isDirectory()) {
                throw new IOException("manifest.json is missing");
            }
            if (manifest.getSize() > MAX_MANIFEST_BYTES) {
                throw new IOException("manifest.json is too large");
            }

            String json;
            try (InputStream input = zip.getInputStream(manifest)) {
                json = readLimited(input, MAX_MANIFEST_BYTES);
            }

            AsplugManifest parsed = AsplugManifest.parse(json);
            if (zip.getEntry(parsed.entrypoint) == null) {
                throw new IOException("Plugin entrypoint is missing: " + parsed.entrypoint);
            }
            return parsed;
        }
    }

    private static void validateEntry(String name) throws IOException {
        if (name == null || name.isEmpty() || name.startsWith("/") || name.startsWith("\\")
                || name.contains("../") || name.contains("..\\") || name.contains(":")) {
            throw new IOException("Unsafe plugin path: " + name);
        }
    }

    private static String readLimited(InputStream input, int maxBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new IOException("Plugin manifest exceeds limit");
            }
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
