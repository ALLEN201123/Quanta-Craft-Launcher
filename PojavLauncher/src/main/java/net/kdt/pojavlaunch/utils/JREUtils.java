package net.kdt.pojavlaunch.utils;

import static net.kdt.pojavlaunch.utils.Architecture.ARCH_ARM;
import static net.kdt.pojavlaunch.utils.Architecture.ARCH_ARM64;
import static net.kdt.pojavlaunch.utils.Architecture.ARCH_X86;
import static net.kdt.pojavlaunch.utils.Architecture.ARCH_X86_64;
import static net.kdt.pojavlaunch.utils.Architecture.is64BitsDevice;

import android.app.Activity;
import android.content.Context;
import android.system.ErrnoException;
import android.system.Os;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Surface;

import com.oracle.dalvik.VMLauncher;

import net.kdt.pojavlaunch.Logger;

import org.lwjgl.glfw.CallbackBridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

public class JREUtils {
    private JREUtils() {}

    public static String LD_LIBRARY_PATH;
    private static String nativeLibDir;
    public static Map<String, String> jreReleaseList;

    public static ArrayList<File> locateLibs(File path) {
        ArrayList<File> ret = new ArrayList<>();
        File[] list = path.listFiles();
        if(list != null) {for(File f : list) {
            if(f.isFile() && f.getName().endsWith(".so")) {
                ret.add(f);
            }else if(f.isDirectory()) {
                ret.addAll(locateLibs(f));
            }
        }}
        return ret;
    }

    public static String getJavaArchName() {
        // ⚠️ 必须用「应用实际安装的 ABI」，不能用设备 CPU。
        // MuMu 这类模拟器设备是 x86_64，但 APK 以 armeabi-v7a 转译运行；
        // 按设备选会得到 amd64 → lib/amd64 不存在 → dlopen 顺序全乱 → JVM 起不来（白屏）。
        if (Architecture.getRuntimeArchitecture() == ARCH_ARM) return "aarch32";
        if (Architecture.getRuntimeArchitecture() == ARCH_ARM64) return "aarch64";
        if (Architecture.getRuntimeArchitecture() == ARCH_X86) return "i386";
        return "amd64";
    }

    public static String getJavaLibDir(String javaPath) {
        File archDir = new File(javaPath + "/lib/" + getJavaArchName());
        return javaPath.endsWith("default") || archDir.exists()
                ? archDir.getAbsolutePath()
                : new File(javaPath, "lib").getAbsolutePath();
    }

    public static String getJvmLibDir(String javaPath) {
        File server = new File(javaPath + "/lib/server/libjvm.so");
        File archServer = new File(getJavaLibDir(javaPath) + "/server/libjvm.so");
        if (server.exists()) return server.getParent();
        if (archServer.exists()) return archServer.getParent();
        return getJavaLibDir(javaPath) + "/client";
    }

    public static void initJavaRuntime(String javaPath) {
        String path = getJavaLibDir(javaPath);
        String jliPath = new File(path + "/jli/libjli.so").exists()
                ? path + "/jli/libjli.so"
                : path + "/libjli.so";
        dlopen(nativeLibDir + "/libc++_shared.so");
        dlopen(jliPath);
        dlopen(getJvmLibDir(javaPath) + "/libjvm.so");
        dlopen(path + "/libverify.so");
        dlopen(path + "/libjava.so");
        dlopen(path + "/libnet.so");
        dlopen(path + "/libnio.so");
        dlopen(path + "/libawt.so");
        dlopen(path + "/libawt_headless.so");
        dlopen(path + "/libfreetype.so");
        dlopen(path + "/libfontmanager.so");
        dlopen(path + "/libtinyiconv.so");
        for(File f : locateLibs(new File(javaPath))) {
            dlopen(f.getAbsolutePath());
        }
        dlopen( nativeLibDir + "/libopenal.so");
    }

    public static Map<String, String> readJREReleaseProperties(String javaPath) throws IOException {
        Map<String, String> jreReleaseMap = new ArrayMap<>();
        BufferedReader jreReleaseReader = new BufferedReader(new FileReader(javaPath + "/release"));
        String currLine;
        while ((currLine = jreReleaseReader.readLine()) != null) {
            if (!currLine.isEmpty() || currLine.contains("=")) {
                String[] keyValue = currLine.split("=");
                jreReleaseMap.put(keyValue[0], keyValue[1].replace("\"", ""));
            }
        }
        jreReleaseReader.close();
        return jreReleaseMap;
    }

    public static String jvmLibraryPath;
    public static void redirectAndPrintJRELog(Context context) {
        Log.v("jrelog","Log starts here");
        JREUtils.logToLogger(Logger.getInstance(context));
        Thread t = new Thread(new Runnable(){
            int failTime = 0;
            ProcessBuilder logcatPb;
            @Override
            public void run() {
                try {
                    if (logcatPb == null) {
                        logcatPb = new ProcessBuilder().command("logcat", /* "-G", "1mb", */ "-v", "brief", "-s", "jrelog:I", "LIBGL:I").redirectErrorStream(true);
                    }
                    
                    Log.i("jrelog-logcat","Clearing logcat");
                    new ProcessBuilder().command("logcat", "-c").redirectErrorStream(true).start();
                    Log.i("jrelog-logcat","Starting logcat");
                    Process p = logcatPb.start();

                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = p.getInputStream().read(buf)) != -1) {
                        String currStr = new String(buf, 0, len);
                        Logger.getInstance(context).appendToLog(currStr);
                    }
                    
                    if (p.waitFor() != 0) {
                        Log.e("jrelog-logcat", "Logcat exited with code " + p.exitValue());
                        failTime++;
                        Log.i("jrelog-logcat", (failTime <= 10 ? "Restarting logcat" : "Too many restart fails") + " (attempt " + failTime + "/10");
                        if (failTime <= 10) {
                            run();
                        } else {
                            Logger.getInstance(context).appendToLog("ERROR: Unable to get more log.");
                        }
                        return;
                    }
                } catch (Throwable e) {
                    Log.e("jrelog-logcat", "Exception on logging thread", e);
                    Logger.getInstance(context).appendToLog("Exception on logging thread:\n" + Log.getStackTraceString(e));
                }
            }
        });
        t.start();
        Log.i("jrelog-logcat","Logcat thread started");
    }

    public static void relocateLibPath(final Context ctx , String javaPath) throws IOException {

        String javaLibDir = getJavaLibDir(javaPath);

        nativeLibDir = ctx.getApplicationInfo().nativeLibraryDir;

        String libName = is64BitsDevice() ? "lib64" : "lib";
        StringBuilder ldLibraryPath = new StringBuilder();
        ldLibraryPath.append(
                javaLibDir + "/jli:" +
                        javaLibDir + ":"
        );
        ldLibraryPath.append(
                "/system/" + libName + ":" +
                        "/vendor/" + libName + ":" +
                        "/vendor/" + libName + "/hw:" +
                        nativeLibDir
        );
        LD_LIBRARY_PATH = ldLibraryPath.toString();
    }

    public static void setJavaEnvironment(Activity activity,String javaPath,String home,String renderer,String glesVersion) throws Throwable {
        Map<String, String> envMap = new ArrayMap<>();
        envMap.put("POJAV_NATIVEDIR", activity.getApplicationInfo().nativeLibraryDir);
        envMap.put("JAVA_HOME", javaPath);
        envMap.put("HOME", home);
        envMap.put("TMPDIR", activity.getCacheDir().getAbsolutePath());
        envMap.put("LIBGL_MIPMAP", "3");

        // 1.1.0：新渲染桥（FCL 版 egl_bridge）的 pojavInitOpenGL 会读 FORCE_VSYNC。
        // 缺这个 env 时 getenv 返回 NULL，随后的 strcmp(NULL,"true") 直接 SIGSEGV
        //（真机实测 fault addr 0x45，崩溃于 JVM Main thread）。FCL 恒定设置该值。
        envMap.put("FORCE_VSYNC", "false");

        // On certain GLES drivers, overloading default functions shader hack fails, so disable it
        envMap.put("LIBGL_NOINTOVLHACK", "1");

        //envMap.put("LIBGL_GL", "21");

        //envMap.put("LIBGL_SHRINK","0");

        //envMap.put("LIBGL_USEVBO","0");

        // Fix white color on banner and sheep, since GL4ES 1.1.5
        envMap.put("LIBGL_NORMALIZE", "1");

        envMap.put("LIBGL_ES",glesVersion);
   
        envMap.put("MESA_GLSL_CACHE_DIR", activity.getCacheDir().getAbsolutePath());
        if (renderer != null) {
            envMap.put("MESA_GL_VERSION_OVERRIDE", renderer.equals("opengles3_virgl")?"4.3":"4.6");
            envMap.put("MESA_GLSL_VERSION_OVERRIDE", renderer.equals("opengles3_virgl")?"430":"460");
        }
        envMap.put("force_glsl_extensions_warn", "true");
        envMap.put("allow_higher_compat_version", "true");
        envMap.put("allow_glsl_extension_directive_midshader", "true");
        envMap.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
        envMap.put("VTEST_SOCKET_NAME", activity.getCacheDir().getAbsolutePath() + "/.virgl_test");

        envMap.put("LD_LIBRARY_PATH", LD_LIBRARY_PATH);
        envMap.put("PATH", javaPath + "/bin:" + Os.getenv("PATH"));
        
        envMap.put("REGAL_GL_VENDOR", "Android");
        envMap.put("REGAL_GL_RENDERER", "Regal");
        envMap.put("REGAL_GL_VERSION", "4.5");
        if(renderer != null) {
            if (renderer.equals("opengles2_5") || renderer.equals("opengles3") || renderer.equals("opengles3_vgpu")) {
                renderer = "opengles2";
            }
            // 1.1.0：zink 渲染器（Mesa zink-on-Vulkan → 桌面 GL 4.6）。
            // 1.20.5+/26.x 需要 OpenGL 3.2+，GL4ES 只到 2.1 跑不了；zink 提供完整桌面 GL，
            // 且向下兼容 —— 高版本与低版本通吃。
            // 走新版 ctxbridges 的 gl_bridge（eglBindAPI(EGL_OPENGL_API) 桌面 GL 模式）
            // + kopper-zink 的 Mesa EGL（libEGL_mesa.so）与 zink driver（libzink_dri.so），
            // 两个库已随 APK 的 native 目录打包，egl_loader 经 loader_dlopen 加载。
            if (renderer.equals("zink") || renderer.equals("opengles3_desktopgl_zink_kopper")) {
                envMap.put("LIBGL_ES", "3");
                envMap.put("POJAVEXEC_EGL", "libEGL_mesa.so");
                renderer = "opengles3_desktopgl_zink_kopper";
            }
            envMap.put("POJAV_RENDERER", renderer);
        }
        envMap.put("AWTSTUB_WIDTH", Integer.toString(CallbackBridge.windowWidth > 0 ? CallbackBridge.windowWidth : CallbackBridge.physicalWidth));
        envMap.put("AWTSTUB_HEIGHT", Integer.toString(CallbackBridge.windowHeight > 0 ? CallbackBridge.windowHeight : CallbackBridge.physicalHeight));

        for (Map.Entry<String, String> env : envMap.entrySet()) {
            Logger.getInstance(activity).appendToLog("Added custom env: " + env.getKey() + "=" + env.getValue());
            Os.setenv(env.getKey(), env.getValue(), true);
        }

        jvmLibraryPath = getJvmLibDir(javaPath);
        Log.d("DynamicLoader","Base LD_LIBRARY_PATH: " + LD_LIBRARY_PATH);
        Log.d("DynamicLoader","Internal LD_LIBRARY_PATH: " + jvmLibraryPath + ":" + LD_LIBRARY_PATH);
        setLdLibraryPath(jvmLibraryPath + ":" + LD_LIBRARY_PATH);

        // return ldLibraryPath;
    }
    
    public static int launchJavaVM(final Activity activity,String javaPath,String home,String renderer,final List<String> JVMArgs,String gameDir,String glesVersion) throws Throwable {
        relocateLibPath(activity,javaPath);

        setJavaEnvironment(activity,javaPath,home,renderer,glesVersion);

        loadGraphicsLibrary(renderer);

        List<String> userArgs = new ArrayList<>();

        userArgs.addAll(JVMArgs);
        
        initJavaRuntime(javaPath);
        setupExitTrap(activity);
        chdir(gameDir);
        userArgs.add(0,"java");

        final int exitCode = VMLauncher.launchJVM((String[]) userArgs.toArray(new String[0]));
        Logger.getInstance(activity).appendToLog("Java Exit code: " + exitCode);
        return exitCode;
    }

    public static int launchAPIInstaller(Context context,String javaPath, ArrayList<String> args, String home) {
        try {
            args.remove(0);
            redirectAndPrintJRELog(context);
            relocateLibPath(context,javaPath);
            Os.setenv("HOME", home, true);
            Os.setenv("JAVA_HOME" , javaPath, true);
            jvmLibraryPath = getJvmLibDir(javaPath);
            Log.d("DynamicLoader","Base LD_LIBRARY_PATH: " + LD_LIBRARY_PATH);
            Log.d("DynamicLoader","Internal LD_LIBRARY_PATH: " + jvmLibraryPath + ":" + LD_LIBRARY_PATH);
            setLdLibraryPath(jvmLibraryPath + ":" + LD_LIBRARY_PATH);
            List<String> userArgs = new ArrayList<>(args);
            initJavaRuntime(javaPath);
            setupExitTrap(context);
            chdir(home);
            userArgs.add(0,"java");
            final int exitCode = VMLauncher.launchJVM((String[]) userArgs.toArray(new String[0]));
            Logger.getInstance(context).appendToLog("Java Exit code: " + exitCode);
            return exitCode;
        } catch (ErrnoException | IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static List<String> getJavaArgs(Context context) {
        String[] overridableArguments = new String[]{
                "-Dglfwstub.windowWidth=" + CallbackBridge.windowWidth,
                "-Dglfwstub.windowHeight=" + CallbackBridge.windowHeight,
                "-Dglfwstub.initEgl=false",
                "-Dext.net.resolvPath=" + new File(context.getFilesDir().getParent(),"resolv.conf").getAbsolutePath(),
                "-Dlog4j2.formatMsgNoLookups=true"
        };

        List<String> userArguments = new ArrayList<>();

        //Add all the arguments
        userArguments.addAll(Arrays.asList(overridableArguments));
        return userArguments;
    }

    public static String getGraphicsLibrary(String renderer) {
        String renderLibrary;
        switch (renderer){
            case "opengles2":
            case "opengles2_5":
            case "opengles3_vgpu" :
            case "opengles3":
                renderLibrary = "libgl4es_114.so";
                break;
            case "opengles3_virgl":
            case "vulkan_zink":
                renderLibrary = "libOSMesa_8.so";
                break;
            // 1.1.0：zink（Mesa zink-on-Vulkan，桌面 GL）。LWJGL 3.3.5 之前的版本
            // （QCL 用 3.2.3）只认 glXGetProcAddress —— kopper-zink 提供的 libglxshim.so
            // 会把 glXGetProcAddress 转发到 Mesa EGL 的 eglGetProcAddress，
            // 从而把 GL 调用链引到 libglapi/libzink_dri（Vulkan）。缺了这个就是
            // 「native 走 zink、LWJGL 却加载 GL4ES」的冲突（真机实测 GL Caps: ERR）。
            case "zink":
            case "opengles3_desktopgl_zink_kopper":
                renderLibrary = "libglxshim.so";
                break;
            case "mg":
                renderLibrary = "libMobileGlues.so";
                break;
            default:
                Log.w("RENDER_LIBRARY", "No renderer selected, defaulting to opengles2");
                renderLibrary = "libgl4es_114.so";
                break;
        }
        return renderLibrary;
    }

    /**
     * Open the render library in accordance to the settings.
     * It will fallback if it fails to load the library.
     * @return The name of the loaded library
     */
    public static String loadGraphicsLibrary(String renderer) {
        if(renderer == null) return null;
        String renderLibrary;
        switch (renderer){
            case "opengles2":
            case "opengles2_5":
            case "opengles3_vgpu" :
            case "opengles3":
                renderLibrary = "libgl4es_114.so";
                break;
            case "opengles3_virgl":
            case "vulkan_zink":
                renderLibrary = "libOSMesa_8.so";
                break;
            // 1.1.0：zink 用 glxshim（→ Mesa EGL → libglapi/libzink_dri）
            case "zink":
            case "opengles3_desktopgl_zink_kopper":
                renderLibrary = "libglxshim.so";
                break;
            default:
                Log.w("RENDER_LIBRARY", "No renderer selected, defaulting to opengles2");
                renderLibrary = "libgl4es_114.so";
                break;
        }

        if (!dlopen(renderLibrary) && !dlopen(findInLdLibPath(renderLibrary))) {
            Log.e("RENDER_LIBRARY","Failed to load renderer " + renderLibrary + ". Falling back to GL4ES 1.1.4");
            renderer = "opengles2";
            renderLibrary = "libgl4es_114.so";
            dlopen(nativeLibDir + "/libgl4es_114.so");
        }
        return renderLibrary;
    }

    public static String findInLdLibPath(String libName) {
        if(Os.getenv("LD_LIBRARY_PATH") == null) {
            try {
                if (LD_LIBRARY_PATH != null) {
                    Os.setenv("LD_LIBRARY_PATH", LD_LIBRARY_PATH, true);
                }else{
                    return libName;
                }
            }catch (ErrnoException e) {
                e.printStackTrace();
                return libName;
            }
        }
        for (String libPath : Os.getenv("LD_LIBRARY_PATH").split(":")) {
            File f = new File(libPath, libName);
            if (f.exists() && f.isFile()) {
                return f.getAbsolutePath();
            }
        }
        return libName;
    }

    public static native int chdir(String path);
    public static native void logToLogger(final Logger logger);
    public static native boolean dlopen(String libPath);
    public static native void setLdLibraryPath(String ldLibraryPath);
    public static native void setupBridgeWindow(Surface surface);
    /** 1.1.0：新渲染桥（libpojavexec_new.so）的对应实现，高版本用 */
    public static native void setupBridgeWindowNew(Surface surface);
    public static native void setupExitTrap(Context context);
    // Obtain AWT screen pixels to render on Android SurfaceView
    public static native int[] renderAWTScreenFrame(/* Object canvas, int width, int height */);
    static {
        // ★★★ 1.1.0 双栈隔离（最终方案）：
        // 两套渲染桥**都加载**（so 名不同，符号不冲突），Java 侧按 MC 版本显式调用
        // 对应的 JNI 方法（setupBridgeWindow / setupBridgeWindowNew）。
        // 这样彻底避免"进程级属性 + 静态块只执行一次"导致的串味
        //（先跑高版本再跑老版本时，老版本会错误地用上新桥 → 黑屏）。
        System.loadLibrary("pojavexec");        // v1.0.9 旧桥（老版本用）
        try {
            System.loadLibrary("pojavexec_new"); // FCL 新桥（1.20.5+ 用）
        } catch (Throwable ignored) {
        }
        System.loadLibrary("pojavexec_awt");
        dlopen("libxhook.so");
        System.loadLibrary("istdio");
    }
}
