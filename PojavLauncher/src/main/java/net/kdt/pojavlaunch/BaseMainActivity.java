package net.kdt.pojavlaunch;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.TextureView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Vector;
import net.kdt.pojavlaunch.function.PojavCallback;
import net.kdt.pojavlaunch.utils.JREUtils;
import net.kdt.pojavlaunch.utils.Tools;
import org.lwjgl.glfw.CallbackBridge;

/* loaded from: classes2.dex */
public class BaseMainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    public static boolean isInputStackCall;
    public TextureView minecraftGLView;
    boolean mouseMode;
    public PojavCallback pojavCallback;
    public float scaleFactor = 1.0f;
    private boolean picOutputNotified = false;
    public final Handler mouseModeHandler = new Handler() { // from class: net.kdt.pojavlaunch.BaseMainActivity.1
        @Override // android.os.Handler
        public void handleMessage(Message message) {
            super.handleMessage(message);
            if (message.what == 0) {
                BaseMainActivity.this.pojavCallback.onCursorModeChange(0);
            }
            if (message.what == 1) {
                BaseMainActivity.this.pojavCallback.onCursorModeChange(1);
            }
        }
    };

    @Override // android.view.TextureView.SurfaceTextureListener
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void init(String str, boolean z) {
        isInputStackCall = z;
        CallbackBridge.nativeSetUseInputStackQueue(z);
        TextureView textureView = (TextureView) findViewById(R.id.main_game_render_view);
        this.minecraftGLView = textureView;
        textureView.setOpaque(true);
        this.minecraftGLView.setSurfaceTextureListener(this);
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
        startMouseThread();
        this.pojavCallback.onSurfaceTextureAvailable(surfaceTexture, i, i2);
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
        this.pojavCallback.onSurfaceTextureSizeChanged(surfaceTexture, i, i2);
    }

    @Override // android.view.TextureView.SurfaceTextureListener
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        if (this.picOutputNotified) {
            return;
        }
        this.picOutputNotified = true;
        Log.i("jrelog", "[画面切换] 游戏首帧到达（onSurfaceTextureUpdated）");
        this.pojavCallback.onPicOutput();
    }

    public void resetPicOutputFlag() {
        this.picOutputNotified = false;
    }

    public static void onExit(Context context, int i) {
        ((BaseMainActivity) context).pojavCallback.onExit(i);
    }

    public void startGame(final String str, final String str2, boolean z, final Vector<String> vector, final String str3, final String str4, final String str5) {
        this.picOutputNotified = false;
        Thread thread = new Thread(new Runnable() { // from class: net.kdt.pojavlaunch.BaseMainActivity$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                BaseMainActivity.this.m2268lambda$startGame$2$netkdtpojavlaunchBaseMainActivity(str, str2, str3, vector, str4, str5);
            }
        }, "JVM Main thread");
        thread.setPriority(10);
        thread.start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startGame$2$net-kdt-pojavlaunch-BaseMainActivity, reason: not valid java name */
    public /* synthetic */ void m2268lambda$startGame$2$netkdtpojavlaunchBaseMainActivity(String str, String str2, String str3, Vector vector, String str4, String str5) {
        runOnUiThread(new Runnable() { // from class: net.kdt.pojavlaunch.BaseMainActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                BaseMainActivity.this.m2266lambda$startGame$0$netkdtpojavlaunchBaseMainActivity();
            }
        });
        try {
            JREUtils.redirectAndPrintJRELog(this);
        } catch (Throwable th) {
            Log.w("jrelog-logcat", "redirectAndPrintJRELog failed, continue launching anyway", th);
        }
        try {
            Tools.launchMinecraft(this, str, str2, str3, vector, str4, str5);
        } catch (Throwable th2) {
            th2.printStackTrace();
            runOnUiThread(new Runnable() { // from class: net.kdt.pojavlaunch.BaseMainActivity$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    BaseMainActivity.this.m2267lambda$startGame$1$netkdtpojavlaunchBaseMainActivity(th2);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startGame$0$net-kdt-pojavlaunch-BaseMainActivity, reason: not valid java name */
    public /* synthetic */ void m2266lambda$startGame$0$netkdtpojavlaunchBaseMainActivity() {
        this.pojavCallback.onStart();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startGame$1$net-kdt-pojavlaunch-BaseMainActivity, reason: not valid java name */
    public /* synthetic */ void m2267lambda$startGame$1$netkdtpojavlaunchBaseMainActivity(Throwable th) {
        this.pojavCallback.onError(new Exception(th));
    }

    public void startMouseThread() {
        Thread thread = new Thread(new Runnable() { // from class: net.kdt.pojavlaunch.BaseMainActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                BaseMainActivity.this.m2269lambda$startMouseThread$3$netkdtpojavlaunchBaseMainActivity();
            }
        }, "VirtualMouseGrabThread");
        thread.setPriority(1);
        thread.setDaemon(true);
        thread.start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startMouseThread$3$net-kdt-pojavlaunch-BaseMainActivity, reason: not valid java name */
    public /* synthetic */ void m2269lambda$startMouseThread$3$netkdtpojavlaunchBaseMainActivity() {
        while (true) {
            if (!CallbackBridge.isGrabbing() && this.mouseMode) {
                this.mouseModeHandler.sendEmptyMessage(1);
                this.mouseMode = false;
            }
            if (CallbackBridge.isGrabbing() && !this.mouseMode) {
                this.mouseModeHandler.sendEmptyMessage(0);
                this.mouseMode = true;
            }
            try {
                Thread.sleep(16L);
            } catch (InterruptedException unused) {
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onStart() {
        super.onStart();
        CallbackBridge.nativeSetWindowAttrib(131076, 1);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onStop() {
        CallbackBridge.nativeSetWindowAttrib(131076, 0);
        super.onStop();
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (z) {
            getWindow().getDecorView().setSystemUiVisibility(5894);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onPostResume() {
        super.onPostResume();
        TextureView textureView = this.minecraftGLView;
        if (textureView == null || textureView.getSurfaceTexture() == null) {
            return;
        }
        this.minecraftGLView.post(new Runnable() { // from class: net.kdt.pojavlaunch.BaseMainActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                BaseMainActivity.this.m2265lambda$onPostResume$4$netkdtpojavlaunchBaseMainActivity();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onPostResume$4$net-kdt-pojavlaunch-BaseMainActivity, reason: not valid java name */
    public /* synthetic */ void m2265lambda$onPostResume$4$netkdtpojavlaunchBaseMainActivity() {
        this.pojavCallback.onSurfaceTextureSizeChanged(this.minecraftGLView.getSurfaceTexture(), this.minecraftGLView.getWidth(), this.minecraftGLView.getHeight());
    }
}
