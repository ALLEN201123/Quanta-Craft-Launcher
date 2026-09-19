package com.qcl.launcher.launcher.launch;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

/**
 * ★★★ 1.1.4：vivo / iQOO「游戏魔盒」（GameCube，包名 com.vivo.gamecube）集成。
 *
 * 背景：vivo / iQOO 的「游戏魔盒」在系统层提供 性能模式 / 防误触 / 屏蔽通知 / 网络加速 / 4D 震感，
 * 对跑 MC 这类重负载游戏很有帮助。但它的：
 *   · 总开关
 *   · 「游戏时自动开启魔盒」
 *   · 需要加入「游戏空间」的应用列表
 * **都只能由用户在系统设置里配置，vivo 没有提供任何第三方可调用的开启接口。**
 *
 * 所以启动器能做到的最大程度是：
 *   1) **检测**：是否 vivo/iQOO 设备、是否装了游戏魔盒；
 *   2) **自动打开**：在启动游戏时自动把游戏魔盒调起来（放在 Activity onCreate 最前面，
 *      此时游戏还没开始渲染，不会打断它），玩家在里面开启后返回即可继续游戏。
 *
 * 不硬编码 Activity 名 —— 用 PackageManager#getLaunchIntentForPackage 让系统自己解析，
 * 这样 vivo 以后改 Activity 名也不会失效。
 */
public final class VivoGameCube {

    /** 游戏魔盒的包名（vivo/iQOO 系统应用） */
    public static final String PKG = "com.vivo.gamecube";

    private VivoGameCube() {
    }

    /** 是否 vivo / iQOO 家族设备 */
    public static boolean isVivoFamily() {
        try {
            String brand = Build.BRAND == null ? "" : Build.BRAND.toLowerCase();
            String manu = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase();
            return brand.contains("vivo") || brand.contains("iqoo")
                    || manu.contains("vivo") || manu.contains("iqoo");
        } catch (Throwable t) {
            return false;
        }
    }

    /** 设备上是否安装了「游戏魔盒」 */
    public static boolean isInstalled(Context ctx) {
        if (ctx == null) return false;
        try {
            ctx.getPackageManager().getPackageInfo(PKG, 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 取「游戏魔盒」的启动 Intent（由系统解析，不硬编码 Activity） */
    public static Intent launchIntent(Context ctx) {
        if (ctx == null) return null;
        try {
            PackageManager pm = ctx.getPackageManager();
            Intent i = pm.getLaunchIntentForPackage(PKG);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            return i;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 尝试打开「游戏魔盒」。
     *
     * @return true 表示已成功发出启动 Intent
     */
    public static boolean open(Context ctx) {
        Intent i = launchIntent(ctx);
        if (i == null) return false;
        try {
            ctx.startActivity(i);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 启动游戏时的入口：仅当「vivo/iQOO 设备 + 已安装游戏魔盒」时自动打开一次，
     * 并给出提示（让玩家知道这是启动器自动做的、以及怎么让它长期生效）。
     *
     * @return 是否触发过
     */
    public static boolean autoOpenOnGameLaunch(Context ctx) {
        if (!isVivoFamily() || !isInstalled(ctx)) return false;
        boolean ok = open(ctx);
        if (ok) {
            try {
                Toast.makeText(ctx,
                        "已为你打开「游戏魔盒」：请开启总开关，并打开「游戏时自动开启魔盒」，"
                                + "再把本启动器加入游戏空间。返回后游戏会继续启动。",
                        Toast.LENGTH_LONG).show();
            } catch (Throwable ignored) {
            }
        }
        return ok;
    }
}
