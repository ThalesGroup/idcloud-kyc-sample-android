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

import com.thalesgroup.kyc.idvconnect.helpers.Configuration;
import com.thalesgroup.kyc.idvconnect.helpers.KYCManager;
import com.thalesgroup.kyc.idvconnect.helpers.communication.structures.KYCResponse;
import com.thalesgroup.kyc.idvconnect.helpers.util.ImageUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Class which ensures the communication with the verification backend.
 */
public class KYCCommunication {

    //region Definition

    private static final String STATE_WAITING = "Waiting";
    private static final String STATE_FINISHED = "Finished";

    private static final String SCENARIO_DOC = "Connect_Verify_Document";
    private static final String SCENARIO_FACE = "Connect_Verify_Document_Face";

    private static KYCSession mSession;

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
     * @param docFront Front side of the document.
     * @param docBack Back side of the document.
     * @param selfie Selfie image.
     * @param handler Callback.
     */
    public void verifyDocument(final byte[] docFront,
                               final byte[] docBack,
                               final byte[] selfie,
                               final KYCSession.KYCResponseHandler handler) {
        initialRequestPrepareAndSend(docFront, docBack, selfie, handler);
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
     * Starts the first verification step with the verification backend.
     *
     * @param docFront Front side of the document.
     * @param docBack Back side of the document.
     * @param selfie Selfie image.
     * @param handler Callback.
     * */
    private void initialRequestPrepareAndSend(final byte[] docFront,
                               final byte[] docBack,
                               final byte[] selfie,
                               final KYCSession.KYCResponseHandler handler) {
        // Prepare session.
        mSession = new KYCSession(Configuration.IDCLOUD_URL, handler);

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getBaseUrl());
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            // Build post JSON
            final JSONObject json = initialRequestCreateJSON(docFront, docBack, selfie);

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
                        if (status != null && status.equalsIgnoreCase(KYCSession.COMMON_STATE_FAILED)) {
                            final String message = res.getJSONObject("status").getJSONObject("result").getString("message");
                            mSession.handleError(message);
                            return;
                        } else if (status != null && status.equalsIgnoreCase(KYCSession.COMMON_STATE_ERROR)) {
                            mSession.handleError("Configuration error. Contact Thales representative.");
                            return;
                        }

                        // Pass get the session id to current session and continue.
                        mSession.updateWithSessionId(sessionId);
                        // Call it directly so we don't have to deal with sync.
                        verifyDocumentPrepareAndSend(selfie);
                    } catch (final JSONException exception) {
                        mSession.handleError(exception.getLocalizedMessage());
                    }
                } else if (error != null) {
                    // Direct communication error.
                    mSession.handleError(error);
                } else {
                    // Unknown state. Successful communication with empty result.
                    mSession.handleError("Unknown communication error.");
                }
            });

        } catch (final IOException | JSONException exception) {
            // Communication / json parsing issue.
            mSession.handleError(exception.getLocalizedMessage());
        }

    }

    /**
     * Starts the second verification step with the verification backend - document verification.
     *
     * @param selfie Selfie image.
     */
    private void verifyDocumentPrepareAndSend(final byte[] selfie) {
        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getUrlDocumennt());
            connection.setDoInput(true);
            connection.setRequestMethod("PATCH");
            final JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", selfie != null ? SCENARIO_FACE : SCENARIO_DOC);

            // Send request in a new Thread and handle response
            getConnectionResponse(jsonObject, connection, (response, error) -> {
                // Fragment/Activity already gone, so no need to continue with request.
                if (!mSession.isListenerRegistered()) {
                    return;
                }

                if (response != null) {
                    try {
                        // Parse server response and get session id.
                        final JSONObject res = new JSONObject(response);
                        final String status = res.getString("status");

                        // Server operation is still running.
                        if (status != null && status.equalsIgnoreCase(STATE_WAITING) && selfie != null) {
                            verifySelfiePrepareAndSend(selfie);
                        } else if (status != null && status.equalsIgnoreCase(STATE_FINISHED) && selfie == null) {
                            final KYCResponse result = new KYCResponse(res.getJSONObject("state").getJSONObject("result"));
                            mSession.handleResult(result);
                        } else {
                            final int statusCode = res.getJSONObject("state").getJSONObject("result").getInt("code");
                            final String message = res.getJSONObject("state").getJSONObject("result").getString("message");
                            mSession.handleError("Status: " + status + " Code: " + statusCode + " Message: " + message);                        }
                    } catch (final JSONException exception) {
                        mSession.handleError(exception.getLocalizedMessage());
                    }
                } else if (error != null) {
                    mSession.handleError(error);
                } else {
                    mSession.handleError("Unknown communication error.");
                }
            });

        } catch (final IOException | JSONException exception) {
            mSession.handleError(exception.getLocalizedMessage());
        }
    }

    /**
     * Starts the third verification step with the verification backend - selfie verification.
     *
     * @param selfie Selfie image.
     */
    private void verifySelfiePrepareAndSend(final byte[] selfie) {
        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getUrlSelfie());
            connection.setDoInput(true);
            connection.setRequestMethod("PATCH");
            final JSONObject jsonObject = verifySelfieCreateJSON(selfie);

            // Send request in a new Thread and handle response
            getConnectionResponse(jsonObject, connection, (response, error) -> {
                // Fragment/Activity already gone, so no need to continue with request.
                if (!mSession.isListenerRegistered()) {
                    return;
                }

                if (response != null) {
                    try {
                        // Parse server response and get session id.
                        final JSONObject res = new JSONObject(response);
                        final String status = res.getString("status");

                        // Server operation is still running.
                        if (status != null && status.equalsIgnoreCase(STATE_FINISHED)) {
                            final KYCResponse result = new KYCResponse(res.getJSONObject("state").getJSONObject("result"));
                            mSession.handleResult(result);
                        } else {
                            final int statusCode = res.getJSONObject("state").getJSONObject("result").getInt("code");
                            final String message = res.getJSONObject("state").getJSONObject("result").getString("message");
                            mSession.handleError("Status: " + status + " Code: " + statusCode + " Message: " + message);
                        }
                    } catch (final JSONException exception) {
                        mSession.handleError(exception.getLocalizedMessage());
                    }
                } else if (error != null) {
                    mSession.handleError(error);
                } else {
                    mSession.handleError("Unknown communication error.");
                }
            });

        } catch (final IOException | JSONException exception) {
            mSession.handleError(exception.getLocalizedMessage());
        }
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
        connection.addRequestProperty("Authorization", "Bearer " + KYCManager.getInstance().getJsonWebToken());
        connection.addRequestProperty("X-API-KEY", KYCManager.getInstance().getApiKey());

        return connection;
    }

    /**
     * Creates the HTTP JSON body for the first verification step.
     *
     * @param docFront Front side of document.
     * @param docBack Back side of document.
     * @param selfie Selfie image.
     *
     * @return JSON representation of the data.
     *
     * @throws JSONException If error occured while setting up JSON object.
     */
    private JSONObject initialRequestCreateJSON(final byte[] docFront,
                                                final byte[] docBack,
                                                final byte[] selfie) throws JSONException {
        // Build document node with front and back side.
        final JSONObject input = new JSONObject();
        if (docFront != null) {
            input.put("frontWhiteImage", ImageUtil.base64FromImage(docFront));
        }
        if (docBack != null) {
            input.put("backWhiteImage", ImageUtil.base64FromImage(docBack));
        }
        input.put("captureMethod", "SDK");

        // Optional values for faster evaluation.

        // Value: "type"
        // Description: Document type.
        // Possible values are: "Passport", "ID", "DL", "ResidencePermit", "HealthCard", "VISA", "Other"

        // Value: "size"
        // Description: Document size.
        // Possible values are: "TD1", "TD2", "TD3"

        // Build final JSON.
        final JSONObject json = new JSONObject();
        json.put("name", selfie != null ? SCENARIO_FACE : SCENARIO_DOC);
        json.put("input", input);

        return json;
    }

    /**
     * Creates the HTTP JSON body.
     *
     * @param selfie Selfie image.
     *
     * @return JSON representation of the data.
     *
     * @throws JSONException If error occured while setting up JSON object.
     */
    private JSONObject verifySelfieCreateJSON(final byte[] selfie) throws JSONException {
        // Input is object containing document and optionaly face.
        final JSONObject json = new JSONObject();
        json.put("name", selfie != null ? SCENARIO_FACE : SCENARIO_DOC);

        if (selfie != null) {
            // Build selfie node.
            final JSONObject input = new JSONObject();
            input.put("face", ImageUtil.base64FromImage(selfie));
            json.put("input", input);
        }

        return json;
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

                handler.onFinished(responseSB.toString(), null);
            } catch (final IOException exception) {
                handler.onFinished(null, exception.getMessage());
            }

            // Disconnect
            connection.disconnect();
        }).start();
    }
}
