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
import android.widget.LinearLayout;
import android.widget.RadioButton;

import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.helpers.AbstractOption;
import com.thalesgroup.kyc.idv.helpers.DataContainer;
import com.thalesgroup.kyc.idv.helpers.KYCManager;

public class FragmentOnboarding extends AbstractFragmentBase {

    //region Life Cycle
    RadioButton rbPassport;
    RadioButton rbId;
    RadioButton rbFaceYes;
    RadioButton rbFaceNo;

    LinearLayout llPassport;
    LinearLayout llId;
    LinearLayout llFaceYes;
    LinearLayout llFaceNo;


    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View retValue = inflater.inflate(R.layout.fragment_onboarding, container, false);

        rbPassport = retValue.findViewById(R.id.fragment_onboarding_doc_passport);
        rbId = retValue.findViewById(R.id.fragment_onboarding_doc_id);
        rbFaceYes = retValue.findViewById(R.id.fragment_onboarding_face_verif_yes);
        rbFaceNo = retValue.findViewById(R.id.fragment_onboarding_face_verif_no);

        llPassport = retValue.findViewById(R.id.fragment_onboarding_ico_1);
        llId = retValue.findViewById(R.id.fragment_onboarding_ico_2);
        llFaceYes = retValue.findViewById(R.id.fragment_onboarding_ico_3);
        llFaceNo = retValue.findViewById(R.id.fragment_onboarding_ico_4);

        // Animate caption and description
        long delay = KYCManager.animateViewWithDelay(retValue.findViewById(R.id.fragment_onboarding_caption), 0);
        KYCManager.animateViewWithDelay(retValue.findViewById(R.id.fragment_onboarding_text_doc), delay);
        delay = KYCManager.animateViewWithDelay(retValue.findViewById(R.id.fragment_onboarding_doc_type), delay);
        KYCManager.animateViewWithDelay(retValue.findViewById(R.id.fragment_onboarding_text_face), delay);
        KYCManager.animateViewWithDelay(retValue.findViewById(R.id.fragment_onboarding_face_verif), delay);

        rbPassport.setOnClickListener(view -> onRadioButtonPassport());
        rbId.setOnClickListener(view -> onRadioButtonId());
        rbFaceYes.setOnClickListener(view -> onRadioButtonFaceYes());
        rbFaceNo.setOnClickListener(view -> onRadioButtonFaceNo());

        llPassport.setOnClickListener(view -> onRadioButtonPassport());
        llId.setOnClickListener(view -> onRadioButtonId());
        llFaceYes.setOnClickListener(view -> onRadioButtonFaceYes());
        llFaceNo.setOnClickListener(view -> onRadioButtonFaceNo());

        if (KYCManager.getInstance().getDocType() == AbstractOption.DocumentType.Passport) {
            rbPassport.setChecked(true);
        }
        else {
            rbId.setChecked(true);
        }

        if (KYCManager.getInstance().isFacialRecognition()) {
            rbFaceYes.setChecked(true);
        }
        else {
            rbFaceNo.setChecked(true);
        }

        retValue.findViewById(R.id.fragment_onboarding_button_next).setOnClickListener(view -> onButtonPressedNext());

        DataContainer.instance().clearDocData();

        return retValue;
    }

    //endregion

    //region User Interface
    private void onRadioButtonPassport() {
        rbPassport.setChecked(true);
        rbId.setChecked(!rbPassport.isChecked());
    }

    private void onRadioButtonId() {
        rbId.setChecked(true);
        rbPassport.setChecked(!rbId.isChecked());
    }

    private void onRadioButtonFaceYes() {
        rbFaceYes.setChecked(true);
        rbFaceNo.setChecked(!rbFaceYes.isChecked());
    }

    private void onRadioButtonFaceNo() {
        rbFaceNo.setChecked(true);
        rbFaceYes.setChecked(!rbFaceNo.isChecked());
    }

    private void onButtonPressedNext() {
        KYCManager.getInstance().setFacialRecognition(rbFaceYes.isChecked());

        if (rbPassport.isChecked()) {
            KYCManager.getInstance().setDocType(AbstractOption.DocumentType.Passport);
        }
        else {
            KYCManager.getInstance().setDocType(AbstractOption.DocumentType.IdCard);
        }

        KYCManager.getInstance().setNfcMode(false);
        getMainActivity().displayFragment(new FragmentDocumentChip(), true, true);
    }

    //endregion
}
