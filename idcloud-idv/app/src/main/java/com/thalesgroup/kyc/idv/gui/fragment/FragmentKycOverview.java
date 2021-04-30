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

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.thalesgroup.idv.sdk.nfc.CaptureResult;
import com.thalesgroup.kyc.idv.R;
import com.thalesgroup.kyc.idv.helpers.DataContainer;
import com.thalesgroup.kyc.idv.helpers.KYCManager;
import com.thalesgroup.kyc.idv.helpers.communication.KYCChipAction;
import com.thalesgroup.kyc.idv.helpers.communication.KYCCommScheduler;
import com.thalesgroup.kyc.idv.helpers.communication.KYCCommunication;
import com.thalesgroup.kyc.idv.helpers.communication.KYCFailedVerification;
import com.thalesgroup.kyc.idv.helpers.communication.KYCResponse;
import com.thalesgroup.kyc.idv.helpers.communication.KYCSession;
import com.thalesgroup.kyc.idv.helpers.util.ImageUtil;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import java.util.List;

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
    private RelativeLayout mLayoutUserInfo;
    private TextView mTvResultHeader;
    private TextView mTvResultCaptionName;
    private TextView mTvResultValueName;
    private TextView mTvResultCaptionInfo;
    private TextView mTvResultValueInfo;
    private ImageView mIvResultIcon;

    private LinearLayout mLayoutCheckDoc;
    private LinearLayout mLayoutCheckNFC;
    private LinearLayout mLayoutCheckEPL;
    private LinearLayout mLayoutCheckFace;

    private Button mButtonNext;
    private KYCCommunication mKYCCommunication = DataContainer.instance().mKYCCommunication;

    private View mRetValue;
    private int mRetryStep;

    //endregion

    //region Life Cycle

    @SuppressLint("StaticFieldLeak")
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
        mTvResultCaptionName = mRetValue.findViewById(R.id.fragment_kyc_overview_tv_user_name_caption);
        mTvResultValueName = mRetValue.findViewById(R.id.fragment_kyc_overview_tv_user_name_value);
        mTvResultCaptionInfo = mRetValue.findViewById(R.id.fragment_kyc_overview_tv_user_info_caption);
        mTvResultValueInfo = mRetValue.findViewById(R.id.fragment_kyc_overview_tv_user_info_value);
        mIvResultIcon = mRetValue.findViewById(R.id.fragment_kyc_overview_iv_progress);

        mLayoutCheckDoc = mRetValue.findViewById(R.id.fragment_kyc_overview_layout_check_doc);
        mLayoutCheckNFC = mRetValue.findViewById(R.id.fragment_kyc_overview_layout_check_nfc);
        mLayoutCheckEPL = mRetValue.findViewById(R.id.fragment_kyc_overview_layout_check_epl);
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
        mLayoutCheckDoc.setVisibility(View.GONE);
        mLayoutCheckNFC.setVisibility(View.GONE);
        mLayoutCheckEPL.setVisibility(View.GONE);
        mLayoutCheckFace.setVisibility(View.GONE);

        // Update user interface.
        mButtonNext.setVisibility(View.INVISIBLE);

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

        Log.i("KYC", "Scheduler state: " + KYCCommScheduler.getState());
        Log.i("KYC", "Scheduler retry step: " + KYCCommScheduler.getRetryStep());

        // Communication Scheduler
        new AsyncTask<Void, Void, Void>() {
            int state = KYCCommScheduler.getState();
            int retryStep = KYCCommScheduler.getRetryStep();

            @Override
            protected void onPreExecute() {
                getMainActivity().progressBarShow();

                if (KYCManager.getInstance().isNfcMode()) {
                    displayResult(null);
                }

                String status;

                if (state == KYCCommScheduler.FAILURE_RETRY) {
                    KYCCommScheduler.sendData(retryStep);
                }

                if (KYCManager.getInstance().isFacialRecognition()) {
                    if ((state == KYCCommScheduler.FAILURE_RETRY) && (retryStep == KYCCommunication.STEP_SELFIE_VERIFICATION)) {
                        status = getString(R.string.fragment_kyc_overview_verif_doc) + getString(R.string.fragment_kyc_overview_verif_done);
                    }
                    else {
                        status = getString(R.string.fragment_kyc_overview_verif_doc) + getString(R.string.fragment_kyc_overview_verif_ongoing);
                    }
                    status += "\n";
                    status += getString(R.string.fragment_kyc_overview_verif_face) + getString(R.string.fragment_kyc_overview_verif_pending);
                } else {
                    status = getString(R.string.fragment_kyc_overview_verif_doc) + getString(R.string.fragment_kyc_overview_verif_ongoing);
                }

                mTvResultHeader.setText(status);

                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                boolean isStop = false;
                boolean isSelfieSent = false;

                do {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    state = KYCCommScheduler.getState();

                    switch (state) {
                        case KYCCommScheduler.SUCCESS:
                            publishProgress(null);
                            isStop = true;
                            break;

                        case KYCCommScheduler.FAILURE_RETRY:
                        case KYCCommScheduler.FAILURE:
                        case KYCCommScheduler.FAILURE_ABORT:
                            isStop = true;
                            break;

                        case KYCCommScheduler.SENDING_DOC:
                        case KYCCommScheduler.SENDING_SELFIE:
                            publishProgress(null);
                            break;

                        case KYCCommScheduler.WAITING_SELFIE:
                            if (!isSelfieSent) {
                                isSelfieSent = true;

                                KYCCommScheduler.sendData(KYCCommunication.STEP_SELFIE_VERIFICATION);
                                publishProgress(null);
                            }
                            break;
                    }
                }
                while (!isStop);

                return null;
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                showBitmap(DataContainer.instance().mDocFront, ivDocFront);
                showBitmap(DataContainer.instance().mDocBack, ivDocBack);

                if (DataContainer.instance().mSelfie != null) {
                    mLayoutSelfie.setVisibility(View.VISIBLE);

                    byte[] _selfie = DataContainer.instance().mSelfie;
                    showBitmap(_selfie, mIvSelfie);
                }

                state = KYCCommScheduler.getState();

                String status = (String) mTvResultHeader.getText();

                switch (state) {
                    case KYCCommScheduler.SENDING_DOC:
                        if (KYCManager.getInstance().isFacialRecognition()) {
                            status = getString(R.string.fragment_kyc_overview_verif_doc) + getString(R.string.fragment_kyc_overview_verif_ongoing);
                            status += "\n";
                            status += getString(R.string.fragment_kyc_overview_verif_face) + getString(R.string.fragment_kyc_overview_verif_pending);
                        } else {
                            status = getString(R.string.fragment_kyc_overview_verif_doc) + getString(R.string.fragment_kyc_overview_verif_ongoing);
                        }
                        break;

                    case KYCCommScheduler.SENDING_SELFIE:
                        status = getString(R.string.fragment_kyc_overview_verif_doc) + getString(R.string.fragment_kyc_overview_verif_done);
                        status += "\n";
                        status += getString(R.string.fragment_kyc_overview_verif_face) + getString(R.string.fragment_kyc_overview_verif_ongoing);
                        break;

                    case KYCCommScheduler.WAITING_SELFIE:
                        status = getString(R.string.fragment_kyc_overview_verif_doc) + getString(R.string.fragment_kyc_overview_verif_done);
                        status += "\n";
                        status += getString(R.string.fragment_kyc_overview_verif_face) + getString(R.string.fragment_kyc_overview_verif_pending);
                        break;

                    case KYCCommScheduler.SUCCESS:
                        if (KYCManager.getInstance().isFacialRecognition()) {
                            status = getString(R.string.fragment_kyc_overview_verif_doc) + getString(R.string.fragment_kyc_overview_verif_done);
                            status += "\n";
                            status += getString(R.string.fragment_kyc_overview_verif_face) + getString(R.string.fragment_kyc_overview_verif_done);
                        } else {
                            status = getString(R.string.fragment_kyc_overview_verif_doc) + getString(R.string.fragment_kyc_overview_verif_done);
                        }
                        break;

                    default:
                        break;
                }

                mTvResultHeader.setText(status);

                super.onProgressUpdate(values);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                showBitmap(DataContainer.instance().mDocFront, ivDocFront);
                showBitmap(DataContainer.instance().mDocBack, ivDocBack);

                if (DataContainer.instance().mSelfie != null) {
                    mLayoutSelfie.setVisibility(View.VISIBLE);

                    byte[] _selfie = DataContainer.instance().mSelfie;
                    showBitmap(_selfie, mIvSelfie);
                }

                switch (state) {
                    case KYCCommScheduler.SUCCESS:
                        // Operation finished. We can hide progress bar.
                        getMainActivity().progressBarHide();
                        // Update UI with values from response.
                        displayResult(KYCCommScheduler.getResponse());
                        break;

                    case KYCCommScheduler.FAILURE:
                        // Operation finished. We can hide progress bar.
                        getMainActivity().progressBarHide();
                        // Display issue description.
                        displayError(KYCCommScheduler.getError(), null, KYCSession.RETRY_NONE);
                        break;

                    case KYCCommScheduler.FAILURE_RETRY:
                        // Operation finished. We can hide progress bar.
                        getMainActivity().progressBarHide();
                        // Display issue description.
                        displayError(KYCCommScheduler.getError() + "\n" + getString(R.string.try_again), null, KYCCommScheduler.getRetryStep());
                        break;

                    case KYCCommScheduler.FAILURE_ABORT:
                        // Operation finished. We can hide progress bar.
                        getMainActivity().progressBarHide();
                        // Display issue description.
                        displayError(KYCCommScheduler.getError(), null, KYCSession.RETRY_ABORT);
                        break;
                }

                super.onPostExecute(aVoid);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

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
            if (retCaption.length() > 1)
            {
                retCaption.append("\n");
                retValue.append("\n");
            }
            retCaption.append(caption).append(":");
            retValue.append(value);
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
        if (retCaption.length() > 1)
        {
            retCaption.append("\n");
            retValue.append("\n");
        }
        retCaption.append(caption).append(":");
        retValue.append(value);
    }

    /**
     * Displays a result.
     *
     * @param result Result.
     */
    private void displayResult(final KYCResponse result) {
        mRetryStep = KYCSession.RETRY_NONE;

        // --------
        // NFC Case
        // --------
        if (KYCManager.getInstance().isNfcMode()) {
            // Display NFC data if any
            if (result != null && result.getChipNfc() == null) {
                displayError("Failed to get valid response from server.", null, KYCSession.RETRY_NONE);
                return;
            } else if (DataContainer.instance().mNfcResult != null) {
                CaptureResult captureResult = DataContainer.instance().mNfcResult;

                // Build user information strings.
                final StringBuilder captionName = new StringBuilder();
                final StringBuilder valueName = new StringBuilder();
                final StringBuilder captionInfo = new StringBuilder();
                final StringBuilder valueInfo = new StringBuilder();

                String name = captureResult.parsedData.personalDetails.fullName.replace("<", " ");

                captionName.append(getString(R.string.kyc_result_name));
                valueName.append(name);

                appendResultString(captionInfo, valueInfo, getString(R.string.kyc_result_birth_date), captureResult.parsedData.personalDetails.dateOfBirth);
                appendResultString(captionInfo, valueInfo, getString(R.string.kyc_result_gender), captureResult.parsedData.mrz.gender);
                appendResultString(captionInfo, valueInfo, getString(R.string.kyc_result_nationality), captureResult.parsedData.mrz.nationality);
                appendResultString(captionInfo, valueInfo, getString(R.string.kyc_result_doc_type), captureResult.parsedData.mrz.documentType.replace("<", " "));
                appendResultString(captionInfo, valueInfo, getString(R.string.kyc_result_doc_number), captureResult.parsedData.mrz.documentNumber);

                String expiryDate = captureResult.parsedData.mrz.dateOfExpiry;
                appendResultString(captionInfo, valueInfo, getString(R.string.kyc_result_expiry_date), "20" + expiryDate.substring(0, 2) + "/" + expiryDate.substring(2, 4) + "/" + expiryDate.substring(4));

                // Extracted selfie
                if (captureResult.parsedData.faceImage != null) {
                    mIvSelfieExtracted.setVisibility(View.VISIBLE);
                    showBitmap(ImageUtil.bitmapToBytes(captureResult.parsedData.faceImage, Bitmap.CompressFormat.JPEG), mIvSelfieExtracted);

                    // Add margin to left photo. It was not there from beginning since there
                    // was just one image.
                    final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mIvSelfie.getLayoutParams();
                    params.setMargins(0, 0, 8, 0);
                    mIvSelfie.setLayoutParams(params);

                    // Selfie part might be hidden for document scan only, but we sill have extracted image.
                    mLayoutSelfie.setVisibility(View.VISIBLE);
                }

                // Server info if available
                if (result != null) {
                    // Enhanced Passive Liveness Result?
                    if (result.getEnhancedLiveness() != null) {
                        appendResultInt(captionInfo, valueInfo, getString(R.string.kyc_result_liveness_score), result.getEnhancedLiveness().getScore());
                    }

                    // Update status header + icon.
                    if (result.getChipNfc().getChipOutput().toUpperCase().equals("OK")) {
                        mTvResultHeader.setVisibility(View.GONE);
                        mLayoutCheckNFC.setVisibility(View.VISIBLE);
                    } else {
                        String docRes = getString(R.string.fragment_kyc_nfc_status) + " ";

                        if (result.getChipNfc().getChipOutput().toUpperCase().equals("NOK")) {
                            docRes += "Failed";
                        }
                        else if (result.getChipNfc().getChipOutput().toUpperCase().equals("CSANOTFOUND")) {
                            docRes += "Certificate not found";
                        }
                        else {
                            docRes += result.getChipNfc().getChipOutput();
                        }

                        mTvResultHeader.setText(docRes);
                    }

                    mIvResultIcon.setVisibility(View.GONE);

                    // Update NFC Actions
                    List<KYCChipAction> kycNfcActions = result.getChipNfc().getChipActions();

                    for (int i = 0; i < result.getChipNfc().getChipActions().size(); i++) {
                        KYCChipAction action = kycNfcActions.get(i);

                        appendResultString(captionInfo, valueInfo, action.getName(), String.valueOf(action.getResultValueText()));
                    }

                    // -------------
                    // Update checks
                    // -------------

                    if (KYCManager.getInstance().isFacialRecognition()) {
                        mLayoutCheckEPL.setVisibility(View.VISIBLE);
                        if (result.getEnhancedLiveness().getScore() < 80) {
                            ((ImageView)mRetValue.findViewById(R.id.fragment_kyc_overview_icon_check_epl)).setImageResource(R.drawable.error);
                        }

                        mLayoutCheckFace.setVisibility(View.VISIBLE);
                        if (result.getFace().getResult().equals("FACE_NOT_MATCH")) {
                            ((ImageView)mRetValue.findViewById(R.id.fragment_kyc_overview_icon_check_face)).setImageResource(R.drawable.error);
                        }
                    }
                }

                // Show / Hide whole section
                mLayoutUserInfo.setVisibility(captionInfo.length() > 0 ? View.VISIBLE : View.GONE);
                mTvResultCaptionName.setText(captionName.toString());
                mTvResultValueName.setText(valueName.toString());
                mTvResultCaptionInfo.setText(captionInfo.toString());
                mTvResultValueInfo.setText(valueInfo.toString());

                // Update button function
                mButtonNext.setVisibility(View.VISIBLE);
                mButtonNext.setText(R.string.fragment_kyc_overview_button_done);
                mButtonNext.setOnClickListener(view -> onButtonClickDone());
            }
        }

        // ----------------------
        // Standard Document Case
        // ----------------------
        else {
            // Make sure that response is valid and positive.
            if (result == null || result.getDocument() == null) {
                displayError("Failed to get valid response from server.", null, KYCSession.RETRY_NONE);
                return;
            } else if (
                         // DV status
                         (!result.getDocument().getResult().equalsIgnoreCase("SUCCESS"))
                       &&(!result.getDocument().getResult().equalsIgnoreCase("DATA_INTEGRITY_ISSUE"))
                       &&(!result.getDocument().getResult().equalsIgnoreCase("SECURITY_ISSUE"))
                       &&(!result.getDocument().getResult().equalsIgnoreCase("DOCUMENT_EXPIRED"))
                       &&(!result.getDocument().getResult().equalsIgnoreCase("QA_NOT_PASSED"))
                       &&(!result.getDocument().getResult().equalsIgnoreCase("QA_PASSED"))

                         // DV+FV+EL status
                       &&(!result.getDocument().getResult().equalsIgnoreCase("NoFailedChecks"))
                       &&(!result.getDocument().getResult().equalsIgnoreCase("DocumentExpired"))
                       &&(!result.getDocument().getResult().equalsIgnoreCase("MRZCheckFailed"))
                       &&(!result.getDocument().getResult().equalsIgnoreCase("SecurityCheckFailed"))
                       &&(!result.getDocument().getResult().equalsIgnoreCase("VIZCrosscheckFailed"))
                       &&(!result.getDocument().getResult().equalsIgnoreCase("NoFailedSubsetOfChecks"))
                       &&(!result.getDocument().getResult().equalsIgnoreCase("DocumentPageMissing"))
                       &&(!result.getDocument().getResult().equalsIgnoreCase("SpecimenDetected"))
                       &&(!result.getDocument().getResult().equalsIgnoreCase("QA_OK"))
                       &&(!result.getDocument().getResult().equalsIgnoreCase("QA_KO"))
                       &&(!result.getDocument().getResult().equalsIgnoreCase("QA_NOT_DONE"))
                     )
           {
                displayError(result.getDocument().getResult(), result, KYCSession.RETRY_ABORT);
                return;
           }

            // Build user information strings.
            final StringBuilder captionName = new StringBuilder();
            final StringBuilder valueName = new StringBuilder();
            final StringBuilder captionInfo = new StringBuilder();
            final StringBuilder valueInfo = new StringBuilder();
            String firstName = "";
            String lastName = "";

            // Name available in MRZ?
            if (result.getDocument().getMRZ() != null) {
                firstName = result.getDocument().getMRZ().getFirstName();
                lastName = result.getDocument().getMRZ().getLastName();
            }

            // Default name if not in MRZ
            if (firstName.equals("")) {
                firstName = result.getDocument().getFirstName();
            }
            if (lastName.equals("")) {
                lastName = result.getDocument().getSurname();
            }

            valueName.append(firstName).append(' ').append(lastName);

            if (!firstName.equals("") && !lastName.equals("")) {
                captionName.append(getString(R.string.kyc_result_name_surname));
            }

            appendResultString(captionInfo, valueInfo, getString(R.string.kyc_result_gender), result.getDocument().getGender());
            appendResultString(captionInfo, valueInfo, getString(R.string.kyc_result_nationality), result.getDocument().getNationality());
            appendResultString(captionInfo, valueInfo, getString(R.string.kyc_result_expiry_date), result.getDocument().getExpiryDate());
            appendResultString(captionInfo, valueInfo, getString(R.string.kyc_result_birth_date), result.getDocument().getBirthDate());
            appendResultString(captionInfo, valueInfo, getString(R.string.kyc_result_doc_number), result.getDocument().getDocumentNumber());
            appendResultString(captionInfo, valueInfo, getString(R.string.kyc_result_doc_type), result.getDocument().getDocumentType());
            appendResultInt(captionInfo, valueInfo, getString(R.string.kyc_result_verification_count), result.getDocument().getTotalVerificationsDone());
            appendResultInt(captionInfo, valueInfo, getString(R.string.fragment_kyc_alerts), result.getDocument().getFailedVerifications() != null ? result.getDocument().getFailedVerifications().size() : 0);

            // Enhanced Passive Liveness Result?
            if (result.getEnhancedLiveness() != null) {
                appendResultInt(captionInfo, valueInfo, getString(R.string.kyc_result_liveness_score), result.getEnhancedLiveness().getScore());
            }

            // Show / Hide whole section
            mLayoutUserInfo.setVisibility(captionInfo.length() > 0 ? View.VISIBLE : View.GONE);
            mTvResultCaptionName.setText(captionName.toString());
            mTvResultValueName.setText(valueName.toString());
            mTvResultCaptionInfo.setText(captionInfo.toString());
            mTvResultValueInfo.setText(valueInfo.toString());

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
            if (result.getDocument().getInterpretedResult().toLowerCase().equals("success")) {
                mTvResultHeader.setVisibility(View.GONE);
                mLayoutCheckDoc.setVisibility(View.VISIBLE);
            }
            else {
                String docRes = getString(R.string.fragment_kyc_doc_status) + " " + result.getDocument().getInterpretedResult();
                mTvResultHeader.setText(docRes);
            }

            mIvResultIcon.setVisibility(View.GONE);

            // -------------
            // Update checks
            // -------------

            if (KYCManager.getInstance().isFacialRecognition()) {
                mLayoutCheckEPL.setVisibility(View.VISIBLE);
                if (result.getEnhancedLiveness().getScore() < 80) {
                    ((ImageView)mRetValue.findViewById(R.id.fragment_kyc_overview_icon_check_epl)).setImageResource(R.drawable.error);
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
                    if (  (result.getDocument().getFailedVerifications() != null)
                        &&(result.getDocument().getFailedVerifications().size() > 0)
                       ){
                        String alertNb = getString(R.string.fragment_kyc_alerts);
                        String alerts = "";
                        List<KYCFailedVerification> kycAlerts = result.getDocument().getFailedVerifications();

                        alertNb += ": " + result.getDocument().getFailedVerifications().size();


                        for (int i = 0; i < result.getDocument().getFailedVerifications().size(); i++) {
                            KYCFailedVerification alert = kycAlerts.get(i);

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
            mButtonNext.setVisibility(View.VISIBLE);
            mButtonNext.setText(R.string.fragment_kyc_overview_button_done);
            mButtonNext.setOnClickListener(view -> onButtonClickDone());
        }
    }

    /**
     * Displays a error.
     *
     * @param error Error.
     */
    private void displayError(final String error, final KYCResponse response, int retryStep) {
        // Append detail information about failed check if available.
        final StringBuilder fullErr = new StringBuilder(error);

        if (response != null && response.getDocument() != null && !response.getDocument().getFailedVerifications().isEmpty()) {
            fullErr.append('\n');

            for (final KYCFailedVerification loopVerify : response.getDocument().getFailedVerifications()) {
                fullErr.append(loopVerify.getName()).append(", ");
            }

            fullErr.delete(fullErr.length() - 2, fullErr.length());
        }

        mTvResultHeader.setText(fullErr);
        mIvResultIcon.setImageDrawable(getResources().getDrawable(R.drawable.error));

        mRetryStep = retryStep;

        if ((mRetryStep == KYCSession.RETRY_DOC_SCAN) || (mRetryStep == KYCSession.RETRY_SELFIE_SCAN)) {
            mButtonNext.setVisibility(View.VISIBLE);
            mButtonNext.setText(getString(R.string.button_retry));
            mButtonNext.setOnClickListener(view -> onButtonClickRetry());
        }
        else if (mRetryStep == KYCSession.RETRY_ABORT) {
            mButtonNext.setVisibility(View.VISIBLE);
            mButtonNext.setText(getString(R.string.button_abort));
            mButtonNext.setOnClickListener(view -> onButtonClickAbort());
        }
    }

    //endregion


    //region User Interface

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
            DataContainer.instance().mVerificationStep = KYCCommunication.STEP_START_DOC_VERIFICATION;
            getMainActivity().getSupportFragmentManager().popBackStack(1, FragmentManager.POP_BACK_STACK_INCLUSIVE);

            return;
        }
    }

    /**
     * On click listener for the abort button.
     */
    private void onButtonClickAbort() {
        DataContainer.instance().mVerificationStep = KYCCommunication.STEP_START_DOC_VERIFICATION;
        getMainActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    //endregion


}
