package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.astra.AstraSettings;
import org.telegram.messenger.astra.ai.AstraAiProvider;
import org.telegram.messenger.astra.ai.AstraCallback;
import org.telegram.messenger.astra.ai.AstraProviderStore;
import org.telegram.messenger.astra.ai.ProfilePlan;
import org.telegram.messenger.astra.ai.ProfileRequest;
import org.telegram.messenger.astra.automation.TelegramProfileApplier;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.Collections;
import java.util.Locale;

public class AstraProfileStudioActivity extends BaseFragment {
    private EditTextBoldCursor promptField;
    private TextView runButton;
    private TextView alternateButton;
    private TextView statusView;

    private ProfilePlan lastPlan;
    private int nextAvatarIndex;
    private boolean running;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(org.telegram.messenger.R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Profile Studio");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1 && !running) {
                    finishFragment();
                }
            }
        });

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(24),
                AndroidUtilities.dp(20), AndroidUtilities.dp(30));
        scroll.addView(root, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT
        ));
        fragmentView = scroll;

        TextView title = new TextView(context);
        title.setText("Tell AstraGram what your profile should feel like.");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        root.addView(title, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8
        ));

        TextView subtitle = new TextView(context);
        subtitle.setText("AstraGram will generate the profile and apply the name, bio and avatar for you.");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        root.addView(subtitle, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 22
        ));

        promptField = new EditTextBoldCursor(context);
        promptField.setHint("For example: dark cyber style, short bio, clean nickname...");
        promptField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        promptField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        promptField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        promptField.setGravity(Gravity.TOP | Gravity.START);
        promptField.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        promptField.setMinLines(5);
        promptField.setMaxLines(9);
        promptField.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14),
                AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        promptField.setBackgroundDrawable(Theme.createRoundRectDrawable(
                AndroidUtilities.dp(16),
                Theme.getColor(Theme.key_windowBackgroundGray)
        ));
        root.addView(promptField, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16
        ));

        runButton = makeButton(context, "Do it");
        runButton.setOnClickListener(v -> runAutomation(context));
        root.addView(runButton, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 50, 0, 0, 0, 10
        ));

        alternateButton = makeButton(context, "Use another avatar");
        alternateButton.setVisibility(View.GONE);
        alternateButton.setOnClickListener(v -> applyNextAvatar());
        root.addView(alternateButton, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 50, 0, 0, 0, 10
        ));

        statusView = new TextView(context);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        statusView.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(statusView, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0
        ));

        return fragmentView;
    }

    private TextView makeButton(Context context, String text) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        button.setTypeface(AndroidUtilities.bold());
        button.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        button.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(14),
                Theme.getColor(Theme.key_featuredStickers_addButton),
                Theme.getColor(Theme.key_featuredStickers_addButtonPressed)
        ));
        return button;
    }

    private void runAutomation(Context context) {
        if (running) {
            return;
        }
        String instruction = promptField.getText().toString().trim();
        if (instruction.isEmpty()) {
            Toast.makeText(context, "Describe the profile first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!AstraProviderStore.isConfigured(context)) {
            Toast.makeText(context, "Configure the AI provider first", Toast.LENGTH_LONG).show();
            presentFragment(new AstraProviderSettingsActivity());
            return;
        }

        TLRPC.User user = UserConfig.getInstance(currentAccount).getCurrentUser();
        TLRPC.UserFull full = MessagesController.getInstance(currentAccount)
                .getUserFull(UserConfig.getInstance(currentAccount).getClientUserId());

        String currentName = user == null ? "" :
                ((user.first_name == null ? "" : user.first_name)
                        + (user.last_name == null || user.last_name.isEmpty() ? "" : " " + user.last_name)).trim();
        String currentBio = full != null && full.about != null ? full.about : "";

        setRunning(true, "Creating your profile...");
        AstraAiProvider provider = AstraProviderStore.create(context.getApplicationContext());
        ProfileRequest request = new ProfileRequest(
                instruction,
                currentName,
                currentBio,
                Locale.getDefault().toLanguageTag()
        );

        Utilities.globalQueue.postRunnable(() -> {
            try {
                ProfilePlan plan = provider.buildProfile(request);
                AndroidUtilities.runOnUIThread(() -> applyPlan(plan));
            } catch (Throwable error) {
                AndroidUtilities.runOnUIThread(() -> fail(error));
            }
        });
    }

    private void applyPlan(ProfilePlan plan) {
        statusView.setText("Applying changes to Telegram...");
        TelegramProfileApplier.apply(this, plan, new AstraCallback<ProfilePlan>() {
            @Override
            public void onSuccess(ProfilePlan value) {
                lastPlan = value;
                nextAvatarIndex = 0;
                while (nextAvatarIndex < value.avatarOptions.size()
                        && value.avatarOptions.get(nextAvatarIndex).equals(value.resolvedAvatarUrl())) {
                    nextAvatarIndex++;
                }
                alternateButton.setVisibility(
                        nextAvatarIndex < value.avatarOptions.size() ? View.VISIBLE : View.GONE
                );
                setRunning(false, "Done. The profile has been updated.");
            }

            @Override
            public void onError(Throwable error) {
                fail(error);
            }
        });
    }

    private void applyNextAvatar() {
        if (running || lastPlan == null || nextAvatarIndex >= lastPlan.avatarOptions.size()) {
            return;
        }
        String avatar = lastPlan.avatarOptions.get(nextAvatarIndex++);
        setRunning(true, "Applying another avatar...");
        ProfilePlan avatarOnly = new ProfilePlan(
                null, null, avatar, Collections.singletonList(avatar)
        );
        TelegramProfileApplier.apply(this, avatarOnly, new AstraCallback<ProfilePlan>() {
            @Override
            public void onSuccess(ProfilePlan value) {
                alternateButton.setVisibility(
                        nextAvatarIndex < lastPlan.avatarOptions.size() ? View.VISIBLE : View.GONE
                );
                setRunning(false, "Avatar updated.");
            }

            @Override
            public void onError(Throwable error) {
                fail(error);
            }
        });
    }

    private void fail(Throwable error) {
        String message = error == null || error.getMessage() == null
                ? "Something went wrong"
                : error.getMessage();
        setRunning(false, message);
        if (getParentActivity() != null) {
            Toast.makeText(getParentActivity(), message, Toast.LENGTH_LONG).show();
        }
    }

    private void setRunning(boolean value, String status) {
        running = value;
        promptField.setEnabled(!value);
        runButton.setEnabled(!value);
        runButton.setAlpha(value ? 0.55f : 1f);
        statusView.setText(status);
    }
}
