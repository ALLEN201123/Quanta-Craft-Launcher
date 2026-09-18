package com.qcl.launcher.auth.offline;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/* loaded from: classes2.dex */
public class Texture {
    private static final Map<String, Texture> textures = new HashMap();
    private final byte[] data;
    private final String hash;

    public Texture(String str, byte[] bArr) {
        this.hash = (String) Objects.requireNonNull(str);
        this.data = (byte[]) Objects.requireNonNull(bArr);
    }

    public byte[] getData() {
        return this.data;
    }

    public String getHash() {
        return this.hash;
    }

    public InputStream getInputStream() {
        return new ByteArrayInputStream(this.data);
    }

    public int getLength() {
        return this.data.length;
    }

    public static boolean hasTexture(String str) {
        return textures.containsKey(str);
    }

    public static Texture getTexture(String str) {
        return textures.get(str);
    }

    private static String computeTextureHash(Bitmap bitmap) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            byte[] bArr = new byte[4096];
            putInt(bArr, 0, width);
            putInt(bArr, 4, height);
            int i = 8;
            for (int i2 = 0; i2 < width; i2++) {
                for (int i3 = 0; i3 < height; i3++) {
                    putInt(bArr, i, bitmap.getPixel(i2, i3));
                    if (bArr[i + 0] == 0) {
                        bArr[i + 3] = 0;
                        bArr[i + 2] = 0;
                        bArr[i + 1] = 0;
                    }
                    i += 4;
                    if (i == 4096) {
                        messageDigest.update(bArr, 0, 4096);
                        i = 0;
                    }
                }
            }
            if (i > 0) {
                messageDigest.update(bArr, 0, i);
            }
            byte[] digest = messageDigest.digest();
            return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static void putInt(byte[] bArr, int i, int i2) {
        bArr[i + 0] = (byte) ((i2 >> 24) & 255);
        bArr[i + 1] = (byte) ((i2 >> 16) & 255);
        bArr[i + 2] = (byte) ((i2 >> 8) & 255);
        bArr[i + 3] = (byte) ((i2 >> 0) & 255);
    }

    public static Texture loadTexture(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }
        Bitmap decodeStream = BitmapFactory.decodeStream(inputStream);
        if (decodeStream == null) {
            throw new IOException("No image found");
        }
        String computeTextureHash = computeTextureHash(decodeStream);
        Map<String, Texture> map = textures;
        Texture texture = map.get(computeTextureHash);
        if (texture != null) {
            Log.e("yggdrasilServer", "textureAlreadyLoaded!");
            return texture;
        }
        Log.e("yggdrasilServer", "textureLoading...");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        decodeStream.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        Texture texture2 = new Texture(computeTextureHash, byteArrayOutputStream.toByteArray());
        Texture putIfAbsent = map.putIfAbsent(computeTextureHash, texture2);
        return putIfAbsent != null ? putIfAbsent : texture2;
    }

    public static Texture loadTexture(String str) throws IOException {
        if (str == null) {
            return null;
        }
        return loadTexture(new URL(str).openStream());
    }
}
