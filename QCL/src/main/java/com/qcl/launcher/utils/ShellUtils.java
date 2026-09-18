package com.qcl.launcher.utils;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/* loaded from: classes2.dex */
public class ShellUtils {
    public static void doShell(String str) throws Exception {
        Log.e("命令", str);
        Process exec = Runtime.getRuntime().exec(str.split(" "));
        String consumeInputStream = consumeInputStream(exec.getInputStream());
        String consumeInputStream2 = consumeInputStream(exec.getErrorStream());
        if (exec.waitFor() == 0) {
            Log.e("成功", consumeInputStream);
        } else {
            Log.e("失败", consumeInputStream2);
        }
    }

    private static String consumeInputStream(InputStream inputStream) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine != null) {
                    System.out.println(readLine);
                    Log.e("ShellUtils", readLine);
                    sb.append(readLine + "\n");
                } else {
                    return sb.toString();
                }
            }
        } catch (IOException unused) {
            return "";
        }
    }
}
