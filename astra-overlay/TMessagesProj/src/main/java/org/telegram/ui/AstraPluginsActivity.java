package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.messenger.astra.AstraSettings;
import org.telegram.messenger.astra.plugins.AstraPluginStore;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.AstraPluginCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AstraPluginsActivity extends BaseFragment {
    private static final int REQUEST_PLUGIN = 7124;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final List<AstraPluginStore.InstalledPlugin> plugins = new ArrayList<>();

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Astra plugins");
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
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setVerticalScrollBarEnabled(false);
        listView.setClipToPadding(false);
        listView.setPadding(0, 0, 0, org.telegram.messenger.AndroidUtilities.dp(16));
        listView.setAdapter(adapter = new ListAdapter(context));

        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setDurations(AstraSettings.animationDuration(context, 260));
        animator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        animator.setSupportsChangeAnimations(false);
        listView.setItemAnimator(animator);

        listView.setOnItemClickListener((view, position) -> {
            if (position == 1) {
                pickPlugin();
            } else if (position >= 2 && position < 2 + plugins.size()) {
                presentFragment(new AstraPluginDetailsActivity(plugins.get(position - 2).manifest.id));
            }
        });
        frame.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        reload(context);
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getParentActivity() != null) {
            reload(getParentActivity());
        }
    }

    private void pickPlugin() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_PLUGIN);
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_PLUGIN || resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null || getParentActivity() == null) {
            return;
        }
        try (InputStream input = getParentActivity().getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IllegalStateException("Could not open plugin");
            }
            AstraPluginStore.InstalledPlugin plugin = AstraPluginStore.install(getParentActivity(), input);
            Toast.makeText(getParentActivity(), plugin.manifest.name + " installed", Toast.LENGTH_SHORT).show();
            reload(getParentActivity());
        } catch (Exception e) {
            Toast.makeText(getParentActivity(), "Plugin install failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void reload(Context context) {
        plugins.clear();
        plugins.addAll(AstraPluginStore.list(context));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_INSTALL = 1;
        private static final int TYPE_PLUGIN = 2;
        private static final int TYPE_INFO = 3;

        private final Context context;

        ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getItemCount() {
            return 3 + plugins.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return position == 1 || (position >= 2 && position < 2 + plugins.size());
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == TYPE_HEADER) {
                view = new HeaderCell(context);
            } else if (viewType == TYPE_INSTALL) {
                view = new TextSettingsCell(context);
            } else if (viewType == TYPE_PLUGIN) {
                view = new AstraPluginCell(context);
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
            if (position == 0) {
                ((HeaderCell) holder.itemView).setText("Extensions");
            } else if (position == 1) {
                ((TextSettingsCell) holder.itemView).setTextAndValue(
                        "Install plugin",
                        "Choose an .asplug file",
                        false
                );
            } else if (position >= 2 && position < 2 + plugins.size()) {
                AstraPluginStore.InstalledPlugin plugin = plugins.get(position - 2);
                ((AstraPluginCell) holder.itemView).setData(
                        plugin.manifest.name,
                        plugin.manifest.version,
                        plugin.enabled,
                        plugin.manifest.permissions.size()
                );
            } else {
                ((TextInfoPrivacyCell) holder.itemView).setText(
                        plugins.isEmpty()
                                ? "No plugins installed yet. Add an .asplug file to start."
                                : "Plugins stay disabled until you explicitly grant their requested permissions."
                );
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return TYPE_HEADER;
            }
            if (position == 1) {
                return TYPE_INSTALL;
            }
            if (position >= 2 && position < 2 + plugins.size()) {
                return TYPE_PLUGIN;
            }
            return TYPE_INFO;
        }
    }
}
