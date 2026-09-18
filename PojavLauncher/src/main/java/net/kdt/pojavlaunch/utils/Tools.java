package net.kdt.pojavlaunch.utils;

import android.app.Activity;
import android.content.Context;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import net.kdt.pojavlaunch.Logger;

/* loaded from: classes2.dex */
public final class Tools {
    public static void launchMinecraft(Activity activity, String str, String str2, String str3, Vector<String> vector, String str4, String str5) throws Throwable {
        String[] strArr = new String[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            if (!vector.get(i).equals(" ")) {
                strArr[i] = vector.get(i);
                System.out.println("Minecraft Args:" + strArr[i]);
                Logger.getInstance(activity).appendToLog("Minecraft Args:" + strArr[i]);
            }
        }
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(Arrays.asList(strArr));
        JREUtils.launchJavaVM(activity, str, str2, str3, arrayList, str4, str5);
    }

    public static void getCacioJavaArgs(Context context, List<String> list, boolean z, int i, int i2) {
        if (z) {
            list.add("-Djava.awt.headless=false");
            list.add("-Dcacio.managed.screensize=" + i + "x" + i2);
            list.add("-Dcacio.font.fontscaler=sun.font.FreetypeFontScaler");
            list.add("-Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel");
            if (z) {
                list.add("-Dcacio.font.fontmanager=sun.awt.X11FontManager");
                list.add("-Dawt.toolkit=net.java.openjdk.cacio.ctc.CTCToolkit");
                list.add("-Djava.awt.graphicsenv=net.java.openjdk.cacio.ctc.CTCGraphicsEnvironment");
            } else {
                list.add("-Dcacio.font.fontmanager=com.github.caciocavallosilano.cacio.ctc.CTCFontManager");
                list.add("-Dawt.toolkit=com.github.caciocavallosilano.cacio.ctc.CTCToolkit");
                list.add("-Djava.awt.graphicsenv=com.github.caciocavallosilano.cacio.ctc.CTCGraphicsEnvironment");
                list.add("-Djava.system.class.loader=com.github.caciocavallosilano.cacio.ctc.CTCPreloadClassLoader");
                list.add("--add-exports=java.desktop/java.awt=ALL-UNNAMED");
                list.add("--add-exports=java.desktop/java.awt.peer=ALL-UNNAMED");
                list.add("--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED");
                list.add("--add-exports=java.desktop/sun.java2d=ALL-UNNAMED");
                list.add("--add-exports=java.desktop/java.awt.dnd.peer=ALL-UNNAMED");
                list.add("--add-exports=java.desktop/sun.awt=ALL-UNNAMED");
                list.add("--add-exports=java.desktop/sun.awt.event=ALL-UNNAMED");
                list.add("--add-exports=java.desktop/sun.awt.datatransfer=ALL-UNNAMED");
                list.add("--add-exports=java.desktop/sun.font=ALL-UNNAMED");
                list.add("--add-exports=java.base/sun.security.action=ALL-UNNAMED");
                list.add("--add-opens=java.base/java.util=ALL-UNNAMED");
                list.add("--add-opens=java.desktop/java.awt=ALL-UNNAMED");
                list.add("--add-opens=java.desktop/sun.font=ALL-UNNAMED");
                list.add("--add-opens=java.desktop/sun.java2d=ALL-UNNAMED");
                list.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED");
                list.add("--add-opens=java.base/java.net=ALL-UNNAMED");
            }
            StringBuilder sb = new StringBuilder();
            sb.append("-Xbootclasspath/" + (z ? "p" : "a"));
            File file = new File(context.getDir("runtime", 0).getAbsolutePath() + "/caciocavallo" + (z ? "" : "17"));
            if (file.exists() && file.isDirectory()) {
                for (File file2 : file.listFiles()) {
                    if (file2.getName().endsWith(".jar")) {
                        sb.append(":" + file2.getAbsolutePath());
                    }
                }
            }
            list.add(sb.toString());
        }
    }

    public static String read(InputStream inputStream) throws IOException {
        byte[] bArr = new byte[512];
        String str = "";
        while (true) {
            int read = inputStream.read(bArr);
            if (read == -1) {
                return str;
            }
            str = str + new String(bArr, 0, read);
        }
    }

    public static String read(String str) throws IOException {
        return read(new FileInputStream(str));
    }

    public static void write(String str, byte[] bArr) throws IOException {
        File file = new File(str);
        file.getParentFile().mkdirs();
        file.createNewFile();
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(str));
        bufferedOutputStream.write(bArr, 0, bArr.length);
        bufferedOutputStream.close();
    }

    public static void write(String str, String str2) throws IOException {
        write(str, str2.getBytes());
    }
}
