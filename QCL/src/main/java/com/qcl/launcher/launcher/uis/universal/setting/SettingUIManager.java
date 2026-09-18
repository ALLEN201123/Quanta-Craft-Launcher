package com.qcl.launcher.launcher.uis.universal.setting;

import android.content.Context;
import android.content.Intent;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.launcher.uis.universal.setting.right.UniversalGameSettingUI;
import com.qcl.launcher.launcher.uis.universal.setting.right.help.AboutUsUI;
import com.qcl.launcher.launcher.uis.universal.setting.right.help.DonateUI;
import com.qcl.launcher.launcher.uis.universal.setting.right.help.FeedbackUI;
import com.qcl.launcher.launcher.uis.universal.setting.right.help.HelpUI;
import com.qcl.launcher.launcher.uis.universal.setting.right.launcher.DownloadSettingUI;
import com.qcl.launcher.launcher.uis.universal.setting.right.launcher.ExteriorSettingUI;
import com.qcl.launcher.launcher.uis.universal.setting.right.launcher.UniversalSettingUI;

/* loaded from: classes2.dex */
public class SettingUIManager {
    public AboutUsUI aboutUsUI;
    public DonateUI donateUI;
    public DownloadSettingUI downloadSettingUI;
    public ExteriorSettingUI exteriorSettingUI;
    public FeedbackUI feedbackUI;
    public HelpUI helpUI;
    public BaseUI[] settingUIs;
    public UniversalGameSettingUI universalGameSettingUI;
    public UniversalSettingUI universalSettingUI;

    public SettingUIManager(Context context, MainActivity mainActivity) {
        this.universalGameSettingUI = new UniversalGameSettingUI(context, mainActivity);
        this.downloadSettingUI = new DownloadSettingUI(context, mainActivity);
        this.exteriorSettingUI = new ExteriorSettingUI(context, mainActivity);
        this.universalSettingUI = new UniversalSettingUI(context, mainActivity);
        this.helpUI = new HelpUI(context, mainActivity);
        this.feedbackUI = new FeedbackUI(context, mainActivity);
        this.donateUI = new DonateUI(context, mainActivity);
        this.aboutUsUI = new AboutUsUI(context, mainActivity);
        this.universalGameSettingUI.onCreate();
        this.downloadSettingUI.onCreate();
        this.exteriorSettingUI.onCreate();
        this.universalSettingUI.onCreate();
        this.helpUI.onCreate();
        this.feedbackUI.onCreate();
        this.donateUI.onCreate();
        this.aboutUsUI.onCreate();
        UniversalGameSettingUI universalGameSettingUI = this.universalGameSettingUI;
        this.settingUIs = new BaseUI[]{universalGameSettingUI, this.universalSettingUI, this.downloadSettingUI, this.exteriorSettingUI, this.helpUI, this.feedbackUI, this.donateUI, this.aboutUsUI};
        switchSettingUIs(universalGameSettingUI);
    }

    public void switchSettingUIs(BaseUI baseUI) {
        int i = 0;
        while (true) {
            BaseUI[] baseUIArr = this.settingUIs;
            if (i >= baseUIArr.length) {
                return;
            }
            if (baseUIArr[i] == baseUI) {
                baseUIArr[i].onStart();
            } else {
                baseUIArr[i].onStop();
            }
            i++;
        }
    }

    public void onActivityResult(int i, int i2, Intent intent) {
        for (BaseUI baseUI : this.settingUIs) {
            baseUI.onActivityResult(i, i2, intent);
        }
    }

    public void onPause() {
        for (BaseUI baseUI : this.settingUIs) {
            baseUI.onPause();
        }
    }

    public void onResume() {
        for (BaseUI baseUI : this.settingUIs) {
            baseUI.onResume();
        }
    }
}
