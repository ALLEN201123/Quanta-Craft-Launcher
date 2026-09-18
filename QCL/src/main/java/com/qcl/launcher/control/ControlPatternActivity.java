package com.qcl.launcher.control;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.qcl.launcher.control.view.LayoutPanel;
import com.qcl.launcher.utils.LocaleUtils;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ControlPatternActivity extends AppCompatActivity implements View.OnTouchListener {
    public static final int CONTROL_PATTERN_REQUEST_CODE = 3000;
    public static final int CONTROL_PATTERN_REQUEST_CODE_ISOLATE = 7700;
    private LayoutPanel baseLayout;
    private FrameLayout drawerLayout;
    public MenuHelper menuHelper;

    @Override // android.view.View.OnTouchListener
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (Build.VERSION.SDK_INT >= 28) {
            if (getIntent().getExtras().getBoolean("fullscreen")) {
                getWindow().getAttributes().layoutInDisplayCutoutMode = 1;
            } else {
                getWindow().getAttributes().layoutInDisplayCutoutMode = 2;
            }
        }
        getWindow().setFlags(256, 256);
        setContentView(R.layout.activity_control_pattern);
        this.drawerLayout = (FrameLayout) findViewById(R.id.drawer_layout);
        this.baseLayout = (LayoutPanel) findViewById(R.id.base_layout);
        MenuHelper menuHelper = new MenuHelper(this, this, getIntent().getExtras().getBoolean("fullscreen"), null, this.drawerLayout, this.baseLayout, true, getIntent().getExtras().getString("pattern"), 0, 1.0f);
        this.menuHelper = menuHelper;
        menuHelper.initialPattern = getIntent().getExtras().getString("initial");
    }

    @Override // androidx.activity.ComponentActivity, android.app.Activity
    public void onBackPressed() {
        String str;
        if (this.menuHelper.initialPattern == null) {
            str = getIntent().getExtras().getString("initial");
        } else {
            str = this.menuHelper.initialPattern;
        }
        Intent intent = new Intent();
        intent.setData(Uri.parse(str));
        setResult(-1, intent);
        super.onBackPressed();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.appcompat.app.AppCompatActivity, android.app.Activity, android.view.ContextThemeWrapper, android.content.ContextWrapper
    public void attachBaseContext(Context context) {
        super.attachBaseContext(LocaleUtils.setLanguage(context));
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        LocaleUtils.setLanguage(this);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onPostResume() {
        super.onPostResume();
        if (Build.VERSION.SDK_INT >= 28) {
            if (getIntent().getExtras().getBoolean("fullscreen")) {
                getWindow().getAttributes().layoutInDisplayCutoutMode = 1;
            } else {
                getWindow().getAttributes().layoutInDisplayCutoutMode = 2;
            }
        }
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (z) {
            getWindow().getDecorView().setSystemUiVisibility(5894);
        }
    }
}
