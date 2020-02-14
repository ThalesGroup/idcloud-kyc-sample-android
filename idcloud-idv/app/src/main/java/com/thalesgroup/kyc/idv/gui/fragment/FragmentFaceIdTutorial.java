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
import android.widget.ImageView;

import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.helpers.util.AssetHelper;

public class FragmentFaceIdTutorial extends AbstractFragmentBase {

    //region Definition

    private ImageView mImageSelfieBad = null;
    private ImageView mImageSelfieGood = null;
    private ImageView mImageSelfieTutorial = null;

    //endregion

    //region Life Cycle

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View retValue = inflater.inflate(R.layout.fragment_face_id_tutorial, container, false);

        mImageSelfieBad = retValue.findViewById(R.id.fragment_face_id_tutorial_bad_selfie);
        mImageSelfieBad.setImageBitmap(AssetHelper.getBitmapFromAsset(AssetHelper.ASSET_HOW_TO_SELFIE_BAD, getContext()));

        mImageSelfieGood = retValue.findViewById(R.id.fragment_face_id_tutorial_good_selfie);
        mImageSelfieGood.setImageBitmap(AssetHelper.getBitmapFromAsset(AssetHelper.ASSET_HOW_TO_SELFIE_GOOD, getContext()));

        mImageSelfieTutorial = retValue.findViewById(R.id.fragment_face_id_tutorial_tutorial);
        mImageSelfieTutorial.setImageBitmap(AssetHelper.getBitmapFromAsset(AssetHelper.ASSET_HOW_TO_SELFIE_TUTORIAL, getContext()));

        retValue.findViewById(R.id.fragment_face_id_tutorial_button_next).setOnClickListener(view -> {
            getMainActivity().openFaceScanActivity();
        });

        return retValue;
    }

    @Override
    protected void finalize() throws Throwable {
        // Make sure we will release all bitmaps properly to same some memory.
        AssetHelper.cleanUpBitmapImageView(mImageSelfieBad);
        AssetHelper.cleanUpBitmapImageView(mImageSelfieGood);
        AssetHelper.cleanUpBitmapImageView(mImageSelfieTutorial);

        super.finalize();
    }

    //endregion

}
