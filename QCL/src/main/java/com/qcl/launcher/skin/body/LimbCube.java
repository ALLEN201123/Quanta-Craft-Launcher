package com.qcl.launcher.skin.body;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import javax.microedition.khronos.opengles.GL10;

/* loaded from: classes2.dex */
public class LimbCube {
    protected int[] face_indecies;
    protected float[] face_vertices;
    protected float fixedDir;
    protected boolean isFixOneAxis;
    protected float mMainAngle;
    protected FloatBuffer mNormalVertexBuffer;
    protected float[] mOffset;
    protected float[] mScale;
    protected float mSubAngle;
    protected float mSwingAngle;
    protected ArrayList<FloatBuffer> mTextureBuffers;
    protected FloatBuffer mVertexBuffer;
    protected float[] main_angle_axis;
    protected float main_max_angle;
    protected float main_min_angle;
    protected float main_step_value;
    protected float[] normal_vertices;
    protected float[] sub_angle_axis;
    protected float sub_max_angle;
    protected float sub_min_angle;
    protected float sub_step_value;
    private float[] vertices;

    public void setSwingAngle(float f) {
        this.mSwingAngle = f;
    }

    public LimbCube(float f, float f2, float f3, float f4, float f5, float f6) {
        this.mScale = new float[]{0.0f, 0.0f, 0.0f};
        this.mOffset = new float[]{0.0f, 0.0f, 0.0f};
        this.mMainAngle = 0.0f;
        this.main_step_value = -3.0f;
        this.main_max_angle = 3.0f;
        this.main_min_angle = -3.0f;
        this.main_angle_axis = new float[]{0.0f, 0.0f, 0.0f};
        this.mSubAngle = 0.0f;
        this.sub_step_value = -0.15f;
        this.sub_max_angle = 3.0f;
        this.sub_min_angle = -3.0f;
        this.sub_angle_axis = new float[]{0.0f, 0.0f, 0.0f};
        this.isFixOneAxis = true;
        this.fixedDir = 1.0f;
        this.vertices = new float[]{-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, -1.0f, -1.0f, 0.0f, -1.0f};
        int[] iArr = {0, 8, 9, 1, 8, 3, 2, 9, 3, 7, 6, 2, 1, 5, 4, 0, 1, 9, 10, 5, 9, 2, 6, 10, 4, 11, 8, 0, 11, 7, 3, 8, 5, 10, 11, 4, 10, 6, 7, 11};
        this.face_indecies = iArr;
        this.face_vertices = new float[iArr.length * 3];
        this.normal_vertices = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f};
        this.mTextureBuffers = new ArrayList<>();
        float[] fArr = this.mScale;
        fArr[0] = f;
        fArr[1] = f2;
        fArr[2] = f3;
        float[] fArr2 = this.mOffset;
        fArr2[0] = f4;
        fArr2[1] = f5;
        fArr2[2] = f6;
        ByteBuffer allocateDirect = ByteBuffer.allocateDirect(this.face_vertices.length * 4);
        allocateDirect.order(ByteOrder.nativeOrder());
        FloatBuffer asFloatBuffer = allocateDirect.asFloatBuffer();
        this.mVertexBuffer = asFloatBuffer;
        asFloatBuffer.put(this.face_vertices);
        this.mVertexBuffer.position(0);
        ByteBuffer allocateDirect2 = ByteBuffer.allocateDirect(this.normal_vertices.length * 4);
        allocateDirect2.order(ByteOrder.nativeOrder());
        FloatBuffer asFloatBuffer2 = allocateDirect2.asFloatBuffer();
        this.mNormalVertexBuffer = asFloatBuffer2;
        asFloatBuffer2.put(this.normal_vertices);
        this.mNormalVertexBuffer.position(0);
    }

    public LimbCube(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, float f13, float f14, float f15, boolean z, float f16) {
        this(f, f2, f3, f4, f5, f6);
        this.main_step_value = f7;
        float[] fArr = this.main_angle_axis;
        fArr[0] = f8;
        fArr[1] = f9;
        fArr[2] = f10;
        this.main_max_angle = f11;
        this.main_min_angle = f12;
        this.sub_step_value = f13;
        this.sub_max_angle = f14;
        this.sub_min_angle = f15;
        this.isFixOneAxis = z;
        this.fixedDir = f16;
    }

    public FloatBuffer AddTextures(float[] fArr) {
        ByteBuffer allocateDirect = ByteBuffer.allocateDirect(fArr.length * 4);
        allocateDirect.order(ByteOrder.nativeOrder());
        FloatBuffer asFloatBuffer = allocateDirect.asFloatBuffer();
        asFloatBuffer.put(fArr);
        asFloatBuffer.position(0);
        this.mTextureBuffers.add(asFloatBuffer);
        return asFloatBuffer;
    }

    public void ClearAllTextures() {
        this.mTextureBuffers.clear();
    }

    public void draw(GL10 gl10, boolean z) {
        gl10.glEnable(3042);
        gl10.glBlendFunc(1, 771);
        gl10.glEnableClientState(32884);
        gl10.glEnableClientState(32885);
        gl10.glEnableClientState(32888);
        gl10.glNormalPointer(5126, 0, this.mNormalVertexBuffer);
        int i = 0;
        while (true) {
            int[] iArr = this.face_indecies;
            if (i >= iArr.length) {
                break;
            }
            int i2 = iArr[i];
            float[] fArr = this.vertices;
            int i3 = i2 * 3;
            float f = fArr[i3 + 1];
            float[] fArr2 = this.mScale;
            float f2 = (f * fArr2[1]) / 2.0f;
            float f3 = (fArr[i3 + 2] * fArr2[2]) / 2.0f;
            int i4 = i * 3;
            this.mVertexBuffer.put(i4, (fArr[i3] * fArr2[0]) / 2.0f);
            this.mVertexBuffer.put(i4 + 1, f2);
            this.mVertexBuffer.put(i4 + 2, f3);
            i++;
        }
        gl10.glVertexPointer(3, 5126, 0, this.mVertexBuffer);
        gl10.glPushMatrix();
        float[] fArr3 = this.mOffset;
        gl10.glTranslatef(fArr3[0], fArr3[1], fArr3[2]);
        if (z && this.mSwingAngle != 0.0f) {
            float f4 = (this.mScale[1] / 4.0f) * 3.0f;
            gl10.glTranslatef(0.0f, f4, 0.0f);
            gl10.glRotatef(this.mSwingAngle, 1.0f, 0.0f, 0.0f);
            gl10.glTranslatef(0.0f, -f4, 0.0f);
        }
        for (int i5 = 0; i5 < this.mTextureBuffers.size(); i5++) {
            gl10.glTexCoordPointer(2, 5126, 0, this.mTextureBuffers.get(i5));
            for (int i6 = 0; i6 < 10; i6++) {
                gl10.glDrawArrays(6, i6 * 4, 4);
            }
        }
        gl10.glPopMatrix();
        gl10.glDisable(3042);
        gl10.glDisableClientState(32888);
        gl10.glDisableClientState(32884);
    }

    public void setZeroRun() {
        this.mMainAngle = 1.5f;
        this.main_step_value = 0.7f;
        this.mSubAngle = 0.5f;
        this.sub_step_value = -0.5f;
    }
}
