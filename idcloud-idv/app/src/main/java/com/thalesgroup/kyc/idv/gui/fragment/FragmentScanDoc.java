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

package com.thalesgroup.kyc.idv.gui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.gui.MainActivity;
import com.thalesgroup.kyc.idv.helpers.AbstractOption;
import com.thalesgroup.kyc.idv.helpers.DataContainer;
import com.thalesgroup.kyc.idv.helpers.KYCManager;
import com.thalesgroup.kyc.idv.helpers.util.AssetHelper;

import java.util.Locale;

public class FragmentScanDoc extends AbstractFragmentBase {

    //region Definition

    private AbstractOption.DocumentType mDocumentType;
    private TextView mLabelCaption;
    private TextView mLabelDescriptionFirst;
    private TextView mLabelDescriptionSecond;
    private ImageView mImageFirst;
    private ImageView mImageSecond;

    //endregion

    //region Life Cycle

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        DataContainer.instance().clearDocData();

        final View retValue = inflater.inflate(R.layout.fragment_scan_doc, container, false);

        // Get all elements.
        mLabelCaption = retValue.findViewById(R.id.fragment_scan_doc_caption);
        mLabelDescriptionFirst = retValue.findViewById(R.id.fragment_scan_doc_description_01);
        mLabelDescriptionSecond = retValue.findViewById(R.id.fragment_scan_doc_description_02);
        mImageFirst = retValue.findViewById(R.id.fragment_scan_doc_image_01);
        mImageSecond = retValue.findViewById(R.id.fragment_scan_doc_image_02);
        final Button buttonNext = retValue.findViewById(R.id.fragment_scan_doc_button_next);

        // Get argument to determine document type.
        final Bundle arguments = getArguments();
        if (arguments != null) {
            mDocumentType = (AbstractOption.DocumentType) arguments.getSerializable(MainActivity.BUNDLE_ARGUMENT_DOC_TYPE);
        }

        // Update caption and pictures based on type.
        mLabelCaption.setText(caption());
        mLabelDescriptionFirst.setText(labelForIndex(1));
        mLabelDescriptionSecond.setText(labelForIndex(2));
        mImageFirst.setImageBitmap(AssetHelper.getBitmapFromAsset(imageForIndex(1), inflater.getContext()));
        mImageSecond.setImageBitmap(AssetHelper.getBitmapFromAsset(imageForIndex(2), inflater.getContext()));

        // Animate caption and description.
        long delay = KYCManager.animateViewWithDelay(mLabelCaption, 0);
        delay = KYCManager.animateViewWithDelay(mLabelDescriptionFirst, delay);
        delay = KYCManager.animateViewWithDelay(mImageFirst, delay);
        delay = KYCManager.animateViewWithDelay(mLabelDescriptionSecond, delay);
        KYCManager.animateViewWithDelay(mImageSecond, delay);
        KYCManager.animateViewWithDelay(buttonNext, 0);

        // User Interface
        buttonNext.setOnClickListener(view -> onButtonPressedNext());

        DataContainer.instance().clearDocData();

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

    private int caption() {
        if (KYCManager.getInstance().isNfcMode()) {
            if (mDocumentType == AbstractOption.DocumentType.Passport) {
                return R.string.STRING_KYC_MRZ_SCAN_CAPTION_PASSPORT;
            } else {
                return R.string.STRING_KYC_MRZ_SCAN_CAPTION_IDCARD;
            }
        }
        else {
            if (mDocumentType == AbstractOption.DocumentType.Passport) {
                return R.string.STRING_KYC_DOC_SCAN_CAPTION_PASSPORT;
            } else {
                return R.string.STRING_KYC_DOC_SCAN_CAPTION_IDCARD;
            }
        }
    }

    private int labelForIndex(final int index) {
        String stringKey = String.format(Locale.ENGLISH,"STRING_KYC_DOC_SCAN_%02d", index);

        if (KYCManager.getInstance().isNfcMode()) {
            stringKey = String.format(Locale.ENGLISH,"STRING_KYC_MRZ_SCAN_%02d", index);
        }

        return KYCManager.getResId(stringKey, R.string.class);
    }

    private String imageForIndex(final int index) {
        final String type = mDocumentType == AbstractOption.DocumentType.Passport ? "passport" : "id_card";

        if (KYCManager.getInstance().isNfcMode()) {
            return String.format(Locale.ENGLISH,"kyc_scan_doc_%s_%02d.png", "mrz", index);
        }

        return String.format(Locale.ENGLISH,"kyc_scan_doc_%s_%02d.png", type, index);
    }

    //endregion


    //region User Interface

    private void onButtonPressedNext() {
        getMainActivity().openDocScanActivity(mDocumentType);
    }

    //endregion
}
