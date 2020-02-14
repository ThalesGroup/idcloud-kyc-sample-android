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

import com.thalesgroup.kyc.idv.helpers.Configuration;
import com.thalesgroup.kyc.idv.helpers.KYCManager;
import com.thalesgroup.kyc.idv.helpers.util.ImageUtil;

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

    private static KYCSession mSession;

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
     * @param docFront Front side of the document.
     * @param docBack  Back side of the document.
     * @param selfie   Selfie image.
     * @param handler  Callback.
     */
    public void verifyDocument(final byte[] docFront,
                               final byte[] docBack,
                               final byte[] selfie,
                               final KYCSession.KYCResponseHandler handler) {
        // Prepare session.
        mSession = new KYCSession(Configuration.IDCLOUD_URL, handler);

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getBaseUrl());
            connection.setDoOutput(true);

            // Build post JSON
            final JSONObject json = createVerificationJSON(docFront, docBack, selfie);

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
                        verifyDocumentSecondStep();
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
     * Starts the second verification step with the verification backend.
     */
    private void verifyDocumentSecondStep() {

        try {
            Thread.sleep(Configuration.IDCLOUD_RETRY_DELAY_SEC * 1000);
        } catch (final InterruptedException e) {
            // nothing to do
        }

        try {
            // Get connection
            final HttpURLConnection connection = getUrlConnection(mSession.getUrlWithSessionId());
            connection.setDoInput(true);

            // Send request in a new Thread and handle response
            getConnectionResponse(null, connection, (response, error) -> {
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
                        if (status.equalsIgnoreCase("Running")) {
                            // Make sure we will not create infinite loop.
                            if (mSession.getTryCount() <= Configuration.IDCLOUD_NUMBER_OF_RETRIES) {
                                mSession.setTryCount(mSession.getTryCount() + 1);
                                verifyDocumentSecondStep();
                            } else {
                                // Already pass number of retries. We can end here.
                                mSession.handleError("Failed to get server response in time.");
                            }
                        } else if (status.equalsIgnoreCase("Finished")) {
                            // Server operation finished.
                            final KYCResponse result = new KYCResponse(res.getJSONObject("state").getJSONObject("result"));

                            // Check document node. It's mandatory.
                            if (result.getDocument() != null) {
                                mSession.handleResult(result);
                            } else {
                                // Return to handler.
                                mSession.handleError("Error Code: " + result.getCode() + ", Error Message: " + result.getMessageReadable());
                            }

                        } else if (status.equalsIgnoreCase("Failure")) {
                            // Server operation failed.
                            final KYCResponse result = new KYCResponse(res.getJSONObject("state").getJSONObject("result"));
                            // Return to handler.
                            mSession.handleError("Error Code: " + result.getCode() + ", Error Message: " + result.getMessageReadable());
                        } else {
                            // Unknown state. Not handled response type.
                            mSession.handleError("Unexpected server response.");
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

        } catch (final IOException exception) {
            mSession.handleError(exception.getLocalizedMessage());
        }
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
        connection.addRequestProperty("Authorization", "Bearer " + KYCManager.getInstance().getJsonWebToken());

        return connection;
    }

    /**
     * Creates the HTTP JSON body.
     *
     * @param docFront Front side of document.
     * @param docBack  Back side of document.
     * @param selfie   Selfie image.
     * @return JSON representation of the data.
     * @throws JSONException If error occured while setting up JSON object.
     */
    private JSONObject createVerificationJSON(final byte[] docFront,
                                              final byte[] docBack,
                                              final byte[] selfie) throws JSONException {
        // Build document node with front and back side.
        final JSONObject document = new JSONObject();
        if (docFront != null) {
            document.put("front", ImageUtil.base64FromImage(docFront));
        }
        if (docBack != null) {
            document.put("back", ImageUtil.base64FromImage(docBack));
        }
        document.put("captureMethod", "SDK");
        document.put("type", "Residence_Permit");
        document.put("size", "TD1");

        // Input is object containing document and optionaly face.
        final JSONObject input = new JSONObject();
        input.put("document", document);
        if (selfie != null) {
            // Build selfie node.
            final JSONObject face = new JSONObject();
            face.put("image", ImageUtil.base64FromImage(selfie));
            input.put("face", face);
        }

        // Build final JSON.
        final JSONObject json = new JSONObject();
        json.put("name", selfie != null ? "Verify_Document_Face" : "Verify_Document");
        json.put("input", input);

        return json;
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

    //endregion
}
