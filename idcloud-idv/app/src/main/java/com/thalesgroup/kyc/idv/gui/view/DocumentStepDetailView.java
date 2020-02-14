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
import android.os.Handler;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.gui.MainActivity;
import com.thalesgroup.kyc.idv.gui.animation.EaseInterpolators;
import com.thalesgroup.kyc.idv.gui.animation.FinishedOrCanceledListener;
import com.thalesgroup.kyc.idv.helpers.util.AssetHelper;

import androidx.annotation.StringRes;

public class DocumentStepDetailView extends LinearLayout {

    //region Definition

    private static final int AUTOHIDE_DELAY_MS = 3000;

    private final TextView mCaptionTop;
    private final TextView mCaptionBottom;
    private final ImageView mImage;
    private boolean mCanceled;

    private Handler mAutoHideHandler = null;

    public interface CompletionHandler {
        void onFinished();
    }

    //endregion


    //region Life Cycle

    public DocumentStepDetailView(final Context context,
                                  @StringRes final int captionTop,
                                  @StringRes final int captionBottom,
                                  final String iconName) {
        super(context);

        // Load visuals.
        inflate(getContext(), R.layout.view_document_step_detail, this);

        // Center in parent.
        setGravity(Gravity.CENTER);

        // Get and update captions
        mCaptionTop = findViewById(R.id.view_document_step_detail_caption_top);
        mCaptionBottom = findViewById(R.id.view_document_step_detail_caption_bottom);
        mCaptionTop.setText(captionTop);
        mCaptionBottom.setText(captionBottom);

        // Get and update icon.
        mImage = findViewById(R.id.view_document_step_detail_image);
        mImage.setImageBitmap(AssetHelper.getBitmapFromAsset(iconName, context));
    }

    @Override
    protected void finalize() throws Throwable {
        AssetHelper.cleanUpBitmapImageView(mImage);

        super.finalize();
    }

    //endregion

    //region Public API

    public void presentInGroupView(final ViewGroup parent,
                                   final ImageView overlay,
                                   final CompletionHandler handler) {
        parent.addView(this);

        // TODO: Read actual value. For demo purposes we know, that view is 240dp tall.
        final int viewHalfSize = Math.round(120.f * getResources().getDisplayMetrics().density);
        final float hiddenPosY = getResources().getDisplayMetrics().heightPixels * .5f + viewHalfSize;

        // Move view out of the screen (bottom side).
        mCanceled = false;
        mAutoHideHandler = null;
        setVisibility(VISIBLE);
        setTranslationY(hiddenPosY);
        animate().translationY(.0f)
                .setDuration(MainActivity.ANIM_DURATION_SLOW_MS)
                .setInterpolator(new EaseInterpolators.EaseInOut())
                .setListener(new FinishedOrCanceledListener(() -> {
                    // User canceled animation before it's finished.
                    if (mCanceled) {
                        hideView(hiddenPosY, overlay, handler);
                    } else {
                        mAutoHideHandler = new Handler();
                        mAutoHideHandler.postDelayed(() ->
                                hideView(hiddenPosY, overlay, handler), AUTOHIDE_DELAY_MS);
                    }
                }, false))
                .start();

        // Animate overlay with same progress
        if (overlay != null) {
            overlay.animate().alpha(.0f)
                    .setDuration(MainActivity.ANIM_DURATION_SLOW_MS)
                    .setInterpolator(new EaseInterpolators.EaseInOut()).start();
        }

        // Allow user to hide this view before AUTOHIDE_DELAY_MS by tapping on it.
        setOnClickListener(view -> {
            // Show animation is finished. Stop handler and go to hide directly.
            if (mAutoHideHandler != null) {
                mAutoHideHandler.removeCallbacksAndMessages(null);
                mAutoHideHandler = null;
                hideView(hiddenPosY, overlay, handler);
            } else {
                // This will trigger finish listener. In that case if it's canceled, we will move directly without timer.
                mCanceled = true;
                clearAnimation();
                animate().cancel();
            }
        });
    }

    //endregion

    //region Private Helpers

    private void hideView(final float hiddenPosY,
                          final ImageView overlay,
                          final CompletionHandler handler) {
        // Remove listener once view is going to hide.
        setOnClickListener(null);

        // Hide view and notify handler.
        animate().translationY(hiddenPosY)
                .setDuration(MainActivity.ANIM_DURATION_SLOW_MS)
                .setInterpolator(new EaseInterpolators.EaseInOut())
                .setListener(new FinishedOrCanceledListener(() -> ((ViewGroup) this.getParent()).removeView(this), true))
                .start();

        // Animate overlay with same progress
        if (overlay != null) {
            overlay.animate().alpha(1.f)
                    .setDuration(MainActivity.ANIM_DURATION_SLOW_MS)
                    .setInterpolator(new EaseInterpolators.EaseInOut()).start();
        }

        // Enable handler sooner than easing finished.
        new Handler().postDelayed(handler::onFinished, (long) (MainActivity.ANIM_DURATION_SLOW_MS * .5f));
    }

    //endregion
}
