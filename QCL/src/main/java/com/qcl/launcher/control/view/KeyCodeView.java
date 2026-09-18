package com.qcl.launcher.control.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatButton;
import com.qcl.launcher.R;
import java.util.ArrayList;

/* loaded from: classes2.dex */
public class KeyCodeView extends AppCompatButton {
    private long downTime;
    private float initialX;
    private float initialY;
    private Integer keyCode;
    private OnKeyCodeChangeListener onKeyCodeChangeListener;
    private boolean selected;

    /* loaded from: classes2.dex */
    public interface OnKeyCodeChangeListener {
        void onKeyCodeAdd(int i);

        void onKeyCodeRemove(int i);
    }

    public KeyCodeView(Context context) {
        super(context);
        this.selected = false;
    }

    public KeyCodeView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.selected = false;
        this.keyCode = Integer.valueOf(context.obtainStyledAttributes(attributeSet, R.styleable.KeyCodeView).getInteger(0, -1));
    }

    @Override // android.widget.TextView, android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            this.downTime = System.currentTimeMillis();
            this.initialX = motionEvent.getX();
            this.initialY = motionEvent.getY();
        } else if ((actionMasked == 1 || actionMasked == 3) && Math.abs(motionEvent.getX() - this.initialX) <= 10.0f && Math.abs(motionEvent.getY() - this.initialY) <= 10.0f && System.currentTimeMillis() - this.downTime <= 200) {
            if (this.selected) {
                setBackground(getContext().getDrawable(R.drawable.launcher_button_normal));
                OnKeyCodeChangeListener onKeyCodeChangeListener = this.onKeyCodeChangeListener;
                if (onKeyCodeChangeListener != null) {
                    onKeyCodeChangeListener.onKeyCodeRemove(this.keyCode.intValue());
                }
                this.selected = false;
            } else {
                setBackground(getContext().getDrawable(R.drawable.launcher_button_selected));
                OnKeyCodeChangeListener onKeyCodeChangeListener2 = this.onKeyCodeChangeListener;
                if (onKeyCodeChangeListener2 != null) {
                    onKeyCodeChangeListener2.onKeyCodeAdd(this.keyCode.intValue());
                }
                this.selected = true;
            }
        }
        return true;
    }

    public void checkSelection(ArrayList<Integer> arrayList) {
        if (arrayList.contains(this.keyCode)) {
            setBackground(getContext().getDrawable(R.drawable.launcher_button_selected));
            this.selected = true;
        } else {
            setBackground(getContext().getDrawable(R.drawable.launcher_button_normal));
            this.selected = false;
        }
    }

    public void setOnKeyCodeChangeListener(OnKeyCodeChangeListener onKeyCodeChangeListener) {
        this.onKeyCodeChangeListener = onKeyCodeChangeListener;
    }
}
