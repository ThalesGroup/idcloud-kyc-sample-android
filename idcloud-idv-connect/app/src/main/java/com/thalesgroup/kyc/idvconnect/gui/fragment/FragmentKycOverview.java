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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.thalesgroup.kyc.idvconnect.R;
import com.thalesgroup.kyc.idvconnect.helpers.DataContainer;
import com.thalesgroup.kyc.idvconnect.helpers.KYCManager;
import com.thalesgroup.kyc.idvconnect.helpers.communication.KYCCommunication;
import com.thalesgroup.kyc.idvconnect.helpers.communication.KYCSession;
import com.thalesgroup.kyc.idvconnect.helpers.communication.structures.KYCResponse;
import com.thalesgroup.kyc.idvconnect.helpers.communication.structures.KycAlert;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

/**
 * Overview {@Fragment} for submitting the scan results to the backend.
 */
public class FragmentKycOverview extends AbstractFragmentBase {

    //region Definition

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

    private LinearLayout mLayoutCheckEPL;
    private LinearLayout mLayoutCheckPL;
    private LinearLayout mLayoutCheckAL;
    private LinearLayout mLayoutCheckFace;

    private Button mButtonNext;
    private KYCCommunication mKYCCommunication;

    private View mRetValue;
    private int mRetryStep;

    //endregion

    //region Life Cycle

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        mRetValue = inflater.inflate(R.layout.fragment_kyc_overview, container, false);

        // Find all UI components
        mLayoutSelfie = mRetValue.findViewById(R.id.fragment_kyc_overview_layout_selfie);
        mIvSelfie = mRetValue.findViewById(R.id.fragment_kyc_overview_iv_selfie);
        mIvSelfieExtracted = mRetValue.findViewById(R.id.fragment_kyc_overview_iv_selfie_extracted);
        final ImageView ivDocFront = mRetValue.findViewById(R.id.fragment_kyc_overview_iv_doc_front);
        final ImageView ivDocBack = mRetValue.findViewById(R.id.fragment_kyc_overview_iv_doc_back);
        mButtonNext = mRetValue.findViewById(R.id.fragment_kyc_overview_button_next);
        mLayoutUserInfo = mRetValue.findViewById(R.id.fragment_kyc_overview_layout_user_info);
        mTvResultHeader = mRetValue.findViewById(R.id.fragment_kyc_overview_tv_status);
        mTvResultCaption = mRetValue.findViewById(R.id.fragment_kyc_overview_tv_user_info_caption);
        mTvResultValue = mRetValue.findViewById(R.id.fragment_kyc_overview_tv_user_info_value);
        mIvResultIcon = mRetValue.findViewById(R.id.fragment_kyc_overview_iv_progress);

        mLayoutCheckEPL = mRetValue.findViewById(R.id.fragment_kyc_overview_layout_check_epl);
        mLayoutCheckPL = mRetValue.findViewById(R.id.fragment_kyc_overview_layout_check_pl);
        mLayoutCheckAL = mRetValue.findViewById(R.id.fragment_kyc_overview_layout_check_al);
        mLayoutCheckFace = mRetValue.findViewById(R.id.fragment_kyc_overview_layout_check_face);

        // Display current data.
        final byte[] selfie = DataContainer.instance().mSelfie;
        showBitmap(selfie, mIvSelfie);
        showBitmap(DataContainer.instance().mDocFront, ivDocFront);
        showBitmap(DataContainer.instance().mDocBack, ivDocBack);

        // Hide selfie part if it's not present.
        mLayoutSelfie.setVisibility(selfie != null ? View.VISIBLE : View.GONE);

        // Hide bottom part with parsed data from server.
        mLayoutUserInfo.setVisibility(View.GONE);

        // Hide Checks
        mLayoutCheckEPL.setVisibility(View.GONE);
        mLayoutCheckPL.setVisibility(View.GONE);
        mLayoutCheckAL.setVisibility(View.GONE);
        mLayoutCheckFace.setVisibility(View.GONE);

        // Update user interface.
        mButtonNext.setOnClickListener(view -> onButtonClickSubmit());

        mRetValue.setFocusableInTouchMode(true);
        mRetValue.requestFocus();
        mRetValue.setOnKeyListener((view, keyCode, keyEvent) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.getAction() == KeyEvent.ACTION_UP ) {
                getMainActivity().progressBarHide();
                if (mKYCCommunication != null) {
                    mKYCCommunication.removeListener();
                }
            }

            return false;
        });

        return mRetValue;
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
            retCaption.append("\n").append(caption).append(":");
            retValue.append("\n").append(value);
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
        retCaption.append("\n").append(caption).append(":");
        retValue.append("\n").append(value);
    }

/**
 * Displays a result.
 *
 * @param result Result.
 */
    private void displayResult(final KYCResponse result) {
        mRetryStep = KYCSession.RETRY_NONE;

        // Make sure that response is valid and positive.
        if (result == null || result.getDocument() == null) {
            displayError("Failed to get valid response from server.", null, KYCSession.RETRY_NONE);
            return;
        }
        else if (result.getDocument().getKycVerificationResult().getResult().equalsIgnoreCase("Unknown")) {
            displayError(KYCManager.getInstance().getErrorMessage(String.valueOf("5301"), null) + "\n" + getString(R.string.try_again), null, KYCSession.RETRY_DOC_SCAN);
            return;
        }
        else if ((result.getFace() != null) && (result.getFace().getResult().equalsIgnoreCase("FACE_NOT_MATCH"))) {
            displayError(KYCManager.getInstance().getErrorMessage(String.valueOf("9903"), null) + "\n" + getString(R.string.try_again), null, KYCSession.RETRY_SELFIE_SCAN);
            return;
        }
        else if (  (!result.getDocument().getKycVerificationResult().getResult().equalsIgnoreCase("Passed"))
                 &&(!(  (result.getDocument().getKycVerificationResult().getResult().equalsIgnoreCase("Attention"))
                      ||(result.getDocument().getKycVerificationResult().getResult().equalsIgnoreCase("Failed"))
                     )
                  )){
            displayError(result.getMessageReadable(), result, KYCSession.RETRY_NONE);
            return;
        }

        // Build user information strings.
        final StringBuilder caption = new StringBuilder();
        final StringBuilder value = new StringBuilder();

        if (  (result.getDocument().getKycVerificationResult().getFirstName() != null)
            &&(result.getDocument().getKycVerificationResult().getSurname() != null)
            &&(!result.getDocument().getKycVerificationResult().getFirstName().equals(""))
            &&(!result.getDocument().getKycVerificationResult().getSurname().equals(""))
           ) {
            caption.append(getString(R.string.kyc_result_name_surname));
            if (result.getDocument().getKycVerificationResult().getFirstName().length() >= 16) {
                caption.append('\n');
            }
            if (result.getDocument().getKycVerificationResult().getSurname().length() >= 16) {
                caption.append('\n');
            }
            value.append(result.getDocument().getKycVerificationResult().getFirstName())
                 .append(' ')
                 .append(result.getDocument().getKycVerificationResult().getSurname());
        }
        appendResultString(caption, value, getString(R.string.kyc_result_gender), result.getDocument().getKycVerificationResult().getGender());
        appendResultString(caption, value, getString(R.string.kyc_result_nationality), result.getDocument().getKycVerificationResult().getNationality());
        appendResultString(caption, value, getString(R.string.kyc_result_expiry_date), result.getDocument().getKycVerificationResult().getExpirationDate());
        appendResultString(caption, value, getString(R.string.kyc_result_birth_date), result.getDocument().getKycVerificationResult().getBirthDate());
        appendResultString(caption, value, getString(R.string.kyc_result_doc_number), result.getDocument().getKycVerificationResult().getDocumentNumber());
        appendResultString(caption, value, getString(R.string.kyc_result_doc_type), result.getDocument().getKycVerificationResult().getDocumentType());
        appendResultInt(caption, value, getString(R.string.kyc_result_verification_count), result.getDocument().getKycVerificationResult().getTotalVerificationsDone());
        appendResultInt(caption, value, getString(R.string.fragment_kyc_alerts), result.getDocument().getKycVerificationResult().getAlerts() != null ? result.getDocument().getKycVerificationResult().getAlerts().size() : 0);

        // Face Verification Result?
        if (result.getFace() != null) {
            appendResultInt(caption, value, getString(R.string.kyc_result_face_score), result.getFace().getScore());
        }

        // Passive Liveness Result?
        if (result.getLivenessResult() != null) {
            appendResultInt(caption, value, getString(R.string.kyc_result_liveness_score), result.getLivenessResult().getScore());
        }

        // Enhanced Passive Liveness Result?
        if (result.getEnhancedLivenessResult() != null) {
            appendResultInt(caption, value, getString(R.string.kyc_result_liveness_score), result.getEnhancedLivenessResult().getScore());
        }

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
        String docRes = getString(R.string.fragment_kyc_doc_status) + " " + result.getDocument().getKycVerificationResult().getResult();
        mTvResultHeader.setText(docRes);
        mIvResultIcon.setVisibility(View.GONE);

        // -------------
        // Update checks
        // -------------
        if (KYCManager.getInstance().isFacialRecognition()) {
            if (KYCManager.getInstance().getFaceLivenessMode().equals(getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_ENHANCED_PASSIVE))) {
                mLayoutCheckEPL.setVisibility(View.VISIBLE);
                if (result.getEnhancedLivenessResult().getScore() < 80) {
                    ((ImageView)mRetValue.findViewById(R.id.fragment_kyc_overview_icon_check_epl)).setImageResource(R.drawable.error);
                }
            }
            else if (KYCManager.getInstance().getFaceLivenessMode().equals(getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_PASSIVE))) {
                mLayoutCheckPL.setVisibility(View.VISIBLE);
                if (!result.getLivenessResult().getAssessment().equals("Live")) {
                    ((ImageView)mRetValue.findViewById(R.id.fragment_kyc_overview_icon_check_pl)).setImageResource(R.drawable.error);
                }
            }
            else if (KYCManager.getInstance().getFaceLivenessMode().equals(getString(R.string.STRING_KYC_OPTION_FACE_LIVENESS_MODE_ACTIVE))) {
                mLayoutCheckAL.setVisibility(View.VISIBLE);
            }

            mLayoutCheckFace.setVisibility(View.VISIBLE);
            if (result.getFace().getResult().equals("FACE_NOT_MATCH")) {
                ((ImageView)mRetValue.findViewById(R.id.fragment_kyc_overview_icon_check_face)).setImageResource(R.drawable.error);
            }
        }

        // Alerts
        mLayoutUserInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (  (result.getDocument().getKycVerificationResult().getAlerts() != null)
                        &&(result.getDocument().getKycVerificationResult().getAlerts().size() > 0)
                ){
                    String alertNb = getString(R.string.fragment_kyc_alerts);
                    String alerts = "";
                    List<KycAlert> kycAlerts = result.getDocument().getKycVerificationResult().getAlerts();

                    alertNb += ": " + result.getDocument().getKycVerificationResult().getAlerts().size();


                    for (int i = 0; i < result.getDocument().getKycVerificationResult().getAlerts().size(); i++) {
                        KycAlert alert = kycAlerts.get(i);

                        alerts += "\n" + alert.getName();
                    }

                    final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(alertNb);
                    builder.setIcon(R.drawable.error);
                    builder.setMessage(alerts);
                    builder.setPositiveButton("Ok", null);
                    builder.show();
                }
            }
        });

        // Update button function
        mButtonNext.setText(R.string.fragment_kyc_overview_button_done);
        mButtonNext.setOnClickListener(view -> onButtonClickDone());
    }

    /**
     * Displays a error.
     *
     * @param error Error.
     */
    private void displayError(final String error, final KYCResponse response, int retryStep) {
        // Append detail information about failed check if available.
        final StringBuilder fullErr = new StringBuilder(error);

        if (response != null && !response.getDocument().getKycVerificationResult().getAlerts().isEmpty()) {
            fullErr.append('\n');
            for (final KycAlert loopVerify : response.getDocument().getKycVerificationResult().getAlerts()) {
                fullErr.append(loopVerify.getName()).append(", ");
            }
            fullErr.delete(fullErr.length() - 2, fullErr.length());
        }

        mTvResultHeader.setText(fullErr);
        mIvResultIcon.setImageDrawable(getResources().getDrawable(R.drawable.error));

        mRetryStep = retryStep;

        if ((mRetryStep == KYCSession.RETRY_DOC_SCAN) || (mRetryStep == KYCSession.RETRY_SELFIE_SCAN)) {
            mButtonNext.setText(getString(R.string.button_retry));
            mButtonNext.setOnClickListener(view -> onButtonClickRetry());
        }
        else if (mRetryStep == KYCSession.RETRY_ABORT) {
            mButtonNext.setText(getString(R.string.button_abort));
            mButtonNext.setOnClickListener(view -> onButtonClickAbort());
        }
    }
    //endregion


    //region User Interface

    /**
     * On click listener for the done button.
     */
    private void onButtonClickSubmit() {
        // Show loading progress bar during asynchronous operation.
        getMainActivity().progressBarShow();
        mIvResultIcon.setVisibility(View.VISIBLE);

        // Send data to server and wait for response.
        mKYCCommunication = DataContainer.instance().mKYCCommunication;
        mKYCCommunication.verifyDocument(new KYCSession.KYCResponseHandler() {
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
                        displayError(error, null, KYCSession.RETRY_NONE);
                    }

                    @Override
                    public void onFailureRetry(final String error, int retryStep) {
                        // Operation finished. We can hide progress bar.
                        getMainActivity().progressBarHide();
                        // Display issue description.
                        displayError(error + "\n" + getString(R.string.try_again), null, retryStep);
                    }

                    @Override
                    public void onFailureAbort(final String error) {
                        // Operation finished. We can hide progress bar.
                        getMainActivity().progressBarHide();
                        // Display issue description.
                        displayError(error, null, KYCSession.RETRY_ABORT);
                    }
                }, DataContainer.instance().mVerificationStep);
    }

    /**
     * On click listener for the done button.
     */
    private void onButtonClickDone() {
        // Close all fragments
        getMainActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    /**
     * On click listener for the retry button.
     */
    private void onButtonClickRetry() {
        if (mRetryStep == KYCSession.RETRY_SELFIE_SCAN) {
            DataContainer.instance().mVerificationStep = KYCCommunication.STEP_SELFIE_VERIFICATION;
            getMainActivity().getSupportFragmentManager().popBackStack();

            return;
        }
        else if (mRetryStep == KYCSession.RETRY_DOC_SCAN) {
            DataContainer.instance().mVerificationStep = KYCCommunication.STEP_START_VERIFICATION;
            getMainActivity().getSupportFragmentManager().popBackStack();
            getMainActivity().getSupportFragmentManager().popBackStack();
            getMainActivity().getSupportFragmentManager().popBackStack();

            return;
        }
    }

    /**
     * On click listener for the abort button.
     */
    private void onButtonClickAbort() {
        DataContainer.instance().mVerificationStep = KYCCommunication.STEP_START_VERIFICATION;
        getMainActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
    //endregion


}
