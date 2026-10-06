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
        // ★★★ 1.1.4：自动开启「持续性能模式」（等价于 vivo/iQOO 的"游戏魔盒"性能优化，照搬 FCL）。
        //   默认开启；玩家可在游戏菜单里点一下关 / 再点一下开（存于 qcl_perf 的 performanceMode）。
        try {
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                boolean qclPerf = getSharedPreferences("qcl_perf", 0).getBoolean("performanceMode", true);
                getWindow().setSustainedPerformanceMode(qclPerf);
            }
        }
        catch (Throwable ignored) {
            // 某些窗口/设备不支持也不影响启动
        }
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
        // ★ 1.4.3：给「无标题界面版本」开护栏 —— 它们进游戏先加载、不经标题界面，加载完游戏才自己
        //   报 grab（实测首帧后 ~2.7s）；这期间 QCL 仍是光标模式，滑动被当绝对光标投给游戏 → 开局大偏。
        org.lwjgl.glfw.CallbackBridge.setSuppressPointerUntilFirstGrab(
                isNoTitleScreenVersion(this.gameLaunchSetting.currentVersion));
        LaunchLogWindow.GameLaunchSettingInfo info = new LaunchLogWindow.GameLaunchSettingInfo();
        info.backend = "Pojav";
        info.version = this.gameLaunchSetting.currentVersion;
        info.javaRuntime = this.gameLaunchSetting.javaPath;
        info.renderer = this.gameLaunchSetting.pojavRenderer;
        info.ramMb = this.gameLaunchSetting.maxRam;
        LaunchLogWindow.setBasics(info);
        new LaunchLogWindow((Activity)this, (ViewGroup)this.drawerLayout).show(info);
    }

    /**
     * 该版本是否「没有标题界面」（= 进游戏先加载一段时间、不经标题界面就直接在世界里）。
     *
     * 依据 Minecraft Wiki「Title Screen」历史章节：**标题界面是 Indev 0.31 的 20100131 构建加入的**
     * （20100206 背景不再滚动；Infdev 20100327 只是把按钮换成 Singleplayer/Multiplayer —— 界面本身早在
     * Indev 20100131 就有了）。所以属于此类只有：
     *   · Classic 全系（`c*`）—— 经典客户端本体没有标题界面，菜单在网页外壳里；
     *   · Indev 0.31 中早于 20100131 的构建（`in-YYYYMMDD-*`）。
     * infdev 起（含 Alpha / Beta / 现代版本）一律 false，不去动它们。
     */
    private static boolean isNoTitleScreenVersion(String currentVersionPath) {
        if (currentVersionPath == null) {
            return false;
        }
        String name = new java.io.File(currentVersionPath).getName().trim().toLowerCase();
        // Classic 全系：版本名是 c0.30-c-1900 / c0.0.13a / c0.28 这种「c + 数字」。
        // ★ 必须限定「c 后面跟数字」—— 曾用 startsWith("c") 误命中 "Cursed-Fabric-MultiMCnew"
        //   （现代 Fabric 整合包，**有**标题界面），会把它的标题界面光标也冻住。
        // pre-classic（RubyDung，rd-*）比 Classic 还早，同样没有标题界面。
        if (java.util.regex.Pattern.compile("^c\\d").matcher(name).find() || name.startsWith("rd-")) {
            return true;
        }
        if (!name.startsWith("in-")) {
            return false;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^in-(\\d{8})").matcher(name);
        return matcher.find() && matcher.group(1).compareTo("20100131") < 0;
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
                // ★★★ 2026-09-19 照 FCL FCLGameLauncher.generateOptionsTxt()：
                //   玩家新下载的游戏版本首次启动时没有 options.txt（或其中没有 lang 项），
                //   MC 会回落到英文。这里按「启动器语言 / 系统语言」补上中文，
                //   并按 MC 版本规范化语言码大小写（<1.11 用 zh_CN，≥1.11 用 zh_cn）。
                //   注意：只在缺失时补 —— 玩家在游戏里改过语言的话不会被覆盖。
                try {
                    String qclLang = MCOptionUtils.get("lang");
                    if (qclLang == null || qclLang.trim().isEmpty()) {
                        String ver = new java.io.File(
                                ((PojavMinecraftActivity)PojavMinecraftActivity.this).gameLaunchSetting.currentVersion).getName();
                        MCOptionUtils.set("lang", com.qcl.launcher.utils.LocaleUtils.normalizeMinecraftLang(
                                ver, com.qcl.launcher.utils.LocaleUtils.getMinecraftLang((Context)PojavMinecraftActivity.this)));
                    }
                } catch (Throwable t) {
                    android.util.Log.w("jrelog", "[默认语言] 写入 lang 失败", t);
                }
                MCOptionUtils.set("overrideWidth", String.valueOf(CallbackBridge.windowWidth));
                MCOptionUtils.set("overrideHeight", String.valueOf(CallbackBridge.windowHeight));
                if (GameLaunchSetting.isHighVersion(PojavMinecraftActivity.this.gameLaunchSetting)) {
                    MCOptionUtils.set("fullscreen", "false");
                }
                MCOptionUtils.save(((PojavMinecraftActivity)PojavMinecraftActivity.this).gameLaunchSetting.game_directory);
                // ★★★ 1.3.8：启动前按渲染器改 mod 配置文件（照 FCL 的 modifyIfConfigDetected）——
                //   GL4ES / VGPU 渲染器下关掉 Sodium / Rubidium 的区块渲染 mixin，避免画面闪烁。
                try {
                    com.qcl.launcher.launcher.launch.ModCompatPatcher.patchBeforeLaunch(
                            (Context)PojavMinecraftActivity.this,
                            ((PojavMinecraftActivity)PojavMinecraftActivity.this).gameLaunchSetting);
                } catch (Throwable ignored) {
                }
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
                // ★ 1.4.3：首帧到达 = 「世界开始出现」，护栏（首次 grab 前不投绝对光标）从这一刻起计时。
                org.lwjgl.glfw.CallbackBridge.notifyFirstFrame();
                PojavMinecraftActivity.this.stopFrameProbe();
                PojavMinecraftActivity.this.baseLayout.hideBackground();
            }

            public void onError(Exception e) {
            }

            public void onExit(int code) {
                // ★★★【2026-10-06 修复 · 用户实测"26.2 点退出游戏卡死"】
                //   照 FCL 的 GameMenu.onExit：游戏 JVM 一结束就**直接杀掉本进程**，
                //   玩家下次点开启动器是冷启动，回到主界面。
                //
                //   ★ 原来这个回调是**空的** —— 游戏进程退出了，但游戏 Activity 还挂在前台
                //     （黑屏 / 卡死，玩家以为是启动器死了）。exitCode==0 是正常退出，
                //     非 0 是异常退出，这里都统一走 killProcess（与 FCL 行为一致）。
                Log.i("jrelog", "[游戏退出] exitCode=" + code);
                try {
                    android.os.Process.killProcess(android.os.Process.myPid());
                } catch (Throwable t) {
                    // 兜底：万一杀不掉，至少把界面收掉，别让玩家对着黑屏
                    try {
                        PojavMinecraftActivity.this.finish();
                    } catch (Throwable ignored) {
                    }
                }
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

    protected void onResume() {
        super.onResume();
        // ★★★ 1.4.8：保活。玩家切到后台时，没有前台服务的进程会被系统很快回收，
        //   而游戏 JVM 就跑在本进程里 → 游戏直接挂掉、只能重开。
        try {
            GameAliveService.start(this);
        } catch (Throwable t) {
            Log.w("QCL-alive", "启动保活服务失败: " + t);
        }
        // ★★★ 1.4.8：启动游戏内「载入文件…／保存文件…」的文件桥（详见 QclFileBridge）。
        //   它平时只是每 400ms 看一眼有没有请求文件，没有任何性能影响。
        try {
            QclFileBridge.start(this);
        } catch (Throwable t) {
            Log.w("QCL-filebridge", "启动文件桥失败: " + t);
        }
    }

    protected void onDestroy() {
        try {
            QclFileBridge.stop();
        } catch (Throwable ignored) {
        }
        try {
            GameAliveService.stop(this);
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // ★★★ 1.4.8：游戏内「载入文件…／保存文件…」按钮的文件桥。
        //   游戏 JVM 弹不出安卓组件，所以走文件信箱（见 QclFileBridge 的类注释）。
        if (QclFileBridge.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        // ★★★ 远古版本的「加载文件…」：游戏里的 cacio 对话框会弹**系统文件选择器**，
        //     结果回到这里，再由我们把路径交回游戏进程内的 cacio（同一个进程）。
        if (deliverGamePickedFile(requestCode, resultCode, data)) {
            return;
        }
        TerracottaHelper.onActivityResult((Activity)this, requestCode);
        if (this.menuHelper != null) {
            this.menuHelper.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * 把"玩家在系统文件选择器里选中的存档文件"交回游戏里的 cacio 对话框。
     *
     * <p>为什么要这么绕：cacio 的 AWT 窗口在安卓上没有窗口实体，画不出文件对话框，
     * 所以改成弹系统选择器；而系统选择器是异步的，结果只能从这里回去。
     * 游戏进程和本 Activity 是同一个（manifest 里 {@code multiprocess="true"}），
     * 所以直接用反射调用 cacio 里的静态钩子即可，不需要跨进程通信。
     *
     * @return 这次结果是不是文件选择器的（是的话就不再往下传）
     */
    private boolean deliverGamePickedFile(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != 0x0C1F) {
            return false;
        }
        String path = null;
        try {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // ① 我们自己的文件浏览器（LevelFileChooserActivity）直接给绝对路径
                path = data.getStringExtra("path");
                // ② 兼容系统选择器返回的 Uri
                if (path == null && data.getData() != null) {
                    android.net.Uri uri = data.getData();
                    if ("file".equals(uri.getScheme())) {
                        path = uri.getPath();
                    } else {
                        path = com.qcl.launcher.utils.file.UriUtils.getRealPathFromUri_AboveApi19(this, uri);
                    }
                }
            }
        } catch (Throwable t) {
            Log.w("QCL-import", "解析选择的文件失败: " + t);
        }
        try {
            // 反射调用 cacio 补丁里的静态钩子（它在游戏进程的类加载器里）
            ClassLoader[] loaders = new ClassLoader[]{
                    getClassLoader(),
                    ClassLoader.getSystemClassLoader(),
                    Thread.currentThread().getContextClassLoader()
            };
            Class<?> hook = null;
            for (ClassLoader cl : loaders) {
                if (cl == null) {
                    continue;
                }
                try {
                    hook = Class.forName("sun.awt.peer.cacio.CacioFileDialogPeer$PickerHook", false, cl);
                    break;
                } catch (Throwable ignored) {
                }
            }
            if (hook == null) {
                hook = Class.forName("sun.awt.peer.cacio.CacioFileDialogPeer$PickerHook");
            }
            hook.getMethod("deliver", String.class).invoke(null, path);
            Log.i("QCL-import", "已把选择的文件交回游戏: " + path);
        } catch (Throwable t) {
            Log.w("QCL-import", "交回游戏失败（可能是没走选择器那条路）: " + t);
        }
        return true;
    }

    public void onPostResume() {
        super.onPostResume();
        if (Build.VERSION.SDK_INT >= 28) {
            this.getWindow().getAttributes().layoutInDisplayCutoutMode = this.gameLaunchSetting.fullscreen ? 1 : 2;
        }
        this.getWindow().setFlags(256, 256);
    }
}

