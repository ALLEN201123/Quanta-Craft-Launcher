package net.kdt.pojavlaunch.function;

import android.graphics.SurfaceTexture;

/* loaded from: classes2.dex */
public interface PojavCallback {
    void onCursorModeChange(int i);

    void onError(Exception exc);

    void onExit(int i);

    void onPicOutput();

    void onStart();

    void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2);

    void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2);
}
