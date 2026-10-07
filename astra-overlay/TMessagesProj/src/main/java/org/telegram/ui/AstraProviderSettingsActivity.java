package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.astra.AstraSettings;
import org.telegram.messenger.astra.ai.AstraProviderStore;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

public class AstraProviderSettingsActivity extends BaseFragment {
    private static final int DONE = 1;

    private EditTextBoldCursor endpointField;
    private EditTextBoldCursor modelField;
    private EditTextBoldCursor keyField;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("AI provider");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == DONE) {
                    save(context);
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItemWithWidth(DONE, R.drawable.ic_ab_done, AndroidUtilities.dp(56));

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = scroll;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(28));
        scroll.addView(root, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT
        ));

        FrameLayout hero = new FrameLayout(context);
        GradientDrawable heroBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF6F5CFF, 0xFF4B83FF}
        );
        heroBg.setCornerRadius(AndroidUtilities.dp(22));
        hero.setBackground(heroBg);
        root.addView(hero, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 118, 0, 0, 0, 14
        ));

        TextView eyebrow = new TextView(context);
        eyebrow.setText("ASTRA AI");
        eyebrow.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        eyebrow.setTypeface(AndroidUtilities.bold());
        eyebrow.setTextColor(0xCCFFFFFF);
        eyebrow.setLetterSpacing(0.12f);
        FrameLayout.LayoutParams eLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.TOP
        );
        eLp.leftMargin = AndroidUtilities.dp(20);
        eLp.topMargin = AndroidUtilities.dp(18);
        hero.addView(eyebrow, eLp);

        TextView title = new TextView(context);
        title.setText("Connect your provider");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(0xFFFFFFFF);
        FrameLayout.LayoutParams tLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.TOP
        );
        tLp.leftMargin = AndroidUtilities.dp(20);
        tLp.topMargin = AndroidUtilities.dp(45);
        hero.addView(title, tLp);

        TextView subtitle = new TextView(context);
        subtitle.setText("OpenAI-compatible endpoint");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitle.setTextColor(0xD8FFFFFF);
        FrameLayout.LayoutParams sLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.BOTTOM
        );
        sLp.leftMargin = AndroidUtilities.dp(20);
        sLp.bottomMargin = AndroidUtilities.dp(17);
        hero.addView(subtitle, sLp);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        card.setBackground(Theme.createRoundRectDrawable(
                AndroidUtilities.dp(20),
                Theme.getColor(Theme.key_windowBackgroundWhite)
        ));
        root.addView(card, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12
        ));

        TextView intro = new TextView(context);
        intro.setText("AstraGram keeps provider settings separate from the automation engine, so you can replace the backend without changing the client.");
        intro.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        intro.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        intro.setGravity(Gravity.START);
        card.addView(intro, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16
        ));

        card.addView(fieldLabel(context, "Endpoint"), LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6
        ));
        endpointField = makeField(context, "https://...");
        endpointField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        endpointField.setText(AstraProviderStore.endpoint(context));
        card.addView(endpointField, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 54, 0, 0, 0, 14
        ));

        card.addView(fieldLabel(context, "Model"), LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6
        ));
        modelField = makeField(context, "Model name");
        modelField.setText(AstraProviderStore.model(context));
        card.addView(modelField, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 54, 0, 0, 0, 14
        ));

        card.addView(fieldLabel(context, "API key"), LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6
        ));
        keyField = makeField(context, "Optional");
        keyField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyField.setText(AstraProviderStore.apiKey(context));
        card.addView(keyField, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 54
        ));

        TextView note = new TextView(context);
        note.setText("The key is stored in AstraGram's private app storage. A restricted provider key is recommended.");
        note.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        note.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        root.addView(note, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 2, 4, 14
        ));

        TextView saveButton = new TextView(context);
        saveButton.setText("Save provider");
        saveButton.setGravity(Gravity.CENTER);
        saveButton.setTypeface(AndroidUtilities.bold());
        saveButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        saveButton.setTextColor(0xFFFFFFFF);
        GradientDrawable buttonBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF735CFF, 0xFF4A84FF}
        );
        buttonBg.setCornerRadius(AndroidUtilities.dp(15));
        saveButton.setBackground(buttonBg);
        saveButton.setOnClickListener(v -> save(context));
        root.addView(saveButton, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 50
        ));

        if (AstraSettings.animationsEnabled(context)) {
            root.setAlpha(0f);
            root.setTranslationY(AndroidUtilities.dp(8));
            root.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(AstraSettings.animationDuration(context, 280))
                    .start();
        }

        return fragmentView;
    }

    private TextView fieldLabel(Context context, String text) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        label.setTypeface(AndroidUtilities.bold());
        label.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        return label;
    }

    private EditTextBoldCursor makeField(Context context, String hint) {
        EditTextBoldCursor field = new EditTextBoldCursor(context);
        field.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        field.setHint(hint);
        field.setSingleLine(true);
        field.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        field.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        field.setBackgroundDrawable(Theme.createRoundRectDrawable(
                AndroidUtilities.dp(13),
                Theme.getColor(Theme.key_windowBackgroundGray)
        ));
        field.setPadding(AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14), 0);
        field.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        return field;
    }

    private void save(Context context) {
        String endpoint = endpointField.getText().toString().trim();
        String model = modelField.getText().toString().trim();
        String apiKey = keyField.getText().toString().trim();

        if (!endpoint.isEmpty() && !endpoint.startsWith("https://")) {
            Toast.makeText(context, "Use an HTTPS endpoint", Toast.LENGTH_SHORT).show();
            return;
        }
        if (endpoint.isEmpty() != model.isEmpty()) {
            Toast.makeText(context, "Set both endpoint and model", Toast.LENGTH_SHORT).show();
            return;
        }

        AstraProviderStore.save(context, endpoint, model, apiKey);
        Toast.makeText(context, "Provider saved", Toast.LENGTH_SHORT).show();
        finishFragment();
    }
}
