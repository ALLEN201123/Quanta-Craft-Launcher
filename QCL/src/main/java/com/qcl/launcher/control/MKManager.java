package com.qcl.launcher.control;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import org.lwjgl.glfw.CallbackBridge;

/* loaded from: classes2.dex */
public class MKManager implements View.OnKeyListener, View.OnCapturedPointerListener, View.OnGenericMotionListener {
    private boolean capslockMode;
    private final MenuHelper menuHelper;
    private boolean shiftMode;

    public void disableCursor() {
    }

    public void enableCursor() {
    }

    public MKManager(MenuHelper menuHelper) {
        this.menuHelper = menuHelper;
        menuHelper.baseLayout.setFocusable(true);
        menuHelper.baseLayout.setOnCapturedPointerListener(this);
        menuHelper.baseLayout.setOnGenericMotionListener(this);
        menuHelper.baseLayout.setOnKeyListener(this);
        menuHelper.baseLayout.requestFocus();
        menuHelper.baseLayout.requestPointerCapture();
        System.out.println("----------------------------------MKManager initialized!");
    }

    public boolean handleMouseEvent(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == 11) {
            if (motionEvent.getActionButton() == 1) {
                InputBridge.sendMouseEvent(this.menuHelper.launcher, 0, true);
            } else if (motionEvent.getActionButton() == 2 || motionEvent.getActionButton() == 8) {
                InputBridge.sendMouseEvent(this.menuHelper.launcher, 1, true);
            } else if (motionEvent.getActionButton() == 4) {
                InputBridge.sendMouseEvent(this.menuHelper.launcher, 2, true);
            }
        } else if (motionEvent.getActionMasked() == 12) {
            if (motionEvent.getActionButton() == 1) {
                InputBridge.sendMouseEvent(this.menuHelper.launcher, 0, false);
            } else if (motionEvent.getActionButton() == 2 || motionEvent.getActionButton() == 8) {
                InputBridge.sendMouseEvent(this.menuHelper.launcher, 1, false);
            } else if (motionEvent.getActionButton() == 4) {
                InputBridge.sendMouseEvent(this.menuHelper.launcher, 2, false);
            }
        } else if (motionEvent.getActionMasked() == 8) {
            if (this.menuHelper.launcher == 2) {
                CallbackBridge.sendScroll(motionEvent.getAxisValue(10), motionEvent.getAxisValue(9));
            } else {
                if (motionEvent.getAxisValue(9) > 0.0f) {
                    for (int i = 0; i < Math.abs((int) motionEvent.getAxisValue(9)); i++) {
                        InputBridge.sendMouseEvent(this.menuHelper.launcher, 3, true);
                    }
                }
                if (motionEvent.getAxisValue(9) < 0.0f) {
                    for (int i2 = 0; i2 < Math.abs((int) motionEvent.getAxisValue(9)); i2++) {
                        InputBridge.sendMouseEvent(this.menuHelper.launcher, 4, true);
                    }
                }
            }
        }
        return true;
    }

    @Override // android.view.View.OnGenericMotionListener
    public boolean onGenericMotion(View view, MotionEvent motionEvent) {
        if (this.menuHelper.touchCharInput.isEnabled()) {
            return true;
        }
        this.menuHelper.baseLayout.requestFocus();
        this.menuHelper.baseLayout.requestPointerCapture();
        return true;
    }

    @Override // android.view.View.OnCapturedPointerListener
    public boolean onCapturedPointer(View view, MotionEvent motionEvent) {
        float x;
        if (this.menuHelper.gameCursorMode == 0) {
            float f = 0.0f;
            if (this.menuHelper.cursorX + (motionEvent.getX() * this.menuHelper.gameMenuSetting.mouseSpeed) < 0.0f) {
                x = 0.0f;
            } else if (this.menuHelper.cursorX + (motionEvent.getX() * this.menuHelper.gameMenuSetting.mouseSpeed) > this.menuHelper.baseLayout.getWidth()) {
                x = this.menuHelper.baseLayout.getWidth();
            } else {
                x = this.menuHelper.cursorX + (motionEvent.getX() * this.menuHelper.gameMenuSetting.mouseSpeed);
            }
            if (this.menuHelper.cursorY + (motionEvent.getY() * this.menuHelper.gameMenuSetting.mouseSpeed) >= 0.0f) {
                if (this.menuHelper.cursorY + (motionEvent.getY() * this.menuHelper.gameMenuSetting.mouseSpeed) > this.menuHelper.baseLayout.getHeight()) {
                    f = this.menuHelper.baseLayout.getHeight();
                } else {
                    f = this.menuHelper.cursorY + (motionEvent.getY() * this.menuHelper.gameMenuSetting.mouseSpeed);
                }
            }
            this.menuHelper.cursorX = x;
            this.menuHelper.cursorY = f;
            this.menuHelper.pointerX = x;
            this.menuHelper.pointerY = f;
            InputBridge.setPointer(this.menuHelper.launcher, (int) (x * this.menuHelper.scaleFactor), (int) (f * this.menuHelper.scaleFactor));
        } else {
            this.menuHelper.pointerX += motionEvent.getX() * this.menuHelper.gameMenuSetting.mouseSpeed;
            this.menuHelper.pointerY += motionEvent.getY() * this.menuHelper.gameMenuSetting.mouseSpeed;
            MenuHelper menuHelper = this.menuHelper;
            menuHelper.currentX = menuHelper.pointerX;
            MenuHelper menuHelper2 = this.menuHelper;
            menuHelper2.currentY = menuHelper2.pointerY;
            InputBridge.setPointer(this.menuHelper.launcher, (int) (this.menuHelper.pointerX * this.menuHelper.scaleFactor), (int) (this.menuHelper.pointerY * this.menuHelper.scaleFactor));
        }
        return handleMouseEvent(motionEvent);
    }

    @Override // android.view.View.OnKeyListener
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        char c = 'B';
        if ((keyEvent.getFlags() & 2) == 2) {
            if (keyEvent.getKeyCode() == 66) {
                return true;
            }
            this.menuHelper.touchCharInput.dispatchKeyEvent(keyEvent);
            return true;
        }
        int keyCode = keyEvent.getKeyCode();
        if (keyCode != 0) {
            char c2 = 'C';
            char c3 = 'A';
            char c4 = 'y';
            char c5 = 'Q';
            if (keyCode == 81) {
                InputBridge.sendEvent(this.menuHelper.launcher, 334, keyEvent.getAction() == 0);
            } else if (keyCode == 111) {
                InputBridge.sendEvent(this.menuHelper.launcher, 256, keyEvent.getAction() == 0);
            } else if (keyCode == 121) {
                InputBridge.sendEvent(this.menuHelper.launcher, 284, keyEvent.getAction() == 0);
            } else if (keyCode == 124) {
                InputBridge.sendEvent(this.menuHelper.launcher, 260, keyEvent.getAction() == 0);
            } else if (keyCode == 3) {
                InputBridge.sendEvent(this.menuHelper.launcher, 268, keyEvent.getAction() == 0);
            } else if (keyCode != 4) {
                if (keyCode == 92) {
                    InputBridge.sendEvent(this.menuHelper.launcher, 266, keyEvent.getAction() == 0);
                } else if (keyCode != 93) {
                    switch (keyCode) {
                        case 7:
                            InputBridge.sendEvent(this.menuHelper.launcher, 48, keyEvent.getAction() == 0);
                            break;
                        case 8:
                            InputBridge.sendEvent(this.menuHelper.launcher, 49, keyEvent.getAction() == 0);
                            break;
                        case 9:
                            InputBridge.sendEvent(this.menuHelper.launcher, 50, keyEvent.getAction() == 0);
                            break;
                        case 10:
                            InputBridge.sendEvent(this.menuHelper.launcher, 51, keyEvent.getAction() == 0);
                            break;
                        case 11:
                            InputBridge.sendEvent(this.menuHelper.launcher, 52, keyEvent.getAction() == 0);
                            break;
                        case 12:
                            InputBridge.sendEvent(this.menuHelper.launcher, 53, keyEvent.getAction() == 0);
                            break;
                        case 13:
                            InputBridge.sendEvent(this.menuHelper.launcher, 54, keyEvent.getAction() == 0);
                            break;
                        case 14:
                            InputBridge.sendEvent(this.menuHelper.launcher, 55, keyEvent.getAction() == 0);
                            break;
                        case 15:
                            InputBridge.sendEvent(this.menuHelper.launcher, 56, keyEvent.getAction() == 0);
                            break;
                        case 16:
                            InputBridge.sendEvent(this.menuHelper.launcher, 57, keyEvent.getAction() == 0);
                            break;
                        default:
                            switch (keyCode) {
                                case 18:
                                    InputBridge.sendEvent(this.menuHelper.launcher, 51, keyEvent.getAction() == 0);
                                    break;
                                case 19:
                                    InputBridge.sendEvent(this.menuHelper.launcher, 265, keyEvent.getAction() == 0);
                                    break;
                                case 20:
                                    InputBridge.sendEvent(this.menuHelper.launcher, 264, keyEvent.getAction() == 0);
                                    break;
                                case 21:
                                    InputBridge.sendEvent(this.menuHelper.launcher, 263, keyEvent.getAction() == 0);
                                    break;
                                case 22:
                                    InputBridge.sendEvent(this.menuHelper.launcher, 262, keyEvent.getAction() == 0);
                                    break;
                                default:
                                    switch (keyCode) {
                                        case 29:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 65, keyEvent.getAction() == 0);
                                            break;
                                        case 30:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 66, keyEvent.getAction() == 0);
                                            break;
                                        case 31:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 67, keyEvent.getAction() == 0);
                                            break;
                                        case 32:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 68, keyEvent.getAction() == 0);
                                            break;
                                        case 33:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 69, keyEvent.getAction() == 0);
                                            break;
                                        case 34:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 70, keyEvent.getAction() == 0);
                                            break;
                                        case 35:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 71, keyEvent.getAction() == 0);
                                            break;
                                        case 36:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 72, keyEvent.getAction() == 0);
                                            break;
                                        case 37:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 73, keyEvent.getAction() == 0);
                                            break;
                                        case 38:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 74, keyEvent.getAction() == 0);
                                            break;
                                        case 39:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 75, keyEvent.getAction() == 0);
                                            break;
                                        case 40:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 76, keyEvent.getAction() == 0);
                                            break;
                                        case 41:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 77, keyEvent.getAction() == 0);
                                            break;
                                        case 42:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 78, keyEvent.getAction() == 0);
                                            break;
                                        case 43:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 79, keyEvent.getAction() == 0);
                                            break;
                                        case 44:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 80, keyEvent.getAction() == 0);
                                            break;
                                        case 45:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 81, keyEvent.getAction() == 0);
                                            break;
                                        case 46:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 82, keyEvent.getAction() == 0);
                                            break;
                                        case 47:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 83, keyEvent.getAction() == 0);
                                            break;
                                        case 48:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 84, keyEvent.getAction() == 0);
                                            break;
                                        case 49:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 85, keyEvent.getAction() == 0);
                                            break;
                                        case 50:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 86, keyEvent.getAction() == 0);
                                            break;
                                        case 51:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 87, keyEvent.getAction() == 0);
                                            break;
                                        case 52:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 88, keyEvent.getAction() == 0);
                                            break;
                                        case 53:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 89, keyEvent.getAction() == 0);
                                            break;
                                        case 54:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 90, keyEvent.getAction() == 0);
                                            break;
                                        case 55:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 44, keyEvent.getAction() == 0);
                                            break;
                                        case 56:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 46, keyEvent.getAction() == 0);
                                            break;
                                        case 57:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 342, keyEvent.getAction() == 0);
                                            break;
                                        case 58:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 346, keyEvent.getAction() == 0);
                                            break;
                                        case 59:
                                            this.shiftMode = keyEvent.getAction() == 0;
                                            InputBridge.sendEvent(this.menuHelper.launcher, 340, keyEvent.getAction() == 0);
                                            break;
                                        case 60:
                                            this.shiftMode = keyEvent.getAction() == 0;
                                            InputBridge.sendEvent(this.menuHelper.launcher, 344, keyEvent.getAction() == 0);
                                            break;
                                        case 61:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 258, keyEvent.getAction() == 0);
                                            break;
                                        case 62:
                                            InputBridge.sendEvent(this.menuHelper.launcher, 32, keyEvent.getAction() == 0);
                                            break;
                                        default:
                                            switch (keyCode) {
                                                case 66:
                                                    if (!this.menuHelper.enterLock && keyEvent.getAction() == 1 && this.menuHelper.touchCharInput != null && !this.menuHelper.touchCharInput.isEnabled()) {
                                                        this.menuHelper.touchCharInput.switchKeyboardState();
                                                    }
                                                    if (this.menuHelper.enterLock) {
                                                        this.menuHelper.enterLock = false;
                                                    }
                                                    return true;
                                                case 67:
                                                    InputBridge.sendEvent(this.menuHelper.launcher, 259, keyEvent.getAction() == 0);
                                                    break;
                                                case 68:
                                                    InputBridge.sendEvent(this.menuHelper.launcher, 96, keyEvent.getAction() == 0);
                                                    break;
                                                case 69:
                                                    InputBridge.sendEvent(this.menuHelper.launcher, 45, keyEvent.getAction() == 0);
                                                    break;
                                                case 70:
                                                    InputBridge.sendEvent(this.menuHelper.launcher, 61, keyEvent.getAction() == 0);
                                                    break;
                                                case 71:
                                                    InputBridge.sendEvent(this.menuHelper.launcher, 91, keyEvent.getAction() == 0);
                                                    break;
                                                case 72:
                                                    InputBridge.sendEvent(this.menuHelper.launcher, 93, keyEvent.getAction() == 0);
                                                    break;
                                                case 73:
                                                    InputBridge.sendEvent(this.menuHelper.launcher, 92, keyEvent.getAction() == 0);
                                                    break;
                                                case 74:
                                                    InputBridge.sendEvent(this.menuHelper.launcher, 59, keyEvent.getAction() == 0);
                                                    break;
                                                case 75:
                                                    InputBridge.sendEvent(this.menuHelper.launcher, 39, keyEvent.getAction() == 0);
                                                    break;
                                                case 76:
                                                    InputBridge.sendEvent(this.menuHelper.launcher, 47, keyEvent.getAction() == 0);
                                                    break;
                                                case 77:
                                                    InputBridge.sendEvent(this.menuHelper.launcher, 50, keyEvent.getAction() == 0);
                                                    break;
                                                default:
                                                    switch (keyCode) {
                                                        case 113:
                                                            InputBridge.sendEvent(this.menuHelper.launcher, 341, keyEvent.getAction() == 0);
                                                            break;
                                                        case 114:
                                                            InputBridge.sendEvent(this.menuHelper.launcher, 345, keyEvent.getAction() == 0);
                                                            break;
                                                        case 115:
                                                            if (keyEvent.getAction() == 1) {
                                                                this.capslockMode = !this.capslockMode;
                                                            }
                                                            InputBridge.sendEvent(this.menuHelper.launcher, 280, keyEvent.getAction() == 0);
                                                            break;
                                                        default:
                                                            switch (keyCode) {
                                                                case 131:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 290, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 132:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 291, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 133:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 292, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 134:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 293, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 135:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 294, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 136:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 295, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 137:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 296, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 138:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 297, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 139:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 298, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 140:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 299, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 141:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 300, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 142:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 301, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 143:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 282, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 144:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 320, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 145:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 321, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 146:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 322, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 147:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 323, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 148:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 324, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 149:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 325, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 150:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 326, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 151:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 327, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 152:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 328, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 153:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 329, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 154:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 331, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 155:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 332, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 156:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 333, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 157:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 334, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 158:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 46, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 159:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 44, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 160:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 257, keyEvent.getAction() == 0);
                                                                    break;
                                                                case 161:
                                                                    InputBridge.sendEvent(this.menuHelper.launcher, 61, keyEvent.getAction() == 0);
                                                                    break;
                                                            }
                                                    }
                                            }
                                    }
                            }
                    }
                } else {
                    InputBridge.sendEvent(this.menuHelper.launcher, 267, keyEvent.getAction() == 0);
                }
            }
            if (this.menuHelper.gameCursorMode == 0 && keyEvent.getAction() == 0) {
                int keyCode2 = keyEvent.getKeyCode();
                if (keyCode2 == 62) {
                    InputBridge.sendKeyChar(this.menuHelper.launcher, ' ');
                } else if (keyCode2 == 158) {
                    InputBridge.sendKeyChar(this.menuHelper.launcher, '.');
                } else if (keyCode2 != 159) {
                    switch (keyCode2) {
                        case 7:
                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? ')' : '0');
                            break;
                        case 8:
                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '!' : '1');
                            break;
                        case 9:
                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '@' : '2');
                            break;
                        case 10:
                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '#' : '3');
                            break;
                        case 11:
                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '$' : '4');
                            break;
                        case 12:
                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '%' : '5');
                            break;
                        case 13:
                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '^' : '6');
                            break;
                        case 14:
                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '&' : '7');
                            break;
                        case 15:
                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '*' : '8');
                            break;
                        case 16:
                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '(' : '9');
                            break;
                        default:
                            switch (keyCode2) {
                                case 29:
                                    int i2 = this.menuHelper.launcher;
                                    if (!this.capslockMode ? !this.shiftMode : this.shiftMode) {
                                        c3 = 'a';
                                    }
                                    InputBridge.sendKeyChar(i2, c3);
                                    break;
                                case 30:
                                    int i3 = this.menuHelper.launcher;
                                    if (!this.capslockMode ? !this.shiftMode : this.shiftMode) {
                                        c = 'b';
                                    }
                                    InputBridge.sendKeyChar(i3, c);
                                    break;
                                case 31:
                                    int i4 = this.menuHelper.launcher;
                                    if (!this.capslockMode ? !this.shiftMode : this.shiftMode) {
                                        c2 = 'c';
                                    }
                                    InputBridge.sendKeyChar(i4, c2);
                                    break;
                                case 32:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'd' : 'D');
                                    break;
                                case 33:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'e' : 'E');
                                    break;
                                case 34:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'f' : 'F');
                                    break;
                                case 35:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'g' : 'G');
                                    break;
                                case 36:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'h' : 'H');
                                    break;
                                case 37:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'i' : 'I');
                                    break;
                                case 38:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'j' : 'J');
                                    break;
                                case 39:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'k' : 'K');
                                    break;
                                case 40:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'l' : 'L');
                                    break;
                                case 41:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'm' : 'M');
                                    break;
                                case 42:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'n' : 'N');
                                    break;
                                case 43:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'o' : 'O');
                                    break;
                                case 44:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'p' : 'P');
                                    break;
                                case 45:
                                    int i5 = this.menuHelper.launcher;
                                    if (!this.capslockMode ? !this.shiftMode : this.shiftMode) {
                                        c5 = 'q';
                                    }
                                    InputBridge.sendKeyChar(i5, c5);
                                    break;
                                case 46:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'r' : 'R');
                                    break;
                                case 47:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 's' : 'S');
                                    break;
                                case 48:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 't' : 'T');
                                    break;
                                case 49:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'u' : 'U');
                                    break;
                                case 50:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'v' : 'V');
                                    break;
                                case 51:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'w' : 'W');
                                    break;
                                case 52:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'x' : 'X');
                                    break;
                                case 53:
                                    int i6 = this.menuHelper.launcher;
                                    if (!this.capslockMode ? this.shiftMode : !this.shiftMode) {
                                        c4 = 'Y';
                                    }
                                    InputBridge.sendKeyChar(i6, c4);
                                    break;
                                case 54:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, (!this.capslockMode ? this.shiftMode : !this.shiftMode) ? 'z' : 'Z');
                                    break;
                                case 55:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '<' : ',');
                                    break;
                                case 56:
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '>' : '.');
                                    break;
                                default:
                                    switch (keyCode2) {
                                        case 68:
                                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '~' : '`');
                                            break;
                                        case 69:
                                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '_' : '-');
                                            break;
                                        case 70:
                                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '+' : '=');
                                            break;
                                        case 71:
                                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '{' : '[');
                                            break;
                                        case 72:
                                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '}' : ']');
                                            break;
                                        case 73:
                                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '|' : '\\');
                                            break;
                                        case 74:
                                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? ':' : ';');
                                            break;
                                        case 75:
                                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '\"' : '\'');
                                            break;
                                        case 76:
                                            InputBridge.sendKeyChar(this.menuHelper.launcher, this.shiftMode ? '?' : '/');
                                            break;
                                        default:
                                            switch (keyCode2) {
                                                case 144:
                                                    InputBridge.sendKeyChar(this.menuHelper.launcher, '0');
                                                    break;
                                                case 145:
                                                    InputBridge.sendKeyChar(this.menuHelper.launcher, '1');
                                                    break;
                                                case 146:
                                                    InputBridge.sendKeyChar(this.menuHelper.launcher, '2');
                                                    break;
                                                case 147:
                                                    InputBridge.sendKeyChar(this.menuHelper.launcher, '3');
                                                    break;
                                                case 148:
                                                    InputBridge.sendKeyChar(this.menuHelper.launcher, '4');
                                                    break;
                                                case 149:
                                                    InputBridge.sendKeyChar(this.menuHelper.launcher, '5');
                                                    break;
                                                case 150:
                                                    InputBridge.sendKeyChar(this.menuHelper.launcher, '6');
                                                    break;
                                                case 151:
                                                    InputBridge.sendKeyChar(this.menuHelper.launcher, '7');
                                                    break;
                                                case 152:
                                                    InputBridge.sendKeyChar(this.menuHelper.launcher, '8');
                                                    break;
                                                case 153:
                                                    InputBridge.sendKeyChar(this.menuHelper.launcher, '9');
                                                    break;
                                            }
                                    }
                            }
                    }
                } else {
                    InputBridge.sendKeyChar(this.menuHelper.launcher, ',');
                }
            }
        }
        return true;
    }
}
