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
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;

import com.bumptech.glide.Glide;
import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.helpers.DataContainer;
import com.thalesgroup.kyc.idv.helpers.KYCManager;

public class FragmentDocumentChip extends AbstractFragmentBase {

    //region Life Cycle

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View retValue = inflater.inflate(R.layout.fragment_document_chip, container, false);

        // Animate caption and description
        long delay = KYCManager.animateViewWithDelay(retValue.findViewById(R.id.fragment_doc_chip_caption), 0);
        delay = KYCManager.animateViewWithDelay(retValue.findViewById(R.id.fragment_doc_chip_description), delay);

        // Populate layout with all steps
        final LinearLayout layout = retValue.findViewById(R.id.fragment_doc_chip_layout);

        addGif(R.drawable.chip, layout);

        KYCManager.animateViewWithDelay(layout, delay);

        retValue.findViewById(R.id.fragment_doc_chip_button_yes).setOnClickListener(view -> onButtonPressedYes());
        retValue.findViewById(R.id.fragment_doc_chip_button_no).setOnClickListener(view -> onButtonPressedNo());

        DataContainer.instance().clearDocData();

        return retValue;
    }

    //endregion

    //region Private Helpers
    private long addGif(@DrawableRes final int gif,
                        final LinearLayout parent) {
        final View view = getLayoutInflater().inflate(R.layout.view_gif, null);
        final ImageView image = view.findViewById(R.id.view_gif_image_small);

        Glide.with(this).load(gif).into(image);
        parent.addView(view);

        return KYCManager.animateViewWithDelay(view, 0);
    }

    //endregion

    private void onButtonPressedYes() {
        KYCManager.getInstance().setNfcMode(true);
        getMainActivity().displayFragment(new FragmentOnboardingSteps(), true, true);
    }

    private void onButtonPressedNo() {
        KYCManager.getInstance().setNfcMode(false);
        getMainActivity().displayFragment(new FragmentOnboardingSteps(), true, true);
    }
}
