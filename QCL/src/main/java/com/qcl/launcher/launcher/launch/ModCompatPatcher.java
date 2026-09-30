package com.qcl.launcher.launcher.launch;

import android.content.Context;
import android.util.Log;

import com.qcl.launcher.launcher.setting.game.GameLaunchSetting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * ★★★ 1.3.8：启动前的 mod 兼容性配置修改。
 *
 * <p>照搬 FCL {@code FCLGameLauncher.modifyIfConfigDetected}：某些渲染类 mod
 * （Sodium / Rubidium 等）在特定渲染器（GL4ES、VGPU）下会画面异常（闪烁 / 花屏），
 * FCL 的做法是启动前修改 mod 自己的配置文件，关闭有问题的渲染特性。
 *
 * <p>渲染器对照：FCL 的 {@code RENDERER_GL4ES}（Holy-GL4ES）↔ QCL 的 {@code opengles2}；
 * FCL 的 {@code RENDERER_VGPU} ↔ QCL 的 {@code opengles3_vgpu}。
 */
public final class ModCompatPatcher {

    private static final String TAG = "jrelog";

    /** GL4ES / VGPU 两个渲染器下需要打补丁（照 FCL 的渲染器集合） */
    private static final String[] GL4ES_VGPU = {"opengles2", "opengles3_vgpu"};

    private ModCompatPatcher() {
    }

    public static void patchBeforeLaunch(Context context, GameLaunchSetting setting) {
        if (setting == null) {
            return;
        }
        try {
            // Sodium（Fabric / Quilt / NeoForge 的 sodium）
            modifyIfConfigDetected(setting, "sodium-mixins.properties", "",
                    "mixin.features.chunk_rendering=false", false, GL4ES_VGPU);
            // Rubidium（Forge 版 Sodium）
            modifyIfConfigDetected(setting, "rubidium-mixins.properties", "",
                    "mixin.features.chunk_rendering=false", false, GL4ES_VGPU);
        } catch (Throwable t) {
            Log.w(TAG, "[mod兼容] 启动前配置修改失败", t);
        }
    }

    private static void modifyIfConfigDetected(GameLaunchSetting setting, String config,
                                               String option, String replacement,
                                               boolean overwrite, String... rendererIds) {
        boolean patch = rendererIds == null || rendererIds.length == 0;
        if (!patch) {
            for (String id : rendererIds) {
                if (id != null && id.equals(setting.pojavRenderer)) {
                    patch = true;
                    break;
                }
            }
        }
        if (!patch) {
            return;
        }
        boolean changed = false;
        for (File configDir : configDirs(setting)) {
            File configFile = new File(configDir, config);
            if (!configFile.isFile()) {
                continue;
            }
            patchFile(configFile, option, replacement, overwrite);
            changed = true;
        }
        if (changed) {
            Log.i(TAG, "[mod兼容] 已检查并修改配置：" + config);
        }
    }

    /** 依次尝试：游戏目录 / config、gameFileDirectory / config、版本目录 / config（覆盖版本隔离两种情况） */
    private static List<File> configDirs(GameLaunchSetting setting) {
        List<File> roots = new ArrayList<File>();
        addIfNotNull(roots, setting.game_directory);
        addIfNotNull(roots, setting.gameFileDirectory);
        if (setting.currentVersion != null && !setting.currentVersion.isEmpty()) {
            // 版本隔离：<版本目录>/config
            File v = new File(setting.currentVersion);
            if (v.isDirectory()) {
                roots.add(v);
            }
        }
        List<File> out = new ArrayList<File>();
        for (File d : roots) {
            out.add(new File(d, "config"));
        }
        return out;
    }

    private static void addIfNotNull(List<File> list, String path) {
        if (path != null && !path.isEmpty()) {
            File f = new File(path);
            boolean dup = false;
            for (File e : list) {
                if (e.equals(f)) {
                    dup = true;
                    break;
                }
            }
            if (!dup) {
                list.add(f);
            }
        }
    }

    private static void patchFile(File configFile, String option, String replacement, boolean overwrite) {
        StringBuilder str = new StringBuilder();
        try (BufferedReader bfr = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = bfr.readLine()) != null) {
                if (overwrite && option != null && !option.isEmpty() && line.contains(option)) {
                    str.append(replacement).append("\n");
                } else {
                    str.append(line).append("\n");
                }
            }
            if (!overwrite && !str.toString().contains(replacement.replace("false", "").replace("true", ""))) {
                str.append(replacement);
            }
        } catch (Exception e) {
            Log.w(TAG, "[mod兼容] 读取 " + configFile.getName() + " 失败", e);
            return;
        }
        if (str.length() > 0) {
            try (FileWriter fw = new FileWriter(configFile)) {
                fw.write(str.toString());
            } catch (Exception e) {
                Log.w(TAG, "[mod兼容] 写入 " + configFile.getName() + " 失败", e);
            }
        }
    }
}
