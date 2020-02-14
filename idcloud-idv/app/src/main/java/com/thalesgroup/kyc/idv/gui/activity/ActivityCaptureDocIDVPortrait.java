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

import android.graphics.Point;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.view.ViewGroup;

import com.thalesgroup.kyc.idv.helpers.AbstractOption;

import java.util.List;

public class ActivityCaptureDocIDVPortrait extends AbstractActivityCaptureDocIDV {

    //region Abstract methods

    @Override
    Camera.Size initIdealCameraResolutionAndUpdateLayout() {
        Camera.Size retValue;

        // Calculate ideal menu size based on configuration.
        final int minHeightPx = dpToPx(SIDE_MENU_MIN_SIZE_DP);
        final int maxHeightPx = dpToPx(SIDE_MENU_MAX_SIZE_DP);

        // Find closest possible camera preview resolution.
        retValue = getIdealCameraResolution(minHeightPx, maxHeightPx);

        // Get actual display resolution
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        // Calculate scale based on height differences.
        final double scale = (double) displayMetrics.widthPixels / (double) retValue.height;

        // Get actual preview width after scaling
        final int scaledHeight = (int) ((double) retValue.width * scale);

        // Device might not support any ideal resolution.
        // In that case preview will be scaled vertically.
        // Do not allow menu to be smaller than configuration.
        final ViewGroup.LayoutParams layoutParams = mLayoutTutorial.getLayoutParams();
        final int space = displayMetrics.heightPixels - scaledHeight;
        layoutParams.height = Math.max(space, minHeightPx);
        mLayoutTutorial.setLayoutParams(layoutParams);

        return retValue;
    }

    protected void resizeCaptureFrame() {
        if (mCaptureFrame == null) {
            return;
        }

        double percentage;
        if (mLimitDetectionZone) {
            percentage = 0.80;
        } else {
            percentage = 0.95;
        }

        double ratio;
        if (mDocumentType == AbstractOption.DocumentType.IdCard) {
            ratio = 1.586;
        } else {
            ratio = 1.333;
        }

        getWindowManager(). getDefaultDisplay();
        final Point size = new Point();
        getWindowManager(). getDefaultDisplay().getSize(size);

        final ViewGroup.LayoutParams params = mCaptureFrame.getLayoutParams();
        params.width = (int)((double) size.x * percentage);
        params.height = (int)((double) size.x * percentage / ratio);
    }

    //endregion

    //region Private Helpers

    private Camera.Size getIdealCameraResolution(final int minHeight, final int maxHeight) {
        final List<Camera.Size> resList = Camera.open().getParameters().getSupportedPreviewSizes();
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        // resList is ordered from highest / best resolution supported.
        for (final Camera.Size loopSize : resList) {
            // Calculate scale based on height differences.
            final double scale = (double) displayMetrics.widthPixels / (double) loopSize.height;
            // Get actual preview height after scaling
            final int scaledSize = (int) ((double) loopSize.width * scale);

            // Preview aspect ratio allow to fit require size.
            final int space = displayMetrics.heightPixels - scaledSize;
            if (space > minHeight && space < maxHeight) {
                return loopSize;
            }
        }

        // There is no ideal aspect to fit both. Simple return best resolution possible.
        return resList.get(0);
    }

    //endregion

}
