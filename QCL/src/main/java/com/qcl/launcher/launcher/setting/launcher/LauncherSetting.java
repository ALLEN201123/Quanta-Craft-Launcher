package com.qcl.launcher.launcher.setting.launcher;

import com.qcl.launcher.launcher.setting.launcher.child.BackgroundSetting;
import com.qcl.launcher.launcher.setting.launcher.child.SourceSetting;

public class LauncherSetting {

    public String gameFileDirectory;
    public SourceSetting downloadUrlSource;
    public int language;
    public int maxDownloadTask;
    public boolean autoDownloadTaskQuantity;
    public boolean autoCheckUpdate;
    public boolean getBetaVersion;
    public boolean fullscreen;
    public boolean transBar;
    public String launcherTheme;
    public String panelColor;
    public BackgroundSetting launcherBackground;
    public String cachePath;
    /** UI 风格：0 = 默认（灰白），1 = 草方块（复古像素风）。1.0.5 新增。 */
    public int uiTheme;

    public LauncherSetting(String gameFileDirectory,SourceSetting downloadUrlSource,int language,int maxDownloadTask,boolean autoDownloadTaskQuantity,boolean autoCheckUpdate,boolean getBetaVersion,boolean fullscreen,boolean transBar,String launcherTheme,String panelColor,BackgroundSetting launcherBackground,String cachePath){
        this(gameFileDirectory, downloadUrlSource, language, maxDownloadTask, autoDownloadTaskQuantity, autoCheckUpdate, getBetaVersion, fullscreen, transBar, launcherTheme, panelColor, launcherBackground, cachePath, 0);
    }

    public LauncherSetting(String gameFileDirectory,SourceSetting downloadUrlSource,int language,int maxDownloadTask,boolean autoDownloadTaskQuantity,boolean autoCheckUpdate,boolean getBetaVersion,boolean fullscreen,boolean transBar,String launcherTheme,String panelColor,BackgroundSetting launcherBackground,String cachePath,int uiTheme){
        this.gameFileDirectory = gameFileDirectory;
        this.downloadUrlSource = downloadUrlSource;
        this.language = language;
        this.maxDownloadTask = maxDownloadTask;
        this.autoDownloadTaskQuantity = autoDownloadTaskQuantity;
        this.autoCheckUpdate = autoCheckUpdate;
        this.getBetaVersion = getBetaVersion;
        this.fullscreen = fullscreen;
        this.transBar = transBar;
        this.launcherTheme = launcherTheme;
        this.panelColor = panelColor;
        this.launcherBackground = launcherBackground;
        this.cachePath = cachePath;
        this.uiTheme = uiTheme;
    }

}
