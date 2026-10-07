package org.telegram.ui;

import android.content.Context;
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
import org.telegram.messenger.astra.plugins.AsplugPermission;
import org.telegram.messenger.astra.plugins.AstraPluginStore;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.AstraPluginCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.List;

public class AstraPluginDetailsActivity extends BaseFragment {
    private final String pluginId;
    private AstraPluginStore.InstalledPlugin plugin;
    private final List<AsplugPermission> permissions = new ArrayList<>();
    private RecyclerListView listView;
    private Adapter adapter;

    public AstraPluginDetailsActivity(String pluginId) {
        this.pluginId = pluginId;
    }

    @Override
    public View createView(Context context) {
        plugin = AstraPluginStore.find(context, pluginId);
        if (plugin == null) {
            finishFragment();
            return new View(context);
        }
        permissions.clear();
        permissions.addAll(plugin.manifest.permissions);

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(plugin.manifest.name);
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
        listView.setClipToPadding(false);
        listView.setPadding(0, org.telegram.messenger.AndroidUtilities.dp(8), 0,
                org.telegram.messenger.AndroidUtilities.dp(16));
        listView.setAdapter(adapter = new Adapter(context));

        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setDurations(AstraSettings.animationDuration(context, 240));
        animator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        animator.setSupportsChangeAnimations(false);
        listView.setItemAnimator(animator);

        listView.setOnItemClickListener((view, position) -> onRowClicked(context, position));
        frame.addView(listView, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.MATCH_PARENT
        ));
        return fragmentView;
    }

    private int permissionsStart() {
        return 3;
    }

    private int infoRow() {
        return permissionsStart() + permissions.size();
    }

    private void onRowClicked(Context context, int position) {
        if (position == 1) {
            boolean next = !AstraPluginStore.isEnabled(context, pluginId);
            try {
                AstraPluginStore.setEnabled(context, pluginId, next);
                reload(context);
            } catch (SecurityException error) {
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
            }
            return;
        }

        int permissionIndex = position - permissionsStart();
        if (permissionIndex >= 0 && permissionIndex < permissions.size()) {
            AsplugPermission permission = permissions.get(permissionIndex);
            boolean current = AstraPluginStore.isPermissionGranted(context, pluginId, permission);
            AstraPluginStore.setPermissionGranted(context, pluginId, permission, !current);
            reload(context);
        }
    }

    private void reload(Context context) {
        plugin = AstraPluginStore.find(context, pluginId);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private class Adapter extends RecyclerListView.SelectionAdapter {
        private static final int TYPE_SUMMARY = 0;
        private static final int TYPE_HEADER = 1;
        private static final int TYPE_CHECK = 2;
        private static final int TYPE_INFO = 3;
        private final Context context;

        Adapter(Context context) {
            this.context = context;
        }

        @Override
        public int getItemCount() {
            return infoRow() + 1;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return position == 1 || (position >= permissionsStart() && position < infoRow());
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == TYPE_SUMMARY) {
                view = new AstraPluginCell(context);
            } else if (viewType == TYPE_HEADER) {
                view = new HeaderCell(context);
            } else if (viewType == TYPE_CHECK) {
                view = new TextCheckCell(context);
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
            if (holder.getItemViewType() == TYPE_SUMMARY) {
                ((AstraPluginCell) holder.itemView).setData(
                        plugin.manifest.name,
                        plugin.manifest.version,
                        plugin.enabled,
                        plugin.manifest.permissions.size()
                );
            } else if (holder.getItemViewType() == TYPE_HEADER) {
                ((HeaderCell) holder.itemView).setText("Permissions");
            } else if (holder.getItemViewType() == TYPE_CHECK) {
                TextCheckCell cell = (TextCheckCell) holder.itemView;
                if (position == 1) {
                    cell.setTextAndCheck(
                            "Enabled",
                            plugin != null && AstraPluginStore.isEnabled(context, pluginId),
                            false
                    );
                } else {
                    AsplugPermission permission = permissions.get(position - permissionsStart());
                    cell.setTextAndCheck(
                            friendlyName(permission),
                            AstraPluginStore.isPermissionGranted(context, pluginId, permission),
                            position < infoRow() - 1
                    );
                }
            } else {
                ((TextInfoPrivacyCell) holder.itemView).setText(
                        "AstraGram enables this plugin only after all requested permissions are granted. Revoking one of them disables the plugin automatically."
                );
            }
        }

        private String friendlyName(AsplugPermission permission) {
            switch (permission) {
                case UI: return "Modify interface";
                case NETWORK: return "Internet access";
                case FILES_READ: return "Read files";
                case FILES_WRITE: return "Write files";
                case MESSAGES_READ: return "Read messages";
                case MESSAGES_SEND: return "Send messages";
                case PROFILE_READ: return "Read profile";
                case PROFILE_WRITE: return "Change profile";
                case AUTOMATION: return "Run automations";
                default: return permission.wireName();
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return TYPE_SUMMARY;
            }
            if (position == 2) {
                return TYPE_HEADER;
            }
            if (position == infoRow()) {
                return TYPE_INFO;
            }
            return TYPE_CHECK;
        }
    }
}
