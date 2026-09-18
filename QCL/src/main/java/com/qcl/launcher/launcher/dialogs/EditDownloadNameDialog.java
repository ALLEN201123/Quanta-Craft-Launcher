package com.qcl.launcher.launcher.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.uis.game.download.right.resource.DownloadResourceUI;
import com.qcl.launcher.utils.file.FileUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import java.io.File;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class EditDownloadNameDialog extends Dialog implements View.OnClickListener {
    private boolean alert;
    private String dir;
    private EditText editText;
    private Button negative;
    private Button positive;
    private DownloadResourceUI ui;
    private RemoteMod.Version version;

    public EditDownloadNameDialog(Context context, DownloadResourceUI downloadResourceUI, RemoteMod.Version version, boolean z, String str) {
        super(context);
        this.ui = downloadResourceUI;
        this.version = version;
        this.alert = z;
        this.dir = str;
        setContentView(R.layout.dialog_edit_download_name);
        setCancelable(false);
        init();
    }

    private void init() {
        this.editText = (EditText) findViewById(R.id.download_name);
        this.positive = (Button) findViewById(R.id.download);
        this.negative = (Button) findViewById(R.id.cancel);
        this.positive.setOnClickListener(this);
        this.negative.setOnClickListener(this);
        this.editText.setText(this.version.getFile().getFilename());
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        StringBuilder append;
        PrivateGameSetting privateGameSetting;
        if (view == this.positive && !this.editText.getText().toString().equals("") && !this.editText.getText().toString().contains("/")) {
            String str = null;
            if (this.ui.resourceType == 0) {
                if (this.ui.activity.uiManager.downloadUI.downloadUIManager.downloadModUI.gameVersion != null) {
                    str = this.ui.activity.launcherSetting.gameFileDirectory + "/versions/" + this.ui.activity.uiManager.downloadUI.downloadUIManager.downloadModUI.gameVersion;
                }
            } else if (this.ui.activity.uiManager.downloadUI.downloadUIManager.downloadResourcePackUI.gameVersion != null) {
                str = this.ui.activity.launcherSetting.gameFileDirectory + "/versions/" + this.ui.activity.uiManager.downloadUI.downloadUIManager.downloadResourcePackUI.gameVersion;
            }
            if (str != null) {
                String str2 = str + "/qcl.cfg";
                if (new File(str2).exists() && GsonUtils.getPrivateGameSettingFromFile(str2) != null && (GsonUtils.getPrivateGameSettingFromFile(str2).forceEnable || GsonUtils.getPrivateGameSettingFromFile(str2).enable)) {
                    privateGameSetting = GsonUtils.getPrivateGameSettingFromFile(str2);
                } else {
                    privateGameSetting = this.ui.activity.privateGameSetting;
                }
                if (privateGameSetting.gameDirSetting.type == 0) {
                    str = this.ui.activity.launcherSetting.gameFileDirectory;
                } else if (privateGameSetting.gameDirSetting.type != 1) {
                    str = privateGameSetting.gameDirSetting.path;
                }
            } else {
                PrivateGameSetting privateGameSetting2 = this.ui.activity.privateGameSetting;
                if (privateGameSetting2.gameDirSetting.type == 0 || privateGameSetting2.gameDirSetting.type == 1) {
                    str = this.ui.activity.launcherSetting.gameFileDirectory;
                } else {
                    str = privateGameSetting2.gameDirSetting.path;
                }
            }
            FileUtils.createDirectory(str + (this.ui.resourceType == 0 ? "/mods/" : "/resourcepacks/"));
            String name = this.version.getName();
            String url = this.version.getFile().getUrl();
            if (this.dir == null) {
                append = new StringBuilder().append(str).append(this.ui.resourceType != 0 ? "/resourcepacks/" : "/mods/");
            } else {
                append = new StringBuilder().append(this.dir).append("/");
            }
            DownloadTaskListBean downloadTaskListBean = new DownloadTaskListBean(name, url, append.append(this.editText.getText().toString()).toString(), "");
            ArrayList arrayList = new ArrayList();
            arrayList.add(downloadTaskListBean);
            DownloadDialog downloadDialog = new DownloadDialog(getContext(), this.ui.activity, arrayList, this.alert);
            dismiss();
            downloadDialog.show();
        }
        if (view == this.negative) {
            dismiss();
        }
    }
}
