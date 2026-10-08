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
    private ImageButton jumpToGit;

    public FeedbackUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.feedbackUI = (LinearLayout) this.activity.findViewById(R.id.ui_feedback);
        // ★★★ 1.5.0：移除 Discord 按钮（HMCL PE 遗留，QCL 没有自己的 Discord）。
        //   ★ 布局里那个 ImageButton 已一并删掉，**这里绝不能再 findViewById** ——
        //     否则返回 null，再调 setOnClickListener 直接 NPE（删控件必须四处同步）。
        this.jumpToGit = (ImageButton) this.activity.findViewById(R.id.jump_to_git_issues);
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
        // ★ 1.5.0：Discord 分支已移除（HMCL PE 遗留）；反馈只剩 GitHub Issues 一条。
        if (view == this.jumpToGit) {
            this.context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://github.com/ALLEN201123/Quanta-Craft-Launcher/issues")));
        }
    }
}
