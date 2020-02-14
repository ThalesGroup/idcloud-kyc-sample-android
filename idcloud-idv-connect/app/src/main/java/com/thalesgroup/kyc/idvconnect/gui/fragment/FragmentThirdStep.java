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

package com.thalesgroup.kyc.idvconnect.gui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.thalesgroup.kyc.idvconnect.R;
import com.thalesgroup.kyc.idvconnect.gui.MainActivity;
import com.thalesgroup.kyc.idvconnect.helpers.AbstractOption;
import com.thalesgroup.kyc.idvconnect.helpers.KYCManager;
import com.thalesgroup.kyc.idvconnect.helpers.util.AssetHelper;

import java.util.Locale;

/**
 * {@code Fragment} for the third step - start scanning of the selected document type.
 */
public class FragmentThirdStep extends AbstractFragmentBase {

    //region Definition

    private AbstractOption.DocumentType mDocumentType;
    private ImageView mImageFirst;
    private ImageView mImageSecond;

    //endregion

    //region Life Cycle

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View retValue = inflater.inflate(R.layout.fragment_third_step, container, false);

        // Get all elements.
        final TextView labelCaption = retValue.findViewById(R.id.fragment_third_step_caption);
        final TextView labelDescriptionFirst = retValue.findViewById(R.id.fragment_third_step_description_01);
        final TextView labelDescriptionSecond = retValue.findViewById(R.id.fragment_third_step_description_02);
        mImageFirst = retValue.findViewById(R.id.fragment_third_step_image_01);
        mImageSecond = retValue.findViewById(R.id.fragment_third_step_image_02);
        final Button buttonNext = retValue.findViewById(R.id.fragment_third_step_button_next);

        // Get argument to determine document type.
        final Bundle arguments = getArguments();
        if (arguments != null) {
            mDocumentType = (AbstractOption.DocumentType) arguments.getSerializable(MainActivity.BUNDLE_ARGUMENT_DOC_TYPE);
        }

        // Update caption and pictures based on type.
        labelCaption.setText(caption());
        labelDescriptionFirst.setText(labelForIndex(1));
        labelDescriptionSecond.setText(labelForIndex(2));
        mImageFirst.setImageBitmap(AssetHelper.getBitmapFromAsset(imageForIndex(1), inflater.getContext()));
        mImageSecond.setImageBitmap(AssetHelper.getBitmapFromAsset(imageForIndex(2), inflater.getContext()));

        // Animate caption and description.
        long delay = KYCManager.animateViewWithDelay(labelCaption, 0);
        delay = KYCManager.animateViewWithDelay(labelDescriptionFirst, delay);
        delay = KYCManager.animateViewWithDelay(mImageFirst, delay);
        delay = KYCManager.animateViewWithDelay(labelDescriptionSecond, delay);
        KYCManager.animateViewWithDelay(mImageSecond, delay);
        KYCManager.animateViewWithDelay(buttonNext, 0);

        // User Interface
        buttonNext.setOnClickListener(view -> onButtonPressedNext());

        return retValue;
    }

    @Override
    public void onDestroy() {
        AssetHelper.cleanUpBitmapImageView(mImageFirst);
        AssetHelper.cleanUpBitmapImageView(mImageSecond);

        super.onDestroy();
    }

    //endregion

    //region Private Helpers

    /**
     * Returns the caption based on the selected document type.
     *
     * @return Caption based on the selected document type.
     */
    private int caption() {
        if (mDocumentType == AbstractOption.DocumentType.Passport) {
            return R.string.STRING_KYC_DOC_SCAN_CAPTION_PASSPORT;
        } else {
            return R.string.STRING_KYC_DOC_SCAN_CAPTION_IDCARD;
        }
    }

    /**
     * Returns the asset based on the index.
     *
     * @param index Index.
     *
     * @return Asset.
     */
    private int labelForIndex(final int index) {
        final boolean automatic = KYCManager.getInstance().isAutomaticTypeDetection();
        final String mode = automatic ? "AUTO" : "MANUAL";
        final String stringKey = String.format(Locale.ENGLISH,"STRING_KYC_DOC_SCAN_%02d_%s", index, mode);

        return KYCManager.getResId(stringKey, R.string.class);
    }

    /**
     * Returns the image name based on the selected document type.
     *
     * @param index Index.
     *
     * @return Image name.
     */
    private String imageForIndex(final int index) {
        final boolean automatic = KYCManager.getInstance().isAutomaticTypeDetection();
        final String type = mDocumentType == AbstractOption.DocumentType.Passport ? "passport" : "id_card";
        final String mode = automatic ? "auto" : "manual";

        return String.format(Locale.ENGLISH,"kyc_third_step_%s_%02d_%s.png", type, index, mode);
    }

    //endregion


    //region User Interface

    /**
     * On click listener for the next button.
     */
    private void onButtonPressedNext() {
        getMainActivity().scanFrontSide(mDocumentType);
    }

    //endregion
}
