package com.tungsten.hmclpe.launcher.terracotta;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.tungsten.hmclpe.R;

/**
 * 多人联机（Terracotta）弹窗。
 *
 * <p><b>游戏外（启动器主界面）</b>：这里是**开启**陶瓷联机的地方 —— 先看协议与免责声明，同意后开启；
 * 开启后进游戏，悬浮窗里的联机模块才可用。也可以在这里关闭。
 *
 * <p><b>游戏内（悬浮窗 → 联机模块）</b>：**使用**的地方 —— 创建房间 / 加入房间 / 房间信息 / 帮助；
 * 若尚未在游戏外开启，则无法使用，并提示去哪里开启。
 */
public final class MultiplayerDialogHelper {

    private static final String PREF = "qcl_multiplayer";

    private MultiplayerDialogHelper() {
    }

    // ==================== 游戏外：开启 / 关闭 ====================

    /** 主界面入口：开启或关闭陶瓷联机（首次会弹协议与免责声明）。 */
    public static void showEnable(Activity activity, Context context) {
        if (activity == null || context == null) {
            return;
        }
        final SharedPreferences sp = activity.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        boolean agreed = sp.getBoolean("agreed", false);
        boolean enabled = sp.getBoolean("enabled", false);

        if (enabled) {
            new AlertDialog.Builder(context)
                    .setTitle("多人联机 · 已开启")
                    .setMessage("陶瓷联机已开启。\n\n现在可以启动游戏，在游戏内悬浮窗的「联机模块」里创建房间或加入房间。\n\n房主：把邀请码发给伙伴；访客：在游戏里添加房主给的服务器地址。")
                    .setPositiveButton("关闭多人联机", (d, i) -> {
                        sp.edit().putBoolean("enabled", false).apply();
                        TerracottaHelper.reset(activity);
                        Toast.makeText(context, "已关闭多人联机", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("保持开启", (d, i) -> {
                    })
                    .create().show();
            return;
        }

        if (!agreed) {
            new AlertDialog.Builder(context)
                    .setTitle("多人联机 · 协议与免责声明")
                    .setMessage(activity.getString(R.string.qcl_multiplayer_disclaimer))
                    .setCancelable(false)
                    .setPositiveButton("我已知悉并同意", (d, i) -> {
                        sp.edit().putBoolean("agreed", true).apply();
                        enable(activity, context, sp);
                    })
                    .setNegativeButton("取消", (d, i) -> {
                    })
                    .create().show();
            return;
        }
        enable(activity, context, sp);
    }

    private static void enable(Activity activity, Context context, SharedPreferences sp) {
        sp.edit().putBoolean("enabled", true).apply();
        TerracottaHelper.initialize(activity);
        new AlertDialog.Builder(context)
                .setTitle("多人联机 · 已开启")
                .setMessage("陶瓷联机已开启。\n\n接下来：启动游戏 → 打开悬浮窗的「联机模块」→ 选择创建房间（当房主）或加入房间（当访客）。\n\n房主请先在游戏里把存档「对局域网开放」，再把邀请码发给伙伴。")
                .setPositiveButton("知道了", (d, i) -> {
                })
                .create().show();
    }

    // ==================== 游戏内：使用 ====================

    /** 游戏内联机模块：只有游戏外开启过才能使用。 */
    public static void showInGame(Activity activity, Context context) {
        if (activity == null || context == null) {
            return;
        }
        SharedPreferences sp = activity.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!sp.getBoolean("enabled", false)) {
            new AlertDialog.Builder(context)
                    .setTitle("联机模块 · 尚未开启")
                    .setMessage("陶瓷联机需要在游戏外开启：\n\n回到启动器主界面 → 点顶部的「多人联机」→ 同意协议后开启。\n开启之后再进游戏，这里就能创建或加入房间了。")
                    .setPositiveButton("知道了", (d, i) -> {
                    })
                    .create().show();
            return;
        }
        showMenu(activity, context);
    }

    private static void showMenu(final Activity activity, final Context context) {
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_hin2n_menu, null);
        final AlertDialog dialog = new AlertDialog.Builder(context).setView(content).setCancelable(true).create();

        content.findViewById(R.id.create).setOnClickListener(v -> {
            dialog.dismiss();
            TerracottaHelper.host(activity);
            Toast.makeText(context, "正在创建房间…", Toast.LENGTH_SHORT).show();
            pollRoom(activity, context, true);
        });
        content.findViewById(R.id.join).setOnClickListener(v -> {
            dialog.dismiss();
            final EditText input = new EditText(context);
            input.setHint("粘贴房主发给你的邀请码");
            new AlertDialog.Builder(context)
                    .setTitle("加入房间")
                    .setView(input)
                    .setPositiveButton("加入", (d2, i2) -> {
                        TerracottaHelper.join(activity, input.getText().toString());
                        Toast.makeText(context, "正在加入房间…", Toast.LENGTH_SHORT).show();
                        pollRoom(activity, context, false);
                    })
                    .setNegativeButton("取消", (d2, i2) -> {
                    })
                    .create().show();
        });
        content.findViewById(R.id.info).setOnClickListener(v -> {
            String code = TerracottaHelper.getInviteCode();
            String addr = TerracottaHelper.getServerAddress();
            String st = TerracottaHelper.getState();
            String msg;
            if (!code.isEmpty()) {
                msg = "你是房主\n邀请码：" + code;
            } else if (!addr.isEmpty()) {
                msg = "你是访客\n服务器地址：" + addr;
            } else {
                msg = "尚未创建/加入房间\n当前状态：" + (st.isEmpty() ? "未初始化" : st);
            }
            new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.dialog_hin2n_menu_info))
                    .setMessage(msg)
                    .setPositiveButton("知道了", (d2, i2) -> {
                    })
                    .create().show();
        });
        content.findViewById(R.id.help).setOnClickListener(v -> new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.dialog_hin2n_help_title))
                .setMessage(context.getString(R.string.dialog_hin2n_help_text))
                .setPositiveButton("知道了", (d2, i2) -> {
                })
                .create().show());
        dialog.show();
    }

    /** 轮询后端状态：拿到邀请码 / 服务器地址后弹窗并自动复制 */
    private static void pollRoom(final Activity activity, final Context context, final boolean host) {
        final Handler handler = new Handler(Looper.getMainLooper());
        final int[] tries = {0};
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String value = host ? TerracottaHelper.getInviteCode() : TerracottaHelper.getServerAddress();
                if (value != null && !value.isEmpty()) {
                    showResult(activity, context, host, value);
                    return;
                }
                if (++tries[0] > 60) {
                    String st = TerracottaHelper.getState();
                    Toast.makeText(context,
                            "联机后端未就绪（状态：" + (st == null || st.isEmpty() ? "无" : st) + "），请确认已授权 VPN 后重试",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                handler.postDelayed(this, 500);
            }
        }, 500);
    }

    private static void showResult(Activity activity, Context context, boolean host, String value) {
        copy(context, value);
        String title = host ? "房间已创建 · 邀请码（已复制）" : "已加入房间 · 服务器地址（已复制）";
        String body = host
                ? "邀请码：\n" + value + "\n\n把它发给联机伙伴即可。"
                : "在游戏的「多人游戏 → 添加服务器」里，地址填：\n" + value;
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(body)
                .setPositiveButton("再复制一次", (d, i) -> {
                    copy(context, value);
                    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("关闭", (d, i) -> {
                })
                .create().show();
    }

    private static void copy(Context context, String value) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("qcl_multiplayer", value));
            }
        } catch (Throwable ignored) {
        }
    }
}
