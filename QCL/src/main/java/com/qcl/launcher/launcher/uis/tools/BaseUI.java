package com.qcl.launcher.launcher.uis.tools;

import android.content.Context;
import android.content.Intent;
import com.qcl.launcher.launcher.MainActivity;

/* loaded from: classes2.dex */
public class BaseUI implements UILifecycleCallbacks {
    public MainActivity activity;
    public Context context;

    @Override // com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onActivityResult(int i, int i2, Intent intent) {
    }

    @Override // com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
    }

    @Override // com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onDestroy() {
    }

    @Override // com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onLoaded() {
    }

    @Override // com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onNewIntent() {
    }

    @Override // com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onPause() {
    }

    @Override // com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onRestart() {
    }

    @Override // com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onResume() {
    }

    @Override // com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
    }

    @Override // com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
    }

    public BaseUI(Context context, MainActivity mainActivity) {
        this.context = context;
        this.activity = mainActivity;
    }

    public boolean canGoBackToLast() {
        int size;
        try {
            MainActivity mainActivity = this.activity;
            if (mainActivity == null || mainActivity.uiManager == null || this.activity.uiManager.uis == null || (size = this.activity.uiManager.uis.size()) < 2) {
                return false;
            }
            return this.activity.uiManager.uis.get(size - 2) != this.activity.uiManager.mainUI;
        } catch (Throwable unused) {
            return false;
        }
    }
}
