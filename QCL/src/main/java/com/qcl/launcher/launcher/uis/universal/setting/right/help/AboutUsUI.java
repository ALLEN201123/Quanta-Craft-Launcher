package com.qcl.launcher.launcher.uis.universal.setting.right.help;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

/**
 * About page, reduced to the two external services the launcher actually depends on:
 * the download mirror and the mod wiki. Everything else (author and contributor rows)
 * was removed on request.
 */
public class AboutUsUI extends BaseUI implements View.OnClickListener {

    public LinearLayout aboutUsUI;

    private ImageButton downloadMirror;
    private ImageButton mcmod;
    private ImageButton upstream;
    private ImageButton author;

    public AboutUsUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        aboutUsUI = activity.findViewById(R.id.ui_about);

        downloadMirror = activity.findViewById(R.id.download_mirror_link);
        mcmod = activity.findViewById(R.id.mcmod_link);
        upstream = activity.findViewById(R.id.upstream_link);
        author = activity.findViewById(R.id.author_link);

        downloadMirror.setOnClickListener(this);
        mcmod.setOnClickListener(this);
        upstream.setOnClickListener(this);
        author.setOnClickListener(this);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(aboutUsUI,activity,context,false);
        if (activity.isLoaded){
            activity.uiManager.settingUI.startAboutUsUI.setBackground(context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(aboutUsUI,activity,context,false);
        if (activity.isLoaded){
            activity.uiManager.settingUI.startAboutUsUI.setBackground(context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    @Override
    public void onClick(View view) {
        Uri uri = null;

        if (view == downloadMirror) {
            uri = Uri.parse("https://bmclapidoc.bangbang93.com/");
        }
        if (view == mcmod) {
            uri = Uri.parse("https://www.mcmod.cn/");
        }
        if (view == upstream) {
            // Original HMCL-PE author
            uri = Uri.parse("https://space.bilibili.com/18115101");
        }
        if (view == author) {
            uri = Uri.parse("https://space.bilibili.com/550905358");
        }

        if (uri != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(intent);
        }
    }
}
