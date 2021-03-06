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

package com.thalesgroup.kyc.idvconnect.helpers.communication;

import android.util.Log;

import com.thalesgroup.kyc.idvconnect.BuildConfig;
import com.thalesgroup.kyc.idvconnect.helpers.DataContainer;
import com.thalesgroup.kyc.idvconnect.helpers.KYCConfiguration;
import com.thalesgroup.kyc.idvconnect.helpers.KYCManager;
import com.thalesgroup.kyc.idvconnect.helpers.communication.structures.KYCResponse;
import com.thalesgroup.kyc.idvconnect.helpers.util.ImageUtil;
import com.thalesgroup.kyc.idvconnect.helpers.util.JsonUtil;

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

import static com.thalesgroup.kyc.idvconnect.helpers.KYCManager.KYC_QR_CODE_VERSION_KYC2;
import static com.thalesgroup.kyc.idvconnect.helpers.communication.KYCSession.RETRY_SELFIE_SCAN;

/**
 * Class which ensures the communication with the verification backend.
 */
public class KYCCommunication {

    //region Definition

    public static final int STEP_START_VERIFICATION = 0;
    public static final int STEP_DOC_FRONT_VERIFICATION = 1;
    public static final int STEP_DOC_BACK_VERIFICATION = 2;
    public static final int STEP_SELFIE_VERIFICATION = 3;

    private static final String STATE_WAITING = "Waiting";
    private static final String STATE_FINISHED = "Finished";
    private static final String STATE_RUNNING = "Running";
    private static final String STATE_FAILED = "Failed";
    private static final String STATE_ERROR = "Error";

    private static final String SCENARIO_DOC = "Connect_Verify_Document";
    private static final String SCENARIO_FACE = "Connect_Verify_Document_Face";
    private static final String SCENARIO_FACE_PASSIVE_LIVENESS = "Connect_Verify_Document_Face_Passive_Liveness";
    private static final String SCENARIO_FACE_ENHANCED_PASSIVE_LIVENESS = "Connect_Verify_Document_Face_Enhanced_Liveness";

    private static KYCSession mSession;
    private static int mCurrentStep = 1;

    private String getFaceScenario() {
        if (KYCManager.getInstance().isActiveFaceLivenessMode()) {
            return SCENARIO_FACE;
        }
        else if (KYCManager.getInstance().isPassiveFaceLivenessMode()) {
            return SCENARIO_FACE_PASSIVE_LIVENESS;
        }
        else if (KYCManager.getInstance().isEnhancedPassiveFaceLivenessMode()) {
            return SCENARIO_FACE_ENHANCED_PASSIVE_LIVENESS;
        }

        return SCENARIO_FACE_PASSIVE_LIVENESS;
    }

    /**
     * Response callback.
     */
    private interface GenericResponse {

        /**
         * Finish callback method.
         *
         * @param response Response.
         * @param error Error.
         */
        void onFinished(String response, String error);
    }

    //endregion

    //region Public API

    /**
     * Sends the document and face images to the verification backend for verification.
     *
     * @param handler Callback.
     */
    public void verifyDocument(final KYCSession.KYCResponseHandler handler, int startStep) {
        // Prepare session.
        if (startStep == STEP_START_VERIFICATION) {
            mSession = new KYCSession(KYCManager.getInstance().getBaseUrl(), handler);
            step1RequestPrepareAndSend();
        }
        else if (startStep == STEP_DOC_FRONT_VERIFICATION) {
            mSession.setHandler(handler);
            step2RequestPrepareAndSend();
        }
        else if (startStep == STEP_DOC_BACK_VERIFICATION) {
            mSession.setHandler(handler);
            step3RequestPrepareAndSend();
        }
        else if (startStep == STEP_SELFIE_VERIFICATION) {
            mSession.setHandler(handler);

            if (KYCManager.getInstance().isEnhancedPassiveFaceLivenessMode()) {
                enhancedLivenessStep5RequestPrepareAndSend();
            }
            else {
                step5RequestPrepareAndSend();
            }
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
     * Step #1 verification step with the verification backend.
     * */
    private void step1RequestPrepareAndSend() {
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
            final JSONObject json = step1RequestCreateJSON(DataContainer.instance().mSelfie != null);

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
                        if (status != null && status.equalsIgnoreCase(STATE_FAILED)) {
                            final String message = res.getJSONObject("status").getJSONObject("result").getString("message");
                            mSession.handleErrorRetry(message, KYCSession.RETRY_DOC_SCAN);
                            return;
                        } else if (status != null && status.equalsIgnoreCase(STATE_ERROR)) {
                            mSession.handleErrorAbort("Configuration error.");
                            return;
                        }

                        // Pass get the session id to current session and continue.
                        mSession.updateWithSessionId(sessionId);
                        // Call it directly so we don't have to deal with sync.
                        step2RequestPrepareAndSend();
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
     * Step #2 verification step with the verification backend.
     *
     * */
    private void step2RequestPrepareAndSend() {
        mCurrentStep = 2;

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getUrlDocumentFront());
            connection.setDoInput(true);
            connection.setRequestMethod("PATCH");

            if (BuildConfig.DEBUG) {
                Log.w("KYC", "URL: " + connection.getURL().toString());
            }

            // Build post JSON
            final JSONObject json = step2RequestCreateJSON(DataContainer.instance().mDocFront, DataContainer.instance().mSelfie != null);

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
                        if (status != null && status.equalsIgnoreCase(STATE_FAILED)) {
                            final String message = res.getJSONObject("status").getJSONObject("result").getString("message");
                            mSession.handleErrorRetry(message, KYCSession.RETRY_DOC_SCAN);
                            return;
                        } else if (status != null && status.equalsIgnoreCase(STATE_ERROR)) {
                            mSession.handleErrorAbort("Configuration error.");
                            return;
                        }

                        // Pass get the session id to current session and continue.
                        mSession.updateWithSessionId(sessionId);
                        // Call it directly so we don't have to deal with sync.
                        step3RequestPrepareAndSend();
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
     * Step #3 verification step with the verification backend.
     *
     * */
    private void step3RequestPrepareAndSend() {
        mCurrentStep = 3;

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getUrlDocumentBack());
            connection.setDoInput(true);
            connection.setRequestMethod("PATCH");

            if (BuildConfig.DEBUG) {
                Log.w("KYC", "URL: " + connection.getURL().toString());
            }

            // Build post JSON
            final JSONObject json = step3RequestCreateJSON(DataContainer.instance().mDocBack, DataContainer.instance().mSelfie != null);

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
                        if (status != null && status.equalsIgnoreCase(STATE_FAILED)) {
                            final String message = res.getJSONObject("status").getJSONObject("result").getString("message");
                            mSession.handleErrorRetry(message, KYCSession.RETRY_DOC_SCAN);
                            return;
                        } else if (status != null && status.equalsIgnoreCase(STATE_ERROR)) {
                            mSession.handleErrorAbort("Configuration error.");
                            return;
                        }

                        // Pass get the session id to current session and continue.
                        mSession.updateWithSessionId(sessionId);
                        // Call it directly so we don't have to deal with sync.
                        step4RequestPrepareAndSend();
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
     * Step #4 verification step with the verification backend.
     *
     * */
    private void step4RequestPrepareAndSend() {
        mCurrentStep = 4;

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getUrlDocument());
            connection.setDoInput(true);
            connection.setRequestMethod("PATCH");

            if (BuildConfig.DEBUG) {
                Log.w("KYC", "URL: " + connection.getURL().toString());
            }

            // Build post JSON
            final JSONObject json = step4RequestCreateJSON(DataContainer.instance().mSelfie != null);

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
                        final String status = res.getString("status");

                        // Server operation is finished.
                        if (status != null && status.equalsIgnoreCase(STATE_WAITING) && DataContainer.instance().mSelfie != null) {
                            if (KYCManager.getInstance().isEnhancedPassiveFaceLivenessMode()) {
                                enhancedLivenessStep5RequestPrepareAndSend();
                            }
                            else {
                                step5RequestPrepareAndSend();
                            }
                        } else if (status != null && status.equalsIgnoreCase(STATE_FINISHED) && DataContainer.instance().mSelfie == null) {
                            final KYCResponse result = new KYCResponse(res.getJSONObject("state").getJSONObject("result"));
                            mSession.handleResult(result);
                        } else {
                            final int statusCode = res.getJSONObject("state").getJSONObject("result").getInt("code");
                            final String message = res.getJSONObject("state").getJSONObject("result").getString("message");
                            mSession.handleErrorRetry("Status: " + status +
                                    "\nCode: " + statusCode +
                                    "\n" + KYCManager.getInstance().getErrorMessage(""+ statusCode, message),
                                    KYCSession.RETRY_DOC_SCAN);
                        }
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
     * Step #5 verification step with the verification backend.
     *
     * */
    private void step5RequestPrepareAndSend() {
        mCurrentStep = 5;

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getUrlSelfie());
            connection.setDoInput(true);
            connection.setRequestMethod("PATCH");

            if (BuildConfig.DEBUG) {
                Log.w("KYC", "URL: " + connection.getURL().toString());
            }

            // Build post JSON
            final JSONObject json = step5RequestCreateJSON(DataContainer.instance().mSelfie);

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
                        final String status = res.getString("status");

                        // Server operation is finished.
                        if (status != null && status.equalsIgnoreCase(STATE_FINISHED)) {
                            final KYCResponse result = new KYCResponse(res.getJSONObject("state").getJSONObject("result"));
                            mSession.handleResult(result);
                        }
                        else if (  (status != null && status.equalsIgnoreCase(STATE_RUNNING))
                                 &&(KYCManager.getInstance().isPassiveFaceLivenessMode())) {
                            passiveLivenessStep6RequestPrepareAndSend(0);
                        }
                        else {
                            final int statusCode = res.getJSONObject("state").getJSONObject("result").getInt("code");
                            final String message = res.getJSONObject("state").getJSONObject("result").getString("message");
                            mSession.handleErrorRetry("Status: " + status +
                                    "\nCode: " + statusCode +
                                    "\n" + KYCManager.getInstance().getErrorMessage(""+ statusCode, message),
                                    KYCSession.RETRY_DOC_SCAN);
                        }
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
     * Step #5 verification step with the verification backend.
     *
     * */
    private void enhancedLivenessStep5RequestPrepareAndSend() {
        mCurrentStep = 5;

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getUrlEnhancedLiveness());
            connection.setDoInput(true);
            connection.setRequestMethod("PATCH");

            if (BuildConfig.DEBUG) {
                Log.w("KYC", "URL: " + connection.getURL().toString());
            }

            // Build post JSON
            final JSONObject json = enhancedLivenessStep5RequestCreateJSON(DataContainer.instance().mEnhancedSelfieJson);

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
                        if (status != null && status.equalsIgnoreCase(STATE_FAILED)) {
                            final String message = res.getJSONObject("status").getJSONObject("result").getString("message");
                            mSession.handleErrorRetry(message, KYCSession.RETRY_DOC_SCAN);
                            return;
                        } else if (status != null && status.equalsIgnoreCase(STATE_ERROR)) {
                            mSession.handleErrorAbort("Configuration error.");
                            return;
                        }

                        // Pass get the session id to current session and continue.
                        mSession.updateWithSessionId(sessionId);
                        // Call it directly so we don't have to deal with sync.
                        enhancedLivenessStep6RequestPrepareAndSend(0);
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
     * Step #6 verification step with the verification backend.
     *
     *  @param counter Polling counter.
     *
     * */
    private void passiveLivenessStep6RequestPrepareAndSend(int counter) {
        mCurrentStep = 6;

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getUrlPassiveLivenessPollResult());
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

                                passiveLivenessStep6RequestPrepareAndSend(counter + 1);
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
                            if (statusCode == 5321) {
                                final KYCResponse result = new KYCResponse(res.getJSONObject("state").getJSONObject("result"));
                                String assessment = null;
                                String livenessError = "Error during liveness processing";

                                if (result.getLivenessResult() != null)  {
                                    assessment = result.getLivenessResult().getAssessment();
                                    livenessError = result.getLivenessResult().getError();
                                }

                                if (assessment != null) {
                                    if (assessment.equals("NotLive")) {
                                        mSession.handleErrorRetry(KYCManager.getInstance().getErrorMessage("9901"), RETRY_SELFIE_SCAN);
                                    }
                                    else if (assessment.equals("PoorQuality")) {
                                        mSession.handleErrorRetry(KYCManager.getInstance().getErrorMessage("9902"), RETRY_SELFIE_SCAN);
                                    }
                                } else {
                                    mSession.handleErrorRetry(livenessError, RETRY_SELFIE_SCAN);
                                }
                            }
                            else if (  (statusCode >= 5331)
                                     &&(statusCode <= 5333)) {
                                final KYCResponse result = new KYCResponse(res.getJSONObject("state").getJSONObject("result"));

                                if (statusCode == 5332) {
                                    mSession.handleErrorRetry(KYCManager.getInstance().getErrorMessage("5332"), RETRY_SELFIE_SCAN);
                                }
                                else {
                                    mSession.handleErrorRetry(KYCManager.getInstance().getErrorMessage(String.valueOf(statusCode), message), RETRY_SELFIE_SCAN);
                                }
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
     * Step #6 verification step with the verification backend.
     *
     *  @param counter Polling counter.
     *
     * */
    private void enhancedLivenessStep6RequestPrepareAndSend(int counter) {
        mCurrentStep = 6;

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

                                enhancedLivenessStep6RequestPrepareAndSend(counter + 1);
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
                                    mSession.handleErrorRetry(KYCManager.getInstance().getErrorMessage("5332"), RETRY_SELFIE_SCAN);
                                }
                                else {
                                    mSession.handleErrorRetry(KYCManager.getInstance().getErrorMessage(String.valueOf(statusCode), message), RETRY_SELFIE_SCAN);
                                }
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
     * Creates the HTTP JSON body for the verification Step #1.
     *
     * @param isSelfie Selfie availability.
     *
     * @return JSON representation of the data.
     *
     * @throws JSONException If error occured while setting up JSON object.
     */
    private JSONObject step1RequestCreateJSON(final boolean isSelfie) throws JSONException {
        // Build final JSON.
        final JSONObject json = new JSONObject();
        json.put("name", isSelfie ? getFaceScenario() : SCENARIO_DOC);

        return json;
    }

    /**
     * Creates the HTTP JSON body for the verification Step #2.
     *
     * @param docFront Front side of document.
     * @param isSelfie Selfie availability.
     *
     * @return JSON representation of the data.
     *
     * @throws JSONException If error occurred while setting up JSON object.
     */
    private JSONObject step2RequestCreateJSON(final byte[] docFront,
                                              final boolean isSelfie) throws JSONException {
        // Build document node with front doc.
        final JSONObject input = new JSONObject();
        if (docFront != null) {
            input.put("frontWhiteImage", ImageUtil.base64FromImage(docFront));
        }

        // Build final JSON.
        final JSONObject json = new JSONObject();
        json.put("name", isSelfie ? getFaceScenario() : SCENARIO_DOC);
        json.put("input", input);

        return json;
    }

    /**
     * Creates the HTTP JSON body for the verification Step #3.
     *
     * @param docBack Back side of document.
     * @param isSelfie Selfie availability.
     *
     * @return JSON representation of the data.
     *
     * @throws JSONException If error occurred while setting up JSON object.
     */
    private JSONObject step3RequestCreateJSON(final byte[] docBack,
                                              final boolean isSelfie) throws JSONException {
        // Build document node with back doc.
        final JSONObject input = new JSONObject();
        if (docBack != null) {
            input.put("backWhiteImage", ImageUtil.base64FromImage(docBack));
        }

        // Build final JSON.
        final JSONObject json = new JSONObject();
        json.put("name", isSelfie ? getFaceScenario() : SCENARIO_DOC);
        json.put("input", input);

        return json;
    }

    /**
     * Creates the HTTP JSON body for the verification Step #4.
     *
     * @param isSelfie Selfie availability.
     *
     * @return JSON representation of the data.
     *
     * @throws JSONException If error occured while setting up JSON object.
     */
    private JSONObject step4RequestCreateJSON(final boolean isSelfie) throws JSONException {
        // Build final JSON.
        final JSONObject json = new JSONObject();
        json.put("name", isSelfie ? getFaceScenario() : SCENARIO_DOC);

        return json;
    }

    /**
     * Creates the HTTP JSON body for the verification Step #5.
     *
     * @param selfie Selfie image.
     *
     * @return JSON representation of the data.
     *
     * @throws JSONException If error occurred while setting up JSON object.
     */
    private JSONObject step5RequestCreateJSON(final byte[] selfie) throws JSONException {
        // Build document node with selfie.
        final JSONObject input = new JSONObject();
        if (selfie != null) {
            input.put("face", ImageUtil.base64FromImage(selfie));
        }

        // Build final JSON.
        final JSONObject json = new JSONObject();
        json.put("name", getFaceScenario());
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
    private JSONObject enhancedLivenessStep5RequestCreateJSON(final String enhancedSelfieJson) throws JSONException {
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
        json.put("name", getFaceScenario());

        final JSONObject input = new JSONObject();
        input.put("enhancedLiveness", newServerData);
        json.put("input", input);

        return json;
    }

    /**
     * Setups the {@code HttpURLConnection} (authorization, content type, type).
     *
     * @param url URL.
     *
     * @return {@code HttpURLConnection}.
     *
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
     * @param json JSON body.
     * @param connection Connection.
     * @param handler Callback.
     */
    private void getConnectionResponse(final JSONObject json,
                                       final HttpURLConnection connection,
                                       final GenericResponse handler) {
        new Thread(() -> {
            try {
                if (json != null) {
                    JsonUtil.logJson(json.toString(), "JSON Request");
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
                    JsonUtil.logJson(responseSB.toString(), "JSON Response");
                }
                
                handler.onFinished(responseSB.toString(), null);

            } catch (final IOException exception) {
                Log.e("KYC", "Fail: " + exception.toString());

                try  {
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
            }

            // Disconnect
            connection.disconnect();
        }).start();
    }
}

