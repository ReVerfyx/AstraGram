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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.messenger.astra.plugins.AstraPluginStore;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
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
        listView.setAdapter(adapter = new ListAdapter(context));
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
        private static final int TYPE_VALUE = 1;
        private static final int TYPE_INFO = 2;

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
            if (position == 0) {
                ((HeaderCell) holder.itemView).setText("Extensions");
            } else if (position == 1) {
                ((TextSettingsCell) holder.itemView).setTextAndValue("Install plugin", ".asplug", plugins.isEmpty());
            } else if (position >= 2 && position < 2 + plugins.size()) {
                AstraPluginStore.InstalledPlugin plugin = plugins.get(position - 2);
                String state = plugin.enabled ? "enabled" : "disabled";
                String value = "v" + plugin.manifest.version + " • " + state + " • "
                        + plugin.manifest.permissions.size() + " permissions";
                ((TextSettingsCell) holder.itemView).setTextAndValue(plugin.manifest.name, value, position < 1 + plugins.size());
            } else {
                ((TextInfoPrivacyCell) holder.itemView).setText(
                        plugins.isEmpty()
                                ? "No plugins installed. AstraGram validates the package before it is added."
                                : "Installed plugins do not receive requested permissions automatically."
                );
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return TYPE_HEADER;
            }
            if (position == 2 + plugins.size()) {
                return TYPE_INFO;
            }
            return TYPE_VALUE;
        }
    }
}
