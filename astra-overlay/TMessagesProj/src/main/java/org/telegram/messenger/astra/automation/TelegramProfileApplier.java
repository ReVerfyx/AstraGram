package org.telegram.messenger.astra.automation;

import android.content.Context;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.astra.ai.AstraCallback;
import org.telegram.messenger.astra.ai.ProfilePlan;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.ImageUpdater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public final class TelegramProfileApplier {
    private static final long MAX_AVATAR_BYTES = 10L * 1024L * 1024L;
    private static final int MAX_REDIRECTS = 4;

    private TelegramProfileApplier() {}

    private static final class Snapshot {
        final String firstName;
        final String lastName;
        final String bio;

        Snapshot(String firstName, String lastName, String bio) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.bio = bio;
        }
    }

    public static void apply(BaseFragment fragment, ProfilePlan plan, AstraCallback<ProfilePlan> callback) {
        if (fragment == null || fragment.getParentActivity() == null) {
            callback.onError(new IllegalStateException("AstraGram screen is not attached"));
            return;
        }
        if (plan == null) {
            callback.onError(new IllegalArgumentException("Profile plan is empty"));
            return;
        }

        final int account = fragment.getCurrentAccount();
        final Snapshot before = snapshot(account);
        updateTextProfile(account, plan, new AstraCallback<Void>() {
            @Override
            public void onSuccess(Void value) {
                String avatarUrl = plan.resolvedAvatarUrl();
                if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                    callback.onSuccess(plan);
                    return;
                }
                downloadAndUploadAvatar(fragment, avatarUrl, new AstraCallback<Void>() {
                    @Override
                    public void onSuccess(Void value) {
                        callback.onSuccess(plan);
                    }

                    @Override
                    public void onError(Throwable error) {
                        rollbackTextProfile(account, before, () -> callback.onError(error));
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                callback.onError(error);
            }
        });
    }

    private static Snapshot snapshot(int account) {
        TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
        TLRPC.UserFull full = MessagesController.getInstance(account)
                .getUserFull(UserConfig.getInstance(account).getClientUserId());
        return new Snapshot(
                user != null && user.first_name != null ? user.first_name : "",
                user != null && user.last_name != null ? user.last_name : "",
                full != null && full.about != null ? full.about : ""
        );
    }

    private static void updateTextProfile(int account, ProfilePlan plan, AstraCallback<Void> callback) {
        TL_account.updateProfile req = new TL_account.updateProfile();

        if (plan.displayName != null) {
            String displayName = plan.displayName.trim();
            if (displayName.isEmpty()) {
                callback.onError(new IllegalArgumentException("Display name cannot be empty"));
                return;
            }
            req.first_name = displayName;
            req.last_name = "";
            req.flags |= 1;
            req.flags |= 2;
        }
        if (plan.bio != null) {
            req.about = plan.bio.replace("\n", " ").trim();
            req.flags |= 4;
        }

        if (req.flags == 0) {
            callback.onSuccess(null);
            return;
        }

        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) ->
                AndroidUtilities.runOnUIThread(() -> {
                    if (error != null) {
                        callback.onError(new IllegalStateException(
                                "Telegram rejected profile update: " + error.text
                        ));
                        return;
                    }

                    TLRPC.User current = UserConfig.getInstance(account).getCurrentUser();
                    if (current != null) {
                        if ((req.flags & 1) != 0) {
                            current.first_name = req.first_name;
                        }
                        if ((req.flags & 2) != 0) {
                            current.last_name = req.last_name;
                        }
                        UserConfig.getInstance(account).setCurrentUser(current);
                        UserConfig.getInstance(account).saveConfig(true);
                        MessagesController.getInstance(account).putUser(current, true);
                    }

                    if ((req.flags & 4) != 0) {
                        TLRPC.UserFull full = MessagesController.getInstance(account)
                                .getUserFull(UserConfig.getInstance(account).getClientUserId());
                        if (full != null) {
                            full.about = req.about;
                            MessagesStorage.getInstance(account).updateUserInfo(full, false);
                        }
                    }

                    NotificationCenter.getInstance(account).postNotificationName(
                            NotificationCenter.mainUserInfoChanged
                    );
                    NotificationCenter.getInstance(account).postNotificationName(
                            NotificationCenter.updateInterfaces,
                            MessagesController.UPDATE_MASK_NAME
                    );
                    callback.onSuccess(null);
                })
        );
    }

    private static void rollbackTextProfile(int account, Snapshot snapshot, Runnable after) {
        ProfilePlan rollback = new ProfilePlan(
                snapshot.firstName,
                snapshot.bio,
                null,
                Collections.emptyList()
        );

        TL_account.updateProfile req = new TL_account.updateProfile();
        req.first_name = snapshot.firstName;
        req.last_name = snapshot.lastName;
        req.about = snapshot.bio;
        req.flags = 1 | 2 | 4;

        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) ->
                AndroidUtilities.runOnUIThread(() -> {
                    TLRPC.User current = UserConfig.getInstance(account).getCurrentUser();
                    if (current != null) {
                        current.first_name = snapshot.firstName;
                        current.last_name = snapshot.lastName;
                        UserConfig.getInstance(account).setCurrentUser(current);
                        UserConfig.getInstance(account).saveConfig(true);
                    }
                    TLRPC.UserFull full = MessagesController.getInstance(account)
                            .getUserFull(UserConfig.getInstance(account).getClientUserId());
                    if (full != null) {
                        full.about = snapshot.bio;
                    }
                    NotificationCenter.getInstance(account).postNotificationName(
                            NotificationCenter.mainUserInfoChanged
                    );
                    after.run();
                })
        );
    }

    private static void downloadAndUploadAvatar(
            BaseFragment fragment,
            String avatarUrl,
            AstraCallback<Void> callback
    ) {
        Context context = fragment.getParentActivity().getApplicationContext();
        Utilities.globalQueue.postRunnable(() -> {
            File file = null;
            try {
                file = downloadImage(context, avatarUrl);
                File finalFile = file;
                AndroidUtilities.runOnUIThread(() -> uploadAvatar(fragment, finalFile, callback));
            } catch (Throwable error) {
                if (file != null && file.exists()) {
                    file.delete();
                }
                AndroidUtilities.runOnUIThread(() -> callback.onError(error));
            }
        });
    }

    private static File downloadImage(Context context, String source) throws Exception {
        URL url = new URL(source);
        HttpURLConnection connection = null;

        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            validateRemoteUrl(url);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(20000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "AstraGram/1.0");
            int status = connection.getResponseCode();

            if (status >= 300 && status < 400) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null) {
                    throw new IllegalStateException("Avatar redirect has no location");
                }
                url = new URL(url, location);
                continue;
            }

            if (status < 200 || status >= 300) {
                connection.disconnect();
                throw new IllegalStateException("Avatar download returned HTTP " + status);
            }

            String contentType = connection.getContentType();
            if (contentType != null && !contentType.toLowerCase(Locale.US).startsWith("image/")) {
                connection.disconnect();
                throw new IllegalStateException("Avatar URL is not an image");
            }

            long length = connection.getContentLengthLong();
            if (length > MAX_AVATAR_BYTES) {
                connection.disconnect();
                throw new IllegalStateException("Avatar is too large");
            }

            File output = File.createTempFile("astra_avatar_", ".jpg", context.getCacheDir());
            try (InputStream input = connection.getInputStream();
                 FileOutputStream fileOutput = new FileOutputStream(output)) {
                byte[] buffer = new byte[16 * 1024];
                long total = 0;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_AVATAR_BYTES) {
                        throw new IllegalStateException("Avatar is too large");
                    }
                    fileOutput.write(buffer, 0, read);
                }
            } finally {
                connection.disconnect();
            }
            return output;
        }

        throw new IllegalStateException("Too many avatar redirects");
    }

    private static void validateRemoteUrl(URL url) throws Exception {
        if (!"https".equalsIgnoreCase(url.getProtocol())) {
            throw new IllegalArgumentException("Avatar URL must use HTTPS");
        }
        String host = url.getHost();
        if (host == null || host.trim().isEmpty() || "localhost".equalsIgnoreCase(host)) {
            throw new IllegalArgumentException("Invalid avatar host");
        }
        for (InetAddress address : InetAddress.getAllByName(host)) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                    || address.isLinkLocalAddress() || address.isSiteLocalAddress()) {
                throw new IllegalArgumentException("Local avatar hosts are not allowed");
            }
        }
    }

    private static void uploadAvatar(
            BaseFragment fragment,
            File image,
            AstraCallback<Void> callback
    ) {
        final int account = fragment.getCurrentAccount();
        MediaController.PhotoEntry entry = new MediaController.PhotoEntry(
                0, 0, 0, image.getAbsolutePath(), 0, false, 0, 0, 0
        );

        final ImageUpdater updater = new ImageUpdater(true, ImageUpdater.FOR_TYPE_USER, true);
        updater.parentFragment = fragment;
        updater.setDelegate((photo, video, videoStartTimestamp, videoPath, bigSize, smallSize, isVideo, emojiMarkup) -> {
            TLRPC.TL_photos_uploadProfilePhoto req = new TLRPC.TL_photos_uploadProfilePhoto();
            if (photo != null) {
                req.file = photo;
                req.flags |= 1;
            }
            if (video != null) {
                req.video = video;
                req.video_start_ts = videoStartTimestamp;
                req.flags |= 2;
                req.flags |= 4;
            }
            if (emojiMarkup != null) {
                req.video_emoji_markup = emojiMarkup;
                req.flags |= 16;
            }

            ConnectionsManager.getInstance(account).sendRequest(req, (response, error) ->
                    AndroidUtilities.runOnUIThread(() -> {
                        try {
                            if (error != null || !(response instanceof TLRPC.TL_photos_photo)) {
                                callback.onError(new IllegalStateException(
                                        error != null ? "Avatar upload failed: " + error.text : "Invalid avatar response"
                                ));
                                return;
                            }

                            TLRPC.TL_photos_photo result = (TLRPC.TL_photos_photo) response;
                            MessagesController.getInstance(account).putUsers(result.users, false);
                            TLRPC.User user = MessagesController.getInstance(account)
                                    .getUser(UserConfig.getInstance(account).getClientUserId());
                            if (user == null) {
                                user = UserConfig.getInstance(account).getCurrentUser();
                            }
                            if (user != null && result.photo != null) {
                                org.telegram.messenger.utils.PhotoUtilities.applyPhotoToUser(
                                        result.photo, user, false
                                );
                                UserConfig.getInstance(account).setCurrentUser(user);
                                UserConfig.getInstance(account).saveConfig(true);
                                MessagesController.getInstance(account).putUser(user, true);

                                TLRPC.UserFull full = MessagesController.getInstance(account)
                                        .getUserFull(UserConfig.getInstance(account).getClientUserId());
                                if (full != null) {
                                    full.profile_photo = result.photo;
                                    MessagesStorage.getInstance(account).updateUserInfo(full, false);
                                }
                                NotificationCenter.getInstance(account).postNotificationName(
                                        NotificationCenter.mainUserInfoChanged
                                );
                            }
                            callback.onSuccess(null);
                        } finally {
                            updater.onPause();
                            if (image.exists()) {
                                image.delete();
                            }
                        }
                    })
            );
        });
        updater.processEntry(entry);
    }
}
