package com.qcl.launcher.utils.file;

import android.app.Activity;
import android.net.Uri;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;

/* loaded from: classes2.dex */
public class FileUtils {
    public static void createDirectory(String str) {
        if (new File(str).exists()) {
            return;
        }
        new File(str).mkdirs();
    }

    public static void createFile(String str) throws IOException {
        if (new File(str).exists()) {
            return;
        }
        new File(str).createNewFile();
    }

    public static boolean rename(String str, String str2) {
        return new File(str).renameTo(new File(str.substring(0, str.lastIndexOf("/") + 1) + str2));
    }

    public static boolean copyDirectory(String str, String str2) {
        File file = new File(str);
        File file2 = new File(str2);
        if (!file.isDirectory()) {
            return false;
        }
        if (!file2.isDirectory() && !file2.mkdirs()) {
            return false;
        }
        for (File file3 : file.listFiles()) {
            File file4 = new File(file2, file3.getName());
            if (file3.isFile()) {
                if (!copyFile(file3.getAbsolutePath(), file4.getAbsolutePath())) {
                    return false;
                }
            } else if (file3.isDirectory() && !copyDirectory(file3.getAbsolutePath(), file4.getAbsolutePath())) {
                return false;
            }
        }
        return true;
    }

    public static boolean copyFile(String str, String str2) {
        File file = new File(str);
        File file2 = new File(str2);
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file2));
            byte[] bArr = new byte[1024];
            while (true) {
                int read = bufferedInputStream.read(bArr);
                if (read != -1) {
                    bufferedOutputStream.write(bArr, 0, read);
                } else {
                    bufferedOutputStream.flush();
                    bufferedOutputStream.close();
                    bufferedInputStream.close();
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void copyFileWithUri(Uri uri, String str, Activity activity) throws IOException {
        InputStream openInputStream = activity.getContentResolver().openInputStream(uri);
        FileOutputStream fileOutputStream = new FileOutputStream(new File(str));
        byte[] bArr = new byte[1024];
        while (true) {
            int read = openInputStream.read(bArr);
            if (read == -1) {
                return;
            } else {
                fileOutputStream.write(bArr, 0, read);
            }
        }
    }

    public static boolean deleteDirectory(String str) {
        try {
            File file = new File(str);
            if (!file.exists()) {
                return true;
            }
            if (file.isFile()) {
                file.delete();
                return true;
            }
            File[] listFiles = file.listFiles();
            if (listFiles == null) {
                return false;
            }
            for (File file2 : listFiles) {
                deleteDirectory(file2.toString());
            }
            file.delete();
            return true;
        } catch (Exception unused) {
            return false;
        }
    }

    public static ArrayList<File> getAllFiles(String str) {
        ArrayList<File> arrayList = new ArrayList<>();
        File file = new File(str);
        if (!file.exists()) {
            return arrayList;
        }
        if (file.isFile()) {
            arrayList.add(file);
            return arrayList;
        }
        File[] listFiles = file.listFiles();
        if (listFiles == null) {
            return arrayList;
        }
        for (File file2 : listFiles) {
            arrayList.addAll(getAllFiles(file2.toString()));
        }
        return arrayList;
    }

    public static String getFileSha1(String str) {
        try {
            FileInputStream fileInputStream = new FileInputStream(new File(str));
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            byte[] bArr = new byte[1024];
            while (true) {
                int read = fileInputStream.read(bArr);
                if (read > 0) {
                    messageDigest.update(bArr, 0, read);
                } else {
                    return bytesToHex(messageDigest.digest()).toLowerCase();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String bytesToHex(byte[] bArr) {
        StringBuffer stringBuffer = new StringBuffer();
        if (bArr != null && bArr.length > 0) {
            for (byte b : bArr) {
                stringBuffer.append(byteToHex(b));
            }
        }
        return stringBuffer.toString();
    }

    private static String byteToHex(byte b) {
        String hexString = Integer.toHexString(b & 255);
        if (hexString.length() < 2) {
            hexString = String.valueOf(0) + hexString;
        }
        return hexString.toUpperCase();
    }
}
