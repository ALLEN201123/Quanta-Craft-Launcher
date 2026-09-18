package com.qcl.launcher.launcher.launch;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.offline.LoadedSkin;
import com.qcl.launcher.auth.offline.OfflineSkinSetting;
import com.qcl.launcher.auth.offline.SkinJson;
import com.qcl.launcher.auth.offline.Texture;
import com.qcl.launcher.auth.offline.YggdrasilServer;
import com.qcl.launcher.auth.yggdrasil.TextureModel;
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
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class AccountPatch {
    public static boolean isAuthlibInjectorUsable(String str) {
        File file = new File(str);
        if (!file.isFile() || file.length() == 0) {
            Log.e("AccountPatch", "authlib-injector.jar missing or empty: " + str);
            return false;
        }
        try {
            JarFile jarFile = new JarFile(file);
            try {
                Manifest manifest = jarFile.getManifest();
                if (manifest == null) {
                    Log.e("AccountPatch", "authlib-injector.jar has no manifest: " + str);
                    jarFile.close();
                    return false;
                }
                Attributes mainAttributes = manifest.getMainAttributes();
                String value = mainAttributes.getValue("Implementation-Title");
                if (!"authlib-injector".equals(value)) {
                    Log.e("AccountPatch", "bad Implementation-Title: " + value);
                    jarFile.close();
                    return false;
                }
                String value2 = mainAttributes.getValue("Build-Number");
                if (value2 == null) {
                    Log.e("AccountPatch", "missing Build-Number");
                    jarFile.close();
                    return false;
                }
                Integer.parseInt(value2);
                String value3 = mainAttributes.getValue("Premain-Class");
                if (value3 != null && !value3.isEmpty()) {
                    Log.i("AccountPatch", "authlib-injector OK: version=" + mainAttributes.getValue("Implementation-Version") + " build=" + value2);
                    jarFile.close();
                    return true;
                }
                Log.e("AccountPatch", "missing Premain-Class");
                jarFile.close();
                return false;
            } finally {
            }
        } catch (Throwable th) {
            Log.e("AccountPatch", "authlib-injector.jar not usable: " + th);
            return false;
        }
    }

    public static String[] getAccountArgs(Context context, Account account) {
        String str = context.getFilesDir().getAbsolutePath() + "/plugin/login/authlib-injector/authlib-injector.jar";
        int i = account.loginType;
        if (i != 1) {
            if (i == 4) {
                if (isAuthlibInjectorUsable(str)) {
                    return new String[]{"-javaagent:" + str + "=" + account.loginServer, "-Dauthlibinjector.side=client"};
                }
                Log.e("AccountPatch", "skip -javaagent (authlib login) to avoid JVM crash");
                return new String[0];
            }
            if (i != 5) {
                return new String[0];
            }
            String str2 = context.getFilesDir().getAbsolutePath() + "/plugin/login/nide8auth/nide8auth.jar";
            if (new File(str2).isFile()) {
                return new String[]{"-javaagent:" + str2 + "=" + account.loginServer.substring(account.loginServer.length() - 33, account.loginServer.length() - 1), "-Dnide8auth.client=true"};
            }
            Log.e("AccountPatch", "nide8auth.jar missing, skip -javaagent");
            return new String[0];
        }
        if (account.offlineSkinSetting == null) {
            return new String[0];
        }
        if (!isAuthlibInjectorUsable(str)) {
            Log.e("AccountPatch", "skip -javaagent (offline skin) to avoid JVM crash");
            return new String[0];
        }
        YggdrasilServer yggdrasilServer = new YggdrasilServer(0);
        try {
            yggdrasilServer.start();
            yggdrasilServer.addCharacter(new YggdrasilServer.Character(UUIDTypeAdapter.fromString(account.auth_uuid), account.auth_player_name, getOfflineSkin(context, account.offlineSkinSetting, account.auth_player_name)));
            return new String[]{"-javaagent:" + str + "=http://localhost:" + yggdrasilServer.getListeningPort(), "-Dauthlibinjector.side=client"};
        } catch (IOException e) {
            Log.e("yggdrasilServer", e.toString());
            return new String[0];
        }
    }

    public static LoadedSkin getOfflineSkin(Context context, OfflineSkinSetting offlineSkinSetting, String str) {
        Bitmap bitmapFromRes;
        String str2;
        Bitmap decodeStream;
        Bitmap decodeStream2;
        AssetManager assets = context.getAssets();
        try {
            InputStream open = assets.open("img/alex.png");
            InputStream open2 = assets.open("img/steve.png");
            int i = offlineSkinSetting.type;
            if (i == 1) {
                try {
                    return new LoadedSkin(TextureModel.STEVE, Texture.loadTexture(open2), null);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            if (i == 2) {
                try {
                    return new LoadedSkin(TextureModel.ALEX, Texture.loadTexture(open), null);
                } catch (IOException e2) {
                    e2.printStackTrace();
                    return null;
                }
            }
            if (i == 3) {
                if (new File(offlineSkinSetting.skinPath).exists()) {
                    bitmapFromRes = (BitmapFactory.decodeFile(offlineSkinSetting.skinPath).getWidth() == 64 && (BitmapFactory.decodeFile(offlineSkinSetting.skinPath).getHeight() == 32 || BitmapFactory.decodeFile(offlineSkinSetting.skinPath).getHeight() == 64)) ? BitmapFactory.decodeFile(offlineSkinSetting.skinPath) : Avatar.getBitmapFromRes(context, R.drawable.skin_alex);
                } else {
                    bitmapFromRes = Avatar.getBitmapFromRes(context, R.drawable.skin_alex);
                }
                Bitmap decodeFile = (new File(offlineSkinSetting.capePath).exists() && BitmapFactory.decodeFile(offlineSkinSetting.capePath).getWidth() == 64 && BitmapFactory.decodeFile(offlineSkinSetting.capePath).getHeight() == 32) ? BitmapFactory.decodeFile(offlineSkinSetting.capePath) : null;
                try {
                    NormalizedSkin normalizedSkin = new NormalizedSkin(bitmapFromRes);
                    return new LoadedSkin(normalizedSkin.isSlim() ? TextureModel.ALEX : TextureModel.STEVE, Texture.loadTexture(bitmap2InputStream(normalizedSkin.isOldFormat() ? normalizedSkin.getNormalizedTexture() : normalizedSkin.getOriginalTexture())), decodeFile == null ? null : Texture.loadTexture(bitmap2InputStream(decodeFile)));
                } catch (InvalidSkinException | IOException e3) {
                    e3.printStackTrace();
                    return null;
                }
            }
            if (i != 4 && i != 5) {
                return null;
            }
            if (offlineSkinSetting.type == 4) {
                str2 = "https://mcskin.littleservice.cn";
            } else {
                boolean startsWith = offlineSkinSetting.server.startsWith("http://");
                str2 = offlineSkinSetting.server;
                if (startsWith) {
                    str2 = str2.replace("http://", "https://");
                }
            }
            try {
                new URL(StringUtils.removeSuffix(str2, "/") + "/" + str + ".json");
                Log.e("cslApi", StringUtils.removeSuffix(str2, "/") + "/" + str + ".json");
                try {
                    SkinJson skinJson = (SkinJson) JsonUtils.GSON.fromJson(NetworkUtils.doGet(NetworkUtils.toURL(StringUtils.removeSuffix(str2, "/") + "/" + str + ".json")), SkinJson.class);
                    if (skinJson != null && skinJson.hasSkin()) {
                        if (skinJson.getHash() == null) {
                            decodeStream = Avatar.getBitmapFromRes(context, R.drawable.skin_alex);
                        } else {
                            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(StringUtils.removeSuffix(str2, "/") + "/textures/" + skinJson.getHash()).openConnection();
                            httpURLConnection.setDoInput(true);
                            httpURLConnection.connect();
                            decodeStream = BitmapFactory.decodeStream(httpURLConnection.getInputStream());
                        }
                        if (skinJson.getCapeHash() == null) {
                            decodeStream2 = null;
                        } else {
                            HttpURLConnection httpURLConnection2 = (HttpURLConnection) new URL(StringUtils.removeSuffix(str2, "/") + "/textures/" + skinJson.getCapeHash()).openConnection();
                            httpURLConnection2.setDoInput(true);
                            httpURLConnection2.connect();
                            decodeStream2 = BitmapFactory.decodeStream(httpURLConnection2.getInputStream());
                        }
                        try {
                            NormalizedSkin normalizedSkin2 = new NormalizedSkin(decodeStream);
                            return new LoadedSkin(normalizedSkin2.isSlim() ? TextureModel.ALEX : TextureModel.STEVE, Texture.loadTexture(bitmap2InputStream(normalizedSkin2.isOldFormat() ? normalizedSkin2.getNormalizedTexture() : normalizedSkin2.getOriginalTexture())), decodeStream2 == null ? null : Texture.loadTexture(bitmap2InputStream(decodeStream2)));
                        } catch (InvalidSkinException | IOException e4) {
                            e4.printStackTrace();
                        }
                    }
                    return null;
                } catch (IOException e5) {
                    e5.printStackTrace();
                    return null;
                }
            } catch (MalformedURLException e6) {
                e6.printStackTrace();
                return null;
            }
        } catch (IOException e7) {
            e7.printStackTrace();
            return null;
        }
    }

    public static InputStream bitmap2InputStream(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }
}
