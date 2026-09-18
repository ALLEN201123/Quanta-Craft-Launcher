package com.qcl.launcher.launcher.uis.game.version.universal;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ExportPackageFileUI extends BaseUI implements View.OnClickListener {
    public LinearLayout exportPackageFileUI;

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
    }

    public ExportPackageFileUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.exportPackageFileUI = (LinearLayout) this.activity.findViewById(R.id.ui_export_package_file);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.export_package_file_ui_title), false, true);
        CustomAnimationUtils.showViewFromLeft(this.exportPackageFileUI, this.activity, this.context, true);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.exportPackageFileUI, this.activity, this.context, true);
    }
}
