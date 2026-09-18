package com.qcl.launcher.control.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatEditText;
import com.qcl.launcher.control.CharacterSenderStrategy;
import com.qcl.launcher.control.MenuHelper;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class TouchCharInput extends AppCompatEditText {
    private CharacterSenderStrategy mCharacterSender;
    private boolean mIsDoingInternalChanges;
    private MenuHelper menuHelper;

    public TouchCharInput(Context context) {
        this(context, null);
    }

    public TouchCharInput(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.editTextStyle);
    }

    public TouchCharInput(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mIsDoingInternalChanges = false;
        setup();
    }

    @Override // android.widget.TextView
    protected void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        super.onTextChanged(charSequence, i, i2, i3);
        if (this.mIsDoingInternalChanges) {
            return;
        }
        if (this.mCharacterSender != null) {
            int i4 = 0;
            for (int i5 = 0; i5 < i2; i5++) {
                this.mCharacterSender.sendBackspace(this.menuHelper.launcher);
            }
            while (i4 < i3) {
                this.mCharacterSender.sendChar(this.menuHelper.launcher, charSequence.charAt(i));
                i4++;
                i++;
            }
        }
        if (charSequence.length() < 1) {
            clear();
        }
    }

    @Override // android.widget.TextView, android.view.View
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        disable();
    }

    @Override // android.widget.TextView, android.view.View
    public boolean onKeyPreIme(int i, KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == 4 && keyEvent.getAction() == 1) {
            disable();
        }
        return super.onKeyPreIme(i, keyEvent);
    }

    public boolean switchKeyboardState() {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService("input_method");
        if (hasFocus() || (getResources().getConfiguration().keyboard == 2 && getResources().getConfiguration().hardKeyboardHidden == 2)) {
            inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            clear();
            disable();
            this.menuHelper.baseLayout.requestFocus();
            this.menuHelper.baseLayout.requestPointerCapture();
            return false;
        }
        this.menuHelper.baseLayout.releasePointerCapture();
        this.menuHelper.baseLayout.clearFocus();
        enable();
        inputMethodManager.showSoftInput(this, 1);
        return true;
    }

    public void clear() {
        this.mIsDoingInternalChanges = true;
        setText("                              ");
        setSelection(getText().length());
        this.mIsDoingInternalChanges = false;
    }

    public void enable() {
        setEnabled(true);
        setFocusable(true);
        setVisibility(0);
        requestFocus();
    }

    public void disable() {
        clear();
        setVisibility(8);
        clearFocus();
        setEnabled(false);
    }

    private void sendEnter() {
        this.mCharacterSender.sendEnter(this.menuHelper.launcher);
        clear();
    }

    public void setCharacterSender(MenuHelper menuHelper, CharacterSenderStrategy characterSenderStrategy) {
        this.menuHelper = menuHelper;
        this.mCharacterSender = characterSenderStrategy;
    }

    private void setup() {
        setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: com.qcl.launcher.control.view.TouchCharInput$$ExternalSyntheticLambda0
            @Override // android.widget.TextView.OnEditorActionListener
            public final boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                return TouchCharInput.this.m202lambda$setup$0$comqcllaunchercontrolviewTouchCharInput(textView, i, keyEvent);
            }
        });
        clear();
        disable();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setup$0$com-qcl-launcher-control-view-TouchCharInput, reason: not valid java name */
    public /* synthetic */ boolean m202lambda$setup$0$comqcllaunchercontrolviewTouchCharInput(TextView textView, int i, KeyEvent keyEvent) {
        this.menuHelper.enterLock = true;
        ((InputMethodManager) getContext().getSystemService("input_method")).hideSoftInputFromWindow(getWindowToken(), 0);
        sendEnter();
        clear();
        disable();
        this.menuHelper.baseLayout.requestFocus();
        this.menuHelper.baseLayout.requestPointerCapture();
        return false;
    }
}
