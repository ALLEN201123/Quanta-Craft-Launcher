package com.qcl.launcher.control;

import org.lwjgl.glfw.CallbackBridge;

/* loaded from: classes2.dex */
public class LwjglCharSender implements CharacterSenderStrategy {
    @Override // com.qcl.launcher.control.CharacterSenderStrategy
    public void sendBackspace(int i) {
        CallbackBridge.sendKeycode(259, '\b', 0, 0, true);
        CallbackBridge.sendKeycode(259, '\b', 0, 0, false);
    }

    @Override // com.qcl.launcher.control.CharacterSenderStrategy
    public void sendEnter(int i) {
        CallbackBridge.sendKeyPress(257);
    }

    @Override // com.qcl.launcher.control.CharacterSenderStrategy
    public void sendChar(int i, char c) {
        CallbackBridge.sendChar(c, 0);
    }
}
