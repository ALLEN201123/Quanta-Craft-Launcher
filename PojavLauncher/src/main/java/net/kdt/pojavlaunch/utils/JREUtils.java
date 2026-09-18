package net.kdt.pojavlaunch.utils;

import android.app.Activity;
import android.content.Context;
import android.system.ErrnoException;
import android.system.Os;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Surface;
import com.oracle.dalvik.VMLauncher;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.kdt.pojavlaunch.Logger;
import org.lwjgl.glfw.CallbackBridge;

/* loaded from: classes2.dex */
public class JREUtils {
    public static String LD_LIBRARY_PATH = null;
    public static final boolean QCL_DEBUG_INPUT = false;
    public static Map<String, String> jreReleaseList;
    public static String jvmLibraryPath;
    private static String nativeLibDir;

    public static native int chdir(String str);

    public static native boolean dlopen(String str);

    public static native void logToLogger(Logger logger);

    public static native int[] renderAWTScreenFrame();

    public static native void setLdLibraryPath(String str);

    public static native void setupBridgeWindowNew(Surface surface);

    public static native void setupExitTrap(Context context);

    private JREUtils() {
    }

    public static ArrayList<File> locateLibs(File file) {
        ArrayList<File> arrayList = new ArrayList<>();
        File[] listFiles = file.listFiles();
        if (listFiles != null) {
            for (File file2 : listFiles) {
                if (file2.isFile() && file2.getName().endsWith(".so")) {
                    arrayList.add(file2);
                } else if (file2.isDirectory()) {
                    arrayList.addAll(locateLibs(file2));
                }
            }
        }
        return arrayList;
    }

    public static String getJavaArchName() {
        return Architecture.getRuntimeArchitecture() == Architecture.ARCH_ARM ? "aarch32" : Architecture.getRuntimeArchitecture() == Architecture.ARCH_ARM64 ? "aarch64" : Architecture.getRuntimeArchitecture() == Architecture.ARCH_X86 ? "i386" : "amd64";
    }

    public static String getJavaLibDir(String str) {
        File file = new File(str + "/lib/" + getJavaArchName());
        if (str.endsWith("default") || file.exists()) {
            return file.getAbsolutePath();
        }
        return new File(str, "lib").getAbsolutePath();
    }

    public static String getJvmLibDir(String str) {
        File file = new File(str + "/lib/server/libjvm.so");
        File file2 = new File(getJavaLibDir(str) + "/server/libjvm.so");
        return file.exists() ? file.getParent() : file2.exists() ? file2.getParent() : getJavaLibDir(str) + "/client";
    }

    public static void initJavaRuntime(String str) {
        String str2;
        String javaLibDir = getJavaLibDir(str);
        if (new File(javaLibDir + "/jli/libjli.so").exists()) {
            str2 = javaLibDir + "/jli/libjli.so";
        } else {
            str2 = javaLibDir + "/libjli.so";
        }
        dlopen(nativeLibDir + "/libc++_shared.so");
        dlopen(str2);
        dlopen(getJvmLibDir(str) + "/libjvm.so");
        dlopen(javaLibDir + "/libverify.so");
        dlopen(javaLibDir + "/libjava.so");
        dlopen(javaLibDir + "/libnet.so");
        dlopen(javaLibDir + "/libnio.so");
        dlopen(javaLibDir + "/libawt.so");
        dlopen(javaLibDir + "/libawt_headless.so");
        dlopen(javaLibDir + "/libfreetype.so");
        dlopen(javaLibDir + "/libfontmanager.so");
        dlopen(javaLibDir + "/libtinyiconv.so");
        Iterator<File> it = locateLibs(new File(str)).iterator();
        while (it.hasNext()) {
            dlopen(it.next().getAbsolutePath());
        }
        dlopen(nativeLibDir + "/libopenal.so");
    }

    public static Map<String, String> readJREReleaseProperties(String str) throws IOException {
        ArrayMap arrayMap = new ArrayMap();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(str + "/release"));
        while (true) {
            String readLine = bufferedReader.readLine();
            if (readLine != null) {
                if (!readLine.isEmpty() || readLine.contains("=")) {
                    String[] split = readLine.split("=");
                    arrayMap.put(split[0], split[1].replace("\"", ""));
                }
            } else {
                bufferedReader.close();
                return arrayMap;
            }
        }
    }

    public static void redirectAndPrintJRELog(final Context context) {
        Log.v("jrelog", "Log starts here");
        callLogToLoggerSafely(Logger.getInstance(context));
        new Thread(new Runnable() { // from class: net.kdt.pojavlaunch.utils.JREUtils.1
            int failTime = 0;
            ProcessBuilder logcatPb;

            @Override // java.lang.Runnable
            public void run() {
                try {
                    if (this.logcatPb == null) {
                        this.logcatPb = new ProcessBuilder(new String[0]).command("logcat", "-v", "brief", "-s", "jrelog:I", "LIBGL:I").redirectErrorStream(true);
                    }
                    Log.i("jrelog-logcat", "Clearing logcat");
                    new ProcessBuilder(new String[0]).command("logcat", "-c").redirectErrorStream(true).start();
                    Log.i("jrelog-logcat", "Starting logcat");
                    Process start = this.logcatPb.start();
                    byte[] bArr = new byte[1024];
                    while (true) {
                        int read = start.getInputStream().read(bArr);
                        if (read == -1) {
                            break;
                        } else {
                            Logger.getInstance(context).appendToLog(new String(bArr, 0, read));
                        }
                    }
                    if (start.waitFor() != 0) {
                        Log.e("jrelog-logcat", "Logcat exited with code " + start.exitValue());
                        this.failTime++;
                        Log.i("jrelog-logcat", (this.failTime <= 10 ? "Restarting logcat" : "Too many restart fails") + " (attempt " + this.failTime + "/10");
                        if (this.failTime <= 10) {
                            run();
                        } else {
                            Logger.getInstance(context).appendToLog("ERROR: Unable to get more log.");
                        }
                    }
                } catch (Throwable th) {
                    Log.e("jrelog-logcat", "Exception on logging thread", th);
                    Logger.getInstance(context).appendToLog("Exception on logging thread:\n" + Log.getStackTraceString(th));
                }
            }
        }).start();
        Log.i("jrelog-logcat", "Logcat thread started");
    }

    public static void relocateLibPath(Context context, String str) throws IOException {
        String javaLibDir = getJavaLibDir(str);
        nativeLibDir = context.getApplicationInfo().nativeLibraryDir;
        String str2 = Architecture.is64BitsDevice() ? "lib64" : "lib";
        StringBuilder sb = new StringBuilder();
        // 1.1.1 SDL3：把 APK 原生库目录放最前，确保 libstdc++.so（NDK 桩）优先于
        // 系统 /system/lib64 里的 libtcb.so（vivo 把废弃的 libstdc++.so 替换成它，
        // 加载时触发 libc++ iostream 静态初始化崩溃）。
        sb.append(nativeLibDir + ":");
        sb.append(javaLibDir + "/jli:" + javaLibDir + ":");
        sb.append("/system/" + str2 + ":/vendor/" + str2 + ":/vendor/" + str2 + "/hw:/system_ext/" + str2);
        LD_LIBRARY_PATH = sb.toString();
    }

    public static String getAndroidLibDirName() {
        return Architecture.is64BitsDevice() ? "lib64" : "lib";
    }

    public static void setJavaEnvironment(Activity activity, String str, String str2, String str3, String str4) throws Throwable {
        String str5;
        String str6 = str3;
        ArrayMap arrayMap = new ArrayMap();
        arrayMap.put("POJAV_NATIVEDIR", activity.getApplicationInfo().nativeLibraryDir);
        arrayMap.put("JAVA_HOME", str);
        arrayMap.put("HOME", str2);
        arrayMap.put("TMPDIR", activity.getCacheDir().getAbsolutePath());
        arrayMap.put("LIBGL_MIPMAP", "3");
        arrayMap.put("FORCE_VSYNC", "false");
        // ★★★ 1.1.2 音频修复：照搬 FCL（FCLauncher 的 envMap 里有 ALSOFT_DRIVERS=opensl）。
        // 不设这个变量时，OpenAL-soft 在模拟器/部分机型上会落到静音后端（"OpenAL initialized" 成功却没声）。
        // 强制走 OpenSL ES 后端，libopenal.so 已内置该后端（DT_NEEDED/libOpenSLES 在 .so 内）。
        arrayMap.put("ALSOFT_DRIVERS", "opensl");
        arrayMap.put("POJAV_VSYNC_IN_ZINK", "1");
        arrayMap.put("LIBGL_NOINTOVLHACK", "1");
        // ★★★ 1.1.2 性能优化（照搬 FCL addRendererEnvInner）：FCL 在 opengles2 与 ng_gl4es 分支都设
        // LIBGL_NOERROR=1，让 gl4es 跳过 glGetError 错误跟踪开销，提升帧率。QCL 原先只在 ng_gl4es
        // 分支有，opengles2（≤1.16.5 默认渲染器）缺失。mesa/zink 不识别 LIBGL_* 前缀，全局设置无害。
        arrayMap.put("LIBGL_NOERROR", "1");
        arrayMap.put("LIBGL_NORMALIZE", "1");
        arrayMap.put("LIBGL_ES", str4);
        arrayMap.put("MESA_GLSL_CACHE_DIR", activity.getCacheDir().getAbsolutePath());
        if (str6 != null) {
            arrayMap.put("MESA_GL_VERSION_OVERRIDE", str6.equals("opengles3_virgl") ? "4.3" : "4.6");
            arrayMap.put("MESA_GLSL_VERSION_OVERRIDE", str6.equals("opengles3_virgl") ? "430" : "460");
        }
        arrayMap.put("force_glsl_extensions_warn", "true");
        arrayMap.put("allow_higher_compat_version", "true");
        arrayMap.put("allow_glsl_extension_directive_midshader", "true");
        arrayMap.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
        arrayMap.put("VTEST_SOCKET_NAME", activity.getCacheDir().getAbsolutePath() + "/.virgl_test");
        arrayMap.put("LD_LIBRARY_PATH", LD_LIBRARY_PATH);
        arrayMap.put("PATH", str + "/bin:" + Os.getenv("PATH"));
        arrayMap.put("REGAL_GL_VENDOR", "Android");
        arrayMap.put("REGAL_GL_RENDERER", "Regal");
        arrayMap.put("REGAL_GL_VERSION", "4.5");
        // ★★★ 1.1.2：输入调试日志开关交给常量 QCL_DEBUG_INPUT（发布版默认 false，关掉冗余 native 日志）
        arrayMap.put("QCL_DBG_INPUT", QCL_DEBUG_INPUT ? "1" : "0");
        if (str6 != null) {
            if (str6.equals("opengles2_5") || str6.equals("opengles3") || str6.equals("opengles3_vgpu")) {
                str6 = "opengles2";
            }
            String property = System.getProperty("qcl.renderer.picked", null);
            if (property != null) {
                str6 = property;
            }
            if ("ng_gl4es".equals(str6)) {
                arrayMap.remove("MESA_LOADER_DRIVER_OVERRIDE");
                arrayMap.remove("MESA_GL_VERSION_OVERRIDE");
                arrayMap.remove("MESA_GLSL_VERSION_OVERRIDE");
                arrayMap.remove("VTEST_SOCKET_NAME");
                arrayMap.remove("GALLIUM_DRIVER");
                arrayMap.put("LIBGL_USE_MC_COLOR", "1");
                arrayMap.put("DLOPEN", "libspirv-cross-c-shared.so");
                arrayMap.put("LIBGL_GL", "31");
                arrayMap.put("LIBGL_ES", "3");
                arrayMap.put("LIBGL_NORMALIZE", "1");
                arrayMap.put("LIBGL_NOINTOVLHACK", "1");
                arrayMap.put("LIBGL_NOERROR", "1");
                arrayMap.put("POJAV_RENDERER", "opengles3");
                arrayMap.put("POJAVEXEC_EGL", "libEGL.so");
            }
            if (str6.equals("zink") || str6.equals("opengles3_desktopgl_zink_kopper")) {
                arrayMap.put("LIBGL_ES", "3");
                arrayMap.put("POJAVEXEC_EGL", "libEGL_mesa.so");
                str5 = "ng_gl4es";
                str6 = "opengles3_desktopgl_zink_kopper";
            } else {
                str5 = "ng_gl4es";
            }
            if (!str5.equals(str6)) {
                arrayMap.put("POJAV_RENDERER", str6);
            }
        }
        // ★★★ 1.1.1：SDL3 集成（照搬 FCL 的 FCLauncher.addRendererEnv）——给 SDL 明确的 GL / EGL 库路径。
        // 非 SDL 版本不会读这两个变量，所以无副作用。SDL 自己会在需要时加载。
        String sdlGl = str6 == null ? "" : getGraphicsLibrary(str6);
        if (sdlGl != null && !sdlGl.isEmpty()) {
            arrayMap.put("SDL_OPENGL_LIBRARY", sdlGl);
        }
        String sdlEgl = (String) arrayMap.get("POJAVEXEC_EGL");
        if (sdlEgl != null && !sdlEgl.isEmpty() && !sdlEgl.startsWith("/")) {
            // 渲染器自带 EGL；系统 EGL（libEGL.so）交给 SDL 按自身默认解析。
            arrayMap.put("SDL_EGL_LIBRARY", sdlEgl);
        }
        arrayMap.put("AWTSTUB_WIDTH", Integer.toString(CallbackBridge.windowWidth > 0 ? CallbackBridge.windowWidth : CallbackBridge.physicalWidth));
        arrayMap.put("AWTSTUB_HEIGHT", Integer.toString(CallbackBridge.windowHeight > 0 ? CallbackBridge.windowHeight : CallbackBridge.physicalHeight));
        Iterator it = arrayMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Logger.getInstance(activity).appendToLog("Added custom env: " + ((String) entry.getKey()) + "=" + ((String) entry.getValue()));
            Os.setenv((String) entry.getKey(), (String) entry.getValue(), true);
        }
        jvmLibraryPath = getJvmLibDir(str);
        Log.d("DynamicLoader", "Base LD_LIBRARY_PATH: " + LD_LIBRARY_PATH);
        Log.d("DynamicLoader", "Internal LD_LIBRARY_PATH: " + jvmLibraryPath + ":" + LD_LIBRARY_PATH);
        setLdLibraryPath(jvmLibraryPath + ":" + LD_LIBRARY_PATH);
    }

    public static int launchJavaVM(Activity activity, String str, String str2, String str3, List<String> list, String str4, String str5) throws Throwable {
        relocateLibPath(activity, str);
        setJavaEnvironment(activity, str, str2, str3, str5);
        loadGraphicsLibrary(str3);
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(list);
        initJavaRuntime(str);
        setupExitTrap(activity);
        chdir(str4);
        arrayList.add(0, "java");
        int launchJVM = VMLauncher.launchJVM((String[]) arrayList.toArray(new String[0]));
        Logger.getInstance(activity).appendToLog("Java Exit code: " + launchJVM);
        return launchJVM;
    }

    public static int launchAPIInstaller(Context context, String str, ArrayList<String> arrayList, String str2) {
        try {
            arrayList.remove(0);
            redirectAndPrintJRELog(context);
            relocateLibPath(context, str);
            Os.setenv("HOME", str2, true);
            Os.setenv("JAVA_HOME", str, true);
            jvmLibraryPath = getJvmLibDir(str);
            Log.d("DynamicLoader", "Base LD_LIBRARY_PATH: " + LD_LIBRARY_PATH);
            Log.d("DynamicLoader", "Internal LD_LIBRARY_PATH: " + jvmLibraryPath + ":" + LD_LIBRARY_PATH);
            setLdLibraryPath(jvmLibraryPath + ":" + LD_LIBRARY_PATH);
            ArrayList arrayList2 = new ArrayList(arrayList);
            initJavaRuntime(str);
            setupExitTrap(context);
            chdir(str2);
            arrayList2.add(0, "java");
            int launchJVM = VMLauncher.launchJVM((String[]) arrayList2.toArray(new String[0]));
            Logger.getInstance(context).appendToLog("Java Exit code: " + launchJVM);
            return launchJVM;
        } catch (ErrnoException | IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static List<String> getJavaArgs(Context context) {
        String[] strArr = {"-Dglfwstub.windowWidth=" + CallbackBridge.windowWidth, "-Dglfwstub.windowHeight=" + CallbackBridge.windowHeight, "-Dglfwstub.initEgl=false", "-Dext.net.resolvPath=" + new File(context.getFilesDir().getParent(), "resolv.conf").getAbsolutePath(), "-Dlog4j2.formatMsgNoLookups=true"};
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(Arrays.asList(strArr));
        // ★★★ 1.1.2 性能优化（照搬 FCL DefaultLauncher）：让 JVM 用满设备全部核心。
        // 不设这个时 JVM 可能只识别到部分核心，导致渲染/世界生成线程跑不满、卡顿。
        arrayList.add("-XX:ActiveProcessorCount=" + String.valueOf(Runtime.getRuntime().availableProcessors()));
        // FCL 专用：LWJGL 用系统分配器，减少内存碎片（低风险提示，FCL 一贯启用）
        arrayList.add("-Dorg.lwjgl.system.allocator=system");
        return arrayList;
    }

    public static String getGraphicsLibrary(String str) {
        str.hashCode();
        char c = 65535;
        switch (str.hashCode()) {
            case -2113734149:
                if (str.equals("opengles3_virgl")) {
                    c = 0;
                    break;
                }
                break;
            case -1877202435:
                if (str.equals("opengles3_desktopgl_zink_kopper")) {
                    c = 1;
                    break;
                }
                break;
            case -1822630185:
                if (str.equals("ng_gl4es")) {
                    c = 2;
                    break;
                }
                break;
            case -1749180245:
                if (str.equals("opengles2_5")) {
                    c = 3;
                    break;
                }
                break;
            case 3482:
                if (str.equals("mg")) {
                    c = 4;
                    break;
                }
                break;
            case 3738924:
                if (str.equals("zink")) {
                    c = 5;
                    break;
                }
                break;
            case 190643136:
                if (str.equals("vulkan_zink")) {
                    c = 6;
                    break;
                }
                break;
            case 1040191711:
                if (str.equals("opengles3_vgpu")) {
                    c = 7;
                    break;
                }
                break;
            case 1553485365:
                if (str.equals("opengles2")) {
                    c = '\b';
                    break;
                }
                break;
            case 1553485366:
                if (str.equals("opengles3")) {
                    c = '\t';
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
            case 6:
                return "libOSMesa_8.so";
            case 1:
            case 5:
                return "libglxshim.so";
            case 2:
                return "libng_gl4es.so";
            case 3:
            case 7:
            case '\b':
            case '\t':
                return "libgl4es_114.so";
            case 4:
                return "libMobileGlues.so";
            default:
                Log.w("RENDER_LIBRARY", "No renderer selected, defaulting to opengles2");
                return "libgl4es_114.so";
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static String loadGraphicsLibrary(String str) {
        String str2;
        if (str == null) {
            return null;
        }
        str.hashCode();
        char c = 65535;
        switch (str.hashCode()) {
            case -2113734149:
                if (str.equals("opengles3_virgl")) {
                    c = 0;
                    break;
                }
                break;
            case -1877202435:
                if (str.equals("opengles3_desktopgl_zink_kopper")) {
                    c = 1;
                    break;
                }
                break;
            case -1822630185:
                if (str.equals("ng_gl4es")) {
                    c = 2;
                    break;
                }
                break;
            case -1749180245:
                if (str.equals("opengles2_5")) {
                    c = 3;
                    break;
                }
                break;
            case 3482:
                if (str.equals("mg")) {
                    c = 4;
                    break;
                }
                break;
            case 3738924:
                if (str.equals("zink")) {
                    c = 5;
                    break;
                }
                break;
            case 190643136:
                if (str.equals("vulkan_zink")) {
                    c = 6;
                    break;
                }
                break;
            case 1040191711:
                if (str.equals("opengles3_vgpu")) {
                    c = 7;
                    break;
                }
                break;
            case 1553485365:
                if (str.equals("opengles2")) {
                    c = '\b';
                    break;
                }
                break;
            case 1553485366:
                if (str.equals("opengles3")) {
                    c = '\t';
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
            case 6:
                str2 = "libOSMesa_8.so";
                break;
            case 1:
            case 5:
                str2 = "libglxshim.so";
                break;
            case 2:
                str2 = "libng_gl4es.so";
                break;
            case 3:
            case 7:
            case '\b':
            case '\t':
                str2 = "libgl4es_114.so";
                break;
            case 4:
                str2 = "libMobileGlues.so";
                break;
            default:
                Log.w("RENDER_LIBRARY", "No renderer selected, defaulting to opengles2");
                str2 = "libgl4es_114.so";
                break;
        }
        if (dlopen(str2) || dlopen(findInLdLibPath(str2))) {
            return str2;
        }
        Log.e("RENDER_LIBRARY", "Failed to load renderer " + str2 + ". Falling back to GL4ES 1.1.4");
        dlopen(nativeLibDir + "/libgl4es_114.so");
        return "libgl4es_114.so";
    }

    public static String findInLdLibPath(String str) {
        if (Os.getenv("LD_LIBRARY_PATH") == null) {
            try {
                String str2 = LD_LIBRARY_PATH;
                if (str2 == null) {
                    return str;
                }
                Os.setenv("LD_LIBRARY_PATH", str2, true);
            } catch (ErrnoException e) {
                e.printStackTrace();
                return str;
            }
        }
        for (String str3 : Os.getenv("LD_LIBRARY_PATH").split(":")) {
            File file = new File(str3, str);
            if (file.exists() && file.isFile()) {
                return file.getAbsolutePath();
            }
        }
        return str;
    }

    private static void callLogToLoggerSafely(Logger logger) {
        try {
            logToLogger(logger);
        } catch (Throwable th) {
            Log.i("jrelog-logcat", "logToLogger unavailable (" + th.getClass().getSimpleName() + "), falling back to Java stream capture");
            Logger.setStreamCapture(true);
        }
    }

    static {
        System.loadLibrary("pojavexec");
        System.loadLibrary("pojavexec_awt");
        dlopen("libxhook.so");
        System.loadLibrary("istdio");
    }
}
