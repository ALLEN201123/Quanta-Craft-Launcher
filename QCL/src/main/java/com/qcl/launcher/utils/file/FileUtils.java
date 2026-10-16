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

    /**
     * ★★★★★ 2026-10-16 **健壮地重命名一个「游戏版本」目录**（修「重命名实例后实例消失」）。
     *
     * <p>用户实测（评测视频 33.0s）：「你给你的游戏实例重命名实例会消失」。
     *
     * <p>★ 根因：{@code SettingUtils.getLocalVersionInfo()} 识别一个版本是**硬性条件**：
     * <pre>
     *   if (!new File(dir + "/versions/" + str + "/" + str + ".json").exists()
     *    || !new File(dir + "/versions/" + str + "/" + str + ".jar").exists()) continue;   // 直接跳过
     * </pre>
     * 也就是 **jar / json 的文件名必须与所在文件夹名完全一致**，否则实例不会被列出（= 消失）。
     *
     * <p>而原来的重命名只改「与旧文件夹同名的」jar/json：
     * <pre>
     *   rename(".../versions/" + old + "/" + old + ".jar",  new + ".jar");
     *   rename(".../versions/" + old + "/" + old + ".json", new + ".json");
     *   rename(".../versions/" + old, new);          // 文件夹照改
     * </pre>
     * 整合包装出来的实例，其 jar/json 经常**不叫文件夹名**（例如
     * {@code Pix Android Boost Lite_1.20.1} 目录里放的是 {@code 5.json} / {@code asm-9.9.jar}）。
     * 此时那两行 `rename` 找不到源文件 ⇒ 静默失败，**但文件夹还是被改名了**
     * ⇒ 改名后 jar/json 名 ≠ 文件夹名 ⇒ 实例彻底识别不到 = **消失**。
     *
     * <p>★ 本方法的做法（顺序很关键）：
     * <ol>
     *   <li>**先把目录改名为新名**（保留原目录路径以便回滚）；</li>
     *   <li>再进入**新目录**，把里面的 jar/json 一律改成与新目录同名
     *       —— 不再假设它们原来就叫旧文件夹名；</li>
     *   <li>若目标名已存在则先删掉（避免 renameTo 失败）。</li>
     * </ol>
     *
     * @param versionsDir 版本根目录（如 {@code .../.minecraft/versions}）
     * @param oldName     旧版本名（= 旧文件夹名）
     * @param newName     新版本名
     * @return 目录是否成功改名（jar/json 的改名失败不影响返回值，但会尽量修好）
     */
    public static boolean renameVersionDir(String versionsDir, String oldName, String newName) {
        if (versionsDir == null || oldName == null || newName == null) {
            return false;
        }
        File src = new File(versionsDir, oldName);
        File dst = new File(versionsDir, newName);
        if (!src.isDirectory()) {
            return false;
        }
        if (dst.exists()) {
            return false;   // 已存在同名版本，拒绝覆盖
        }
        // ① 先改目录名
        if (!src.renameTo(dst)) {
            return false;
        }
        // ② 再把新目录里的 jar/json 统一改成与新目录同名（识别条件要求）
        File[] children = dst.listFiles();
        if (children != null) {
            for (File child : children) {
                String n = child.getName();
                boolean isJar = n.toLowerCase().endsWith(".jar");
                boolean isJson = n.toLowerCase().endsWith(".json");
                if (!isJar && !isJson) {
                    continue;
                }
                if (n.equals(newName + (isJar ? ".jar" : ".json"))) {
                    continue;   // 已经正确
                }
                File target = new File(dst, newName + (isJar ? ".jar" : ".json"));
                if (target.exists()) {
                    target.delete();
                }
                child.renameTo(target);
            }
        }
        return true;
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
