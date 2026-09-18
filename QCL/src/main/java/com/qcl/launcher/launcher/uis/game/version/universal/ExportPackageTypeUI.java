package com.qcl.launcher.launcher.uis.game.version.universal;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ExportPackageTypeUI extends BaseUI implements View.OnClickListener {
    public LinearLayout exportPackageTypeUI;
    private LinearLayout hmclPackage;
    private LinearLayout multimc;
    private LinearLayout server;

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
    }

    public ExportPackageTypeUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.exportPackageTypeUI = (LinearLayout) this.activity.findViewById(R.id.ui_export_package_type);
        this.hmclPackage = (LinearLayout) this.activity.findViewById(R.id.export_package_hmcl);
        this.multimc = (LinearLayout) this.activity.findViewById(R.id.export_package_multimc);
        this.server = (LinearLayout) this.activity.findViewById(R.id.export_package_server);
        this.hmclPackage.setOnClickListener(this);
        this.multimc.setOnClickListener(this);
        this.server.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.export_package_type_ui_title), false, true);
        CustomAnimationUtils.showViewFromLeft(this.exportPackageTypeUI, this.activity, this.context, true);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.exportPackageTypeUI, this.activity, this.context, true);
    }
}
