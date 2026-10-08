package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
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

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(20),
                AndroidUtilities.dp(20), AndroidUtilities.dp(20));
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        fragmentView = root;

        TextView intro = new TextView(context);
        intro.setText("Astra AI uses your local Qwen server by default. You can override it with another OpenAI-compatible endpoint here.");
        intro.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        intro.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        intro.setGravity(Gravity.START);
        root.addView(intro, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18
        ));

        endpointField = makeField(context, "Endpoint URL");
        endpointField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        endpointField.setText(AstraProviderStore.endpoint(context));
        root.addView(endpointField, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 56, 0, 0, 0, 10
        ));

        modelField = makeField(context, "Model");
        modelField.setText(AstraProviderStore.model(context));
        root.addView(modelField, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 56, 0, 0, 0, 10
        ));

        keyField = makeField(context, "API key (optional)");
        keyField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyField.setText(AstraProviderStore.apiKey(context));
        root.addView(keyField, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 56, 0, 0, 0, 10
        ));

        TextView note = new TextView(context);
        note.setText("The built-in Astra AI needs no API key. A key is only used when you override the provider with one that requires it.");
        note.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        note.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        root.addView(note, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0
        ));

        return fragmentView;
    }

    private EditTextBoldCursor makeField(Context context, String hint) {
        EditTextBoldCursor field = new EditTextBoldCursor(context);
        field.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        field.setHint(hint);
        field.setSingleLine(true);
        field.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        field.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        field.setBackgroundDrawable(Theme.createRoundRectDrawable(
                AndroidUtilities.dp(12),
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
