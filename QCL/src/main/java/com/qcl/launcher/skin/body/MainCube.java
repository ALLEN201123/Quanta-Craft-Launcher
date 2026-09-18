package com.qcl.launcher.skin.body;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import javax.microedition.khronos.opengles.GL10;

/* loaded from: classes2.dex */
public class MainCube {
    protected float[] angle_axis;
    protected float[] face_vertices;
    protected float mAngle;
    protected FloatBuffer mNormalVertexBuffer;
    protected float[] mOffset;
    protected float[] mScale;
    protected ArrayList<FloatBuffer> mTextureBuffers;
    protected FloatBuffer mVertexBuffer;
    protected float max_angle;
    protected float min_angle;
    protected float[] normal_vertices;
    protected float step_value;

    public MainCube(float f, float f2, float f3, float f4, float f5, float f6) {
        this.mScale = new float[]{0.0f, 0.0f, 0.0f};
        this.mOffset = new float[]{0.0f, 0.0f, 0.0f};
        this.mAngle = 0.0f;
        this.step_value = -0.15f;
        this.max_angle = 3.0f;
        this.min_angle = -3.0f;
        this.angle_axis = new float[]{0.0f, 0.0f, 0.0f};
        this.face_vertices = new float[]{-1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f};
        this.normal_vertices = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f};
        this.mTextureBuffers = new ArrayList<>();
        float[] fArr = this.mScale;
        fArr[0] = f;
        fArr[1] = f2;
        fArr[2] = f3;
        float[] fArr2 = this.mOffset;
        fArr2[0] = f4;
        fArr2[1] = f5;
        fArr2[2] = f6;
        for (int i = 0; i < 24; i++) {
            float[] fArr3 = this.face_vertices;
            int i2 = i * 3;
            float f7 = fArr3[i2];
            float[] fArr4 = this.mScale;
            fArr3[i2] = (f7 * fArr4[0]) / 2.0f;
            int i3 = i2 + 1;
            fArr3[i3] = (fArr3[i3] * fArr4[1]) / 2.0f;
            int i4 = i2 + 2;
            fArr3[i4] = (fArr3[i4] * fArr4[2]) / 2.0f;
        }
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

    public MainCube(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12) {
        this(f, f2, f3, f4, f5, f6);
        this.step_value = f7;
        this.max_angle = f11;
        this.min_angle = f12;
        float[] fArr = this.angle_axis;
        fArr[0] = f8;
        fArr[1] = f9;
        fArr[2] = f10;
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
        gl10.glVertexPointer(3, 5126, 0, this.mVertexBuffer);
        gl10.glNormalPointer(5126, 0, this.mNormalVertexBuffer);
        gl10.glPushMatrix();
        float[] fArr = this.mOffset;
        gl10.glTranslatef(fArr[0], fArr[1], fArr[2]);
        if (z && this.mAngle != 0.0f) {
            float f = this.mScale[1] / 2.0f;
            gl10.glTranslatef(0.0f, f, 0.0f);
            gl10.glRotatef(this.mAngle, 1.0f, 0.0f, 0.0f);
            gl10.glTranslatef(0.0f, -f, 0.0f);
        }
        for (int i = 0; i < this.mTextureBuffers.size(); i++) {
            gl10.glTexCoordPointer(2, 5126, 0, this.mTextureBuffers.get(i));
            for (int i2 = 0; i2 < 6; i2++) {
                gl10.glDrawArrays(6, i2 * 4, 4);
            }
        }
        gl10.glPopMatrix();
        gl10.glDisable(3042);
        gl10.glDisableClientState(32888);
        gl10.glDisableClientState(32884);
    }

    public void setSwingAngle(float f) {
        this.mAngle = f;
    }

    public void setZeroRun() {
        this.mAngle = 0.0f;
        this.step_value = -0.15f;
        this.max_angle = 3.0f;
        this.min_angle = -3.0f;
    }
}
