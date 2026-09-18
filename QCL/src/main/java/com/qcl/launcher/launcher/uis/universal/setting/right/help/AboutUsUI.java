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
public class AboutUsUI extends BaseUI implements View.OnClickListener {
    public LinearLayout aboutUsUI;
    private ImageButton author;
    private ImageButton downloadMirror;
    private ImageButton fclLink;
    private ImageButton hmclpeLink;
    private ImageButton mcmod;
    private ImageButton upstream;

    public AboutUsUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.aboutUsUI = (LinearLayout) this.activity.findViewById(R.id.ui_about);
        this.downloadMirror = (ImageButton) this.activity.findViewById(R.id.download_mirror_link);
        this.mcmod = (ImageButton) this.activity.findViewById(R.id.mcmod_link);
        this.upstream = (ImageButton) this.activity.findViewById(R.id.upstream_link);
        this.author = (ImageButton) this.activity.findViewById(R.id.author_link);
        // ★ 1.1.2：补上 FCL / HMCL-PE（已归档）两个仓库入口，此前字段未绑定是死代码
        this.fclLink = (ImageButton) this.activity.findViewById(R.id.fcl_link);
        this.hmclpeLink = (ImageButton) this.activity.findViewById(R.id.hmclpe_link);
        this.downloadMirror.setOnClickListener(this);
        this.mcmod.setOnClickListener(this);
        this.upstream.setOnClickListener(this);
        this.author.setOnClickListener(this);
        this.fclLink.setOnClickListener(this);
        this.hmclpeLink.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(this.aboutUsUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.settingUI.startAboutUsUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.aboutUsUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.settingUI.startAboutUsUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        Uri parse = view == this.downloadMirror ? Uri.parse("https://bmclapidoc.bangbang93.com/") : null;
        if (view == this.mcmod) {
            parse = Uri.parse("https://www.mcmod.cn/");
        }
        if (view == this.upstream) {
            parse = Uri.parse("https://space.bilibili.com/18115101");
        }
        if (view == this.author) {
            parse = Uri.parse("https://space.bilibili.com/550905358");
        }
        if (view == this.fclLink) {
            parse = Uri.parse("https://github.com/FCL-Team/FoldCraftLauncher");
        }
        if (view == this.hmclpeLink) {
            parse = Uri.parse("https://github.com/HMCL-dev/HMCL-PE");
        }
        if (parse != null) {
            this.context.startActivity(new Intent("android.intent.action.VIEW", parse));
        }
    }
}
