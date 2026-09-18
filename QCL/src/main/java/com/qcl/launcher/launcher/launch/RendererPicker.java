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

            for (RendererCompat.Info info : RendererCompat.ALL) {
                if (info.glName != null && !info.glName.isEmpty()) {
                    if (!new File(nativeDir, info.glName).isFile()) {
                        continue;   // 库不存在就不显示
                    }
                }
                String line = info.displayName + "\n（" + info.supportRangeText() + "）"
                        + (info.recommended ? " ★推荐" : "")
                        + (info.id.equals(current) ? "  ✓当前" : "")
                        + (RendererCompat.supports(info.id, mcVer) ? "" : "  ⚠不支持当前版本");
                labels.add(line);
                ids.add(info.id);
            }

            new AlertDialog.Builder(activity)
                    .setTitle("选择渲染器（当前版本 " + (mcVer != null ? mcVer : "?") + "）")
                    .setItems(labels.toArray(new String[0]), (d, which) -> {
                        final String id = ids.get(which);
                        String warnText = RendererCompat.warningOf(id, mcVer);
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
