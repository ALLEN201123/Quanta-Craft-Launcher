package com.qcl.launcher.skin;

import com.qcl.launcher.skin.body.BodyPart;
import com.qcl.launcher.skin.body.cube.Body;
import com.qcl.launcher.skin.body.cube.BodyOverlay;
import com.qcl.launcher.skin.body.cube.Hat;
import com.qcl.launcher.skin.body.cube.Head;
import com.qcl.launcher.skin.body.cube.LLeg;
import com.qcl.launcher.skin.body.cube.LLegOverlay;
import com.qcl.launcher.skin.body.cube.RLeg;
import com.qcl.launcher.skin.body.cube.RLegOverlay;
import com.qcl.launcher.skin.body.cube.alex.AlexLArm;
import com.qcl.launcher.skin.body.cube.alex.AlexLArmOverlay;
import com.qcl.launcher.skin.body.cube.alex.AlexRArm;
import com.qcl.launcher.skin.body.cube.alex.AlexRArmOverlay;
import com.qcl.launcher.skin.body.cube.steve.LArm;
import com.qcl.launcher.skin.body.cube.steve.LArmOverlay;
import com.qcl.launcher.skin.body.cube.steve.RArm;
import com.qcl.launcher.skin.body.cube.steve.RArmOverlay;
import com.qcl.launcher.skin.cape.CapePart;
import com.qcl.launcher.skin.cape.cube.Cape;
import javax.microedition.khronos.opengles.GL10;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class GameCharacter {
    public static int selected_resource = R.dimen.abc_alert_dialog_button_dimen;
    private boolean isAlexSkin;
    private boolean isRunning;
    private AlexLArm mAlexLArm;
    private AlexLArmOverlay mAlexLArmOverlay;
    private AlexRArm mAlexRArm;
    private AlexRArmOverlay mAlexRArmOverlay;
    private Body mBody;
    private BodyOverlay mBodyOverlay;
    private boolean mBodyOverlayVisible;
    private boolean mBodyVisible;
    private Cape mCape;
    private boolean mCapeVisible;
    private boolean mCheckAlexOrSteve;
    private Hat mHat;
    private boolean mHatVisible;
    private Head mHead;
    private boolean mHeadVisible;
    private LArm mLArm;
    private LArmOverlay mLArmOverlay;
    private boolean mLArmOverlayVisible;
    private boolean mLArmVisible;
    private LLeg mLLeg;
    private LLegOverlay mLLegOverlay;
    private boolean mLLegOverlayVisible;
    private boolean mLLegVisible;
    private RArm mRArm;
    private RArmOverlay mRArmOverlay;
    private boolean mRArmOverlayVisible;
    private boolean mRArmVisible;
    private RLeg mRLeg;
    private RLegOverlay mRLegOverlay;
    private boolean mRLegOverlayVisible;
    private boolean mRLegVisible;
    private float[] mRotate;
    private float[] rotate_step;
    public float scale;

    public GameCharacter() {
        this.scale = 1.0f;
        this.isAlexSkin = false;
        this.isRunning = false;
        this.mBodyOverlayVisible = true;
        this.mBodyVisible = true;
        this.mHatVisible = true;
        this.mHeadVisible = true;
        this.mLArmOverlayVisible = true;
        this.mLArmVisible = true;
        this.mLLegOverlayVisible = true;
        this.mLLegVisible = true;
        this.mRArmOverlayVisible = true;
        this.mRArmVisible = true;
        this.mRLegOverlayVisible = true;
        this.mRLegVisible = true;
        this.mCapeVisible = true;
        this.mRotate = new float[]{0.0f, 0.0f, 0.0f};
        this.rotate_step = new float[]{5.0f, 5.0f, 5.0f};
        this.mHead = new Head(this.scale);
        this.mHat = new Hat(this.scale);
        this.mBody = new Body(this.scale);
        this.mBodyOverlay = new BodyOverlay(this.scale);
        this.mLArm = new LArm(this.scale);
        this.mRArm = new RArm(this.scale);
        this.mLArmOverlay = new LArmOverlay(this.scale);
        this.mRArmOverlay = new RArmOverlay(this.scale);
        this.mLLeg = new LLeg(this.scale);
        this.mRLeg = new RLeg(this.scale);
        this.mLLegOverlay = new LLegOverlay(this.scale);
        this.mRLegOverlay = new RLegOverlay(this.scale);
        this.mCape = new Cape(this.scale);
    }

    public GameCharacter(int i) {
        this.scale = 1.0f;
        this.isAlexSkin = false;
        this.isRunning = false;
        this.mBodyOverlayVisible = true;
        this.mBodyVisible = true;
        this.mHatVisible = true;
        this.mHeadVisible = true;
        this.mLArmOverlayVisible = true;
        this.mLArmVisible = true;
        this.mLLegOverlayVisible = true;
        this.mLLegVisible = true;
        this.mRArmOverlayVisible = true;
        this.mRArmVisible = true;
        this.mRLegOverlayVisible = true;
        this.mRLegVisible = true;
        this.mCapeVisible = true;
        this.mRotate = new float[]{0.0f, 0.0f, 0.0f};
        this.rotate_step = new float[]{5.0f, 5.0f, 5.0f};
        selected_resource = i;
        this.mHead = new Head(this.scale);
        this.mHat = new Hat(this.scale);
        this.mBody = new Body(this.scale);
        this.mBodyOverlay = new BodyOverlay(this.scale);
        this.mLArm = new LArm(this.scale);
        this.mRArm = new RArm(this.scale);
        this.mLArmOverlay = new LArmOverlay(this.scale);
        this.mRArmOverlay = new RArmOverlay(this.scale);
        this.mLLeg = new LLeg(this.scale);
        this.mRLeg = new RLeg(this.scale);
        this.mLLegOverlay = new LLegOverlay(this.scale);
        this.mRLegOverlay = new RLegOverlay(this.scale);
        this.mCape = new Cape(this.scale);
    }

    public GameCharacter(boolean z) {
        this.scale = 1.0f;
        this.isAlexSkin = false;
        this.isRunning = false;
        this.mBodyOverlayVisible = true;
        this.mBodyVisible = true;
        this.mHatVisible = true;
        this.mHeadVisible = true;
        this.mLArmOverlayVisible = true;
        this.mLArmVisible = true;
        this.mLLegOverlayVisible = true;
        this.mLLegVisible = true;
        this.mRArmOverlayVisible = true;
        this.mRArmVisible = true;
        this.mRLegOverlayVisible = true;
        this.mRLegVisible = true;
        this.mCapeVisible = true;
        this.mRotate = new float[]{0.0f, 0.0f, 0.0f};
        this.rotate_step = new float[]{5.0f, 5.0f, 5.0f};
        this.mCheckAlexOrSteve = z;
        if (!z) {
            this.mHead = new Head(this.scale);
            this.mHat = new Hat(this.scale);
            this.mBody = new Body(this.scale);
            this.mBodyOverlay = new BodyOverlay(this.scale);
            this.mLArm = new LArm(this.scale);
            this.mRArm = new RArm(this.scale);
            this.mLArmOverlay = new LArmOverlay(this.scale);
            this.mRArmOverlay = new RArmOverlay(this.scale);
            this.mLLeg = new LLeg(this.scale);
            this.mRLeg = new RLeg(this.scale);
            this.mLLegOverlay = new LLegOverlay(this.scale);
            this.mRLegOverlay = new RLegOverlay(this.scale);
            this.mCape = new Cape(this.scale);
            return;
        }
        this.mHead = new Head(this.scale);
        this.mHat = new Hat(this.scale);
        this.mBody = new Body(this.scale);
        this.mBodyOverlay = new BodyOverlay(this.scale);
        this.mAlexLArm = new AlexLArm(this.scale);
        this.mAlexRArm = new AlexRArm(this.scale);
        this.mAlexLArmOverlay = new AlexLArmOverlay(this.scale);
        this.mAlexRArmOverlay = new AlexRArmOverlay(this.scale);
        this.mLLeg = new LLeg(this.scale);
        this.mRLeg = new RLeg(this.scale);
        this.mLLegOverlay = new LLegOverlay(this.scale);
        this.mRLegOverlay = new RLegOverlay(this.scale);
        this.mCape = new Cape(this.scale);
    }

    public GameCharacter(boolean z, int i) {
        this.scale = 1.0f;
        this.isAlexSkin = false;
        this.isRunning = false;
        this.mBodyOverlayVisible = true;
        this.mBodyVisible = true;
        this.mHatVisible = true;
        this.mHeadVisible = true;
        this.mLArmOverlayVisible = true;
        this.mLArmVisible = true;
        this.mLLegOverlayVisible = true;
        this.mLLegVisible = true;
        this.mRArmOverlayVisible = true;
        this.mRArmVisible = true;
        this.mRLegOverlayVisible = true;
        this.mRLegVisible = true;
        this.mCapeVisible = true;
        this.mRotate = new float[]{0.0f, 0.0f, 0.0f};
        this.rotate_step = new float[]{5.0f, 5.0f, 5.0f};
        this.mCheckAlexOrSteve = z;
        selected_resource = i;
        if (!z) {
            this.mHead = new Head(this.scale);
            this.mHat = new Hat(this.scale);
            this.mBody = new Body(this.scale);
            this.mBodyOverlay = new BodyOverlay(this.scale);
            this.mLArm = new LArm(this.scale);
            this.mRArm = new RArm(this.scale);
            this.mLArmOverlay = new LArmOverlay(this.scale);
            this.mRArmOverlay = new RArmOverlay(this.scale);
            this.mLLeg = new LLeg(this.scale);
            this.mRLeg = new RLeg(this.scale);
            this.mLLegOverlay = new LLegOverlay(this.scale);
            this.mRLegOverlay = new RLegOverlay(this.scale);
            this.mCape = new Cape(this.scale);
            return;
        }
        this.mHead = new Head(this.scale);
        this.mHat = new Hat(this.scale);
        this.mBody = new Body(this.scale);
        this.mBodyOverlay = new BodyOverlay(this.scale);
        this.mAlexLArm = new AlexLArm(this.scale);
        this.mAlexRArm = new AlexRArm(this.scale);
        this.mAlexLArmOverlay = new AlexLArmOverlay(this.scale);
        this.mAlexRArmOverlay = new AlexRArmOverlay(this.scale);
        this.mLLeg = new LLeg(this.scale);
        this.mRLeg = new RLeg(this.scale);
        this.mLLegOverlay = new LLegOverlay(this.scale);
        this.mRLegOverlay = new RLegOverlay(this.scale);
        this.mCape = new Cape(this.scale);
    }

    public void ResetRunForBody() {
        if (!this.mCheckAlexOrSteve) {
            this.mHead = new Head(this.scale);
            this.mHat = new Hat(this.scale);
            this.mBody = new Body(this.scale);
            this.mBodyOverlay = new BodyOverlay(this.scale);
            this.mLArm = new LArm(this.scale);
            this.mRArm = new RArm(this.scale);
            this.mLArmOverlay = new LArmOverlay(this.scale);
            this.mRArmOverlay = new RArmOverlay(this.scale);
            this.mLLeg = new LLeg(this.scale);
            this.mRLeg = new RLeg(this.scale);
            this.mLLegOverlay = new LLegOverlay(this.scale);
            this.mRLegOverlay = new RLegOverlay(this.scale);
            return;
        }
        this.mHead = new Head(this.scale);
        this.mHat = new Hat(this.scale);
        this.mBody = new Body(this.scale);
        this.mBodyOverlay = new BodyOverlay(this.scale);
        this.mAlexLArm = new AlexLArm(this.scale);
        this.mAlexRArm = new AlexRArm(this.scale);
        this.mAlexLArmOverlay = new AlexLArmOverlay(this.scale);
        this.mAlexRArmOverlay = new AlexRArmOverlay(this.scale);
        this.mLLeg = new LLeg(this.scale);
        this.mRLeg = new RLeg(this.scale);
        this.mLLegOverlay = new LLegOverlay(this.scale);
        this.mRLegOverlay = new RLegOverlay(this.scale);
    }

    public void setScale(float f) {
        this.scale = f;
        if (!this.mCheckAlexOrSteve) {
            this.mHead = new Head(f);
            this.mHat = new Hat(f);
            this.mBody = new Body(f);
            this.mBodyOverlay = new BodyOverlay(f);
            this.mLArm = new LArm(f);
            this.mRArm = new RArm(f);
            this.mLArmOverlay = new LArmOverlay(f);
            this.mRArmOverlay = new RArmOverlay(f);
            this.mLLeg = new LLeg(f);
            this.mRLeg = new RLeg(f);
            this.mLLegOverlay = new LLegOverlay(f);
            this.mRLegOverlay = new RLegOverlay(f);
            this.mCape = new Cape(f);
            return;
        }
        this.mHead = new Head(f);
        this.mHat = new Hat(f);
        this.mBody = new Body(f);
        this.mBodyOverlay = new BodyOverlay(f);
        this.mAlexLArm = new AlexLArm(f);
        this.mAlexRArm = new AlexRArm(f);
        this.mAlexLArmOverlay = new AlexLArmOverlay(f);
        this.mAlexRArmOverlay = new AlexRArmOverlay(f);
        this.mLLeg = new LLeg(f);
        this.mRLeg = new RLeg(f);
        this.mLLegOverlay = new LLegOverlay(f);
        this.mRLegOverlay = new RLegOverlay(f);
        this.mCape = new Cape(f);
    }

    public void SetRotate(float f, float f2, float f3) {
        float[] fArr = this.mRotate;
        fArr[0] = f;
        fArr[1] = f2;
        fArr[2] = f3;
    }

    public void SetRotateStep(float f, float f2) {
        if (Math.abs(f) >= 1.0f) {
            float[] fArr = this.mRotate;
            fArr[1] = fArr[1] + (this.rotate_step[1] * Math.signum(f) * 2.0f);
        }
        if (Math.abs(f2) >= 1.0f) {
            float[] fArr2 = this.mRotate;
            fArr2[0] = fArr2[0] + (this.rotate_step[0] * Math.signum(f2) * 2.0f);
        }
    }

    public void rotateBy(float f, float f2) {
        float[] fArr = this.mRotate;
        fArr[1] = fArr[1] + f;
        fArr[0] = fArr[0] + f2;
        fArr[0] = ((fArr[0] % 360.0f) + 360.0f) % 360.0f;
        fArr[1] = ((fArr[1] % 360.0f) + 360.0f) % 360.0f;
    }

    public void SetRunning(boolean z) {
        this.isRunning = z;
    }

    public void setWalkSwing(float f) {
        LLeg lLeg = this.mLLeg;
        if (lLeg != null) {
            lLeg.setSwingAngle(-f);
        }
        RLeg rLeg = this.mRLeg;
        if (rLeg != null) {
            rLeg.setSwingAngle(f);
        }
        LLegOverlay lLegOverlay = this.mLLegOverlay;
        if (lLegOverlay != null) {
            lLegOverlay.setSwingAngle(-f);
        }
        RLegOverlay rLegOverlay = this.mRLegOverlay;
        if (rLegOverlay != null) {
            rLegOverlay.setSwingAngle(f);
        }
        RArm rArm = this.mRArm;
        if (rArm != null) {
            if (rArm != null) {
                rArm.setSwingAngle(-f);
            }
            LArm lArm = this.mLArm;
            if (lArm != null) {
                lArm.setSwingAngle(f);
            }
        } else {
            AlexRArm alexRArm = this.mAlexRArm;
            if (alexRArm != null) {
                alexRArm.setSwingAngle(-f);
            }
            AlexLArm alexLArm = this.mAlexLArm;
            if (alexLArm != null) {
                alexLArm.setSwingAngle(f);
            }
        }
        RArmOverlay rArmOverlay = this.mRArmOverlay;
        if (rArmOverlay != null) {
            rArmOverlay.setSwingAngle(-f);
            LArmOverlay lArmOverlay = this.mLArmOverlay;
            if (lArmOverlay != null) {
                lArmOverlay.setSwingAngle(f);
                return;
            }
            return;
        }
        AlexRArmOverlay alexRArmOverlay = this.mAlexRArmOverlay;
        if (alexRArmOverlay != null) {
            alexRArmOverlay.setSwingAngle(-f);
        }
        AlexLArmOverlay alexLArmOverlay = this.mAlexLArmOverlay;
        if (alexLArmOverlay != null) {
            alexLArmOverlay.setSwingAngle(f);
        }
    }

    public void drawBody(GL10 gl10) {
        gl10.glMatrixMode(5888);
        gl10.glRotatef(this.mRotate[0], 1.0f, 0.0f, 0.0f);
        gl10.glRotatef(this.mRotate[1], 0.0f, 1.0f, 0.0f);
        gl10.glRotatef(this.mRotate[2], 0.0f, 0.0f, 1.0f);
        if (this.mBodyVisible) {
            this.mBody.draw(gl10, this.isRunning);
        }
        if (this.mHeadVisible) {
            this.mHead.draw(gl10, this.isRunning);
        }
        if (this.mLLegVisible) {
            this.mLLeg.draw(gl10, this.isRunning);
        }
        if (this.mRLegVisible) {
            this.mRLeg.draw(gl10, this.isRunning);
        }
        if (this.mRArm != null) {
            if (this.mLArmVisible) {
                this.mLArm.draw(gl10, this.isRunning);
            }
            if (this.mRArmVisible) {
                this.mRArm.draw(gl10, this.isRunning);
            }
            if (this.mHatVisible) {
                this.mHat.draw(gl10, this.isRunning);
            }
            if (this.mLArmOverlayVisible) {
                this.mLArmOverlay.draw(gl10, this.isRunning);
            }
            if (this.mRArmOverlayVisible) {
                this.mRArmOverlay.draw(gl10, this.isRunning);
            }
        } else {
            if (this.mLArmVisible) {
                this.mAlexLArm.draw(gl10, this.isRunning);
            }
            if (this.mRArmVisible) {
                this.mAlexRArm.draw(gl10, this.isRunning);
            }
            if (this.mHatVisible) {
                this.mHat.draw(gl10, this.isRunning);
            }
            if (this.mLArmOverlayVisible) {
                this.mAlexLArmOverlay.draw(gl10, this.isRunning);
            }
            if (this.mRArmOverlayVisible) {
                this.mAlexRArmOverlay.draw(gl10, this.isRunning);
            }
        }
        if (this.mLLegOverlayVisible) {
            this.mLLegOverlay.draw(gl10, this.isRunning);
        }
        if (this.mRLegOverlayVisible) {
            this.mRLegOverlay.draw(gl10, this.isRunning);
        }
        if (this.mBodyOverlayVisible) {
            this.mBodyOverlay.draw(gl10, this.isRunning);
        }
    }

    public void drawCape(GL10 gl10) {
        if (this.mCapeVisible) {
            this.mCape.draw(gl10, this.isRunning);
        }
    }

    public Boolean getRunning() {
        return Boolean.valueOf(this.isRunning);
    }

    public float getXRotation() {
        return this.mRotate[0];
    }

    public float getYRotation() {
        return this.mRotate[1];
    }

    public float getZRotation() {
        return this.mRotate[2];
    }

    public void hideBodyPart(BodyPart bodyPart, boolean z) {
        if (bodyPart == BodyPart.BODY) {
            this.mBodyVisible = z;
        }
        if (bodyPart == BodyPart.HAT) {
            this.mHatVisible = z;
        }
        if (bodyPart == BodyPart.HEAD) {
            this.mHeadVisible = z;
        }
        if (bodyPart == BodyPart.JACKET) {
            this.mBodyOverlayVisible = z;
        }
        if (bodyPart == BodyPart.LEFT_ARM) {
            this.mLArmVisible = z;
        }
        if (bodyPart == BodyPart.RIGHT_ARM) {
            this.mRArmVisible = z;
        }
        if (bodyPart == BodyPart.LEFT_LEG) {
            this.mLLegVisible = z;
        }
        if (bodyPart == BodyPart.RIGHT_LEG) {
            this.mRLegVisible = z;
        }
        if (bodyPart == BodyPart.LEFT_SLEEVE) {
            this.mLArmOverlayVisible = z;
        }
        if (bodyPart == BodyPart.RIGHT_SLEEVE) {
            this.mRArmOverlayVisible = z;
        }
        if (bodyPart == BodyPart.LEFT_LEG_OVERLAY) {
            this.mLLegOverlayVisible = z;
        }
        if (bodyPart == BodyPart.RIGHT_LEG_OVERLAY) {
            this.mRLegOverlayVisible = z;
        }
    }

    public void hideCapePart(CapePart capePart, boolean z) {
        if (capePart == CapePart.CAPE) {
            this.mCapeVisible = z;
        }
    }

    public void setXRotation(int i) {
        this.mRotate[0] = i;
    }

    public void setYRotation(int i) {
        this.mRotate[1] = i;
    }

    public void setZRotation(int i) {
        this.mRotate[2] = i;
    }

    public void showAllBodyParts() {
        this.mHeadVisible = true;
        this.mHatVisible = true;
        this.mBodyVisible = true;
        this.mBodyOverlayVisible = true;
        this.mLArmVisible = true;
        this.mRArmVisible = true;
        this.mLArmOverlayVisible = true;
        this.mRArmOverlayVisible = true;
        this.mLLegVisible = true;
        this.mRLegVisible = true;
        this.mLLegOverlayVisible = true;
        this.mRLegOverlayVisible = true;
        this.mCapeVisible = true;
    }
}
