package com.tungsten.hmclpe.launcher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * QCL 崩溃界面：不再直接回主界面 / 直接退出。
 *
 * <p>左边是完整崩溃日志（可横向+纵向滚动），右边是错误摘要与处置建议；
 * 右下角两个按钮：<b>退出</b> / <b>返回启动器</b>。
 */
public class CrashReportActivity extends Activity {

    public static final String EXTRA_CRASH_TEXT = "crash_text";
    public static final String EXTRA_SUMMARY = "crash_summary";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String crashText = getIntent() != null ? getIntent().getStringExtra(EXTRA_CRASH_TEXT) : null;
        if (crashText == null) {
            crashText = "(没有捕获到崩溃日志)";
        }
        String summary = getIntent() != null ? getIntent().getStringExtra(EXTRA_SUMMARY) : null;
        if (summary == null) {
            summary = firstLines(crashText);
        }

        int pad = dp(12);

        // 根：水平排列 —— 左日志 / 右摘要+按钮
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(0xFF2B2B2B);
        root.setPadding(pad, pad, pad, pad);

        // ===== 左：崩溃日志 =====
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        ScrollView vScroll = new ScrollView(this);
        TextView logView = new TextView(this);
        logView.setText(crashText);
        logView.setTextColor(0xFFE6E6E6);
        logView.setTextSize(10);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setTextIsSelectable(true);
        vScroll.addView(logView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        hScroll.addView(vScroll, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        GradientDrawable logBg = rounded(0xFF1E1E1E, 10);
        hScroll.setBackground(logBg);
        hScroll.setPadding(pad, pad, pad, pad);
        root.addView(hScroll, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1.6f));

        // ===== 右：摘要 + 按钮 =====
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setPadding(pad, 0, 0, 0);

        TextView title = new TextView(this);
        title.setText("启动器 / 游戏出错了");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(16);
        right.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = new TextView(this);
        hint.setText("下面是错误摘要与崩溃日志。\n可以把左侧日志发给作者反馈；\n也可以直接退出或返回启动器重试。");
        hint.setTextColor(0xFFB8B8B8);
        hint.setTextSize(12);
        hint.setPadding(0, dp(6), 0, dp(8));
        right.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView summaryScroll = new ScrollView(this);
        TextView summaryView = new TextView(this);
        summaryView.setText(summary);
        summaryView.setTextColor(0xFFFFCC80);
        summaryView.setTextSize(11);
        summaryView.setTypeface(Typeface.MONOSPACE);
        summaryView.setTextIsSelectable(true);
        summaryScroll.addView(summaryView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        GradientDrawable sumBg = rounded(0xFF1E1E1E, 10);
        summaryScroll.setBackground(sumBg);
        summaryScroll.setPadding(pad, pad, pad, pad);
        right.addView(summaryScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 右下两个按钮：退出 / 返回启动器
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);
        buttons.setPadding(0, dp(10), 0, 0);

        Button exit = new Button(this);
        exit.setText("退出");
        exit.setBackground(rounded(0xFF8C5E5E, 12));
        exit.setTextColor(Color.WHITE);
        exit.setOnClickListener(v -> {
            finishAffinity();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        });
        buttons.addView(exit, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button back = new Button(this);
        back.setText("返回启动器");
        back.setBackground(rounded(0xFF4E8A4E, 12));
        back.setTextColor(Color.WHITE);
        back.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        backParams.leftMargin = dp(8);
        buttons.addView(back, backParams);

        right.addView(buttons, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(right, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        setContentView(root);
    }

    private static String firstLines(String text) {
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length && i < 25; i++) {
            sb.append(lines[i]).append('\n');
        }
        return sb.toString();
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
