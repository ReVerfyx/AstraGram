package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.astra.AstraSettings;
import org.telegram.messenger.astra.ai.AstraAiProvider;
import org.telegram.messenger.astra.ai.AstraCallback;
import org.telegram.messenger.astra.ai.AstraProviderStore;
import org.telegram.messenger.astra.ai.ProfilePlan;
import org.telegram.messenger.astra.ai.ProfileRequest;
import org.telegram.messenger.astra.automation.TelegramProfileApplier;
import org.telegram.tgnet.TLRPC;
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
    private TextView previewName;
    private TextView previewBio;
    private TextView avatarCount;

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
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(30));
        scroll.addView(root, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT
        ));
        fragmentView = scroll;

        FrameLayout hero = new FrameLayout(context);
        GradientDrawable heroBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF6F5CFF, 0xFF4F7FFF, 0xFF43A7FF}
        );
        heroBg.setCornerRadius(AndroidUtilities.dp(24));
        hero.setBackground(heroBg);
        root.addView(hero, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 132, 0, 0, 0, 14
        ));

        TextView eyebrow = new TextView(context);
        eyebrow.setText("PROFILE STUDIO");
        eyebrow.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        eyebrow.setTypeface(AndroidUtilities.bold());
        eyebrow.setTextColor(0xCCFFFFFF);
        eyebrow.setLetterSpacing(0.1f);
        FrameLayout.LayoutParams eyebrowLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.TOP
        );
        eyebrowLp.leftMargin = AndroidUtilities.dp(20);
        eyebrowLp.topMargin = AndroidUtilities.dp(18);
        hero.addView(eyebrow, eyebrowLp);

        TextView title = new TextView(context);
        title.setText("One prompt.\nA finished profile.");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(0xFFFFFFFF);
        FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.TOP
        );
        titleLp.leftMargin = AndroidUtilities.dp(20);
        titleLp.topMargin = AndroidUtilities.dp(44);
        hero.addView(title, titleLp);

        TextView sparkle = new TextView(context);
        sparkle.setText("✦");
        sparkle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 40);
        sparkle.setGravity(Gravity.CENTER);
        sparkle.setTextColor(0xF0FFFFFF);
        GradientDrawable sparkleBg = new GradientDrawable();
        sparkleBg.setShape(GradientDrawable.OVAL);
        sparkleBg.setColor(0x20FFFFFF);
        sparkle.setBackground(sparkleBg);
        FrameLayout.LayoutParams sparkleLp = new FrameLayout.LayoutParams(
                AndroidUtilities.dp(64), AndroidUtilities.dp(64), Gravity.END | Gravity.CENTER_VERTICAL
        );
        sparkleLp.rightMargin = AndroidUtilities.dp(18);
        hero.addView(sparkle, sparkleLp);

        LinearLayout promptCard = card(context);
        root.addView(promptCard, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12
        ));

        TextView promptLabel = label(context, "Describe the vibe");
        promptCard.addView(promptLabel, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10
        ));

        promptField = new EditTextBoldCursor(context);
        promptField.setHint("Dark cyber style, short bio, clean nickname...");
        promptField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        promptField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        promptField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        promptField.setGravity(Gravity.TOP | Gravity.START);
        promptField.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        promptField.setMinLines(4);
        promptField.setMaxLines(8);
        promptField.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12),
                AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        promptField.setBackgroundDrawable(Theme.createRoundRectDrawable(
                AndroidUtilities.dp(14),
                Theme.getColor(Theme.key_windowBackgroundGray)
        ));
        promptCard.addView(promptField, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12
        ));

        HorizontalScrollView chipsScroll = new HorizontalScrollView(context);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(context);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chipsScroll.addView(chips);
        promptCard.addView(chipsScroll, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 36
        ));
        addChip(context, chips, "Clean", "Clean minimal profile, short nickname, simple bio, modern avatar");
        addChip(context, chips, "Dark", "Dark cyber profile, bold nickname, compact mysterious bio, dark avatar");
        addChip(context, chips, "Soft", "Soft calm profile, friendly nickname, warm short bio, clean portrait avatar");
        addChip(context, chips, "Random", "Surprise me with a stylish cohesive profile");

        runButton = makeButton(context, "Create and apply", true);
        runButton.setOnClickListener(v -> runAutomation(context));
        root.addView(runButton, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 52, 0, 0, 0, 12
        ));

        LinearLayout previewCard = card(context);
        root.addView(previewCard, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12
        ));

        TextView previewLabel = label(context, "Profile preview");
        previewCard.addView(previewLabel, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12
        ));

        TLRPC.User user = UserConfig.getInstance(currentAccount).getCurrentUser();
        TLRPC.UserFull full = MessagesController.getInstance(currentAccount)
                .getUserFull(UserConfig.getInstance(currentAccount).getClientUserId());
        String currentName = user == null ? "Your name" :
                ((user.first_name == null ? "" : user.first_name)
                        + (user.last_name == null || user.last_name.isEmpty() ? "" : " " + user.last_name)).trim();

        previewName = new TextView(context);
        previewName.setText(currentName.isEmpty() ? "Your name" : currentName);
        previewName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        previewName.setTypeface(AndroidUtilities.bold());
        previewName.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        previewCard.addView(previewName, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4
        ));

        previewBio = new TextView(context);
        previewBio.setText(full != null && full.about != null && !full.about.isEmpty()
                ? full.about
                : "Your bio will appear here");
        previewBio.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        previewBio.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        previewCard.addView(previewBio, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10
        ));

        avatarCount = new TextView(context);
        avatarCount.setText("Avatar options: waiting for a generated profile");
        avatarCount.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        avatarCount.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        previewCard.addView(avatarCount, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT
        ));

        alternateButton = makeButton(context, "Use another avatar", false);
        alternateButton.setVisibility(View.GONE);
        alternateButton.setOnClickListener(v -> applyNextAvatar());
        root.addView(alternateButton, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 48, 0, 0, 0, 8
        ));

        statusView = new TextView(context);
        statusView.setText("Ready");
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        statusView.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(statusView, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0
        ));

        if (AstraSettings.animationsEnabled(context)) {
            root.setAlpha(0f);
            root.setTranslationY(AndroidUtilities.dp(10));
            root.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(AstraSettings.animationDuration(context, 320))
                    .start();
        }

        return fragmentView;
    }

    private LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        card.setBackground(Theme.createRoundRectDrawable(
                AndroidUtilities.dp(20),
                Theme.getColor(Theme.key_windowBackgroundWhite)
        ));
        return card;
    }

    private TextView label(Context context, String text) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        label.setTypeface(AndroidUtilities.bold());
        label.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        return label;
    }

    private void addChip(Context context, LinearLayout parent, String text, String prompt) {
        TextView chip = new TextView(context);
        chip.setText(text);
        chip.setGravity(Gravity.CENTER);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        chip.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        chip.setPadding(AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(999));
        bg.setColor(0x127A5CFF);
        chip.setBackground(bg);
        chip.setOnClickListener(v -> promptField.setText(prompt));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                AndroidUtilities.dp(34)
        );
        lp.rightMargin = AndroidUtilities.dp(8);
        parent.addView(chip, lp);
    }

    private TextView makeButton(Context context, String text, boolean primary) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        button.setTypeface(AndroidUtilities.bold());
        if (primary) {
            button.setTextColor(0xFFFFFFFF);
            GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFF735CFF, 0xFF4A84FF}
            );
            gradient.setCornerRadius(AndroidUtilities.dp(15));
            button.setBackground(gradient);
        } else {
            button.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
            button.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                    AndroidUtilities.dp(15),
                    Theme.getColor(Theme.key_windowBackgroundWhite),
                    Theme.getColor(Theme.key_listSelector)
            ));
        }
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
        statusView.setText("Applying changes...");
        if (plan.displayName != null) {
            previewName.setText(plan.displayName);
        }
        if (plan.bio != null) {
            previewBio.setText(plan.bio);
        }
        avatarCount.setText("Avatar options: " + Math.max(1, plan.avatarOptions.size()));

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
                setRunning(false, "Profile updated");
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
                setRunning(false, "Avatar updated");
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
