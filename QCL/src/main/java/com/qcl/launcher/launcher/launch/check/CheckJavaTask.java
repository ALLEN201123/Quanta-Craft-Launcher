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
}
