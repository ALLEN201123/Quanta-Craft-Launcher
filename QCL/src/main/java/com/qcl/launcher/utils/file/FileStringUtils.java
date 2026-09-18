package com.qcl.launcher.utils.file;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

/* loaded from: classes2.dex */
public class FileStringUtils {
    public static String getStringFromFile(String str) {
        try {
            FileInputStream fileInputStream = new FileInputStream(str);
            byte[] bArr = new byte[fileInputStream.available()];
            fileInputStream.read(bArr);
            fileInputStream.close();
            return new String(bArr);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public static void writeFile(String str, String str2) {
        try {
            FileUtils.createDirectory(new File(str).getParent());
            FileUtils.createFile(str);
            FileWriter fileWriter = new FileWriter(str);
            fileWriter.write(str2);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("writeFile", e.toString());
        }
    }

    public static String replaceBlank(String str) {
        return str != null ? Pattern.compile("\\s*|\t|\r|\n").matcher(str).replaceAll("") : "";
    }
}
