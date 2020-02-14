/*
 * MIT License
 *
 * Copyright (c) 2020 Thales DIS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * IMPORTANT: This source code is intended to serve training information purposes only.
 *            Please make sure to review our IdCloud documentation, including security guidelines.
 */

package com.thalesgroup.kyc.idv.gui.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.thalesgroup.kyc.idv.R;

/**
 * A view that crop an image circularly to give impression of a circular video display
 */
public class ClippingView extends View {

    //region Defines

    //private final static float CLIP_RADIUS = 150; // dp
    private final static float WIDTH_RATIO = 0.95f;

    private Bitmap mBackgroundDrawable;
    private Bitmap mBitmapBuffer;

    private Canvas mCanvasBuffer;
    private Paint mTransparentPaint;
    private float mCx;
    private float mCy;
    private float mRadius;
    private Rect mRectDrawableSrc;
    private Rect mRectDrawableDest;

    //endregion

    //region Life Cycle

    public ClippingView(final Context context) {
        super(context);
        init();
    }

    public ClippingView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ClippingView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mBackgroundDrawable = getBackgroundBitmap();

        mTransparentPaint = new Paint();
        mTransparentPaint.setColor(getResources().getColor(android.R.color.transparent));
        mTransparentPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
        mTransparentPaint.setAntiAlias(true);
    }

    @Override
    protected void finalize() throws Throwable {
        if(mBitmapBuffer != null) {
            mBitmapBuffer.recycle();
        }
        if (mBackgroundDrawable != null) {
            mBackgroundDrawable.recycle();
        }

        super.finalize();
    }

    //endregion

    //region Override

    @Override
    protected void onSizeChanged(final int width, final int height, final int oldw, final int oldh) {
        super.onSizeChanged(width, height, oldw, oldh);

        if (mBitmapBuffer != null) {
            mBitmapBuffer.recycle();
        }

        mBitmapBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mBitmapBuffer.eraseColor(Color.TRANSPARENT);
        mCanvasBuffer = new Canvas(mBitmapBuffer);

        final int paddBottom = getPaddingBottom();

        mCx = width / 2;
        mCy = height / 2 - paddBottom / 2;

        mRadius = WIDTH_RATIO * width / 2;

        mRectDrawableSrc = new Rect(0,
                                    0,
                                    mBackgroundDrawable.getWidth() - 1,
                                    mBackgroundDrawable.getHeight() - 1);
        mRectDrawableDest = new Rect(0, 0, width - 1, height - 1);
    }

    @Override
    public void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        mCanvasBuffer.drawBitmap(mBackgroundDrawable, mRectDrawableSrc, mRectDrawableDest, null);
        mCanvasBuffer.drawCircle(mCx, mCy, mRadius, mTransparentPaint);
        canvas.drawBitmap(mBitmapBuffer, 0, 0, null);
    }

    //endregion

    //region Private Helpers

    private Bitmap getBackgroundBitmap() {
        final Point size = new Point();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getSize(size);

        final Drawable drawable = getContext().getResources().getDrawable(R.drawable.img_gradient_primary);
        final Bitmap mutableBitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_4444);
        final Canvas canvas = new Canvas(mutableBitmap);
        drawable.setBounds(0, 0, size.x, size.y);
        drawable.draw(canvas);

        return mutableBitmap;
    }

    //endregion
}