package com.qcl.launcher.launcher.uis.game.manager.universal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.dialogs.UpdateDialog;
import com.qcl.launcher.launcher.list.download.ModUpdateListAdapter;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.launcher.mod.LocalModFile;
import com.qcl.launcher.launcher.mod.ModManager;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ModUpdateUI extends BaseUI implements View.OnClickListener {
    private Button cancel;
    private ListView listView;
    public ModManager modManager;
    public LinearLayout modUpdateUI;
    public ArrayList<LocalModFile.ModUpdate> modUpdates;
    private TextView noUpdateText;
    public ArrayList<LocalModFile.ModUpdate> selectedMods;
    private Button update;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onClick$0(DialogInterface dialogInterface, int i) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onClick$2(DialogInterface dialogInterface, int i) {
    }

    public ModUpdateUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.modUpdateUI = (LinearLayout) this.activity.findViewById(R.id.ui_mod_update);
        this.listView = (ListView) this.activity.findViewById(R.id.update_mod_list);
        this.noUpdateText = (TextView) this.activity.findViewById(R.id.no_mod_to_update);
        this.update = (Button) this.activity.findViewById(R.id.update_mods);
        this.cancel = (Button) this.activity.findViewById(R.id.cancel_update_mods);
        this.update.setOnClickListener(this);
        this.cancel.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.mod_update_ui_title), canGoBackToLast(), false);
        CustomAnimationUtils.showViewFromLeft(this.modUpdateUI, this.activity, this.context, true);
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.modUpdateUI, this.activity, this.context, true);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.update) {
            if (this.selectedMods.size() > 0) {
                UpdateDialog.OnUpdateFinish onUpdateFinish = new UpdateDialog.OnUpdateFinish() { // from class: com.qcl.launcher.launcher.uis.game.manager.universal.ModUpdateUI$$ExternalSyntheticLambda2
                    @Override // com.qcl.launcher.launcher.dialogs.UpdateDialog.OnUpdateFinish
                    public final void onFinish() {
                        ModUpdateUI.this.m544xd302c2fa();
                    }
                };
                ArrayList arrayList = new ArrayList();
                Iterator<LocalModFile.ModUpdate> it = this.selectedMods.iterator();
                while (it.hasNext()) {
                    RemoteMod.Version version = it.next().getCandidates().get(0);
                    arrayList.add(new DownloadTaskListBean(version.getName(), version.getFile().getUrl(), this.modManager.getModsDirectory() + "/" + version.getFile().getFilename(), ""));
                }
                new UpdateDialog(this.context, this.activity, arrayList, onUpdateFinish).show();
            } else {
                this.activity.backToLastUI();
                AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
                builder.setTitle(this.context.getString(R.string.dialog_install_success_title));
                builder.setMessage(this.context.getString(R.string.dialog_install_success_text));
                builder.setPositiveButton(this.context.getString(R.string.dialog_install_success_positive), new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.uis.game.manager.universal.ModUpdateUI$$ExternalSyntheticLambda1
                    @Override // android.content.DialogInterface.OnClickListener
                    public final void onClick(DialogInterface dialogInterface, int i) {
                        ModUpdateUI.lambda$onClick$2(dialogInterface, i);
                    }
                });
                builder.create().show();
            }
        }
        if (view == this.cancel) {
            this.activity.backToLastUI();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$1$com-qcl-launcher-launcher-uis-game-manager-universal-ModUpdateUI, reason: not valid java name */
    public /* synthetic */ void m544xd302c2fa() {
        Iterator<LocalModFile.ModUpdate> it = this.selectedMods.iterator();
        while (it.hasNext()) {
            try {
                it.next().getLocalMod().setOld(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.activity.backToLastUI();
        AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        builder.setTitle(this.context.getString(R.string.dialog_install_success_title));
        builder.setMessage(this.context.getString(R.string.dialog_install_success_text));
        builder.setPositiveButton(this.context.getString(R.string.dialog_install_success_positive), new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.uis.game.manager.universal.ModUpdateUI$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                ModUpdateUI.lambda$onClick$0(dialogInterface, i);
            }
        });
        builder.create().show();
    }

    private void init() {
        ArrayList<LocalModFile.ModUpdate> arrayList = new ArrayList<>();
        this.selectedMods = arrayList;
        arrayList.addAll(this.modUpdates);
        if (this.modUpdates.size() > 0) {
            this.listView.setAdapter((ListAdapter) new ModUpdateListAdapter(this.context, this));
            this.listView.setVisibility(0);
            this.noUpdateText.setVisibility(8);
            return;
        }
        this.listView.setVisibility(8);
        this.noUpdateText.setVisibility(0);
    }
}
