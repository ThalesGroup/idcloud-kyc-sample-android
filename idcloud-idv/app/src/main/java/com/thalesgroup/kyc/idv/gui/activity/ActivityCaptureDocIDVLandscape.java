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

package com.thalesgroup.kyc.idv.gui.activity;

import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.thalesgroup.kyc.idv.helpers.AbstractOption;

public class ActivityCaptureDocIDVLandscape extends AbstractActivityCaptureDocIDV {

    //region Abstract methods

    protected void resizeCaptureFrame() {
        if (mCaptureFrame == null) {
            return;
        }

        double percentage = 0.77;

        double ratio;
        if (mDocumentType == AbstractOption.DocumentType.IdCard) {
            ratio = 1.586;
        } else {
            ratio = 1.333;
        }

        getWindowManager().getDefaultDisplay();
        final Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);

        final ViewGroup.LayoutParams params = mCaptureFrame.getLayoutParams();
        params.width = (int) ((double) size.y * percentage * ratio);
        params.height = (int) ((double) size.y * percentage);

        mCaptureFrameMrz.setVisibility(mDocumentType == AbstractOption.DocumentType.IdCard ? View.INVISIBLE : View.VISIBLE);

        final ViewGroup.MarginLayoutParams paramsMargin = (ViewGroup.MarginLayoutParams)mCaptureFrameMrz.getLayoutParams();
        paramsMargin.bottomMargin = (int)(params.height / 3.8f);
        mCaptureFrameMrz.setLayoutParams(paramsMargin);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState){
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause(){
        if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }

        super.onPause();
    }

    @Override
    public void onResume(){
        if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

        super.onResume();
    }

    //endregion
}
