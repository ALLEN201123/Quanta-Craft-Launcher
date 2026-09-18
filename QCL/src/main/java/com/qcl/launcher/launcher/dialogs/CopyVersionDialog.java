package com.qcl.launcher.launcher.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.qcl.launcher.launcher.list.local.game.GameListBean;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.utils.file.FileUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class CopyVersionDialog extends Dialog implements View.OnClickListener {
    private CopyVersionCallback callback;
    private CheckBox checkBox;
    private String currentName;
    private String currentPath;
    private EditText editName;
    private String gameDir;
    private ArrayList<GameListBean> list;
    private Button negative;
    private Button positive;
    private PrivateGameSetting privateGameSetting;
    private ProgressBar progressBar;

    /* loaded from: classes2.dex */
    public interface CopyVersionCallback {
        void onFinish();
    }

    public CopyVersionDialog(Context context, ArrayList<GameListBean> arrayList, PrivateGameSetting privateGameSetting, String str, String str2, String str3, CopyVersionCallback copyVersionCallback) {
        super(context);
        this.list = arrayList;
        this.privateGameSetting = privateGameSetting;
        this.gameDir = str;
        this.currentPath = str2;
        this.currentName = str3;
        this.callback = copyVersionCallback;
        setContentView(R.layout.dialog_copy_version);
        setCancelable(false);
        init();
    }

    private void init() {
        this.editName = (EditText) findViewById(R.id.copy_version);
        this.checkBox = (CheckBox) findViewById(R.id.check_copy_world);
        this.positive = (Button) findViewById(R.id.copy);
        this.negative = (Button) findViewById(R.id.cancel);
        this.progressBar = (ProgressBar) findViewById(R.id.copy_progress);
        this.editName.setText(this.currentName);
        this.positive.setOnClickListener(this);
        this.negative.setOnClickListener(this);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.positive && !this.editName.getText().toString().equals("") && !this.editName.getText().toString().contains("/")) {
            ArrayList arrayList = new ArrayList();
            Iterator<GameListBean> it = this.list.iterator();
            while (it.hasNext()) {
                arrayList.add(it.next().name);
            }
            if (arrayList.contains(this.editName.getText().toString())) {
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_copy_version_exist), 0).show();
            } else {
                final Handler handler = new Handler();
                new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.CopyVersionDialog$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        CopyVersionDialog.this.m218x327646e0(handler);
                    }
                }).start();
            }
        }
        if (view == this.negative) {
            dismiss();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$2$com-qcl-launcher-launcher-dialogs-CopyVersionDialog, reason: not valid java name */
    public /* synthetic */ void m218x327646e0(Handler handler) {
        handler.post(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.CopyVersionDialog$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                CopyVersionDialog.this.m216x1800e3de();
            }
        });
        FileUtils.createDirectory(this.currentPath + this.editName.getText().toString());
        FileUtils.copyFile(this.currentPath + this.currentName + "/" + this.currentName + ".jar", this.currentPath + this.editName.getText().toString() + "/" + this.editName.getText().toString() + ".jar");
        FileUtils.copyFile(this.currentPath + this.currentName + "/" + this.currentName + ".json", this.currentPath + this.editName.getText().toString() + "/" + this.editName.getText().toString() + ".json");
        if (this.checkBox.isChecked() && new File(this.gameDir + "/saves").exists() && new File(this.gameDir + "/saves").isDirectory()) {
            FileUtils.copyDirectory(this.gameDir + "/saves", this.currentPath + this.editName.getText().toString() + "/saves");
        }
        try {
            PrivateGameSetting privateGameSetting = (PrivateGameSetting) this.privateGameSetting.clone();
            privateGameSetting.enable = true;
            privateGameSetting.gameDirSetting.type = 1;
            GsonUtils.savePrivateGameSetting(privateGameSetting, this.currentPath + this.editName.getText().toString() + "/qcl.cfg");
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        handler.post(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.CopyVersionDialog$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                CopyVersionDialog.this.m217xa53b955f();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$0$com-qcl-launcher-launcher-dialogs-CopyVersionDialog, reason: not valid java name */
    public /* synthetic */ void m216x1800e3de() {
        this.progressBar.setVisibility(0);
        this.positive.setVisibility(8);
        this.negative.setEnabled(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$1$com-qcl-launcher-launcher-dialogs-CopyVersionDialog, reason: not valid java name */
    public /* synthetic */ void m217xa53b955f() {
        this.progressBar.setVisibility(8);
        this.positive.setVisibility(0);
        this.negative.setEnabled(true);
        this.callback.onFinish();
        dismiss();
    }
}
