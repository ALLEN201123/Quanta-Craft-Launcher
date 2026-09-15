package com.qcl.launcher.launcher.uis.game.manager.universal;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.qcl.launcher.R;
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

public class ModUpdateUI extends BaseUI implements View.OnClickListener {

    public LinearLayout modUpdateUI;

    public ModManager modManager;
    public ArrayList<LocalModFile.ModUpdate> modUpdates;
    public ArrayList<LocalModFile.ModUpdate> selectedMods;

    private ListView listView;
    private TextView noUpdateText;

    private Button update;
    private Button cancel;

    public ModUpdateUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        modUpdateUI = activity.findViewById(R.id.ui_mod_update);

        listView = activity.findViewById(R.id.update_mod_list);
        noUpdateText = activity.findViewById(R.id.no_mod_to_update);

        update = activity.findViewById(R.id.update_mods);
        cancel = activity.findViewById(R.id.cancel_update_mods);
        update.setOnClickListener(this);
        cancel.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        activity.showBarTitle(context.getResources().getString(R.string.mod_update_ui_title),canGoBackToLast(),false);
        CustomAnimationUtils.showViewFromLeft(modUpdateUI,activity,context,true);
        init();
    }

    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(modUpdateUI,activity,context,true);
    }

    @Override
    public void onClick(View view) {
        if (view == update) {
            if (selectedMods.size() > 0) {
                UpdateDialog.OnUpdateFinish onUpdateFinish = () -> {
                    for (LocalModFile.ModUpdate modUpdate : selectedMods) {
                        try {
                            modUpdate.getLocalMod().setOld(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    activity.backToLastUI();
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(context.getString(R.string.dialog_install_success_title));
                    builder.setMessage(context.getString(R.string.dialog_install_success_text));
                    builder.setPositiveButton(context.getString(R.string.dialog_install_success_positive), (dialogInterface, i) -> {});
                    builder.create().show();
                };
                ArrayList<DownloadTaskListBean> downloadTaskListBeans = new ArrayList<>();
                for (LocalModFile.ModUpdate modUpdate : selectedMods) {
                    RemoteMod.Version version = modUpdate.getCandidates().get(0);
                    String name = version.getName();
                    String url = version.getFile().getUrl();
                    String path = modManager.getModsDirectory() + "/" + version.getFile().getFilename();
                    DownloadTaskListBean bean = new DownloadTaskListBean(name,url,path,"");
                    downloadTaskListBeans.add(bean);
                }
                UpdateDialog dialog = new UpdateDialog(context,activity,downloadTaskListBeans,onUpdateFinish);
                dialog.show();
            }
            else {
                activity.backToLastUI();
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(context.getString(R.string.dialog_install_success_title));
                builder.setMessage(context.getString(R.string.dialog_install_success_text));
                builder.setPositiveButton(context.getString(R.string.dialog_install_success_positive), (dialogInterface, i) -> {});
                builder.create().show();
            }
        }
        if (view == cancel) {
            activity.backToLastUI();
        }
    }

    private void init() {
        selectedMods = new ArrayList<>();
        selectedMods.addAll(modUpdates);
        if (modUpdates.size() > 0) {
            ModUpdateListAdapter adapter = new ModUpdateListAdapter(context,this);
            listView.setAdapter(adapter);
            listView.setVisibility(View.VISIBLE);
            noUpdateText.setVisibility(View.GONE);
        }
        else {
            listView.setVisibility(View.GONE);
            noUpdateText.setVisibility(View.VISIBLE);
        }
    }
}
