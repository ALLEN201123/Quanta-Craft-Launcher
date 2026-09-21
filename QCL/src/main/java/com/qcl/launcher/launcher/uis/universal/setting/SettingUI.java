package com.qcl.launcher.launcher.uis.universal.setting;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class SettingUI extends BaseUI implements View.OnClickListener {
    public LinearLayout settingUI;
    public SettingUIManager settingUIManager;
    public LinearLayout startAboutUsUI;
    public LinearLayout startDonateUI;
    public LinearLayout startDownloadSettingUI;
    public LinearLayout startExteriorSettingUI;
    public LinearLayout startFeedbackUI;
    public LinearLayout startGlobalGameSettingUI;

    public SettingUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.settingUI = (LinearLayout) this.activity.findViewById(R.id.ui_setting);
        this.startGlobalGameSettingUI = (LinearLayout) this.activity.findViewById(R.id.start_global_game_setting_ui);
        this.startExteriorSettingUI = (LinearLayout) this.activity.findViewById(R.id.start_exterior_setting_ui);
        this.startDownloadSettingUI = (LinearLayout) this.activity.findViewById(R.id.start_download_setting_ui);
        this.startFeedbackUI = (LinearLayout) this.activity.findViewById(R.id.start_feedback_ui);
        this.startDonateUI = (LinearLayout) this.activity.findViewById(R.id.start_donate_ui);
        this.startAboutUsUI = (LinearLayout) this.activity.findViewById(R.id.start_about_ui);
        this.startGlobalGameSettingUI.setOnClickListener(this);
        this.startExteriorSettingUI.setOnClickListener(this);
        this.startDownloadSettingUI.setOnClickListener(this);
        this.startFeedbackUI.setOnClickListener(this);
        this.startDonateUI.setOnClickListener(this);
        this.startAboutUsUI.setOnClickListener(this);
        this.settingUIManager = new SettingUIManager(this.context, this.activity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.setting_ui_title), canGoBackToLast(), true);
        CustomAnimationUtils.showViewFromLeft(this.settingUI, this.activity, this.context, true);
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.settingUI, this.activity, this.context, true);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        this.settingUIManager.onActivityResult(i, i2, intent);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onPause() {
        super.onPause();
        this.settingUIManager.onPause();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onResume() {
        super.onResume();
        this.settingUIManager.onResume();
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.startGlobalGameSettingUI) {
            SettingUIManager settingUIManager = this.settingUIManager;
            settingUIManager.switchSettingUIs(settingUIManager.universalGameSettingUI);
        }
        if (view == this.startExteriorSettingUI) {
            SettingUIManager settingUIManager3 = this.settingUIManager;
            settingUIManager3.switchSettingUIs(settingUIManager3.exteriorSettingUI);
        }
        if (view == this.startDownloadSettingUI) {
            SettingUIManager settingUIManager4 = this.settingUIManager;
            settingUIManager4.switchSettingUIs(settingUIManager4.downloadSettingUI);
        }
        if (view == this.startFeedbackUI) {
            SettingUIManager settingUIManager6 = this.settingUIManager;
            settingUIManager6.switchSettingUIs(settingUIManager6.feedbackUI);
        }
        if (view == this.startDonateUI) {
            SettingUIManager settingUIManager7 = this.settingUIManager;
            settingUIManager7.switchSettingUIs(settingUIManager7.donateUI);
        }
        if (view == this.startAboutUsUI) {
            SettingUIManager settingUIManager8 = this.settingUIManager;
            settingUIManager8.switchSettingUIs(settingUIManager8.aboutUsUI);
        }
    }

    private void init() {
        this.settingUIManager.universalGameSettingUI.refresh();
    }
}
