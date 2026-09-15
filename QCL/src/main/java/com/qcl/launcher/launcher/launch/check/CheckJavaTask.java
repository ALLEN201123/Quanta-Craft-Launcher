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
            String settingPath = launchVersion + "/hmclpe.cfg";
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
            if (GameLaunchSetting.requiresSdl(version)) {
                return new Exception(activity.getString(R.string.revival_sdl_unavailable));
            }
            expectedJava = GameLaunchSetting.requiredJava(version);
            String runtimeName = privateGameSetting.javaSetting.autoSelect
                    ? GameLaunchSetting.selectJavaRuntime(expectedJava) : privateGameSetting.javaSetting.name;
            java = GameLaunchSetting.runtimeMajor(runtimeName);
            File runtime = new File(AppManifest.JAVA_DIR, runtimeName == null ? "" : runtimeName);
            // 32 位运行时（aarch32 / i386）**不支持 Java 21 和 25**：
            //  - 这两个版本的 32 位构建不存在（assets 里只有 21-arm64 / 25-arm64 等 64 位包）；
            //  - 即便装上，Java 21+ 的 JVM 在 32 位地址空间里也几乎起不来。
            // 所以只要"实际会用的运行时"是 32 位，就直接拦下来给玩家一个明确的提示，
            // 而不是让他启动后看到黑屏/闪退。
            int runtimeArch = Architecture.getRuntimeArchitecture();
            boolean is32BitRuntime = runtimeArch == Architecture.ARCH_ARM
                    || runtimeArch == Architecture.ARCH_X86;
            if (java >= 21 && is32BitRuntime) {
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
