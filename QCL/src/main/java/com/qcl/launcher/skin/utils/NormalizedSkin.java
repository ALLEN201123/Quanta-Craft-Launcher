package com.qcl.launcher.skin.utils;

import android.graphics.Bitmap;

/* loaded from: classes2.dex */
public class NormalizedSkin {
    private final Bitmap normalizedTexture;
    private final boolean oldFormat;
    private final int scale;
    private final Bitmap texture;

    private static void copyImage(Bitmap bitmap, Bitmap bitmap2, int i, int i2, int i3, int i4, int i5, int i6, boolean z) {
        for (int i7 = 0; i7 < i6; i7++) {
            for (int i8 = 0; i8 < i5; i8++) {
                bitmap2.setPixel((z ? (i5 - i8) - 1 : i8) + i3, i4 + i7, bitmap.getPixel(i + i8, i2 + i7));
            }
        }
    }

    public NormalizedSkin(Bitmap bitmap) throws InvalidSkinException {
        this.texture = bitmap;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width % 64 != 0) {
            throw new InvalidSkinException("Invalid size " + width + "x" + height);
        }
        if (width == height) {
            this.oldFormat = false;
        } else if (width == height * 2) {
            this.oldFormat = true;
        } else {
            throw new InvalidSkinException("Invalid size " + width + "x" + height);
        }
        this.scale = width / 64;
        Bitmap createBitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
        this.normalizedTexture = createBitmap;
        copyImage(bitmap, createBitmap, 0, 0, 0, 0, width, height, false);
        if (this.oldFormat) {
            convertOldSkin();
        }
    }

    private void convertOldSkin() {
        copyImageRelative(4, 16, 20, 48, 4, 4, true);
        copyImageRelative(8, 16, 24, 48, 4, 4, true);
        copyImageRelative(0, 20, 24, 52, 4, 12, true);
        copyImageRelative(4, 20, 20, 52, 4, 12, true);
        copyImageRelative(8, 20, 16, 52, 4, 12, true);
        copyImageRelative(12, 20, 28, 52, 4, 12, true);
        copyImageRelative(44, 16, 36, 48, 4, 4, true);
        copyImageRelative(48, 16, 40, 48, 4, 4, true);
        copyImageRelative(40, 20, 40, 52, 4, 12, true);
        copyImageRelative(44, 20, 36, 52, 4, 12, true);
        copyImageRelative(48, 20, 32, 52, 4, 12, true);
        copyImageRelative(52, 20, 44, 52, 4, 12, true);
    }

    private void copyImageRelative(int i, int i2, int i3, int i4, int i5, int i6, boolean z) {
        Bitmap bitmap = this.normalizedTexture;
        int i7 = this.scale;
        copyImage(bitmap, bitmap, i * i7, i2 * i7, i3 * i7, i4 * i7, i5 * i7, i6 * i7, z);
    }

    public Bitmap getOriginalTexture() {
        return this.texture;
    }

    public Bitmap getNormalizedTexture() {
        return this.normalizedTexture;
    }

    public int getScale() {
        return this.scale;
    }

    public boolean isOldFormat() {
        return this.oldFormat;
    }

    public boolean isSlim() {
        return hasTransparencyRelative(50, 16, 2, 4) || hasTransparencyRelative(54, 20, 2, 12) || hasTransparencyRelative(42, 48, 2, 4) || hasTransparencyRelative(46, 52, 2, 12) || (isAreaBlackRelative(50, 16, 2, 4) && isAreaBlackRelative(54, 20, 2, 12) && isAreaBlackRelative(42, 48, 2, 4) && isAreaBlackRelative(46, 52, 2, 12));
    }

    private boolean hasTransparencyRelative(int i, int i2, int i3, int i4) {
        int i5 = this.scale;
        int i6 = i * i5;
        int i7 = i2 * i5;
        int i8 = i3 * i5;
        int i9 = i4 * i5;
        for (int i10 = 0; i10 < i9; i10++) {
            for (int i11 = 0; i11 < i8; i11++) {
                if ((this.normalizedTexture.getPixel(i6 + i11, i7 + i10) >>> 24) != 255) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAreaBlackRelative(int i, int i2, int i3, int i4) {
        int i5 = this.scale;
        int i6 = i * i5;
        int i7 = i2 * i5;
        int i8 = i3 * i5;
        int i9 = i4 * i5;
        for (int i10 = 0; i10 < i9; i10++) {
            for (int i11 = 0; i11 < i8; i11++) {
                if (this.normalizedTexture.getPixel(i6 + i11, i7 + i10) != -16777216) {
                    return false;
                }
            }
        }
        return true;
    }
}
