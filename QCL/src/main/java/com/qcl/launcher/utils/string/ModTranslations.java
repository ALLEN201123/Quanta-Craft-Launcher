package com.qcl.launcher.utils.string;

import com.qcl.launcher.launcher.mod.RemoteModRepository;
import com.qcl.launcher.utils.Logging;
import com.qcl.launcher.utils.Pair;
import com.qcl.launcher.utils.io.IOUtils;
import com.qcl.launcher.utils.string.ModTranslations;
import com.qcl.launcher.utils.string.StringUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

/* loaded from: classes2.dex */
public final class ModTranslations {
    private Map<String, Mod> curseForgeMap;
    private List<Pair<String, Mod>> keywords;
    private int maxKeywordLength = -1;
    private Map<String, Mod> modIdMap;
    private List<Mod> mods;
    private final String resourceName;
    public static ModTranslations MOD = new ModTranslations("/assets/mod_data.txt");
    public static ModTranslations MODPACK = new ModTranslations("/assets/modpack_data.txt");
    public static ModTranslations EMPTY = new ModTranslations("");

    public static /* synthetic */ StringBuilder $r8$lambda$z4Sacp0dHgNB96bTlXZdt5SF0q4() {
        return new StringBuilder();
    }

    /* renamed from: com.qcl.launcher.utils.string.ModTranslations$1, reason: invalid class name */
    /* loaded from: classes2.dex */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$Type;

        static {
            int[] iArr = new int[RemoteModRepository.Type.values().length];
            $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$Type = iArr;
            try {
                iArr[RemoteModRepository.Type.MOD.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$Type[RemoteModRepository.Type.MODPACK.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
        }
    }

    public static ModTranslations getTranslationsByRepositoryType(RemoteModRepository.Type type) {
        int i = AnonymousClass1.$SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$Type[type.ordinal()];
        if (i == 1) {
            return MOD;
        }
        if (i == 2) {
            return MODPACK;
        }
        return EMPTY;
    }

    private ModTranslations(String str) {
        this.resourceName = str;
    }

    public Mod getModByCurseForgeId(String str) {
        if (StringUtils.isBlank(str) || !loadCurseForgeMap()) {
            return null;
        }
        return this.curseForgeMap.get(str);
    }

    public Mod getModById(String str) {
        if (StringUtils.isBlank(str) || !loadModIdMap()) {
            return null;
        }
        return this.modIdMap.get(str);
    }

    public List<Mod> searchMod(String str) {
        if (!loadKeywords()) {
            return Collections.emptyList();
        }
        String sb = ((StringBuilder) str.chars().filter(new IntPredicate() { // from class: com.qcl.launcher.utils.string.ModTranslations$$ExternalSyntheticLambda4
            @Override // java.util.function.IntPredicate
            public final boolean test(int i) {
                return ModTranslations.lambda$searchMod$0(i);
            }
        }).collect(new Supplier() { // from class: com.qcl.launcher.utils.string.ModTranslations$$ExternalSyntheticLambda7
            @Override // java.util.function.Supplier
            public final Object get() {
                return ModTranslations.$r8$lambda$z4Sacp0dHgNB96bTlXZdt5SF0q4();
            }
        }, new ObjIntConsumer() { // from class: com.qcl.launcher.utils.string.ModTranslations$$ExternalSyntheticLambda5
            @Override // java.util.function.ObjIntConsumer
            public final void accept(Object obj, int i) {
                ((StringBuilder) obj).append((char) i);
            }
        }, new BiConsumer() { // from class: com.qcl.launcher.utils.string.ModTranslations$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((StringBuilder) obj).append((CharSequence) obj2);
            }
        })).toString();
        StringUtils.LongestCommonSubsequence longestCommonSubsequence = new StringUtils.LongestCommonSubsequence(sb.length(), this.maxKeywordLength);
        ArrayList arrayList = new ArrayList();
        for (Pair<String, Mod> pair : this.keywords) {
            int calc = longestCommonSubsequence.calc(sb, pair.getKey());
            if (calc >= Math.max(1, sb.length() - 3)) {
                arrayList.add(Pair.pair(Integer.valueOf(calc), pair.getValue()));
            }
        }
        return (List) arrayList.stream().sorted(new Comparator() { // from class: com.qcl.launcher.utils.string.ModTranslations$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return ModTranslations.lambda$searchMod$2((Pair) obj, (Pair) obj2);
            }
        }).map(new Function() { // from class: com.qcl.launcher.utils.string.ModTranslations$$ExternalSyntheticLambda2
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return (ModTranslations.Mod) ((Pair) obj).getValue();
            }
        }).collect(Collectors.toList());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$searchMod$0(int i) {
        return !Character.isSpaceChar(i);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$searchMod$2(Pair pair, Pair pair2) {
        return -((Integer) pair.getKey()).compareTo((Integer) pair2.getKey());
    }

    private boolean loadFromResource() {
        if (this.mods != null) {
            return true;
        }
        if (StringUtils.isBlank(this.resourceName)) {
            this.mods = Collections.emptyList();
            return true;
        }
        try {
            // ★★★★★ 2026-10-11 关键修复：**Android 上 getResourceAsStream 读不到 assets！**
            //   APK 里的 assets/ 不在 Java 类路径上（类路径只有 classes*.dex），
            //   所以 `ModTranslations.class.getResourceAsStream("/assets/mod_data.txt")`
            //   在真机/模拟器上**永远返回 null** ⇒ 表加载不了 ⇒ **一条模组名都翻译不出来**
            //   （用户实测「没有一个模组名被翻译成中文过」）。
            //   （桌面上 JVM 跑测试能通，是因为那时候 assets 真在 classpath 上 —— 这就是
            //     "本地验证过了但真机不行" 的经典陷阱。）
            //
            //   现在改成**双路**：
            //     ① 先试类路径（桌面/单元测试环境还能用）
            //     ② 读不到再走 Android 的 AssetManager（真机真正的读法）
            java.io.InputStream in = ModTranslations.class.getResourceAsStream(this.resourceName);
            if (in == null) {
                in = openFromAndroidAssets(this.resourceName);
            }
            if (in == null) {
                return false;
            }
            this.mods = parseModsSafely(
                    IOUtils.readFullyAsString(in, StandardCharsets.UTF_8),
                    this.resourceName);
            return true;
        } catch (Exception e) {
            Logging.LOG.log(Level.WARNING, "Failed to load " + this.resourceName, (Throwable) e);
            return false;
        }
    }

    /**
     * ★ 2026-10-11 新增：用 Android 的 AssetManager 打开 assets 里的文件。
     *
     * <p>为什么必须单独写：{@code getResourceAsStream} 只能读 **Java 类路径**，
     * 而 APK 的 {@code assets/} 目录不在类路径上（只有 {@code classes*.dex} 在）。
     * 想在真机上读 assets，只能用 {@code AssetManager.open("mod_data.txt")} ——
     * 注意路径**不带 "assets/" 前缀**。
     *
     * <p>这里用反射拿 Application 的 Context，避免给这个工具类增加构造参数
     * （它被 12 处静态调用，改签名牵动太广）。
     *
     * @param resourceName 形如 {@code /assets/mod_data.txt}
     * @return 输入流；拿不到返回 null（调用方负责报错）
     */
    private static java.io.InputStream openFromAndroidAssets(String resourceName) {
        try {
            String path = normalizeAssetPath(resourceName);
            // ★ 优先用**外部显式设置**的 Context（MainActivity 启动时 {@link #preload}
            //   会把 Application 传进来）。这条路径最可靠 —— 不依赖任何反射与全局状态。
            android.content.Context ctx = cachedContext;
            if (ctx == null) {
                // 兜底：老路径（AppGlobals 反射拿 Application）
                //   注意必须强转成 android.content.Context 再调 getAssets()
                //   （踩过的坑：`app.getClass().getMethod("getAssets")` 找不到 ——
                //     getAssets 继承自 ContextWrapper，反射在 Application 类里找不到）
                Class<?> appGlobals = Class.forName("android.app.AppGlobals");
                Object app = appGlobals.getMethod("getInitialApplication").invoke(null);
                if (app instanceof android.content.Context) {
                    ctx = (android.content.Context) app;
                }
            }
            if (ctx == null) {
                return null;
            }
            return ctx.getAssets().open(path);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * ★ 2026-10-11 新增：由 {@code MainActivity} 在启动时调用，**显式**把 Context 交进来并
     * **主动预热**翻译表。
     *
     * <p>为什么需要：
     * <ul>
     *   <li>原来只靠 {@code AppGlobals.getInitialApplication()} 反射拿 Application，
     *       一旦它在首次调用时还没准备好，就会返回 null ⇒ 表加载失败
     *       ⇒ 而且失败结果会被**缓存**（mods 被设成空列表）⇒ 之后**永远没中文**，
     *       表现就是用户实测的「下载页里的模组一个中文也没有」。</li>
     *   <li>预热还有一个好处：1.8 MB 的表在**主线程之外**先读好，
     *       列表滚动时才不会第一次查表就卡一下。</li>
     * </ul>
     *
     * @param context 任意 Context（内部只取 ApplicationContext，不会泄漏 Activity）
     */
    public static void preload(final android.content.Context context) {
        if (context == null) {
            return;
        }
        try {
            cachedContext = context.getApplicationContext() != null
                    ? context.getApplicationContext() : context;
        } catch (Throwable ignored) {
            return;
        }
        // 放到后台线程预热：1.8 MB 的解析不该占主线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MOD.loadFromResource();
                    MOD.loadCurseForgeMap();
                    MOD.loadModIdMap();
                    MODPACK.loadFromResource();
                    MODPACK.loadCurseForgeMap();
                    MODPACK.loadModIdMap();
                    android.util.Log.i("QCL-i18n", "翻译表预热完成: MOD="
                            + (MOD.mods == null ? -1 : MOD.mods.size())
                            + " MODPACK=" + (MODPACK.mods == null ? -1 : MODPACK.mods.size()));
                } catch (Throwable t) {
                    android.util.Log.w("QCL-i18n", "翻译表预热失败: " + t);
                }
            }
        }, "qcl-i18n-preload").start();
    }

    /** ★ 外部显式设置的 Context（见 {@link #preload}）。 */
    private static volatile android.content.Context cachedContext;

    /** 把 {@code /assets/xxx.txt} 归一化成 AssetManager 认识的 {@code xxx.txt}。 */
    private static String normalizeAssetPath(String resourceName) {
        String path = resourceName == null ? "" : resourceName;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.startsWith("assets/")) {
            path = path.substring("assets/".length());
        }
        return path;
    }

    /**
     * ★★★ 1.5.0 新增：**逐行容错**地把一整张表读成 Mod 列表。
     *
     * <p>为什么必须单独写这个：原来用 stream 的 {@code map(line -> new Mod(line))}，
     * 而 {@link Mod#Mod(String)} 对格式不对的行**直接 throw** ⇒ **一行坏、整张表全废**
     * （{@code collect} 抛异常 → 外层 catch → mods 为 null → 一条中文都出不来）。
     *
     * <p>现在改成逐行 try/catch：坏行跳过并继续，**其余行照常生效**。
     * 顺带统计跳过条数，便于日后排查数据文件。
     *
     * @return 成功解析的条数（可能为 0，不抛异常）
     */
    private static List<Mod> parseModsSafely(String content, String sourceName) {
        List<Mod> out = new ArrayList<>();
        if (content == null) {
            return out;
        }
        int skipped = 0;
        for (String raw : content.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            try {
                out.add(new Mod(line));
            } catch (Throwable t) {
                skipped++;
            }
        }
        if (skipped > 0) {
            Logging.LOG.log(Level.WARNING,
                    "Skipped " + skipped + " malformed lines in " + sourceName);
        }
        return out;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$loadFromResource$3(String str) {
        return !str.startsWith("#");
    }

    private boolean loadCurseForgeMap() {
        if (this.curseForgeMap != null) {
            return true;
        }
        if (this.mods == null && !loadFromResource()) {
            return false;
        }
        this.curseForgeMap = new HashMap();
        for (Mod mod : this.mods) {
            if (StringUtils.isNotBlank(mod.getCurseforge())) {
                this.curseForgeMap.put(mod.getCurseforge(), mod);
            }
        }
        return true;
    }

    private boolean loadModIdMap() {
        if (this.modIdMap != null) {
            return true;
        }
        if (this.mods == null && !loadFromResource()) {
            return false;
        }
        this.modIdMap = new HashMap();
        for (Mod mod : this.mods) {
            for (String str : mod.getModIds()) {
                if (StringUtils.isNotBlank(str) && !"examplemod".equals(str)) {
                    this.modIdMap.put(str, mod);
                }
            }
        }
        return true;
    }

    private boolean loadKeywords() {
        if (this.keywords != null) {
            return true;
        }
        if (this.mods == null && !loadFromResource()) {
            return false;
        }
        this.keywords = new ArrayList();
        this.maxKeywordLength = -1;
        for (Mod mod : this.mods) {
            if (StringUtils.isNotBlank(mod.getName())) {
                this.keywords.add(Pair.pair(mod.getName(), mod));
                this.maxKeywordLength = Math.max(this.maxKeywordLength, mod.getName().length());
            }
            if (StringUtils.isNotBlank(mod.getSubname())) {
                this.keywords.add(Pair.pair(mod.getSubname(), mod));
                this.maxKeywordLength = Math.max(this.maxKeywordLength, mod.getSubname().length());
            }
            if (StringUtils.isNotBlank(mod.getAbbr())) {
                this.keywords.add(Pair.pair(mod.getAbbr(), mod));
                this.maxKeywordLength = Math.max(this.maxKeywordLength, mod.getAbbr().length());
            }
        }
        return true;
    }

    /* loaded from: classes2.dex */
    public static class Mod {
        private final String abbr;
        private final String curseforge;
        private final String mcbbs;
        private final String mcmod;
        private final List<String> modIds;
        private final String name;
        private final String subname;

        public Mod(String str) {
            // ★★★ 1.5.0 修复（用户实测「下载列表的模组依旧没有中文翻译」，实测数据取证）：
            //   这份翻译表（assets/mod_data.txt 31062 条 / modpack_data.txt 1493 条）
            //   的**真实格式是 6 段**：`curseforge;mcmod;modIds;名称;副名;缩写`
            //   ——例：`industrial-craft;2;IC2,ic2;工业时代2;Industrial Craft 2;IC2`
            //   而旧代码要求 7 段、**不满足直接 throw IllegalArgumentException**
            //   ⇒ 解析第一条就炸 ⇒ **整张表加载失败** ⇒ 一条中文都出不来（现象正是"完全没翻译"）。
            //   ⇒ 现在按真实格式解析 6 段；同时保留对 7 段（旧格式）的兼容。
            String[] split = str.split(";", -1);
            if (split.length == 6) {
                this.curseforge = split[0];
                this.mcmod = split[1];
                this.mcbbs = "";
                this.modIds = Collections.unmodifiableList(Arrays.asList(split[2].split(",")));
                this.name = split[3];
                this.subname = split[4];
                this.abbr = split[5];
            } else if (split.length == 7) {
                this.curseforge = split[0];
                this.mcmod = split[1];
                this.mcbbs = split[2];
                this.modIds = Collections.unmodifiableList(Arrays.asList(split[3].split(",")));
                this.name = split[4];
                this.subname = split[5];
                this.abbr = split[6];
            } else {
                throw new IllegalArgumentException("Illegal mod data line, 6 or 7 items expected " + str);
            }
        }

        public Mod(String str, String str2, String str3, List<String> list, String str4, String str5, String str6) {
            this.curseforge = str;
            this.mcmod = str2;
            this.mcbbs = str3;
            this.modIds = list;
            this.name = str4;
            this.subname = str5;
            this.abbr = str6;
        }

        public String getDisplayName() {
            StringBuilder sb = new StringBuilder();
            if (StringUtils.isNotBlank(this.abbr)) {
                sb.append("[").append(this.abbr.trim()).append("] ");
            }
            sb.append(this.name);
            if (StringUtils.isNotBlank(this.subname)) {
                sb.append(" (").append(this.subname).append(")");
            }
            return sb.toString();
        }

        public String getCurseforge() {
            return this.curseforge;
        }

        public String getMcmod() {
            return this.mcmod;
        }

        public String getMcbbs() {
            return this.mcbbs;
        }

        public List<String> getModIds() {
            return this.modIds;
        }

        public String getName() {
            return this.name;
        }

        public String getSubname() {
            return this.subname;
        }

        public String getAbbr() {
            return this.abbr;
        }
    }
}
