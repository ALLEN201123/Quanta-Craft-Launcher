package com.qcl.launcher.launcher.uis.tools;

import android.content.Intent;

/* loaded from: classes2.dex */
public interface UILifecycleCallbacks {
    void onActivityResult(int i, int i2, Intent intent);

    void onCreate();

    void onDestroy();

    void onLoaded();

    void onNewIntent();

    void onPause();

    void onRestart();

    void onResume();

    void onStart();

    void onStop();
}
