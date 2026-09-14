package com.tungsten.hmclpe.launcher.launch.check;

import android.os.AsyncTask;

import com.google.gson.Gson;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.MainActivity;
import com.tungsten.hmclpe.launcher.game.Argument;
import com.tungsten.hmclpe.launcher.game.Artifact;
import com.tungsten.hmclpe.launcher.game.RuledArgument;
import com.tungsten.hmclpe.launcher.game.Version;
import com.tungsten.hmclpe.launcher.setting.game.PrivateGameSetting;
import com.tungsten.hmclpe.launcher.setting.game.GameLaunchSetting;
import com.tungsten.hmclpe.manifest.AppManifest;
import com.tungsten.hmclpe.utils.Architecture;
import com.tungsten.hmclpe.utils.file.FileStringUtils;
import com.tungsten.hmclpe.utils.gson.GsonUtils;
import com.tungsten.hmclpe.utils.gson.JsonUtils;
import com.tungsten.hmclpe.utils.platform.Bits;

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
            if (java < 0 || !new File(runtime, "release").isFile()
                    || (java == 25 && Architecture.getDeviceArchitecture() == Architecture.ARCH_X86)
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
