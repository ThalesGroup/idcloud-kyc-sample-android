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

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.thalesgroup.kyc.idv.R;

public class LivenessProgressbarView extends RelativeLayout {

    //region Definition

    private static final int SMOOTH_MULTIPLIER = 100;

    private ProgressBar mProgressPositive = null;
    private ProgressBar mProgressNegative = null;
    private int mThreshold = 0;
    private int mProgress = 0;

    //endregion

    //region Life Cycle

    public LivenessProgressbarView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mProgressPositive = findViewById(R.id.view_liveness_progressbar_positive);
        mProgressPositive.setProgress(0);

        mProgressNegative = findViewById(R.id.view_liveness_progressbar_negative);
        mProgressNegative.setProgress(0);
    }

    //endregion

    //region Public API

    public void setThreshold(final int threshold) {
        mThreshold = threshold * SMOOTH_MULTIPLIER;
    }

    public void setProgress(final int progress) {
        // Prevent multiple calls with same value.
        if (mProgress == progress) {
            return;
        }
        mProgress = progress;

        final int progressScaled = progress * SMOOTH_MULTIPLIER;
        final int progressNegative = Math.min(progressScaled, mThreshold);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mProgressNegative.setProgress(progressNegative, true);
            mProgressPositive.setProgress(progressScaled, true);
        } else {
            mProgressNegative.setProgress(progressNegative);
            mProgressPositive.setProgress(progressScaled);
        }
    }

    //endregion
}
