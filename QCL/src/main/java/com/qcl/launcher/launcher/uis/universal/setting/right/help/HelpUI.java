package com.qcl.launcher.launcher.uis.universal.setting.right.help;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.LinearLayout;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

public class HelpUI extends BaseUI {

    public LinearLayout helpUI;

    public HelpUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        helpUI = activity.findViewById(R.id.ui_help);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(helpUI,activity,context,false);
        if (activity.isLoaded){
            activity.uiManager.settingUI.startHelpUI.setBackground(context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(helpUI,activity,context,false);
        if (activity.isLoaded){
            activity.uiManager.settingUI.startHelpUI.setBackground(context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

}
