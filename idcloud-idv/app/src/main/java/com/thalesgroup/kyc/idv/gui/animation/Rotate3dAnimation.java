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

package com.thalesgroup.kyc.idv.gui.animation;


import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class Rotate3dAnimation extends Animation {

    //region Definition

    private final float mFromXDegrees;
    private final float mToXDegrees;
    private final float mFromYDegrees;
    private final float mToYDegrees;
    private final float mFromZDegrees;
    private final float mToZDegrees;
    private Camera mCamera;
    private int mWidth = 0;
    private int mHeight = 0;

    FinishedOrCanceledListener.FinishedOrCanceledHandler mMiddleHandler = null;

    //endregion

    //region Life Cycle

    public Rotate3dAnimation(final float fromXDegrees,
                             final float toXDegrees,
                             final float fromYDegrees,
                             final float toYDegrees,
                             final float fromZDegrees,
                             final float toZDegrees) {
        super();

        mFromXDegrees = fromXDegrees;
        mToXDegrees = toXDegrees;
        mFromYDegrees = fromYDegrees;
        mToYDegrees = toYDegrees;
        mFromZDegrees = fromZDegrees;
        mToZDegrees = toZDegrees;
    }

    //endregion

    //region Public API

    public void setMiddleHandler(final FinishedOrCanceledListener.FinishedOrCanceledHandler handler) {
        mMiddleHandler = handler;
    }

    //endregion

    //region Override

    @Override
    public void initialize(final int width,
                           final int height,
                           final int parentWidth,
                           final int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);

        mWidth = width / 2;
        mHeight = height / 2;
        mCamera = new Camera();
    }

    @Override
    protected void applyTransformation(final float interpolatedTime,
                                       final Transformation transformation) {
        final float xDegrees = mFromXDegrees + ((mToXDegrees - mFromXDegrees) * interpolatedTime);
        final float yDegrees = mFromYDegrees + ((mToYDegrees - mFromYDegrees) * interpolatedTime);
        final float zDegrees = mFromZDegrees + ((mToZDegrees - mFromZDegrees) * interpolatedTime);

        final Matrix matrix = transformation.getMatrix();

        mCamera.save();
        mCamera.rotateX(xDegrees);
        mCamera.rotateY(yDegrees);
        mCamera.rotateZ(zDegrees);
        mCamera.getMatrix(matrix);
        mCamera.restore();

        matrix.preTranslate(-this.mWidth, -this.mHeight);
        matrix.postTranslate(this.mWidth, this.mHeight);

        // Notify in or slightly after half.
        if (mMiddleHandler != null && interpolatedTime >= .5f) {
            mMiddleHandler.onFinished();
            mMiddleHandler = null;
        }
    }

    //endregion
}