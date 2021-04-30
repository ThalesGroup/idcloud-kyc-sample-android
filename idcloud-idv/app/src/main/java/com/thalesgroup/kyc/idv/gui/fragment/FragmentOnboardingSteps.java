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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.gui.MainActivity;
import com.thalesgroup.kyc.idv.helpers.DataContainer;
import com.thalesgroup.kyc.idv.helpers.KYCManager;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

public class FragmentOnboardingSteps extends AbstractFragmentBase {

    //region Life Cycle

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View retValue = inflater.inflate(R.layout.fragment_onboarding_steps, container, false);

        // Animate caption and description
        long delay = KYCManager
                .animateViewWithDelay(retValue.findViewById(R.id.fragment_onboarding_steps_caption), 0);
        delay = KYCManager.animateViewWithDelay(retValue.findViewById(R.id.fragment_onboarding_steps_description), delay);

        // Populate layout with all steps
        final LinearLayout layout = retValue.findViewById(R.id.fragment_onboarding_steps_layout);

        if (KYCManager.getInstance().isNfcMode()) {
            delay = addChevron(R.drawable.chevron_id, R.string.STRING_KYC_ONBOARDING_STEPS_MRZ, layout, delay);
            delay = addChevron(R.drawable.chevron_nfc, R.string.STRING_KYC_ONBOARDING_STEPS_NFC, layout, delay);
        }
        else {
            delay = addChevron(R.drawable.chevron_id, R.string.STRING_KYC_ONBOARDING_STEPS_ID, layout, delay);
        }

        if (KYCManager.getInstance().isFacialRecognition()) {
            delay = addChevron(R.drawable.chevron_identity, R.string.STRING_KYC_ONBOARDING_STEPS_FACE, layout, delay);
        }
        addChevron(R.drawable.chevron_review, R.string.STRING_KYC_ONBOARDING_STEPS_REVIEW, layout, delay);

        ((Button)retValue.findViewById(R.id.fragment_onboarding_steps_button_next)).setText(R.string.button_next);

        KYCManager.animateViewWithDelay(retValue.findViewById(R.id.fragment_onboarding_steps_button_next), 0);
        retValue.findViewById(R.id.fragment_onboarding_steps_button_next).setOnClickListener(view -> onButtonPressedNext());

        DataContainer.instance().clearDocData();

        return retValue;
    }

    //endregion

    //region Private Helpers

    private long addChevron(@DrawableRes final int icon,
                            @StringRes final int caption,
                            final LinearLayout parent,
                            final long delay) {
        final View view = getLayoutInflater().inflate(R.layout.view_step, null);
        final ImageView image = view.findViewById(R.id.view_step_image);
        final TextView text = view.findViewById(R.id.view_step_text);

        image.setImageDrawable(getContext().getDrawable(icon));
        text.setText(caption);
        parent.addView(view);

        return KYCManager.animateViewWithDelay(view, delay);
    }

    //endregion

    private void onButtonPressedNext() {
        if (KYCManager.getInstance().isNfcMode()) {
            getMainActivity().displayFragment(new FragmentDocumentMrz(), true, true);
        }
        else {
            final Bundle args = new Bundle();
            args.putSerializable(MainActivity.BUNDLE_ARGUMENT_DOC_TYPE, KYCManager.getInstance().getDocType());

            final FragmentScanDoc newFragment = new FragmentScanDoc();
            newFragment.setArguments(args);

            getMainActivity().displayFragment(newFragment, true, true);
        }
    }
}
