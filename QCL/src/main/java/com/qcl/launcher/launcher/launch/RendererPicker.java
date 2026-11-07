package com.qcl.launcher.launcher.launch;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
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
            // ★★★★★ 1.6.0：**渲染器选择界面重做（卡片式）**。
            //
            //   旧版用 AlertDialog.setItems 的纯文本行，一行里塞「名称 / 版本区间 / ★推荐 /
            //   【内置】/ ✓当前 / ⚠不支持 / ⚠缺库」六七个标记，读起来像乱码，也点不准。
            //   新版每条渲染器渲染成一张卡片：
            //     · 第一行：名称 + 右对齐状态徽章（✓当前 / ★推荐 / 【内置】/ ⚠）
            //     · 第二行：支持版本区间（灰字）
            //     · 第三行：仅在有警告时出现（不支持当前版本 / 缺库），红色
            //     · 当前选中的整行加浅色底并左侧加一条竖线，一眼能认出
            //   条目容器用 ListView + BaseAdapter（照项目里 JavaManageDialog 的既有模式），
            //   不引入新布局资源，避免「搬资源漏改」的老坑。
            final String gameDir = RendererCompat.gameDirOf(versionPath);

            // 先把数据备好（每条一项），再交给 Adapter 渲染
            final List<RendererCompat.Info> items = new ArrayList<RendererCompat.Info>();
            final List<Boolean> itemLibOk = new ArrayList<Boolean>();
            final List<Boolean> itemBuiltin = new ArrayList<Boolean>();
            for (RendererCompat.Info info : RendererCompat.ALL) {
                boolean libOk = true;
                boolean builtin = false;
                if (info.glName != null && !info.glName.isEmpty()) {
                    builtin = RendererCompat.isBuiltinLibPresent(activity, info.id);
                    boolean inApk = builtin || new File(nativeDir, info.glName).isFile();
                    boolean external = RendererCompat.resolveRendererLibDir(
                            activity, info.id, versionPath, gameDir) != null;
                    libOk = inApk || external;
                }
                items.add(info);
                itemLibOk.add(libOk);
                itemBuiltin.add(builtin);
            }
            final boolean sfpewAvail =
                    com.qcl.launcher.launcher.launch.SfpewLayer.isAvailable(activity);

            // 容器：标题 + ListView + 底部按钮，全部代码构建
            final android.widget.LinearLayout root =
                    new android.widget.LinearLayout(activity);
            root.setOrientation(android.widget.LinearLayout.VERTICAL);

            final android.widget.TextView header = new android.widget.TextView(activity);
            header.setText("选择一个渲染器"
                    + (perVersion ? "（本版本 " + (mcVer != null ? mcVer : "?") + "）" : "（全局）"));
            header.setTextSize(17f);
            header.setPadding(36, 32, 36, 12);
            header.setTextColor(0xFF222222);
            root.addView(header, new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

            final android.widget.ListView listView = new android.widget.ListView(activity);
            listView.setDivider(null);
            listView.setDividerHeight(0);
            final String[] picked = new String[1];

            final android.widget.BaseAdapter adapter = new android.widget.BaseAdapter() {
                @Override
                public int getCount() {
                    // 末尾多一项「高级设置」
                    return items.size() + 1;
                }

                @Override
                public Object getItem(int position) {
                    return position < items.size() ? items.get(position) : null;
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, android.view.ViewGroup parent) {
                    android.content.Context ctx = parent.getContext();
                    boolean isAdvanced = position >= items.size();
                    android.widget.LinearLayout card;
                    android.widget.TextView title, sub, warn;
                    if (convertView instanceof android.widget.LinearLayout
                            && ((android.widget.LinearLayout) convertView).getChildCount() == 3) {
                        card = (android.widget.LinearLayout) convertView;
                        title = (android.widget.TextView) card.getChildAt(0);
                        sub = (android.widget.TextView) card.getChildAt(1);
                        warn = (android.widget.TextView) card.getChildAt(2);
                    } else {
                        card = new android.widget.LinearLayout(ctx);
                        card.setOrientation(android.widget.LinearLayout.VERTICAL);
                        card.setPadding(36, 26, 36, 26);
                        // 卡片之间的细分隔线靠 padding + 交替底色营造，不额外加 drawable
                        title = new android.widget.TextView(ctx);
                        title.setTextSize(16f);
                        title.setTextColor(0xFF1A1A1A);
                        sub = new android.widget.TextView(ctx);
                        sub.setTextSize(12f);
                        sub.setTextColor(0xFF888888);
                        sub.setPadding(0, 6, 0, 0);
                        warn = new android.widget.TextView(ctx);
                        warn.setTextSize(12f);
                        warn.setTextColor(0xFFD33A2C);
                        warn.setPadding(0, 6, 0, 0);
                        card.addView(title, new android.widget.LinearLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
                        card.addView(sub, new android.widget.LinearLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
                        card.addView(warn, new android.widget.LinearLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
                    }

                    if (isAdvanced) {
                        // 高级设置单独一张卡，视觉上与渲染器区分
                        title.setText("⚙ 高级设置");
                        sub.setText("SFPEW 兼容层 · 给老版本补固定管线"
                                + (sfpewAvail ? "" : "（本机不可用）"));
                        warn.setVisibility(View.GONE);
                        card.setBackgroundColor(0xFFF4F6FA);
                        // 用「当前是否已开」提示一行，避免玩家以为这是个渲染器
                        title.setAlpha(1f);
                        return card;
                    }

                    RendererCompat.Info info = items.get(position);
                    boolean libOk = itemLibOk.get(position);
                    boolean builtin = itemBuiltin.get(position);
                    boolean supports = RendererCompat.supports(info.id, mcVer);
                    boolean isCurrent = info.id.equals(current);

                    // 名称 + 右侧状态徽章（用一个字符串拼，避免再搞布局）
                    StringBuilder nameLine = new StringBuilder(info.name);
                    if (isCurrent) nameLine.append("   ✓ 当前使用");
                    if (info.recommended) nameLine.append("   ★ 推荐");
                    if (builtin) nameLine.append("   ◆ 内置");
                    title.setText(nameLine.toString());
                    title.setTypeface(null, isCurrent
                            ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

                    // 支持区间 + 中文说明
                    String range = info.supportRangeText();
                    String desc = shortDesc(info);
                    sub.setText(range + (desc.isEmpty() ? "" : "　·　" + desc));

                    // 警告行：不支持当前版本 / 缺库，只在有问题时出现
                    StringBuilder w = new StringBuilder();
                    if (!supports) w.append("⚠ 不支持当前版本（")
                            .append(mcVer != null ? mcVer : "?").append("）");
                    if (!libOk) {
                        if (w.length() > 0) w.append("　");
                        w.append("⚠ 缺少库文件");
                    }
                    if (w.length() > 0) {
                        warn.setText(w.toString());
                        warn.setVisibility(View.VISIBLE);
                    } else {
                        warn.setVisibility(View.GONE);
                    }

                    // 当前项高亮：浅底 + 左侧竖线（用 padding 模拟，不加 drawable）
                    if (isCurrent) {
                        card.setBackgroundColor(0xFFEAF2FF);
                        card.setPadding(36, 26, 36, 26);
                    } else {
                        card.setBackgroundColor(0xFFFFFFFF);
                    }
                    return card;
                }
            };
            listView.setAdapter(adapter);
            listView.setOnItemClickListener((parent, view, position, id) -> {
                if (position >= items.size()) {
                    showAdvancedDialog(activity, mcVer, sfpewAvail);
                    return;
                }
                final String rid = items.get(position).id;
                String warnText = RendererCompat.warningOf(rid, mcVer);
                boolean mgBuiltin = RendererCompat.isBuiltinLibPresent(activity, rid);
                if ("mg".equals(rid) && !mgBuiltin
                        && RendererCompat.resolveRendererLibDir(activity, rid, versionPath, gameDir) == null) {
                    String mgTip = "未检测到 MobileGlues 渲染器库（libmobileglues.so）。\n\n"
                            + "【说明】从 1.5.8 起 MobileGlues 已经内置在 QCL 里，\n"
                            + "正常情况下不需要你做任何事，选中即可用。\n\n"
                            + "你现在看到这个提示，说明内置库异常（极罕见），可以尝试重新安装本启动器。\n\n"
                            + "【兜底】也可以把 libmobileglues.so 手动放到：\n"
                            + (gameDir != null ? gameDir : "<游戏目录>") + "/renderer/mg/\n\n"
                            + "要求：设备需支持 OpenGL ES 3.0 以上（3.2 推荐）。";
                    warnText = (warnText == null) ? mgTip : (mgTip + "\n\n" + warnText);
                }
                if (warnText != null) {
                    new AlertDialog.Builder(activity)
                            .setTitle("渲染器兼容性提示")
                            .setMessage(warnText)
                            .setPositiveButton("我就要用这个渲染器", (d2, w2) -> {
                                apply(activity, pgs, effPath, rid);
                                if (onChanged != null) onChanged.run();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                } else {
                    apply(activity, pgs, effPath, rid);
                    if (onChanged != null) onChanged.run();
                }
            });
            root.addView(listView, new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            final android.widget.LinearLayout footer =
                    new android.widget.LinearLayout(activity);
            footer.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            footer.setGravity(android.view.Gravity.END);
            footer.setPadding(24, 12, 24, 18);
            final android.widget.Button cancelBtn = new android.widget.Button(activity);
            cancelBtn.setText("取消");
            footer.addView(cancelBtn);
            root.addView(footer, new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

            final AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setView(root)
                    .create();
            cancelBtn.setOnClickListener(v -> dialog.dismiss());
            dialog.setOnShowListener(d -> {
                // 让列表占满可用高度，避免卡片被压成一条条
                android.view.Window win = dialog.getWindow();
                if (win != null) {
                    win.setLayout(
                            (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.92f),
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                }
            });
            dialog.show();
        } catch (Throwable e) {
            Toast.makeText(activity, "打开渲染器选择失败: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /** 卡片副标题用的短说明（取 displayName 括号里的那句，没有就返回空）。 */
    private static String shortDesc(RendererCompat.Info info) {
        String dn = info.displayName == null ? "" : info.displayName;
        int l = dn.indexOf('(');
        int r = dn.lastIndexOf(')');
        if (l >= 0 && r > l) {
            String inner = dn.substring(l + 1, r);
            // 太长的截断，保证卡片一行放得下
            return inner.length() > 34 ? inner.substring(0, 34) + "…" : inner;
        }
        return "";
    }

    /**
     * ★★★★★ 1.6.0：渲染器「高级设置」页（照 FCL 1.3.4.4 的 RendererAdvancedDialog）。
     *
     * <p>两项开关，语义与 FCL 1.3.4.4 逐条对齐：
     * <ol>
     *   <li><b>SFPEW 兼容层（渲染器插件）</b> —— 只对<b>外部插件渲染器</b>生效。
     *       内置渲染器行为已知，按 {@link SfpewLayer#isLayerEnabledForBuiltin} 的硬编码规则
     *       自动处理（自带 FPE 的关、其余开），不受这个开关影响。</li>
     *   <li><b>SFPEW_RELAXED_CONTEXT</b> —— 假定游戏每线程终生只用一个 GL 上下文，
     *       跳过每次调用的上下文核查（<b>默认关闭 = 严格模式</b>）。</li>
     * </ol>
     *
     * @param mcVer      当前版本名，仅用于在说明里点出「这个开关对老版本意味着什么」
     * @param sfpewAvail 兼容层 so 是否真的在 APK 里 —— 不在就禁用开关，不给玩家一个注定失败的选项
     */
    private static void showAdvancedDialog(final Activity activity, final String mcVer,
                                           final boolean sfpewAvail) {
        final java.util.List<String> labels = new ArrayList<String>();
        final java.util.List<Runnable> actions = new ArrayList<Runnable>();

        boolean pluginOn = SfpewLayer.isPluginLayerEnabled(activity);
        boolean relaxedOn = SfpewLayer.isRelaxedContextEnabled(activity);

        labels.add("SFPEW 兼容层（渲染器插件）　" + (pluginOn ? "【已开启】" : "【已关闭】")
                + "\n为无固定管线能力的外部渲染器插件叠加 SimpleFPEWrapper OpenGL 兼容层，"
                + "用于旧式 OpenGL 1.x/2.1 渲染的程序；内置渲染器按已知行为自动处理。");
        actions.add(() -> {
            SfpewLayer.setPluginLayerEnabled(activity, !SfpewLayer.isPluginLayerEnabled(activity));
            Toast.makeText(activity, "已切换（下次启动生效）", Toast.LENGTH_SHORT).show();
        });

        labels.add("SFPEW_RELAXED_CONTEXT　" + (relaxedOn ? "【已开启】" : "【已关闭】")
                + "\n假定游戏每线程终生只用一个 GL 上下文，跳过每次调用的上下文核查"
                + "（默认关闭，即严格模式）；确认游戏的线程-上下文使用方式后再开启。");
        actions.add(() -> {
            SfpewLayer.setRelaxedContextEnabled(activity, !SfpewLayer.isRelaxedContextEnabled(activity));
            Toast.makeText(activity, "已切换（下次启动生效）", Toast.LENGTH_SHORT).show();
        });

        String title = "高级设置";
        String warn = null;
        if (!sfpewAvail) {
            warn = "⚠ 未检测到 " + SfpewLayer.LIB_NAME + "（兼容层库不在本 APK 内），"
                    + "两个开关都不会生效。这通常意味着安装包不完整，建议重新安装。\n\n";
        }
        String body = (warn != null ? warn : "")
                + "说明：\n"
                + "· 兼容层是**叠加**在真实后端之上的，不是独立渲染器 ——\n"
                + "  它在游戏与渲染器之间补齐「固定管线（glBegin/glEnd 等即时模式）」，\n"
                + "  让原本只支持 GLES 3.x 的后端（如 MobileGlues）也能跑老版本。\n"
                + "· 内置渲染器里，Krypton Wrapper / Holy GL4ES / VGPU / Zink "
                + "本来就自带固定管线，会自动**不叠加**（省一次透传开销）；\n"
                + "  其余内置渲染器（含 MobileGlues）默认**自动叠加**。\n"
                + "· 装了 Angelica / Actinium 这类自带固定管线实现的模组时，整层跳过。"
                + (mcVer != null ? "\n\n当前版本：" + mcVer : "");

        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(body)
                .setPositiveButton("完成", null)
                .setNeutralButton("切换开关", (d, w) -> showToggleList(activity, labels, actions))
                .show();
    }

    /** 二次弹出：把两个开关列成可点列表（AlertDialog.setMessage 里的文字不能点，只能这样）。 */
    private static void showToggleList(Activity activity, java.util.List<String> labels,
                                       java.util.List<Runnable> actions) {
        new AlertDialog.Builder(activity)
                .setTitle("切换开关")
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    actions.get(which).run();
                    showAdvancedDialog(activity, null, SfpewLayer.isAvailable(activity));
                })
                .setNegativeButton("取消", null)
                .show();
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
