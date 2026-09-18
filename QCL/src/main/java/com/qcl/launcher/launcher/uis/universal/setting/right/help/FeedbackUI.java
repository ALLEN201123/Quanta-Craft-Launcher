package com.qcl.launcher.launcher.uis.universal.setting.right.help;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class FeedbackUI extends BaseUI implements View.OnClickListener {
    public LinearLayout feedbackUI;
    private ImageButton joinDiscord;
    private ImageButton jumpToGit;

    public FeedbackUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.feedbackUI = (LinearLayout) this.activity.findViewById(R.id.ui_feedback);
        this.joinDiscord = (ImageButton) this.activity.findViewById(R.id.join_discord);
        this.jumpToGit = (ImageButton) this.activity.findViewById(R.id.jump_to_git_issues);
        this.joinDiscord.setOnClickListener(this);
        this.jumpToGit.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(this.feedbackUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.settingUI.startFeedbackUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.feedbackUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.settingUI.startFeedbackUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.joinDiscord) {
            this.context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://discord.gg/zeMNy8Wdgd")));
        }
        if (view == this.jumpToGit) {
            this.context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://github.com/ALLEN201123/Quanta-Craft-Launcher/issues")));
        }
    }
}
