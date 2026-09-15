package com.qcl.launcher.launcher.launch.boat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import com.qcl.launcher.R;
import com.qcl.launcher.control.InputBridge;
import com.qcl.launcher.control.MenuHelper;
import com.qcl.launcher.control.view.LayoutPanel;
import com.qcl.launcher.launcher.setting.game.GameLaunchSetting;

import com.qcl.launcher.launcher.launch.MCOptionUtils;
import com.qcl.launcher.launcher.launch.LaunchLogWindow;
import com.qcl.launcher.utils.LocaleUtils;

import java.util.Vector;

import cosine.boat.BoatActivity;
import cosine.boat.BoatInput;
import cosine.boat.function.BoatCallback;
import cosine.boat.keyboard.BoatKeycodes;

public class BoatMinecraftActivity extends BoatActivity {

    private GameLaunchSetting gameLaunchSetting;
    private boolean eventPipeReady;
    private int bufferWidth;
    private int bufferHeight;
    private android.widget.FrameLayout drawerLayout;
    private LayoutPanel baseLayout;

    /** 1.0.7：等待界面兜底关闭探测器（与 Pojav 侧共用 GameFrameProbe） */
    private com.qcl.launcher.launcher.launch.GameFrameProbe frameProbe;

    public MenuHelper menuHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameLaunchSetting = GameLaunchSetting.getGameLaunchSetting(getIntent().getExtras().getString("setting_path"), getIntent().getExtras().getString("version"));

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

        setContentView(cosine.boat.R.layout.activity_boat);

        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT);

        drawerLayout = (android.widget.FrameLayout) getLayoutInflater().inflate(R.layout.activity_control_pattern, null);
        addContentView(drawerLayout, params);

        baseLayout = findViewById(R.id.base_layout);

        scaleFactor = gameLaunchSetting.scaleFactor;


        handleCallback();

        init();

        menuHelper = new MenuHelper(this, this, gameLaunchSetting.fullscreen, gameLaunchSetting.game_directory, drawerLayout, baseLayout, false, gameLaunchSetting.controlLayout, 1, scaleFactor);
        // 启动日志悬浮窗：默认开启；退出游戏回主界面时随 Activity 销毁自动关闭
        // 默认显示（老存档里 log=false 也照样显示），游戏进主界面后自动关闭；× 可手动关
        // 1.0.6：带上基础启动信息（设备/后端/运行时/渲染器/内存），避免日志窗内容太少
        LaunchLogWindow.GameLaunchSettingInfo logInfo = new LaunchLogWindow.GameLaunchSettingInfo();
        logInfo.backend = "Boat";
        logInfo.version = gameLaunchSetting.currentVersion;
        logInfo.javaRuntime = gameLaunchSetting.javaPath;
        logInfo.renderer = gameLaunchSetting.boatRenderer;
        logInfo.ramMb = gameLaunchSetting.maxRam;
        // 登记基础信息：之后从悬浮窗开关打开日志窗也能带上
        LaunchLogWindow.setBasics(logInfo);
        LaunchLogWindow.showForBoat(this, drawerLayout, logInfo);

    }

    /**
     * 远古版本（LWJGL2）拿窗口尺寸的唯一途径是 ConfigureNotify 事件。
     * 事件管道刚建立时游戏还没开始 poll，第一条会被丢；
     * 这里在启动后按 30ms 间隔连推若干次当前 surface 尺寸，保证游戏一定读到。
     * 只推「当前真实 buffer 尺寸」，不做任何缩放换算。
     */
    private void scheduleWindowSizePush() {
        for (int delay : new int[]{0, 30, 80, 160, 300, 600, 1200, 2400}) {
            windowSizeHandler.postDelayed(this::pushCurrentWindowSize, delay);
        }
    }

    private void pushCurrentWindowSize() {
        if (!eventPipeReady || windowSizeHandler == null) return;
        int w = bufferWidth > 0 ? bufferWidth : 1;
        int h = bufferHeight > 0 ? bufferHeight : 1;
        if (w < 64 || h < 64) {
            // buffer 尺寸还没拿到有效值 → 用屏幕尺寸兜底，别把窗口推成小条
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            w = dm.widthPixels;
            h = dm.heightPixels;
        }
        BoatInput.pushEventWindow(w, h);
    }

    @SuppressLint("HandlerLeak")
    private final Handler windowSizeHandler = new Handler(android.os.Looper.getMainLooper());

    /** 1.0.7：启动画面探测兜底（逻辑在 GameFrameProbe，与 Pojav 侧共用） */
    private void startFrameProbe() {
        if (frameProbe == null) {
            frameProbe = new com.qcl.launcher.launcher.launch.GameFrameProbe(
                    getMainTextureView(), () -> {
                baseLayout.hideBackground();
            });
        }
        frameProbe.start();
    }

    private void stopFrameProbe() {
        if (frameProbe != null) {
            frameProbe.stop();
        }
    }

    private void handleCallback() {
        setBoatCallback(new BoatCallback() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                // ⚠️ TextureView 回调时若布局尚未完成，width/height 可能是 0 或极小值。
                // 直接用它算 buffer / overrideWidth 会把窗口缩成一小条（远古版本尤其明显，
                // 因为远古版本完全依赖 overrideWidth/overrideHeight 决定窗口尺寸）。
                // 这里兜底取「屏幕可用尺寸」，保证远古版本首次进入一定是满屏。
                int safeW = width;
                int safeH = height;
                if (safeW < 64 || safeH < 64) {
                    android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
                    safeW = dm.widthPixels;
                    safeH = dm.heightPixels;
                }
                bufferWidth = Math.max(1, Math.round(safeW * scaleFactor));
                bufferHeight = Math.max(1, Math.round(safeH * scaleFactor));
                surface.setDefaultBufferSize(bufferWidth, bufferHeight);

                MCOptionUtils.load(gameLaunchSetting.game_directory);
                int finalWidth = bufferWidth;
                int finalHeight = bufferHeight;
                boolean highVersion = GameLaunchSetting.isHighVersion(gameLaunchSetting);
                MCOptionUtils.set("overrideWidth", String.valueOf(finalWidth));
                MCOptionUtils.set("overrideHeight", String.valueOf(finalHeight));
                // ⚠️ 远古版本（LWJGL2 时代）的 options.txt 没有 fullscreen 这个键，
                // 写进去会让它的解析器读到未知键 → 部分版本直接抛异常 / 后续选项全丢，
                // 结果窗口按默认 854x480 且非全屏。只有现代版本才需要写 fullscreen。
                if (highVersion) {
                    MCOptionUtils.set("fullscreen", String.valueOf(gameLaunchSetting.fullscreen));
                }
                MCOptionUtils.save(gameLaunchSetting.game_directory);

                new Thread(() -> {
                    Vector<String> args = BoatLauncher.getMcArgs(gameLaunchSetting, BoatMinecraftActivity.this, finalWidth, finalHeight, gameLaunchSetting.server);
                    runOnUiThread(() -> {
                        BoatActivity.setBoatNativeWindow(new Surface(surface));
                        BoatInput.setEventPipe();
                        eventPipeReady = true;

                        startGame(gameLaunchSetting.javaPath,
                                gameLaunchSetting.home,
                                GameLaunchSetting.isHighVersion(gameLaunchSetting),
                                args,
                                gameLaunchSetting.boatRenderer,
                                gameLaunchSetting.game_directory);

                        // ⚠️ 远古版本（LWJGL2）的窗口尺寸**完全**依赖 ConfigureNotify 事件。
                        // 但事件管道刚建立时游戏还没开始 poll 事件，此时推的那一条会被丢掉
                        // → 游戏退回默认窗口（远古版本是 854x480）→ 画面只占屏幕一小块。
                        // 因此这里在游戏起来之后**多次**重推当前 surface 尺寸，直到游戏真正读到。
                        scheduleWindowSizePush();
                    });
                }).start();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                int safeW = width;
                int safeH = height;
                if (safeW < 64 || safeH < 64) {
                    // 尺寸异常时沿用上一次的有效值，别把窗口缩成小条
                    safeW = Math.max(bufferWidth, 64);
                    safeH = Math.max(bufferHeight, 64);
                }
                bufferWidth = Math.max(1, Math.round(safeW * scaleFactor));
                bufferHeight = Math.max(1, Math.round(safeH * scaleFactor));
                int finalWidth = bufferWidth;
                int finalHeight = bufferHeight;
                surface.setDefaultBufferSize(finalWidth, finalHeight);
                if (eventPipeReady) BoatInput.pushEventWindow(finalWidth, finalHeight);
                MCOptionUtils.load(gameLaunchSetting.game_directory);
                MCOptionUtils.set("overrideWidth", String.valueOf(finalWidth));
                MCOptionUtils.set("overrideHeight", String.valueOf(finalHeight));
                MCOptionUtils.save(gameLaunchSetting.game_directory);
            }

            @Override
            public void onCursorModeChange(int mode) {
                cursorModeHandler.sendEmptyMessage(mode);
            }

            @Override
            public void onStart() {
                baseLayout.showBackground();
                // 1.0.7：等待界面一出现就启动画面探测兜底
                startFrameProbe();
            }

            @Override
            public void onPicOutput() {
                // 正规路径：回调来了就正常关掉，并停掉探测
                stopFrameProbe();
                baseLayout.hideBackground();
            }

            @Override
            public void onError(Exception e) {

            }

            @Override
            public void onExit(int code) {
                Intent virGLService = new Intent(BoatMinecraftActivity.this, VirGLService.class);
                stopService(virGLService);
            }
        });
    }

    @SuppressLint("HandlerLeak")
    private final Handler cursorModeHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == BoatInput.CursorDisabled) {
                menuHelper.disableCursor();
            }
            if (msg.what == BoatInput.CursorEnabled) {
                menuHelper.enableCursor();
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (menuHelper.gameMenuSetting.mousePatch) {
            InputBridge.sendMouseEvent(1, InputBridge.MOUSE_RIGHT, true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (menuHelper.gameMenuSetting.mousePatch) {
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
            BoatInput.setKey(BoatKeycodes.KEY_ESC, 0, true);
            BoatInput.setKey(BoatKeycodes.KEY_ESC, 0, false);
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
            BoatInput.setKey(BoatKeycodes.KEY_ESC, 0, true);
            BoatInput.setKey(BoatKeycodes.KEY_ESC, 0, false);
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

    @Override
    protected void onDestroy() {
        Intent virGLService = new Intent(this, VirGLService.class);
        stopService(virGLService);
        super.onDestroy();
    }
}
