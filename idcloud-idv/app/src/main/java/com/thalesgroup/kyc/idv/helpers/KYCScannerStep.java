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

package com.thalesgroup.kyc.idv.helpers;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;

public class KYCScannerStep {

    //region Definition

    enum KYCStepAnimation {
        None,
        FlipHorizontally
    }

    enum KYCStepType {
        Capture,
        TurnOver
    }

    final private KYCStepType mStepType;

    @StringRes
    private int mSideBarCaption;
    @IdRes
    private int mSideBarIcon;
    @IdRes
    private int mOverlayIcon;
    @StringRes
    private int mOverlayCaptionTop;
    @StringRes
    private int mOverlayCaptionBottom;
    @IdRes
    private int mOverlayAnimationImage;
    private KYCStepAnimation mOverlayAnimation;
    private String mSoundName;


    //endregion

    KYCScannerStep(final KYCStepType type) {
        mStepType = type;
    }

    //region Properties

    public KYCStepType getmStepType() {
        return mStepType;
    }

    @StringRes
    public int getSideBarCaption() {
        return mSideBarCaption;
    }

    public KYCScannerStep setSideBarCaption(@StringRes final int mSideBarCaption) {
        this.mSideBarCaption = mSideBarCaption;
        return this;
    }

    @IdRes
    public int getSideBarIcon() {
        return mSideBarIcon;
    }

    public KYCScannerStep setSideBarIcon(final @IdRes int mSideBarIcon) {
        this.mSideBarIcon = mSideBarIcon;
        return this;
    }

    @IdRes
    public int getOverlayIcon() {
        return mOverlayIcon;
    }

    public KYCScannerStep setOverlayIcon(final @IdRes int mOverlayIcon) {
        this.mOverlayIcon = mOverlayIcon;
        return this;
    }

    @StringRes
    public int getOverlayCaptionTop() {
        return mOverlayCaptionTop;
    }

    public KYCScannerStep setOverlayCaptionTop(final @StringRes int mOverlayCaptionTop) {
        this.mOverlayCaptionTop = mOverlayCaptionTop;
        return this;
    }

    @StringRes
    public int getOverlayCaptionBottom() {
        return mOverlayCaptionBottom;
    }

    public KYCScannerStep setOverlayCaptionBottom(final @StringRes int mOverlayCaptionBottom) {
        this.mOverlayCaptionBottom = mOverlayCaptionBottom;
        return this;
    }

    @IdRes
    public int getOverlayAnimationImage() {
        return mOverlayAnimationImage;
    }

    public KYCScannerStep setOverlayAnimationImage(final @IdRes int mOverlayAnimationImage) {
        this.mOverlayAnimationImage = mOverlayAnimationImage;
        return this;
    }

    public KYCStepAnimation getOverlayAnimation() {
        return mOverlayAnimation;
    }

    public KYCScannerStep setOverlayAnimation(final KYCStepAnimation mOverlayAnimation) {
        this.mOverlayAnimation = mOverlayAnimation;
        return this;
    }

    public String getSoundName() {
        return mSoundName;
    }

    public KYCScannerStep setSoundName(final String mSoundName) {
        this.mSoundName = mSoundName;
        return this;
    }

    //endregion

}


