package com.hongbog.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.tzutalin.dlibtest.CameraConnectionFragment;
import com.tzutalin.dlibtest.Dlog;
import com.tzutalin.dlibtest.R;

/**
 * Created by darts on 2017-10-07.
 */

public class CustomView extends View {

    private Bitmap mBitmap;

    private float mEyeWidth = 0;
    private float mEyeHeight = 0;
    private float mStartLeft = 0;
    private float mStartTop = 0;

    private float mEye2Eye = 0;

    private float mRatioWidth = 0;
    private float mRatioHeight = 0;

    public float getmRatioWidth() {
        return mRatioWidth;
    }
    public float getmRatioHeight() {
        return mRatioHeight;
    }

    public float getEyeWidth() {
        return mEyeWidth;
    }

    public float getEyeHeight() {
        return mEyeHeight;
    }

    public float getStartLeft() {
        return mStartLeft;
    }

    public float getStartTop() {
        return mStartTop;
    }

    public float getEye2Eye() {
        return mEye2Eye;
    }

    public CustomView(CameraConnectionFragment context) {
        super(context.getActivity().getBaseContext());
        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.guide_eye);
        setBackgroundColor(Color.TRANSPARENT);
    }


    public void setAspectRatio(final int width, final int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }

        mRatioWidth = width;
        mRatioHeight = height;

        float hundredDp = TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 100,
                getResources().getDisplayMetrics() );

        mEyeWidth = mRatioWidth/9;
        mEyeHeight = mRatioHeight/20;
        mStartLeft = mEyeWidth * 3;                 // start point x
        mStartTop = (mEyeHeight * 2) + hundredDp;   // start point y
        mEye2Eye = mEyeWidth;
        mBitmap = Bitmap.createScaledBitmap(mBitmap, (int)mEyeWidth, (int)mEyeHeight,  true);

        Dlog.d("mRatioHeight  : " + mRatioHeight );
        Dlog.d("(int)mEyeHeight  : " + (int)mEyeHeight );
        Dlog.d("(int)mEyeWidth  : " + (int)mEyeWidth );
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, mStartLeft, mStartTop, null);
        canvas.drawBitmap(mBitmap, mStartLeft + mEyeWidth + mEye2Eye, mStartTop, null);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Dlog.d("onSizeChanged");
        setAspectRatio(w, h);
    }
}