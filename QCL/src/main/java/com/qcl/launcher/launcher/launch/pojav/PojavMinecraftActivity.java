/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.app.Activity
 *  android.content.Context
 *  android.content.Intent
 *  android.content.res.Configuration
 *  android.graphics.SurfaceTexture
 *  android.os.Build$VERSION
 *  android.os.Bundle
 *  android.util.DisplayMetrics
 *  android.util.Log
 *  android.view.InputDevice
 *  android.view.KeyEvent
 *  android.view.Surface
 *  android.view.View
 *  android.view.ViewGroup
 *  android.view.ViewGroup$LayoutParams
 *  android.widget.FrameLayout
 *  android.widget.FrameLayout$LayoutParams
 *  android.widget.Toast
 *  androidx.annotation.Nullable
 *  androidx.appcompat.app.AppCompatActivity
 *  net.kdt.pojavlaunch.BaseMainActivity
 *  net.kdt.pojavlaunch.function.PojavCallback
 *  net.kdt.pojavlaunch.utils.JREUtils
 *  org.lwjgl.glfw.CallbackBridge
 */
package com.qcl.launcher.launcher.launch.pojav;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.qcl.launcher.control.InputBridge;
import com.qcl.launcher.control.MenuHelper;
import com.qcl.launcher.control.view.LayoutPanel;
import com.qcl.launcher.launcher.launch.GameFrameProbe;
import com.qcl.launcher.launcher.launch.LaunchLogWindow;
import com.qcl.launcher.launcher.launch.MCOptionUtils;
import com.qcl.launcher.launcher.launch.pojav.PojavLauncher;
import com.qcl.launcher.launcher.setting.game.GameLaunchSetting;
import com.qcl.launcher.launcher.terracotta.TerracottaHelper;
import com.qcl.launcher.utils.LocaleUtils;
import java.util.Vector;
import net.kdt.pojavlaunch.BaseMainActivity;
import net.kdt.pojavlaunch.function.PojavCallback;
import net.kdt.pojavlaunch.utils.JREUtils;
import org.lwjgl.glfw.CallbackBridge;

import com.qcl.launcher.R;
public class PojavMinecraftActivity
extends BaseMainActivity {
    private GameLaunchSetting gameLaunchSetting;
    private FrameLayout drawerLayout;
    private LayoutPanel baseLayout;
    private GameFrameProbe frameProbe;
    public MenuHelper menuHelper;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.gameLaunchSetting = GameLaunchSetting.getGameLaunchSetting(this.getIntent().getExtras().getString("setting_path"), this.getIntent().getExtras().getString("version"));
        if (this.getIntent().getExtras().getBoolean("test") || this.gameLaunchSetting.log) {
            // empty if block
        }
        if (Build.VERSION.SDK_INT >= 28) {
            this.getWindow().getAttributes().layoutInDisplayCutoutMode = this.gameLaunchSetting.fullscreen ? 1 : 2;
        }
        this.getWindow().setFlags(256, 256);
        this.setContentView(R.layout.activity_pojav);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -1);
        this.drawerLayout = (FrameLayout)this.getLayoutInflater().inflate(R.layout.activity_control_pattern, null);
        this.addContentView((View)this.drawerLayout, (ViewGroup.LayoutParams)params);
        this.baseLayout = (LayoutPanel)this.findViewById(R.id.base_layout);
        this.scaleFactor = this.gameLaunchSetting.scaleFactor;
        this.handleCallback();
        this.init(this.gameLaunchSetting.game_directory, GameLaunchSetting.isHighVersion(this.gameLaunchSetting));
        this.menuHelper = new MenuHelper((Context)this, (AppCompatActivity)this, this.gameLaunchSetting.fullscreen, this.gameLaunchSetting.game_directory, this.drawerLayout, this.baseLayout, false, this.gameLaunchSetting.controlLayout, 2, this.scaleFactor);
        LaunchLogWindow.GameLaunchSettingInfo info = new LaunchLogWindow.GameLaunchSettingInfo();
        info.backend = "Pojav";
        info.version = this.gameLaunchSetting.currentVersion;
        info.javaRuntime = this.gameLaunchSetting.javaPath;
        info.renderer = this.gameLaunchSetting.pojavRenderer;
        info.ramMb = this.gameLaunchSetting.maxRam;
        LaunchLogWindow.setBasics(info);
        new LaunchLogWindow((Activity)this, (ViewGroup)this.drawerLayout).show(info);
    }

    private void startFrameProbe() {
        if (this.frameProbe == null) {
            this.frameProbe = new GameFrameProbe(this.minecraftGLView, () -> this.baseLayout.hideBackground());
        }
        this.frameProbe.start();
    }

    private void stopFrameProbe() {
        if (this.frameProbe != null) {
            this.frameProbe.stop();
        }
    }

    public void handleCallback() {
        this.pojavCallback = new PojavCallback(){

            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                int usableWidth = width;
                int usableHeight = height;
                if (usableWidth < 64 || usableHeight < 64) {
                    DisplayMetrics dm = PojavMinecraftActivity.this.getResources().getDisplayMetrics();
                    usableWidth = dm.widthPixels;
                    usableHeight = dm.heightPixels;
                }
                CallbackBridge.windowWidth = (int)((float)usableWidth * PojavMinecraftActivity.this.scaleFactor);
                CallbackBridge.windowHeight = (int)((float)usableHeight * PojavMinecraftActivity.this.scaleFactor);
                surface.setDefaultBufferSize(CallbackBridge.windowWidth, CallbackBridge.windowHeight);
                CallbackBridge.sendUpdateWindowSize((int)CallbackBridge.windowWidth, (int)CallbackBridge.windowHeight);
                MCOptionUtils.load(((PojavMinecraftActivity)PojavMinecraftActivity.this).gameLaunchSetting.game_directory);
                MCOptionUtils.set("overrideWidth", String.valueOf(CallbackBridge.windowWidth));
                MCOptionUtils.set("overrideHeight", String.valueOf(CallbackBridge.windowHeight));
                if (GameLaunchSetting.isHighVersion(PojavMinecraftActivity.this.gameLaunchSetting)) {
                    MCOptionUtils.set("fullscreen", "false");
                }
                MCOptionUtils.save(((PojavMinecraftActivity)PojavMinecraftActivity.this).gameLaunchSetting.game_directory);
                int argWidth = usableWidth;
                int argHeight = usableHeight;
                new Thread(() -> {
                    Vector<String> args = PojavLauncher.getMcArgs(PojavMinecraftActivity.this.gameLaunchSetting, (Context)PojavMinecraftActivity.this, (int)((float)argWidth * PojavMinecraftActivity.this.scaleFactor), (int)((float)argHeight * PojavMinecraftActivity.this.scaleFactor), ((PojavMinecraftActivity)PojavMinecraftActivity.this).gameLaunchSetting.server);
                    if (args == null) {
                        PojavMinecraftActivity.this.runOnUiThread(() -> {
                            try {
                                Toast.makeText((Context)PojavMinecraftActivity.this, (CharSequence)"\u542f\u52a8\u5931\u8d25\uff1a\u8fd0\u884c\u5e93\u6216\u7248\u672c\u6587\u4ef6\u4e0d\u5b8c\u6574\uff0c\u8be6\u60c5\u89c1\u542f\u52a8\u65e5\u5fd7", (int)1).show();
                            }
                            catch (Throwable throwable) {
                                // empty catch block
                            }
                            PojavMinecraftActivity.this.finish();
                        });
                        return;
                    }
                    PojavMinecraftActivity.this.runOnUiThread(() -> {
                        Surface nativeSurface = new Surface(surface);
                        // ★★★ 1.1.1 SDL3 集成：绑定 SDL surface（非 SDL 版本无副作用，SDL3 版本必须）
                        try {
                            org.libsdl.app.SdlBridge.prepareSurface(PojavMinecraftActivity.this,
                                    nativeSurface, null, PojavMinecraftActivity.this);
                        } catch (Throwable ignored) {
                        }
                        JREUtils.setupBridgeWindowNew(nativeSurface);
                        PojavMinecraftActivity.this.startGame(((PojavMinecraftActivity)PojavMinecraftActivity.this).gameLaunchSetting.javaPath, ((PojavMinecraftActivity)PojavMinecraftActivity.this).gameLaunchSetting.home, GameLaunchSetting.isHighVersion(PojavMinecraftActivity.this.gameLaunchSetting), args, ((PojavMinecraftActivity)PojavMinecraftActivity.this).gameLaunchSetting.pojavRenderer, ((PojavMinecraftActivity)PojavMinecraftActivity.this).gameLaunchSetting.game_directory, PojavLauncher.getGlVersion(((PojavMinecraftActivity)PojavMinecraftActivity.this).gameLaunchSetting.currentVersion));
                    });
                }).start();
            }

            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                CallbackBridge.windowWidth = (int)((float)width * PojavMinecraftActivity.this.scaleFactor);
                CallbackBridge.windowHeight = (int)((float)height * PojavMinecraftActivity.this.scaleFactor);
                surface.setDefaultBufferSize(CallbackBridge.windowWidth, CallbackBridge.windowHeight);
                CallbackBridge.sendUpdateWindowSize((int)CallbackBridge.windowWidth, (int)CallbackBridge.windowHeight);
            }

            public void onCursorModeChange(int mode) {
                if (PojavMinecraftActivity.this.menuHelper != null) {
                    if (mode == 1) {
                        PojavMinecraftActivity.this.menuHelper.enableCursor();
                    } else {
                        PojavMinecraftActivity.this.menuHelper.disableCursor();
                    }
                }
            }

            public void onStart() {
                PojavMinecraftActivity.this.baseLayout.showBackground();
                PojavMinecraftActivity.this.startFrameProbe();
                PojavMinecraftActivity.this.resetPicOutputFlag();
            }

            public void onPicOutput() {
                Log.i((String)"jrelog", (String)"[\u753b\u9762\u5207\u6362] \u6536\u5230 onPicOutput\uff0c\u64a4\u9664\u7b49\u5f85\u754c\u9762");
                PojavMinecraftActivity.this.stopFrameProbe();
                PojavMinecraftActivity.this.baseLayout.hideBackground();
            }

            public void onError(Exception e) {
            }

            public void onExit(int code) {
            }
        };
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (this.menuHelper.gameMenuSetting.mousePatch && keyCode == 4) {
            InputBridge.sendMouseEvent(1, 1, true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (this.menuHelper.gameMenuSetting.mousePatch && keyCode == 4) {
            InputBridge.sendMouseEvent(1, 1, false);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public void onBackPressed() {
        int[] devices;
        boolean mouse = false;
        for (int j : devices = InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice((int)j);
            if (device == null || device.isVirtual() || !device.getName().contains("Mouse") && (this.menuHelper == null || this.menuHelper.touchCharInput == null || this.menuHelper.touchCharInput.isEnabled())) continue;
            if (Build.VERSION.SDK_INT >= 29 && device.isExternal()) {
                mouse = true;
                break;
            }
            if (Build.VERSION.SDK_INT >= 29) continue;
            mouse = true;
            break;
        }
        if (!mouse) {
            CallbackBridge.sendKeyPress((int)256);
        }
    }

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtils.setLanguage(base));
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleUtils.setLanguage((Context)this);
    }

    protected void onPause() {
        if (this.menuHelper.viewManager != null && this.menuHelper.gameCursorMode == 1) {
            CallbackBridge.sendKeyPress((int)256);
        }
        super.onPause();
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        TerracottaHelper.onActivityResult((Activity)this, requestCode);
        if (this.menuHelper != null) {
            this.menuHelper.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onPostResume() {
        super.onPostResume();
        if (Build.VERSION.SDK_INT >= 28) {
            this.getWindow().getAttributes().layoutInDisplayCutoutMode = this.gameLaunchSetting.fullscreen ? 1 : 2;
        }
        this.getWindow().setFlags(256, 256);
    }
}

