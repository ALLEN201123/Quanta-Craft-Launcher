package com.qcl.launcher.launcher.terracotta;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * ★★★ 2026-10-06：**联机进度弹窗**（房主 / 访客各自独立）。
 *
 * <p><b>修的问题</b>（用户实测反馈）：在游戏内悬浮窗的「联机模块」里选
 * 「我要当房主」/「我要当访客」之后**没有任何独立弹窗**，只有一个 Toast，
 * 玩家完全不知道现在进行到哪一步、也没有进度条；而且检测完了也不自动弹邀请码。
 *
 * <p><b>本类做的事</b>（与 FCL 的逻辑对齐）：
 * <ol>
 *   <li>弹出**独立**的进度对话框（标题区分房主/访客），带**进度条**与状态文字；</li>
 *   <li>每 500ms 轮询 {@link TerracottaHelper} 的状态，把后端状态映射成中文提示，
 *       进度条按阶段推进（0→30→60→90→100），玩家能看出"正在扫描房间/正在连接"；</li>
 *   <li>拿到邀请码（房主）或服务器地址（访客）后**自动关掉进度框**，
 *       弹结果框并**自动复制到剪贴板**（不用玩家再点"复制代码"）；</li>
 *   <li>超过 90 秒仍未就绪则提示失败原因（含后端状态），并给出 VPN 授权提示。</li>
 * </ol>
 *
 * <p>状态名来自 Terracotta 原生层（与 FCL 的 {@code TerracottaState} 同名）：
 * {@code waiting / host_scanning / host_starting / host_ok /
 * guest_connecting / guest_starting / guest_ok / exception}。
 */
final class TerracottaProgressDialog {

    /** 轮询间隔（与 FCL 的 500ms 一致）。 */
    private static final long POLL_MS = 500L;
    /** 最长等待时间：90 秒。 */
    private static final long TIMEOUT_MS = 90000L;

    private final Activity activity;
    private final Context context;
    private final boolean host;
    private final AlertDialog dialog;
    private final ProgressBar bar;
    private final TextView statusText;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final long startAt = System.currentTimeMillis();
    private boolean finished = false;

    private TerracottaProgressDialog(Activity activity, Context context, boolean host) {
        this.activity = activity;
        this.context = context;
        this.host = host;

        int pad = dp(20);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(context);
        title.setText(host ? "正在创建房间（我是房主）" : "正在加入房间（我是访客）");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView tip = new TextView(context);
        tip.setText(host
                ? "正在让伙伴能找到你。\n就绪后会自动把邀请码复制下来并发给你。"
                : "正在连接房主。\n就绪后会自动把服务器地址复制下来。");
        tip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tip.setPadding(0, dp(8), 0, dp(12));
        root.addView(tip);

        bar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(5);
        root.addView(bar);

        statusText = new TextView(context);
        statusText.setText("准备中…");
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, dp(10), 0, 0);
        root.addView(statusText);

        Button cancel = new Button(context);
        cancel.setText("取消");
        cancel.setOnClickListener(v -> {
            finished = true;
            safeDismiss();
        });
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.END);
        row.setPadding(0, dp(8), 0, 0);
        row.addView(cancel);
        root.addView(row);

        dialog = new AlertDialog.Builder(context).setView(root).setCancelable(false).create();
    }

    /** 显示进度框并开始轮询。 */
    static void show(final Activity activity, final Context context, final boolean host) {
        final TerracottaProgressDialog d = new TerracottaProgressDialog(activity, context, host);
        d.dialog.show();
        d.tick();
    }

    private void tick() {
        ui.postDelayed(() -> {
            if (finished) {
                return;
            }
            String value = host ? TerracottaHelper.getInviteCode() : TerracottaHelper.getServerAddress();
            if (value != null && !value.isEmpty()) {
                finished = true;
                safeDismiss();
                showResult(value);
                return;
            }
            String st = TerracottaHelper.getState();
            applyState(st);
            if (System.currentTimeMillis() - startAt > TIMEOUT_MS) {
                finished = true;
                safeDismiss();
                new AlertDialog.Builder(context)
                        .setTitle(host ? "创建房间超时" : "加入房间超时")
                        .setMessage("联机后端一直没有就绪。\n\n当前状态："
                                + (st == null || st.isEmpty() ? "无（可能 VPN 未授权）" : st)
                                + "\n\n请确认：\n1. 弹出 VPN 授权时点了「确定」\n"
                                + "2. 网络可用\n然后重试。")
                        .setPositiveButton("知道了", (d, i) -> {
                        })
                        .create().show();
                return;
            }
            tick();
        }, POLL_MS);
    }

    /**
     * 把后端状态映射成中文 + 推进进度条（与 FCL 的状态语义一一对应）。
     */
    private void applyState(String st) {
        String s = st == null ? "" : st;
        int progress;
        String text;
        if (s.contains("host_scanning")) {
            progress = 30;
            text = "正在扫描局域网，准备房间…";
        } else if (s.contains("host_starting")) {
            progress = 60;
            text = "正在启动房间服务…";
        } else if (s.contains("guest_connecting")) {
            progress = 45;
            text = "正在连接房主…";
        } else if (s.contains("guest_starting")) {
            progress = 75;
            text = "正在建立通道…";
        } else if (s.contains("host_ok") || s.contains("guest_ok")) {
            progress = 95;
            text = "即将就绪…";
        } else if (s.contains("exception")) {
            progress = 100;
            String type = TerracottaHelper.getExceptionType();
            text = "出错了：" + (type == null || type.isEmpty() ? "未知错误" : type);
        } else if (s.contains("waiting")) {
            progress = 15;
            text = host ? "等待房间服务响应…" : "等待房主响应…";
        } else {
            progress = 10;
            text = "正在连接联机服务…";
        }
        bar.setProgress(progress);
        statusText.setText(text);
    }

    /** 结果框：自动复制 + 明确告诉玩家怎么用。 */
    private void showResult(String value) {
        copy(value);
        String title = host ? "房间已创建 · 邀请码已复制" : "已加入房间 · 地址已复制";
        String body = host
                ? "邀请码：\n" + value + "\n\n已自动复制到剪贴板，直接发给伙伴即可。\n"
                  + "别忘了先在游戏里把存档「对局域网开放」。"
                : "服务器地址：\n" + value + "\n\n已自动复制。\n"
                  + "在游戏的「多人游戏 → 直接连接」里粘贴这个地址。";
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(body)
                .setPositiveButton("再复制一次", (d, i) -> {
                    copy(value);
                    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("关闭", (d, i) -> {
                })
                .create().show();
    }

    private void safeDismiss() {
        try {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Throwable ignored) {
        }
    }

    private void copy(String value) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("qcl_multiplayer", value));
            }
        } catch (Throwable ignored) {
        }
    }

    private int dp(int v) {
        return (int) (v * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
