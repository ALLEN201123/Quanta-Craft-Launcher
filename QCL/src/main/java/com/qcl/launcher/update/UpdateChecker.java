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
                        List<String> urls = new ArrayList<String>();
                        if (LocaleUtils.isChinese(UpdateChecker.this.context)) {
                            urls.add(UPDATE_URL_CN);   // 国内先试 Gitee
                            urls.add(UPDATE_URL);      // 再 GitHub raw
                        } else {
                            urls.add(UPDATE_URL);
                            urls.add(UPDATE_URL_CN);
                        }
                        urls.add(UPDATE_URL_CDN);      // 最后 jsDelivr

                        String updateJson = null;
                        for (String url : urls) {
                            try {
                                updateJson = NetworkUtils.doGet(NetworkUtils.toURL(url));
                                if (updateJson != null && updateJson.trim().length() > 0) {
                                    break;
                                }
                            }
                            catch (Throwable e) {
                                Log.w("jrelog", "[更新] 拉取失败，换下一个源: " + url, e);
                                updateJson = null;
                            }
                        }
                        if (updateJson == null) {
                            Log.w("jrelog", "[更新] 所有源都拉不到，跳过本次检查");
                            UpdateChecker.this.isChecking = false;
                            if (callback != null) {
                                UpdateChecker.this.handler.post(() -> callback.onFinish(true));
                            }
                            return;
                        }

                        // 2) 解析
                        UpdateJSON json = (UpdateJSON) new Gson().fromJson(updateJson, UpdateJSON.class);
                        if (json == null) {
                            Log.w("jrelog", "[更新] 解析失败，跳过");
                            UpdateChecker.this.isChecking = false;
                            if (callback != null) {
                                UpdateChecker.this.handler.post(() -> callback.onFinish(true));
                            }
                            return;
                        }

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

