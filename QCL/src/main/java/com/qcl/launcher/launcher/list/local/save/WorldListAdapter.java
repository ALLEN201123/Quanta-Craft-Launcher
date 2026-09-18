package com.qcl.launcher.launcher.list.local.save;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.game.World;
import com.qcl.launcher.utils.versioning.VersionNumber;
import com.tungsten.filepicker.FileBrowser;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class WorldListAdapter extends BaseAdapter {
    private MainActivity activity;
    private Context context;
    private ArrayList<World> list;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ViewHolder {
        TextView info;
        ImageButton more;
        TextView name;

        private ViewHolder() {
        }
    }

    public WorldListAdapter(Context context, MainActivity mainActivity, ArrayList<World> arrayList) {
        this.context = context;
        this.activity = mainActivity;
        this.list = arrayList;
    }

    @Override // android.widget.Adapter
    public int getCount() {
        return this.list.size();
    }

    @Override // android.widget.Adapter
    public Object getItem(int i) {
        return this.list.get(i);
    }

    @Override // android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        View view2;
        final ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view2 = LayoutInflater.from(this.context).inflate(R.layout.item_save, (ViewGroup) null);
            viewHolder.name = (TextView) view2.findViewById(R.id.save_name);
            viewHolder.info = (TextView) view2.findViewById(R.id.save_info);
            viewHolder.more = (ImageButton) view2.findViewById(R.id.more_vert);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        final World world = this.list.get(i);
        viewHolder.name.setText(world.getWorldName());
        String fileName = world.getFileName();
        viewHolder.info.setText(this.context.getString(R.string.world_manager_ui_info).replace("%f", fileName).replace("%t", DateTimeFormatter.ofPattern(this.context.getString(R.string.time_pattern)).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(world.getLastPlayed()))).replace("%v", world.getGameVersion() == null ? this.context.getString(R.string.world_manager_ui_unknown_game_version) : world.getGameVersion()));
        viewHolder.more.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.local.save.WorldListAdapter$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                WorldListAdapter.this.m456x32533f36(viewHolder, world, view3);
            }
        });
        return view2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$1$com-qcl-launcher-launcher-list-local-save-WorldListAdapter, reason: not valid java name */
    public /* synthetic */ void m456x32533f36(ViewHolder viewHolder, final World world, View view) {
        PopupMenu popupMenu = new PopupMenu(new ContextThemeWrapper(this.context, R.style.MenuStyle), (View) viewHolder.more.getParent(), 8388613);
        popupMenu.inflate(R.menu.world_menu);
        popupMenu.setForceShowIcon(true);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: com.qcl.launcher.launcher.list.local.save.WorldListAdapter$$ExternalSyntheticLambda1
            @Override // androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener
            public final boolean onMenuItemClick(MenuItem menuItem) {
                return WorldListAdapter.this.m455x2c4f73d7(world, menuItem);
            }
        });
        popupMenu.show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$0$com-qcl-launcher-launcher-list-local-save-WorldListAdapter, reason: not valid java name */
    public /* synthetic */ boolean m455x2c4f73d7(World world, MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.export_world) {
            this.activity.uiManager.exportWorldUI.world = world;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.exportWorldUI);
            return true;
        }
        if (itemId != R.id.manage_assets) {
            if (itemId != R.id.open_dir) {
                return false;
            }
            Intent intent = new Intent(this.context, (Class<?>) FileBrowser.class);
            intent.putExtra("INITIAL_DIRECTORY", world.getFile().toString());
            this.context.startActivity(intent);
            return true;
        }
        if (world.getGameVersion() == null || (VersionNumber.isIntVersionNumber(world.getGameVersion()) && VersionNumber.asVersion(world.getGameVersion()).compareTo(VersionNumber.asVersion("1.13")) < 0)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
            builder.setTitle(this.context.getString(R.string.dialog_manage_packmc_title));
            builder.setMessage(this.context.getString(R.string.dialog_manage_packmc_msg));
            builder.setPositiveButton(this.context.getString(R.string.dialog_manage_packmc_positive), (DialogInterface.OnClickListener) null);
            builder.create().show();
        } else {
            this.activity.uiManager.packMcManagerUI.world = world;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.packMcManagerUI);
        }
        return true;
    }
}
