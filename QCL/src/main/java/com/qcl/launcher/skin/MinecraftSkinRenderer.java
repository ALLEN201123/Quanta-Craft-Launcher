package com.qcl.launcher.skin;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.SystemClock;
import com.qcl.launcher.skin.utils.TextureHelper;
import com.qcl.launcher.skin.utils.Utils;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/* loaded from: classes2.dex */
public class MinecraftSkinRenderer implements GLSurfaceView.Renderer {
    public static float[] light0Position = {0.0f, 0.0f, 5100.0f, 0.0f};
    public Bitmap cape;
    public boolean changeSkinImage;
    private int mBackTexData;
    public GameCharacter mCharacter;
    private int[] mCharacterTexData;
    private Context mContext;
    public String path;
    public float[] plane_texcords;
    protected float[] plane_vertices;
    public Bitmap skin;
    boolean superRun;
    boolean updateBitmapSkin;
    // ★★★ 1.1.5：背景色（默认黑色透明，微软换皮对话框设白色）
    private float bgR = 0.0f, bgG = 0.0f, bgB = 0.0f, bgA = 0.0f;

    /* ★★★ 1.5.0：3D 人物的**动作**（用户：「目前只有走路动作，能不能添加更多」）。
       原来 onDrawFrame 里硬编码 SetRunning(true) + setWalkSwing(sin(...))，
       ⇒ 永远只有「走路」一种动作。现改为按 animMode 驱动：

         0 = 站立（idle）  ：四肢归位，只轻微呼吸（Y 轴极缓浮动）
         1 = 走路（walk）  ：原行为，手脚交替摆动（幅度 22）
         2 = 跑步（run）   ：同走路但更快更大（幅度 40、周期 150ms）
         3 = 旋转（spin）  ：原地匀速转圈展示（Y 轴 360°，四肢不动）
         4 = 挥手（wave）  ：站姿 + 单臂上下挥动（用整体摆动模拟 + 身体轻微侧倾）

       ★ 实现约束：GameCharacter 只暴露 setWalkSwing（同时驱动手脚）与
         setX/Y/ZRotation（整体旋转），**没有单独控制某条手臂的 API**。
         所以「挥手」是用「小幅摆动 + 身体侧倾」合成的观感，不是真·单臂动画。
       ★ 默认值 = 1（走路），与改动前完全一致 —— 存量用户无感知。 */
    public static final int ANIM_IDLE = 0;
    public static final int ANIM_WALK = 1;
    public static final int ANIM_RUN = 2;
    public static final int ANIM_SPIN = 3;
    public static final int ANIM_WAVE = 4;
    /** ★ 1.5.0：待机变体 3（点头），凑齐 FCL 的 idle_sub_1/2/3 三个变体。 */
    public static final int ANIM_NOD = 5;

    /** ★ 1.5.0：待机变体池（对齐 FCL 的 `SkinAnimations.variantIds` = idle_sub_1/2/3）。 */
    private static final int[] IDLE_VARIANTS = {ANIM_WAVE, ANIM_SPIN, ANIM_NOD};
    /** ★ 1.5.0：变体插播间隔下限 8 秒（照 FCL `IDLE_VARIANT_INTERVAL_MIN = 8f`）。 */
    private static final long IDLE_VARIANT_MIN_MS = 8000L;
    /** ★ 1.5.0：随机浮动 7 秒 ⇒ 实际 8~15 秒（照 FCL `IDLE_VARIANT_INTERVAL_RANGE = 7f`）。 */
    private static final long IDLE_VARIANT_RANGE_MS = 7000L;
    /** ★ 1.5.0：单个变体播多久就回待机（照 FCL「播完一轮回基础待机」）。 */
    private static final long IDLE_VARIANT_DUR_MS = 2600L;

    /** ★ 1.5.0：玩家手动选择的动作；只有它等于 ANIM_IDLE 时才插播变体（照 FCL）。 */
    private int userAnimMode = ANIM_IDLE;
    /** ★ 1.5.0：当前实际播的动作（可能是插播中的变体）。 */
    private int playAnimMode = ANIM_IDLE;
    /** ★ 1.5.0：待机计时与变体状态（对齐 FCL 的 variant / idleTimer / lastVariant）。 */
    private int curIdleVariant = -1;
    private int lastIdleVariant = -1;
    private long idleAccumMs = 0L;
    private long variantStartMs = 0L;
    private long lastFrameMs = 0L;
    private final java.util.Random idleRandom = new java.util.Random();

    /**
     * ★ 1.5.0：**默认改为待机**（原来是 ANIM_WALK）。
     * 用户实测「一直走路」观感不对；FCL 默认也是 idle（基础待机），
     * 动作变化由「待机随机变体」提供，而不是一直走。
     */
    private int animMode = ANIM_IDLE;

    public void setAnimMode(int mode) {
        if (mode < ANIM_IDLE || mode > ANIM_NOD) {
            return;
        }
        this.userAnimMode = mode;
        this.animMode = mode;
        // 玩家手动切换动作 ⇒ 打断插播并复位计时（照 FCL playAnimation 的处理）
        this.curIdleVariant = -1;
        this.idleAccumMs = 0L;
    }

    public int getAnimMode() {
        return this.userAnimMode;
    }

    /**
     * ★ 1.5.0：挑一个待机变体索引，**排除上一次**（照 FCL
     * {@code SkinAnimations.variantIds.filter { it != lastVariant }.randomOrNull()}）。
     */
    private int pickIdleVariantExcept(int except) {
        if (IDLE_VARIANTS.length <= 1) {
            return IDLE_VARIANTS.length - 1;
        }
        int idx;
        int guard = 0;
        do {
            idx = idleRandom.nextInt(IDLE_VARIANTS.length);
            guard++;
        } while (idx == except && guard < 8);
        return idx;
    }

    public void setBackgroundColor(float r, float g, float b, float a) {
        this.bgR = r;
        this.bgG = g;
        this.bgB = b;
        this.bgA = a;
    }

    public MinecraftSkinRenderer(Context context) {
        this.mCharacterTexData = new int[]{0, 0};
        this.changeSkinImage = false;
        this.plane_texcords = new float[]{0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f};
        this.plane_vertices = new float[]{-200.0f, -100.0f, -100.0f, -200.0f, 100.0f, -100.0f, 200.0f, 100.0f, -100.0f, 200.0f, -100.0f, -100.0f};
        this.updateBitmapSkin = false;
        this.superRun = false;
        this.mContext = context;
        this.mCharacter = new GameCharacter();
    }

    public MinecraftSkinRenderer(Context context, int i) {
        this.mCharacterTexData = new int[]{0, 0};
        this.changeSkinImage = false;
        this.plane_texcords = new float[]{0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f};
        this.plane_vertices = new float[]{-200.0f, -100.0f, -100.0f, -200.0f, 100.0f, -100.0f, 200.0f, 100.0f, -100.0f, 200.0f, -100.0f, -100.0f};
        this.updateBitmapSkin = false;
        this.superRun = false;
        this.mContext = context;
        this.mCharacter = new GameCharacter(i);
    }

    public MinecraftSkinRenderer(Context context, int i, boolean z) {
        this.mCharacterTexData = new int[]{0, 0};
        this.changeSkinImage = false;
        this.plane_texcords = new float[]{0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f};
        this.plane_vertices = new float[]{-200.0f, -100.0f, -100.0f, -200.0f, 100.0f, -100.0f, 200.0f, 100.0f, -100.0f, 200.0f, -100.0f, -100.0f};
        this.updateBitmapSkin = false;
        this.superRun = false;
        this.mContext = context;
        this.mCharacter = new GameCharacter(z, i);
    }

    public MinecraftSkinRenderer(Context context, boolean z) {
        this.mCharacterTexData = new int[]{0, 0};
        this.changeSkinImage = false;
        this.plane_texcords = new float[]{0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f};
        this.plane_vertices = new float[]{-200.0f, -100.0f, -100.0f, -200.0f, 100.0f, -100.0f, 200.0f, 100.0f, -100.0f, 200.0f, -100.0f, -100.0f};
        this.updateBitmapSkin = false;
        this.superRun = false;
        this.mContext = context;
        this.mCharacter = new GameCharacter(z);
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onDrawFrame(GL10 gl10) {
        if (this.changeSkinImage) {
            this.changeSkinImage = false;
        }
        GameCharacter gameCharacter = this.mCharacter;
        if (gameCharacter != null) {
            // ★★★★★ 1.5.0：**待机随机变体状态机**（照 FCL `GltfPlayerModel.update` 完整搬过来）
            //   FCL 原版规则：① 基础待机累计到 [8,15) 秒随机点 ⇒ 挑一个变体插播（**排除上一次**）；
            //   ② 变体播完一轮 ⇒ 回基础待机；③ 玩家手动选的动作 ⇒ 不插播、计时归零。
            long now = SystemClock.uptimeMillis();
            long delta = (lastFrameMs == 0L) ? 16L : (now - lastFrameMs);
            if (delta < 0L || delta > 500L) {
                delta = 16L;   // 首帧 / 从后台切回来，别让计时跳一大截
            }
            lastFrameMs = now;
            if (userAnimMode == ANIM_IDLE) {
                if (curIdleVariant < 0) {
                    idleAccumMs += delta;
                    long interval = IDLE_VARIANT_MIN_MS
                            + (long) (idleRandom.nextFloat() * (float) IDLE_VARIANT_RANGE_MS);
                    if (idleAccumMs >= interval) {
                        int idx = pickIdleVariantExcept(lastIdleVariant);
                        if (idx >= 0) {
                            curIdleVariant = idx;
                            lastIdleVariant = idx;
                            variantStartMs = now;
                        }
                        idleAccumMs = 0L;
                    }
                } else if (now - variantStartMs >= IDLE_VARIANT_DUR_MS) {
                    curIdleVariant = -1;   // 这一轮播完 → 回基础待机
                    idleAccumMs = 0L;
                }
            } else {
                idleAccumMs = 0L;
            }
            playAnimMode = (curIdleVariant >= 0) ? IDLE_VARIANTS[curIdleVariant] : userAnimMode;
            // ★★★ 1.5.0：按实际要播的动作驱动（原为 this.animMode）
            long t = now;
            switch (playAnimMode) {
                case ANIM_IDLE: {
                    // 站立：四肢归位 + 极缓的上下浮动（呼吸感），不摆臂
                    gameCharacter.SetRunning(false);
                    gameCharacter.setWalkSwing(0.0f);
                    gameCharacter.setYRotation((int) (Math.sin(t / 900.0d) * 2.0d));
                    break;
                }
                case ANIM_RUN: {
                    // 跑步：摆幅更大（40）、周期更短（150ms）→ 明显比走路急
                    gameCharacter.SetRunning(true);
                    gameCharacter.setWalkSwing((float) (Math.sin(t / 150.0d) * 40.0d));
                    gameCharacter.setYRotation(0);
                    break;
                }
                case ANIM_SPIN: {
                    // 旋转展示：原地匀速转圈（4 秒一圈），四肢不动
                    gameCharacter.SetRunning(false);
                    gameCharacter.setWalkSwing(0.0f);
                    gameCharacter.setYRotation((int) ((t % 4000L) * 0.09f));
                    break;
                }
                case ANIM_WAVE: {
                    // ★★★★★ 1.5.0 修复（用户实测「手和身体都分离了」）：
                    //   原来这里写的是 `SetRunning(true)` + 摆幅 **60**，而 Pojav 的
                    //   `SetRunning(true)` 会把手臂/腿切到**跑步姿态**，再叠 60° 摆臂 ⇒
                    //   手臂被甩到极端角度，看起来"和身体断开"。
                    //   ★ 改成与 ANIM_IDLE 一致的站姿（SetRunning(false) + 小摆幅 + 呼吸浮动），
                    //     只是摆臂幅度略大一点、周期略快，让它看起来"在动"而不是"静止"，
                    //     同时**保证肢体不会脱节**。
                    gameCharacter.SetRunning(false);
                    gameCharacter.setWalkSwing((float) (Math.sin(t / 420.0d) * 14.0d));
                    gameCharacter.setYRotation((int) (Math.sin(t / 900.0d) * 2.0d));
                    gameCharacter.setZRotation((int) (Math.sin(t / 420.0d) * 1.5d));
                    break;
                }
                case ANIM_NOD: {
                    // ★ 1.5.0 新增：点头（对应 FCL 的 idle_sub_3）—— 身体前后点 + 呼吸
                    gameCharacter.SetRunning(false);
                    gameCharacter.setWalkSwing(0.0f);
                    gameCharacter.setYRotation((int) (Math.sin(t / 900.0d) * 2.0d));
                    gameCharacter.setZRotation((int) (Math.sin(t / 340.0d) * 5.0d));
                    break;
                }
                default: {
                    // ★ 走路：摆幅由 22 → **45**（用户实测「只会迈开一只脚一只手」）。
                    //   GameCharacter.setWalkSwing 里左腿/右臂=-f、右腿/左臂=+f（对侧），
                    //   逻辑本身没错；问题是 **22° 幅度太小**，在人物框里几乎看不出在迈步。
                    gameCharacter.SetRunning(true);
                    gameCharacter.setWalkSwing((float) (Math.sin(t / 260.0d) * 45.0d));
                    gameCharacter.setYRotation(0);
                    break;
                }
            }
        }
        if (this.updateBitmapSkin) {
            Bitmap bitmap = this.skin;
            if (bitmap != null) {
                this.mCharacterTexData = TextureHelper.loadGLTextureFromBitmap(bitmap, this.cape, gl10);
            }
            this.updateBitmapSkin = false;
        }
        gl10.glClearColor(this.bgR, this.bgG, this.bgB, this.bgA);
        gl10.glClear(16640);
        gl10.glEnable(3553);
        gl10.glLoadIdentity();
        gl10.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl10.glTranslatef(0.0f, 0.0f, -60.0f);
        gl10.glPushMatrix();
        int[] iArr = this.mCharacterTexData;
        int i = iArr != null ? iArr[0] : 0;
        if (i != 0) {
            gl10.glBindTexture(3553, i);
            this.mCharacter.drawBody(gl10);
            Bitmap bitmap2 = this.cape;
            if (bitmap2 != null && bitmap2.getWidth() == 64 && this.cape.getHeight() == 32) {
                int[] iArr2 = this.mCharacterTexData;
                if (iArr2.length > 1 && iArr2[1] != 0) {
                    gl10.glBindTexture(3553, iArr2[1]);
                    this.mCharacter.drawCape(gl10);
                }
            }
        }
        gl10.glPopMatrix();
        gl10.glLoadIdentity();
        if (!this.superRun || i == 0) {
            return;
        }
        GLU.gluLookAt(gl10, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        gl10.glRotatef(((int) (SystemClock.uptimeMillis() % 4000)) * 0.09f, 0.0f, 0.0f, 1.0f);
        gl10.glBindTexture(3553, i);
        this.mCharacter.drawBody(gl10);
    }

    public boolean hasValidTexture() {
        int[] iArr = this.mCharacterTexData;
        return (iArr == null || iArr[0] == 0) ? false : true;
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onSurfaceChanged(GL10 gl10, int i, int i2) {
        int min = Math.min(i, i2);
        gl10.glViewport((i - min) / 2, (i2 - min) / 2, min, min);
        gl10.glMatrixMode(5889);
        gl10.glLoadIdentity();
        float f = min / 2.0f;
        float tan = f / ((float) Math.tan(Utils.d2r(22.5f)));
        GLU.gluPerspective(gl10, 45.0f, 1.0f, 0.5f, Math.max(1500.0f, tan));
        gl10.glMatrixMode(5888);
        gl10.glLoadIdentity();
        GLU.gluLookAt(gl10, f, f, tan, f, f, 0.0f, 0.0f, 1.0f, 0.0f);
        gl10.glDisable(2896);
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onSurfaceCreated(GL10 gl10, EGLConfig eGLConfig) {
        try {
            this.mCharacterTexData = TextureHelper.loadTexture(this.mContext, GameCharacter.selected_resource);
        } catch (Throwable th) {
            th.printStackTrace();
            this.mCharacterTexData = new int[]{0, 0};
        }
        gl10.glEnable(3042);
        gl10.glCullFace(1028);
        gl10.glShadeModel(7425);
        gl10.glEnable(6408);
        gl10.glEnable(2929);
        gl10.glDepthFunc(515);
        gl10.glHint(3152, 4354);
        gl10.glClearDepthf(1.0f);
        gl10.glDisable(2896);
        gl10.glTexEnvf(5952, 8704, 7681.0f);
    }

    public void setSuperRun(boolean z) {
        this.superRun = z;
    }

    public void updateTexture(Bitmap bitmap, Bitmap bitmap2) {
        if (bitmap == null) {
            return;
        }
        this.skin = bitmap;
        if (bitmap2 == null || bitmap2.getWidth() != 64 || bitmap2.getHeight() != 32) {
            bitmap2 = null;
        }
        this.cape = bitmap2;
        this.updateBitmapSkin = true;
    }
}
