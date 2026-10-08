package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.astra.ai.AstraChannelAiStore;
import org.telegram.messenger.astra.ai.AstraTextGenerator;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AstraChannelAiSettingsActivity extends BaseFragment {
    private static final int DONE = 1;
    private static final String SYSTEM_PROMPT =
            "You are Astra AI, the editor of a Telegram channel. " +
            "Return only valid JSON in the exact form {\"posts\":[\"...\"]}. " +
            "Create exactly seven distinct posts. Do not wrap JSON in markdown fences. " +
            "Follow the user's requested tone closely. Keep each post suitable for Telegram and avoid repetitive filler.";

    private final long chatId;
    private boolean enabled;
    private boolean running;

    private TextCheckCell enabledCell;
    private EditTextBoldCursor instructionField;
    private EditTextBoldCursor timeField;
    private TextView scheduleButton;
    private TextView statusView;

    public AstraChannelAiSettingsActivity(long chatId) {
        this.chatId = chatId;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("AI settings");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1 && !running) {
                    finishFragment();
                } else if (id == DONE && !running) {
                    if (save(context, true)) {
                        Toast.makeText(context, "AI settings saved", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItemWithWidth(DONE, R.drawable.ic_ab_done, AndroidUtilities.dp(56));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = root;

        enabled = AstraChannelAiStore.enabled(context, currentAccount, chatId);
        enabledCell = new TextCheckCell(context);
        enabledCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        enabledCell.setTextAndCheck("AI manages this channel", enabled, false);
        enabledCell.setOnClickListener(v -> {
            if (running) return;
            enabled = !enabled;
            enabledCell.setChecked(enabled);
        });
        root.addView(enabledCell, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 0
        ));

        TextView hint = new TextView(context);
        hint.setText("Describe the channel in ordinary words. Astra AI will create seven different posts and schedule them in Telegram, so they can publish even when your phone is offline.");
        hint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        hint.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        hint.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12),
                AndroidUtilities.dp(20), AndroidUtilities.dp(10));
        root.addView(hint, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT
        ));

        instructionField = new EditTextBoldCursor(context);
        instructionField.setHint("For example: every evening post short gaming news, friendly style, no clickbait...");
        instructionField.setText(AstraChannelAiStore.instruction(context, currentAccount, chatId));
        instructionField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        instructionField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        instructionField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        instructionField.setGravity(Gravity.TOP | Gravity.START);
        instructionField.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        instructionField.setMinLines(5);
        instructionField.setMaxLines(8);
        instructionField.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14),
                AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        instructionField.setBackgroundDrawable(Theme.createRoundRectDrawable(
                AndroidUtilities.dp(14), Theme.getColor(Theme.key_windowBackgroundWhite)
        ));
        root.addView(instructionField, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16, 6, 16, 12
        ));

        timeField = new EditTextBoldCursor(context);
        timeField.setHint("18:00");
        timeField.setSingleLine(true);
        timeField.setText(String.format("%02d:%02d",
                AstraChannelAiStore.hour(context, currentAccount, chatId),
                AstraChannelAiStore.minute(context, currentAccount, chatId)));
        timeField.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        timeField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        timeField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        timeField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        timeField.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);
        timeField.setBackgroundDrawable(Theme.createRoundRectDrawable(
                AndroidUtilities.dp(14), Theme.getColor(Theme.key_windowBackgroundWhite)
        ));
        root.addView(timeField, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 54, 16, 0, 16, 12
        ));

        scheduleButton = new TextView(context);
        scheduleButton.setText("Generate and schedule 7 posts");
        scheduleButton.setGravity(Gravity.CENTER);
        scheduleButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        scheduleButton.setTypeface(AndroidUtilities.bold());
        scheduleButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        scheduleButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(14),
                Theme.getColor(Theme.key_featuredStickers_addButton),
                Theme.getColor(Theme.key_featuredStickers_addButtonPressed)
        ));
        scheduleButton.setOnClickListener(v -> scheduleWeek(context));
        root.addView(scheduleButton, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 50, 16, 4, 16, 8
        ));

        statusView = new TextView(context);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        statusView.setGravity(Gravity.CENTER_HORIZONTAL);
        statusView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6),
                AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        root.addView(statusView, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT
        ));

        return fragmentView;
    }

    private boolean save(Context context, boolean showErrors) {
        int[] time = parseTime(timeField.getText().toString());
        if (time == null) {
            if (showErrors) {
                Toast.makeText(context, "Time must look like 18:00", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        AstraChannelAiStore.save(
                context,
                currentAccount,
                chatId,
                enabled,
                instructionField.getText().toString(),
                time[0],
                time[1]
        );
        return true;
    }

    private void scheduleWeek(Context context) {
        if (running) return;
        String instruction = instructionField.getText().toString().trim();
        if (instruction.isEmpty()) {
            Toast.makeText(context, "Describe what the AI should post", Toast.LENGTH_SHORT).show();
            return;
        }
        int[] time = parseTime(timeField.getText().toString());
        if (time == null) {
            Toast.makeText(context, "Time must look like 18:00", Toast.LENGTH_SHORT).show();
            return;
        }

        enabled = true;
        enabledCell.setChecked(true);
        save(context, false);
        setRunning(true, "Astra AI is writing seven posts...");

        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(chatId);
        String title = chat == null || chat.title == null ? "Telegram channel" : chat.title;
        String prompt = "Channel: " + title + "\n"
                + "Instruction from owner: " + instruction + "\n"
                + "Create exactly seven posts for seven consecutive days. "
                + "Make them different from each other. Return JSON only.";

        Context appContext = context.getApplicationContext();
        Utilities.globalQueue.postRunnable(() -> {
            try {
                String raw = AstraTextGenerator.generate(appContext, SYSTEM_PROMPT, prompt);
                List<String> posts = parsePosts(raw);
                AndroidUtilities.runOnUIThread(() -> schedulePosts(posts, time[0], time[1]));
            } catch (Throwable error) {
                AndroidUtilities.runOnUIThread(() -> fail(error));
            }
        });
    }

    private void schedulePosts(List<String> posts, int hour, int minute) {
        if (posts.size() < 7) {
            fail(new IllegalStateException("AI returned fewer than 7 posts"));
            return;
        }

        Calendar first = Calendar.getInstance();
        first.set(Calendar.HOUR_OF_DAY, hour);
        first.set(Calendar.MINUTE, minute);
        first.set(Calendar.SECOND, 0);
        first.set(Calendar.MILLISECOND, 0);
        if (first.getTimeInMillis() <= System.currentTimeMillis() + 60_000L) {
            first.add(Calendar.DAY_OF_YEAR, 1);
        }

        long dialogId = -chatId;
        for (int i = 0; i < 7; i++) {
            Calendar date = (Calendar) first.clone();
            date.add(Calendar.DAY_OF_YEAR, i);
            int scheduleDate = (int) (date.getTimeInMillis() / 1000L);
            SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(
                    posts.get(i),
                    dialogId,
                    null,
                    null,
                    null,
                    true,
                    null,
                    null,
                    null,
                    true,
                    scheduleDate,
                    0,
                    null,
                    false
            );
            SendMessagesHelper.getInstance(currentAccount).sendMessage(params);
        }

        setRunning(false, "Done. Seven posts are scheduled in Telegram.");
        if (getParentActivity() != null) {
            Toast.makeText(getParentActivity(), "7 posts scheduled", Toast.LENGTH_SHORT).show();
        }
    }

    private static List<String> parsePosts(String raw) throws Exception {
        String content = raw == null ? "" : raw.trim();
        if (content.startsWith("\u0060\u0060\u0060")) {
            int newline = content.indexOf('\n');
            int end = content.lastIndexOf("\u0060\u0060\u0060");
            if (newline >= 0 && end > newline) {
                content = content.substring(newline + 1, end).trim();
            }
        }
        JSONObject object = new JSONObject(content);
        JSONArray array = object.getJSONArray("posts");
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String post = array.optString(i, "").trim();
            if (!post.isEmpty()) {
                result.add(post);
            }
        }
        return result;
    }

    private static int[] parseTime(String value) {
        if (value == null) return null;
        String[] parts = value.trim().split(":");
        if (parts.length != 2) return null;
        try {
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (h < 0 || h > 23 || m < 0 || m > 59) return null;
            return new int[] { h, m };
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private void fail(Throwable error) {
        String message = error == null || error.getMessage() == null
                ? "Astra AI request failed"
                : error.getMessage();
        setRunning(false, message);
        if (getParentActivity() != null) {
            Toast.makeText(getParentActivity(), message, Toast.LENGTH_LONG).show();
        }
    }

    private void setRunning(boolean value, String status) {
        running = value;
        enabledCell.setEnabled(!value);
        instructionField.setEnabled(!value);
        timeField.setEnabled(!value);
        scheduleButton.setEnabled(!value);
        scheduleButton.setAlpha(value ? 0.55f : 1f);
        statusView.setText(status);
    }
}
