package com.qcl.launcher.utils;

import android.content.Context;
import com.github.gzuliyujiang.oaid.DeviceIdentifier;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/* loaded from: classes2.dex */
public final class DigestUtils {
    private static final int STREAM_BUFFER_LENGTH = 1024;

    private DigestUtils() {
    }

    public static MessageDigest getDigest(String str) {
        try {
            return MessageDigest.getInstance(str);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static byte[] digest(String str, String str2) {
        return digest(str, str2.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] digest(String str, byte[] bArr) {
        return getDigest(str).digest(bArr);
    }

    public static byte[] digest(String str, Path path) throws IOException {
        InputStream newInputStream = Files.newInputStream(path, new OpenOption[0]);
        try {
            byte[] digest = digest(str, newInputStream);
            if (newInputStream != null) {
                newInputStream.close();
            }
            return digest;
        } catch (Throwable th) {
            if (newInputStream != null) {
                try {
                    newInputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public static byte[] digest(String str, InputStream inputStream) throws IOException {
        return digest(getDigest(str), inputStream);
    }

    public static byte[] digest(MessageDigest messageDigest, InputStream inputStream) throws IOException {
        return updateDigest(messageDigest, inputStream).digest();
    }

    public static MessageDigest updateDigest(MessageDigest messageDigest, InputStream inputStream) throws IOException {
        byte[] bArr = new byte[1024];
        int read = inputStream.read(bArr, 0, 1024);
        while (read > -1) {
            messageDigest.update(bArr, 0, read);
            read = inputStream.read(bArr, 0, 1024);
        }
        return messageDigest;
    }

    public static String encryptToMD5(String str) {
        try {
            return bytesToHex(MessageDigest.getInstance("MD5").digest(str.getBytes("utf-8")));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String bytesToHex(byte[] bArr) {
        StringBuffer stringBuffer = new StringBuffer();
        if (bArr != null && bArr.length > 0) {
            for (byte b : bArr) {
                stringBuffer.append(byteToHex(b));
            }
        }
        return stringBuffer.toString();
    }

    public static String byteToHex(byte b) {
        String hexString = Integer.toHexString(b & 255);
        if (hexString.length() < 2) {
            hexString = String.valueOf(0) + hexString;
        }
        return hexString.toUpperCase();
    }

    public static String getDeviceCode(Context context) {
        return encryptToMD5(DeviceIdentifier.getOAID(context) + DeviceIdentifier.getAndroidID(context) + DeviceIdentifier.getWidevineID() + DeviceIdentifier.getPseudoID());
    }
}
