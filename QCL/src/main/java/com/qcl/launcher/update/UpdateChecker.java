/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.content.pm.PackageInfo
 *  android.content.pm.PackageManager
 *  android.content.pm.PackageManager$NameNotFoundException
 *  android.os.Handler
 *  android.util.Log
 *  android.widget.Toast
 *  com.google.gson.Gson
 */
package com.qcl.launcher.update;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.update.LauncherVersion;
import com.qcl.launcher.update.UpdateDialog;
import com.qcl.launcher.update.UpdateJSON;
import com.qcl.launcher.utils.LocaleUtils;
import com.qcl.launcher.utils.io.NetworkUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.qcl.launcher.R;
public class UpdateChecker {
    public static final String UPDATE_URL = "https://raw.githubusercontent.com/ALLEN201123/Quanta-Craft-Launcher/main/launcher_version.json";
    public static final String UPDATE_URL_CDN = "https://cdn.jsdelivr.net/gh/ALLEN201123/Quanta-Craft-Launcher@main/launcher_version.json";
    /**
     * ★★★ 1.2.2：国内镜像（Gitee），照 FCL 的做法
     * （FCL 用的是 https://gitee.com/fcl-team/FCL-Repo/raw/main/res/version_map.json）。
     *
     * 为什么需要它：上面那两个地址国内经常连不上，很多用户一直收不到更新提示 ——
     * 不是版本号没改，是根本拉不到文件。Gitee 镜像就是给国内用户用的。
     *
     * 这个仓库只放一个 launcher_version.json，不放安装包；
     * 安装包仍然从 GitHub Release 下（国内用户可以选弹窗里的「网盘下载」）。
     */
    public static final String UPDATE_URL_CN = "https://gitee.com/allne201123/qcl-repo/raw/master/launcher_version.json";

    /**
     * ★★★ 1.2.6：再多兜几个国内能通的源。
     * 实测（2026-09-22，本机）：
     *   fastly.jsdelivr / gcore.jsdelivr / ghproxy.net 都能拿到最新 json，
     *   而 Gitee 镜像那份**曾经落后一版**（只推了 GitHub 没推它）——
     *   这就是「手机上收不到更新推送」的真凶（旧代码中文环境先读 Gitee，
     *   读到旧版本号就判定「没有新版」，后面更快的源根本没机会看）。
     */
    public static final String UPDATE_URL_FASTLY = "https://fastly.jsdelivr.net/gh/ALLEN201123/Quanta-Craft-Launcher@main/launcher_version.json";
    public static final String UPDATE_URL_GCORE = "https://gcore.jsdelivr.net/gh/ALLEN201123/Quanta-Craft-Launcher@main/launcher_version.json";
    public static final String UPDATE_URL_PROXY = "https://ghproxy.net/https://raw.githubusercontent.com/ALLEN201123/Quanta-Craft-Launcher/main/launcher_version.json";

    /**
     * ★★★ 1.2.8：再加两家国内可达的 jsDelivr 镜像。
     * 为什么：实测不同 CDN 的缓存刷新**不同步** —— 同一时刻 fastly/gcore 还停在旧版本号，
     * 而 cdn.jsdmirror.com、jsdelivr.b-cdn.net 已经是新的（0.4s / 1.1s 就返回）。
     * 1.2.6 只列了部分源，玩家如果恰好只能连到那几个「还没刷新」的源，
     * 取到的最大值就还是旧的 → 依然收不到提示。多列几个源，命中新版本号的几率才够。
     */
    public static final String UPDATE_URL_JSDMIRROR = "https://cdn.jsdmirror.com/gh/ALLEN201123/Quanta-Craft-Launcher@main/launcher_version.json";
    public static final String UPDATE_URL_BCDN = "https://jsdelivr.b-cdn.net/gh/ALLEN201123/Quanta-Craft-Launcher@main/launcher_version.json";
    private Context context;
    private MainActivity activity;
    private boolean isChecking;
    private Handler handler;

    public UpdateChecker(Context context, MainActivity activity) {
        this.context = context;
        this.activity = activity;
        this.handler = new Handler();
    }

    public void checkAuto() {
        this.check(false, false, null);
    }

    public void checkManually(UpdateCallback callback) {
        this.check(true, true, callback);
    }

    public void check(boolean getBetaVersion, UpdateCallback callback) {
        this.check(getBetaVersion, true, callback);
    }

    public void check(final boolean getBetaVersion, final boolean showAlert, final UpdateCallback callback) {
        if (!this.isChecking) {
            new Thread(new Runnable(){

                @Override
                public void run() {
                    // ★★★ 1.2.2：检查逻辑照 FCL 的 UpdateChecker.check() 重写。
                    //   FCL 的顺序是：
                    //     取远程文件 → 解析 → 遍历找第一个 versionCode 更大的 →
                    //     没被忽略就弹窗 → 重置 isChecking → return
                    //   我们也按这个顺序来。唯一差别：FCL 的远程文件是「版本数组」
                    //   （version_map.json），我们是 {latestRelease, latestPrerelease} 结构 ——
                    //   这个不能改成数组，改了老版本 App 会解析失败，所以用那两个字段当候选
                    //   来跑同样的遍历。
                    try {
                        if (callback != null) {
                            UpdateChecker.this.handler.post(callback::onCheck);
                        }
                        UpdateChecker.this.isChecking = true;
                        if (showAlert) {
                            UpdateChecker.this.handler.post(() -> Toast.makeText((Context)UpdateChecker.this.context, (CharSequence)UpdateChecker.this.context.getString(R.string.update_checking), (int)0).show());
                        }

                        // 1) 取远程文件。
                        //    照 FCL：中文环境优先走国内镜像。
                        //    ★ 但 FCL 是「二选一、不兜底」—— 选中的那个挂了就整个失败。
                        //      这里改成按顺序逐个试，谁先通用谁，更不容易「有更新却检测不到」。
                        // ★★★ 1.2.6：国内优先，多带几个兜底源（顺序只是「先试哪个」，不影响结果）
                        List<String> urls = new ArrayList<String>();
                        if (LocaleUtils.isChinese(UpdateChecker.this.context)) {
                            urls.add(UPDATE_URL_CN);
                            urls.add(UPDATE_URL_JSDMIRROR);
                            urls.add(UPDATE_URL_BCDN);
                            urls.add(UPDATE_URL_FASTLY);
                            urls.add(UPDATE_URL_GCORE);
                            urls.add(UPDATE_URL);
                            urls.add(UPDATE_URL_CDN);
                            urls.add(UPDATE_URL_PROXY);
                        } else {
                            urls.add(UPDATE_URL);
                            urls.add(UPDATE_URL_CDN);
                            urls.add(UPDATE_URL_FASTLY);
                            urls.add(UPDATE_URL_GCORE);
                            urls.add(UPDATE_URL_JSDMIRROR);
                            urls.add(UPDATE_URL_BCDN);
                            urls.add(UPDATE_URL_PROXY);
                            urls.add(UPDATE_URL_CN);
                        }

                        // ★★★ 1.2.6 关键修法：**每个源都读一遍，取 versionCode 最大的那份**。
                        //   老代码是「谁先返回用谁」→ 中文环境先读 Gitee 镜像，
                        //   镜像只要落后一版（某次只推了 GitHub、没推镜像），
                        //   手机就会拿到旧版本号 → 判定「没有新版」→ 更新提示永远收不到，
                        //   而且**后面的源根本没机会被读到**。
                        //   改成取最大值后：任何一个源是新的，就一定能收到提示。
                        UpdateJSON json = null;
                        int bestCode = -1;
                        String bestUrl = null;
                        for (String url : urls) {
                            try {
                                String text = NetworkUtils.doGet(NetworkUtils.toURL(url));
                                if (text == null || text.trim().isEmpty()) {
                                    continue;
                                }
                                UpdateJSON parsed = (UpdateJSON) new Gson().fromJson(text, UpdateJSON.class);
                                if (parsed == null) {
                                    continue;
                                }
                                int code = parsed.latestRelease == null ? -1 : parsed.latestRelease.versionCode;
                                if (code > bestCode) {
                                    bestCode = code;
                                    json = parsed;
                                    bestUrl = url;
                                }
                            }
                            catch (Throwable e) {
                                Log.w("jrelog", "[更新] 拉取失败，换下一个源: " + url, e);
                            }
                        }
                        if (json == null) {
                            Log.w("jrelog", "[更新] 所有源都拉不到，跳过本次检查");
                            UpdateChecker.this.isChecking = false;
                            if (callback != null) {
                                UpdateChecker.this.handler.post(() -> callback.onFinish(true));
                            }
                            return;
                        }
                        Log.i("jrelog", "[更新] 远端最高版本号 " + bestCode + "（来自 " + bestUrl + "）");

                        // 3) 遍历候选，找第一个版本比本机大的（跟 FCL 一样）
                        List<LauncherVersion> candidates = new ArrayList<LauncherVersion>();
                        if (getBetaVersion && json.latestPrerelease != null) {
                            candidates.add(json.latestPrerelease);
                        }
                        candidates.add(json.latestRelease);

                        int current = UpdateChecker.this.getPackageVersionCode();
                        for (LauncherVersion version : candidates) {
                            if (version == null) {
                                continue;
                            }
                            boolean isBeta = version == json.latestPrerelease;
                            if (version.versionCode > current) {
                                if (!UpdateChecker.this.isIgnore(version.versionCode)) {
                                    UpdateChecker.this.showUpdateDialog(version, isBeta);
                                }
                                UpdateChecker.this.isChecking = false;
                                if (callback != null) {
                                    UpdateChecker.this.handler.post(() -> callback.onFinish(false));
                                }
                                return;
                            }
                        }

                        // 4) 没有新版本
                        if (showAlert) {
                            UpdateChecker.this.handler.post(() -> Toast.makeText((Context)UpdateChecker.this.context, (CharSequence)UpdateChecker.this.context.getString(R.string.update_not_exist), (int)0).show());
                        }
                        UpdateChecker.this.isChecking = false;
                        if (callback != null) {
                            UpdateChecker.this.handler.post(() -> callback.onFinish(true));
                        }
                    }
                    catch (Throwable e) {
                        Log.w("jrelog", "[更新] 检查更新失败（已忽略）", e);
                        if (callback != null) {
                            UpdateChecker.this.handler.post(() -> callback.onFinish(true));
                        }
                        UpdateChecker.this.isChecking = false;
                    }
                }
            }).start();
        }
    }

    private int getPackageVersionCode() {
        PackageManager pm = this.context.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(this.context.getPackageName(), 0);
            int versionCode = packageInfo.versionCode;
            return versionCode;
        }
        catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void showUpdateDialog(LauncherVersion version, boolean isBeta) {
        this.handler.post(() -> {
            UpdateDialog dialog = new UpdateDialog(this.context, this.activity, version, isBeta);
            dialog.show();
        });
    }

    public static boolean isIgnore(Context context, int versionCode) {
        return context.getSharedPreferences("launcher", 0).getInt("ignore_update", -1) == versionCode;
    }

    public static void setIgnore(Context context, int versionCode) {
        context.getSharedPreferences("launcher", 0).edit().putInt("ignore_update", versionCode).apply();
    }

    private boolean isIgnore(int versionCode) {
        return UpdateChecker.isIgnore(this.context, versionCode);
    }

    public static interface UpdateCallback {
        public void onCheck();

        public void onFinish(boolean var1);
    }
}

