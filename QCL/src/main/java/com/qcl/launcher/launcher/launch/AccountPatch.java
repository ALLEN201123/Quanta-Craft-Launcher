package com.qcl.launcher.launcher.launch;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.qcl.launcher.R;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.offline.LoadedSkin;
import com.qcl.launcher.auth.offline.OfflineSkinSetting;
import com.qcl.launcher.auth.offline.SkinJson;
import com.qcl.launcher.auth.offline.Texture;
import com.qcl.launcher.auth.offline.YggdrasilServer;
import com.qcl.launcher.auth.yggdrasil.TextureModel;
import com.qcl.launcher.skin.GameCharacter;
import com.qcl.launcher.skin.utils.Avatar;
import com.qcl.launcher.skin.utils.InvalidSkinException;
import com.qcl.launcher.skin.utils.NormalizedSkin;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.gson.UUIDTypeAdapter;
import com.qcl.launcher.utils.io.NetworkUtils;
import com.qcl.launcher.utils.string.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class AccountPatch {

    /**
     * 校验 authlib-injector.jar 是否真的可用。
     *
     * <p>为什么必须校验：`-javaagent` 在 JVM 启动的 premain 阶段加载 jar。
     * 如果这个 jar 读不了（文件缺失 / 结构异常 / 与当前 JVM 不兼容），
     * JVM 会在 premain 直接 FATAL ERROR + SIGABRT，**把整个游戏进程带走**，
     * 玩家看到的就是"点启动后黑屏"（1.20.6 在 Java 21 下就是这么崩的）。
     * 所以宁可退化成"没有自定义皮肤"，也不能把启动参数加错。
     *
     * <p>校验方式参照 FCL（FoldCraftLauncher）的
     * {@code AuthlibInjectorArtifactInfo.from()}：读 Manifest，
     * 确认 Implementation-Title 是 authlib-injector 且 Build-Number 可解析。
     */
    public static boolean isAuthlibInjectorUsable(String path) {
        File jar = new File(path);
        if (!jar.isFile() || jar.length() == 0) {
            Log.e("AccountPatch", "authlib-injector.jar missing or empty: " + path);
            return false;
        }
        try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jar)) {
            java.util.jar.Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                Log.e("AccountPatch", "authlib-injector.jar has no manifest: " + path);
                return false;
            }
            java.util.jar.Attributes attributes = manifest.getMainAttributes();
            String title = attributes.getValue("Implementation-Title");
            if (!"authlib-injector".equals(title)) {
                Log.e("AccountPatch", "bad Implementation-Title: " + title);
                return false;
            }
            String buildNumber = attributes.getValue("Build-Number");
            if (buildNumber == null) {
                Log.e("AccountPatch", "missing Build-Number");
                return false;
            }
            Integer.parseInt(buildNumber);
            String premain = attributes.getValue("Premain-Class");
            if (premain == null || premain.isEmpty()) {
                Log.e("AccountPatch", "missing Premain-Class");
                return false;
            }
            Log.i("AccountPatch", "authlib-injector OK: version="
                    + attributes.getValue("Implementation-Version") + " build=" + buildNumber);
            return true;
        } catch (Throwable e) {
            // 宁可这里失败，也不要让 JVM 在 premain 阶段 SIGABRT
            Log.e("AccountPatch", "authlib-injector.jar not usable: " + e);
            return false;
        }
    }

    public static String[] getAccountArgs(Context context,Account account) {
        String authlibPath = context.getFilesDir().getAbsolutePath() + "/plugin/login/authlib-injector/authlib-injector.jar";
        switch (account.loginType) {
            case 1:
                if (account.offlineSkinSetting == null) {
                    return new String[0];
                }
                else {
                    // jar 不可用就不加 -javaagent：牺牲自定义皮肤，换取游戏能正常启动
                    if (!isAuthlibInjectorUsable(authlibPath)) {
                        Log.e("AccountPatch", "skip -javaagent (offline skin) to avoid JVM crash");
                        return new String[0];
                    }
                    YggdrasilServer server = new YggdrasilServer(0);
                    try {
                        server.start();
                        server.addCharacter(new YggdrasilServer.Character(UUIDTypeAdapter.fromString(account.auth_uuid), account.auth_player_name, getOfflineSkin(context,account.offlineSkinSetting,account.auth_player_name)));
                    } catch (IOException e) {
                        Log.e("yggdrasilServer",e.toString());
                        return new String[0];
                    }
                    return new String[] {
                            "-javaagent:" + authlibPath + "=http://localhost:" + server.getListeningPort(),
                            "-Dauthlibinjector.side=client"
                    };
                }
            case 4:
                if (!isAuthlibInjectorUsable(authlibPath)) {
                    Log.e("AccountPatch", "skip -javaagent (authlib login) to avoid JVM crash");
                    return new String[0];
                }
                return new String[] {
                        "-javaagent:" + authlibPath + "=" + account.loginServer,
                        "-Dauthlibinjector.side=client"
                };
            case 5:
                String nide8authPath = context.getFilesDir().getAbsolutePath() + "/plugin/login/nide8auth/nide8auth.jar";
                if (!new File(nide8authPath).isFile()) {
                    Log.e("AccountPatch", "nide8auth.jar missing, skip -javaagent");
                    return new String[0];
                }
                String serverId = account.loginServer.substring(account.loginServer.length() - 33,account.loginServer.length() - 1);
                return new String[] {
                        "-javaagent:" + nide8authPath + "=" + serverId,
                        "-Dnide8auth.client=true"
                };
            default:
                return new String[0];
        }
    }

    public static LoadedSkin getOfflineSkin(Context context,OfflineSkinSetting skinSetting,String name) {
        AssetManager manager = context.getAssets();
        InputStream alexInputStream;
        InputStream steveInputStream;
        Bitmap skin;
        Bitmap cape;
        try {
            alexInputStream = manager.open("img/alex.png");
            steveInputStream = manager.open("img/steve.png");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        switch (skinSetting.type) {
            case 1:
                try {
                    return new LoadedSkin(TextureModel.STEVE, Texture.loadTexture(steveInputStream),null);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            case 2:
                try {
                    return new LoadedSkin(TextureModel.ALEX, Texture.loadTexture(alexInputStream),null);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            case 3:
                if (new File(skinSetting.skinPath).exists()) {
                    skin = BitmapFactory.decodeFile(skinSetting.skinPath).getWidth() == 64 && (BitmapFactory.decodeFile(skinSetting.skinPath).getHeight() == 32 || BitmapFactory.decodeFile(skinSetting.skinPath).getHeight() == 64) ? BitmapFactory.decodeFile(skinSetting.skinPath) : Avatar.getBitmapFromRes(context, R.drawable.skin_alex);
                }
                else {
                    skin = Avatar.getBitmapFromRes(context,R.drawable.skin_alex);
                }
                if (new File(skinSetting.capePath).exists()) {
                    cape = (BitmapFactory.decodeFile(skinSetting.capePath).getWidth() == 64 && BitmapFactory.decodeFile(skinSetting.capePath).getHeight() == 32) ? BitmapFactory.decodeFile(skinSetting.capePath) : null;
                }
                else {
                    cape = null;
                }
                try {
                    NormalizedSkin normalizedSkin = new NormalizedSkin(skin);
                    return new LoadedSkin(normalizedSkin.isSlim() ? TextureModel.ALEX : TextureModel.STEVE,
                            Texture.loadTexture(bitmap2InputStream(normalizedSkin.isOldFormat() ? normalizedSkin.getNormalizedTexture() : normalizedSkin.getOriginalTexture())),
                            cape == null ? null : Texture.loadTexture(bitmap2InputStream(cape)));
                } catch (InvalidSkinException | IOException e) {
                    e.printStackTrace();
                    return null;
                }
            case 4:
            case 5:
                String cslApi = skinSetting.type == 4 ? "https://mcskin.littleservice.cn" : (skinSetting.server.startsWith("http://") ? skinSetting.server.replace("http://","https://") : skinSetting.server);
                URL u = null;
                try {
                    u = new URL(StringUtils.removeSuffix(cslApi, "/") + "/" + name + ".json");
                    Log.e("cslApi",StringUtils.removeSuffix(cslApi, "/") + "/" + name + ".json");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return null;
                }
                try {
                    String resultText = NetworkUtils.doGet(NetworkUtils.toURL(StringUtils.removeSuffix(cslApi, "/") + "/" + name + ".json"));
                    SkinJson result = JsonUtils.GSON.fromJson(resultText, SkinJson.class);
                    if (result != null && result.hasSkin()) {
                        if (result.getHash() == null) {
                            skin = Avatar.getBitmapFromRes(context,R.drawable.skin_alex);
                        }
                        else {
                            URL url = new URL(StringUtils.removeSuffix(cslApi, "/") + "/textures/" + result.getHash());
                            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                            httpURLConnection.setDoInput(true);
                            httpURLConnection.connect();
                            InputStream inputStream = httpURLConnection.getInputStream();
                            skin = BitmapFactory.decodeStream(inputStream);
                        }
                        if (result.getCapeHash() == null) {
                            cape = null;
                        }
                        else {
                            URL url = new URL(StringUtils.removeSuffix(cslApi, "/") + "/textures/" + result.getCapeHash());
                            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                            httpURLConnection.setDoInput(true);
                            httpURLConnection.connect();
                            InputStream inputStream = httpURLConnection.getInputStream();
                            cape = BitmapFactory.decodeStream(inputStream);
                        }
                        try {
                            NormalizedSkin normalizedSkin = new NormalizedSkin(skin);
                            return new LoadedSkin(normalizedSkin.isSlim() ? TextureModel.ALEX : TextureModel.STEVE,
                                    Texture.loadTexture(bitmap2InputStream(normalizedSkin.isOldFormat() ? normalizedSkin.getNormalizedTexture() : normalizedSkin.getOriginalTexture())),
                                    cape == null ? null : Texture.loadTexture(bitmap2InputStream(cape)));
                        } catch (InvalidSkinException | IOException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                    else {
                        return null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            default:
                return null;
        }
    }

    // 将Bitmap转换成InputStream
    public static InputStream bitmap2InputStream(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        return is;
    }

}
