package com.qcl.launcher.auth.microsoft;

import android.util.Log;

import com.qcl.launcher.utils.io.HttpMultipartRequest;
import com.qcl.launcher.utils.io.NetworkUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * ★★★ 1.1.6：微软皮肤/披风服务（照搬 FCL 的 MinecraftSkinService）。
 * 端点基于 api.minecraftservices.com，用微软 Minecraft access token（Msa.mcToken）作 Bearer 认证。
 * 参考：https://wiki.vg/Mojang_API
 */
public class MinecraftSkinService {

    private static final String BASE_URL = "https://api.minecraftservices.com/minecraft/profile";
    private static final String TAG = "MinecraftSkinService";

    private MinecraftSkinService() {
    }

    /** 上传新皮肤（multipart：variant + file） */
    public static void uploadSkin(String accessToken, String model, File file) throws IOException {
        HttpURLConnection con = NetworkUtils.createHttpConnection(new URL(BASE_URL + "/skins"));
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", "Bearer " + accessToken);
        con.setDoOutput(true);
        HttpMultipartRequest request = new HttpMultipartRequest(con);
        try {
            request.param("variant", model);
            FileInputStream fis = new FileInputStream(file);
            try {
                request.file("file", file.getName(), "image/png", fis);
            } finally {
                try { fis.close(); } catch (IOException ignored) {}
            }
        } finally {
            request.close();
        }
        int responseCode = con.getResponseCode();
        String response = NetworkUtils.readData(con);
        if (responseCode / 100 != 2) {
            throw new IOException("Failed to upload skin: HTTP " + responseCode + " - " + response);
        }
        Log.i(TAG, "Skin uploaded successfully, model: " + model);
    }

    /** 重置皮肤（恢复默认皮肤） */
    public static void resetSkin(String accessToken) throws IOException {
        HttpURLConnection con = NetworkUtils.createHttpConnection(new URL(BASE_URL + "/skins/active"));
        con.setRequestMethod("DELETE");
        con.setRequestProperty("Authorization", "Bearer " + accessToken);
        int responseCode = con.getResponseCode();
        if (responseCode / 100 != 2) {
            String response = NetworkUtils.readData(con);
            throw new IOException("Failed to reset skin: HTTP " + responseCode + " - " + response);
        }
        Log.i(TAG, "Skin reset successfully");
    }

    /** 激活某个披风 */
    public static void showCape(String accessToken, String capeId) throws IOException {
        byte[] payload = ("{\"capeId\":\"" + capeId + "\"}").getBytes(StandardCharsets.UTF_8);
        HttpURLConnection con = NetworkUtils.createHttpConnection(new URL(BASE_URL + "/capes/active"));
        con.setRequestMethod("PUT");
        con.setDoOutput(true);
        con.setRequestProperty("Authorization", "Bearer " + accessToken);
        con.setRequestProperty("Content-Type", "application/json");
        OutputStream os = con.getOutputStream();
        try {
            os.write(payload);
        } finally {
            os.close();
        }
        int responseCode = con.getResponseCode();
        if (responseCode / 100 != 2) {
            String response = NetworkUtils.readData(con);
            throw new IOException("Failed to show cape: HTTP " + responseCode + " - " + response);
        }
        Log.i(TAG, "Cape activated successfully, capeId: " + capeId);
    }

    /** 隐藏当前披风 */
    public static void hideCape(String accessToken) throws IOException {
        HttpURLConnection con = NetworkUtils.createHttpConnection(new URL(BASE_URL + "/capes/active"));
        con.setRequestMethod("DELETE");
        con.setRequestProperty("Authorization", "Bearer " + accessToken);
        int responseCode = con.getResponseCode();
        if (responseCode / 100 != 2) {
            String response = NetworkUtils.readData(con);
            throw new IOException("Failed to hide cape: HTTP " + responseCode + " - " + response);
        }
        Log.i(TAG, "Cape hidden successfully");
    }
}
