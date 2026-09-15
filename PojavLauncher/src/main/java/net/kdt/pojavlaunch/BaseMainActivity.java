package net.kdt.pojavlaunch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.kdt.pojavlaunch.function.PojavCallback;
import net.kdt.pojavlaunch.keyboard.LwjglGlfwKeycode;
import net.kdt.pojavlaunch.utils.JREUtils;
import net.kdt.pojavlaunch.utils.Tools;

import org.lwjgl.glfw.CallbackBridge;

import java.util.Vector;

public class BaseMainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    public TextureView minecraftGLView;
    public float scaleFactor = 1.0F;
    public static boolean isInputStackCall;

    public PojavCallback pojavCallback;

    boolean mouseMode;
    /** 1.0.7：游戏画面已输出通知是否已发出（只发一次） */
    private boolean picOutputNotified = false;

    protected void init(String gameDir , boolean highVersion) {

        isInputStackCall = highVersion;

        minecraftGLView = findViewById(R.id.main_game_render_view);
        // ⚠️ 1.0.7 关键修复：这里原本是 setOpaque(false)（透明）。
        // 透明 TextureView 在部分设备/驱动上**不会触发 onSurfaceTextureUpdated 回调**
        // （系统认为没有可见内容需要合成），而关掉「启动等待界面」的唯一正规路径
        // 正是 onSurfaceTextureUpdated → onPicOutput()。
        // 结果：游戏其实已经在后台正常渲染，但等待界面永远不消失 →
        // 玩家看到的就是「卡在启动画面、游戏窗口不显示」。
        // 游戏画面本身就是不透明的，这里必须是 true。
        minecraftGLView.setOpaque(true);

        minecraftGLView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
        startMouseThread();
        pojavCallback.onSurfaceTextureAvailable(surfaceTexture,i,i1);
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
        pojavCallback.onSurfaceTextureSizeChanged(surfaceTexture,i,i1);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
        // 1.0.7：原来用的是 int output 计数器（output==1 才触发一次），
        // 逻辑绕且易漏（第一次调用时 output 还不到 1，要等第二次；一旦有别的路径
        // 动过这个值就再也不会触发）。改成直白的 boolean，语义清楚、只触发一次。
        if (!picOutputNotified) {
            picOutputNotified = true;
            android.util.Log.i("jrelog", "[画面切换] 游戏首帧到达（onSurfaceTextureUpdated）");
            pojavCallback.onPicOutput();
        }
    }

    /**
     * 1.0.9：修复「等待界面盖回来后切换信号断路」的时序竞争。
     *
     * 场景：游戏首帧早于 onStart（showBackground）到达 → picOutputNotified 提前置位
     * → onStart 把等待界面盖回来 → 此后标志一直是 true，onSurfaceTextureUpdated
     * 不再转发 onPicOutput → 等待界面永远不撤（只剩 10s/30s 兜底）。
     *
     * 修法：onStart 在 showBackground() 之后调用本方法清零标志，
     * 游戏下一帧（每秒几十帧）立刻重新触发 onPicOutput → hideBackground。
     * 等待界面只在「真正没有帧」时显示，游戏出画即切换。
     */
    public void resetPicOutputFlag() {
        picOutputNotified = false;
    }

    public static void onExit(Context ctx, int code) {
        ((BaseMainActivity) ctx).pojavCallback.onExit(code);
    }

    public void startGame(String javaPath,String home,boolean highVersion,final Vector<String> args, String renderer,String gameDir,String glesVersion) {
        // 1.0.7：重置「画面已输出」标志。
        // 必须在每次启动时清零 —— 否则上一次启动（或视图预热阶段）可能已经把它置位，
        // 导致这次的等待界面再也等不到 onPicOutput()，界面就一直卡着。
        picOutputNotified = false;
        Thread JVMThread = new Thread(() -> {
            runOnUiThread(() -> {
                pojavCallback.onStart();
            });
            try {
                JREUtils.redirectAndPrintJRELog(this);
                Tools.launchMinecraft(this, javaPath,home,renderer, args,gameDir,glesVersion);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                runOnUiThread(() -> {
                    pojavCallback.onError(new Exception(throwable));
                });
            }
        }, "JVM Main thread");
        JVMThread.setPriority(Thread.MAX_PRIORITY);
        JVMThread.start();
    }

    public void startMouseThread() {
        Thread virtualMouseGrabThread = new Thread(() -> {
            while (true) {
                if (!CallbackBridge.isGrabbing() && mouseMode) {
                    mouseModeHandler.sendEmptyMessage(1);
                    mouseMode = false;
                }
                if (CallbackBridge.isGrabbing() && !mouseMode) {
                    mouseModeHandler.sendEmptyMessage(0);
                    mouseMode = true;
                }
            }
        }, "VirtualMouseGrabThread");
        virtualMouseGrabThread.setPriority(Thread.MIN_PRIORITY);
        virtualMouseGrabThread.start();
    }

    @SuppressLint("HandlerLeak")
    public final Handler mouseModeHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                pojavCallback.onCursorModeChange(0);
            }
            if (msg.what == 1) {
                pojavCallback.onCursorModeChange(1);
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_VISIBLE, 1);
    }

    @Override
    protected void onStop() {
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_VISIBLE, 0);
        super.onStop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (minecraftGLView != null && minecraftGLView.getSurfaceTexture() != null) {
            minecraftGLView.post(() -> {
                pojavCallback.onSurfaceTextureSizeChanged(minecraftGLView.getSurfaceTexture(),minecraftGLView.getWidth(),minecraftGLView.getHeight());
            });
        }
    }
}
