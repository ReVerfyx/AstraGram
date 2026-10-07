package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public class AstraPluginCell extends FrameLayout {
    private final TextView icon;
    private final TextView title;
    private final TextView subtitle;
    private final TextView status;

    public AstraPluginCell(Context context) {
        super(context);
        setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(5), AndroidUtilities.dp(12), AndroidUtilities.dp(5));

        FrameLayout card = new FrameLayout(context);
        card.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(18),
                Theme.getColor(Theme.key_windowBackgroundWhite),
                Theme.getColor(Theme.key_listSelector)
        ));
        addView(card, new LayoutParams(LayoutParams.MATCH_PARENT, AndroidUtilities.dp(72)));

        icon = new TextView(context);
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor(0xFFFFFFFF);
        icon.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        icon.setTypeface(AndroidUtilities.bold());
        GradientDrawable iconBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF7A5CFF, 0xFF4A84FF}
        );
        iconBg.setCornerRadius(AndroidUtilities.dp(15));
        icon.setBackground(iconBg);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(
                AndroidUtilities.dp(46), AndroidUtilities.dp(46), Gravity.START | Gravity.CENTER_VERTICAL
        );
        iconLp.leftMargin = AndroidUtilities.dp(13);
        card.addView(icon, iconLp);

        title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setSingleLine(true);
        FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START
        );
        titleLp.leftMargin = AndroidUtilities.dp(72);
        titleLp.rightMargin = AndroidUtilities.dp(100);
        titleLp.topMargin = AndroidUtilities.dp(13);
        card.addView(title, titleLp);

        subtitle = new TextView(context);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        subtitle.setSingleLine(true);
        FrameLayout.LayoutParams subtitleLp = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START
        );
        subtitleLp.leftMargin = AndroidUtilities.dp(72);
        subtitleLp.rightMargin = AndroidUtilities.dp(90);
        subtitleLp.bottomMargin = AndroidUtilities.dp(13);
        card.addView(subtitle, subtitleLp);

        status = new TextView(context);
        status.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        status.setTypeface(AndroidUtilities.bold());
        status.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams statusLp = new FrameLayout.LayoutParams(
                AndroidUtilities.dp(74), AndroidUtilities.dp(28), Gravity.END | Gravity.CENTER_VERTICAL
        );
        statusLp.rightMargin = AndroidUtilities.dp(13);
        card.addView(status, statusLp);
    }

    public void setData(String name, String version, boolean enabled, int permissionCount) {
        title.setText(name);
        subtitle.setText("v" + version + " • " + permissionCount + " permissions");
        String first = name == null || name.isEmpty() ? "A" : name.substring(0, 1).toUpperCase();
        icon.setText(first);

        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(AndroidUtilities.dp(999));
        if (enabled) {
            status.setText("Enabled");
            status.setTextColor(0xFF2AA66A);
            badge.setColor(0x182AA66A);
        } else {
            status.setText("Off");
            status.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            badge.setColor(0x12000000);
        }
        status.setBackground(badge);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(82), MeasureSpec.EXACTLY)
        );
    }
}
