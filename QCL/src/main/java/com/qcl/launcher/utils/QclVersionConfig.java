package com.qcl.launcher.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.qcl.launcher.launcher.MainActivity;

import java.io.File;

/**
 * ★ 1.3.0：按版本写配置的小工具。
 *
 * 目前只干一件事：**把某个版本的「不检查游戏文件」打开**
 * （{@code versions/<id>/qcl.cfg} 里的 {@code PrivateGameSetting.notCheckMinecraft}）。
 *
 * 为什么要自动打开：
 * 启动器默认会校验并「修复」游戏文件 —— 对远古版本来说这会把玩家自己装的东西
 * （汉化过的 jar、ModLoader/Babric 改过的本体）**覆盖回去**，还会顺手把音效音乐重下一遍。
 * 所以远古版本装好之后就默认关掉校验，玩家想校验可以自己去「版本设置 → 检查游戏文件」打开。
 */
public final class QclVersionConfig {

    private static final String TAG = "QCLVersionConfig";

    private QclVersionConfig() {
    }

    /**
     * 打开某个版本的「不检查游戏文件」。
     *
     * @param activity    当前 Activity（用来拿默认设置当模板）
     * @param versionId   版本目录名（versions/&lt;versionId&gt;）
     * @return 是否写入成功
     */
    public static boolean enableNotCheckMinecraft(MainActivity activity, String versionId) {
        if (activity == null || versionId == null || versionId.isEmpty()) {
            return false;
        }
        try {
            File versionDir = new File(activity.launcherSetting.gameFileDirectory, "versions/" + versionId);
            File cfg = new File(versionDir, "qcl.cfg");
            PrivateGameSetting setting = GsonUtils.getPrivateGameSettingFromFile(cfg.getAbsolutePath());
            if (setting == null) {
                // 这个版本还没有自己的配置：拿当前设置深拷贝一份当模板
                PrivateGameSetting template = activity.privateGameSetting;
                if (template == null) {
                    Log.w(TAG, "拿不到设置模板，跳过自动关闭校验");
                    return false;
                }
                Gson gson = new Gson();
                setting = gson.fromJson(gson.toJson(template), PrivateGameSetting.class);
                if (setting == null) {
                    return false;
                }
            }
            if (setting.notCheckMinecraft) {
                return true;   // 已经开着，不用重复写
            }
            setting.notCheckMinecraft = true;
            GsonUtils.savePrivateGameSetting(setting, cfg.getAbsolutePath());
            Log.i(TAG, "已关闭版本 " + versionId + " 的文件校验（写入 " + cfg.getAbsolutePath() + "）");
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "关闭版本 " + versionId + " 的文件校验失败: " + t);
            return false;
        }
    }
}
