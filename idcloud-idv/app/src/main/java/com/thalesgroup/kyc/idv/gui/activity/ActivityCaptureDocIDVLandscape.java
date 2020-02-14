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

public class ActivityCaptureDocIDVLandscape extends AbstractActivityCaptureDocIDV {

    //region Abstract methods

    @Override
    Camera.Size initIdealCameraResolutionAndUpdateLayout() {
        Camera.Size retValue;

        // Calculate ideal menu size based on configuration.
        final int minWidthPx = dpToPx(SIDE_MENU_MIN_SIZE_DP);
        final int maxWidthPx = dpToPx(SIDE_MENU_MAX_SIZE_DP);

        // Find closest possible camera preview resolution.
        retValue = getIdealCameraResolution(minWidthPx, maxWidthPx);

        // Get actual display resolution
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        // Calculate scale based on height differences.
        final double scale = (double) displayMetrics.heightPixels / (double) retValue.height;

        // Get actual preview width after scaling
        final int scaledWidth = (int) ((double) retValue.width * scale);

        // Device might not support any ideal resolution.
        // In that case preview will be scaled vertically.
        // Do not allow menu to be smaller than configuration.
        final ViewGroup.LayoutParams layoutParams = mLayoutTutorial.getLayoutParams();
        final int space = displayMetrics.widthPixels - scaledWidth;
        layoutParams.width = Math.max(space, minWidthPx);
        mLayoutTutorial.setLayoutParams(layoutParams);

        return retValue;
    }

    protected void resizeCaptureFrame() {
        if (mCaptureFrame == null) {
            return;
        }

        double percentage;
        if (mLimitDetectionZone) {
            percentage = 0.72;
        } else {
            percentage = 0.85;
        }

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
    }

    //endregion

    //region Private Helpers

    private Camera.Size getIdealCameraResolution(final int minWidth, final int maxWidth) {
        final List<Camera.Size> resList = Camera.open().getParameters().getSupportedPreviewSizes();
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        // resList is ordered from highest / best resolution supported.
        for (final Camera.Size loopSize : resList) {
            // Calculate scale based on height differences.
            final double scale = (double) displayMetrics.heightPixels / (double) loopSize.height;
            // Get actual preview width after scaling
            final int scaledWidth = (int) ((double) loopSize.width * scale);

            // Preview aspect ratio allow to fit require size.
            final int space = displayMetrics.widthPixels - scaledWidth;
            if (space > minWidth && space < maxWidth) {
                return loopSize;
            }
        }

        // There is no ideal aspect to fit both. Simple return best resolution possible.
        return resList.get(0);
    }

    //endregion
}
