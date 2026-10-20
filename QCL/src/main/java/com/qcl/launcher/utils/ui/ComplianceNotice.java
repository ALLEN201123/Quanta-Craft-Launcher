package com.qcl.launcher.utils.ui;

import android.app.Activity;
import android.app.AlertDialog;

import com.qcl.launcher.R;

/**
 * ★★★★★ 2026-10-20：**合规性临时封禁**的统一提示。
 *
 * <p>背景：2026-10-20 起，PCL / HMCL 等在中国大陆展业的第三方启动器
 * 因**合规性原因**关停了 Minecraft 核心文件与资源包的下载功能
 * （MineBBS 官方公告，2026-10-20 17:55，原文明确说
 * 「本次事件与网易游戏、Mojang Studio 与 Microsoft 均无直接关联」，
 * 且「各开发组均未对关闭具体原因做出公告」）。
 *
 * <p>本项目的处置：**临时封禁**下面这些与「下载 Minecraft 本体 / 分发核心文件」
 * 相关的功能入口，**代码全部保留不删**（只把调用点注释掉），风波过去后可以一条命令恢复。
 *
 * <p>被封禁的功能（见 {@code UIManager#complianceBannedUIs()}）：
 * <ul>
 *   <li>下载页 {@code downloadUI}（游戏版本 / 整合包 / 模组 / 资源包 / 光影 / 收藏 / 世界 七个标签）</li>
 *   <li>陶瓷联机 {@code multiplayerUI}</li>
 *   <li>安装游戏 {@code installGameUI}、安装整合包 {@code installPackageUI}</li>
 *   <li>各加载器的版本下载页（Forge / NeoForge / Fabric / FabricAPI / LiteLoader /
 *       OptiFine / Quilt / QuiltAPI / 通用列表页 simpleListUI）</li>
 * </ul>
 *
 * <p>**没有封**的（不涉及分发 MC 本体）：账号、版本列表（本地已装版本）、
 * 游戏管理、实验室、JAR 启动、设置、主页。
 */
public final class ComplianceNotice {

    private ComplianceNotice() {
    }

    /**
     * 弹一个"此功能已被禁止"的提示。
     *
     * @param activity 任意 Activity（传 null 时静默返回，不崩）
     */
    public static void show(Activity activity) {
        if (activity == null) {
            return;
        }
        try {
            if (activity.isFinishing()) {
                return;
            }
            if (android.os.Build.VERSION.SDK_INT >= 17 && activity.isDestroyed()) {
                return;
            }
        } catch (Throwable ignored) {
        }
        try {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.qcl_compliance_banned_title)
                    .setMessage(R.string.qcl_compliance_banned_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .setCancelable(true)
                    .show();
        } catch (Throwable ignored) {
            // 极端情况下（窗口已销毁）只吞掉，不把启动器弄崩
        }
    }

    /** 只弹一个短提示（Toast 风格），用于不想打断操作的地方。 */
    public static void toast(Activity activity) {
        if (activity == null) {
            return;
        }
        try {
            android.widget.Toast.makeText(activity,
                    R.string.qcl_compliance_banned_short,
                    android.widget.Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }
}
