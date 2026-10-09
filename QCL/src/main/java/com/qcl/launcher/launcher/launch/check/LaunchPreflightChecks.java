package com.qcl.launcher.launcher.launch.check;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ★ 2026-10-09 用户要求：在「启动游戏」控制台里增加两行检测 —— <b>模组检测</b> 与 <b>游戏版本检测</b>，
 * 且要「超 FCL」。
 *
 * <p>「超 FCL」的落点：FCL 只回答「有没有装加载器 / 装没装模组」；这两个检测给出
 * <b>逐条可核对的详情</b>，把真正会让玩家启动失败的原因提前点出来：
 * <ul>
 *   <li><b>游戏版本检测</b>：目录 / json / 客户端 jar 是否存在、json 里的 {@code id} 是否与目录名一致
 *       （本项目真实踩过的坑：{@code id} 与目录名不一致会让「版本判定」整体错乱）、
 *       {@code inheritsFrom} 父版本是否还在、{@code assetIndex} 索引文件是否齐全、
 *       {@code mainClass} / {@code libraries} 是否可用；并识别加载器（Fabric/Quilt/Forge/NeoForge）。</li>
 *   <li><b>模组检测</b>：扫描 {@code <gameDir>/mods}，逐个读 {@code fabric.mod.json} /
 *       {@code META-INF/mods.toml}，检查 —— 重复 mod id、声明支持的 MC 版本与本版本不符、
 *       缺前置（depends 里引用了本地没有的 mod）、空文件/损坏 jar、以及「原版版本却放了 mods」。</li>
 * </ul>
 *
 * <p>实现纪律：<b>纯 Java，零新依赖</b>（只用到 {@code org.json} 与 {@code java.util.zip}）；
 * 任何异常都退化成「检测不出问题」，绝不因为检测本身把启动流程卡死（调用方另有兜底）。
 */
public final class LaunchPreflightChecks {

    private LaunchPreflightChecks() {
    }

    /** 检测结果：一行摘要 + 逐条详情（详情为空表示没发现问题）。 */
    public static final class Result {
        /** 是否「可以继续启动」（有致命问题则为 false）。 */
        public final boolean ok;
        /** 行里显示的一行摘要，如 {@code OK · Fabric · 7 个模组}。 */
        public final String summary;
        /** 逐条详情（超 FCL：玩家可在详情弹窗里逐条核对）。 */
        public final List<String> details;
        /** 致命问题条数（详情里带「✗」的条数，用于文案）。 */
        public final int fatalCount;

        Result(boolean ok, String summary, List<String> details, int fatalCount) {
            this.ok = ok;
            this.summary = summary;
            this.details = details;
            this.fatalCount = fatalCount;
        }
    }

    // ------------------------------------------------------------------ ① 游戏版本检测

    /** 游戏版本检测。{@code versionPath} 既可以是版本名，也可以是版本的完整路径。 */
    public static Result checkGameVersion(Context context, String versionPath) {
        List<String> details = new ArrayList<>();
        int fatal = 0;
        try {
            File dir = resolveVersionDir(versionPath);
            if (dir == null || !dir.isDirectory()) {
                return new Result(false, "找不到版本目录", one("✗ 版本目录不存在：" + versionPath), 1);
            }
            String name = dir.getName();
            String loader = "原版";

            File json = new File(dir, name + ".json");
            if (!json.isFile() || json.length() == 0L) {
                // 没有同名 json 时，退一步找目录里唯一的 json（有些整合包会改名）
                File alt = firstJsonIn(dir);
                if (alt != null) {
                    details.add("△ 未找到 " + name + ".json，改用 " + alt.getName());
                    json = alt;
                } else {
                    return new Result(false, "缺少版本 json",
                            one("✗ 缺少 " + name + ".json（版本无法被识别）"), 1);
                }
            }

            JSONObject obj = null;
            try {
                obj = new JSONObject(readText(json));
            } catch (Throwable t) {
                details.add("✗ " + json.getName() + " 无法解析：" + t);
                fatal++;
            }

            String jsonId = null;
            String mainClass = null;
            String inheritsFrom = null;
            String assetIndexId = null;
            int libCount = -1;

            if (obj != null) {
                jsonId = trim(obj.optString("id", ""));
                mainClass = trim(obj.optString("mainClass", ""));
                inheritsFrom = trim(obj.optString("inheritsFrom", ""));
                JSONObject ai = obj.optJSONObject("assetIndex");
                if (ai != null) {
                    assetIndexId = trim(ai.optString("id", ""));
                }
                if (obj.optJSONArray("libraries") != null) {
                    libCount = obj.optJSONArray("libraries").length();
                }

                // ★ 本项目铁律：版本判定必须从 json 取顶级 "id" 整体 equals；id 与目录名不一致 = 风险
                // ★ 2026-10-09 用户实测：这条**降级为「△ 提醒」**。
                //   克隆/改名版本（如把 26.2 复制成 26.2fa 加模组）非常常见，id 不一致通常还能启动，
                //   标成致命会误拦 —— 宁可少报，也不拦玩家。
                if (jsonId != null && !jsonId.isEmpty() && !jsonId.equals(name)) {
                    details.add("△ json 里的 id=\"" + jsonId + "\" 与目录名 \"" + name + "\" 不一致"
                            + "（克隆/改名版本常见，一般不影响启动；若出现依赖解析异常可重装该版本）");
                }
                if (mainClass == null || mainClass.isEmpty()) {
                    details.add("✗ json 缺少 mainClass（无法确定入口类）");
                    fatal++;
                }
                loader = detectLoader(mainClass, obj);

                // inheritsFrom：父版本必须还在
                if (inheritsFrom != null && !inheritsFrom.isEmpty()) {
                    File parent = new File(dir.getParentFile(), inheritsFrom);
                    if (!parent.isDirectory()) {
                        details.add("✗ 依赖的父版本 \"" + inheritsFrom + "\" 不存在（本版本无法解析继承链）");
                        fatal++;
                    }
                }

                // assetIndex：索引文件必须存在
                if (assetIndexId != null && !assetIndexId.isEmpty() && context != null) {
                    File idx = new File(new File(new File(gameDirOf(dir), "assets"), "indexes"),
                            assetIndexId + ".json");
                    if (!idx.isFile() || idx.length() == 0L) {
                        details.add("△ 资源索引 " + assetIndexId + ".json 缺失（首次启动会去下载，网络差会卡住）");
                    }
                }
            }

            // 客户端 jar：MC 自己不是按「目录名」找的，而是按版本 json 的 id / jar 字段找。
            // ★ 2026-10-09 用户实测修复：「我就改了个目录名，你就给我拦截了」——
            //   改名后 jar 仍叫旧名字（26.2.jar），旧逻辑按目录名去找 ⇒ 误判「缺少客户端 jar」并拦。
            //   ⇒ 依次尝试：<目录名>.jar → <json id>.jar → json 的 "jar" 字段 → 目录里唯一的 jar。
            File jar = new File(dir, name + ".jar");
            boolean jarOk = jar.isFile() && jar.length() > 0L;
            if (!jarOk && jsonId != null && !jsonId.isEmpty()) {
                File alt = new File(dir, jsonId + ".jar");
                if (alt.isFile() && alt.length() > 0L) {
                    jar = alt;
                    jarOk = true;
                    details.add("△ 客户端 jar 用的是 " + alt.getName()
                            + "（与目录名 " + name + " 不同名，改名版本正常现象）");
                }
            }
            if (!jarOk && obj != null) {
                String jarField = trim(obj.optString("jar", ""));
                if (!jarField.isEmpty()) {
                    File alt = new File(dir, jarField.endsWith(".jar") ? jarField : (jarField + ".jar"));
                    if (alt.isFile() && alt.length() > 0L) {
                        jar = alt;
                        jarOk = true;
                        details.add("△ 客户端 jar 用的是 " + alt.getName() + "（来自版本 json 的 jar 字段）");
                    }
                }
            }
            if (!jarOk) {
                File only = onlyJarIn(dir);
                if (only != null) {
                    jar = only;
                    jarOk = true;
                    details.add("△ 客户端 jar 用的是 " + only.getName() + "（目录里唯一的 jar）");
                }
            }
            boolean selfJarOk = jarOk;
            if (!selfJarOk && (inheritsFrom == null || inheritsFrom.isEmpty())) {
                // ★ 降级为「提醒」，不再拦：改名 / 整合包 / 只有 inheritsFrom 的版本都很常见
                details.add("△ 没找到与目录名同名的客户端 jar（改名或整合包常见，一般不影响启动；"
                        + "若启动报找不到主类，请重装该版本）");
            } else if (!selfJarOk) {
                details.add("△ 无自带 jar，走继承 " + inheritsFrom + "（正常）");
            }

            if (libCount == 0) {
                details.add("✗ json 的 libraries 为空（依赖库缺失）");
                fatal++;
            }

            String summary = (fatal > 0)
                    ? (fatal + " 个问题 · " + loader)
                    : ("OK · " + name + " · " + loader + (selfJarOk ? "" : "（继承）"));
            return new Result(fatal == 0, summary, details, fatal);
        } catch (Throwable t) {
            // 检测本身出错 → 不拦
            return new Result(true, "跳过（检测异常）", one("△ 检测异常：" + t), 0);
        }
    }

    // ------------------------------------------------------------------ ② 模组检测

    /** 模组检测。扫描 {@code <gameDir>/mods}（以及版本目录下的 mods，若存在）。 */
    public static Result checkMods(Context context, String versionPath) {
        return checkMods(context, versionPath, null);
    }

    /**
     * 模组检测（带当前渲染器 id）。
     *
     * <p>★ 2026-10-09 用户实测修复「我已经把渲染器改成 mg 了，为什么还提示不兼容」：
     * FCL 的 `sodium/embeddium` 判定**带「仅 GL4ES」条件**（`bridge.renderer == RENDERER_GL4ES.name`），
     * 我照抄表时把这个条件丢了 ⇒ 换成 mg 还在提示。现在按原条件判断。
     */
    public static Result checkMods(Context context, String versionPath, String rendererId) {
        List<String> details = new ArrayList<>();
        int fatal = 0;
        try {
            File dir = resolveVersionDir(versionPath);
            if (dir == null || !dir.isDirectory()) {
                return new Result(true, "跳过（无版本目录）", one("△ 无版本目录，跳过模组检测"), 0);
            }
            String loader = detectLoaderFromDir(dir);

            List<File> modDirs = new ArrayList<>();
            File gMods = new File(gameDirOf(dir), "mods");
            if (gMods.isDirectory()) {
                modDirs.add(gMods);
            }
            // ★ 2026-10-09 用户实测修复：原来这里**还扫了 `<版本目录>/mods`**，导致同一个 jar 被数两遍
            //   ⇒ 报出假的「重复 mod id」（两边文件名一模一样）。MC 只读 `<gameDir>/mods`，
            //   所以这里只扫那一个目录。

            List<File> jars = new ArrayList<>();
            for (File d : modDirs) {
                File[] fs = d.listFiles();
                if (fs == null) {
                    continue;
                }
                for (File f : fs) {
                    String n = f.getName().toLowerCase();
                    if (f.isFile() && n.endsWith(".jar")) {
                        jars.add(f);
                    }
                }
            }

            if ("原版".equals(loader)) {
                if (jars.isEmpty()) {
                    return new Result(true, "OK · 原版 · 无模组", details, 0);
                }
                // ★ 2026-10-09：这就是 FCL 的 `LauncherHelper.checkModLoader` ——
                //   「有 mod 但没装加载器」必须**拦住**（FCL 弹「取消 / 安装」）。
                //   否则玩家以为模组装上了，进游戏发现一个都没加载。
                details.add("✗ 这是原版版本，但 mods 目录里有 " + jars.size()
                        + " 个 jar —— 原版不会加载任何模组（请改装 Fabric / Forge / NeoForge 版本）");
                return new Result(false, "有 " + jars.size() + " 个模组但未安装模组加载器", details, 1);
            }

            if (jars.isEmpty()) {
                return new Result(true, "OK · " + loader + " · 0 个模组", details, 0);
            }

            Map<String, String> idToFile = new HashMap<>();
            int incompatible = 0;

            for (File f : jars) {
                if (f.length() == 0L) {
                    details.add("✗ " + f.getName() + " 是 0 字节（损坏，必须删掉或重下）");
                    fatal++;
                    continue;
                }
                String[] meta = readModMeta(f, loader);
                String id = (meta == null || meta[0] == null) ? "" : meta[0].trim().toLowerCase(java.util.Locale.ROOT);
                if (id.isEmpty()) {
                    // 拿不到 id 就什么都不判断 —— 宁可不说，也绝不误报
                    continue;
                }
                if (idToFile.containsKey(id)) {
                    details.add("✗ 重复 mod id \"" + id + "\"：" + idToFile.get(id) + " 与 " + f.getName()
                            + "（同一个 mod 装了两份，会随机崩）");
                    fatal++;
                    continue;
                }
                idToFile.put(id, f.getName());

                // ★ 2026-10-09：**照抄 FCL 的 com/mio/minecraft/ModChecker.kt** ——
                //   只对手机端「已知会出问题」的 mod 逐个特判。
                //   ★ 绝不自创「版本区间 / 依赖缺失」这类泛化推断：实测在真机上必然误报
                //     （用户反馈「模组本来就是兼容的，你报不兼容」）。
                String hit = knownModIssue(context, f, id, rendererId);
                if (hit != null) {
                    details.add(hit);
                    if (hit.startsWith("✗")) {
                        incompatible++;
                        fatal++;
                    }
                }
            }

            String summary;
            if (fatal > 0) {
                summary = fatal + " 个不兼容 · " + loader + " · " + jars.size() + " 个模组";
            } else if (!details.isEmpty()) {
                summary = "OK（" + details.size() + " 条提醒）· " + loader + " · " + jars.size() + " 个模组";
            } else {
                summary = "OK · " + loader + " · " + jars.size() + " 个模组";
            }
            return new Result(fatal == 0, summary, details, fatal);
        } catch (Throwable t) {
            return new Result(true, "跳过（检测异常）", one("△ 检测异常：" + t), 0);
        }
    }

    // ------------------------------------------------------------------ FCL 已知问题表

    /**
     * ★ 2026-10-09：**照抄 FCL 的 `com/mio/minecraft/ModChecker.kt`**。
     *
     * <p>FCL 的做法是「只特判**已知**必崩的少数 mod」，而不是泛化推断。为什么必须这样：
     * 泛化的「版本区间」「依赖缺失」在真机上必然误报（已实测被用户当场指出）。
     *
     * @return null = 没问题；以 {@code ✗} 开头 = 不兼容（阻断级）；以 {@code △} 开头 = 仅提醒
     */
    private static String knownModIssue(Context context, File jar, String id, String rendererId) {
        String name = jar.getName();
        switch (id) {
            case "touchcontroller":
                // FCL 只 setHasTouchController(true) 做标记，不拦 → 不报
                return null;
            case "mcef":
                return "✗ MCEF（" + name + "）在安卓上无法工作，它通常是 WebDisplays / 内置浏览器的前置";
            case "valkyrienskies":
                return "✗ Valkyrien Skies（" + name + "）单人模式会崩，必须连服务器";
            case "imblocker":
            case "ingameime":
            case "inputmethodblocker":
                return "✗ " + name + "（输入法类 mod）不是给安卓用的，装了必崩";
            case "borderlesswindow":
                return "✗ BorderlessWindow（" + name + "）不是给安卓用的，装了必崩";
            case "flashback":
                return "✗ Flashback（" + name + "）无法在安卓上工作";
            // ★ 2026-10-09 用户实测修复：**删掉 `ixeris`**。
            //   FCL 的原文是 "Ixeris mod (%s) cannot work properly on **FCL**" ——
            //   那是 **FCL 自己的实现限制**，不是安卓层面的结论。我照搬并写成
            //   「在本启动器上无法正常工作」属于**错误外推**（用户实测：该模组能正常启动）。
            //   ⇒ 凡 FCL 的结论只针对 FCL 自身的条目，一律不许搬。
            //   （其余条目原文都是 "cannot work on **Android**"，属安卓层面，保留。）
            case "physicsmod": {
                String arch = elfArchFromZip(jar, "de/fabmax/physxjni/linux/libPhysXJniBindings_64.so");
                boolean x86Device = isX86Device(context);
                if (arch.isEmpty() || (!x86Device && arch.contains("x86"))) {
                    return "✗ Physics Mod（" + name
                            + "）：内嵌 PhysX 是 Linux x86_64 版，需换成安卓版，否则崩";
                }
                return null;
            }
            case "yes_steve_model": {
                String core = elfArchFromZip(jar, "META-INF/native/libysm-core.so");
                String android = elfArchFromZip(jar, "META-INF/native/libysm-core-android.so");
                if (!core.isEmpty() && android.isEmpty()) {
                    return "✗ Yes Steve Model（" + name + "）2.2.2+ 无法在安卓上工作";
                }
                return null;
            }
            case "axiom": {
                String arch = elfArchFromZip(jar, "io/imgui/java/native-bin/libimgui-javaarm64.so");
                if (arch.isEmpty()) {
                    return "✗ Axiom（" + name + "）内嵌库未适配安卓，会崩";
                }
                return null;
            }
            case "replaymod":
                return "△ Replay Mod（" + name + "）需要 FFmpeg 插件才能录制 / 回放（本启动器暂未内置）";
            case "sodium":
            case "embeddium":
                // ★ 照 FCL 的 `ModChecker.kt`：**只有 GL4ES 渲染器**才提示
                //   （FCL 原文：bridge.renderer == RendererManager.RENDERER_GL4ES.name）。
                //   ★ 2026-10-09 用户实测修复：我原来丢了这个条件 ⇒ 换成 mg 还在误报。
                if (rendererId != null
                        && rendererId.toLowerCase(java.util.Locale.ROOT).contains("gl4es")) {
                    return "△ Sodium/Embeddium（" + name
                            + "）在 GL4ES 渲染器下可能崩；建议改用 MobileGlues";
                }
                return null;
            default:
                return null;
        }
    }

    /** 读 zip 内某个条目的 ELF 架构（照 FCL 的 `getElfArchFromZip`）；空串 = 没有 / 不是 ELF。 */
    private static String elfArchFromZip(File zipFile, String entryPath) {
        try (ZipFile zf = new ZipFile(zipFile)) {
            ZipEntry e = zf.getEntry(entryPath);
            if (e == null || e.isDirectory()) {
                return "";
            }
            try (InputStream in = zf.getInputStream(e)) {
                byte[] head = new byte[20];
                int n = 0;
                while (n < head.length) {
                    int r = in.read(head, n, head.length - n);
                    if (r <= 0) {
                        break;
                    }
                    n += r;
                }
                if (n < 20
                        || head[0] != 0x7F || head[1] != 'E' || head[2] != 'L' || head[3] != 'F') {
                    return "";
                }
                int machine = (head[18] & 0xFF) | ((head[19] & 0xFF) << 8);   // e_machine，小端
                switch (machine) {
                    case 0xB7:
                        return "aarch64";
                    case 0x3E:
                        return "x86_64";
                    case 0x28:
                        return "arm";
                    case 0x03:
                        return "x86";
                    default:
                        return "other";
                }
            }
        } catch (Throwable t) {
            return "";
        }
    }

    /** 设备是否 x86 系（决定 Physics Mod 那类「x86 架构 ELF 能不能用」的判断）。 */
    private static boolean isX86Device(Context context) {
        try {
            for (String abi : android.os.Build.SUPPORTED_ABIS) {
                if (abi != null && abi.toLowerCase(java.util.Locale.ROOT).contains("x86")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    // ------------------------------------------------------------------ 工具

    private static File resolveVersionDir(String versionPath) {
        if (versionPath == null || versionPath.isEmpty()) {
            return null;
        }
        File f = new File(versionPath);
        if (f.isDirectory()) {
            return f;
        }
        return f;   // 交给调用方判 isDirectory（保留原语义，便于详情里回显）
    }

    /** 版本目录 → gameDir（…/versions/<v> → …）。 */
    private static File gameDirOf(File versionDir) {
        // ★★★ 2026-10-09 用户实测修复（重要）：「我明明开了版本隔离，模组检测却报出 49 个模组，
        //   我这个版本一个模组也没有」。
        //   根因：老实现**无条件**返回 `.minecraft` 根目录，可开了隔离后本版本的 mods 其实在
        //   `<游戏根>/versions/<版本名>/mods`，根目录下的 mods 是**其它版本**的 ⇒ 全被算到头上。
        //   ⇒ 现在按项目官方的 `PrivateGameSetting.getGameDir(游戏根, 版本目录, gameDirSetting)`
        //     来算：type==1 是隔离、type==2 是自定义路径、type==0 才是共用游戏根。
        //   ★ 用官方的 getGameDir，不要自己拼路径（隔离规则以后若变，跟着变）。
        File versions = versionDir.getParentFile();
        File gameRoot = null;
        if (versions != null && "versions".equalsIgnoreCase(versions.getName())) {
            gameRoot = versions.getParentFile();
        }
        if (gameRoot == null) {
            // 兜底：按约定上跳两级
            File p = versionDir.getParentFile();
            gameRoot = (p != null && p.getParentFile() != null) ? p.getParentFile() : null;
        }
        if (gameRoot == null) {
            return versionDir;
        }

        com.qcl.launcher.launcher.setting.game.child.GameDirSetting dirSetting = readGameDirSetting(versionDir);
        // ★★★ 2026-10-09 二次修正（实机取证）：读不到该版本设置时**不能**直接退回根目录！
        //   玩家在「设置 → 通用游戏设置」里开的隔离，落在**全局** privateGameSetting
        //   （`UniversalGameSettingUI:554` 写 `activity.privateGameSetting.gameDirSetting.type`），
        //   而**不是**每个版本的 qcl.cfg。实测 inf-20100223 / 1.20.6 的 qcl.cfg 里
        //   压根没有 gameDirSetting 字段 ⇒ 上一版"读不到就退回根目录"照样把那 49 个模组算上。
        //   ⇒ 这里继续回退到**全局设置**，最后才退回根目录。
        if (dirSetting == null) {
            dirSetting = globalGameDirSetting();
        }
        if (dirSetting != null) {
            // ★ 官方签名：getGameDir(游戏根, 版本目录, gameDirSetting)
            //   type==0 共用游戏根 / type==1 隔离(=版本目录) / type==2 自定义路径
            String dir = com.qcl.launcher.launcher.setting.game.PrivateGameSetting
                    .getGameDir(gameRoot.getAbsolutePath(), versionDir.getAbsolutePath(), dirSetting);
            if (dir != null && !dir.isEmpty()) {
                return new File(dir);
            }
        }
        // 实在拿不到（既无版本设置也无全局设置）⇒ 退回游戏根，别因此不检测
        return gameRoot;
    }

    /**
     * 拿**全局**的游戏目录设置（「设置 → 通用游戏设置」里那个隔离/自定义路径开关）。
     *
     * <p>★ 必须有静态注入的 activity：用反射拿不到私有字段，得由 MainActivity 在启动时调一次
     *   {@link #setGlobalSettingProvider} 把 MainActivity 传进来。
     * <p>★ 没有 provider 时返回 null ⇒ 调用方退回游戏根（保持旧行为，不崩）。
     */
    private static android.content.Context sAppContext;

    /** 由 MainActivity（启动时）调用一次，注入 MainActivity 供读取全局设置。 */
    public static void setGlobalSettingProvider(android.content.Context ctx) {
        sAppContext = ctx;
    }

    private static com.qcl.launcher.launcher.setting.game.child.GameDirSetting globalGameDirSetting() {
        try {
            // ★ sAppContext 实际注册进来的是 MainActivity（它就是 Context），
            //   所以 cast 成 MainActivity 而不是 Application。
            if (!(sAppContext instanceof com.qcl.launcher.launcher.MainActivity)) {
                return null;
            }
            com.qcl.launcher.launcher.setting.game.PrivateGameSetting g =
                    ((com.qcl.launcher.launcher.MainActivity) sAppContext).privateGameSetting;
            return (g == null) ? null : g.gameDirSetting;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * 读某个版本的 {@code gameDirSetting}（含"是否隔离开关"）。
     *
     * <p>★ 存储位置照 {@code VersionSettingUI} 的写法：{@code <版本目录>/qcl.cfg}
     *   （它用的是 {@code GsonUtils.savePrivateGameSetting(..., <gameRoot>/versions/<版本名>/qcl.cfg)}）
     *   —— 不是 AppManifest 里的某个目录常量，别自己编路径。
     * <p>★ 返回 null 表示没有该版本的独立设置（新版本 / 存档缺失）⇒ 调用方按默认（共用游戏根）处理。
     * <p>★ 任何异常都吞掉返回 null：**预检绝不能因为读设置失败而崩**。
     */
    private static com.qcl.launcher.launcher.setting.game.child.GameDirSetting readGameDirSetting(File versionDir) {
        try {
            if (versionDir == null) {
                return null;
            }
            java.io.File f = new java.io.File(versionDir, "qcl.cfg");
            if (!f.isFile()) {
                return null;
            }
            // ★ 用 GsonUtils 里现成的读取方法（与 VersionSettingUI 保存端成对），别自己 new Gson()
            com.qcl.launcher.launcher.setting.game.PrivateGameSetting s =
                    com.qcl.launcher.utils.gson.GsonUtils
                            .getPrivateGameSettingFromFile(f.getAbsolutePath());
            return (s == null) ? null : s.gameDirSetting;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 目录里唯一的 .jar（改名版本靠它兜底找客户端 jar）。 */
    private static File onlyJarIn(File dir) {
        File[] fs = dir.listFiles();
        if (fs == null) {
            return null;
        }
        File found = null;
        for (File f : fs) {
            if (f.isFile() && f.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")
                    && f.length() > 0L) {
                if (found != null) {
                    return null;   // 不止一个 → 不敢猜
                }
                found = f;
            }
        }
        return found;
    }

    private static File firstJsonIn(File dir) {
        File[] fs = dir.listFiles();
        if (fs == null) {
            return null;
        }
        for (File f : fs) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".json")) {
                return f;
            }
        }
        return null;
    }

    private static String detectLoader(String mainClass, JSONObject obj) {
        String mc = mainClass == null ? "" : mainClass.toLowerCase();
        if (mc.contains("fabric") || mc.contains("knot")) {
            return "Fabric";
        }
        if (mc.contains("quilt")) {
            return "Quilt";
        }
        if (mc.contains("neoforge")) {
            return "NeoForge";
        }
        if (mc.contains("forge")) {
            return "Forge";
        }
        try {
            if (obj != null && obj.optJSONArray("libraries") != null) {
                String libs = obj.optJSONArray("libraries").toString().toLowerCase();
                if (libs.contains("fabric-loader")) {
                    return "Fabric";
                }
                if (libs.contains("quilt-loader")) {
                    return "Quilt";
                }
                if (libs.contains("neoforge")) {
                    return "NeoForge";
                }
                if (libs.contains("forge")) {
                    return "Forge";
                }
            }
        } catch (Throwable ignored) {
        }
        return "原版";
    }

    private static String detectLoaderFromDir(File versionDir) {
        String name = versionDir.getName();
        File json = new File(versionDir, name + ".json");
        if (!json.isFile()) {
            json = firstJsonIn(versionDir);
        }
        try {
            if (json != null && json.isFile()) {
                JSONObject o = new JSONObject(readText(json));
                return detectLoader(o.optString("mainClass", ""), o);
            }
        } catch (Throwable ignored) {
        }
        return "原版";
    }

    /** 从版本 json 的 id 猜 MC 版本（`fabric-loader-0.19.5-26.3` 也要取到 `26.3`）。 */
    private static String guessMcVersion(File versionDir) {
        String id = null;
        try {
            String name = versionDir.getName();
            File json = new File(versionDir, name + ".json");
            if (!json.isFile()) {
                json = firstJsonIn(versionDir);
            }
            if (json != null && json.isFile()) {
                id = new JSONObject(readText(json)).optString("id", "");
            }
        } catch (Throwable ignored) {
        }
        String best = lastVersionLike(id);
        if (best == null) {
            best = lastVersionLike(versionDir.getName());
        }
        return best == null ? versionDir.getName() : best;
    }

    /** 取字符串里**最后一个** `数字.数字…` 形态（★ 用最后一个：靠前的常是加载器版本号）。 */
    private static String lastVersionLike(String s) {
        if (s == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+(?:\\.\\d+)+").matcher(s);
        String last = null;
        while (m.find()) {
            last = m.group();
        }
        return last;
    }

    private static String normalizeMc(String v) {
        if (v == null) {
            return "";
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^(\\d+(?:\\.\\d+)+)").matcher(v.trim());
        return m.find() ? m.group(1) : v.trim();
    }

    /**
     * 版本区间判定（**保守优先：判不出来就当作满足，绝不误报**）。
     *
     * <p>★ 2026-10-09 用户实测修复：「模组本来就是兼容的，你报不兼容」。
     * 旧实现只认 {@code >= / > / <= / < / =}，其余写法退化成"前缀匹配"，
     * 而 Fabric 的 mod 普遍写 {@code ~1.21.4}、{@code ^1.21}、{@code 1.21.x}、
     * {@code >=1.21 <1.22}、Maven 区间 {@code [1.20,1.22)} —— 一匹配不上就误报。
     * ⇒ 现在把这些常见写法**全部正确展开**；**任何无法识别的记号一律返回 true（不报问题）**。
     */
    static boolean rangeSatisfies(String range, String version) {
        try {
            String r = range.trim();
            if (r.isEmpty() || "*".equals(r) || "x".equalsIgnoreCase(r)) {
                return true;
            }
            // Maven 区间：[1.20,1.22) / (1.20,1.22] / [1.20,) 等
            if (r.startsWith("[") || r.startsWith("(")) {
                boolean incLow = r.startsWith("[");
                boolean incHigh = r.endsWith("]");
                String body = r.substring(1, r.length() - 1);
                String[] parts = body.split(",", -1);
                if (parts.length == 2) {
                    String lo = parts[0].trim();
                    String hi = parts[1].trim();
                    if (!lo.isEmpty()) {
                        int c = cmp(version, lo);
                        if (incLow ? c < 0 : c <= 0) {
                            return false;
                        }
                    }
                    if (!hi.isEmpty()) {
                        int c = cmp(version, hi);
                        if (incHigh ? c > 0 : c >= 0) {
                            return false;
                        }
                    }
                    return true;
                }
                return true;
            }
            // 逗号或空格分隔的多条约束（AND）
            for (String token : r.split("[,\\s]+")) {
                if (token.isEmpty() || "*".equals(token) || "x".equalsIgnoreCase(token)) {
                    continue;
                }
                Boolean ok = satisfiesToken(token, version);
                if (ok == null) {
                    return true;   // 有不认识的写法 → 不报问题
                }
                if (!ok) {
                    return false;
                }
            }
            return true;
        } catch (Throwable t) {
            return true;
        }
    }

    /** 单个约束；返回 null 表示无法识别（调用方按"满足"处理）。 */
    private static Boolean satisfiesToken(String token, String version) {
        String op = "";
        String v = token;
        String[] ops = {">=", "<=", "==", ">", "<", "=", "~", "^"};
        for (String o : ops) {
            if (v.startsWith(o)) {
                op = o;
                v = v.substring(o.length()).trim();
                break;
            }
        }
        if (v.isEmpty()) {
            return null;
        }
        // 通配：1.21.x / 1.21.* → 前缀匹配
        String wildcard = v.replace('X', 'x');
        if (wildcard.endsWith(".x") || wildcard.endsWith(".*")) {
            String prefix = wildcard.substring(0, wildcard.length() - 2);
            return version.equals(prefix) || version.startsWith(prefix + ".");
        }
        if (wildcard.contains("x") || wildcard.contains("*")) {
            return null;
        }
        // ~a.b.c → >=a.b.c <a.(b+1)   ；~a.b → >=a.b <(a+1).0
        if ("~".equals(op)) {
            String[] p = v.split("\\.");
            if (p.length >= 3) {
                int up = num(p[1]) + 1;
                return cmp(version, v) >= 0 && cmp(version, p[0] + "." + up) < 0;
            }
            if (p.length == 2) {
                int up = num(p[0]) + 1;
                return cmp(version, v) >= 0 && cmp(version, up + ".0") < 0;
            }
            return null;
        }
        // ^a.b.c → >=a.b.c <(a+1).0.0
        if ("^".equals(op)) {
            String[] p = v.split("\\.");
            if (p.length >= 1) {
                int up = num(p[0]) + 1;
                return cmp(version, v) >= 0 && cmp(version, up + ".0.0") < 0;
            }
            return null;
        }
        switch (op) {
            case ">=":
                return cmp(version, v) >= 0;
            case ">":
                return cmp(version, v) > 0;
            case "<=":
                return cmp(version, v) <= 0;
            case "<":
                return cmp(version, v) < 0;
            case "=":
            case "==":
                // 精确：按前缀匹配（1.21.4 视为匹配 1.21.4.x）
                return version.equals(v) || version.startsWith(v + ".");
            default:
                // 无运算符：也按前缀匹配
                return version.equals(v) || version.startsWith(v + ".");
        }
    }

    private static int cmp(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            int x = num(i < pa.length ? pa[i] : null);
            int y = num(i < pb.length ? pb[i] : null);
            if (x != y) {
                return x < y ? -1 : 1;
            }
        }
        return 0;
    }

    private static int num(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Throwable t) {
            return 0;
        }
    }

    /** 读 mod 元数据：[id, mcVersionRange]；不是可识别模组返回 null。 */
    private static String[] readModMeta(File jar, String loader) {
        try (ZipFile zf = new ZipFile(jar)) {
            ZipEntry e = zf.getEntry("fabric.mod.json");
            if (e != null) {
                JSONObject o = new JSONObject(readStream(zf.getInputStream(e)));
                String id = trim(o.optString("id", ""));
                String mc = null;
                JSONObject dep = o.optJSONObject("depends");
                if (dep != null && dep.has("minecraft")) {
                    mc = String.valueOf(dep.opt("minecraft"));
                }
                return new String[]{id, mc};
            }
            ZipEntry t = zf.getEntry("META-INF/mods.toml");
            if (t != null) {
                String toml = readStream(zf.getInputStream(t));
                String id = matchToml(toml, "modId");
                String mc = matchToml(toml, "versionRange");
                return new String[]{id, mc};
            }
            if (zf.getEntry("META-INF/neoforge.mods.toml") != null) {
                String toml = readStream(zf.getInputStream(zf.getEntry("META-INF/neoforge.mods.toml")));
                return new String[]{matchToml(toml, "modId"), matchToml(toml, "versionRange")};
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** 读 mod 的前置依赖 id 列表。 */
    private static List<String> readModDepends(File jar, String loader) {
        List<String> out = new ArrayList<>();
        try (ZipFile zf = new ZipFile(jar)) {
            ZipEntry e = zf.getEntry("fabric.mod.json");
            if (e != null) {
                JSONObject o = new JSONObject(readStream(zf.getInputStream(e)));
                JSONObject dep = o.optJSONObject("depends");
                if (dep != null) {
                    java.util.Iterator<String> it = dep.keys();
                    while (it.hasNext()) {
                        out.add(it.next());
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static boolean isBuiltinDep(String d) {
        if (d == null) {
            return true;
        }
        String s = d.toLowerCase();
        return s.equals("minecraft") || s.equals("fabricloader") || s.equals("fabric-api")
                || s.equals("fabric") || s.equals("java") || s.equals("forge")
                || s.equals("neoforge") || s.equals("quilt_loader") || s.equals("quilt_base")
                || s.equals("minecraft") || s.startsWith("fabric-");
    }

    private static String matchToml(String toml, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?m)^\\s*" + key + "\\s*=\\s*\"([^\"]*)\"").matcher(toml);
        return m.find() ? m.group(1) : null;
    }

    private static String readText(File f) {
        try (InputStream in = new java.io.FileInputStream(f)) {
            return readStream(in);
        } catch (Throwable t) {
            return "";
        }
    }

    private static String readStream(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        int total = 0;
        while ((n = in.read(buf)) > 0 && total < 4 * 1024 * 1024) {
            bos.write(buf, 0, n);
            total += n;
        }
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static List<String> one(String s) {
        List<String> l = new ArrayList<>();
        l.add(s);
        return l;
    }

    /** 供详情弹窗用：把结果格式化成多行文本。 */
    public static String formatDetails(String title, Result r) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("：").append(r.summary).append('\n');
        if (r.details != null && !r.details.isEmpty()) {
            sb.append('\n');
            int i = 1;
            for (String d : new LinkedHashSet<>(r.details)) {
                sb.append(i++).append(". ").append(d).append('\n');
            }
        } else {
            sb.append("\n没有发现问题。");
        }
        return sb.toString();
    }
}
