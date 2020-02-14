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
import android.graphics.Bitmap;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.gui.MainActivity;
import com.thalesgroup.kyc.idv.gui.animation.EaseInterpolators;
import com.thalesgroup.kyc.idv.gui.animation.FinishedOrCanceledListener;
import com.thalesgroup.kyc.idv.gui.animation.Rotate3dAnimation;
import com.thalesgroup.kyc.idv.helpers.util.AssetHelper;

import androidx.annotation.StringRes;

public class DocumentStepView extends LinearLayout {

    //region Definition

    private static final float ALPHA_HIGHLIGHTED = 1.f;
    private static final float ALPHA_NORMAL = .2f;

    private final TextView mCaption;
    private final ImageView mImage;
    private Bitmap mBitmap = null;

    //endregion

    //region Life Cycle

    public DocumentStepView(final Context context,
                            @StringRes final int caption,
                            final String iconName) {
        super(context);

        // Load visuals.
        inflate(getContext(), R.layout.view_document_step, this);

        // Make sure that all elements does have same layout weight.
        final LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        setLayoutParams(param);

        // Get and update caption
        mCaption = findViewById(R.id.view_document_step_caption);
        mCaption.setText(caption);

        // Get and update icon.
        mImage = findViewById(R.id.view_document_step_image);
        setImage(iconName, context);

        // By default view is not highlighted.
        highlight(false, false);
    }

    @Override
    protected void finalize() throws Throwable {
        AssetHelper.cleanUpBitmapImageView(mImage);

        super.finalize();
    }

    //endregion

    //region Public API

    public void highlightAndRotateToIcon(final Context context, final String iconName) {
        highlight(true, true, new FinishedOrCanceledListener.FinishedOrCanceledHandler() {
            @Override
            public void onFinished() {
                final Rotate3dAnimation rotate3dAnimation = new Rotate3dAnimation(0, 0, 0, -180, 0, 0);
                rotate3dAnimation.setDuration(3000);
                rotate3dAnimation.setFillAfter(true);
                rotate3dAnimation.setInterpolator(new EaseInterpolators.EaseInOut());
                rotate3dAnimation.setMiddleHandler(() -> {
                    setImage(iconName, context);
                });
                mImage.startAnimation(rotate3dAnimation);
            }
        });
    }

    public void highlight(final boolean highlight, final boolean animated) {
        highlight(highlight, animated, null);
    }

    //endregion

    //region Private helpers

    private void setImage(final String imageName, final Context context) {
        if (mBitmap != null) {
            mImage.setImageBitmap(null);
            mBitmap.recycle();
        }

        mBitmap = AssetHelper.getBitmapFromAsset(imageName, context);
        mImage.setImageBitmap(mBitmap);
    }


    private void highlight(final boolean highlight,
                           final boolean animated,
                           final FinishedOrCanceledListener.FinishedOrCanceledHandler handler) {
        final float destAlpha = highlight ? ALPHA_HIGHLIGHTED : ALPHA_NORMAL;

        if (animated) {
            animate().alpha(destAlpha)
                    .setDuration(MainActivity.ANIM_DURATION_SLOW_MS)
                    .setInterpolator(new EaseInterpolators.EaseInOut())
                    .setListener(handler != null ? new FinishedOrCanceledListener(handler, true) : null);
        } else {
            setAlpha(destAlpha);
        }
    }
    //endregion
}
