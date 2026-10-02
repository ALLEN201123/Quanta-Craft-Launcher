package com.qcl.launcher.launcher.uis.game.version.universal;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.mod.export.ModpackExporter;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

import java.io.File;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ExportPackageInfoUI extends BaseUI implements View.OnClickListener {
    public LinearLayout exportPackageInfoUI;

    private EditText nameEdit;
    private EditText versionEdit;
    private EditText authorEdit;
    private EditText filenameEdit;
    private Button next;

    /** 当前选中的整合包类型，由类型选择页写入 */
    private int packType = ModpackExporter.TYPE_HMCL;

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view != this.next) {
            return;
        }
        String name = this.nameEdit.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this.context, this.context.getString(R.string.qcl_export_pack_name_empty), Toast.LENGTH_SHORT).show();
            return;
        }
        String version = this.versionEdit.getText().toString().trim();
        String author = this.authorEdit.getText().toString().trim();
        String fileName = this.filenameEdit.getText().toString().trim();
        if (fileName.isEmpty()) {
            fileName = name;
        }
        // 把信息交给「选择文件」页面后进入下一步
        this.activity.uiManager.exportPackageFileUI.setPackInfo(this.packType, name, version, author, fileName);
        this.activity.uiManager.switchMainUI(this.activity.uiManager.exportPackageFileUI);
    }

    public ExportPackageInfoUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    /** 由 {@link ExportPackageTypeUI} 调用 */
    public void setPackType(int packType) {
        this.packType = packType;
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.exportPackageInfoUI = (LinearLayout) this.activity.findViewById(R.id.ui_export_package_info);
        this.nameEdit = (EditText) this.activity.findViewById(R.id.export_package_name_edit);
        this.versionEdit = (EditText) this.activity.findViewById(R.id.export_package_version_edit);
        this.authorEdit = (EditText) this.activity.findViewById(R.id.export_package_author_edit);
        this.filenameEdit = (EditText) this.activity.findViewById(R.id.export_package_filename_edit);
        this.next = (Button) this.activity.findViewById(R.id.export_package_info_next);
        this.next.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.export_package_info_ui_title), false, true);
        CustomAnimationUtils.showViewFromLeft(this.exportPackageInfoUI, this.activity, this.context, true);
        prefill();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.exportPackageInfoUI, this.activity, this.context, true);
    }

    /** 用当前版本名预填默认值（用户已改过则不动） */
    private void prefill() {
        String gameVersion = currentGameVersionName();
        String defaultName = gameVersion + "-" + this.context.getString(R.string.qcl_export_pack_default_suffix);
        if (this.nameEdit.getText().toString().trim().isEmpty()) {
            this.nameEdit.setText(defaultName);
        }
        if (this.versionEdit.getText().toString().trim().isEmpty()) {
            this.versionEdit.setText("1.0");
        }
        if (this.filenameEdit.getText().toString().trim().isEmpty()) {
            this.filenameEdit.setText(defaultName);
        }
    }

    private String currentGameVersionName() {
        try {
            String cur = this.activity.publicGameSetting.currentVersion;
            if (cur != null && !cur.isEmpty()) {
                return new File(cur).getName();
            }
        } catch (Throwable ignored) {
        }
        return "modpack";
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
    }
}