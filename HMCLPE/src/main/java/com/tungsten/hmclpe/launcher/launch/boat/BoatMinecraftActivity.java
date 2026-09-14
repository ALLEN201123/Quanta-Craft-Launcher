package com.tungsten.hmclpe.launcher.launch.boat;

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

import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.control.InputBridge;
import com.tungsten.hmclpe.control.MenuHelper;
import com.tungsten.hmclpe.control.view.LayoutPanel;
import com.tungsten.hmclpe.launcher.setting.game.GameLaunchSetting;

import com.tungsten.hmclpe.launcher.launch.MCOptionUtils;
import com.tungsten.hmclpe.launcher.launch.LaunchLogWindow;
import com.tungsten.hmclpe.utils.LocaleUtils;

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
    private DrawerLayout drawerLayout;
    private LayoutPanel baseLayout;

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

        DrawerLayout.LayoutParams params = new DrawerLayout.LayoutParams(DrawerLayout.LayoutParams.MATCH_PARENT, DrawerLayout.LayoutParams.MATCH_PARENT);

        drawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_control_pattern, null);
        addContentView(drawerLayout, params);

        baseLayout = findViewById(R.id.base_layout);

        scaleFactor = gameLaunchSetting.scaleFactor;


        handleCallback();

        init();

        menuHelper = new MenuHelper(this, this, gameLaunchSetting.fullscreen, gameLaunchSetting.game_directory, drawerLayout, baseLayout, false, gameLaunchSetting.controlLayout, 1, scaleFactor);
        // 启动日志悬浮窗：默认开启；退出游戏回主界面时随 Activity 销毁自动关闭
        // 默认显示（老存档里 log=false 也照样显示），游戏进主界面后自动关闭；× 可手动关
        new LaunchLogWindow(this, drawerLayout).show();

    }

    private void handleCallback() {
        setBoatCallback(new BoatCallback() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                bufferWidth = Math.max(1, Math.round(width * scaleFactor));
                bufferHeight = Math.max(1, Math.round(height * scaleFactor));
                surface.setDefaultBufferSize(bufferWidth, bufferHeight);

                MCOptionUtils.load(gameLaunchSetting.game_directory);
                int finalWidth = bufferWidth;
                int finalHeight = bufferHeight;
                MCOptionUtils.set("overrideWidth", String.valueOf(finalWidth));
                MCOptionUtils.set("overrideHeight", String.valueOf(finalHeight));
                MCOptionUtils.set("fullscreen", String.valueOf(gameLaunchSetting.fullscreen));
                MCOptionUtils.save(gameLaunchSetting.game_directory);

                new Thread(() -> {
                    Vector<String> args = BoatLauncher.getMcArgs(gameLaunchSetting, BoatMinecraftActivity.this, finalWidth, finalHeight, gameLaunchSetting.server);
                    runOnUiThread(() -> {
                        BoatActivity.setBoatNativeWindow(new Surface(surface));
                        BoatInput.setEventPipe();
                        eventPipeReady = true;
                        // Surface size may have changed while the arguments were prepared.
                        BoatInput.pushEventWindow(bufferWidth, bufferHeight);

                        startGame(gameLaunchSetting.javaPath,
                                gameLaunchSetting.home,
                                GameLaunchSetting.isHighVersion(gameLaunchSetting),
                                args,
                                gameLaunchSetting.boatRenderer,
                                gameLaunchSetting.game_directory);
                    });
                }).start();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                bufferWidth = Math.max(1, Math.round(width * scaleFactor));
                bufferHeight = Math.max(1, Math.round(height * scaleFactor));
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
        com.tungsten.hmclpe.launcher.terracotta.TerracottaHelper.onActivityResult(this, requestCode);
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
