package com.qcl.launcher.launcher.setting.launcher;

import com.qcl.launcher.launcher.setting.launcher.child.BackgroundSetting;
import com.qcl.launcher.launcher.setting.launcher.child.SourceSetting;

/* loaded from: classes2.dex */
public class LauncherSetting {
    /** ★ 1.2.3：界面背景是否透明（默认全透明；关掉恢复原来的灰色面板） */
    public boolean transparentBackground = true;
    public boolean autoCheckUpdate;
    public boolean autoDownloadTaskQuantity;
    public String cachePath;
    public SourceSetting downloadUrlSource;
    public boolean fullscreen;
    public String gameFileDirectory;
    public boolean getBetaVersion;
    public int language;
    public BackgroundSetting launcherBackground;
    public String launcherTheme;
    public int maxDownloadTask;
    public String panelColor;
    public boolean transBar;
    /** 1.3.7: 主界面是否显示账号人物（默认开） */
    public boolean showAccountModel = true;
    /**
     * ★★★ 1.5.0：**排版风格**。0 = 旧排版（顶部横排入口），1 = 新排版（左侧竖向导航）。
     *
     * <p>★ 注意它**不能单独看** —— 必须配合 {@link #uiStyleChosen}：
     * {@code LauncherSetting} 没有无参构造，Gson 反序列化走的是 Unsafe 分配，
     * **字段初始值不会执行**，老玩家的 json 里没这个键 → 读出来就是 0（= 旧排版）。
     * 所以用 {@link #uiStyleChosen} 区分「玩家真的选了旧排版」和「老配置没这个键」。
     *
     * <p>取用请一律走 {@link #useNewLayout()}，不要直接读这个字段。
     */
    public int uiStyle;
    /** ★ 1.5.0：玩家是否**真的**在「外观」里选过排版。老配置里没这个键 → false。 */
    public boolean uiStyleChosen;

    /**
     * ★ 1.5.0：是否使用新排版。
     * 老配置（{@code uiStyleChosen=false}）→ **默认新排版**（1.5.0 的主打界面）；
     * 玩家手动选过 → 以 {@link #uiStyle} 为准。
     */
    public boolean useNewLayout() {
        return !this.uiStyleChosen || this.uiStyle == 1;
    }

    /**
     * ★ 1.4.5：玩家累计启动游戏的次数（每次点「启动游戏」+1，存进 launcher_setting.json）。
     */
    public int gameLaunchCount;
    /**
     * ★ 1.4.5：上一次弹「启动次数提示」时的里程碑（20 / 60 / 80 / 100 …）。
     * 用它保证同一个里程碑**只弹一次**，玩家点过就不会反复烦他。
     */
    public int lastLaunchPromptAt;

    public LauncherSetting(String str, SourceSetting sourceSetting, int i, int i2, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, String str2, String str3, BackgroundSetting backgroundSetting, String str4) {
        this.gameFileDirectory = str;
        this.downloadUrlSource = sourceSetting;
        this.language = i;
        this.maxDownloadTask = i2;
        this.autoDownloadTaskQuantity = z;
        this.autoCheckUpdate = z2;
        this.getBetaVersion = z3;
        this.fullscreen = z4;
        this.transBar = z5;
        this.launcherTheme = str2;
        this.panelColor = str3;
        this.launcherBackground = backgroundSetting;
        this.cachePath = str4;
        // ★ 1.5.0：原来这个构造末尾还有第 14 个参数 i3 → this.uiTheme（草方块主题）。
        //   草方块 UI 已下线，14 参构造整条删掉；调用方（InitializeSetting）用的是这个
        //   13 参版本，没有别的调用点。uiStyle 的默认值交给 useNewLayout() 处理。
    }
}
