package com.qcl.launcher.launcher.launch;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;

import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.gson.GsonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ★★★ 1.1.1：渲染器选择器（主界面长按启动按钮 / 版本设置 / 全局游戏设置 三处共用）。
 *
 * 之前版本设置页只有两个写死的单选按钮（Holy-GL4ES / VirGLRenderer），既不全也和
 * {@link RendererCompat} 的注册表不一致；现在统一改为弹窗，列表直接来自注册表：
 * 全称 + 支持版本区间 + ★推荐 + ✓当前 + ⚠不支持当前版本。
 * 原生库文件不存在的渲染器（例如没导入的 MobileGlues）不显示。
 */
public final class RendererPicker {

    private RendererPicker() {
    }

    /** 从版本目录路径取版本名（用于兼容性判断）。 */
    public static String versionNameOf(String versionPath) {
        try {
            if (versionPath != null && !versionPath.isEmpty()) {
                return new File(versionPath).getName();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** 渲染器 id → 界面上显示的名字。 */
    public static String displayNameOf(String id) {
        RendererCompat.Info info = RendererCompat.find(id);
        return info != null ? info.displayName : id;
    }

    /** 弹出渲染器选择窗口。onChanged 可以为 null。 */
    public static void show(final Activity activity, final PrivateGameSetting pgs,
                            final String versionPath, final Runnable onChanged) {
        try {
            final String mcVer = versionNameOf(versionPath);
            final String current = pgs.pojavLauncherSetting.renderer;
            final List<String> labels = new ArrayList<String>();
            final List<String> ids = new ArrayList<String>();
            String nativeDir = activity.getApplicationInfo().nativeLibraryDir;

            // ★★★ 2026-09-19 修复「外部渲染器（mg）检测不到」：
            //   原逻辑对「APK 内没有该 so」的渲染器直接 continue 隐藏 —— 而 mg（MobileGlues）
            //   是外部渲染器，so 由玩家放在 <游戏目录>/renderer/mg/，永远不会出现在 APK 里，
            //   于是列表里根本看不到它，玩家误以为"没抄/没加"。
            //   现在：**所有渲染器都列出**，缺库的标注「⚠缺少库文件（需自行导入）」，一目了然。
            final String gameDir = RendererCompat.gameDirOf(versionPath);
            for (RendererCompat.Info info : RendererCompat.ALL) {
                boolean libOk = true;
                if (info.glName != null && !info.glName.isEmpty()) {
                    boolean inApk = new File(nativeDir, info.glName).isFile();
                    // 外部渲染器（mg）：优先从「已安装的插件应用」取（与 FCL 一致），
                    // 其次才是 <gameDir>/renderer/<id>/ 手工放置
                    boolean external = RendererCompat.resolveRendererLibDir(activity, info.id, versionPath, gameDir) != null;
                    libOk = inApk || external;
                }
                String line = info.displayName + "\n（" + info.supportRangeText() + "）"
                        + (info.recommended ? " ★推荐" : "")
                        + (info.id.equals(current) ? "  ✓当前" : "")
                        + (RendererCompat.supports(info.id, mcVer) ? "" : "  ⚠不支持当前版本")
                        + (libOk ? "" : "  ⚠缺少库文件");
                labels.add(line);
                ids.add(info.id);
            }

            new AlertDialog.Builder(activity)
                    .setTitle("选择渲染器（当前版本 " + (mcVer != null ? mcVer : "?") + "）")
                    .setItems(labels.toArray(new String[0]), (d, which) -> {
                        final String id = ids.get(which);
                        String warnText = RendererCompat.warningOf(id, mcVer);
                        // ★ mg 是外部渲染器：缺库时明确告诉玩家放哪里
                        if ("mg".equals(id) && RendererCompat.resolveRendererLibDir(activity, id, versionPath, gameDir) == null) {
                            String mgTip = "未检测到 MobileGlues 渲染器库（libmobileglues.so）。\n\n"
                                    + "推荐做法：安装官方 MobileGlues 插件 APK\n"
                                    + "（github.com/MobileGL-Dev/MobileGlues-release/releases），装好后回到这里重新选择即可（无需拷贝文件）。\n\n"
                                    + "也可以把 libmobileglues.so 手动放到：\n"
                                    + (gameDir != null ? gameDir : "<游戏目录>") + "/renderer/mg/";
                            warnText = (warnText == null) ? mgTip : (mgTip + "\n\n" + warnText);
                        }
                        if (warnText != null) {
                            new AlertDialog.Builder(activity)
                                    .setTitle("渲染器兼容性提示")
                                    .setMessage(warnText)
                                    .setPositiveButton("我就要用这个渲染器", (d2, w2) -> {
                                        apply(activity, pgs, id);
                                        if (onChanged != null) onChanged.run();
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        } else {
                            apply(activity, pgs, id);
                            if (onChanged != null) onChanged.run();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (Throwable e) {
            Toast.makeText(activity, "打开渲染器选择失败: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /** 写入设置并提示。 */
    public static void apply(Activity activity, PrivateGameSetting pgs, String id) {
        try {
            pgs.pojavLauncherSetting.renderer = id;
            GsonUtils.savePrivateGameSetting(pgs,
                    AppManifest.SETTING_DIR + "/private_game_setting.json");
            Toast.makeText(activity, "渲染器已切换: " + displayNameOf(id),
                    Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }
}
