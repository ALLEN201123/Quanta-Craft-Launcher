package com.qcl.launcher.launcher.launch;

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;

/* loaded from: classes2.dex */
public class GameFrameProbe {
    private static final int INTERVAL_MS = 200;
    private static final int REQUIRED_FRESH_FRAMES = 2;
    private final Callback callback;
    private volatile boolean stopped;
    private final TextureView target;
    private Thread worker;

    /* loaded from: classes2.dex */
    public interface Callback {
        void onFirstFrame();
    }

    public GameFrameProbe(TextureView textureView, Callback callback) {
        this.target = textureView;
        this.callback = callback;
    }

    public synchronized void start() {
        stop();
        this.stopped = false;
        Thread thread = new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.launch.GameFrameProbe$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                GameFrameProbe.this.m354lambda$start$0$comqcllauncherlauncherlaunchGameFrameProbe();
            }
        }, "GameFrameProbe");
        this.worker = thread;
        thread.setDaemon(true);
        this.worker.start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$start$0$com-qcl-launcher-launcher-launch-GameFrameProbe, reason: not valid java name */
    public /* synthetic */ void m354lambda$start$0$comqcllauncherlauncherlaunchGameFrameProbe() {
        boolean z = false;
        int i = 0;
        long j = 0;
        while (!this.stopped) {
            try {
                Thread.sleep(200L);
                if (this.stopped) {
                    return;
                }
                long currentTimestamp = currentTimestamp();
                if (currentTimestamp > 0) {
                    if (!z) {
                        z = true;
                    } else if (currentTimestamp == j) {
                        continue;
                    } else {
                        i++;
                        if (i >= 2) {
                            fire(currentTimestamp, i);
                            return;
                        }
                    }
                    j = currentTimestamp;
                }
            } catch (InterruptedException unused) {
                return;
            }
        }
    }

    public synchronized void stop() {
        this.stopped = true;
        Thread thread = this.worker;
        this.worker = null;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private long currentTimestamp() {
        SurfaceTexture surfaceTexture;
        try {
            TextureView textureView = this.target;
            if (textureView == null || (surfaceTexture = textureView.getSurfaceTexture()) == null) {
                return 0L;
            }
            return surfaceTexture.getTimestamp();
        } catch (Throwable unused) {
            return 0L;
        }
    }

    private void fire(long j, int i) {
        try {
            Log.i("jrelog", "[GameFrameProbe] 放行: 探测到游戏在持续出帧 ts=" + j + " (连续新帧=" + i + ")");
        } catch (Throwable unused) {
        }
        try {
            this.callback.onFirstFrame();
        } catch (Throwable unused2) {
        }
    }
}
