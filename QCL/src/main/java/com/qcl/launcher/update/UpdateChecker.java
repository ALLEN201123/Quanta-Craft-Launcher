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
import com.qcl.launcher.utils.io.NetworkUtils;
import java.io.IOException;

import com.qcl.launcher.R;
public class UpdateChecker {
    public static final String UPDATE_URL = "https://raw.githubusercontent.com/ALLEN201123/Quanta-Craft-Launcher/main/launcher_version.json";
    public static final String UPDATE_URL_CDN = "https://cdn.jsdelivr.net/gh/ALLEN201123/Quanta-Craft-Launcher@main/launcher_version.json";
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
                    try {
                        boolean isBeta;
                        LauncherVersion latest;
                        String updateJson;
                        if (callback != null) {
                            UpdateChecker.this.handler.post(callback::onCheck);
                        }
                        UpdateChecker.this.isChecking = true;
                        if (showAlert) {
                            UpdateChecker.this.handler.post(() -> Toast.makeText((Context)UpdateChecker.this.context, (CharSequence)UpdateChecker.this.context.getString(R.string.update_checking), (int)0).show());
                        }
                        try {
                            updateJson = NetworkUtils.doGet(NetworkUtils.toURL(UpdateChecker.UPDATE_URL));
                        }
                        catch (IOException primaryFailed) {
                            updateJson = NetworkUtils.doGet(NetworkUtils.toURL(UpdateChecker.UPDATE_URL_CDN));
                        }
                        UpdateJSON json = (UpdateJSON)new Gson().fromJson(updateJson, UpdateJSON.class);
                        if (json == null || json.latestRelease == null) {
                            Log.w((String)"jrelog", (String)"[\u66f4\u65b0] \u66f4\u65b0\u4fe1\u606f\u4e3a\u7a7a\u6216\u7f3a\u5c11 latestRelease\uff0c\u8df3\u8fc7");
                            UpdateChecker.this.isChecking = false;
                            if (callback != null) {
                                UpdateChecker.this.handler.post(() -> callback.onFinish(true));
                            }
                            return;
                        }
                        if (getBetaVersion && json.latestPrerelease != null) {
                            LauncherVersion betaVersion = json.latestPrerelease;
                            LauncherVersion releaseVersion = json.latestRelease;
                            latest = betaVersion.versionCode > releaseVersion.versionCode ? betaVersion : releaseVersion;
                            isBeta = betaVersion.versionCode > releaseVersion.versionCode;
                        } else {
                            latest = json.latestRelease;
                            isBeta = false;
                        }
                        if (latest.versionCode > UpdateChecker.this.getPackageVersionCode()) {
                            if (!UpdateChecker.this.isIgnore(latest.versionCode)) {
                                UpdateChecker.this.showUpdateDialog(latest, isBeta);
                            }
                            if (callback != null) {
                                UpdateChecker.this.handler.post(() -> callback.onFinish(false));
                            }
                        } else {
                            if (showAlert) {
                                UpdateChecker.this.handler.post(() -> Toast.makeText((Context)UpdateChecker.this.context, (CharSequence)UpdateChecker.this.context.getString(R.string.update_not_exist), (int)0).show());
                            }
                            if (callback != null) {
                                UpdateChecker.this.handler.post(() -> callback.onFinish(true));
                            }
                        }
                        UpdateChecker.this.isChecking = false;
                    }
                    catch (Throwable e) {
                        Log.w((String)"jrelog", (String)"[\u66f4\u65b0] \u68c0\u67e5\u66f4\u65b0\u5931\u8d25\uff08\u5df2\u5ffd\u7565\uff09", (Throwable)e);
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

