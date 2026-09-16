package net.kdt.pojavlaunch.utils;

import android.app.Activity;
import android.content.Context;

import net.kdt.pojavlaunch.Logger;

import org.lwjgl.glfw.CallbackBridge;

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

public final class Tools {

    public static void launchMinecraft(final Activity activity,String javaPath,String home,String renderer, Vector<String> args,String gameDir,String glesVersion) throws Throwable {

        String[] launchArgs = new String[args.size()];
        for (int i = 0; i < args.size(); i++) {
            if (!args.get(i).equals(" ")) {
                launchArgs[i] = args.get(i);
                System.out.println("Minecraft Args:" + launchArgs[i]);
                Logger.getInstance(activity).appendToLog("Minecraft Args:" + launchArgs[i]);
            }
        }

        List<String> javaArgList = new ArrayList<String>();

        javaArgList.addAll(Arrays.asList(launchArgs));
        JREUtils.launchJavaVM(activity,javaPath,home,renderer, javaArgList,gameDir,glesVersion);
    }

    /**
     * ★★ 1.1.0 隔离开关：是否启用 cacio17（Java 17+ 的新 AWT 方案）。
     * 由启动器在 launch 前根据「该版本是否需要新栈（1.20.5+）」设置。
     * 默认 false —— 即老版本（含 Java 17 的 1.18~1.20.4）完全走 v1.0.9 行为
     * （Java 17+ 时一个 cacio 参数都不加，与原版一致）。
     */
    public static boolean qclUseCacio17 = false;

    public static void getCacioJavaArgs(Context context, List<String> javaArgList, boolean isJava8, int width, int height) {
        // 1.1.0：启用 caciocavallo（Java 8 → caciocavallo；Java 17+ → caciocavallo17）。
        //
        // ⚠️ 原实现整个方法体被外层 `if (isJava8) { ... }` 包住，导致 Java 17/21/25 时
        // **完全不加任何 cacio 配置**（内层的 Java 17+ else 分支是永不执行的死代码，
        // 注释写着 "Disable caciocavallo 17 for now"）。而 FCL 在 Java 17+ 会挂
        // cacio-agent.jar（-javaagent）+ 全套 --add-exports/--add-opens + Xbootclasspath/a ——
        // 这是与 FCL 的最后一块机制差异（javaagent 在 main 之前运行，会影响类加载/初始化顺序，
        // 正是 MC gui.<clinit> → DateTimeFormatter.<clinit> NPE 相关的那条路径）。
        if (isJava8 || qclUseCacio17) {
            javaArgList.add("-Djava.awt.headless=false");
            javaArgList.add("-Dcacio.managed.screensize=" + width + "x" + height);
        }
        if (isJava8) {
            // Caciocavallo config AWT-enabled version
            javaArgList.add("-Dcacio.font.fontmanager=sun.awt.X11FontManager");
            javaArgList.add("-Dcacio.font.fontscaler=sun.font.FreetypeFontScaler");
            javaArgList.add("-Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel");
            javaArgList.add("-Dawt.toolkit=net.java.openjdk.cacio.ctc.CTCToolkit");
            javaArgList.add("-Djava.awt.graphicsenv=net.java.openjdk.cacio.ctc.CTCGraphicsEnvironment");
        } else if (qclUseCacio17) {
            // Java 17+ 且该版本需要新栈（1.20.5+）：与 FCL 的 CacioJavaArgs 对齐
            javaArgList.add("-Dcacio.font.fontmanager=sun.awt.X11FontManager");
            javaArgList.add("-Dcacio.font.fontscaler=sun.font.FreetypeFontScaler");
            javaArgList.add("-Dswing.defaultlaf=javax.swing.plaf.nimbus.NimbusLookAndFeel");
            javaArgList.add("-Dawt.toolkit=com.github.caciocavallosilano.cacio.ctc.CTCToolkit");
            javaArgList.add("-Djava.awt.graphicsenv=com.github.caciocavallosilano.cacio.ctc.CTCGraphicsEnvironment");
            // cacio-agent.jar：premain 挂钩（FCL 同款；agent 在 main 之前运行）
            File cacio17Dir = new File(context.getDir("runtime", 0).getAbsolutePath(), "caciocavallo17");
            File cacioAgent = new File(cacio17Dir, "cacio-agent.jar");
            if (cacioAgent.isFile()) {
                javaArgList.add("-javaagent:" + cacioAgent.getAbsolutePath());
            }
            javaArgList.add("--add-exports=java.desktop/java.awt=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.desktop/java.awt.peer=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.desktop/sun.java2d=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.desktop/java.awt.dnd.peer=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.desktop/sun.awt=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.desktop/sun.awt.event=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.desktop/sun.awt.datatransfer=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.desktop/sun.font=ALL-UNNAMED");
            javaArgList.add("--add-exports=java.base/sun.security.action=ALL-UNNAMED");
            javaArgList.add("--add-opens=java.base/java.util=ALL-UNNAMED");
            javaArgList.add("--add-opens=java.desktop/java.awt=ALL-UNNAMED");
            javaArgList.add("--add-opens=java.desktop/sun.font=ALL-UNNAMED");
            javaArgList.add("--add-opens=java.desktop/sun.java2d=ALL-UNNAMED");
            javaArgList.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED");
            // Opens the java.net package to Arc DNS injector on Java 9+
            javaArgList.add("--add-opens=java.base/java.net=ALL-UNNAMED");
        }

        StringBuilder cacioClasspath = new StringBuilder();
        cacioClasspath.append("-Xbootclasspath/" + (isJava8 ? "p" : "a"));
        File cacioDir = new File(context.getDir("runtime", 0).getAbsolutePath() + "/caciocavallo" + (isJava8 ? "" : "17"));
        if (cacioDir.exists() && cacioDir.isDirectory()) {
            File[] cacioFiles = cacioDir.listFiles();
            if (cacioFiles != null) {
                for (File file : cacioFiles) {
                    if (file.getName().endsWith(".jar")) {
                        cacioClasspath.append(":" + file.getAbsolutePath());
                    }
                }
            }
        }
        javaArgList.add(cacioClasspath.toString());
    }

    public static String read(InputStream is) throws IOException {
        String out = "";
        int len;
        byte[] buf = new byte[512];
        while((len = is.read(buf))!=-1) {
            out += new String(buf,0,len);
        }
        return out;
    }

    public static String read(String path) throws IOException {
        return read(new FileInputStream(path));
    }

    public static void write(String path, byte[] content) throws IOException
    {
        File outPath = new File(path);
        outPath.getParentFile().mkdirs();
        outPath.createNewFile();

        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(path));
        fos.write(content, 0, content.length);
        fos.close();
    }

    public static void write(String path, String content) throws IOException {
        write(path, content.getBytes());
    }

}
