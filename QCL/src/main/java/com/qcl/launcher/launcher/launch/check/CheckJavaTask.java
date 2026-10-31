package com.qcl.launcher.launcher.launch.check;

import android.os.AsyncTask;

import com.google.gson.Gson;
import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.game.Argument;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.launcher.game.RuledArgument;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.setting.game.GameLaunchSetting;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.Architecture;
import com.qcl.launcher.utils.file.FileStringUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.platform.Bits;

import java.io.File;

public class CheckJavaTask extends AsyncTask<Object,Integer,Exception> {

    private final MainActivity activity;
    private final String launchVersion;
    private final CheckJavaCallback callback;

    public CheckJavaTask (MainActivity activity,String launchVersion,CheckJavaCallback callback) {
        this.activity = activity;
        this.launchVersion = launchVersion;
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        callback.onStart();
    }

    @Override
    protected Exception doInBackground(Object... objects) {
        try {
            PrivateGameSetting privateGameSetting;
            String settingPath = launchVersion + "/qcl.cfg";
            if (new File(settingPath).exists() && GsonUtils.getPrivateGameSettingFromFile(settingPath) != null && (GsonUtils.getPrivateGameSettingFromFile(settingPath).forceEnable || GsonUtils.getPrivateGameSettingFromFile(settingPath).enable)) {
                privateGameSetting = GsonUtils.getPrivateGameSettingFromFile(settingPath);
            }
            else {
                privateGameSetting = activity.privateGameSetting;
            }
            String gameDir;
            if (privateGameSetting.gameDirSetting.type == 0){
                gameDir = activity.launcherSetting.gameFileDirectory;
            }
            else if (privateGameSetting.gameDirSetting.type == 1){
                gameDir = launchVersion;
            }
            else {
                gameDir = privateGameSetting.gameDirSetting.path;
            }
            if (!privateGameSetting.notCheckForge) {
                FileStringUtils.writeFile(gameDir + "/config/splash.properties","enabled=false");
            }
            // ★★★★★ 1.5.8：**26.3 启动 UI 冻死/崩溃的真根因修复**（照搬 FCL 1.3.4.0 + 1.3.4.1）。
            //
            //   背景：NeoForge 12（MC 26.3 用的）**移除了 `-Dfml.earlyprogresswindow`
            //   属性通道** —— FMLConfig 现在**只读 `config/fml.toml`**，所以 QCL 在
            //   PojavLauncher 里加的那个 `-Dfml.earlyprogresswindow=false` **完全失效**。
            //   早期加载屏照样起来，它的 ANGLE/Vulkan 渲染在启动扫描期反复分配 gralloc
            //   缓冲，与启动器 UI 的 present 在 vendor gralloc 锁上互堵
            //   ⇒ **UI 冻死直至 ANR**（这就是「26.3 启动不了」的真根因）。
            //
            //   修法（对齐 FCL）：**直接写 `config/fml.toml` 的 `earlyWindowControl = false`**，
            //   且**只对 26.3+ 写**（更早版本无此冲突，写了会误伤它们的模组加载进度动画
            //   —— 这就是 FCL 1.3.4.1 修的那个问题）。
            //
            //   键置于文件**头部**，不会落入任何 TOML 表作用域；FML 加载后会按 spec
            //   回写补全注释与其余键。
            try {
                String qclVerName = new File(launchVersion).getName();
                if (isFmlEarlyWindowConflict(qclVerName)) {
                    File cfgDir = new File(gameDir, "config");
                    if (!cfgDir.isDirectory()) {
                        cfgDir.mkdirs();
                    }
                    File fmlToml = new File(cfgDir, "fml.toml");
                    StringBuilder sb = new StringBuilder("earlyWindowControl = false\n");
                    if (fmlToml.isFile()) {
                        // 已存在：保留原有内容，但删掉任何旧的 earlyWindowControl 行（防重复键）
                        java.io.BufferedReader br = null;
                        try {
                            br = new java.io.BufferedReader(new java.io.InputStreamReader(
                                    new java.io.FileInputStream(fmlToml), "UTF-8"));
                            String line;
                            while ((line = br.readLine()) != null) {
                                if (!line.trim().startsWith("earlyWindowControl")) {
                                    sb.append(line).append('\n');
                                }
                            }
                        } finally {
                            if (br != null) {
                                try { br.close(); } catch (Throwable ignored) { }
                            }
                        }
                    }
                    FileStringUtils.writeFile(fmlToml.getAbsolutePath(), sb.toString());
                } else {
                    // 26.3 以下：**清掉**残留的 earlyWindowControl 行（防上一版写坏后不恢复）
                    File fmlToml = new File(new File(gameDir, "config"), "fml.toml");
                    if (fmlToml.isFile()) {
                        StringBuilder sb = new StringBuilder();
                        java.io.BufferedReader br = null;
                        boolean changed = false;
                        try {
                            br = new java.io.BufferedReader(new java.io.InputStreamReader(
                                    new java.io.FileInputStream(fmlToml), "UTF-8"));
                            String line;
                            while ((line = br.readLine()) != null) {
                                if (line.trim().startsWith("earlyWindowControl")) {
                                    changed = true;
                                    continue;
                                }
                                sb.append(line).append('\n');
                            }
                        } finally {
                            if (br != null) {
                                try { br.close(); } catch (Throwable ignored) { }
                            }
                        }
                        if (changed) {
                            FileStringUtils.writeFile(fmlToml.getAbsolutePath(), sb.toString());
                        }
                    }
                }
            } catch (Throwable qclFmlToml) {
                // 写不了也不影响启动
            }
            int expectedJava;
            int java;
            String versionJson = FileStringUtils.getStringFromFile(launchVersion + "/" + new File(launchVersion).getName() + ".json");
            Gson gson = JsonUtils.defaultGsonBuilder()
                    .registerTypeAdapter(Artifact.class, new Artifact.Serializer())
                    .registerTypeAdapter(Bits.class, new Bits.Serializer())
                    .registerTypeAdapter(RuledArgument.class, new RuledArgument.Serializer())
                    .registerTypeAdapter(Argument.class, new Argument.Deserializer())
                    .create();
            Version version = gson.fromJson(versionJson, Version.class);
            if (version == null) {
                // Empty / half-written version json: report it instead of crashing. Gson returns
                // null here rather than throwing.
                return new Exception(activity.getString(R.string.launch_check_dialog_exception_lib_failed));
            }
            // ★★★ 1.1.1：SDL3 已由 native hooks（编进 libpojavexec.so）支持，这里不再拦截，继续正常检查。
            expectedJava = GameLaunchSetting.requiredJava(version);
            String runtimeName = privateGameSetting.javaSetting.autoSelect
                    ? GameLaunchSetting.selectJavaRuntime(expectedJava) : privateGameSetting.javaSetting.name;
            java = GameLaunchSetting.runtimeMajor(runtimeName);
            File runtime = new File(AppManifest.JAVA_DIR, runtimeName == null ? "" : runtimeName);
            // ⚠️ QCL 内置的运行时是**按架构**分目录的（`assets/app_runtime/java/21-arm`、`21-x86` …），
            // `InstallLauncherFile.prepareModernJava()` 会按 getRuntimeArchitecture() 拷对应那份。
            // 所以 32 位设备**有**能用的 Java 21（21-arm / 21-x86 都在）。
            // 1.0.8 曾在这里把所有 32 位运行时的 Java 21/25 全拦掉 —— 那是错的，会让 1.20.6 起不来。
            // 目前**唯一**真的缺构建的组合是「Java 25 + 32 位 x86」（没有 assets/app_runtime/java/25-x86）。
            int runtimeArch = Architecture.getRuntimeArchitecture();
            if (java >= 25 && runtimeArch == Architecture.ARCH_X86) {
                return new Exception(activity.getString(R.string.revival_java_unavailable_32bit) + " -- " + runtimeName);
            }
            if (java < 0 || !new File(runtime, "release").isFile()
                    || (java >= 21 && (!new File(runtime, "version").isFile()
                    || new File(runtime, "lib/server/libjvm.so").length() == 0
                    || new File(runtime, "lib/modules").length() == 0
                    || (java == 25 && !new File(activity.getApplicationInfo().nativeLibraryDir, "libc++_shared.so").isFile())))) {
                return new Exception(activity.getString(R.string.revival_java_unavailable) + " -- " + runtimeName);
            }
            // Skipping version compatibility must not bypass an absent or wrong-ABI runtime.
            if (privateGameSetting.notCheckJvm || java == expectedJava
                    || (java == 17 && expectedJava > 8 && expectedJava <= 17)
                    || (java == 21 && expectedJava > 17 && expectedJava <= 21)
                    || (java == 25 && expectedJava > 21 && expectedJava <= 25)) {
                return null;
            }
            else {
                return new Exception(activity.getString(R.string.launch_check_dialog_exception_error_java) + " -- java" + java);
            }
        }
        catch (Exception e) {
            return e;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Exception e) {
        super.onPostExecute(e);
        callback.onFinish(e);
    }

    public interface CheckJavaCallback{
        void onStart();
        void onFinish(Exception e);
    }

    /**
     * ★★★★★ 1.5.8：**这个版本是否需要关闭 FML 早期加载屏**（即 MC ≥ 26.3）。
     *
     * <p>为什么要关：NeoForge 12（26.3 用的）的早期加载屏会在启动扫描期用
     * ANGLE/Vulkan 反复分配 gralloc 缓冲，与启动器 UI 的 present 在 vendor
     * gralloc 锁上互堵 ⇒ **UI 冻死直至 ANR**。这就是「26.3 启动不了」的真根因。
     *
     * <p>为什么不无脑对所有版本关：更早的 Forge 也认这个键，关掉会
     * **误伤它们的模组加载进度动画**（FCL 1.3.4.1 就是这个修复）。
     *
     * <p>版本名可能的形态：
     * <ul>
     *   <li>{@code 26.3} / {@code 26.4-snapshot-3} —— 直接</li>
     *   <li>{@code 26.2fa} / {@code 26.3-forge-1.2.3} —— 带后缀</li>
     *   <li>{@code fabric-loader-0.19.5-26.3} —— 加载器前缀，要取最后一段版本</li>
     *   <li>{@code Fabulously.Optimized-v15.0.0-alpha.3} —— 整合包名，**判不了，一律当需要关**
     *       （因为整合包多为高版本，宁可多关一次也不要冻死）</li>
     * </ul>
     */
    private static boolean isFmlEarlyWindowConflict(String versionId) {
        if (versionId == null) {
            return false;
        }
        String v = versionId.trim();
        if (v.isEmpty()) {
            return false;
        }
        // 取「最后一个像 MC 版本号的片段」：26.3 / 1.21.11 / 26.4
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,2})\\.(\\d{1,2})(?:\\.(\\d{1,2}))?")
                .matcher(v);
        int major = -1;
        int minor = -1;
        int patch = 0;
        while (m.find()) {
            // 一直覆盖到最后一个匹配（加载器前缀里也有版本号，MC 的版本号在最后）
            try {
                major = Integer.parseInt(m.group(1));
                minor = Integer.parseInt(m.group(2));
                patch = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
            } catch (Throwable ignored) {
            }
        }
        if (major < 0) {
            // 完全没有版本号形状（例如纯整合包名）：**保守按需要关**处理
            return true;
        }
        // 26.x 全线需要关（26.1 起就是新版本号体系）
        if (major >= 26) {
            return true;
        }
        return false;
    }
}
