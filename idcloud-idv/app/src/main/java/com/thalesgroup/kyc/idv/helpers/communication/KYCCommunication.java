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

package com.thalesgroup.kyc.idv.helpers.communication;

import android.util.Base64;
import android.util.Log;

import com.aware.face_liveness.yy.c;
import com.thalesgroup.idv.sdk.nfc.CaptureResult;
import com.thalesgroup.kyc.idv.BuildConfig;
import com.thalesgroup.kyc.idv.helpers.DataContainer;
import com.thalesgroup.kyc.idv.helpers.KYCConfiguration;
import com.thalesgroup.kyc.idv.helpers.KYCManager;
import com.thalesgroup.kyc.idv.helpers.util.ImageUtil;
import com.thalesgroup.kyc.idv.helpers.util.JsonUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.thalesgroup.kyc.idv.helpers.KYCManager.KYC_QR_CODE_VERSION_KYC2;

/**
 * Class which ensures the communication with the verification backend.
 */
public class KYCCommunication {
    //region Definition
    public static final int STEP_START_DOC_VERIFICATION = 1;
    public static final int STEP_SELFIE_VERIFICATION = 2;

    private static final String STATE_WAITING = "Waiting";
    private static final String STATE_FINISHED = "Finished";
    private static final String STATE_RUNNING = "Running";
    private static final String STATE_FAILURE = "Failure";
    private static final String STATE_ERROR = "Error";

    private static KYCSession mSession;
    private static int mCurrentStep = 1;
    private static boolean mIsIncremental = false;

    /**
     * Response callback.
     */
    private interface GenericResponse {

        /**
         * Finish callback method.
         *
         * @param response Response.
         * @param error    Error.
         */
        void onFinished(String response, String error);
    }

    //endregion

    //region Public API

    /**
     * Sends the document and face images to the verification backend for verification.
     *
     * @param handler  Callback.
     * @param startStep  Step to start scenario.
     */
    public void verifyDocument(final KYCSession.KYCResponseHandler handler, boolean isIncremental, int startStep) {
        mIsIncremental = isIncremental;

        // Prepare session.
        if (startStep == STEP_START_DOC_VERIFICATION) {
            mSession = new KYCSession(KYCManager.getInstance().getBaseUrl(), handler);

            // IDV mode
            if (!KYCManager.getInstance().isFacialRecognition()) {
                // NFC mode
                if (KYCManager.getInstance().isNfcMode()) {
                    idv_verifyNfc();
                }
                // OCR mode
                else {
                    idv_verifyDocument();
                }
            }

            // Aware mode
            else {
                // NFC mode
                if (KYCManager.getInstance().isNfcMode()) {
                    idv_verifyNfc();
                }
                // OCR mode
                else {
                    aware_verifyDocument();
                }
            }
        }
        else if (startStep == STEP_SELFIE_VERIFICATION) {
            mSession.setHandler(handler);

            aware_verifyFaceStep();
        }
    }

    /**
     * Removes the callback listener.
     */
    public void removeListener() {
        if (mSession != null) {
            mSession.removeListener();
        }
    }

    //endregion

    //region Private Helpers

    /**
     * IDV - Sends the document and face images to the verification backend for verification.
     */
    private void idv_verifyDocument() {
        mCurrentStep = 1;

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getBaseUrl());
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            if (BuildConfig.DEBUG) {
                Log.w("KYC", "URL: " + connection.getURL().toString());
            }

            // Build post JSON
            final JSONObject json = idv_createVerificationJSON(DataContainer.instance().mDocFront,
                                                               DataContainer.instance().mDocBack);

            // Send request in a new Thread and handle response
            getConnectionResponse(json, connection, (response, error) -> {
                // Fragment/Activity which is waiting for the result is already gone, so no need to continue with request.
                if (!mSession.isListenerRegistered()) {
                    return;
                }

                if (response != null) {
                    try {
                        // Parse server response and get session id.
                        final JSONObject res = new JSONObject(response);
                        final String sessionId = res.getString("id");

                        // Failed to get valid operation session id.
                        if (sessionId == null || sessionId.isEmpty()) {
                            mSession.handleError("Failed to get valid session id.");
                            return;
                        }

                        // Pass get the session id to current session and continue.
                        mSession.updateWithSessionId(sessionId);
                        // Call it directly so we don't have to deal with sync.
                        idv_pollingDocResultStep(0);
                    } catch (final JSONException exception) {
                        mSession.handleErrorAbort(exception.getLocalizedMessage());
                    }
                } else if (error != null) {
                    // Direct communication error.
                    mSession.handleErrorAbort(error);
                } else {
                    // Unknown state. Successful communication with empty result.
                    mSession.handleErrorAbort(KYCManager.getInstance().getErrorMessage("9919", null));
                }
            });

        } catch (final IOException | JSONException exception) {
            // Communication / json parsing issue.
            mSession.handleError(exception.getLocalizedMessage());
        }
    }

    /**
     * IDV - Starts the second verification step with the verification backend.
     */
    private void idv_pollingDocResultStep(int counter) {
        mCurrentStep = 2;

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getUrlWithSessionId());
            connection.setRequestMethod("GET");

            if (BuildConfig.DEBUG) {
                Log.w("KYC", "URL: " + connection.getURL().toString());
                Log.w("KYC", "Try #" + (counter + 1));
            }

            // Send request in a new Thread and handle response
            getConnectionResponse(null, connection, (response, error) -> {
                // Fragment/Activity which is waiting for the result is already gone, so no need to continue with request.
                if (!mSession.isListenerRegistered()) {
                    return;
                }

                if (response != null) {
                    try {
                        // Parse server response and get session id.
                        final JSONObject res = new JSONObject(response);
                        final String status = res.getString("status");

                        // Server operation still running.
                        if (status != null && status.equalsIgnoreCase(STATE_RUNNING)) {
                            if (counter > KYCConfiguration.IDCLOUD_NUMBER_OF_RETRIES) {
                                mSession.handleError("Failed to Poll final result, numbers of retries was reached.");
                            }
                            else {
                                Thread.sleep(KYCConfiguration.IDCLOUD_RETRY_DELAY_SEC * 1000);

                                idv_pollingDocResultStep(counter + 1);
                            }
                        }
                        // Server operation is finished.
                        else if (status != null && status.equalsIgnoreCase(STATE_FINISHED)) {
                            final KYCResponse result = new KYCResponse(res.getJSONObject("state").getJSONObject("result"));
                            final int statusCode = res.getJSONObject("state").getJSONObject("result").getInt("code");
                            final String message = res.getJSONObject("state").getJSONObject("result").getString("message");

                            // Specific error management for doc verification
                            if (  (statusCode >= 4600)
                                &&(statusCode <= 4604)) {
                                mSession.handleErrorRetry(KYCManager.getInstance().getErrorMessage(String.valueOf(statusCode), message), KYCSession.RETRY_DOC_SCAN);
                            }
                            else {
                                mSession.handleResult(result);
                            }
                        }
                        else{
                            final int statusCode = res.getJSONObject("state").getJSONObject("result").getInt("code");
                            final String message = res.getJSONObject("state").getJSONObject("result").getString("message");

                            // Specific error management for doc verification
                            if (  (statusCode >= 4600)
                                &&(statusCode <= 4604)) {
                                mSession.handleErrorRetry(KYCManager.getInstance().getErrorMessage(String.valueOf(statusCode), message), KYCSession.RETRY_DOC_SCAN);
                            }

                            // Specific error management for unrecognized doc
                            else if (statusCode == 5301) {
                                mSession.handleErrorRetry(KYCManager.getInstance().getErrorMessage(String.valueOf(statusCode), message), KYCSession.RETRY_DOC_SCAN);
                            }

                            // Default error display
                            else {
                                mSession.handleErrorRetry("Status: " + status +
                                                "\nCode: " + statusCode +
                                                "\n" + KYCManager.getInstance().getErrorMessage(""+ statusCode, message),
                                        KYCSession.RETRY_DOC_SCAN);
                            }
                        }
                    } catch (final JSONException exception) {
                        mSession.handleErrorAbort(exception.getLocalizedMessage());
                    } catch (InterruptedException exception) {
                        mSession.handleErrorAbort(exception.toString());
                    }
                } else if (error != null) {
                    // Direct communication error.
                    mSession.handleErrorAbort(error);
                } else {
                    // Unknown state. Successful communication with empty result.
                    mSession.handleErrorAbort(KYCManager.getInstance().getErrorMessage("9919", null));
                }
            });
        } catch (final IOException exception) {
            // Communication / json parsing issue.
            mSession.handleErrorAbort(exception.getLocalizedMessage());
        }
    }

    /**
     * IDV - Sends the document NFC data to the verification backend for verification.
     */
    private void idv_verifyNfc() {
        mCurrentStep = 1;

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getBaseUrl());
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            if (BuildConfig.DEBUG) {
                Log.w("KYC", "URL: " + connection.getURL().toString());
            }

            // Build post JSON
            final JSONObject json = idv_createNfcVerificationJSON(DataContainer.instance().mNfcResult);

            // Send request in a new Thread and handle response
            getConnectionResponse(json, connection, (response, error) -> {
                // Fragment/Activity which is waiting for the result is already gone, so no need to continue with request.
                if (!mSession.isListenerRegistered()) {
                    return;
                }

                if (response != null) {
                    try {
                        // Parse server response and get session id.
                        final JSONObject res = new JSONObject(response);
                        final String sessionId = res.getString("id");

                        // Failed to get valid operation session id.
                        if (sessionId == null || sessionId.isEmpty()) {
                            mSession.handleError("Failed to get valid session id.");
                            return;
                        }

                        // Pass get the session id to current session and continue.
                        mSession.updateWithSessionId(sessionId);
                        // Call it directly so we don't have to deal with sync.
                        idv_pollingNfcResultStep(0);
                    } catch (final JSONException exception) {
                        mSession.handleErrorAbort(exception.getLocalizedMessage());
                    }
                } else if (error != null) {
                    // Direct communication error.
                    mSession.handleErrorAbort(error);
                } else {
                    // Unknown state. Successful communication with empty result.
                    mSession.handleErrorAbort(KYCManager.getInstance().getErrorMessage("9919", null));
                }
            });

        } catch (final IOException | JSONException exception) {
            // Communication / json parsing issue.
            mSession.handleError(exception.getLocalizedMessage());
        }
    }

    /**
     * IDV - Starts the second verification step (NFC) with the verification backend.
     */
    private void idv_pollingNfcResultStep(int counter) {
        mCurrentStep = 2;

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getUrlWithSessionId());
            connection.setRequestMethod("GET");

            if (BuildConfig.DEBUG) {
                Log.w("KYC", "URL: " + connection.getURL().toString());
                Log.w("KYC", "Try #" + (counter + 1));
            }

            // Send request in a new Thread and handle response
            getConnectionResponse(null, connection, (response, error) -> {
                // Fragment/Activity which is waiting for the result is already gone, so no need to continue with request.
                if (!mSession.isListenerRegistered()) {
                    return;
                }

                if (response != null) {
                    try {
                        // Parse server response and get session id.
                        final JSONObject res = new JSONObject(response);
                        final String status = res.getString("status");

                        // Server operation still running.
                        if (status != null && status.equalsIgnoreCase(STATE_RUNNING)) {
                            if (counter > KYCConfiguration.IDCLOUD_NUMBER_OF_RETRIES) {
                                mSession.handleError("Failed to Poll final result, numbers of retries was reached.");
                            }
                            else {
                                Thread.sleep(KYCConfiguration.IDCLOUD_RETRY_DELAY_SEC * 1000);

                                idv_pollingNfcResultStep(counter + 1);
                            }
                        }
                        // Server operation is finished.
                        else if (status != null && status.equalsIgnoreCase(STATE_FINISHED)) {
                            final KYCResponse result = new KYCResponse(res.getJSONObject("state").getJSONObject("result"));

                            mSession.handleResult(result);
                        }
                        else if (status != null && status.equalsIgnoreCase(STATE_WAITING)) {
                            JSONObject tmpResponse = res.getJSONObject("state").getJSONObject("result");

                            if (tmpResponse.has("object")) {
                                final JSONObject object = tmpResponse.getJSONObject("object");

                                // Hack to store intermediate response
                                if (object.has("chipResult")) {
                                    DataContainer.instance().mIdvChipResult = object.getJSONObject("chipResult").toString();
                                }
                            }
                            if (!mIsIncremental) {
                                aware_verifyFaceStep();
                            }
                            else {
                                final KYCResponse result = new KYCResponse(res.getJSONObject("state").getJSONObject("result"));
                                mSession.handleProgress(2, STEP_SELFIE_VERIFICATION, result);
                            }
                        }
                        else{
                            final int statusCode = res.getJSONObject("state").getJSONObject("result").getInt("code");
                            final String message = res.getJSONObject("state").getJSONObject("result").getString("message");
                            mSession.handleErrorRetry("Status: " + status +
                                            "\nCode: " + statusCode +
                                            "\n" + KYCManager.getInstance().getErrorMessage(""+ statusCode, message),
                                    KYCSession.RETRY_DOC_SCAN);
                        }
                    } catch (final JSONException exception) {
                        mSession.handleErrorAbort(exception.getLocalizedMessage());
                    } catch (InterruptedException exception) {
                        mSession.handleErrorAbort(exception.toString());
                    }
                } else if (error != null) {
                    // Direct communication error.
                    mSession.handleErrorAbort(error);
                } else {
                    // Unknown state. Successful communication with empty result.
                    mSession.handleErrorAbort(KYCManager.getInstance().getErrorMessage("9919", null));
                }
            });
        } catch (final IOException exception) {
            // Communication / json parsing issue.
            mSession.handleErrorAbort(exception.getLocalizedMessage());
        }
    }

    /**
     * Aware - Sends the document and face images to the verification backend for verification.
     */
    private void aware_verifyDocument() {
        mCurrentStep = 1;

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getBaseUrl());
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            if (BuildConfig.DEBUG) {
                Log.w("KYC", "URL: " + connection.getURL().toString());
            }

            // Build post JSON
            final JSONObject json = aware_createVerificationJSON(DataContainer.instance().mDocFront, DataContainer.instance().mDocBack);

            // Send request in a new Thread and handle response
            getConnectionResponse(json, connection, (response, error) -> {
                // Fragment/Activity which is waiting for the result is already gone, so no need to continue with request.
                if (!mSession.isListenerRegistered()) {
                    return;
                }

                if (response != null) {
                    try {
                        // Parse server response and get session id.
                        final JSONObject res = new JSONObject(response);
                        final String sessionId = res.getString("id");

                        // Failed to get valid operation session id.
                        if (sessionId == null || sessionId.isEmpty()) {
                            mSession.handleError("Failed to get valid session id.");
                            return;
                        }

                        // Pass get the session id to current session and continue.
                        mSession.updateWithSessionId(sessionId);

                        // Call it directly so we don't have to deal with sync.
                        aware_pollingDocResultStep(0);
                    } catch (final JSONException exception) {
                        mSession.handleErrorAbort(exception.getLocalizedMessage());
                    }
                } else if (error != null) {
                    // Direct communication error.
                    mSession.handleErrorAbort(error);
                } else {
                    // Unknown state. Successful communication with empty result.
                    mSession.handleErrorAbort(KYCManager.getInstance().getErrorMessage("9919", null));
                }
            });

        } catch (final IOException | JSONException exception) {
            // Communication / json parsing issue.
            mSession.handleErrorAbort(exception.getLocalizedMessage());
        }
    }

    /**
     * Aware - Polling for face verification & enhanced liveness.
     *
     *  @param counter Polling counter.
     *
     * */
    private void aware_pollingDocResultStep(int counter) {
        mCurrentStep = 2;

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getUrlWithSessionId());
            connection.setRequestMethod("GET");

            if (BuildConfig.DEBUG) {
                Log.w("KYC", "URL: " + connection.getURL().toString());
                Log.w("KYC", "Try #" + (counter + 1));
            }

            // Send request in a new Thread and handle response
            getConnectionResponse(null, connection, (response, error) -> {
                // Fragment/Activity which is waiting for the result is already gone, so no need to continue with request.
                if (!mSession.isListenerRegistered()) {
                    return;
                }

                if (response != null) {
                    try {
                        // Parse server response and get session id.
                        final JSONObject res = new JSONObject(response);
                        final String status = res.getString("status");

                        // Server operation still running.
                        if (status != null && status.equalsIgnoreCase(STATE_RUNNING)) {
                            if (counter > KYCConfiguration.IDCLOUD_NUMBER_OF_RETRIES) {
                                mSession.handleError("Failed to Poll final result, numbers of retries was reached.");
                            }
                            else {
                                Thread.sleep(KYCConfiguration.IDCLOUD_RETRY_DELAY_SEC * 1000);

                                aware_pollingDocResultStep(counter + 1);
                            }
                        }
                        // Server operation is finished.
                        else if (status != null && status.equalsIgnoreCase(STATE_FINISHED)) {
                            final KYCResponse result = new KYCResponse(res.getJSONObject("state").getJSONObject("result"));
                            mSession.handleResult(result);
                        }
                        else if (status != null && status.equalsIgnoreCase(STATE_WAITING)) {
                            if (!mIsIncremental) {
                                aware_verifyFaceStep();
                            }
                            else {
                                final KYCResponse result = new KYCResponse(res.getJSONObject("state").getJSONObject("result"));

                                mSession.handleProgress(2, STEP_SELFIE_VERIFICATION, result);
                            }
                        }
                        else{
                            final int statusCode = res.getJSONObject("state").getJSONObject("result").getInt("code");
                            final String message = res.getJSONObject("state").getJSONObject("result").getString("message");
                            mSession.handleErrorRetry("Status: " + status +
                                            "\nCode: " + statusCode +
                                            "\n" + KYCManager.getInstance().getErrorMessage(""+ statusCode, message),
                                    KYCSession.RETRY_DOC_SCAN);
                        }
                    } catch (final JSONException exception) {
                        mSession.handleErrorAbort(exception.getLocalizedMessage());
                    } catch (InterruptedException exception) {
                        mSession.handleErrorAbort(exception.toString());
                    }
                } else if (error != null) {
                    // Direct communication error.
                    mSession.handleErrorAbort(error);
                } else {
                    // Unknown state. Successful communication with empty result.
                    mSession.handleErrorAbort(KYCManager.getInstance().getErrorMessage("9919", null));
                }
            });
        } catch (final IOException exception) {
            // Communication / json parsing issue.
            mSession.handleErrorAbort(exception.getLocalizedMessage());
        }
    }

    /**
     * Aware - Sends data for face verification & enhanced liveness.
     *
     * */
    private void aware_verifyFaceStep() {
        mCurrentStep = 3;

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getUrlEnhancedLiveness());
            connection.setDoInput(true);
            connection.setRequestMethod("PATCH");

            if (BuildConfig.DEBUG) {
                Log.w("KYC", "URL: " + connection.getURL().toString());
            }

            // Build post JSON
            final JSONObject json = aware_enhancedLivenessJSON(DataContainer.instance().mEnhancedSelfieJson);

            // Send request in a new Thread and handle response
            getConnectionResponse(json, connection, (response, error) -> {
                // Fragment/Activity which is waiting for the result is already gone, so no need to continue with request.
                if (!mSession.isListenerRegistered()) {
                    return;
                }

                if (response != null) {
                    try {
                        // Parse server response and get session id.
                        final JSONObject res = new JSONObject(response);
                        final String sessionId = res.getString("id");

                        // Failed to get valid operation session id.
                        if (sessionId == null || sessionId.isEmpty()) {
                            mSession.handleError("Failed to get valid session id.");
                            return;
                        }

                        final String status = res.getString("status");

                        if (status != null && status.equalsIgnoreCase(STATE_FAILURE)) {
                            final int statusCode = res.getJSONObject("state").getJSONObject("result").getInt("code");
                            final String message = res.getJSONObject("state").getJSONObject("result").getString("message");
                            mSession.handleErrorRetry("Status: " + status +
                                            "\nCode: " + statusCode +
                                            "\n" + KYCManager.getInstance().getErrorMessage(""+ statusCode, message),
                                    KYCSession.RETRY_SELFIE_SCAN);
                            return;
                        } else if (status != null && status.equalsIgnoreCase(STATE_ERROR)) {
                            mSession.handleErrorAbort("Configuration error. Contact Thales representative.");
                            return;
                        }

                        // Pass get the session id to current session and continue.
                        mSession.updateWithSessionId(sessionId);

                        // Call it directly so we don't have to deal with sync.
                        aware_pollingFinalResultStep(0);
                    } catch (final JSONException exception) {
                        mSession.handleErrorAbort(exception.getLocalizedMessage());
                    }
                } else if (error != null) {
                    // Direct communication error.
                    mSession.handleErrorAbort(error);
                } else {
                    // Unknown state. Successful communication with empty result.
                    mSession.handleErrorAbort(KYCManager.getInstance().getErrorMessage("9919", null));
                }
            });

        } catch (final IOException | JSONException exception) {
            // Communication / json parsing issue.
            mSession.handleErrorAbort(exception.getLocalizedMessage());
        }
    }

    /**
     * Aware - Polling for face verification & enhanced liveness.
     *
     *  @param counter Polling counter.
     *
     * */
    private void aware_pollingFinalResultStep(int counter) {
        mCurrentStep = 4;

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getUrlEnhancedLivenessPollResult());
            connection.setRequestMethod("GET");

            if (BuildConfig.DEBUG) {
                Log.w("KYC", "URL: " + connection.getURL().toString());
                Log.w("KYC", "Try #" + (counter + 1));
            }

            // Send request in a new Thread and handle response
            getConnectionResponse(null, connection, (response, error) -> {
                // Fragment/Activity which is waiting for the result is already gone, so no need to continue with request.
                if (!mSession.isListenerRegistered()) {
                    return;
                }

                if (response != null) {
                    try {
                        // Parse server response and get session id.
                        final JSONObject res = new JSONObject(response);
                        final String status = res.getString("status");

                        // Server operation still running.
                        if (status != null && status.equalsIgnoreCase(STATE_RUNNING)) {
                            if (counter > KYCConfiguration.IDCLOUD_NUMBER_OF_RETRIES) {
                                mSession.handleError("Failed to Poll final result, numbers of retries was reached.");
                            }
                            else {
                                Thread.sleep(KYCConfiguration.IDCLOUD_RETRY_DELAY_SEC * 1000);

                                aware_pollingFinalResultStep(counter + 1);
                            }
                        }

                        // Server operation is finished.
                        else if (status != null && status.equalsIgnoreCase(STATE_FINISHED)) {
                            final KYCResponse result = new KYCResponse(res.getJSONObject("state").getJSONObject("result"));
                            mSession.handleResult(result);
                        } else {
                            final int statusCode = res.getJSONObject("state").getJSONObject("result").getInt("code");
                            final String message = res.getJSONObject("state").getJSONObject("result").getString("message");

                            // Specific error management for passive liveness error (Poor Quality / Not Live)
                            if (  (statusCode >= 5331)
                                &&(statusCode <= 5333)) {
                                final KYCResponse result = new KYCResponse(res.getJSONObject("state").getJSONObject("result"));

                                if (statusCode == 5332) {
                                    mSession.handleErrorRetry(KYCManager.getInstance().getErrorMessage("5332"), KYCSession.RETRY_SELFIE_SCAN);
                                }
                                else {
                                    mSession.handleErrorRetry(KYCManager.getInstance().getErrorMessage(String.valueOf(statusCode), message), KYCSession.RETRY_SELFIE_SCAN);
                                }
                            }

                            // Specific error management for doc verification
                            else if (  (statusCode >= 4600)
                                     &&(statusCode <= 4604)) {
                                mSession.handleErrorRetry(KYCManager.getInstance().getErrorMessage(String.valueOf(statusCode), message), KYCSession.RETRY_DOC_SCAN);
                            }

                            // Specific error management for face verification
                            else if (  (  (statusCode >= 4610)
                                        &&(statusCode <= 4612)
                                       )
                                     ||(  (statusCode == 0)
                                        &&(res.getJSONObject("state").getJSONObject("result").getJSONObject("object") != null)
                                        &&(res.getJSONObject("state").getJSONObject("result").getJSONObject("object").getJSONObject("face") != null)
                                        &&(res.getJSONObject("state").getJSONObject("result").getJSONObject("object").getJSONObject("face").getString("result").toUpperCase().equals("MATCH_NEGATIVE"))
                                       )
                                    ) {
                                    mSession.handleErrorRetry(KYCManager.getInstance().getErrorMessage(String.valueOf(statusCode), message), KYCSession.RETRY_SELFIE_SCAN);
                            }

                            // Specific error management for unrecognized doc
                            else if (statusCode == 5301) {
                                mSession.handleErrorRetry(KYCManager.getInstance().getErrorMessage(String.valueOf(statusCode), message), KYCSession.RETRY_DOC_SCAN);
                            }

                            // Default error display
                            else {
                                mSession.handleErrorRetry("Status: " + status +
                                                "\nCode: " + statusCode +
                                                "\n" + KYCManager.getInstance().getErrorMessage(""+ statusCode, message),
                                        KYCSession.RETRY_DOC_SCAN);
                            }
                        }
                    } catch (final JSONException exception) {
                        mSession.handleErrorAbort(exception.getLocalizedMessage());
                    } catch (InterruptedException exception) {
                        mSession.handleErrorAbort(exception.toString());
                    }
                } else if (error != null) {
                    // Direct communication error.
                    mSession.handleErrorAbort(error);
                } else {
                    // Unknown state. Successful communication with empty result.
                    mSession.handleErrorAbort(KYCManager.getInstance().getErrorMessage("9919", null));
                }
            });

        } catch (final IOException exception) {
            // Communication / json parsing issue.
            mSession.handleErrorAbort(exception.getLocalizedMessage());
        }
    }

    /**
     * Creates the HTTP JSON body.
     *
     * @param docFront Front side of document.
     * @param docBack  Back side of document.
     * @return JSON representation of the data.
     * @throws JSONException If error occured while setting up JSON object.
     */
    private JSONObject idv_createVerificationJSON(final byte[] docFront,
                                                  final byte[] docBack) throws JSONException {
        // Build document node with front and back side.
        final JSONObject document = new JSONObject();
        if (docFront != null) {
            document.put("front", ImageUtil.base64FromImage(docFront));
        }
        if (docBack != null) {
            document.put("back", ImageUtil.base64FromImage(docBack));
        }
        document.put("captureMethod", "SDK");

        if (docBack == null) {
            document.put("type", "Passport");
            document.put("size", "TD3");
        }

        // Input is object containing document and optionally face.
        final JSONObject input = new JSONObject();
        input.put("document", document);

        // Build final JSON.
        final JSONObject json = new JSONObject();
        json.put("name", "Verify_Document");
        json.put("input", input);

        return json;
    }

    /**
     * Creates the HTTP JSON body.
     *
     * @param nfcData  NFC data from SDK.
     * @return JSON representation of the data.
     * @throws JSONException If error occured while setting up JSON object.
     */
    private JSONObject idv_createNfcVerificationJSON(final CaptureResult nfcData) throws JSONException {
        JSONObject chipData = new JSONObject();
        JSONObject dg = new JSONObject();
        JSONObject status = new JSONObject();

        try {
            // DG
            Map<String, byte[]> mapDg = nfcData.rawData.dg;

            for (String key : mapDg.keySet()) {
                dg.put(key, Base64.encodeToString(mapDg.get(key), Base64.NO_WRAP));
            }

            // Status
            Map<String, Integer> mapStatus = nfcData.rawData.status;

            for (String key : mapStatus.keySet()) {
                int sw = mapStatus.get(key);

                status.put(key, Integer.toString(sw));

//                if (sw != 0) {
//                    status.put(key, "0x" + Integer.toHexString(sw));
//                }
//                else {
//                    status.put(key, Integer.toHexString(sw));
//                }
            }

            // ChipData
            chipData.put("com", Base64.encodeToString(nfcData.rawData.com, Base64.NO_WRAP));
            chipData.put("sod", Base64.encodeToString(nfcData.rawData.sod, Base64.NO_WRAP));
            chipData.put("dg", dg);
            chipData.put("status", status);
            chipData.put("signature", Base64.encodeToString(nfcData.rawData.signature, Base64.NO_WRAP));
            chipData.put("version", "2");
        }
        catch(Exception e) {
        }

        // Input is object containing document and optionally face.
        final JSONObject input = new JSONObject();

        input.put("chipData", chipData);
        input.put("channel", "defaultchip");

        // Build final JSON.
        final JSONObject json = new JSONObject();
        json.put("name", KYCManager.getInstance().isFacialRecognition() ? "Verify_Electronic_Document_Face_Enhanced_Liveness" : "Verify_Electronic_Document");
        json.put("input", input);

        return json;
    }

    /**
     * Creates the HTTP JSON body.
     *
     * @param docFront Front side of document.
     * @param docBack  Back side of document.
     * @return JSON representation of the data.
     * @throws JSONException If error occured while setting up JSON object.
     */
    private JSONObject aware_createVerificationJSON(final byte[] docFront,
                                                    final byte[] docBack) throws JSONException {
        // Build document node with front and back side.
        final JSONObject document = new JSONObject();

        if (docFront != null) {
            final JSONObject front = new JSONObject();

            front.put("white", ImageUtil.base64FromImage(docFront));
            document.put("front", front);
        }

        if (docBack != null) {
            final JSONObject back = new JSONObject();

            back.put("white", ImageUtil.base64FromImage(docBack));
            document.put("back", back);
        }

        document.put("captureMethod", "SDK");

        if (docBack == null) {
            document.put("type", "Passport");
            document.put("size", "TD3");
        }

        // Input is object containing document.
        final JSONObject input = new JSONObject();
        input.put("document", document);

        // Build final JSON.
        final JSONObject json = new JSONObject();
        json.put("name", "Verify_Document_Face_Enhanced_Liveness");
        json.put("input", input);

        return json;
    }

    /**
     * Creates the HTTP JSON body for the verification Step #5.
     *
     * @param enhancedSelfieJson Aware server data.
     *
     * @return JSON representation of the data.
     *
     * @throws JSONException If error occurred while setting up JSON object.
     */
    private JSONObject aware_enhancedLivenessJSON(final String enhancedSelfieJson) throws JSONException {
        // Build document node with enhanced liveness Aware data.
        final JSONObject serverData = new JSONObject(enhancedSelfieJson);
        final JSONArray frames = serverData.getJSONObject("video").getJSONObject("workflow_data").getJSONArray("frames");
        final JSONObject metaData = serverData.getJSONObject("video").getJSONObject("meta_data");

        JSONObject newServerData = new JSONObject();
        JSONArray newFrames = new JSONArray();
        JSONObject newWorkflowData = new JSONObject();
        JSONObject newMetaData = new JSONObject();

        // Get 3 first frames and update timestamps
        JSONObject frame1 = (JSONObject) frames.get(0);
        JSONObject frame2 = (JSONObject) frames.get(1);
        JSONObject frame3 = (JSONObject) frames.get(2);

        // New frames
        newFrames.put(0, frame1);
        newFrames.put(1, frame2);
        newFrames.put(2, frame3);

        // New Workflow Data
        newWorkflowData.put("workflow", "charlie4");
        newWorkflowData.put("frames", newFrames);

        // New Meta Data
        newMetaData.put("client_device_brand", metaData.get("client_device_brand"));
        newMetaData.put("client_device_model", metaData.get("client_device_model"));
        newMetaData.put("client_os_version", metaData.get("client_os_version"));
        newMetaData.put("client_version", metaData.get("client_version"));
        newMetaData.put("localization", metaData.get("localization"));
        newMetaData.put("programming_language_version", metaData.get("programming_language_version"));

        // Rebuild Aware server data
        JSONObject video = new JSONObject();
        video.put("meta_data", newMetaData);
        video.put("workflow_data", newWorkflowData);
        newServerData.put("video", video);

        // Build final JSON.
        final JSONObject json = new JSONObject();
        json.put("name", KYCManager.getInstance().isNfcMode() ? "Verify_Electronic_Document_Face_Enhanced_Liveness" : "Verify_Document_Face_Enhanced_Liveness");

        final JSONObject input = new JSONObject();
        input.put("enhancedLiveness", newServerData);
        json.put("input", input);

        return json;
    }

    /**
     * Setups the {@code HttpURLConnection} (authorization, content type, type).
     *
     * @param url URL.
     * @return {@code HttpURLConnection}.
     * @throws IOException If error occurs while opening the connection.
     */
    private HttpURLConnection getUrlConnection(final URL url) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.addRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Content-Type", "application/json");

        if (KYCManager.getInstance().getKycQRCodeVersion().equals(KYC_QR_CODE_VERSION_KYC2)) {
            connection.addRequestProperty("Authorization", "Basic " + KYCManager.getInstance().getBaseCredentials());
        }

        return connection;
    }

    /**
     * Sends data to verification backend.
     *
     * @param json       JSON body.
     * @param connection Connection.
     * @param handler    Callback.
     */
    private void getConnectionResponse(final JSONObject json,
                                       final HttpURLConnection connection,
                                       final GenericResponse handler) {
        new Thread(() -> {
            try {
                if (json != null) {
                    JsonUtil.logJson(json, "JSON Request");
                }
                else {
                    Log.w("KYC", "GET...");
                }

                // Prepare stream.
                if (json != null) {
                    final OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(json.toString().getBytes());
                    outputStream.flush();
                }

                // Execute request.
                connection.connect();

                // Read response.
                final StringBuilder responseSB = new StringBuilder();
                final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = reader.readLine();

                while (line != null) {
                    responseSB.append(line);
                    line = reader.readLine();
                }

                if (responseSB != null) {
                    JsonUtil.logJson(new JSONObject(responseSB.toString()), "JSON Response");
                }

                handler.onFinished(responseSB.toString(), null);
            } catch (final IOException exception) {
                StringBuilder sb;

                // Log Header Fields
                Map<String, List<String>> headerFields = connection.getHeaderFields();
                sb = new StringBuilder();
                Iterator<?> it = headerFields.keySet().iterator();
                sb.append('\n');
                sb.append("<HEADERS url=\"" + connection.getURL().toString() + "\">");

                while (it.hasNext()) {
                    String name = (String) it.next();
                    sb.append('\n');
                    sb.append("[" + name + "]:");
                    sb.append(connection.getHeaderField(name));
                }
                sb.append('\n');
                sb.append("</HEADERS>");

                if (sb != null) {
                    Log.e("KYC", sb.toString());

                    // HTTP Error 401
                    if (sb.toString().contains("401 Unauthorized")) {
                        handler.onFinished(null, KYCManager.getInstance().getErrorMessage("9911", null));
                    }
                    // HTTP Error 403
                    else if (sb.toString().contains("403 Forbidden")) {
                        if (mCurrentStep == 1) {
                            handler.onFinished(null, KYCManager.getInstance().getErrorMessage("9910", null));
                        } else {
                            handler.onFinished(null, KYCManager.getInstance().getErrorMessage("9919", null));
                        }
                    }
                    // HTTP Error 404
                    else if (sb.toString().contains("404 Not Found")) {
                        if (mCurrentStep == 1) {
                            handler.onFinished(null, KYCManager.getInstance().getErrorMessage("9910", null));
                        } else {
                            handler.onFinished(null, KYCManager.getInstance().getErrorMessage("9919", null));
                        }
                    }
                    // HTTP Error XXX
                    else {
                        handler.onFinished(null, KYCManager.getInstance().getErrorMessage("9912", null));
                    }
                }
                else {
                    handler.onFinished(null, KYCManager.getInstance().getErrorMessage("9919", null));
                }
            } catch (final Exception e) {
                handler.onFinished(null, KYCManager.getInstance().getErrorMessage("9919", null));
            }

            // Disconnect
            connection.disconnect();
        }).start();
    }

    //endregion
}
