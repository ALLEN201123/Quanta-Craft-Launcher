package com.tungsten.hmclpe.skin;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.SystemClock;
import android.util.Log;

import com.tungsten.hmclpe.skin.utils.TextureHelper;
import com.tungsten.hmclpe.skin.utils.Utils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MinecraftSkinRenderer implements GLSurfaceView.Renderer {
    public static float[] light0Position;
    public boolean changeSkinImage;
    private int mBackTexData;
    public GameCharacter mCharacter;
    // Never null: onDrawFrame already indexes [0] and [1], and a null array would take the GL
    // thread down before anything is ever painted.
    private int[] mCharacterTexData = { 0, 0 };
    private Context mContext;
    public String path;
    public float[] plane_texcords;
    protected float[] plane_vertices;
    public Bitmap skin;
    public Bitmap cape;
    boolean superRun;
    boolean updateBitmapSkin;

    static {
        MinecraftSkinRenderer.light0Position = new float[]{0.0f, 0.0f, 5100.0f, 0.0f};
    }

    public MinecraftSkinRenderer(final Context mContext) {
        this.changeSkinImage = false;
        this.plane_texcords = new float[]{0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f};
        this.plane_vertices = new float[]{-200.0f, -100.0f, -100.0f, -200.0f, 100.0f, -100.0f, 200.0f, 100.0f, -100.0f, 200.0f, -100.0f, -100.0f};
        this.updateBitmapSkin = false;
        this.superRun = false;
        this.mContext = mContext;
        this.mCharacter = new GameCharacter();
    }

    public MinecraftSkinRenderer(final Context mContext, final int n) {
        this.changeSkinImage = false;
        this.plane_texcords = new float[]{0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f};
        this.plane_vertices = new float[]{-200.0f, -100.0f, -100.0f, -200.0f, 100.0f, -100.0f, 200.0f, 100.0f, -100.0f, 200.0f, -100.0f, -100.0f};
        this.updateBitmapSkin = false;
        this.superRun = false;
        this.mContext = mContext;
        this.mCharacter = new GameCharacter(n);
    }

    public MinecraftSkinRenderer(final Context mContext, final int n, final boolean b) {
        this.changeSkinImage = false;
        this.plane_texcords = new float[]{0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f};
        this.plane_vertices = new float[]{-200.0f, -100.0f, -100.0f, -200.0f, 100.0f, -100.0f, 200.0f, 100.0f, -100.0f, 200.0f, -100.0f, -100.0f};
        this.updateBitmapSkin = false;
        this.superRun = false;
        this.mContext = mContext;
        this.mCharacter = new GameCharacter(b, n);
    }

    public MinecraftSkinRenderer(final Context mContext, final boolean b) {
        this.changeSkinImage = false;
        this.plane_texcords = new float[]{0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f};
        this.plane_vertices = new float[]{-200.0f, -100.0f, -100.0f, -200.0f, 100.0f, -100.0f, 200.0f, 100.0f, -100.0f, 200.0f, -100.0f, -100.0f};
        this.updateBitmapSkin = false;
        this.superRun = false;
        this.mContext = mContext;
        this.mCharacter = new GameCharacter(b);
    }

    public void onDrawFrame(final GL10 gl10) {
        if (this.changeSkinImage) {
            this.changeSkinImage = false;
        }
        // 持续原地走步：正弦驱动，手臂与对侧腿同步摆
        if (this.mCharacter != null) {
            this.mCharacter.SetRunning(true);
            this.mCharacter.setWalkSwing((float) (Math.sin(SystemClock.uptimeMillis() / 260.0) * 22.0));
        }
        if (this.updateBitmapSkin) {
            // Only ever swap a good texture for another good one. TextureHelper returns {0, 0}
            // for a null bitmap, and binding texture 0 while GL_TEXTURE_2D is enabled makes every
            // sample come back (0,0,0,1) -- that is exactly what turned the model into a black
            // silhouette.
            if (this.skin != null) {
                this.mCharacterTexData = TextureHelper.loadGLTextureFromBitmap(this.skin, this.cape, gl10);
            }
            this.updateBitmapSkin = false;
        }
        // Keep the surface fully transparent so only the character is drawn; the launcher
        // background stays visible behind it (same behaviour as the skin preview dialog).
        gl10.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl10.glClear(16640);
        gl10.glEnable(3553);
        gl10.glLoadIdentity();
        // Texture colour straight from the skin: no lighting, no material, no tint. Several GLES1
        // translations (MuMu included) mishandle the offset-array form of glLightfv, which zeroes
        // the whole lit result and turns the model into a black silhouette.
        gl10.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl10.glTranslatef(0.0f, 0.0f, -60.0f);
        gl10.glPushMatrix();
        final int skinTexture = (this.mCharacterTexData == null) ? 0 : this.mCharacterTexData[0];
        if (skinTexture != 0) {
            gl10.glBindTexture(3553, skinTexture);
            this.mCharacter.drawBody(gl10);
            if (this.cape != null && cape.getWidth() == 64 && cape.getHeight() == 32
                    && this.mCharacterTexData.length > 1 && this.mCharacterTexData[1] != 0) {
                gl10.glBindTexture(3553, this.mCharacterTexData[1]);
                this.mCharacter.drawCape(gl10);
            }
        }
        gl10.glPopMatrix();
        gl10.glLoadIdentity();
        if (this.superRun && skinTexture != 0) {
            GLU.gluLookAt(gl10, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
            gl10.glRotatef(0.09f * (int) (SystemClock.uptimeMillis() % 4000L), 0.0f, 0.0f, 1.0f);
            gl10.glBindTexture(3553, skinTexture);
            this.mCharacter.drawBody(gl10);
        }
    }

    /** True once a usable skin texture has been uploaded on the GL thread. */
    public boolean hasValidTexture() {
        return this.mCharacterTexData != null && this.mCharacterTexData[0] != 0;
    }

    public void onSurfaceChanged(final GL10 gl10, final int n, final int n2) {
        // 人物视口固定取短边的正方形并居中：宽高比恒为 1，
        // 手机/平板/横屏任何比例下人物比例都一致，不会再出现左右拉伸
        final int size = Math.min(n, n2);
        final int vx = (n - size) / 2;
        final int vy = (n2 - size) / 2;
        gl10.glViewport(vx, vy, size, size);
        gl10.glMatrixMode(5889);
        gl10.glLoadIdentity();
        final float n3 = size / 2.0f / (float) Math.tan(Utils.d2r(22.5f));
        GLU.gluPerspective(gl10, 45.0f, 1.0f, 0.5f, Math.max(1500.0f, n3));
        gl10.glMatrixMode(5888);
        gl10.glLoadIdentity();
        GLU.gluLookAt(gl10, size / 2.0f, size / 2.0f, n3, size / 2.0f, size / 2.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        // 灯光整体关掉：皮肤要的是原色，不要着色。若干 GLES1 翻译层（MuMu 实测）对
        // glLightfv 的数组形式支持有毛病，会把整个受光结果乘成 0 → 人物纯黑。
        gl10.glDisable(2896);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        try {
            this.mCharacterTexData = TextureHelper.loadTexture(this.mContext, GameCharacter.selected_resource);
        } catch (Throwable t) {
            // A dead GL thread never paints, which shows up as an opaque black band in the
            // launcher. Keep the thread alive and let the next updateTexture() fill it in.
            t.printStackTrace();
            this.mCharacterTexData = new int[]{ 0, 0 };
        }
        gl10.glEnable(3042);
        gl10.glCullFace(1028);
        gl10.glShadeModel(7425);
        gl10.glEnable(6408);
        gl10.glEnable(2929);
        gl10.glDepthFunc(515);
        gl10.glHint(3152, 4354);
        gl10.glClearDepthf(1.0f);
        // 关掉光照，并把纹理环境改成 GL_REPLACE：最终颜色 = 皮肤原色，
        // 不再经过任何光照/材质乘法 —— 这是"人物全黑"的最后一道可能来源。
        gl10.glDisable(2896);
        gl10.glTexEnvf(5952, 8704, 7681f);
    }

    public void setSuperRun(final boolean superRun) {
        this.superRun = superRun;
    }

    public void updateTexture(final Bitmap skin, final Bitmap cape) {
        if (skin == null) {
            // Never let a null skin wipe out the texture that is already on screen.
            return;
        }
        this.skin = skin;
        // The renderer only draws a cape it can actually map, so drop anything else here.
        this.cape = (cape != null && cape.getWidth() == 64 && cape.getHeight() == 32) ? cape : null;
        this.updateBitmapSkin = true;
    }
}
