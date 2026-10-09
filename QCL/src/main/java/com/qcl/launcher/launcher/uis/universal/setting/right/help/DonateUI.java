package com.qcl.launcher.launcher.uis.universal.setting.right.help;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DonateUI extends BaseUI implements View.OnClickListener {
    public LinearLayout donateUI;
    private TextView textView;

    public DonateUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.donateUI = (LinearLayout) this.activity.findViewById(R.id.ui_donate);
        TextView textView = (TextView) this.activity.findViewById(R.id.donate);
        this.textView = textView;
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(this.donateUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.settingUI.startDonateUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.donateUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.settingUI.startDonateUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.textView) {
            // ★★★ 2026-10-06（用户要求）：跳赞助页前先弹「未成年人禁止捐款」确认。
            //   原来直接 startActivity 跳转，未成年的孩子可能顺手就扫码转了。
            //   现在先弹框说明，点「我已成年」才跳；点「取消」就不跳。
            new android.app.AlertDialog.Builder(this.context)
                    .setTitle("未成年人禁止捐款")
                    .setMessage("本项目不接受未成年人以任何形式捐款／打赏。\n\n"
                            + "如果你未满 18 周岁（或未达到你所在地区的法定成年年龄），"
                            + "请点「取消」关闭，不要扫码、不要转账。\n\n"
                            + "QCL 完全免费，不赞助也能用全部功能。")
                    .setPositiveButton("我已成年，继续", (d, w) -> {
                        this.context.startActivity(new Intent("android.intent.action.VIEW",
                                Uri.parse(com.qcl.launcher.launcher.dialogs.LaunchCountDialog.SPONSOR_URL)));
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }
}
