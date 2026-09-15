package com.qcl.launcher.launcher.launch.pojav;

import static org.lwjgl.glfw.CallbackBridge.windowHeight;
import static org.lwjgl.glfw.CallbackBridge.windowWidth;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import com.qcl.launcher.R;
import com.qcl.launcher.control.InputBridge;
import com.qcl.launcher.control.MenuHelper;
import com.qcl.launcher.control.view.LayoutPanel;
import com.qcl.launcher.launcher.setting.game.GameLaunchSetting;

import net.kdt.pojavlaunch.BaseMainActivity;
import net.kdt.pojavlaunch.keyboard.LwjglGlfwKeycode;
import net.kdt.pojavlaunch.function.PojavCallback;
import net.kdt.pojavlaunch.utils.JREUtils;
import com.qcl.launcher.launcher.launch.MCOptionUtils;
import com.qcl.launcher.launcher.launch.LaunchLogWindow;
import com.qcl.launcher.utils.LocaleUtils;

import org.lwjgl.glfw.CallbackBridge;

import java.util.Vector;

public class PojavMinecraftActivity extends BaseMainActivity {

    private GameLaunchSetting gameLaunchSetting;

    private android.widget.FrameLayout drawerLayout;
    private LayoutPanel baseLayout;

    public MenuHelper menuHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameLaunchSetting = GameLaunchSetting.getGameLaunchSetting(getIntent().getExtras().getString("setting_path"),getIntent().getExtras().getString("version"));

        if (getIntent().getExtras().getBoolean("test") || gameLaunchSetting.log) {

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (gameLaunchSetting.fullscreen) {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            } else {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            }
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        setContentView(net.kdt.pojavlaunch.R.layout.activity_pojav);

        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT);

        drawerLayout = (android.widget.FrameLayout) getLayoutInflater().inflate(R.layout.activity_control_pattern,null) ;
        addContentView(drawerLayout,params);

        baseLayout = findViewById(R.id.base_layout);

        scaleFactor = gameLaunchSetting.scaleFactor;

        handleCallback();

        init(gameLaunchSetting.game_directory, GameLaunchSetting.isHighVersion(gameLaunchSetting));

        menuHelper = new MenuHelper(this,this,gameLaunchSetting.fullscreen,gameLaunchSetting.game_directory,drawerLayout,baseLayout,false,gameLaunchSetting.controlLayout,2,scaleFactor);
        // 启动日志悬浮窗：默认开启；退出游戏回主界面时随 Activity 销毁自动关闭
        // 默认显示（老存档里 log=false 也照样显示），游戏进主界面后自动关闭；× 可手动关
        // 1.0.6：带上基础启动信息（设备/后端/运行时/渲染器/内存），避免日志窗内容太少
        LaunchLogWindow.GameLaunchSettingInfo info = new LaunchLogWindow.GameLaunchSettingInfo();
        info.backend = "Pojav";
        info.version = gameLaunchSetting.currentVersion;
        info.javaRuntime = gameLaunchSetting.javaPath;
        info.renderer = gameLaunchSetting.pojavRenderer;
        info.ramMb = gameLaunchSetting.maxRam;
        // 登记基础信息：之后从悬浮窗开关打开日志窗也能带上
        LaunchLogWindow.setBasics(info);
        new LaunchLogWindow(this, drawerLayout).show(info);

    }

    public void handleCallback() {
        pojavCallback = new PojavCallback() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                // 布局未完成时 width/height 可能是 0 或极小值 → 兜底取屏幕尺寸
                // 注意：lambda 捕获要求 effectively final，故用新变量名
                int usableWidth = width;
                int usableHeight = height;
                if (usableWidth < 64 || usableHeight < 64) {
                    android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
                    usableWidth = dm.widthPixels;
                    usableHeight = dm.heightPixels;
                }
                CallbackBridge.windowWidth = (int) (usableWidth * scaleFactor);
                CallbackBridge.windowHeight = (int) (usableHeight * scaleFactor);
                surface.setDefaultBufferSize(CallbackBridge.windowWidth, CallbackBridge.windowHeight);
                CallbackBridge.sendUpdateWindowSize(windowWidth, windowHeight);

                MCOptionUtils.load(gameLaunchSetting.game_directory);
                MCOptionUtils.set("overrideWidth", String.valueOf(CallbackBridge.windowWidth));
                MCOptionUtils.set("overrideHeight", String.valueOf(CallbackBridge.windowHeight));
                // 远古版本（LWJGL2 时代）没有 fullscreen 键，写入未知键会破坏其 options 解析
                if (GameLaunchSetting.isHighVersion(gameLaunchSetting)) {
                    MCOptionUtils.set("fullscreen", "false");
                }
                MCOptionUtils.save(gameLaunchSetting.game_directory);

                final int argWidth = usableWidth;
                final int argHeight = usableHeight;
                new Thread(() -> {
                    Vector<String> args = PojavLauncher.getMcArgs(gameLaunchSetting, PojavMinecraftActivity.this, (int) (argWidth * scaleFactor), (int) (argHeight * scaleFactor), gameLaunchSetting.server);
                    if (args == null) {
                        // 启动参数构造失败（通常是 Java 运行库缺文件 / 版本 json 损坏）。
                        // 具体原因已由 PojavLauncher 写进启动日志窗；这里只负责别把进程打死，
                        // 并把玩家退回启动器，让他能换个运行时或版本重试。
                        runOnUiThread(() -> {
                            try {
                                android.widget.Toast.makeText(PojavMinecraftActivity.this,
                                        "启动失败：运行库或版本文件不完整，详情见启动日志",
                                        android.widget.Toast.LENGTH_LONG).show();
                            } catch (Throwable ignored) {
                            }
                            finish();
                        });
                        return;
                    }
                    runOnUiThread(() -> {
                        JREUtils.setupBridgeWindow(new Surface(surface));
                        startGame(gameLaunchSetting.javaPath,
                                gameLaunchSetting.home,
                                GameLaunchSetting.isHighVersion(gameLaunchSetting),
                                args,
                                gameLaunchSetting.pojavRenderer,
                                gameLaunchSetting.game_directory,
                                PojavLauncher.getGlVersion(gameLaunchSetting.currentVersion));
                    });
                }).start();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                CallbackBridge.windowWidth = (int) (width * scaleFactor);
                CallbackBridge.windowHeight = (int) (height * scaleFactor);
                surface.setDefaultBufferSize(CallbackBridge.windowWidth, CallbackBridge.windowHeight);
                CallbackBridge.sendUpdateWindowSize(windowWidth, windowHeight);
            }

            @Override
            public void onCursorModeChange(int mode) {
                if (menuHelper != null) {
                    if (mode == 1){
                        menuHelper.enableCursor();
                    }
                    else {
                        menuHelper.disableCursor();
                    }
                }
            }

            @Override
            public void onStart() {
                baseLayout.showBackground();
            }

            @Override
            public void onPicOutput() {
                baseLayout.hideBackground();
            }

            @Override
            public void onError(Exception e) {

            }

            @Override
            public void onExit(int code) {

            }
        };
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (menuHelper.gameMenuSetting.mousePatch && keyCode == KeyEvent.KEYCODE_BACK) {
            InputBridge.sendMouseEvent(1, InputBridge.MOUSE_RIGHT, true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (menuHelper.gameMenuSetting.mousePatch && keyCode == KeyEvent.KEYCODE_BACK) {
            InputBridge.sendMouseEvent(1, InputBridge.MOUSE_RIGHT, false);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        boolean mouse = false;
        final int[] devices = InputDevice.getDeviceIds();
        for (int j : devices) {
            InputDevice device = InputDevice.getDevice(j);
            if (device != null && !device.isVirtual()) {
                if (device.getName().contains("Mouse") || (menuHelper != null && menuHelper.touchCharInput != null && !menuHelper.touchCharInput.isEnabled())) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && device.isExternal()) {
                        mouse = true;
                        break;
                    }
                    else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        mouse = true;
                        break;
                    }
                }
            }
        }
        if (!mouse) {
            CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtils.setLanguage(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleUtils.setLanguage(this);
    }

    @Override
    protected void onPause() {
        if (menuHelper.viewManager != null && menuHelper.gameCursorMode == 1) {
            CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE);
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        com.qcl.launcher.launcher.terracotta.TerracottaHelper.onActivityResult(this, requestCode);
        if (menuHelper != null) {
            menuHelper.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (gameLaunchSetting.fullscreen) {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            } else {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            }
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
    }

}
