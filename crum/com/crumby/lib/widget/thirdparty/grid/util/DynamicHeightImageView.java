package com.crumby.lib.widget.thirdparty.grid.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;
import android.widget.ImageView;

public class DynamicHeightImageView extends ImageView {
    private double mHeightRatio;

    public DynamicHeightImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DynamicHeightImageView(Context context) {
        super(context);
    }

    public void setHeightRatio(double ratio) {
        if (ratio != this.mHeightRatio) {
            this.mHeightRatio = ratio;
            requestLayout();
        }
    }

    public double getHeightRatio() {
        return this.mHeightRatio;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mHeightRatio > 0.0d) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            setMeasuredDimension(width, (int) (((double) width) * this.mHeightRatio));
            return;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
