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
    public int uiTheme;

    public LauncherSetting(String str, SourceSetting sourceSetting, int i, int i2, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, String str2, String str3, BackgroundSetting backgroundSetting, String str4) {
        this(str, sourceSetting, i, i2, z, z2, z3, z4, z5, str2, str3, backgroundSetting, str4, 0);
    }

    public LauncherSetting(String str, SourceSetting sourceSetting, int i, int i2, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, String str2, String str3, BackgroundSetting backgroundSetting, String str4, int i3) {
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
        this.uiTheme = i3;
    }
}
