package com.qcl.launcher.skin.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.TypedValue;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;

/* loaded from: classes2.dex */
public class Avatar {
    static final Handler handler = new Handler() { // from class: com.qcl.launcher.skin.utils.Avatar.1
        @Override // android.os.Handler
        public void handleMessage(Message message) {
            super.handleMessage(message);
        }
    };

    public static void setAvatar(final String str, final ImageView imageView, final ImageView imageView2) {
        imageView.post(new Runnable() { // from class: com.qcl.launcher.skin.utils.Avatar$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                Avatar.lambda$setAvatar$1(str, imageView, imageView2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setAvatar$1(String str, final ImageView imageView, final ImageView imageView2) {
        Bitmap stringToBitmap = stringToBitmap(str);
        Bitmap createBitmap = Bitmap.createBitmap(stringToBitmap, 8, 8, 8, 8, (Matrix) null, false);
        Bitmap createBitmap2 = Bitmap.createBitmap(stringToBitmap, 40, 8, 8, 8, (Matrix) null, false);
        Matrix matrix = new Matrix();
        float width = imageView.getWidth() / 8;
        Matrix matrix2 = new Matrix();
        float width2 = imageView2.getWidth() / 8;
        matrix.postScale(width, width);
        final Bitmap createBitmap3 = Bitmap.createBitmap(createBitmap, 0, 0, 8, 8, matrix, false);
        matrix2.postScale(width2, width2);
        final Bitmap createBitmap4 = Bitmap.createBitmap(createBitmap2, 0, 0, 8, 8, matrix2, false);
        handler.post(new Runnable() { // from class: com.qcl.launcher.skin.utils.Avatar$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                Avatar.lambda$setAvatar$0(imageView, createBitmap3, imageView2, createBitmap4);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setAvatar$0(ImageView imageView, Bitmap bitmap, ImageView imageView2, Bitmap bitmap2) {
        imageView.setImageBitmap(bitmap);
        imageView2.setImageBitmap(bitmap2);
    }

    public static void setAvatarFromSkin(final Bitmap bitmap, final ImageView imageView, final ImageView imageView2) {
        if (bitmap == null || imageView == null || imageView2 == null) {
            return;
        }
        imageView.post(new Runnable() { // from class: com.qcl.launcher.skin.utils.Avatar$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                Avatar.lambda$setAvatarFromSkin$2(bitmap, imageView, imageView2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setAvatarFromSkin$2(Bitmap bitmap, ImageView imageView, ImageView imageView2) {
        try {
            Bitmap createBitmap = Bitmap.createBitmap(bitmap, 8, 8, 8, 8, (Matrix) null, false);
            Bitmap createBitmap2 = Bitmap.createBitmap(bitmap, 40, 8, 8, 8, (Matrix) null, false);
            Matrix matrix = new Matrix();
            matrix.postScale(imageView.getWidth() / 8.0f, imageView.getHeight() / 8.0f);
            Matrix matrix2 = new Matrix();
            matrix2.postScale(imageView2.getWidth() / 8.0f, imageView2.getHeight() / 8.0f);
            imageView.setImageBitmap(Bitmap.createBitmap(createBitmap, 0, 0, 8, 8, matrix, false));
            imageView2.setImageBitmap(Bitmap.createBitmap(createBitmap2, 0, 0, 8, 8, matrix2, false));
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    public static Bitmap stringToBitmap(String str) {
        try {
            byte[] decode = Base64.decode(str, 0);
            return BitmapFactory.decodeByteArray(decode, 0, decode.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String bitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), 0);
    }

    public static Bitmap getBitmapFromRes(Context context, int i) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inTargetDensity = new TypedValue().density;
        options.inScaled = false;
        return BitmapFactory.decodeResource(context.getResources(), i, options);
    }
}
