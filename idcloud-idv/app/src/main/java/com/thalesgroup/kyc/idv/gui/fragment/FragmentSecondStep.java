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

import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.gui.MainActivity;
import com.thalesgroup.kyc.idv.helpers.AbstractOption;
import com.thalesgroup.kyc.idv.helpers.KYCManager;

public class FragmentSecondStep extends AbstractFragmentBase {

    //region Life Cycle

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View retValue = inflater.inflate(R.layout.fragment_second_step, container, false);

        // Animate caption and description
        long delay = KYCManager
                .animateViewWithDelay(retValue.findViewById(R.id.fragment_second_step_caption), 0);
        delay = KYCManager.animateViewWithDelay(retValue.findViewById(R.id.fragment_second_step_button_id_card), delay);
        KYCManager.animateViewWithDelay(retValue.findViewById(R.id.fragment_second_step_button_passport), delay);

        retValue.findViewById(R.id.fragment_second_step_button_id_card).setOnClickListener(view -> onButtonPressedIdCard());
        retValue.findViewById(R.id.fragment_second_step_button_passport).setOnClickListener(view -> onButtonPressedPassport());

        return retValue;
    }

    //endregion

    //region Private Helpers

    private void displayThirsStep(final AbstractOption.DocumentType type) {
        final Bundle args = new Bundle();
        args.putSerializable(MainActivity.BUNDLE_ARGUMENT_DOC_TYPE, type);

        final FragmentThirdStep newFragment = new FragmentThirdStep();
        newFragment.setArguments(args);
        getMainActivity().displayFragment(newFragment, true, true);
    }

    //endregion


    //region User Interface

    private void onButtonPressedIdCard() {
        displayThirsStep(AbstractOption.DocumentType.IdCard);
    }

    private void onButtonPressedPassport() {
        displayThirsStep(AbstractOption.DocumentType.Passport);
    }

    //endregion
}
