package org.telegram.ui.Cells;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.astra.AstraSettings;
import org.telegram.ui.ActionBar.Theme;

public class AstraHeroCell extends FrameLayout {
    private final FrameLayout card;
    private final TextView star;
    private ValueAnimator animator;

    public AstraHeroCell(Context context) {
        super(context);
        setWillNotDraw(false);
        setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(8));

        card = new FrameLayout(context);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF6F5CFF, 0xFF4D7CFE, 0xFF41A3FF}
        );
        gradient.setCornerRadius(AndroidUtilities.dp(26));
        card.setBackground(gradient);
        addView(card, new LayoutParams(LayoutParams.MATCH_PARENT, AndroidUtilities.dp(156)));

        TextView eyebrow = new TextView(context);
        eyebrow.setText("ASTRAGRAM");
        eyebrow.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        eyebrow.setTypeface(AndroidUtilities.bold());
        eyebrow.setTextColor(0xCCFFFFFF);
        eyebrow.setLetterSpacing(0.12f);
        FrameLayout.LayoutParams eyebrowLp = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.TOP
        );
        eyebrowLp.leftMargin = AndroidUtilities.dp(22);
        eyebrowLp.topMargin = AndroidUtilities.dp(20);
        card.addView(eyebrow, eyebrowLp);

        TextView title = new TextView(context);
        title.setText("Telegram, made yours.");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Color.WHITE);
        FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.TOP
        );
        titleLp.leftMargin = AndroidUtilities.dp(22);
        titleLp.topMargin = AndroidUtilities.dp(48);
        titleLp.rightMargin = AndroidUtilities.dp(70);
        card.addView(title, titleLp);

        TextView subtitle = new TextView(context);
        subtitle.setText("Smooth motion • plugins • automation");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitle.setTextColor(0xDDFFFFFF);
        FrameLayout.LayoutParams subtitleLp = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.TOP
        );
        subtitleLp.leftMargin = AndroidUtilities.dp(22);
        subtitleLp.topMargin = AndroidUtilities.dp(91);
        subtitleLp.rightMargin = AndroidUtilities.dp(18);
        card.addView(subtitle, subtitleLp);

        TextView badge = new TextView(context);
        badge.setText("Astra UI");
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        badge.setTypeface(AndroidUtilities.bold());
        badge.setGravity(Gravity.CENTER);
        badge.setTextColor(Color.WHITE);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(0x24FFFFFF);
        badgeBg.setCornerRadius(AndroidUtilities.dp(999));
        badge.setBackground(badgeBg);
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(
                AndroidUtilities.dp(82), AndroidUtilities.dp(30), Gravity.START | Gravity.BOTTOM
        );
        badgeLp.leftMargin = AndroidUtilities.dp(22);
        badgeLp.bottomMargin = AndroidUtilities.dp(17);
        card.addView(badge, badgeLp);

        star = new TextView(context);
        star.setText("✦");
        star.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 35);
        star.setGravity(Gravity.CENTER);
        star.setTextColor(0xF2FFFFFF);
        GradientDrawable orb = new GradientDrawable();
        orb.setShape(GradientDrawable.OVAL);
        orb.setColor(0x24FFFFFF);
        orb.setStroke(AndroidUtilities.dp(1), 0x38FFFFFF);
        star.setBackground(orb);
        FrameLayout.LayoutParams starLp = new FrameLayout.LayoutParams(
                AndroidUtilities.dp(58), AndroidUtilities.dp(58), Gravity.END | Gravity.TOP
        );
        starLp.rightMargin = AndroidUtilities.dp(18);
        starLp.topMargin = AndroidUtilities.dp(18);
        card.addView(star, starLp);

        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!AstraSettings.animationsEnabled(getContext())) {
            return;
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(AstraSettings.animationDuration(getContext(), 1700));
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(a -> {
            float v = (float) a.getAnimatedValue();
            star.setScaleX(1f + 0.08f * v);
            star.setScaleY(1f + 0.08f * v);
            star.setAlpha(0.78f + 0.22f * v);
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(176), MeasureSpec.EXACTLY)
        );
    }
}
