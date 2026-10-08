package com.qcl.launcher.skin.gltf

import android.content.Context
import android.content.SharedPreferences

/**
 * ★★★★★ 1.5.0：皮肤动画注册表 —— **照搬 FCL**
 * `FCL/src/main/java/com/mio/skin/SkinAnimations.kt` 的语义，落地到 QCL。
 *
 * 与 FCL 原版的差异（**只有持久化方式**）：
 * <ul>
 *   <li>FCL 用 DataStore（`com.mio.datastore.skinAnimationDataStore`）+ `lifecycleScope` 协程；</li>
 *   <li>QCL 改成最朴素的 {@link SharedPreferences}（同步、无协程依赖），
 *       免得把 FCL 的 datastore 模块一起搬进来。</li>
 * </ul>
 *
 * 动画条目与变体筛选规则 **完全照 FCL**：
 * <pre>
 *   entries    = [idle, idle_sub_1, idle_sub_2, idle_sub_3]   // 与 FCL 一致
 *   variantIds = entries 中 id 以 "idle_sub" 开头的             // 即 1/2/3
 *   DEFAULT_ID = "idle"
 *   validId()  : 未知 id（含旧存档）回退 default
 * </pre>
 *
 * ★ 模型里实际存在的 clip（读 classic-player.gltf 确认）：
 * `CINEMA_4D_Main` / `idle` / `idle_sub_1` / `idle_sub_2` / `idle_sub_3` / `interact`
 */
object SkinAnimations {

    /** 默认（基础待机）动画 id。 */
    const val DEFAULT_ID = "idle"

    /** 动画条目；`nameRes` 沿用 FCL 的结构但传 0（QCL 无同名资源，由选择器 UI 自己显示中文名）。 */
    class Entry(val id: String, val nameRes: Int)

    val entries = listOf(
        Entry("idle", 0),
        Entry("idle_sub_1", 0),
        Entry("idle_sub_2", 0),
        Entry("idle_sub_3", 0)
    )

    /** 校验动画 id，未知 id（含旧存档值）回退默认待机。 */
    fun validId(id: String): String =
        entries.firstOrNull { it.id == id }?.id ?: DEFAULT_ID

    /**
     * 待机变体 clip（随机插播候选，**不含**基础待机）。
     * ★ 规则照 FCL：`entries.drop(1).filter { it.id.startsWith("idle_sub") }`。
     */
    val variantIds: List<String> =
        entries.drop(1).filter { it.id.startsWith("idle_sub") }.map { it.id }

    private const val PREF = "qcl_skin_anim"
    private const val KEY = "animation_id"

    /** 读上次选择的动画（同步；读不到或非法都回默认待机）。 */
    fun restore(ctx: Context): String =
        validId(ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, DEFAULT_ID) ?: DEFAULT_ID)

    /** 保存当前动画为下次启动的选择。 */
    fun save(ctx: Context, id: String) {
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY, validId(id)).apply()
    }
}
