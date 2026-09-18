package com.qcl.launcher.launcher.view.list;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import com.qcl.launcher.R;
import com.qcl.launcher.utils.convert.ConvertUtils;

/* loaded from: classes2.dex */
public class MaxHeightRecyclerView extends RecyclerView {
    private int maxHeight;

    public MaxHeightRecyclerView(Context context) {
        super(context);
        this.maxHeight = 300;
        init(context, null);
    }

    public MaxHeightRecyclerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.maxHeight = 300;
        init(context, attributeSet);
    }

    private void init(Context context, AttributeSet attributeSet) {
        TypedArray obtainStyledAttributes = attributeSet == null ? null : context.obtainStyledAttributes(attributeSet, R.styleable.MaxHeightRecycler);
        if (obtainStyledAttributes != null) {
            try {
                this.maxHeight = obtainStyledAttributes.getInteger(0, 300);
            } finally {
                obtainStyledAttributes.recycle();
            }
        }
        this.maxHeight = ConvertUtils.dip2px(getContext(), this.maxHeight);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.recyclerview.widget.RecyclerView, android.view.View
    public void onMeasure(int i, int i2) {
        if (getChildCount() > 0) {
            View childAt = getChildAt(0);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) childAt.getLayoutParams();
            childAt.measure(i, View.MeasureSpec.makeMeasureSpec(0, 0));
            setMeasuredDimension(i, Math.min(getAdapter().getItemCount() * ConvertUtils.dip2px(getContext(), childAt.getMeasuredHeight() + getPaddingTop() + getPaddingBottom() + layoutParams.topMargin + layoutParams.bottomMargin), this.maxHeight));
            return;
        }
        super.onMeasure(i, i2);
    }
}
