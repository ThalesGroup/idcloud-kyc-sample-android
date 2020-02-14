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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.helpers.DataContainer;
import com.thalesgroup.kyc.idv.helpers.communication.KYCCommunication;
import com.thalesgroup.kyc.idv.helpers.communication.KYCFailedVerification;
import com.thalesgroup.kyc.idv.helpers.communication.KYCResponse;
import com.thalesgroup.kyc.idv.helpers.communication.KYCSession;

import androidx.fragment.app.FragmentManager;

/**
 * Overview {@Fragment} for submitting the scan results to the backend.
 */
public class FragmentKycOverview extends AbstractFragmentBase {

    //region Define

    // Top part with images.
    private LinearLayout mLayoutSelfie;
    private ImageView mIvSelfie;
    private ImageView mIvSelfieExtracted;

    // Result part.
    private LinearLayout mLayoutUserInfo;
    private TextView mTvResultHeader;
    private TextView mTvResultCaption;
    private TextView mTvResultValue;
    private ImageView mIvResultIcon;

    private Button mButtonNext;
    private KYCCommunication mKYCCommunication;

    //endregion

    //region Life Cycle

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View retValue = inflater.inflate(R.layout.fragment_kyc_overview, container, false);

        // Find all UI components
        mLayoutSelfie = retValue.findViewById(R.id.fragment_kyc_overview_layout_selfie);
        mIvSelfie = retValue.findViewById(R.id.fragment_kyc_overview_iv_selfie);
        mIvSelfieExtracted = retValue.findViewById(R.id.fragment_kyc_overview_iv_selfie_extracted);
        final ImageView ivDocFront = retValue.findViewById(R.id.fragment_kyc_overview_iv_doc_front);
        final ImageView ivDocBack = retValue.findViewById(R.id.fragment_kyc_overview_iv_doc_back);
        mButtonNext = retValue.findViewById(R.id.fragment_kyc_overview_button_next);
        mLayoutUserInfo = retValue.findViewById(R.id.fragment_kyc_overview_layout_user_info);
        mTvResultHeader = retValue.findViewById(R.id.fragment_kyc_overview_tv_status);
        mTvResultCaption = retValue.findViewById(R.id.fragment_kyc_overview_tv_user_info_caption);
        mTvResultValue = retValue.findViewById(R.id.fragment_kyc_overview_tv_user_info_value);
        mIvResultIcon = retValue.findViewById(R.id.fragment_kyc_overview_iv_progress);

        // Display current data.
        final byte[] selfie = DataContainer.instance().mSelfie;
        showBitmap(selfie, mIvSelfie);
        showBitmap(DataContainer.instance().mDocFront, ivDocFront);
        showBitmap(DataContainer.instance().mDocBack, ivDocBack);

        // Hide selfie part if it's not present.
        mLayoutSelfie.setVisibility(selfie != null ? View.VISIBLE : View.GONE);

        // Hide bottom part with parsed data from server.
        mLayoutUserInfo.setVisibility(View.GONE);

        // Update user interface.
        mButtonNext.setOnClickListener(view -> onButtonClickSubmit());

        retValue.setFocusableInTouchMode(true);
        retValue.requestFocus();
        retValue.setOnKeyListener((view, keyCode, keyEvent) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.getAction() == KeyEvent.ACTION_UP ) {
                getMainActivity().progressBarHide();
                if (mKYCCommunication != null) {
                    mKYCCommunication.removeListener();
                }
            }

            return false;
        });

        return retValue;
    }

    //endregion


    //region Private Hepers

    /**
     * Re sizes and displays an image.
     *
     * @param data Image in form of a byte array.
     * @param imageView {@code ImageView} in which to display the image.
     */
    private void showBitmap(final byte[] data,
                            final ImageView imageView) {
        if (data != null) {
            final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            final DisplayMetrics displayMetrics = new DisplayMetrics();
            getMainActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            imageView.setMinimumHeight(displayMetrics.heightPixels);
            imageView.setMinimumWidth(displayMetrics.widthPixels);
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setVisibility(View.GONE);
        }
    }

    /**
     * Formats the result {@code String}.
     *
     * @param retCaption Caption.
     * @param retValue Return value.
     * @param caption Caption.
     * @param value Value.
     */
    private void appendResultString(final StringBuilder retCaption,
                                    final StringBuilder retValue,
                                    final String caption,
                                    final String value) {
        if (value != null && !"null".equals(value) && !value.isEmpty()) {
            retCaption.append(caption).append(": \n");
            retValue.append(value).append('\n');
        }
    }

    /**
     * Formats the result {@code String}.
     *
     * @param retCaption Caption.
     * @param retValue Return value.
     * @param caption Caption.
     * @param value Value.
     */
    private void appendResultInt(final StringBuilder retCaption,
                                 final StringBuilder retValue,
                                 final String caption,
                                 final int value) {
        retCaption.append(caption).append(": \n");
        retValue.append(value).append('\n');
    }

    /**
     * Displays a result.
     *
     * @param result Result.
     */
    private void displayResult(final KYCResponse result) {
        // Make sure that response is valid and positive.
        if (result == null || result.getDocument() == null) {
            displayError("Failed to get valid response from server.", null);
            return;
        } else if (!result.getDocument().getResult().equalsIgnoreCase("SUCCESS")) {
            displayError(result.getMessageReadable(), result);
            return;
        }

        // Build user information strings.
        final StringBuilder caption = new StringBuilder();
        final StringBuilder value = new StringBuilder();

        if (result.getDocument().getFirstName() != null && result.getDocument().getSurname() != null) {
            caption.append(getString(R.string.kyc_result_name_surname)).append('\n');
            value.append(result.getDocument().getFirstName()).append(' ').append(result.getDocument().getSurname()).append('\n');
        }
        appendResultString(caption, value, getString(R.string.kyc_result_gender), result.getDocument().getGender());
        appendResultString(caption, value, getString(R.string.kyc_result_nationality), result.getDocument().getNationality());
        appendResultString(caption, value, getString(R.string.kyc_result_expiry_date), result.getDocument().getExpiryDate());
        appendResultString(caption, value, getString(R.string.kyc_result_birth_date), result.getDocument().getBirthDate());
        appendResultString(caption, value, getString(R.string.kyc_result_doc_number), result.getDocument().getDocumentNumber());
        appendResultString(caption, value, getString(R.string.kyc_result_doc_type), result.getDocument().getDocumentType());
        appendResultInt(caption, value, getString(R.string.kyc_result_verification_count), result.getDocument().getTotalVerificationsDone());

        // Show / Hide whole section
        mLayoutUserInfo.setVisibility(caption.length() > 0 ? View.VISIBLE : View.GONE);
        mTvResultCaption.setText(caption.toString());
        mTvResultValue.setText(value.toString());

        // Extracted selfie
        if (result.getDocument().getPortrait() != null) {
            mIvSelfieExtracted.setVisibility(View.VISIBLE);
            showBitmap(result.getDocument().getPortrait(), mIvSelfieExtracted);

            // Add margin to left photo. It was not there from beginning since there
            // was just one image.
            final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mIvSelfie.getLayoutParams();
            params.setMargins(0, 0, 8, 0);
            mIvSelfie.setLayoutParams(params);

            // Selfie part might be hidden for document scan only, but we sill have extracted image.
            mLayoutSelfie.setVisibility(View.VISIBLE);
        }

        // Update status header + icon.
        mTvResultHeader.setText(result.getMessageReadable());
        mIvResultIcon.setImageDrawable(getResources().getDrawable(R.drawable.passed));

        // Update button function
        mButtonNext.setText(R.string.fragment_kyc_overview_button_done);
        mButtonNext.setOnClickListener(view -> onButtonClickDone());
    }

    /**
     * Displays a error.
     *
     * @param error Error.
     */
    private void displayError(final String error, final KYCResponse response) {
        // Append detail information about failed check if available.
        final StringBuilder fullErr = new StringBuilder(error);
        if (response != null && !response.getDocument().getFailedVerifications().isEmpty()) {
            fullErr.append('\n');
            for (final KYCFailedVerification loopVerify : response.getDocument().getFailedVerifications()) {
                fullErr.append(loopVerify.getName()).append(", ");
            }
            fullErr.delete(fullErr.length() - 2, fullErr.length());
        }

        mTvResultHeader.setText(fullErr);
        mIvResultIcon.setImageDrawable(getResources().getDrawable(R.drawable.error));
    }

    //endregion


    //region User Interface

    /**
     * On click listener for the done button.
     */
    private void onButtonClickSubmit() {
        // Show loading progress bar during asynchronous operation.
        getMainActivity().progressBarShow();

        // Send data to server and wait for response.
        mKYCCommunication = new KYCCommunication();
        mKYCCommunication.verifyDocument(DataContainer.instance().mDocFront,
                                         DataContainer.instance().mDocBack,
                                         DataContainer.instance().mSelfie,
                                         new KYCSession.KYCResponseHandler() {
                                             @Override
                                             public void onSuccess(final KYCResponse response) {
                                                 // Operation finished. We can hide progress bar.
                                                 getMainActivity().progressBarHide();
                                                 // Update UI with values from response.
                                                 displayResult(response);
                                             }

                                             @Override
                                             public void onFailure(final String error) {
                                                 // Operation finished. We can hide progress bar.
                                                 getMainActivity().progressBarHide();
                                                 // Display issue description.
                                                 displayError(error, null);
                                             }
                                         });
    }

    /**
     * On click listener for the done button.
     */
    private void onButtonClickDone() {
        // Close all fragments
        getMainActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    //endregion


}
