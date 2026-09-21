package com.qcl.launcher.launcher.setting.game;

import com.qcl.launcher.launcher.setting.game.child.BoatLauncherSetting;
import com.qcl.launcher.launcher.setting.game.child.GameDirSetting;
import com.qcl.launcher.launcher.setting.game.child.JavaSetting;
import com.qcl.launcher.launcher.setting.game.child.PojavLauncherSetting;
import com.qcl.launcher.launcher.setting.game.child.RamSetting;

/* loaded from: classes2.dex */
public class PrivateGameSetting implements Cloneable {
    /** ★ 1.2.3：远古版本的兼容 JVM 参数是否已预填过（只填一次，玩家删了不再自动补） */
    public boolean legacyJvmArgsFilled = false;
    public BoatLauncherSetting boatLauncherSetting;
    public String controlLayout;
    public boolean enable;
    public String extraJavaFlags;
    public String extraMinecraftFlags;
    public boolean forceEnable;
    public GameDirSetting gameDirSetting;
    public JavaSetting javaSetting;
    public boolean log;
    public boolean notCheckForge;
    public boolean notCheckJvm;
    public boolean notCheckMinecraft;
    public PojavLauncherSetting pojavLauncherSetting;
    public RamSetting ramSetting;
    public float scaleFactor;
    public String server;
    public boolean touchInjector;

    public PrivateGameSetting(boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7, JavaSetting javaSetting, String str, String str2, String str3, GameDirSetting gameDirSetting, BoatLauncherSetting boatLauncherSetting, PojavLauncherSetting pojavLauncherSetting, RamSetting ramSetting, String str4, float f) {
        this.forceEnable = z;
        this.enable = z2;
        this.log = z3;
        this.notCheckJvm = z4;
        this.notCheckMinecraft = z5;
        this.notCheckForge = z6;
        this.touchInjector = z7;
        this.javaSetting = javaSetting;
        this.extraJavaFlags = str;
        this.extraMinecraftFlags = str2;
        this.server = str3;
        this.gameDirSetting = gameDirSetting;
        this.boatLauncherSetting = boatLauncherSetting;
        this.pojavLauncherSetting = pojavLauncherSetting;
        this.ramSetting = ramSetting;
        this.controlLayout = str4;
        this.scaleFactor = f;
    }

    public static String getGameDir(String str, String str2, GameDirSetting gameDirSetting) {
        return gameDirSetting.type == 0 ? str : gameDirSetting.type == 1 ? str2 : gameDirSetting.path;
    }

    public Object clone() throws CloneNotSupportedException {
        PrivateGameSetting privateGameSetting = (PrivateGameSetting) super.clone();
        privateGameSetting.javaSetting = (JavaSetting) this.javaSetting.clone();
        privateGameSetting.gameDirSetting = (GameDirSetting) this.gameDirSetting.clone();
        privateGameSetting.boatLauncherSetting = (BoatLauncherSetting) this.boatLauncherSetting.clone();
        privateGameSetting.pojavLauncherSetting = (PojavLauncherSetting) this.pojavLauncherSetting.clone();
        privateGameSetting.ramSetting = (RamSetting) this.ramSetting.clone();
        return privateGameSetting;
    }
}
