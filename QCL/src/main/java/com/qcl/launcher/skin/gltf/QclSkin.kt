package com.qcl.launcher.skin.gltf

import android.graphics.Bitmap

/**
 * ★★★★★ 1.5.0：皮肤归一化 —— QCL 版（替掉 FCL 的 `com.tungsten.fclcore.util.skin.NormalizedSkin`）。
 *
 * <p>FCL 用它做两件事：
 * <ol>
 *   <li><b>旧格式转换</b>：老皮肤（64×32）转成新格式（64×64），避免下层糊成一片；</li>
 *   <li><b>slim 检测</b>：按右臂贴图区的透明像素判断是不是细臂（3 像素宽）模型。</li>
 * </ol>
 *
 * <p>判定规则照 FCL（`SkinTextureLoader.isSlimArms` 的语义）：
 * 取右臂区域（x 46..48、y 20..32）里**不透明**列的宽度，≥4 像素算 classic，≤3 算 slim。
 *
 * <p>★ 与 FCL 的差异：FCL 那套还处理 Base64 传输/64×32 之外的怪尺寸，
 * 这里只保留渲染必需的两项，避免把 FCL 的整个 util 目录搬进来。
 */
class QclSkin(val originalTexture: Bitmap,
               val normalizedTexture: Bitmap?,
               val isOldFormat: Boolean,
               val isSlim: Boolean) {

    companion object {

        /** 皮肤不合规（例如空贴图）时抛出，对应 FCL 的 `InvalidSkinException`。 */
        class InvalidSkinException(message: String) : Exception(message)

        /** 标准皮肤尺寸。 */
        private const val W = 64
        private const val H = 64

        /**
         * 归一化：必要时把旧格式（64×32）放大为 64×64，并顺手检测 slim。
         *
         * @throws InvalidSkinException 贴图为空
         */
        @JvmStatic
        @Throws(InvalidSkinException::class)
        fun normalize(skin: Bitmap?): QclSkin {
            if (skin == null) {
                throw InvalidSkinException("皮肤贴图为空")
            }
            val oldFormat = skin.height == H / 2 && skin.width == W
            val normalized: Bitmap? = if (oldFormat) {
                Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888).also { dst ->
                    val src = Bitmap.createScaledBitmap(skin, W, H / 2, false)
                    // ★ Bitmap 本身没有 drawBitmap，要借 Canvas（老皮肤上下半身各画一次）
                    val canvas = android.graphics.Canvas(dst)
                    canvas.drawBitmap(src, 0f, 0f, null)
                    // 下半身沿用上半身（MC 老皮肤就是这么"补"出来的）
                    canvas.drawBitmap(src, 0f, (H / 2).toFloat(), null)
                    src.recycle()
                }
            } else {
                null
            }
            val use = normalized ?: skin
            return QclSkin(
                    originalTexture = skin,
                    normalizedTexture = normalized,
                    isOldFormat = oldFormat,
                    isSlim = !isSlimArms(use)
            )
        }

        /**
         * slim（细臂）检测：看右臂那几列的不透明像素宽度。
         * ★ 与 FCL 口径一致：宽 ≥ 4 视为 classic（粗臂），否则 slim。
         */
        @JvmStatic
        fun isSlimArms(bitmap: Bitmap): Boolean {
            val w = bitmap.width
            val h = bitmap.height
            if (w < W || h < 20) {
                return false
            }
            val x0 = 46
            val y0 = 20
            val y1 = minOf(32, h)
            var solid = 0
            for (x in x0 until minOf(x0 + 4, w)) {
                for (y in y0 until y1) {
                    if (bitmap.getPixel(x, y) ushr 24 != 0) {   // alpha != 0
                        solid++
                        break
                    }
                }
            }
            // 右臂有效列数 ≤3 ⇒ slim
            return solid <= 3
        }
    }
}
