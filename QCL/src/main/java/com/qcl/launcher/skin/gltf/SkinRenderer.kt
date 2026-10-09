package com.qcl.launcher.skin.gltf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.tan

/**
 * 皮肤 3D 渲染器（GLES 2.0，由 [SkinViewer] 的渲染线程驱动 onSurfaceCreated /
 * onSurfaceChanged / onDrawFrame）。模型为 GLTF 资产（刚体层级，详见 [GltfModel]），
 * 动画为模型内烘焙的 clip（详见 [GltfPlayerModel]）。
 *
 * 相机在 +Z 轴上对准原点：fov=50、zoom=0.9，
 * 距离 = 4.5 + 16.5/tan(fov/2)/zoom/scale（clamp 10~256），
 * scale 为手势缩放（等价旧版 0.7~2.0 的模型缩放）。
 * 纹理经 GLUtils.texImage2D 上传（预乘 alpha），混合 GL_ONE/GL_ONE_MINUS_SRC_ALPHA，
 * 片元 alpha<0.1 丢弃（等价旧版 glAlphaFunc）。基础网格无光照；
 * 体素化第二层（[SolidSkinLayer]）带面法线，用环境光 + 方向漫反射增强立体感。
 */
class SkinRenderer(context: Context) {

    private val appContext = context.applicationContext

    private val model = GltfPlayerModel(appContext)

    /** 当前播放的动画 id（模型内烘焙 clip 名），默认待机 */
    var animationId: String = SkinAnimations.DEFAULT_ID
        private set

    init {
        // 默认播放待机（账户弹窗等不调用 restoreSkinAnimation 的场景也有动画）
        model.playAnimation(animationId)
        // ★ 2026-10-09：初始默认皮肤是 **steve（classic / 非 slim）**，
        //   模型必须与之一致 —— 否则手臂会采样到透明贴图区，看起来"糙、只有一层"。
        //   ★ 这里**不能照抄 FCL 的 `model.setSlim(true)`**：FCL 的 `defaultSkin()` 读的是
        //   `/assets/img/alex.png`，alex 是 slim 布局，所以 FCL 设 slim 是对的；
        //   而我们把默认皮肤换成了 `steve.png`（1.5.0 为修「离线账号显示成艾利克斯」改的），
        //   若还照抄 setSlim(true) 就成了**模型与贴图不匹配** ⇒ 正是用户实测的粗糙观感。
        //   ⇒ 真正的皮肤进来时由 `consumePendingUpdate()` 按 `pendingSlim` 自动切对。
        model.setSlim(false)
    }

    /**
     * ★ 1.5.0：主线程 Handler —— 替掉 FCL 的 `Schedulers.androidUIThread()`。
     * 皮肤归一化（Bitmap 读像素）必须在主线程做：与 FCL 原逻辑一致。
     */
    private fun androidMain(): android.os.Handler =
        androidMainHandler ?: android.os.Handler(android.os.Looper.getMainLooper())
            .also { androidMainHandler = it }

    private var androidMainHandler: android.os.Handler? = null

    // 手势状态（UI 线程写、渲染线程读，单字段读写无需同步）
    private var scale = 1f
    private var rotationX = 0f
    private var rotationY = 0f

    /** 当前绑定的皮肤/披风位图（UI 线程读写，[setTexture] 更新） */
    var texture: Array<Bitmap?> = arrayOf(defaultSkin(), null)
        private set

    // 渲染线程状态
    private var program = 0
    private var positionLocation = 0
    private var texCoordLocation = 0
    private var normalLocation = 0
    private var mvpMatrixLocation = 0
    private var normalMatrixLocation = 0
    private var lightMixLocation = 0
    private var skinTextureId = 0
    private var capeTextureId = 0
    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val pvMatrix = FloatArray(16)
    private val wrapperMatrix = FloatArray(16)
    private val wrapperMvp = FloatArray(16)

    // UI 线程 → 渲染线程 的待处理纹理更新
    @Volatile
    private var pendingSkin: Bitmap? = null

    @Volatile
    private var pendingCape: Bitmap? = null

    /**
     * ★ 2026-10-11：是否需要**显式清空**披风。
     * 与 {@code pendingCape == null} 区分：后者现在表示"这次不改披风"（保留原贴图），
     * 只有本标志为 true 才真的删掉披风贴图（玩家点「隐藏披风」）。
     */
    @Volatile
    private var pendingClearCape: Boolean = false

    // ★ 默认 false：与 `defaultSkin()` 读的 steve.png（classic 布局）一致。
    //   FCL 这里是 true，因为它默认贴图是 alex.png（slim 布局）——别照抄。
    @Volatile
    private var pendingSlim = false

    @Volatile
    private var pendingHasUpdate = false

    // ---- FCL 的两个渲染开关（1.5.0 补齐，此前整段缺失） ----

    /**
     * 体素化第二层开关（任意线程可写、渲染线程读）；关闭时第二层回落零厚度面片。
     *
     * ★ 与 FCL 对齐：`SkinRenderer` 暴露这个开关、由 UI 决定初始值。
     *   我们侧此前**只有模型层的 `GltfModel.solidLayerEnabled`**，渲染器这一层没有，
     *   于是「第二层」到底开没开完全取决于模型默认值，谁也关不掉/切不动。
     */
    @Volatile
    var solidLayerEnabled = true
        set(value) {
            field = value
            model.setSolidLayerEnabled(value)
        }

    /**
     * 身体与腿部分离开关：关闭时上身与腿部贴合（腰部接缝可能闪烁）。
     * 抬起量在模型加载时已硬编码应用（`GltfModel` 里 `UPPER_BODY_LIFT`），
     * 这里只把状态同步给模型，供 UI 切换。
     */
    @Volatile
    var upperBodySeparated = true
        set(value) {
            field = value
            pendingSeparation = true
        }

    /** 分离开关待应用标志：`rest` 局部矩阵修改较重，交渲染线程消费以免与绘制竞争。 */
    @Volatile
    private var pendingSeparation = false

    // ---- 对外 API（任意线程可调）----

    /**
     * 更新皮肤纹理，模型类型从皮肤图像自动检测。
     * 同步更新 [texture]（当前纹理可读回，attach 重喂时不会退回默认皮肤）。
     */
    fun updateTexture(skin: Bitmap?, cape: Bitmap?) {
        applyTextureCache(skin, cape)
        scheduleTextureUpdate(skin, cape, null)
    }

    /**
     * 更新皮肤纹理并显式指定模型（[slim] 覆盖图像自动检测）。
     */
    fun updateTexture(skin: Bitmap?, cape: Bitmap?, slim: Boolean) {
        applyTextureCache(skin, cape)
        scheduleTextureUpdate(skin, cape, slim)
    }

    /**
     * ★★★★★ 2026-10-11 新增：**带保护地**更新跨上下文重建缓存。
     *
     * <p>真凶（用户实测「主界面皮肤永远不变 / 一会儿又变回默认皮肤」）：
     * 原来两个 updateTexture 都是 `texture = arrayOf(skin, cape)` ——
     * **无条件覆盖**。而调用方很容易传 null：
     * <ul>
     *   <li>cape：{@code MainUI.refreshAccountModel()} 里
     *       {@code decodeCape(account.capeTexture)} 解码失败就返回 null；</li>
     *   <li>skin：某次刷新时 account.texture 还没回填。</li>
     * </ul>
     * 一旦缓存被 null 覆盖，之后**每次 EGL 上下文重建**（切页面 / Activity 重建 /
     * surface 重来）{@code onSurfaceCreated()} 都会拿 null 去恢复
     * ⇒ 人物**回到默认皮肤、披风消失**，玩家看到的就是"我换的皮肤永远没生效"。
     *
     * <p>所以语义与渲染端保持一致：
     * <b>传 null = 这次不改这一类纹理</b>，绝不用 null 覆盖已有缓存。
     * 只有显式 {@code clearCape()} 才清披风缓存。
     */
    private fun applyTextureCache(skin: Bitmap?, cape: Bitmap?) {
        try {
            if (skin != null) {
                texture[0] = skin
            }
            if (cape != null) {
                texture[1] = cape
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 设置当前皮肤/披风位图并触发渲染更新（与 [updateTexture] 等价，保留旧 API 名）。
     */
    fun setTexture(skin: Bitmap?, cape: Bitmap?) = updateTexture(skin, cape)

    /** 单指拖动旋转，输入为像素增量（已除以屏幕密度） */
    fun rotateStep(dx: Float, dy: Float) {
        if (abs(dx) >= 1f) {
            rotationY += ROTATE_STEP * dx
        }
        if (abs(dy) >= 1f) {
            rotationX += ROTATE_STEP * dy
        }
    }

    fun getScale(): Float = scale

    fun setScale(value: Float) {
        scale = value.coerceIn(MIN_SCALE, MAX_SCALE)
    }

    /**
     * 切换动画（模型内烘焙 clip 名，未知 id 回退默认）。
     */
    fun playAnimation(id: String) {
        val valid = SkinAnimations.validId(id)
        model.playAnimation(valid)
        animationId = model.animationId
    }

    // ---- 渲染线程回调（由 SkinViewer 驱动）----

    fun onSurfaceCreated() {
        // EGL context 是新建的：旧 context 的纹理/VBO id 均已失效，先归零再重建
        skinTextureId = 0
        capeTextureId = 0
        model.resetGpuResources()
        program = buildProgram()
        positionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordLocation = GLES20.glGetAttribLocation(program, "aTexCoord")
        normalLocation = GLES20.glGetAttribLocation(program, "aNormal")
        mvpMatrixLocation = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        normalMatrixLocation = GLES20.glGetUniformLocation(program, "uNormalMatrix")
        lightMixLocation = GLES20.glGetUniformLocation(program, "uLightMix")
        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glEnableVertexAttribArray(texCoordLocation)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        texture[0]?.let { skinTextureId = uploadTexture(skinTextureId, it) }
        // ★ 2026-10-09 补回 FCL 的这一行（此前被删）：披风纹理也要上传，
        //   否则「有披风但画面上什么都没有」。
        texture[1]?.let { capeTextureId = uploadTexture(capeTextureId, it) }
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        Matrix.perspectiveM(projMatrix, 0, FOV, width.toFloat() / height, NEAR_PLANE, FAR_PLANE)
    }

    fun onDrawFrame(deltaSeconds: Float) {
        consumePendingUpdate()
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        model.update(deltaSeconds)

        val distance = cameraDistance()
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, distance, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(pvMatrix, 0, projMatrix, 0, viewMatrix, 0)
        // 模型整体旋转（拖动手势）：先 X 后 Y，与旧版固定管线旋转顺序一致
        Matrix.setIdentityM(wrapperMatrix, 0)
        Matrix.rotateM(wrapperMatrix, 0, rotationX, 1f, 0f, 0f)
        Matrix.rotateM(wrapperMatrix, 0, rotationY, 0f, 1f, 0f)
        Matrix.multiplyMM(wrapperMvp, 0, pvMatrix, 0, wrapperMatrix, 0)

        GLES20.glUseProgram(program)
        if (skinTextureId != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, skinTextureId)
            model.drawSkin(
                positionLocation, texCoordLocation, normalLocation, lightMixLocation,
                mvpMatrixLocation, normalMatrixLocation, wrapperMvp, wrapperMatrix
            )
        }
        if (capeTextureId != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, capeTextureId)
            model.drawCape(
                positionLocation, texCoordLocation, normalLocation, lightMixLocation,
                mvpMatrixLocation, normalMatrixLocation, wrapperMvp, wrapperMatrix
            )
        }
    }

    // ---- 内部实现 ----

    private fun cameraDistance(): Float {
        val distance = 4.5f + 16.5f / FOV_TAN / ZOOM / scale
        return distance.coerceIn(10f, 256f)
    }

    /** 在 UI 线程做皮肤归一化（旧格式转换/slim 检测），再交由渲染线程下一帧消费 */
    private fun scheduleTextureUpdate(skin: Bitmap?, cape: Bitmap?, slimOverride: Boolean?) {
        androidMain().post {            try {
                val normalized = QclSkin.normalize(skin)
                pendingSkin =
                    if (normalized.isOldFormat) normalized.normalizedTexture else normalized.originalTexture
                pendingCape = cape
                pendingSlim = slimOverride ?: normalized.isSlim
                pendingHasUpdate = true
                // ★★★★★ 2026-10-11【关键修复】把新纹理**同步写进跨 GL 上下文的重建缓存**。
                //
                //   真凶：`texture` 数组（第 64 行 `arrayOf(defaultSkin(), null)`）**从来没被更新过**
                //   —— scheduleTextureUpdate 只写 pendingSkin/pendingCape，不写 texture[]。
                //   而 onSurfaceCreated() 里是：
                //       texture[0]?.let { skinTextureId = uploadTexture(skinTextureId, it) }
                //       texture[1]?.let { capeTextureId = uploadTexture(capeTextureId, it) }
                //   于是**每次 EGL 上下文重建**（切页面、Activity 重建、surface 重来）
                //   都会把人物恢复到**默认皮肤**、并且**披风消失**
                //   —— 玩家看到的就是"我换的皮肤永远没生效 / 一会儿又变回去"。
                //
                //   现在写入缓存后，上下文重建时能正确恢复"最后一次真正生效的皮肤/披风"。
                texture[0] = pendingSkin
                if (cape != null) {
                    texture[1] = cape
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(appContext, "Skin Renderer: $e", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun consumePendingUpdate() {
        // ★ 2026-10-09 补回 FCL 的分离开关消费（此前整段缺失）
        if (pendingSeparation) {
            pendingSeparation = false
            model.setUpperBodySeparated(upperBodySeparated)
        }
        if (!pendingHasUpdate) {
            return
        }
        pendingHasUpdate = false
        model.setSlim(pendingSlim)
        pendingSkin?.let {
            skinTextureId = uploadTexture(skinTextureId, it)
            // 用皮肤像素重建体素化第二层（边缘实心化）
            val pixels = IntArray(it.width * it.height)
            it.getPixels(pixels, 0, it.width, 0, 0, it.width, it.height)
            model.rebuildSolidLayers(pixels, it.width)
        }
        pendingSkin = null
        // ★★★★★ 2026-10-11 修（用户实测「上传皮肤之后主界面人物的披风被搞没了」）：
        //
        //   原来这里是：
        //     val cape = pendingCape
        //     if (cape != null) { uploadTexture(...) }
        //     else if (capeTextureId != 0) { glDeleteTextures(...); capeTextureId = 0 }   ← 传 null 就删披风
        //
        //   也就是说**只要有一次 updateTexture 没带披风（cape = null），披风贴图就被删掉**。
        //   而换肤/刷新路径上很容易出现"只更新皮肤、暂时拿不到披风位图"的情况
        //   （比如 account.capeTexture 还没回填、或解码失败返回 null），
        //   于是玩家看到的就是「换了皮肤 → 披风没了」。
        //
        //   现在改成：**cape 为 null 时保留原有披风贴图**，只有显式调用
        //   {@code clearCape()} 才真正清空。语义更安全：
        //     · 没提供披风 = 这次不改披风（而不是"把披风删掉"）
        //     · 玩家点「隐藏披风」时走 clearCape()，行为与以前一致
        val cape = pendingCape
        if (cape != null) {
            capeTextureId = uploadTexture(capeTextureId, cape)
            // ★ 2026-10-11：同步进跨上下文重建缓存，否则下次 onSurfaceCreated 披风就没了
            texture[1] = cape
        } else if (pendingClearCape && capeTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(capeTextureId), 0)
            capeTextureId = 0
            // ★ 2026-10-11：缓存也一并清掉，否则重建时会把"已隐藏的披风"又画回来
            texture[1] = null
        }
        pendingCape = null
        pendingClearCape = false
    }

    /**
     * ★ 2026-10-11 新增：**显式**清空披风（玩家点「隐藏披风」时用）。
     * 与"传 null"区分开 —— 传 null 现在是"这次不改披风"。
     */
    fun clearCape() {
        val empty = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        androidMain().post {
            try {
                pendingCape = null
                pendingClearCape = true
                pendingHasUpdate = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        empty.recycle()
    }

    private fun uploadTexture(existing: Int, bitmap: Bitmap): Int {
        if (existing != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(existing), 0)
        }
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_NEAREST
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        return ids[0]
    }

    private fun buildProgram(): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw IllegalStateException("Skin shader link failed: $log")
        }
        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw IllegalStateException("Skin shader compile failed: $log")
        }
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uNormalMatrix;
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            attribute vec3 aNormal;
            varying vec2 vTexCoord;
            varying vec3 vNormal;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vTexCoord = aTexCoord;
                vNormal = (uNormalMatrix * vec4(aNormal, 0.0)).xyz;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexture;
            uniform float uLightMix;
            varying vec2 vTexCoord;
            varying vec3 vNormal;
            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                if (color.a < 0.1) discard;
                // 体素第二层：环境光 0.70 + 方向漫反射 0.42（光源归一化 (2,4,3)，同 Axolotl）；
                // 基础网格 lightMix=0 保持无光照观感
                float light = mix(1.0, 0.70 + 0.42 * max(dot(normalize(vNormal), vec3(0.371, 0.743, 0.557)), 0.0), uLightMix);
                gl_FragColor = vec4(color.rgb * light, color.a);
            }
        """

        private const val FOV = 50f
        private const val ZOOM = 0.9f
        private const val NEAR_PLANE = 0.5f
        private const val FAR_PLANE = 1500f
        private const val MIN_SCALE = 0.7f
        private const val MAX_SCALE = 2.0f
        private const val ROTATE_STEP = 2f

        private val FOV_TAN = tan(Math.toRadians(FOV / 2.0)).toFloat()

        /**
         * ★ 1.5.0 修正（用户实测「离线账号是史蒂夫，却显示成艾利克斯」）：
         * FCL 原版这里写死读 `/assets/img/alex.png` ⇒ 兜底贴图永远是**艾利克斯**。
         * QCL 的 `assets/img/` 里确实有 `steve.png`，改为优先用它，
         * 读不到再退回 alex（保证任何情况下都有贴图，不会"贴图丢失=空人"）。
         */
        private fun defaultSkin(): Bitmap? {
            val sm = SkinRenderer::class.java.getResourceAsStream("/assets/img/steve.png")
            if (sm != null) {
                BitmapFactory.decodeStream(sm)?.let { return it }
            }
            return SkinRenderer::class.java.getResourceAsStream("/assets/img/alex.png")
                ?.let { BitmapFactory.decodeStream(it) }
        }
    }
}
