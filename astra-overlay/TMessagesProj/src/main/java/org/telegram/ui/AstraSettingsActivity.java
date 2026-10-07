package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.messenger.astra.AstraSettings;
import org.telegram.messenger.astra.ai.AstraProviderStore;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

public class AstraSettingsActivity extends BaseFragment {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CHECK = 1;
    private static final int TYPE_VALUE = 2;
    private static final int TYPE_INFO = 3;

    private static final int ROW_APPEARANCE = 0;
    private static final int ROW_ANIMATIONS = 1;
    private static final int ROW_MOTION = 2;
    private static final int ROW_APPEARANCE_INFO = 3;

    private static final int ROW_AUTOMATION = 4;
    private static final int ROW_PROFILE_STUDIO = 5;
    private static final int ROW_PROFILE_AUTOMATION = 6;
    private static final int ROW_PROVIDER = 7;
    private static final int ROW_PROVIDER_CONFIG = 8;
    private static final int ROW_AUTOMATION_INFO = 9;

    private static final int ROW_EXTENSIONS = 10;
    private static final int ROW_PLUGINS = 11;
    private static final int ROW_EXTENSIONS_INFO = 12;
    private static final int ROW_COUNT = 13;

    private RecyclerListView listView;
    private ListAdapter adapter;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("AstraGram");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frame = new FrameLayout(context);
        frame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = frame;

        listView = new RecyclerListView(context);
        listView.setSections();
        actionBar.setAdaptiveBackground(listView);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter(context));

        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setDurations(AstraSettings.animationDuration(context, 260));
        animator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        animator.setDelayAnimations(false);
        animator.setSupportsChangeAnimations(false);
        listView.setItemAnimator(animator);

        listView.setOnItemClickListener((view, position) -> onRowClicked(context, position));
        frame.addView(listView, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT
        ));
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void onRowClicked(Context context, int position) {
        if (position == ROW_ANIMATIONS) {
            AstraSettings.setAnimationsEnabled(context, !AstraSettings.animationsEnabled(context));
            adapter.notifyItemChanged(ROW_ANIMATIONS);
        } else if (position == ROW_MOTION) {
            float value = AstraSettings.motionScale(context);
            float next = value < 0.9f ? 1.0f : value < 1.15f ? 1.25f : 0.75f;
            AstraSettings.setMotionScale(context, next);
            adapter.notifyItemChanged(ROW_MOTION);
        } else if (position == ROW_PROFILE_STUDIO) {
            presentFragment(new AstraProfileStudioActivity());
        } else if (position == ROW_PROFILE_AUTOMATION) {
            AstraSettings.setProfileAutomationEnabled(
                    context,
                    !AstraSettings.profileAutomationEnabled(context)
            );
            adapter.notifyItemChanged(ROW_PROFILE_AUTOMATION);
        } else if (position == ROW_PROVIDER || position == ROW_PROVIDER_CONFIG) {
            presentFragment(new AstraProviderSettingsActivity());
        } else if (position == ROW_PLUGINS) {
            presentFragment(new AstraPluginsActivity());
        }
    }

    private static String motionLabel(Context context) {
        float scale = AstraSettings.motionScale(context);
        if (scale < 0.9f) {
            return "Soft";
        }
        if (scale > 1.15f) {
            return "Expressive";
        }
        return "Normal";
    }

    private static String providerLabel(Context context) {
        if (!AstraProviderStore.isConfigured(context)) {
            return "Not configured";
        }
        String model = AstraProviderStore.model(context);
        return model.isEmpty() ? "Configured" : model;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context context;

        ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getItemCount() {
            return ROW_COUNT;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int p = holder.getAdapterPosition();
            return p == ROW_ANIMATIONS || p == ROW_MOTION || p == ROW_PROFILE_STUDIO
                    || p == ROW_PROFILE_AUTOMATION || p == ROW_PROVIDER
                    || p == ROW_PROVIDER_CONFIG || p == ROW_PLUGINS;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == TYPE_HEADER) {
                view = new HeaderCell(context);
            } else if (viewType == TYPE_CHECK) {
                view = new TextCheckCell(context);
            } else if (viewType == TYPE_VALUE) {
                view = new TextSettingsCell(context);
            } else {
                view = new TextInfoPrivacyCell(context);
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
            ));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == TYPE_HEADER) {
                HeaderCell cell = (HeaderCell) holder.itemView;
                if (position == ROW_APPEARANCE) {
                    cell.setText("Appearance");
                } else if (position == ROW_AUTOMATION) {
                    cell.setText("Automation");
                } else {
                    cell.setText("Extensions");
                }
            } else if (holder.getItemViewType() == TYPE_CHECK) {
                TextCheckCell cell = (TextCheckCell) holder.itemView;
                if (position == ROW_ANIMATIONS) {
                    cell.setTextAndCheck(
                            "Astra animations",
                            AstraSettings.animationsEnabled(context),
                            false
                    );
                } else {
                    cell.setTextAndCheck(
                            "Apply approved changes automatically",
                            AstraSettings.profileAutomationEnabled(context),
                            false
                    );
                }
            } else if (holder.getItemViewType() == TYPE_VALUE) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                if (position == ROW_MOTION) {
                    cell.setTextAndValue("Motion style", motionLabel(context), false);
                } else if (position == ROW_PROFILE_STUDIO) {
                    cell.setTextAndValue("Profile Studio", "Do it for me", true);
                } else if (position == ROW_PROVIDER) {
                    cell.setTextAndValue("AI provider", providerLabel(context), true);
                } else if (position == ROW_PROVIDER_CONFIG) {
                    cell.setTextAndValue("Provider settings", "Endpoint, model, key", false);
                } else {
                    cell.setTextAndValue("Plugins", ".asplug", false);
                }
            } else {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                if (position == ROW_APPEARANCE_INFO) {
                    cell.setText("Smooth motion is enabled by default. You can reduce or disable it here.");
                } else if (position == ROW_AUTOMATION_INFO) {
                    cell.setText("Profile Studio can apply generated names, bios and avatars directly after you start the action.");
                } else {
                    cell.setText("Install and manage AstraGram extension packages.");
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == ROW_APPEARANCE || position == ROW_AUTOMATION || position == ROW_EXTENSIONS) {
                return TYPE_HEADER;
            }
            if (position == ROW_ANIMATIONS || position == ROW_PROFILE_AUTOMATION) {
                return TYPE_CHECK;
            }
            if (position == ROW_APPEARANCE_INFO
                    || position == ROW_AUTOMATION_INFO
                    || position == ROW_EXTENSIONS_INFO) {
                return TYPE_INFO;
            }
            return TYPE_VALUE;
        }
    }
}
