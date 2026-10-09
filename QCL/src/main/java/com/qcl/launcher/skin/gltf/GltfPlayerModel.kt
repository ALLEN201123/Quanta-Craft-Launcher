package com.qcl.launcher.skin.gltf

import android.content.Context
import java.util.Random

/**
 * 玩家 GLTF 模型运行时：同时加载 classic/slim 两个模型实例（各约 131KB，
 * 解析毫秒级），切换模型零成本；动画播放/推进/绘制全部转发到当前实例。
 *
 * 待机时随机插播变体（语义同 Axolotl）：基础待机累计 [IDLE_VARIANT_INTERVAL_MIN,
 * INTERVAL_MAX] 秒后随机挑一个待机变体 clip 播一轮（播完回基础待机，避开上一次）；
 * 用户手动选择的动画不受影响，插播中被手动切换立即让位。
 */
class GltfPlayerModel(context: Context) {

    private val classic = GltfModel.load(context, CLASSIC_ASSET, isSlim = false)
    private val slimModel = GltfModel.load(context, SLIM_ASSET, isSlim = true)
    private var active = classic

    var slim = false
        private set

    /** 当前动画 id（clip 名） */
    var animationId: String = SkinAnimations.DEFAULT_ID
        private set

    /** 待机变体插播状态：null=未插播 */
    private var variant: String? = null
    private var idleTimer = 0f
    private var lastVariant: String? = null
    private val random = Random()

    /** 切换 classic/slim：新实例重放到当前动画（切换仅在换肤时发生，重置进度无感知） */
    fun setSlim(value: Boolean) {
        if (slim == value) {
            return
        }
        slim = value
        active = if (value) slimModel else classic
        active.playAnimation(animationId)
    }

    /**
     * ★ 2026-10-09：切换体素化第二层（两实例同步，避免 slim 切换后状态不一致）。
     * 关闭时第二层回落零厚度面片（照 FCL 的 `GltfPlayerModel.setSolidLayerEnabled`）。
     */
    fun setSolidLayerEnabled(enabled: Boolean) {
        classic.solidLayerEnabled = enabled
        slimModel.solidLayerEnabled = enabled
    }

    /**
     * ★ 2026-10-09 补回 FCL 的这一对（此前整段缺失）：
     * 切换身体与腿部分离（两实例同步，避免 slim 切换后状态不一致）。
     * 关闭时上身与腿部贴合，腰部共面接缝可能闪烁。
     */
    fun setUpperBodySeparated(separated: Boolean) {
        classic.upperBodySeparated = separated
        slimModel.upperBodySeparated = separated
        // ★ 关键：字段赋值后必须把抬起量真正应用到 rest 矩阵，否则改了开关模型没重抬 = 白改。
        //   `applyUpperBodyLift` 内部按 `appliedLift` 判重、未加载时 nodes 为空自然空转，
        //   所以不需要额外的 isLoaded 判断（与 FCL 同一套语义）。
        classic.applyUpperBodyLift()
        slimModel.applyUpperBodyLift()
    }

    /** 播放指定烘焙动画（clip 名），未知 id 忽略；手动选择重置插播计时 */
    fun playAnimation(id: String) {
        if (active.playAnimation(id)) {
            animationId = id
            variant = null
            idleTimer = 0f
        }
    }

    fun update(deltaSeconds: Float) {
        val playingVariant = variant
        if (playingVariant == null) {
            // 基础待机累计计时，到点随机挑一个变体（排除上一次）插播
            if (animationId == SkinAnimations.DEFAULT_ID) {
                idleTimer += deltaSeconds
                if (idleTimer >= nextVariantDelay()) {
                    SkinAnimations.variantIds
                        .filter { it != lastVariant && active.findClip(it) != null }
                        .randomOrNull()
                        ?.let { startVariant(it) }
                    idleTimer = 0f
                }
            } else {
                idleTimer = 0f
            }
        } else if (active.clipElapsedAtLeast(playingVariant)) {
            // 变体播完一轮（累计经过时间达到 clip 时长，time 已回绕），回基础待机
            animationId = SkinAnimations.DEFAULT_ID
            active.playAnimation(SkinAnimations.DEFAULT_ID)
            lastVariant = playingVariant
            variant = null
        }
        active.update(deltaSeconds)
    }

    private fun startVariant(id: String) {
        variant = id
        active.playAnimation(id)
    }

    private fun nextVariantDelay(): Float =
        IDLE_VARIANT_INTERVAL_MIN + random.nextFloat() * IDLE_VARIANT_INTERVAL_RANGE

    /** 绘制皮肤材质网格（须已绑定皮肤纹理） */
    fun drawSkin(
        positionLocation: Int,
        texCoordLocation: Int,
        normalLocation: Int,
        lightMixLocation: Int,
        mvpMatrixLocation: Int,
        normalMatrixLocation: Int,
        mvpBase: FloatArray,
        modelBase: FloatArray
    ) {
        active.draw(
            positionLocation, texCoordLocation, normalLocation, lightMixLocation,
            mvpMatrixLocation, normalMatrixLocation, mvpBase, modelBase, capeOnly = false
        )
    }

    /** 绘制披风网格（须已绑定披风纹理） */
    fun drawCape(
        positionLocation: Int,
        texCoordLocation: Int,
        normalLocation: Int,
        lightMixLocation: Int,
        mvpMatrixLocation: Int,
        normalMatrixLocation: Int,
        mvpBase: FloatArray,
        modelBase: FloatArray
    ) {
        active.draw(
            positionLocation, texCoordLocation, normalLocation, lightMixLocation,
            mvpMatrixLocation, normalMatrixLocation, mvpBase, modelBase, capeOnly = true
        )
    }

    /**
     * 用皮肤像素重建体素化第二层（仅渲染线程调用）。
     * 两个实例都重建，避免 slim 切换后体素层缺失而退回面片。
     */
    fun rebuildSolidLayers(pixels: IntArray, bitmapWidth: Int) {
        classic.rebuildSolidLayers(pixels, bitmapWidth)
        slimModel.rebuildSolidLayers(pixels, bitmapWidth)
    }

    /** EGL context 重建后调用：两个实例的 VBO id 均可能失效，一并重置 */
    fun resetGpuResources() {
        classic.resetGpuResources()
        slimModel.resetGpuResources()
    }

    companion object {
        private const val CLASSIC_ASSET = "img/skin_model/classic-player.gltf"
        private const val SLIM_ASSET = "img/skin_model/slim-player.gltf"

        /** 待机变体插播间隔：8~15 秒随机 */
        private const val IDLE_VARIANT_INTERVAL_MIN = 8f
        private const val IDLE_VARIANT_INTERVAL_RANGE = 7f
    }
}
