package com.qcl.launcher.launcher.view.list;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;
import com.qcl.launcher.R;

/* loaded from: classes2.dex */
public class ContentListView extends ListView {
    private float maxHeight;

    public ContentListView(Context context) {
        super(context);
        this.maxHeight = 10000.0f;
    }

    public ContentListView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.maxHeight = 10000.0f;
    }

    public ContentListView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.maxHeight = 10000.0f;
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ContentListView, 0, i);
        int indexCount = obtainStyledAttributes.getIndexCount();
        for (int i2 = 0; i2 < indexCount; i2++) {
            int index = obtainStyledAttributes.getIndex(i2);
            if (index == 0) {
                this.maxHeight = obtainStyledAttributes.getDimension(index, -1.0f);
            }
        }
        obtainStyledAttributes.recycle();
    }

    @Override // android.widget.ListView, android.widget.AbsListView, android.view.View
    protected void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i2);
        float f = this.maxHeight;
        if (f <= size && f > -1.0f) {
            i2 = View.MeasureSpec.makeMeasureSpec(Float.valueOf(f).intValue(), Integer.MIN_VALUE);
        }
        super.onMeasure(i, i2);
    }

    public void setMaxHeight(float f) {
        this.maxHeight = f;
    }

    public float getMaxHeight() {
        return this.maxHeight;
    }
}
