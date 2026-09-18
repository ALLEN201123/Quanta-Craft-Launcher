package com.qcl.launcher.skin.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import javax.microedition.khronos.opengles.GL10;

/* loaded from: classes2.dex */
public class TextureHelper {
    public static boolean isLoadingError = false;

    public static int[] loadGLTextureFromBitmap(Bitmap bitmap, Bitmap bitmap2, GL10 gl10) {
        int[] iArr = {0, 0};
        if (bitmap == null) {
            Log.e("loadTex", "null skin bitmap, skipping texture load");
            return iArr;
        }
        gl10.glGenTextures(2, iArr, 0);
        gl10.glBindTexture(3553, iArr[0]);
        gl10.glTexParameterf(3553, 10241, 9728.0f);
        gl10.glTexParameterf(3553, 10240, 9728.0f);
        gl10.glTexParameterf(3553, 10242, 33071.0f);
        gl10.glTexParameterf(3553, 10243, 33071.0f);
        GLUtils.texImage2D(3553, 0, bitmap, 0);
        gl10.glBindTexture(3553, iArr[1]);
        gl10.glTexParameterf(3553, 10241, 9728.0f);
        gl10.glTexParameterf(3553, 10240, 9728.0f);
        gl10.glTexParameterf(3553, 10242, 33071.0f);
        gl10.glTexParameterf(3553, 10243, 33071.0f);
        if (bitmap2 != null) {
            GLUtils.texImage2D(3553, 0, bitmap2, 0);
        }
        if (iArr[0] == 0 || (bitmap2 != null && iArr[1] == 0)) {
            throw new RuntimeException("Error loading texture.");
        }
        Log.e("loadTex", "success");
        return iArr;
    }

    public static int[] loadTexture(Context context, int i) {
        isLoadingError = false;
        int[] iArr = {0};
        GLES20.glGenTextures(1, iArr, 0);
        if (iArr[0] != 0) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            Bitmap decodeResource = BitmapFactory.decodeResource(context.getResources(), i, options);
            GLES20.glBindTexture(3553, iArr[0]);
            GLES20.glTexParameteri(3553, 10241, 9728);
            GLES20.glTexParameterf(3553, 10240, 9728.0f);
            GLUtils.texImage2D(3553, 0, decodeResource, 0);
            decodeResource.recycle();
        }
        if (iArr[0] != 0) {
            return iArr;
        }
        throw new RuntimeException("Error loading texture.");
    }

    public static int loadTexture(Context context, Bitmap bitmap) {
        isLoadingError = false;
        int[] iArr = {0};
        GLES20.glGenTextures(1, iArr, 0);
        if (iArr[0] != 0) {
            new BitmapFactory.Options().inScaled = false;
            GLES20.glBindTexture(3553, iArr[0]);
            GLES20.glTexParameteri(3553, 10241, 9728);
            GLES20.glTexParameterf(3553, 10240, 9728.0f);
            GLUtils.texImage2D(3553, 0, bitmap, 0);
            bitmap.recycle();
        }
        if (iArr[0] != 0) {
            return iArr[0];
        }
        throw new RuntimeException("Error loading texture.");
    }

    public static int loadTextureFromBitmap(Bitmap bitmap) {
        int[] iArr = {0};
        GLES20.glGenTextures(1, iArr, 0);
        if (iArr[0] != 0) {
            GLES20.glBindTexture(3553, iArr[0]);
            GLES20.glTexParameterf(3553, 10241, 9728.0f);
            GLES20.glTexParameterf(3553, 10240, 9728.0f);
            GLUtils.texImage2D(3553, 0, bitmap, 0);
            bitmap.recycle();
        }
        if (iArr[0] != 0) {
            return iArr[0];
        }
        throw new RuntimeException("Error loading texture.");
    }

    public static int loadTextureFromFile(String str, int i) {
        isLoadingError = false;
        int[] iArr = {0};
        GLES20.glGenTextures(1, iArr, 0);
        if (iArr[0] == 0) {
            return i;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        try {
            Bitmap decodeFile = BitmapFactory.decodeFile(str, options);
            GLES20.glBindTexture(3553, iArr[0]);
            GLES20.glTexParameterf(3553, 10241, 9728.0f);
            GLES20.glTexParameterf(3553, 10240, 9728.0f);
            GLUtils.texImage2D(3553, 0, decodeFile, 0);
            decodeFile.recycle();
            if (iArr[0] != 0) {
                return iArr[0];
            }
            isLoadingError = true;
            return i;
        } catch (Exception unused) {
            isLoadingError = true;
            return i;
        }
    }
}
