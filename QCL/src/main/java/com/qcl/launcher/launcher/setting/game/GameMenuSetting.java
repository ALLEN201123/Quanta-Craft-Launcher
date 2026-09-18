package com.qcl.launcher.launcher.setting.game;

import com.google.gson.Gson;
import com.qcl.launcher.launcher.setting.game.child.MenuFloatSetting;
import com.qcl.launcher.launcher.setting.game.child.MenuViewSetting;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.file.FileStringUtils;
import java.io.File;

/* loaded from: classes2.dex */
public class GameMenuSetting {
    public static final int GAME_MENU_VERSION = 4;
    public boolean advanceInput;
    public boolean disableHalfScreen;
    public boolean enableSensor;
    public boolean enableTouch;
    public boolean hideLaunchLog;
    public boolean hideUI;
    public MenuFloatSetting menuFloatSetting;
    public boolean menuSlideSetting;
    public MenuViewSetting menuViewSetting;
    public int mouseMode;
    public boolean mousePatch;
    public int mouseSize;
    public float mouseSpeed;
    public int sensitivity;
    public int touchMode;
    public int version;

    public GameMenuSetting(MenuFloatSetting menuFloatSetting, MenuViewSetting menuViewSetting, boolean z, boolean z2, boolean z3, boolean z4, int i, boolean z5, boolean z6, int i2, int i3, float f, int i4, boolean z7, int i5) {
        this.menuFloatSetting = menuFloatSetting;
        this.menuViewSetting = menuViewSetting;
        this.menuSlideSetting = z;
        this.enableTouch = z2;
        this.mousePatch = z3;
        this.enableSensor = z4;
        this.sensitivity = i;
        this.advanceInput = z5;
        this.disableHalfScreen = z6;
        this.touchMode = i2;
        this.mouseMode = i3;
        this.mouseSpeed = f;
        this.mouseSize = i4;
        this.hideUI = z7;
        this.version = i5;
    }

    public static GameMenuSetting getGameMenuSetting() {
        String str = AppManifest.SETTING_DIR + "/game_menu_setting.json";
        if (!new File(str).exists()) {
            GameMenuSetting gameMenuSetting = new GameMenuSetting(new MenuFloatSetting(true, true, 0.5f, 0.5f), new MenuViewSetting(true, 0, 0.2f), true, true, false, false, 10, false, false, 0, 0, 1.0f, 16, false, 4);
            saveGameMenuSetting(gameMenuSetting);
            return gameMenuSetting;
        }
        GameMenuSetting gameMenuSetting2 = (GameMenuSetting) new Gson().fromJson(FileStringUtils.getStringFromFile(str), GameMenuSetting.class);
        if (gameMenuSetting2.version == 0) {
            gameMenuSetting2.enableTouch = true;
            gameMenuSetting2.mousePatch = false;
            gameMenuSetting2.enableSensor = false;
            gameMenuSetting2.sensitivity = 10;
            gameMenuSetting2.disableHalfScreen = false;
            gameMenuSetting2.touchMode = 0;
            gameMenuSetting2.mouseMode = 0;
            gameMenuSetting2.mouseSpeed = 1.0f;
            gameMenuSetting2.mouseSize = 16;
            gameMenuSetting2.hideUI = false;
            gameMenuSetting2.version = 4;
            saveGameMenuSetting(gameMenuSetting2);
        }
        if (gameMenuSetting2.version == 1) {
            gameMenuSetting2.sensitivity = 10;
            gameMenuSetting2.enableTouch = true;
            gameMenuSetting2.mousePatch = false;
            gameMenuSetting2.version = 4;
            saveGameMenuSetting(gameMenuSetting2);
        }
        if (gameMenuSetting2.version == 2) {
            gameMenuSetting2.enableTouch = true;
            gameMenuSetting2.mousePatch = false;
            gameMenuSetting2.version = 4;
            saveGameMenuSetting(gameMenuSetting2);
        }
        if (gameMenuSetting2.version != 3) {
            return gameMenuSetting2;
        }
        gameMenuSetting2.mousePatch = false;
        gameMenuSetting2.version = 4;
        saveGameMenuSetting(gameMenuSetting2);
        return gameMenuSetting2;
    }

    public static void saveGameMenuSetting(GameMenuSetting gameMenuSetting) {
        FileStringUtils.writeFile(AppManifest.SETTING_DIR + "/game_menu_setting.json", new Gson().toJson(gameMenuSetting));
    }
}
