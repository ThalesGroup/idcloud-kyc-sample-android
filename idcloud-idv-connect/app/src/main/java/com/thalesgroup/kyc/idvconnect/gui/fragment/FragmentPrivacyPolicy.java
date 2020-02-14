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

import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thalesgroup.kyc.idvconnect.R;
import com.thalesgroup.kyc.idvconnect.helpers.Configuration;
import com.thalesgroup.kyc.idvconnect.helpers.KYCManager;

public class FragmentPrivacyPolicy extends AbstractFragmentBase {

    //region Life Cycle

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View retValue = inflater.inflate(R.layout.fragment_privacy_policy, container, false);
        final TextView buttonPP = retValue.findViewById(R.id.fragment_privacy_policy_button);
        final TextView buttonTU = retValue.findViewById(R.id.fragment_privacy_terms_of_use);


        // Animate caption and description
        long delay = KYCManager.animateViewWithDelay(retValue.findViewById(R.id.fragment_privacy_policy_caption), 0);
        delay = KYCManager.animateViewWithDelay(retValue.findViewById(R.id.fragment_privacy_policy_image), delay);
        delay = KYCManager.animateViewWithDelay(retValue.findViewById(R.id.fragment_privacy_policy_description), delay);
        delay = KYCManager.animateViewWithDelay(buttonPP, delay);
        KYCManager.animateViewWithDelay(buttonTU, delay);

        buttonPP.setPaintFlags(buttonPP.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        buttonPP.setOnClickListener(view -> onButtonPressedPrivacyPolicy());

        buttonTU.setPaintFlags(buttonTU.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        buttonTU.setOnClickListener(view -> onButtonPressedTermsAndConditions());

        return retValue;
    }

    //endregion

    //region  User Interface

    /**
     * On click listener for the privacy policy button.
     */
    private void onButtonPressedPrivacyPolicy() {
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Configuration.PRIVACY_POLICY_URL));
        startActivity(browserIntent);
    }

    /**
     * On click listener for the terms and conditions button.
     */
    private void onButtonPressedTermsAndConditions() {
        getMainActivity().displayFragment(new FragmentTermsAndConditions(), true, true);
    }

    //endregion
}
