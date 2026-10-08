package net.kdt.pojavlaunch.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
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
import net.kdt.pojavlaunch.BaseMainActivity;
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
        // ★★★ 1.4.9「点退出游戏卡死、退不回启动器主界面」修复。
        //   launchJavaVM 返回 = 游戏 JVM 彻底结束（JLI_Launch 已退出、native args 已 free）。
        //   ★ 关键：**正常退出根本不会触发 PojavCallback.onExit** —— 那个回调只由游戏进程
        //     内部主动调用（JLI 抛异常时才走）。所以以前「正常退出游戏」时这条链是断的：
        //     游戏 Activity 还挂在前台，界面冻在最后一帧，玩家以为启动器死了。
        //     26.2 崩溃那次日志里有 "[游戏退出] exitCode=1"，是因为异常路径碰巧调到了；
        //     正常退出（exitCode=0）日志里根本没有那一行 —— 这正是漏掉的那一半。
        //   这里在 launchJavaVM 返回后统一补一次 onExit，与游戏内部主动退出互不冲突
        //   （PojavCallback.onExit 里的 killProcess 会立刻结束进程，重复调用无害）。
        int exitCode;
        try {
            exitCode = JREUtils.launchJavaVM(activity, str, str2, str3, arrayList, str4, str5);
        } catch (Throwable launchFailure) {
            // launchJavaVM 本身抛异常（如 native 加载失败）：也要把界面收掉，否则同样卡死
            Log.e("jrelog", "launchJavaVM failed: " + launchFailure);
            try {
                Logger.getInstance(activity).appendToLog("[游戏退出] JVM 启动异常: " + launchFailure);
            } catch (Throwable ignoredLog) {
                // empty catch block
            }
            BaseMainActivity.onExit(activity, -1);
            throw launchFailure;
        }
        Log.i("jrelog", "[游戏退出] JVM 已结束 exitCode=" + exitCode + "，回调 onExit");
        // ★★★★★ 1.5.0：游戏**异常结束**时把关键诊断落盘，供启动器弹窗 + 一键复制。
        //   动机（用户原话「他都崩了，我咋复制日志给你？」）：崩溃后游戏窗口已经没了，
        //   日志窗也看不到，玩家没法把日志交出来 ⇒ 只能下次开启动器时才看得到。
        //   这里在**游戏进程退出这一刻**（日志文件还完整）抓一份尾巴存到 crash_diag.txt。
        if (exitCode != 0) {
            try {
                CrashDiag.capture(activity);
            } catch (Throwable ignoredDiag) {
                // 诊断失败绝不影响退出流程
            }
        }
        BaseMainActivity.onExit(activity, exitCode);
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
                File[] fileArr = file.listFiles();
                if (fileArr == null) {
                    fileArr = new File[0];
                }
                // ★ 必须显式排序：-Xbootclasspath/p 是按顺序找类的，先出现的 jar 胜出。
                //   file.listFiles() 的顺序由文件系统决定（实测设备上 cacio-shared 排在 ResConfHack 前面），
                //   于是原版 CacioFileDialogPeer 抢先加载、我们打进 ResConfHack.jar 的补丁形同不存在
                //   （1.4.5 上「保存/读取世界」依旧 NPE 就是这个原因，javap 已逐字节证实）。
                //   规则：ResConfHack.jar 永远排第一，其余按文件名排序，保证启动参数稳定可复现。
                List<File> jarList = new ArrayList<>();
                for (File file2 : fileArr) {
                    if (file2.getName().endsWith(".jar")) {
                        jarList.add(file2);
                    }
                }
                java.util.Collections.sort(jarList, new java.util.Comparator<File>() {
                    @Override
                    public int compare(File a, File b) {
                        boolean pa = "ResConfHack.jar".equals(a.getName());
                        boolean pb = "ResConfHack.jar".equals(b.getName());
                        if (pa != pb) {
                            return pa ? -1 : 1;
                        }
                        return a.getName().compareTo(b.getName());
                    }
                });
                for (File file2 : jarList) {
                    sb.append(":" + file2.getAbsolutePath());
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
