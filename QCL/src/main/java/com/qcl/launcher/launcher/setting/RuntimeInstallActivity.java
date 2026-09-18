/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.app.Activity
 *  android.content.Context
 *  android.content.Intent
 *  android.content.SharedPreferences
 *  android.content.res.Configuration
 *  android.net.Uri
 *  android.os.Build$VERSION
 *  android.os.Bundle
 *  android.os.Environment
 *  android.os.Handler
 *  android.os.Looper
 *  android.os.SystemClock
 *  android.util.Log
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.widget.Button
 *  android.widget.ImageView
 *  android.widget.ProgressBar
 *  android.widget.TextView
 *  androidx.annotation.Nullable
 *  androidx.appcompat.app.AlertDialog$Builder
 *  androidx.appcompat.app.AppCompatActivity
 *  androidx.core.app.ActivityCompat
 *  androidx.core.content.ContextCompat
 */
package com.qcl.launcher.launcher.setting;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.setting.InitializeSetting;
import com.qcl.launcher.launcher.setting.InstallLauncherFile;
import com.qcl.launcher.launcher.setting.RuntimeUtils;
import com.qcl.launcher.launcher.setting.launcher.LauncherSetting;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.Architecture;
import com.qcl.launcher.utils.LocaleUtils;
import com.qcl.launcher.utils.file.AssetsUtils;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.qcl.launcher.R;
public class RuntimeInstallActivity
extends AppCompatActivity
implements View.OnClickListener {
    private final Map<String, Item> items = new LinkedHashMap<String, Item>();
    private final Map<String, Spec> specs = new LinkedHashMap<String, Spec>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private Button installButton;
    private String arch;
    private boolean installing = false;
    private View prepareLayout;
    public ProgressBar loadingProgress;
    public TextView loadingText;
    public TextView loadingProgressText;
    public LauncherSetting launcherSetting;
    private boolean prepared = false;
    private static final String SP_RUNTIME = "qcl_runtime";
    private static final String KEY_READY = "ready";
    private static final String KEY_APP_VERSION = "app_version";
    private static final String KEY_RUNTIME_VERSION = "runtime_version";

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtils.setLanguage(base));
    }

    @SuppressLint(value={"SetTextI18n"})
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_runtime_install);
        AppManifest.initializeManifest((Context)this);
        this.arch = RuntimeInstallActivity.deviceArchName();
        this.items.put("lwjgl", new Item((ImageView)this.findViewById(R.id.qcl_lwjgl_state), (ProgressBar)this.findViewById(R.id.qcl_lwjgl_progress), (TextView)this.findViewById(R.id.qcl_lwjgl_detail)));
        this.items.put("cacio", new Item((ImageView)this.findViewById(R.id.qcl_cacio_state), (ProgressBar)this.findViewById(R.id.qcl_cacio_progress), (TextView)this.findViewById(R.id.qcl_cacio_detail)));
        this.items.put("cacio17", new Item((ImageView)this.findViewById(R.id.qcl_cacio17_state), (ProgressBar)this.findViewById(R.id.qcl_cacio17_progress), (TextView)this.findViewById(R.id.qcl_cacio17_detail)));
        this.items.put("java8", new Item((ImageView)this.findViewById(R.id.qcl_java8_state), (ProgressBar)this.findViewById(R.id.qcl_java8_progress), (TextView)this.findViewById(R.id.qcl_java8_detail)));
        this.items.put("java17", new Item((ImageView)this.findViewById(R.id.qcl_java17_state), (ProgressBar)this.findViewById(R.id.qcl_java17_progress), (TextView)this.findViewById(R.id.qcl_java17_detail)));
        this.items.put("java21", new Item((ImageView)this.findViewById(R.id.qcl_java21_state), (ProgressBar)this.findViewById(R.id.qcl_java21_progress), (TextView)this.findViewById(R.id.qcl_java21_detail)));
        this.items.put("java25", new Item((ImageView)this.findViewById(R.id.qcl_java25_state), (ProgressBar)this.findViewById(R.id.qcl_java25_progress), (TextView)this.findViewById(R.id.qcl_java25_detail)));
        this.items.put("jna", new Item((ImageView)this.findViewById(R.id.qcl_jna_state), (ProgressBar)this.findViewById(R.id.qcl_jna_progress), (TextView)this.findViewById(R.id.qcl_jna_detail)));
        this.items.put("sdl", new Item((ImageView)this.findViewById(R.id.qcl_sdl_state), (ProgressBar)this.findViewById(R.id.qcl_sdl_progress), (TextView)this.findViewById(R.id.qcl_sdl_detail)));
        this.installButton = (Button)this.findViewById(R.id.qcl_runtime_install);
        this.installButton.setOnClickListener((View.OnClickListener)this);
        this.specs.put("lwjgl", new Spec("lwjgl", "app_runtime/pojav", AppManifest.POJAV_LIB_DIR, false, "lwjgl3"));
        this.specs.put("cacio", new Spec("cacio", "app_runtime/caciocavallo", AppManifest.CACIOCAVALLO_DIR, false, "cacio-shared-1.10-SNAPSHOT.jar"));
        this.specs.put("cacio17", new Spec("cacio17", "app_runtime/caciocavallo17", AppManifest.CACIOCAVALLO17_DIR, false, "cacio-agent.jar"));
        this.specs.put("java8", new Spec("java8", "app_runtime/java/jre8", AppManifest.JAVA_DIR + "/default", true, "bin/java"));
        this.specs.put("java17", new Spec("java17", "app_runtime/java/jre17", AppManifest.JAVA_DIR + "/JRE17", true, "lib/modules"));
        this.specs.put("java21", new Spec("java21", "app_runtime/java/jre21", AppManifest.JAVA_DIR + "/JRE21", true, "lib/modules"));
        this.specs.put("java25", new Spec("java25", "app_runtime/java/jre25", AppManifest.JAVA_DIR + "/JRE25", true, "lib/modules"));
        this.specs.put("jna", new Spec("jna", "app_runtime/lwjgl333/jna", AppManifest.POJAV_LIB_DIR + "/jna", false, RuntimeInstallActivity.deviceAbiDirName()));
        this.specs.put("sdl", new Spec("sdl", "app_runtime/pojav", this.getApplicationInfo().nativeLibraryDir, false, "libSDL3.so"));
        this.prepareLayout = this.findViewById(R.id.qcl_prepare_layout);
        this.loadingProgress = (ProgressBar)this.findViewById(R.id.loading_progress_bar);
        this.loadingText = (TextView)this.findViewById(R.id.loading_text);
        this.loadingProgressText = (TextView)this.findViewById(R.id.loading_progress_text);
        if (this.installButton != null) {
            this.installButton.setEnabled(false);
        }
        this.requestPermission();
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (Environment.isExternalStorageManager()) {
                this.init();
            } else {
                Intent intent = new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");
                intent.setData(Uri.parse((String)("package:" + this.getPackageName())));
                this.startActivityForResult(intent, 1000);
            }
        } else if (ActivityCompat.checkSelfPermission((Context)this, (String)"android.permission.READ_EXTERNAL_STORAGE") == 0 && ContextCompat.checkSelfPermission((Context)this, (String)"android.permission.WRITE_EXTERNAL_STORAGE") == 0) {
            this.init();
        } else {
            ActivityCompat.requestPermissions((Activity)this, (String[])new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"}, (int)1000);
        }
    }

    private boolean isRuntimeReadyCached() {
        try {
            SharedPreferences sp = this.getSharedPreferences(SP_RUNTIME, 0);
            if (!sp.getBoolean(KEY_READY, false)) {
                return false;
            }
            if (sp.getInt(KEY_APP_VERSION, -1) != this.getCurrentAppVersionCode()) {
                return false;
            }
            return sp.getInt(KEY_RUNTIME_VERSION, -1) == this.getCurrentRuntimeVersion();
        }
        catch (Throwable t) {
            return false;
        }
    }

    private void saveRuntimeReadyCache() {
        try {
            this.getSharedPreferences(SP_RUNTIME, 0).edit().putBoolean(KEY_READY, true).putInt(KEY_APP_VERSION, this.getCurrentAppVersionCode()).putInt(KEY_RUNTIME_VERSION, this.getCurrentRuntimeVersion()).apply();
            this.println("[QCL_RUNTIME] \u7f13\u5b58\u5df2\u5199\u5165\uff08ready=true, appVersionCode=" + this.getCurrentAppVersionCode() + ", runtimeVersion=" + this.getCurrentRuntimeVersion() + "\uff09\u2193 \u4e0b\u6b21\u542f\u52a8\u5c06\u76f4\u63a5\u8fdb\u4e3b\u754c\u9762");
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private int getCurrentAppVersionCode() {
        try {
            return this.getPackageManager().getPackageInfo((String)this.getPackageName(), (int)0).versionCode;
        }
        catch (Throwable t) {
            return 0;
        }
    }

    private void println(String msg) {
        System.out.println(msg);
        Log.i((String)"QCL_RUNTIME", (String)msg);
    }

    private int getCurrentRuntimeVersion() {
        try {
            String s = AssetsUtils.readAssetsTxt((Context)this, "app_runtime/version");
            return Integer.parseInt(String.valueOf(s).trim());
        }
        catch (Throwable t) {
            return -1;
        }
    }

    private void init() {
        if (this.prepared) {
            return;
        }
        this.prepared = true;
        // ★ 默认控键布局同步（后台线程）：缓存命中路径会跳过 checkBaseFiles，
        //   必须在这里也执行，否则布局修正到不了已装齐的老用户；进主界面前 join，避免读到半份布局。
        final Thread controlSyncThread = new Thread(() ->
                com.qcl.launcher.launcher.setting.InstallLauncherFile.syncDefaultControl(getApplicationContext()));
        controlSyncThread.start();
        if (this.isRuntimeReadyCached()) {
            try {
                controlSyncThread.join(3000L);
            } catch (InterruptedException ignored) {
            }
            this.println("[QCL_RUNTIME] \u7f13\u5b58\u547d\u4e2d\uff08ready + appVersion + runtimeVersion \u5168\u5bf9\uff09\u2192 \u76f4\u63a5\u8fdb\u4e3b\u754c\u9762");
            this.runOnUiThread(this::enterLauncher);
            return;
        }
        this.println("[QCL_RUNTIME] \u7f13\u5b58\u672a\u547d\u4e2d \u2192 \u8fdb\u5165 8 \u9879\u8fd0\u884c\u73af\u5883\u6821\u9a8c (appVersionCode=" + this.getCurrentAppVersionCode() + ", runtimeVersion=" + this.getCurrentRuntimeVersion() + ", \u5df2\u8bb0\u5f55app=" + this.getSharedPreferences(SP_RUNTIME, 0).getInt(KEY_APP_VERSION, -1) + ", \u5df2\u8bb0\u5f55runtime=" + this.getSharedPreferences(SP_RUNTIME, 0).getInt(KEY_RUNTIME_VERSION, -1) + ", \u5df2\u8bb0\u5f55ready=" + this.getSharedPreferences(SP_RUNTIME, 0).getBoolean(KEY_READY, false) + ")");
        new Thread(() -> {
            this.launcherSetting = InitializeSetting.initializeLauncherSetting();
            this.runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 28) {
                    this.getWindow().getAttributes().layoutInDisplayCutoutMode = this.launcherSetting != null && this.launcherSetting.fullscreen ? 1 : 2;
                }
                this.getWindow().setFlags(256, 256);
            });
            InstallLauncherFile.checkBaseFiles(this);
            this.runOnUiThread(() -> {
                if (this.prepareLayout != null) {
                    this.prepareLayout.setVisibility(8);
                }
                if (this.installButton != null) {
                    this.installButton.setEnabled(true);
                }
            });
            this.checkAll();
            this.main.post(this::refreshDrawables);
            this.main.post(this::enterIfAllReady);
        }).start();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            if (grantResults.length > 0 && grantResults[0] == 0) {
                this.init();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale((Activity)this, (String)"android.permission.WRITE_EXTERNAL_STORAGE")) {
                new AlertDialog.Builder((Context)this).setMessage(R.string.storage_permissions_remind).setPositiveButton((CharSequence)"OK", (dialog1, which) -> ActivityCompat.requestPermissions((Activity)this, (String[])new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, (int)1000)).setNegativeButton((CharSequence)"Cancel", null).create().show();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000 && Build.VERSION.SDK_INT >= 30) {
            if (Environment.isExternalStorageManager()) {
                this.init();
            } else {
                new AlertDialog.Builder((Context)this).setMessage(R.string.storage_permissions_remind).setPositiveButton((CharSequence)"OK", (dialog1, which) -> this.requestPermission()).setNegativeButton((CharSequence)"Cancel", null).create().show();
            }
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleUtils.setLanguage((Context)this);
    }

    public void onBackPressed() {
        if (this.prepared) {
            super.onBackPressed();
        }
    }

    protected void onDestroy() {
        this.worker.shutdownNow();
        super.onDestroy();
    }

    private void checkAll() {
        for (Map.Entry<String, Spec> e : this.specs.entrySet()) {
            Spec spec = e.getValue();
            Item item = this.items.get(e.getKey());
            if (item == null) continue;
            item.installed = this.isInstalled(spec);
        }
    }

    private boolean isInstalled(Spec spec) {
        try {
            if (spec.isJava) {
                if ("java25".equals(spec.key) && "x86".equals(this.arch)) {
                    return true;
                }
                if (!RuntimeUtils.isLatest((Context)this, spec.targetDir, spec.assetDir)) {
                    return false;
                }
                if ("java8".equals(spec.key)) {
                    return new File(spec.targetDir, "bin/java").exists();
                }
                return new File(spec.targetDir, "lib/modules").exists();
            }
            File dir = new File(spec.targetDir);
            if (!dir.isDirectory()) {
                return false;
            }
            File[] children = dir.listFiles();
            if (children == null || children.length == 0) {
                return false;
            }
            return spec.marker == null || new File(dir, spec.marker).exists();
        }
        catch (Throwable t) {
            return false;
        }
    }

    private void refreshDrawables() {
        for (Map.Entry<String, Item> e : this.items.entrySet()) {
            Item item = e.getValue();
            if (item.state == null) continue;
            this.applyStateIcon(item);
            item.state.setVisibility(0);
        }
        if (this.installButton != null) {
            this.installButton.setText(R.string.splash_runtime_install);
        }
    }

    private void enterIfAllReady() {
        for (Item item : this.items.values()) {
            if (item.installed) continue;
            return;
        }
        this.saveRuntimeReadyCache();
        this.enterLauncher();
    }

    private boolean allReady() {
        for (Item item : this.items.values()) {
            if (item.installed) continue;
            return false;
        }
        return true;
    }

    public void onClick(View v) {
        if (v == this.installButton) {
            if (this.installing) {
                return;
            }
            if (!this.isJavaArchSupported()) {
                new AlertDialog.Builder((Context)this).setTitle(R.string.splash_runtime_failed_title).setMessage((CharSequence)this.getString(R.string.splash_runtime_arch_unsupported, new Object[]{this.arch})).setPositiveButton((CharSequence)"OK", null).show();
                return;
            }
            this.installAll();
        }
    }

    private boolean isJavaArchSupported() {
        try {
            String[] javaDirs = new String[]{"jre8", "jre17", "jre21", "jre25"};
            int supported = 0;
            block2: for (String dir : javaDirs) {
                String[] files = this.getAssets().list("app_runtime/java/" + dir);
                if (files == null) continue;
                for (String f : files) {
                    if (!f.equals("bin-" + this.arch + ".tar.xz")) continue;
                    ++supported;
                    continue block2;
                }
            }
            return supported > 0;
        }
        catch (Throwable t) {
            return false;
        }
    }

    private void installAll() {
        this.installing = true;
        this.installButton.setEnabled(false);
        this.installButton.setText(R.string.splash_runtime_installing);
        this.worker.execute(() -> {
            StringBuilder failed = new StringBuilder();
            for (Map.Entry<String, Spec> e : this.specs.entrySet()) {
                Spec spec = e.getValue();
                Item item = this.items.get(e.getKey());
                if (item == null || item.installed || "sdl".equals(spec.key) || "java25".equals(spec.key) && "x86".equals(this.arch)) continue;
                this.main.post(() -> this.beginItem(item));
                try {
                    if (spec.isJava) {
                        RuntimeUtils.installJava((Context)this, spec.targetDir, spec.assetDir, this.arch, this.createListener(item));
                    } else {
                        RuntimeUtils.install((Context)this, spec.targetDir, spec.assetDir, this.createListener(item));
                    }
                    item.installed = true;
                }
                catch (Throwable t) {
                    Log.w((String)"jrelog", (String)("[\u8fd0\u884c\u73af\u5883] \u5b89\u88c5\u5931\u8d25 " + spec.key + " (" + spec.assetDir + " \u2192 " + spec.targetDir + ")"), (Throwable)t);
                    item.installed = false;
                    failed.append("\n\u00b7 ").append(spec.key).append(": ").append(t);
                }
                this.main.post(() -> this.endItem(item));
            }
            this.installing = false;
            String failMsg = failed.length() == 0 ? null : failed.toString();
            this.main.post(() -> {
                this.installButton.setEnabled(true);
                this.installButton.setText(R.string.splash_runtime_install);
                this.checkAll();
                this.refreshDrawables();
                if (failMsg != null) {
                    new AlertDialog.Builder((Context)this).setTitle(R.string.splash_runtime_failed_title).setMessage((CharSequence)failMsg).setPositiveButton((CharSequence)"OK", null).show();
                    return;
                }
                if (this.allReady()) {
                    this.enterLauncher();
                }
            });
        });
    }

    private void beginItem(Item item) {
        if (item.state != null) {
            item.state.setVisibility(8);
        }
        if (item.progress != null) {
            item.progress.setVisibility(0);
        }
    }

    private void endItem(Item item) {
        if (item.progress != null) {
            item.progress.setVisibility(8);
        }
        if (item.state != null) {
            this.applyStateIcon(item);
            item.state.setVisibility(0);
        }
        if (item.detail != null) {
            item.detail.setVisibility(8);
        }
    }

    private void applyStateIcon(Item item) {
        if (item.state == null) {
            return;
        }
        item.state.setImageResource(item.installed ? R.drawable.ic_baseline_done_black : R.drawable.ic_baseline_refresh_black);
        item.state.setColorFilter(item.installed ? -13730510 : -15374912);
    }

    private RuntimeUtils.InstallListener createListener(final Item item) {
        final long[] lastUpdate = new long[]{0L};
        return new RuntimeUtils.InstallListener(){

            @Override
            public void onUpdate(String detail) {
                long now = SystemClock.elapsedRealtime();
                if (now - lastUpdate[0] < 50L) {
                    return;
                }
                lastUpdate[0] = now;
                RuntimeInstallActivity.this.main.post(() -> {
                    if (item.detail != null) {
                        item.detail.setText((CharSequence)detail);
                        item.detail.setVisibility(0);
                    }
                });
            }

            @Override
            public void onStage(String stageKey) {
                String text = "patching".equals(stageKey) ? RuntimeInstallActivity.this.getString(R.string.splash_runtime_patching) : RuntimeInstallActivity.this.getString(R.string.splash_runtime_installing);
                RuntimeInstallActivity.this.main.post(() -> {
                    if (item.detail != null) {
                        item.detail.setText((CharSequence)text);
                        item.detail.setVisibility(0);
                    }
                });
            }

            @Override
            public void onProgress(int percent) {
            }
        };
    }

    private void enterLauncher() {
        Intent intent = new Intent((Context)this, MainActivity.class);
        Bundle bundle = new Bundle();
        if (this.launcherSetting == null) {
            this.launcherSetting = InitializeSetting.initializeLauncherSetting();
        }
        bundle.putBoolean("fullscreen", this.launcherSetting != null && this.launcherSetting.fullscreen);
        intent.putExtras(bundle);
        this.startActivity(intent);
        this.finish();
    }

    private static String deviceArchName() {
        int a = Architecture.getRuntimeArchitecture();
        if (a == Architecture.ARCH_ARM) {
            return "arm";
        }
        if (a == Architecture.ARCH_ARM64) {
            return "arm64";
        }
        if (a == Architecture.ARCH_X86) {
            return "x86";
        }
        return "x86_64";
    }

    private static String deviceAbiDirName() {
        int a = Architecture.getRuntimeArchitecture();
        if (a == Architecture.ARCH_ARM) {
            return "armeabi-v7a";
        }
        if (a == Architecture.ARCH_ARM64) {
            return "arm64-v8a";
        }
        if (a == Architecture.ARCH_X86) {
            return "x86";
        }
        return "x86_64";
    }

    private static final class Item {
        final ImageView state;
        final ProgressBar progress;
        final TextView detail;
        boolean installed;

        Item(ImageView state, ProgressBar progress, TextView detail) {
            this.state = state;
            this.progress = progress;
            this.detail = detail;
        }
    }

    private static final class Spec {
        final String key;
        final String assetDir;
        final String targetDir;
        final boolean isJava;
        final String marker;

        Spec(String key, String assetDir, String targetDir, boolean isJava, String marker) {
            this.key = key;
            this.assetDir = assetDir;
            this.targetDir = targetDir;
            this.isJava = isJava;
            this.marker = marker;
        }
    }
}

