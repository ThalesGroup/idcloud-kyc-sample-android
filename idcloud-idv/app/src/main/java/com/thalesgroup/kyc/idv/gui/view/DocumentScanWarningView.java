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
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;

import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.gui.MainActivity;
import com.thalesgroup.kyc.idv.gui.animation.EaseInterpolators;
import com.thalesgroup.kyc.idv.gui.animation.FinishedOrCanceledListener;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class DocumentScanWarningView extends LinearLayout {

    //region Definition

    private TextSwitcher mCaption;
    private boolean mDisplayed = true;
    private boolean mAnimated = false;
    private @StringRes int mLastCaption = 0;

    //endregion

    //region Life Cycle

    public DocumentScanWarningView(final Context context, final @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mCaption = findViewById(R.id.view_document_scan_warning_caption);
        mCaption.setInAnimation(getContext(), R.anim.fade_in);
        mCaption.setOutAnimation(getContext(), R.anim.fade_out);

        hide(false);
    }

    //endregion


    //region Private Helpers

    private float getHiddenPosY() {
        // TODO: Read actual value.
        return Math.round(120.f * getResources().getDisplayMetrics().density);
    }

    private void cancelCurrentAnimation() {
        if (mAnimated) {
            clearAnimation();
            animate().cancel();
            animate().setListener(null);
            mAnimated = false;
        }
    }

    private void updateCaption(@StringRes final int caption) {
        if (mLastCaption != caption) {
            if (caption != 0) {
                mCaption.setText(getContext().getText(caption));
            }
            mLastCaption = caption;
        }
    }

    //endregion

    //region Public API

    public void display(@StringRes final int caption, final boolean animated) {
        // Redundant call
        if (mDisplayed) {
            // In this case we want to just animate text change.
            updateCaption(caption);
            return;
        }

        // Cancel any possible current animation.
        cancelCurrentAnimation();

        // Animations are fast.. update caption without knowing current display animation state.
        updateCaption(caption);

        mAnimated = true;
        mDisplayed = true;

        if (animated) {
            // Move view out of the screen (bottom side).
            animate().translationY(.0f)
                    .setDuration(MainActivity.ANIM_DURATION_SLOW_MS)
                    .setInterpolator(new EaseInterpolators.EaseInOut())
                    .setListener(new FinishedOrCanceledListener(() -> mAnimated = false, true))
                    .start();
        } else {
            setTranslationY(.0f);
        }
    }

    public void hide(final boolean animated) {
        // Redundant call
        if (!mDisplayed) {
            return;
        }

        // Cancel any possible current animation.
        cancelCurrentAnimation();

        mAnimated = true;
        mDisplayed = false;
        final float hiddenPosY = getHiddenPosY();

        if (animated) {
            // Move view out of the screen (bottom side).
            animate().translationY(hiddenPosY)
                    .setDuration(MainActivity.ANIM_DURATION_SLOW_MS)
                    .setInterpolator(new EaseInterpolators.EaseInOut())
                    .setListener(new FinishedOrCanceledListener(() -> mAnimated = false, true))
                    .start();
        } else {
            setTranslationY(hiddenPosY);
        }
    }

    //endregion
}
