package com.qcl.launcher.launcher.dialogs.account;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qcl.launcher.R;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.microsoft.Msa;
import com.qcl.launcher.auth.microsoft.MsaDeviceCode;
import com.qcl.launcher.auth.yggdrasil.Texture;
import com.qcl.launcher.auth.yggdrasil.TextureType;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.skin.utils.Avatar;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * ★ 2026-10-06：微软账号**设备码登录**对话框。
 *
 * <p>用法：弹出后自动向微软申请设备码，把 8 位码大字显示出来，
 * 并提示玩家用浏览器打开网址、输入这个码。启动器在后台每 5 秒轮询一次，
 * 玩家在手机上确认后立刻完成登录并回调。
 *
 * <p>为什么比 WebView 稳：不依赖 WebView 内核、不受微软对嵌入浏览器的限制，
 * 而且玩家可以在**另一台设备**上确认（比如在电脑浏览器里输码）。
 */
public class DeviceCodeLoginDialog extends AlertDialog {

    private final MainActivity activity;
    private final AddMicrosoftAccountDialog.OnMicrosoftAccountAddListener listener;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private TextView codeView;
    private TextView statusView;
    /** ★ 内嵌授权页（用户要求：不准跳出浏览器，就在启动器里登）。 */
    private LinearLayout rootLayout;
    private LinearLayout webHolder;
    private WebView webView;

    /** 用户关掉对话框后置 true，轮询线程据此退出（避免泄漏线程）。 */
    private volatile boolean cancelled = false;
    private String userCode;
    private String verifyUri;

    public DeviceCodeLoginDialog(MainActivity activity,
                                 AddMicrosoftAccountDialog.OnMicrosoftAccountAddListener listener) {
        super(activity);
        this.activity = activity;
        this.listener = listener;
        buildUi();
        startFlow();
    }

    /** 纯代码搭界面，避免动布局文件。 */
    private void buildUi() {
        Context ctx = getContext();
        int pad = dp(20);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        this.rootLayout = root;

        TextView title = new TextView(ctx);
        title.setText("微软账号登录（设备码）");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView tip = new TextView(ctx);
        tip.setText("1. 在下面的页面里输入上面的代码并确认\n"
                + "2. 确认后这里会自动完成登录\n"
                + "（不用跳出去，就在启动器里完成）");
        tip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tip.setPadding(0, dp(10), 0, dp(10));
        root.addView(tip);

        codeView = new TextView(ctx);
        codeView.setText("正在获取设备码…");
        codeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        codeView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        codeView.setGravity(Gravity.CENTER);
        codeView.setPadding(0, dp(12), 0, dp(12));
        root.addView(codeView);

        // ★ 2026-10-06：玩家要求去掉"复制代码"按钮 ——
        //   代码在拿到的一瞬间就**自动复制**到剪贴板了，页面上也大字显示着，
        //   再放个按钮纯属多余。所以这里不再往界面加任何按钮。
        root.addView(new View(ctx), new LinearLayout.LayoutParams(0, dp(4)));

        // ★★★ 内嵌授权页容器。
        //   ⚠️ 踩过的坑：这里一开始用 weight=1 + height=0，结果在 Dialog 里
        //   **高度塌成 0，玩家完全看不到页面**（被投诉"内置页面打不开"）。
        //   Dialog 的根布局高度是 wrap_content，weight 分不到空间。
        //   现在改成**固定高度**，稳定可见；拿到设备码后再把高度放大。
        webHolder = new LinearLayout(ctx);
        webHolder.setOrientation(LinearLayout.VERTICAL);
        root.addView(webHolder, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0));

        // ★★★ 2026-10-06：**这里原来有一个一直转的圆圈，已删除**。
        //   用户反馈"微软登录下面为什么有一个圆圈一直转"、"还是没有被移除"。
        //   那个圈是这个对话框里 new ProgressBar(ctx) + setIndeterminate(true) 加进去的，
        //   属于"不确定进度"的转圈动画。现在**物理删除**，不再往界面加任何转圈控件，
        //   进度只用下面的文字状态表达（"正在连接联机服务…"之类），更干净也不再晃眼。
        statusView = new TextView(ctx);
        statusView.setText("正在向微软申请设备码…");
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        statusView.setGravity(Gravity.CENTER);
        root.addView(statusView);

        // 底部关闭按钮
        Button closeBtn = new Button(ctx);
        // ★★★ 2026-10-06：改成「返回」并支持系统返回键。
        //   用户反馈：玩家点了登录之后后悔了，想退回去却**退不出来**
        //   （原来按钮写的是"关闭"，而且 setCancelable(false) 让返回键也失效）。
        closeBtn.setText("返回");
        closeBtn.setOnClickListener(v -> {
            cancelled = true;
            dismiss();
        });
        LinearLayout closeRow = new LinearLayout(ctx);
        closeRow.setGravity(Gravity.END);
        closeRow.setPadding(0, dp(10), 0, 0);
        closeRow.addView(closeBtn);
        root.addView(closeRow);

        // ★★★ 2026-10-06 修复：把整块内容套进 ScrollView 再给全屏高度。
        //   用户反馈：内嵌授权页一加载出来，整个界面被"顶到下面去了，根本滑不下去"，
        //   连返回按钮都够不到。
        //   原因：对话框用的是 WRAP_CONTENT 高度，WebView 又设了 55% 屏高，
        //   两者叠加超出屏幕，而根布局不是可滚动的 → 下半截直接被裁掉。
        //   修法：① 根内容换成 ScrollView；② 窗口高度给到屏幕的 92%；
        //        ③ WebView 高度改成"剩余空间"（根布局 weight 分配），不再固定 55%。
        android.widget.ScrollView scroll = new android.widget.ScrollView(ctx);
        scroll.addView(root, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT));
        setView(scroll);
        // ★ 允许返回键取消（onBackPressed 里会置 cancelled 并停掉轮询）
        setCancelable(true);
        setOnCancelListener(d -> cancelled = true);
        // ★ 显式给对话框一个"接近全屏"的尺寸：
        //   宽度 94% 屏宽、高度 92% 屏高，配合 ScrollView 保证任何内容都够得到。
        android.view.Window w = getWindow();
        if (w != null) {
            android.util.DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            w.setLayout((int) (dm.widthPixels * 0.94f), (int) (dm.heightPixels * 0.92f));
        }
    }

    @Override
    public void dismiss() {
        cancelled = true;
        super.dismiss();
    }

    private void startFlow() {
        new Thread(() -> {
            MsaDeviceCode.DeviceCode dc;
            try {
                dc = MsaDeviceCode.requestDeviceCode();
            } catch (Throwable t) {
                final String msg = t.getMessage() == null ? t.toString() : t.getMessage();
                ui.post(() -> {
                    if (cancelled) {
                        return;
                    }
                    statusView.setText("获取设备码失败：" + msg);
                });
                return;
            }
            if (cancelled) {
                return;
            }
            userCode = dc.userCode;
            verifyUri = dc.verificationUri;
            ui.post(() -> {
                if (cancelled) {
                    return;
                }
                codeView.setText(dc.userCode);
                statusView.setText("已自动复制代码，正在打开登录页…");
                // ★★ 照 FCL 的逻辑，两步都自动做，不需要玩家点任何按钮：
                //   ① 自动把设备码复制到剪贴板（玩家在内置页面里长按粘贴即可）
                //   ② 自动在内置 WebView 里打开微软的输码页面
                try {
                    ClipboardManager cm = (ClipboardManager) getContext()
                            .getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("device code", dc.userCode));
                    }
                } catch (Throwable ignored) {
                    // 剪贴板失败不影响流程，码就显示在上面，玩家照样能看
                }
                showWebView();
                statusView.setText("已自动复制代码 " + dc.userCode + "，请在内置页面里粘贴并确认");
            });

            // 轮询直到成功 / 超时 / 用户关闭
            long deadline = System.currentTimeMillis() + dc.expiresInMs;
            long interval = dc.intervalMs;
            while (!cancelled && System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException ie) {
                    return;
                }
                if (cancelled) {
                    return;
                }
                try {
                    MsaDeviceCode.PollOutcome out = MsaDeviceCode.pollOnce(dc.deviceCode);
                    if (out.result == MsaDeviceCode.PollResult.SLOW_DOWN) {
                        interval += 5000L;
                        continue;
                    }
                    if (out.result == MsaDeviceCode.PollResult.PENDING) {
                        continue;
                    }
                    // SUCCESS：拿到 refresh_token，交给既有 Msa 走 XBL → XSTS → Minecraft
                    finishLogin(out.refreshToken);
                    return;
                } catch (Throwable t) {
                    final String msg = t.getMessage() == null ? t.toString() : t.getMessage();
                    ui.post(() -> {
                        if (!cancelled) {
                            statusView.setText("登录失败：" + msg);
                        }
                    });
                    return;
                }
            }
            ui.post(() -> {
                if (!cancelled) {
                    statusView.setText("设备码已过期，请关闭后重试。");
                }
            });
        }, "QCL-devicecode").start();
    }

    /** 用 refresh_token 完成后续登录（与 WebView 流程共用同一段终点逻辑）。 */
    private void finishLogin(final String refreshToken) {
        ui.post(() -> {
            if (!cancelled) {
                statusView.setText("已确认，正在获取游戏档案…");
            }
        });
        try {
            // Msa(isRefresh=true, refreshToken) 会用 refresh_token 换 access_token
            final Msa msa = new Msa(true, refreshToken);
            if (!msa.doesOwnGame) {
                ui.post(() -> {
                    if (!cancelled) {
                        statusView.setText("这个微软账号没有购买 Minecraft Java 版。");
                    }
                });
                return;
            }
            Msa.MinecraftProfileResponse profile = Msa.getMinecraftProfile(msa.tokenType, msa.mcToken);
            Bitmap skin;
            Map<TextureType, Texture> textures = Msa.getTextures(profile).orElse(null);
            Texture texture = textures == null ? null : textures.get(TextureType.SKIN);
            if (texture == null) {
                AssetManager manager = getContext().getAssets();
                InputStream is = manager.open("img/alex.png");
                skin = BitmapFactory.decodeStream(is);
                is.close();
            } else {
                String u = texture.getUrl();
                if (!u.startsWith("https")) {
                    u = u.replaceFirst("http", "https");
                }
                HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
                conn.setDoInput(true);
                conn.connect();
                InputStream is = conn.getInputStream();
                skin = BitmapFactory.decodeStream(is);
                is.close();
            }
            final String skinTexture = Avatar.bitmapToString(skin);
            ui.post(() -> {
                if (cancelled) {
                    return;
                }
                Account account = new Account(3, "", "", "mojang", "0",
                        msa.mcName, msa.mcUuid, msa.mcToken,
                        "00000000-0000-0000-0000-000000000000",
                        msa.msRefreshToken, "", skinTexture);
                listener.onPositive(account);
                dismiss();
            });
        } catch (Throwable t) {
            final String msg = t.getMessage() == null ? t.toString() : t.getMessage();
            ui.post(() -> {
                if (!cancelled) {
                    statusView.setText("登录失败：" + msg);
                }
            });
        }
    }

    /**
     * ★★★ 在**启动器内**打开微软的输码页面（用户明确要求：不准跳出到浏览器）。
     *
     * <p>做法：在对话框里挂一个 WebView 加载 {@code verification_uri}
     * （老通道是 {@code https://www.microsoft.com/link}），玩家就在这里面输码、确认。
     * 与此同时后台轮询照常跑，玩家确认后自动完成登录并关掉对话框 ——
     * 不需要任何外部浏览器。
     */
    private void showWebView() {
        if (verifyUri == null && userCode == null) {
            toast("设备码还没拿到，请稍候");
            return;
        }
        if (webView == null) {
            Context ctx = getContext();
            webView = new WebView(ctx);
            WebSettings s = webView.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    // 一律留在本 WebView 内，绝不外跳
                    view.loadUrl(url);
                    return true;
                }
            });
            webHolder.addView(webView, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        }
        // ★ 给 WebView 一个**明确高度**。
        //   ⚠️ 两个坑都踩过：
        //     ① 用 weight=1 + height=0 → Dialog 里塌成 0，页面完全看不见；
        //     ② 用 55% 屏高 + 根布局不可滚动 → 超出屏幕，下半截被裁掉、滑不下去。
        //   现在：根布局已套 ScrollView（可滚动），这里给一个不超过半屏的固定高度，
        //   页面本身可上下滑，整个对话框也能滑，返回按钮永远够得到。
        int h = (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.45f);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) webHolder.getLayoutParams();
        lp.height = h;
        webHolder.setLayoutParams(lp);
        // ★★★ 用**预填了验证码**的地址打开（OCL 实测写法）：
        //   login.live.com/oauth20_remoteconnect.srf?otc=<码>
        //   页面里的码已经填好，玩家直接点继续即可 —— 不用手输、不用粘贴。
        String url = MsaDeviceCode.buildPrefilledUrl(userCode);
        webView.loadUrl(url);
        statusView.setText("验证码已预填，请在内置页面里点「继续」确认");
    }

    private void toast(String s) {
        Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return (int) (v * getContext().getResources().getDisplayMetrics().density + 0.5f);
    }
}
