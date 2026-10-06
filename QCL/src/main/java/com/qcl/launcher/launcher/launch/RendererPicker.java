package com.qcl.launcher.launcher.launch;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;

import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.setting.game.child.PojavLauncherSetting;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.gson.GsonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ★★★ 1.1.1：渲染器选择器（主界面长按启动按钮 / 版本设置 / 全局游戏设置 三处共用）。
 *
 * 之前版本设置页只有两个写死的单选按钮（Holy-GL4ES / VirGLRenderer），既不全也和
 * {@link RendererCompat} 的注册表不一致；现在统一改为弹窗，列表直接来自注册表：
 * 全称 + 支持版本区间 + ★推荐 + ✓当前 + ⚠不支持当前版本。
 * 原生库文件不存在的渲染器（例如没导入的 MobileGlues）不显示。
 */
public final class RendererPicker {

    private RendererPicker() {
    }

    /** 取当前「实际生效」的渲染器 id：独立设置版本读 qcl.cfg，否则读全局对象。 */
    public static String currentRendererOf(PrivateGameSetting pgs, String versionPath) {
        if (versionPath != null) {
            String qclCfg = versionPath + "/qcl.cfg";
            if (new File(qclCfg).exists()) {
                PrivateGameSetting v = GsonUtils.getPrivateGameSettingFromFile(qclCfg);
                if (v != null && (v.forceEnable || v.enable) && v.pojavLauncherSetting != null) {
                    return v.pojavLauncherSetting.renderer;
                }
            }
        }
        return (pgs != null && pgs.pojavLauncherSetting != null) ? pgs.pojavLauncherSetting.renderer : null;
    }

    /** 从版本目录路径取版本名（用于兼容性判断）。 */
    public static String versionNameOf(String versionPath) {
        try {
            if (versionPath != null && !versionPath.isEmpty()) {
                return new File(versionPath).getName();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** 渲染器 id → 界面上显示的名字。 */
    public static String displayNameOf(String id) {
        RendererCompat.Info info = RendererCompat.find(id);
        return info != null ? info.displayName : id;
    }

    /**
     * 弹出渲染器选择窗口。onChanged 可以为 null。
     *
     * @param versionPath 该选择作用于哪个版本目录；传 null / 非目录 = **写全局设置**。
     *                    ★ 必须传对：这是「渲染器按版本隔开」的唯一开关。
     *                    · 版本独立设置页 → 传本页正在编辑的版本目录（写 qcl.cfg，仅本版本生效）
     *                    · 全局游戏设置页   → 传 null（写 private_game_setting.json，所有版本共用）
     *                    · 主界面长按启动   → 传当前版本目录（等于"只给这个版本改"，符合直觉）
     */
    public static void show(final Activity activity, final PrivateGameSetting pgs,
                            final String versionPath, final Runnable onChanged) {
        show(activity, pgs, versionPath, true, onChanged);
    }

    /**
     * @param perVersion true = 该选择只作用于 versionPath 指定的版本（写 qcl.cfg）；
     *                   false = 写全局设置（忽略 versionPath）。
     *                    全局游戏设置页必须传 false，否则玩家的全局改动会被写进某个版本的 qcl.cfg，
     *                    结果是「全局设置里改了却只对某一个版本生效」——反向的隔离 bug。
     */
    public static void show(final Activity activity, final PrivateGameSetting pgs,
                            final String versionPath, final boolean perVersion, final Runnable onChanged) {
        try {
            final String mcVer = versionNameOf(versionPath);
            final String effPath = perVersion ? versionPath : null;
            final String current = currentRendererOf(pgs, effPath);
            final List<String> labels = new ArrayList<String>();
            final List<String> ids = new ArrayList<String>();
            String nativeDir = activity.getApplicationInfo().nativeLibraryDir;

            // ★★★ 2026-09-19 修复「外部渲染器（mg）检测不到」：
            //   原逻辑对「APK 内没有该 so」的渲染器直接 continue 隐藏 —— 而 mg（MobileGlues）
            //   是外部渲染器，so 由玩家放在 <游戏目录>/renderer/mg/，永远不会出现在 APK 里，
            //   于是列表里根本看不到它，玩家误以为"没抄/没加"。
            //   现在：**所有渲染器都列出**，缺库的标注「⚠缺少库文件（需自行导入）」，一目了然。
            final String gameDir = RendererCompat.gameDirOf(versionPath);
            for (RendererCompat.Info info : RendererCompat.ALL) {
                boolean libOk = true;
                if (info.glName != null && !info.glName.isEmpty()) {
                    boolean inApk = new File(nativeDir, info.glName).isFile();
                    // 外部渲染器（mg）：优先从「已安装的插件应用」取（与 FCL 一致），
                    // 其次才是 <gameDir>/renderer/<id>/ 手工放置
                    boolean external = RendererCompat.resolveRendererLibDir(activity, info.id, versionPath, gameDir) != null;
                    libOk = inApk || external;
                }
                String line = info.displayName + "\n（" + info.supportRangeText() + "）"
                        + (info.recommended ? " ★推荐" : "")
                        + (info.id.equals(current) ? "  ✓当前" : "")
                        + (RendererCompat.supports(info.id, mcVer) ? "" : "  ⚠不支持当前版本")
                        + (libOk ? "" : "  ⚠缺少库文件");
                labels.add(line);
                ids.add(info.id);
            }

            new AlertDialog.Builder(activity)
                    .setTitle("选择渲染器" + (perVersion ? "（当前版本 " + (mcVer != null ? mcVer : "?") + "）" : "（全局设置）"))
                    .setItems(labels.toArray(new String[0]), (d, which) -> {
                        final String id = ids.get(which);
                        String warnText = RendererCompat.warningOf(id, mcVer);
                        // ★ mg 是外部渲染器：缺库时明确告诉玩家放哪里
                        if ("mg".equals(id) && RendererCompat.resolveRendererLibDir(activity, id, versionPath, gameDir) == null) {
                            String mgTip = "未检测到 MobileGlues 渲染器库（libmobileglues.so）。\n\n"
                                    + "【推荐】按官方方式安装（与 FCL 用法一致）：\n"
                                    + "1. 下载并安装 MobileGlues 插件 APK：\n"
                                    + "     github.com/MobileGL-Dev/MobileGlues-release/releases\n"
                                    + "     （选文件名以 .apk 结尾的那个）\n"
                                    + "2. 装好后【打开一次】该应用，允许存储权限（会创建 /MG 目录）\n"
                                    + "3. 回到本列表重新打开即可选中 MobileGlues\n"
                                    + "     （QCL 每次打开都会重新扫描，无需重启启动器）\n\n"
                                    + "【兜底】也可以把 libmobileglues.so 手动放到：\n"
                                    + (gameDir != null ? gameDir : "<游戏目录>") + "/renderer/mg/\n\n"
                                    + "要求：设备需支持 OpenGL ES 3.0 以上（3.2 推荐）。\n\n"
                                    + "【玩 26.3 / 26.3-snapshot-3 及更新版本】\n"
                                    + "MobileGlues 官方说明：必须在 MobileGlues 应用里开启\n"
                                    + "「Ignore shader/program error」（忽略着色器/程序错误），\n"
                                    + "否则 26.3 及之后的版本会因着色器报错而无法进入世界。";
                            warnText = (warnText == null) ? mgTip : (mgTip + "\n\n" + warnText);
                        }
                        if (warnText != null) {
                            new AlertDialog.Builder(activity)
                                    .setTitle("渲染器兼容性提示")
                                    .setMessage(warnText)
                                    .setPositiveButton("我就要用这个渲染器", (d2, w2) -> {
                                        apply(activity, pgs, effPath, id);
                                        if (onChanged != null) onChanged.run();
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        } else {
                            apply(activity, pgs, effPath, id);
                            if (onChanged != null) onChanged.run();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (Throwable e) {
            Toast.makeText(activity, "打开渲染器选择失败: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /** 写入设置并提示。 */
    public static void apply(Activity activity, PrivateGameSetting pgs, String versionPath, String id) {
        try {
            // ★★★ 1.4.9 修复「长按启动按钮切换后，全局设置也被改了 / 版本设置不刷新」
            //   原来这里**无条件**把 id 写进内存里的全局对象：
            //       pgs.pojavLauncherSetting.renderer = id;
            //   `pgs` 是 activity.privateGameSetting —— **全局内存对象**。
            //   于是：
            //     · 只要在版本级入口（长按启动按钮 / 版本设置）切一次渲染器，
            //       全局设置对象就被污染 →「全局游戏设置怎么也变成我切的那个渲染器」；
            //     · 但真正落盘写的是该版本的 qcl.cfg → 版本设置页读 qcl.cfg 时
            //       又对不上内存里的值，表现为「版本设置往下滑，渲染器没同步」。
            //   现在：**只有「全局入口」才动内存里的全局对象**。
            //   perVersion=false（全局游戏设置页）→ 这里同步，界面即时刷新。
            //   perVersion=true（版本级入口）→ 绝碰全局对象，界面靠 refreshRendererText()
            //     重新读 qcl.cfg 显示。
            PrivateGameSetting target = pgs;
            String savePath = AppManifest.SETTING_DIR + "/private_game_setting.json";
            final boolean perVersion;
            try {
                // 由调用方决定：show(activity, pgs, versionPath, onChanged) 视为版本级
                perVersion = versionPath != null && !versionPath.isEmpty()
                        && new File(versionPath).isDirectory();
            } catch (Throwable ignoredDir) {
                return;
            }
            if (!perVersion && pgs != null && pgs.pojavLauncherSetting != null) {
                pgs.pojavLauncherSetting.renderer = id;
            }
            // ★★★ 2026-10-01 修复「独立设置版本切换渲染器不生效」：
            //   启动时（MainUI.startGame / GameManagerUI.testGame）若版本开了独立设置
            //   （存在 <版本目录>/qcl.cfg 且 enable/forceEnable=true），读的是 qcl.cfg 而非全局。
            //   此前 apply() 恒写全局 private_game_setting.json → 独立设置版本里切换渲染器
            //   写到了全局、启动却读 qcl.cfg（旧渲染器）→「切回默认，进游戏还是旧渲染器」。
            //   现在保存路径与启动读取路径对齐：qcl.cfg 优先，否则全局。
            //   （target / savePath / perVersion 已在上面声明）

            // ★★★ 1.4.9 新增「按版本真正隔开渲染器」：
            //   玩家在**版本独立设置页**给某个版本选渲染器，期望的语义是"就这个版本用这个渲染器，
            //   别动我其他版本"。而旧逻辑在该版本还没开独立设置（无 qcl.cfg / enable=false）时
            //   会退回写全局 → 于是「A 版本选 Zink」把**所有**版本都改了，B 版本也被带着变，
            //   表现就是"渲染器根本没按版本隔开"。
            //   现在：只要带了 versionPath（即来自版本级入口，不是全局设置页），就一律落到该版本的
            //   qcl.cfg，并**自动打开独立设置开关**（enable=true），这样启动时的读取路径
            //   （GameManagerUI / MainUI 都是「qcl.cfg 且 enable/forceEnable 才生效」）才认这份值。
            //   · enable 之前会从全局复制一份作为基线，避免该版本其它设置（JVM 参数/内存/游戏目录）
            //     变成 null 或默认值。
            //   · 全局设置页那条入口不传 versionPath 语义（传 null 或非版本目录），保持写全局不变。
            if (perVersion) {
                String qclCfg = versionPath + "/qcl.cfg";
                PrivateGameSetting v = GsonUtils.getPrivateGameSettingFromFile(qclCfg);
                boolean vActive = v != null && (v.forceEnable || v.enable);
                if (!vActive) {
                    // 该版本尚未启用独立设置 → 以全局设置为基线新建一份，并打开开关
                    PrivateGameSetting base = (pgs != null) ? pgs
                            : GsonUtils.getPrivateGameSettingFromFile(
                                    AppManifest.SETTING_DIR + "/private_game_setting.json");
                    if (base != null) {
                        v = base;
                    } else {
                        // 连全局设置都还没有 → 造一份最小可用设置（各子对象都要非 null，
                        // 否则启动路径读 javaSetting / ramSetting 等会 NPE）
                        v = new PrivateGameSetting(
                                false, true, false, false, false, false, false,
                                new com.qcl.launcher.launcher.setting.game.child.JavaSetting(true, "default"),
                                "", "", "",
                                new com.qcl.launcher.launcher.setting.game.child.GameDirSetting(1, ""),
                                new com.qcl.launcher.launcher.setting.game.child.BoatLauncherSetting(false, "default", "default"),
                                new PojavLauncherSetting(true, id, "default"),
                                new com.qcl.launcher.launcher.setting.game.child.RamSetting(0, 0, true),
                                "default", 0.25f);
                    }
                    if (v.pojavLauncherSetting == null) {
                        v.pojavLauncherSetting = new PojavLauncherSetting(true, id, "default");
                    } else {
                        v.pojavLauncherSetting.renderer = id;
                    }
                    v.forceEnable = false;
                    v.enable = true;
                    if (v.gameDirSetting == null) {
                        // 该版本新建独立设置时，游戏目录默认跟随「版本隔离」（与新装版本一致）
                        v.gameDirSetting = new com.qcl.launcher.launcher.setting.game.child.GameDirSetting(1, "");
                    }
                } else if (v.pojavLauncherSetting == null) {
                    v.pojavLauncherSetting = new PojavLauncherSetting(true, id, "default");
                } else {
                    v.pojavLauncherSetting.renderer = id;
                }
                target = v;
                savePath = qclCfg;
            }

            GsonUtils.savePrivateGameSetting(target, savePath);
            Toast.makeText(activity, "渲染器已切换: " + displayNameOf(id)
                            + (savePath.endsWith("qcl.cfg") ? "（仅本版本生效）" : "（全局）"),
                    Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }
}
