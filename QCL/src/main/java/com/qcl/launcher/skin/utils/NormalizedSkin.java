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
        // ★★★ 2026-10-02 修复「老格式皮肤头像 / 头顶 / 下巴变黑」（第二次修正，范围扩大到整块）：
        //   老格式 64x32 的头部第二层占据整个 (32,0)-(64,16)（共 32x16）：
        //     上半 (32,0)-(64,8)  = 帽子层【顶面/底面】→ 3D 模型 Hat 顶面取 (40,0)-(48,8)
        //     下半 (32,8)-(64,16) = 帽子层【四个侧面】→ 3D 模型 Hat 侧面取 (40,8)/(48,8)/...
        //   Notch 等一大批原始皮肤把**整块 (32,0)-(64,16)** 填成纯黑不透明占位（实测唯一色数=1，
        //   全为 0x000000FF）。官方渲染器 / crafatar 对这块的处理是**不渲染**（渲染结果无黑帽子），
        //   而本类原来把整张 64x32 原样搬到 64x64 → 这块黑被当作有效帽子层：
        //     · (40,8) 被 Avatar 叠加 → **账户列表头像黑块**
        //     · (40,0) 被 Hat 顶面使用 → **3D 预览"头顶一圈黑"**
        //   上一版只清了 (32,8)-(64,16) 半块，漏了 (32,0)-(64,8) 上半块 → 头顶仍黑。
        //
        //   ★★ 为什么是「条件清」而不是「整块清」：
        //   实测老皮肤分两类 ——
        //     · Notch：整块 (32,0)-(64,16) 为纯黑不透明（官方"无装饰"占位）→ 应清；
        //     · jeb_ / Marc_IRL：该区域有真实帽子内容（不透明像素 + 多色 + 部分透明）→ **必须保留**。
        //   若无条件整块清，会把 jeb_/Marc_IRL 的帽子一起抹掉（误伤）。
        //   因此判据只有一个：**整块是否为「纯黑不透明」占位** —— 只有这种情况才清。
        //   （jeb_ 那块是"部分透明 + 有颜色"，不属于纯黑占位，天然被排除，无需额外分支。）
        if (isAreaBlackRelative(32, 0, 32, 16)) {
            clearRelative(32, 0, 32, 16);
        }
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

    /** 把指定区块（以 64 为基准的坐标）清成完全透明 */
    private void clearRelative(int i, int i2, int i3, int i4) {
        int i5 = this.scale;
        int i6 = i * i5;
        int i7 = i2 * i5;
        int i8 = i3 * i5;
        int i9 = i4 * i5;
        for (int i10 = 0; i10 < i9; i10++) {
            for (int i11 = 0; i11 < i8; i11++) {
                this.normalizedTexture.setPixel(i6 + i11, i7 + i10, 0);
            }
        }
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
        // ★★★ 2026-10-02 修复「老格式皮肤 3D 渲染错位（脖子/腰出现黑色条、下巴与头顶贴图错位）」：
        //   细手臂（slim/Alex）模型是 1.8 才引入的，老格式 64x32 皮肤**只可能是 classic**。
        //   而下面的探测坐标 (50,16)/(54,20)/(42,48)/(46,52) 全是新格式独有的区域：
        //   老格式里这些位置本来就是空白/透明 → 必然被误判成 slim → 用细手臂模型渲染粗手臂皮肤
        //   → 手臂宽度不匹配、UV 整体错位 → 表现就是脖子一圈、腰间一圈黑色，下巴/头顶"贴图黑了"。
        if (this.oldFormat) {
            return false;
        }
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
