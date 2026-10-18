package com.qcl.launcher.launcher.uis.jar;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.game.QclJavaManager;

import java.util.List;

/**
 * ★★★★★ 2026-10-17（1.5.2 新增）**JAR 启动器独立页面**。
 *
 * <p>用户反馈（原话）：「没有设置独立页面，玩家哪知道那个按钮是干啥的，就跳转到选择文件里面去了」。
 *
 * <p>⇒ 原来的做法是：点左栏按钮**直接弹文件选择器**，玩家完全不知道发生了什么。
 * 现在改成：点左栏 → 进入**本页面**，页面先讲清楚「这是干什么的」，
 * 并列出本机可用的 Java 运行时，然后玩家再点「选择 JAR 文件」。
 *
 * <p>页面结构：
 * <ol>
 *   <li>标题 + 说明（这是独立功能，不是启动 Minecraft）；</li>
 *   <li>「选择 JAR 文件并启动」主按钮；</li>
 *   <li>本机 Java 列表（读 {@link QclJavaManager#scan()}，让玩家知道会用什么 Java 跑）。</li>
 * </ol>
 */
public class JarLauncherUI extends BaseUI implements View.OnClickListener {

    public LinearLayout jarLauncherUI;
    private Button pickButton;
    private LinearLayout javaList;

    public JarLauncherUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.jarLauncherUI = (LinearLayout) this.activity.findViewById(R.id.ui_jar_launcher);
        this.pickButton = (Button) this.activity.findViewById(R.id.jar_launcher_pick);
        this.javaList = (LinearLayout) this.activity.findViewById(R.id.jar_launcher_java_list);
        if (this.pickButton != null) {
            this.pickButton.setOnClickListener(this);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            this.activity.showBarTitle(
                    this.context.getResources().getString(R.string.jar_launcher_title),
                    true, true);
        } catch (Throwable ignored) {
        }
        if (this.jarLauncherUI != null) {
            CustomAnimationUtils.showViewFromLeft(this.jarLauncherUI, this.activity, this.context, true);
        }
        refreshJavaList();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.jarLauncherUI != null) {
            CustomAnimationUtils.hideViewToLeft(this.jarLauncherUI, this.activity, this.context, true);
        }
    }

    /** 刷新「本机 Java」列表。 */
    private void refreshJavaList() {
        if (this.javaList == null) {
            return;
        }
        this.javaList.removeAllViews();
        List<QclJavaManager.JavaEntry> list;
        try {
            list = QclJavaManager.scan();
        } catch (Throwable t) {
            list = null;
        }
        if (list == null || list.isEmpty()) {
            TextView tv = new TextView(this.context);
            tv.setText(R.string.jar_launcher_no_java);
            tv.setTextSize(12f);
            tv.setPadding(8, 8, 8, 8);
            this.javaList.addView(tv);
            return;
        }
        for (QclJavaManager.JavaEntry e : list) {
            LinearLayout row = new LinearLayout(this.context);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(8, 10, 8, 10);

            TextView name = new TextView(this.context);
            name.setText(e.display());
            name.setTextSize(13f);
            try {
                name.setTextColor(this.context.getResources().getColor(R.color.colorPureBlack));
            } catch (Throwable ignored) {
            }
            row.addView(name);

            TextView path = new TextView(this.context);
            path.setText(e.path);
            path.setTextSize(10f);
            path.setAlpha(0.6f);
            row.addView(path);

            this.javaList.addView(row);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == this.pickButton) {
            // 交给 MainActivity 去开文件选择器（那边统一处理 onActivityResult）
            try {
                if (this.activity instanceof MainActivity) {
                    ((MainActivity) this.activity).startPickJar();
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
