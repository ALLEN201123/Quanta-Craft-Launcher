package com.qcl.launcher.launcher.launch;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ★★★ 1.4.9：外部渲染器插件解析（照搬 FCL 的 RendererPlugin v1 协议）。
 *
 * <h3>为什么需要</h3>
 * 之前 QCL 只做「找到插件里的 so、把路径给 JREUtils」，<b>没有读插件声明的环境变量</b>。
 * 后果（实测 26.3 + MobileGlues）：
 * <ul>
 *   <li>mg 插件的 {@code pojavEnv} 声明 {@code POJAV_RENDERER=opengles3}，
 *       而 QCL 一直传 {@code POJAV_RENDERER=mg} → mg 拿不到自己声明的 native 协议名
 *       → 26.3 渲染管线直接崩（首帧后黑屏/闪退）。</li>
 *   <li>其余 {@code LIBGL_ES} / {@code MG_COUNT_LAUNCH} / {@code POJAVEXEC_EGL}
 *       也全部缺失，mg 只能退回代码默认值（日志里 {@code ignoreError=0} 等就是默认值，
 *       <b>不是</b>配置里的 {@code 1} —— 因为 so 根本不读 /sdcard/MG/config.json，
 *       那个文件是 mg 应用自己用的）。</li>
 * </ul>
 *
 * <h3>协议（与 FCL 一致）</h3>
 * 插件在 {@code AndroidManifest.xml} 的 meta-data 里声明：
 * <pre>
 *   fclPlugin   = true                      （boolean，标记是渲染器插件）
 *   renderer    = "libX.so:libEGL.so"       （glName:eglName）
 *   des=描述文本
 *   pojavEnv    = "K1=V1:K2=V2"（冒号分隔，注入游戏进程环境）
 *   boatEnv     = "K=V:..."                  （Boat 后端用，QCL 无Boat，解析但不用）
 *   minMCVer / maxMCVer = 版本区间（可缺省）
 * </pre>
 * 实测 MobileGlues 插件的值：
 * <pre>
 *   renderer   = libmobileglues.so:libmobileglues.so
 *   des= MobileGlues (OpenGL 4.0, 1.17+)
 *   pojavEnv   = LIBGL_ES=3:MG_COUNT_LAUNCH=1:POJAV_RENDERER=opengles3:POJAVEXEC_EGL=libEGL.so
 * </pre>
 *
 * <p>★ 顺带支持 v2 协议（{@code fclPlugin_V2}）：值是一个字符串资源 id，
 * 资源内容是 JSON（含 NormalEnv / SelectableEnv / CustomizableEnv / ToggleableEnv 四类）。
 * mg 目前用的是 v1，v2 一并实现以便将来兼容。
 */
public final class RendererPlugin {

    private static final String TAG = "QCL-RendererPlugin";
    private static final String META_PLUGIN = "fclPlugin";
    private static final String META_PLUGIN_V2 = "fclPlugin_V2";

    /** 一个外部渲染器插件的解析结果。 */
    public static final class Plugin {
        public final String packageName;
        public final String label;
        public final String nativeLibraryDir;
        /** glName:eglName 里的 glName（可能以 / 开头，表示需拼 nativeLibraryDir） */
        public final String glName;
        public final String eglName;
        /** 需要注入的环境变量，形如 {@code KEY=VALUE} */
        public final List<String> env;
        public final String minMCVer;
        public final String maxMCVer;

        Plugin(String packageName, String label, String nativeLibraryDir,
               String glName, String eglName, List<String> env,
               String minMCVer, String maxMCVer) {
            this.packageName = packageName;
            this.label = label;
            this.nativeLibraryDir = nativeLibraryDir;
            this.glName = glName;
            this.eglName = eglName;
            this.env = env;
            this.minMCVer = minMCVer;
            this.maxMCVer = maxMCVer;
        }

        /** 该插件要求的 gl 库绝对路径。 */
        public String glLibPath() {
            return resolveLibPath(glName, nativeLibraryDir);
        }

        /** 该插件要求的 egl 库绝对路径（没有则回落到 gl）。 */
        public String eglLibPath() {
            return eglName == null || eglName.isEmpty() ? glLibPath() : resolveLibPath(eglName, nativeLibraryDir);
        }

        /** 取某个 env 的值（取最后一条，兼容重复声明）。 */
        public String envValue(String key) {
            String v = null;
            for (String e : env) {
                int i = e.indexOf('=');
                if (i > 0 && e.substring(0, i).equals(key)) {
                    v = e.substring(i + 1);
                }
            }
            return v;
        }

        public boolean hasEnv() {
            return env != null && !env.isEmpty();
        }

        /**
         * 插件声明的环境变量（JREUtils 通过反射取这个方法）。
         * <p>★ 暴露成 {@code List<String>} 的 {@code KEY=VALUE}，而不是返回内部 List，
         * 是为了让反射调用方不依赖本类的类型签名。
         */
        public List<String> getEnv() {
            return env == null ? Collections.<String>emptyList() : env;
        }
    }

    /**
     * 候选插件包名（Android 11+ 包可见性限制：<b>只有 manifest 里声明过的包</b>
 * 才能被 {@code getInstalledApplications()} 枚举到；QCL 的 {@code <queries>} 已声明这些）。
 * <p>★ 不使用 {@code QUERY_ALL_PACKAGES}：那是要商店审核的敏感权限，
 * 而渲染器插件就那么几个，显式列出更稳、也更容易过审。
 */
    private static final String[] CANDIDATE_PACKAGES = {
            "com.fcl.plugin.mobileglues",
            "com.fcl.plugin.krypton",
            "com.tungsten.fcl.ngg",
            "com.tungsten.fcl.qualcommdr",
            "com.movtery.zalithlauncher.v2",
    };

    private static final Map<String, Plugin> CACHE = new LinkedHashMap<String, Plugin>();
    private static boolean scanned = false;

    private RendererPlugin() {
    }

    /** 清缓存（插件装完/卸载后调用）。 */
    public static synchronized void invalidate() {
        CACHE.clear();
        scanned = false;
    }

    /** 扫描已安装的渲染器插件（懒初始化，结果缓存到进程结束）。 */
    public static synchronized Map<String, Plugin> plugins(Context context) {
        if (scanned) {
            return CACHE;
        }
        scanned = true;
        CACHE.clear();
        try {
            PackageManager pm = context.getPackageManager();
            // ★ 不能只靠 getInstalledApplications()：Android 11+ 包可见性下，
            //   未在 <queries> 声明的应用根本不会出现在结果里（实测返回空 → 插件全找不到）。
            //   所以改成**逐个主动探测候选包**（清单里逐包声明，稳）。
            for (String pkg : CANDIDATE_PACKAGES) {
                try {
                    // ★★★★ 2026-10-09 修正：flags 必须带 GET_META_DATA，否则 ApplicationInfo
                    //   里的 metaData 恒为 null ⇒ parse() 第一步就 return null ⇒
                    //   实测日志一直是「[渲染器插件] id=mg 未匹配；已装={}」（**空集合**），
                    //   插件声明的 env 永远拿不到。
                    //   ★ FCL 走的是 pm.queryIntentActivities(...).activityInfo.applicationInfo
                    //   （那条路径自带 metaData）；这里不必绕，补上 GET_META_DATA 即可。
                    ApplicationInfo ai = pm.getApplicationInfo(pkg,
                            PackageManager.GET_META_DATA);
                    Plugin p = parse(context, pm, ai);
                    if (p != null) {
                        CACHE.put(p.packageName, p);
                        Log.i(TAG, "发现渲染器插件: " + p.packageName
                                + " gl=" + p.glName + " env=" + p.env);
                    }
                } catch (Throwable notInstalled) {
                    // 该插件没装 —— 正常，继续下一个
                    Log.i(TAG, "候选包未安装/解析失败: " + pkg + " (" + notInstalled + ")");
                }
            }
            // 兜底：若上面因厂商 ROM 差异仍漏了，再扫已安装列表（老系统 / 有宽泛 queries 时有效）
            if (CACHE.isEmpty()) {
                try {
                    List<ApplicationInfo> apps = pm.getInstalledApplications(0);
                    for (ApplicationInfo ai : apps) {
                        if (ai == null || ai.packageName == null) {
                            continue;
                        }
                        if (CACHE.containsKey(ai.packageName)) {
                            continue;
                        }
                        Plugin p = parse(context, pm, ai);
                        if (p != null) {
                            CACHE.put(p.packageName, p);
                            Log.i(TAG, "发现渲染器插件(扫描): " + p.packageName
                                    + " gl=" + p.glName + " env=" + p.env);
                        }
                    }
                } catch (Throwable ignoredScan) {
                    // 扫不到就算了
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "扫描渲染器插件失败: " + t);
        }
        return CACHE;
    }

    /**
     * 按 {@code nativeLibraryDir} 或插件包名找插件。
     * QCL 现有的 {@code findInstalledRendererLibDir} 只认目录，这里按包名找（更准）。
     */
    public static Plugin findByNativeDir(Context context, String nativeLibraryDir) {
        if (nativeLibraryDir == null || nativeLibraryDir.isEmpty()) {
            return null;
        }
        for (Plugin p : plugins(context).values()) {
            if (nativeLibraryDir.equals(p.nativeLibraryDir)) {
                return p;
            }
        }
        return null;
    }

    private static Plugin parse(Context context, PackageManager pm, ApplicationInfo ai) {
        Bundle meta;
        try {
            meta = ai.metaData;
        } catch (Throwable t) {
            return null;
        }
        if (meta == null) {
            return null;
        }
        String pkg = ai.packageName;
        // v2 优先（同时声明两者的插件只按 v2 解析，与 FCL 一致）
        if (meta.containsKey(META_PLUGIN_V2)) {
            Plugin p = parseV2(pm, ai, meta);
            if (p != null) {
                return p;
            }
        }
        if (!meta.getBoolean(META_PLUGIN, false)) {
            return null;
        }
        // ---- v1 ----
        String renderer = meta.getString("renderer");
        String des = meta.getString("des");
        String pojavEnv = meta.getString("pojavEnv");
        if (renderer == null || des == null || pojavEnv == null) {
            return null;
        }
        String[] parts = renderer.split(":");
        String gl = parts.length > 0 ? parts[0] : "";
        String egl = parts.length > 1 ? parts[1] : "";
        String label = loadLabel(pm, ai);
        return new Plugin(pkg, label, ai.nativeLibraryDir, gl, egl,
                splitEnv(pojavEnv),
                nullToEmpty(meta.getString("minMCVer")),
                nullToEmpty(meta.getString("maxMCVer")));
    }

    /** v2：meta-data 的值是字符串资源 id，资源内容是 JSON。 */
    private static Plugin parseV2(PackageManager pm, ApplicationInfo ai, Bundle meta) {
        try {
            int resId = meta.getInt(META_PLUGIN_V2, 0);
            if (resId == 0) {
                return null;
            }
            Resources res = pm.getResourcesForApplication(ai);
            String json = res.getString(resId);
            // GsonUtils 没有暴露 getGson()，这里直接 new（v2 配置全是字符串/布尔/数组，无需定制适配器）
            Map<String, Object> root = new com.google.gson.Gson().fromJson(json, Map.class);
            if (root == null) {
                return null;
            }
            String displayName = str(root.get("displayName"));
            String rendererId = str(root.get("rendererId"));
            String glPath = str(root.get("rendererGLPath"));
            String eglPath = str(root.get("rendererEGLPath"));
            List<String> env = new ArrayList<String>();
            Object envObj = root.get("env");
            if (envObj instanceof List) {
                for (Object o : (List<?>) envObj) {
                    if (!(o instanceof Map)) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> em = (Map<String, Object>) o;
                    String key = str(em.get("key"));
                    if (key == null || key.isEmpty()) {
                        continue;
                    }
                    String type = str(em.get("type"));
                    // Gson 默认不给多态字段名，改用 @type 或 key 特征判断
                    Object val = em.get("value");
                    if ("NormalEnv".equals(type) || (type == null && em.containsKey("value"))) {
                        String v = str(val);
                        if (v != null && !v.isEmpty()) {
                            env.add(key + "=" + v);
                        }
                    } else if ("ToggleableEnv".equals(type)) {
                        Boolean tg = bool(em.get("toggle"), true);
                        if (tg && val != null) {
                            env.add(key + "=" + str(val));
                        }
                    } else if ("SelectableEnv".equals(type)) {
                        Object items = em.get("items");
                        String dv = null;
                        if (items instanceof Map) {
                            dv = str(((Map<?, ?>) items).get("defaultValue"));
                        }
                        if (dv != null && !dv.isEmpty()) {
                            env.add(key + "=" + dv);
                        }
                    } else if ("CustomizableEnv".equals(type)) {
                        String dv = str(em.get("defaultValue"));
                        if (dv != null && !dv.isEmpty()) {
                            env.add(key + "=" + dv);
                        }
                    }
                }
            }
            Object dl = root.get("dlopenLibPaths");
            if (dl instanceof List) {
                for (Object o : (List<?>) dl) {
                    String s = str(o);
                    if (s != null && !s.isEmpty()) {
                        env.add("DLOPEN=" + s);
                    }
                }
            }
            if (glPath == null || glPath.isEmpty()) {
                return null;
            }
            return new Plugin(ai.packageName, loadLabel(pm, ai), ai.nativeLibraryDir,
                    stripNativePrefix(glPath), stripNativePrefix(eglPath == null ? "" : eglPath),
                    env,
                    nullToEmpty(str(root.get("minMCVer"))),
                    nullToEmpty(str(root.get("maxMCVer"))));
        } catch (Throwable t) {
            Log.w(TAG, "解析 v2 插件配置失败: " + t);
            return null;
        }
    }

    private static String loadLabel(PackageManager pm, ApplicationInfo ai) {
        try {
            CharSequence c = pm.getApplicationLabel(ai);
            return c == null ? ai.packageName : c.toString();
        } catch (Throwable ignored) {
            return ai.packageName;
        }
    }

    /** v1 的 pojavEnv 是 {@code K1=V1:K2=V2}。 */
    private static List<String> splitEnv(String raw) {
        List<String> out = new ArrayList<String>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        for (String s : raw.split(":")) {
            s = s.trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    /** v2 里 {@code **}{@code /} 前缀表示「拼插件 nativeLibraryDir」，转成 v1 的「/ 开头」语义。 */
    private static String stripNativePrefix(String raw) {
        if (raw != null && raw.startsWith("**|")) {
            return "/" + raw.substring(3);
        }
        return raw;
    }

    /**
     * 库路径解析：
     * <ul>
     *   <li>以 {@code /} 开头 → 拼插件的 nativeLibraryDir（v1 约定）</li>
     *   <li>以 v2 前缀（两个星号加竖线）开头 → 同上（v2 约定）</li>
     *   <li>绝对路径 → 原样</li>
     * </ul>
     */
    private static String resolveLibPath(String name, String nativeDir) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String n = name;
        if (n.startsWith("**|")) {
            n = n.substring(3);
        }
        if (n.startsWith("/")) {
            if (nativeDir == null || nativeDir.isEmpty()) {
                return n;
            }
            return nativeDir + n;
        }
        return n;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static Boolean bool(Object o, boolean def) {
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        if (o instanceof String) {
            String s = (String) o;
            return "true".equalsIgnoreCase(s) ? Boolean.TRUE
                    : ("false".equalsIgnoreCase(s) ? Boolean.FALSE : null);
        }
        return def;
    }

    /**
     * 找出声明了某个 rendererId 的插件。
     * <p>
     * ★ 匹配要够宽：QCL 的内部 id（{@code mg}）与插件 manifest 里的库名
     * （{@code libmobileglues.so}）**字面完全不同**，直接 contains 会查不到。
     * 所以这里做三路匹配：库名包含 id / id 包含库名里的关键词 / 包名包含 id。
     *
     * @param rendererId QCL 内部的渲染器 id（如 {@code mg}）
     */
    public static Plugin findByRendererId(Context context, String rendererId) {
        if (rendererId == null || rendererId.isEmpty()) {
            return null;
        }
        String want = rendererId.toLowerCase();
        Plugin fallback = null;
        for (Plugin p : plugins(context).values()) {
            String gl = p.glName == null ? "" : p.glName.toLowerCase();
            String egl = p.eglName == null ? "" : p.eglName.toLowerCase();
            String pkg = p.packageName == null ? "" : p.packageName.toLowerCase();
            // ① 库名直接等于 id（ng_gl4es 之类）
            if (gl.equals(want) || egl.equals(want)) {
                return p;
            }
            // ② 库名里含 id（mg → libmobileglues.so 不含 mg，故再试包名）
            if (gl.contains(want) || egl.contains(want) || pkg.contains(want)) {
                return p;
            }
            // ③ id 的「别名」形式出现在库名或包名里。
            //    ★ 2026-10-09 修正：原来这里写的是 `want.replace("_", "")`，
            //      注释说「mg → mobileglues」，但删下划线并不会把 mg 变成 mobileglues，
            //      于是 `"libmobileglues.so".contains("mg")` = false、
            //      `"com.fcl.plugin.mobileglues".contains("mg")` = false
            //      ⇒ 日志一直打「[渲染器插件] id=mg 未匹配」（实测 26.2/26.3 均如此）。
            //      实际能不能跑是另一条路径（RendererCompat 找 so）兜住了，所以没致命，
            //      但匹配失败会导致插件声明的 env 拿不到，属于真 bug。
            //    ⇒ 真正实现别名映射：id → 库名/包名里的特征串。
            for (String alias : rendererAliases(want)) {
                if (gl.contains(alias) || egl.contains(alias) || pkg.contains(alias)) {
                    return p;
                }
            }
            // ④ 兜底：包名去掉固定前缀后仍包含 id
            String bare = want.replace("_", "");
            if (bare.length() >= 2 && pkg.replace("com.fcl.plugin.", "").contains(bare)) {
                if (fallback == null) {
                    fallback = p;
                }
            }
        }
        return fallback;
    }

    /**
     * ★ 2026-10-09：渲染器 id → 库名/包名中的**特征串**别名表。
     *
     * <p>QCL 额外支持了 FCL 没有的渲染器（如 {@code mg} = MobileGlues），
     * 而插件只声明 `libmobileglues.so` / `com.fcl.plugin.mobileglues`，
     * 不含短 id，所以必须在这里显式给出别名才能匹配上。
     */
    private static String[] rendererAliases(String id) {
        if (id == null) {
            return new String[0];
        }
        // 外部插件型：id 太短，无法从库名反推，只能显式列别名
        if ("mg".equals(id) || "mobileglues".equals(id)) {
            return new String[]{"mobileglues"};
        }
        if ("virgl".equals(id)) {
            return new String[]{"virgl", "osmesa"};
        }
        if ("freedreno".equals(id)) {
            return new String[]{"freedreno"};
        }
        if ("zink".equals(id)) {
            return new String[]{"zink"};
        }
        if ("vgpu".equals(id)) {
            return new String[]{"vgpu"};
        }
        if ("krypton".equals(id) || "ng_gl4es".equals(id)) {
            return new String[]{"ng_gl4es"};
        }
        return new String[0];
    }

    /** 便于「插件要求注入哪些 env」的自检与日志。 */
    public static List<String> envSummary(Context context) {
        List<String> out = new ArrayList<String>();
        for (Plugin p : plugins(context).values()) {
            out.add(p.packageName + " -> " + p.env);
        }
        return Collections.unmodifiableList(out);
    }
}