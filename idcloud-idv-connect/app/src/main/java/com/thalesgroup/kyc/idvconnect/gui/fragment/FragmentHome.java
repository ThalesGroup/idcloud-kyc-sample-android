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
import android.widget.TextView;

import com.thalesgroup.kyc.idvconnect.R;
import com.thalesgroup.kyc.idvconnect.helpers.KYCManager;

/**
 * Home {@code Fragment}.
 */
public class FragmentHome extends AbstractFragmentBase {

    private Button mButtonNext;
    private TextView mTextInit;

    //region Life Cycle

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View retValue = inflater.inflate(R.layout.fragment_home, container, false);

        mButtonNext = retValue.findViewById(R.id.fragment_home_button_next);
        mButtonNext.setOnClickListener(view -> onButtonClickNext());
        mTextInit = retValue.findViewById(R.id.fragment_home_text_init);

        return retValue;
    }

    @Override
    public void onStart() {
        super.onStart();

        getMainActivity().enableDrawer(true);
    }

    @Override
    public void onPause() {
        super.onPause();

        getMainActivity().enableDrawer(false);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (KYCManager.getInstance().getJsonWebToken() != null) {
            mTextInit.setVisibility(View.GONE);
            mButtonNext.setText(R.string.fragment_home_button_enroll);
        } else {
            mTextInit.setVisibility(View.VISIBLE);
            mButtonNext.setText(R.string.fragment_home_button_scann);
        }
    }

    //endregion

    //region User Interface

    /**
     * On click listener for the next button.
     */
    private void onButtonClickNext() {
        if (KYCManager.getInstance().getJsonWebToken() != null) {
            getMainActivity().displayFragment(new FragmentFirstStep(), true, true);
        } else {
            KYCManager.getInstance().displayQRcodeScannerForInit();
        }
    }

    //endregion


}
